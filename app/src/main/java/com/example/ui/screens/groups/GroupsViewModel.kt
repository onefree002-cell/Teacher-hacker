package com.example.ui.screens.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.*
import com.example.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PastSessionHistoryItem(
    val date: String,
    val day: String = "",
    val term: String = "",
    val topic: String = "",
    val notes: String = "",
    val homework: String = "",
    val totalStudents: Int = 0,
    val presentCount: Int = 0,
    val absentCount: Int = 0,
    val lateCount: Int = 0,
    val excusedCount: Int = 0,
    val hwCompletedCount: Int = 0,
    val hwNotDoneCount: Int = 0,
    val records: List<AttendanceEntity> = emptyList(),
    val sessionEntity: SessionEntity? = null
)

data class GroupsUiState(
    val groupsWithCounts: List<GroupWithStudentCount> = emptyList(),
    val filteredGroups: List<GroupWithStudentCount> = emptyList(),
    val searchQuery: String = "",
    val selectedGroupDetails: GroupWithStudentCount? = null,
    val groupStudents: List<StudentEntity> = emptyList(),
    val groupSessions: List<SessionEntity> = emptyList(),
    val groupPastSessions: List<PastSessionHistoryItem> = emptyList(),
    val groupExams: List<ExamEntity> = emptyList(),
    val groupStudyFiles: List<StudyFileEntity> = emptyList(),
    val venues: List<VenueEntity> = emptyList(),
    val teacher: TeacherEntity = TeacherEntity(),
    val isLoading: Boolean = false
)

class GroupsViewModel(private val repository: TeacherPlannerRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupsUiState())
    val uiState: StateFlow<GroupsUiState> = _uiState.asStateFlow()

    init {
        loadGroups()
        loadTeacher()
        loadVenues()
    }

    private fun loadVenues() {
        viewModelScope.launch {
            repository.allVenues.collect { venueList ->
                _uiState.value = _uiState.value.copy(venues = venueList)
            }
        }
    }

    private fun loadTeacher() {
        viewModelScope.launch {
            repository.teacher.collect { t ->
                _uiState.value = _uiState.value.copy(teacher = t ?: TeacherEntity())
            }
        }
    }

    private fun loadGroups() {
        viewModelScope.launch {
            repository.groupsWithStudentCount.collect { list ->
                val query = _uiState.value.searchQuery
                val filtered = if (query.isEmpty()) {
                    list
                } else {
                    list.filter {
                        it.group.name.contains(query, ignoreCase = true) ||
                                it.group.grade.contains(query, ignoreCase = true) ||
                                it.group.groupNumber.contains(query)
                    }
                }
                _uiState.value = _uiState.value.copy(
                    groupsWithCounts = list,
                    filteredGroups = filtered,
                    isLoading = false
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        val filtered = if (query.isEmpty()) {
            _uiState.value.groupsWithCounts
        } else {
            _uiState.value.groupsWithCounts.filter {
                it.group.name.contains(query, ignoreCase = true) ||
                        it.group.grade.contains(query, ignoreCase = true) ||
                        it.group.groupNumber.contains(query)
            }
        }
        _uiState.value = _uiState.value.copy(searchQuery = query, filteredGroups = filtered)
    }

    fun addNewVenue(venue: VenueEntity, onSaved: (VenueEntity) -> Unit) {
        viewModelScope.launch {
            val id = repository.insertVenue(venue)
            onSaved(venue.copy(id = id))
        }
    }

    fun addOrUpdateGroup(group: GroupEntity, initialSessions: List<SessionEntity> = emptyList()) {
        viewModelScope.launch {
            val targetGroupId = if (group.id == 0L) {
                val newId = repository.insertGroup(group)
                newId
            } else {
                repository.updateGroup(group)
                if (_uiState.value.selectedGroupDetails?.group?.id == group.id) {
                    loadGroupDetails(group.id)
                }
                group.id
            }

            if (initialSessions.isNotEmpty()) {
                initialSessions.forEach { session ->
                    repository.insertSession(session.copy(groupId = targetGroupId))
                }
                loadGroupDetails(targetGroupId)
            }
        }
    }

    fun addSessionToGroup(session: SessionEntity) {
        viewModelScope.launch {
            repository.insertSession(session)
            loadGroupDetails(session.groupId)
        }
    }

    fun deleteSession(session: SessionEntity) {
        viewModelScope.launch {
            repository.deleteSession(session)
            loadGroupDetails(session.groupId)
        }
    }

    fun transitionToNewTerm(groupId: Long, newTermName: String) {
        viewModelScope.launch {
            repository.transitionGroupToNewTerm(groupId, newTermName.trim())
            loadGroupDetails(groupId)
        }
    }

    fun deleteGroup(group: GroupEntity) {
        viewModelScope.launch {
            repository.deleteGroup(group)
        }
    }

    fun loadGroupDetails(groupId: Long) {
        viewModelScope.launch {
            val details = repository.groupsWithStudentCount.first().firstOrNull { it.group.id == groupId }
            val students = repository.getStudentsByGroup(groupId).first()
            val sessions = repository.getSessionsByGroup(groupId).first()
            val exams = repository.getExamsByGroup(groupId).first()
            val attendanceList = repository.getAttendanceByGroup(groupId).first()
            val grade = details?.group?.grade ?: ""
            val allFiles = repository.getAllStudyFiles().first()
            val matchingFiles = if (grade.isBlank()) allFiles else allFiles.filter {
                it.grade.contains(grade, ignoreCase = true) || grade.contains(it.grade, ignoreCase = true)
            }

            // Aggregate past sessions from attendance records + recorded sessions
            val attendanceByDate = attendanceList.groupBy { it.date }
            val sessionsByDate = sessions.filter { it.completed || it.date.isNotBlank() }.associateBy { it.date }
            
            // All distinct dates
            val allDates = (attendanceByDate.keys + sessionsByDate.keys).filter { it.isNotBlank() }.distinct().sortedDescending()
            
            val pastSessions = allDates.map { date ->
                val records = attendanceByDate[date] ?: emptyList()
                val sessionEntity = sessionsByDate[date]
                val present = records.count { it.status == "present" }
                val absent = records.count { it.status == "absent" }
                val late = records.count { it.status == "late" }
                val excused = records.count { it.status == "excused" }
                val hwComp = records.count { it.homeworkStatus == "completed" }
                val hwNot = records.count { it.homeworkStatus == "not_done" }

                val topic = sessionEntity?.note ?: ""
                val hw = sessionEntity?.homeworkTitle ?: records.firstOrNull { it.note.startsWith("الواجب:") }?.note?.removePrefix("الواجب:")?.trim() ?: ""
                val notes = sessionEntity?.homeworkNotes ?: records.firstOrNull { it.note.isNotBlank() && !it.note.startsWith("الواجب:") }?.note ?: ""

                PastSessionHistoryItem(
                    date = date,
                    day = sessionEntity?.day ?: "",
                    term = sessionEntity?.term ?: details?.group?.currentTerm ?: "الترم الأول",
                    topic = topic,
                    notes = notes,
                    homework = hw,
                    totalStudents = students.size,
                    presentCount = present,
                    absentCount = absent,
                    lateCount = late,
                    excusedCount = excused,
                    hwCompletedCount = hwComp,
                    hwNotDoneCount = hwNot,
                    records = records,
                    sessionEntity = sessionEntity
                )
            }

            _uiState.value = _uiState.value.copy(
                selectedGroupDetails = details,
                groupStudents = students,
                groupSessions = sessions,
                groupPastSessions = pastSessions,
                groupExams = exams,
                groupStudyFiles = matchingFiles
            )
        }
    }

    fun updatePastSessionDetails(
        groupId: Long,
        date: String,
        newTopic: String,
        newNotes: String,
        newHomework: String,
        context: android.content.Context? = null
    ) {
        viewModelScope.launch {
            val details = _uiState.value.selectedGroupDetails
            val currentSessions = repository.getSessionsByGroup(groupId).first()
            val existing = currentSessions.firstOrNull { it.date == date }

            if (existing != null) {
                repository.updateSession(
                    existing.copy(
                        note = newTopic,
                        homeworkTitle = newHomework,
                        homeworkNotes = newNotes
                    )
                )
            } else {
                repository.insertSession(
                    SessionEntity(
                        groupId = groupId,
                        date = date,
                        day = details?.group?.sessionDays?.split(",")?.firstOrNull() ?: "حصة سابقة",
                        time = details?.group?.sessionTime ?: "16:00",
                        completed = true,
                        term = details?.group?.currentTerm ?: "الترم الأول",
                        note = newTopic,
                        homeworkTitle = newHomework,
                        homeworkNotes = newNotes
                    )
                )
            }

            // Update session folder summary if context available
            if (context != null && details?.group != null) {
                try {
                    val attendanceList = repository.getAttendanceByGroupAndDate(groupId, date).first()
                    val students = repository.getStudentsByGroup(groupId).first()
                    val studentMap = students.associateBy { it.id }
                    val states = attendanceList.map {
                        com.example.ui.screens.attendance.StudentAttendanceState(
                            student = studentMap[it.studentId] ?: StudentEntity(name = "طالب"),
                            status = it.status,
                            homeworkStatus = it.homeworkStatus,
                            note = it.note,
                            attendanceId = it.id
                        )
                    }
                    com.example.util.TeacherHackerDirectoryManager.saveSessionSummaryReport(
                        context = context,
                        group = details.group,
                        date = date,
                        attendanceList = states,
                        topic = newTopic,
                        homework = newHomework,
                        notes = newNotes
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            loadGroupDetails(groupId)
        }
    }
}
