package com.example.ui.screens.questions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.QuestionEntity
import com.example.data.repository.TeacherPlannerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class QuestionBankViewModel(
    private val repository: TeacherPlannerRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedDifficulty = MutableStateFlow("الكل")
    val selectedDifficulty: StateFlow<String> = _selectedDifficulty

    private val _selectedType = MutableStateFlow("الكل")
    val selectedType: StateFlow<String> = _selectedType

    val allQuestions = repository.allQuestions
    val teacher = repository.teacher

    val filteredQuestions = combine(
        allQuestions,
        _searchQuery,
        _selectedDifficulty,
        _selectedType
    ) { questions, query, diff, type ->
        questions.filter { q ->
            val matchesQuery = query.isEmpty() ||
                    q.questionText.contains(query, ignoreCase = true) ||
                    q.unitLesson.contains(query, ignoreCase = true) ||
                    q.subject.contains(query, ignoreCase = true)
            val matchesDiff = diff == "الكل" || q.difficulty == diff
            val matchesType = type == "الكل" || when (type) {
                "اختيار من متعدد" -> q.questionType == "mcq"
                "صح أو خطأ" -> q.questionType == "true_false"
                "مقالي" -> q.questionType == "essay"
                else -> true
            }
            matchesQuery && matchesDiff && matchesType
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setDifficulty(difficulty: String) {
        _selectedDifficulty.value = difficulty
    }

    fun setType(type: String) {
        _selectedType.value = type
    }

    fun saveQuestion(question: QuestionEntity) {
        viewModelScope.launch {
            if (question.id == 0L) {
                repository.insertQuestion(question)
            } else {
                repository.updateQuestion(question)
            }
        }
    }

    fun deleteQuestion(question: QuestionEntity) {
        viewModelScope.launch {
            repository.deleteQuestion(question)
        }
    }
}

class QuestionBankViewModelFactory(
    private val repository: TeacherPlannerRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QuestionBankViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return QuestionBankViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
