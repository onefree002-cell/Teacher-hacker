package com.example.ui.screens.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.*
import com.example.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class StudentAttendanceState(
    val student: StudentEntity,
    val status: String = "present", // "present", "absent", "late", "excused"
    val homeworkStatus: String = "completed", // "completed", "partial", "not_done", "exempt", "none"
    val note: String = "",
    val attendanceId: Long = 0L
)

data class AttendanceUiState(
    val groups: List<GroupEntity> = emptyList(),
    val allStudents: List<StudentEntity> = emptyList(),
    val teacher: TeacherEntity? = null,
    val selectedGroupId: Long = 0L,
    val selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    val sessionHomework: String = "",
    val sessionTopic: String = "",
    val studentsAttendanceList: List<StudentAttendanceState> = emptyList(),
    val presentCount: Int = 0,
    val absentCount: Int = 0,
    val lateCount: Int = 0,
    val excusedCount: Int = 0,
    val completedHomeworkCount: Int = 0,
    val partialHomeworkCount: Int = 0,
    val notDoneHomeworkCount: Int = 0,
    val exemptHomeworkCount: Int = 0,
    val isSavedSuccess: Boolean = false,
    val isLoading: Boolean = false
)

class AttendanceViewModel(private val repository: TeacherPlannerRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AttendanceUiState())
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                repository.allGroups,
                repository.allStudents,
                repository.teacher
            ) { groups, allStudents, teacher ->
                val currentGroupId = if (_uiState.value.selectedGroupId == 0L && groups.isNotEmpty()) {
                    groups.first().id
                } else {
                    _uiState.value.selectedGroupId
                }
                _uiState.value = _uiState.value.copy(
                    groups = groups,
                    allStudents = allStudents,
                    teacher = teacher,
                    selectedGroupId = currentGroupId
                )
                if (currentGroupId != 0L) {
                    loadStudentsForAttendance(currentGroupId, _uiState.value.selectedDate)
                }
            }.collect {}
        }
    }

    fun onGroupSelected(groupId: Long) {
        _uiState.value = _uiState.value.copy(selectedGroupId = groupId)
        loadStudentsForAttendance(groupId, _uiState.value.selectedDate)
    }

    fun onDateSelected(date: String) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        if (_uiState.value.selectedGroupId != 0L) {
            loadStudentsForAttendance(_uiState.value.selectedGroupId, date)
        }
    }

    fun onSessionHomeworkChanged(homework: String) {
        _uiState.value = _uiState.value.copy(sessionHomework = homework)
    }

    fun onSessionTopicChanged(topic: String) {
        _uiState.value = _uiState.value.copy(sessionTopic = topic)
    }

    private fun loadStudentsForAttendance(groupId: Long, date: String) {
        viewModelScope.launch {
            val students = repository.getStudentsByGroup(groupId).first()
            val existingAttendance = repository.getAttendanceByGroup(groupId).first().filter { it.date == date }
            val attendanceMap = existingAttendance.associateBy { it.studentId }

            val list = students.map { s ->
                val existing = attendanceMap[s.id]
                StudentAttendanceState(
                    student = s,
                    status = existing?.status ?: "present",
                    homeworkStatus = existing?.homeworkStatus ?: "completed",
                    note = existing?.note ?: "",
                    attendanceId = existing?.id ?: 0L
                )
            }

            recalculateCounts(list)
        }
    }

    fun recordScannedStudent(studentId: Long) {
        updateStudentStatus(studentId, "present")
    }

    fun updateStudentStatus(studentId: Long, newStatus: String) {
        val updated = _uiState.value.studentsAttendanceList.map {
            if (it.student.id == studentId) it.copy(status = newStatus) else it
        }
        recalculateCounts(updated)
    }

    fun updateStudentHomeworkStatus(studentId: Long, newHomeworkStatus: String) {
        val updated = _uiState.value.studentsAttendanceList.map {
            if (it.student.id == studentId) it.copy(homeworkStatus = newHomeworkStatus) else it
        }
        recalculateCounts(updated)
    }

    fun updateStudentNote(studentId: Long, note: String) {
        val updated = _uiState.value.studentsAttendanceList.map {
            if (it.student.id == studentId) it.copy(note = note) else it
        }
        _uiState.value = _uiState.value.copy(studentsAttendanceList = updated)
    }

    fun markAllAs(status: String) {
        val updated = _uiState.value.studentsAttendanceList.map {
            it.copy(status = status)
        }
        recalculateCounts(updated)
    }

    fun markAllHomeworkAs(status: String) {
        val updated = _uiState.value.studentsAttendanceList.map {
            it.copy(homeworkStatus = status)
        }
        recalculateCounts(updated)
    }

    private fun recalculateCounts(list: List<StudentAttendanceState>) {
        val present = list.count { it.status == "present" }
        val absent = list.count { it.status == "absent" }
        val late = list.count { it.status == "late" }
        val excused = list.count { it.status == "excused" }

        val completedHw = list.count { it.homeworkStatus == "completed" }
        val partialHw = list.count { it.homeworkStatus == "partial" }
        val notDoneHw = list.count { it.homeworkStatus == "not_done" }
        val exemptHw = list.count { it.homeworkStatus == "exempt" }

        _uiState.value = _uiState.value.copy(
            studentsAttendanceList = list,
            presentCount = present,
            absentCount = absent,
            lateCount = late,
            excusedCount = excused,
            completedHomeworkCount = completedHw,
            partialHomeworkCount = partialHw,
            notDoneHomeworkCount = notDoneHw,
            exemptHomeworkCount = exemptHw
        )
    }

    fun saveAttendance(context: android.content.Context? = null, onSaved: () -> Unit) {
        viewModelScope.launch {
            val currentGroup = _uiState.value.groups.firstOrNull { it.id == _uiState.value.selectedGroupId }
            val groupName = currentGroup?.name ?: "المجموعة"
            val selectedDate = _uiState.value.selectedDate

            val entities = _uiState.value.studentsAttendanceList.map {
                AttendanceEntity(
                    id = it.attendanceId,
                    studentId = it.student.id,
                    sessionId = 0L,
                    groupId = _uiState.value.selectedGroupId,
                    date = selectedDate,
                    status = it.status,
                    homeworkStatus = it.homeworkStatus,
                    note = if (_uiState.value.sessionHomework.isNotBlank() && it.note.isBlank()) {
                        "الواجب: ${_uiState.value.sessionHomework}"
                    } else it.note
                )
            }
            repository.insertAllAttendance(entities)

            // Ensure Session record is stored in database
            if (_uiState.value.selectedGroupId != 0L) {
                try {
                    val sessionEntity = SessionEntity(
                        groupId = _uiState.value.selectedGroupId,
                        date = selectedDate,
                        day = currentGroup?.sessionDays?.split(",")?.firstOrNull()?.trim() ?: "حصة مسجلة",
                        time = currentGroup?.sessionTime ?: "16:00",
                        completed = true,
                        term = currentGroup?.currentTerm ?: "الترم الأول",
                        homeworkTitle = _uiState.value.sessionHomework,
                        homeworkNotes = _uiState.value.sessionHomework,
                        note = _uiState.value.sessionTopic
                    )
                    repository.insertSession(sessionEntity)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Auto-create session folder in DOCUMENTS/TEACHER HACKER/[Date - GroupName] and save summary report
            if (context != null) {
                try {
                    com.example.util.TeacherHackerDirectoryManager.saveSessionSummaryReport(
                        context = context,
                        group = currentGroup,
                        date = selectedDate,
                        attendanceList = _uiState.value.studentsAttendanceList,
                        topic = _uiState.value.sessionTopic,
                        homework = _uiState.value.sessionHomework,
                        notes = "تم تسجيل الحضور بتاريخ $selectedDate"
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            _uiState.value = _uiState.value.copy(isSavedSuccess = true)
            onSaved()
        }
    }
}
