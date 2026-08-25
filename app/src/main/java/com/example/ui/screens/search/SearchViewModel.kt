package com.example.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.*
import com.example.data.repository.TeacherPlannerRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val allStudents: List<StudentEntity> = emptyList(),
    val allGroups: List<GroupEntity> = emptyList(),
    val allSessions: List<SessionEntity> = emptyList(),
    val allExams: List<ExamEntity> = emptyList(),
    val allPayments: List<PaymentEntity> = emptyList(),
    val filteredStudents: List<StudentEntity> = emptyList(),
    val filteredGroups: List<GroupEntity> = emptyList(),
    val filteredSessions: List<SessionEntity> = emptyList(),
    val filteredExams: List<ExamEntity> = emptyList(),
    val filteredPayments: List<PaymentEntity> = emptyList(),
    val totalResultsCount: Int = 0
)

class SearchViewModel(private val repository: TeacherPlannerRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.allStudents,
                repository.allGroups,
                repository.allSessions,
                repository.allExams,
                repository.allPayments
            ) { students, groups, sessions, exams, payments ->
                _uiState.value = _uiState.value.copy(
                    allStudents = students,
                    allGroups = groups,
                    allSessions = sessions,
                    allExams = exams,
                    allPayments = payments
                )
                filterResults(_uiState.value.query)
            }.collect {}
        }
    }

    fun onQueryChange(newQuery: String) {
        _uiState.value = _uiState.value.copy(query = newQuery)
        filterResults(newQuery)
    }

    private fun filterResults(q: String) {
        val query = q.trim().lowercase()
        if (query.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                filteredStudents = emptyList(),
                filteredGroups = emptyList(),
                filteredSessions = emptyList(),
                filteredExams = emptyList(),
                filteredPayments = emptyList(),
                totalResultsCount = 0
            )
            return
        }

        val students = _uiState.value.allStudents.filter {
            it.name.lowercase().contains(query) ||
            it.phone.contains(query) ||
            it.parentPhone.contains(query) ||
            it.grade.lowercase().contains(query)
        }

        val groups = _uiState.value.allGroups.filter {
            it.name.lowercase().contains(query) ||
            it.groupNumber.lowercase().contains(query) ||
            it.grade.lowercase().contains(query)
        }

        val sessions = _uiState.value.allSessions.filter {
            it.day.lowercase().contains(query) ||
            it.date.contains(query) ||
            it.location.lowercase().contains(query)
        }

        val exams = _uiState.value.allExams.filter {
            it.title.lowercase().contains(query) ||
            it.date.contains(query)
        }

        val payments = _uiState.value.allPayments.filter {
            it.monthName.lowercase().contains(query) ||
            it.date.contains(query) ||
            it.type.lowercase().contains(query) ||
            it.note.lowercase().contains(query)
        }

        val total = students.size + groups.size + sessions.size + exams.size + payments.size

        _uiState.value = _uiState.value.copy(
            filteredStudents = students,
            filteredGroups = groups,
            filteredSessions = sessions,
            filteredExams = exams,
            filteredPayments = payments,
            totalResultsCount = total
        )
    }
}
