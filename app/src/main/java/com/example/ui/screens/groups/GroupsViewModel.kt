package com.example.ui.screens.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.*
import com.example.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class GroupsUiState(
    val groupsWithCounts: List<GroupWithStudentCount> = emptyList(),
    val filteredGroups: List<GroupWithStudentCount> = emptyList(),
    val searchQuery: String = "",
    val selectedGroupDetails: GroupWithStudentCount? = null,
    val groupStudents: List<StudentEntity> = emptyList(),
    val groupSessions: List<SessionEntity> = emptyList(),
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

    fun addOrUpdateGroup(group: GroupEntity) {
        viewModelScope.launch {
            if (group.id == 0L) {
                repository.insertGroup(group)
            } else {
                repository.updateGroup(group)
                if (_uiState.value.selectedGroupDetails?.group?.id == group.id) {
                    loadGroupDetails(group.id)
                }
            }
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
            val grade = details?.group?.grade ?: ""
            val allFiles = repository.getAllStudyFiles().first()
            val matchingFiles = if (grade.isBlank()) allFiles else allFiles.filter {
                it.grade.contains(grade, ignoreCase = true) || grade.contains(it.grade, ignoreCase = true)
            }

            _uiState.value = _uiState.value.copy(
                selectedGroupDetails = details,
                groupStudents = students,
                groupSessions = sessions,
                groupExams = exams,
                groupStudyFiles = matchingFiles
            )
        }
    }
}
