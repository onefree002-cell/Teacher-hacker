package com.example.ui.screens.smartprep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.LessonPlanEntity
import com.example.data.local.entity.TeacherEntity
import com.example.data.repository.TeacherPlannerRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class SmartPrepUiState(
    val plans: List<LessonPlanEntity> = emptyList(),
    val filteredPlans: List<LessonPlanEntity> = emptyList(),
    val groups: List<GroupEntity> = emptyList(),
    val teacher: TeacherEntity? = null,
    val searchQuery: String = "",
    val selectedGrade: String? = null,
    val selectedSubject: String? = null,
    val isLoading: Boolean = false
)

private data class FilterParams(
    val query: String = "",
    val grade: String? = null,
    val subject: String? = null
)

class SmartPrepViewModel(
    private val repository: TeacherPlannerRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedGrade = MutableStateFlow<String?>(null)
    private val _selectedSubject = MutableStateFlow<String?>(null)

    private val _filterParams = combine(_searchQuery, _selectedGrade, _selectedSubject) { query, grade, subject ->
        FilterParams(query, grade, subject)
    }

    val uiState: StateFlow<SmartPrepUiState> = combine(
        repository.allLessonPlans,
        repository.allGroups,
        repository.teacher,
        _filterParams
    ) { plans, groups, teacher, filter ->
        val query = filter.query
        val grade = filter.grade
        val subject = filter.subject

        val filtered = plans.filter { plan ->
            val matchesQuery = query.isBlank() ||
                    plan.title.contains(query, ignoreCase = true) ||
                    plan.objectives.contains(query, ignoreCase = true) ||
                    plan.keyPoints.contains(query, ignoreCase = true) ||
                    plan.subject.contains(query, ignoreCase = true) ||
                    plan.homework.contains(query, ignoreCase = true)

            val matchesGrade = grade == null || plan.grade == grade
            val matchesSubject = subject == null || plan.subject == subject

            matchesQuery && matchesGrade && matchesSubject
        }

        SmartPrepUiState(
            plans = plans,
            filteredPlans = filtered,
            groups = groups,
            teacher = teacher,
            searchQuery = query,
            selectedGrade = grade,
            selectedSubject = subject,
            isLoading = false
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SmartPrepUiState(isLoading = true)
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedGrade(grade: String?) {
        _selectedGrade.value = grade
    }

    fun setSelectedSubject(subject: String?) {
        _selectedSubject.value = subject
    }

    fun savePlan(plan: LessonPlanEntity) {
        viewModelScope.launch {
            repository.saveLessonPlan(plan)
        }
    }

    fun deletePlan(plan: LessonPlanEntity) {
        viewModelScope.launch {
            repository.deleteLessonPlan(plan)
        }
    }

    fun toggleCompleted(plan: LessonPlanEntity) {
        viewModelScope.launch {
            repository.saveLessonPlan(plan.copy(isCompleted = !plan.isCompleted))
        }
    }

    /**
     * AI-Powered Lesson Plan Generator using Gemini AI with fallback.
     */
    suspend fun generateAiPlan(
        subject: String,
        grade: String,
        topic: String,
        durationMinutes: Int = 60,
        extraNotes: String = ""
    ): LessonPlanEntity {
        return com.example.util.AiLessonPlannerService.generateLessonPlan(
            subject = subject,
            grade = grade,
            topic = topic,
            durationMinutes = durationMinutes,
            extraNotes = extraNotes
        )
    }

    /**
     * Smart generator that builds an organized, pedagogical lesson preparation
     * template based on subject, grade level, and lesson topic.
     */
    fun generateSmartTemplate(subject: String, grade: String, topic: String): LessonPlanEntity {
        return com.example.util.AiLessonPlannerService.generatePedagogicalFallback(
            subject = subject,
            grade = grade,
            topic = topic
        )
    }
}
