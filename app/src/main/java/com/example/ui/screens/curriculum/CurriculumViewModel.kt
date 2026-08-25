package com.example.ui.screens.curriculum

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CurriculumEntity
import com.example.data.repository.TeacherPlannerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CurriculumViewModel(
    private val repository: TeacherPlannerRepository
) : ViewModel() {

    val allCurriculum = repository.allCurriculum
    val allGroups = repository.allGroups

    private val _selectedGroupId = MutableStateFlow(0L)
    val selectedGroupId: StateFlow<Long> = _selectedGroupId

    val curriculumList = combine(allCurriculum, _selectedGroupId) { items, groupId ->
        if (groupId == 0L) {
            items
        } else {
            items.filter { it.groupId == groupId || it.groupId == 0L }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun setGroup(groupId: Long) {
        _selectedGroupId.value = groupId
    }

    fun toggleLessonCompletion(item: CurriculumEntity) {
        viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val updated = item.copy(
                isCompleted = !item.isCompleted,
                completionDate = if (!item.isCompleted) today else ""
            )
            repository.updateCurriculum(updated)
        }
    }

    fun saveCurriculum(item: CurriculumEntity) {
        viewModelScope.launch {
            if (item.id == 0L) {
                repository.insertCurriculum(item)
            } else {
                repository.updateCurriculum(item)
            }
        }
    }

    fun deleteCurriculum(item: CurriculumEntity) {
        viewModelScope.launch {
            repository.deleteCurriculum(item)
        }
    }
}

class CurriculumViewModelFactory(
    private val repository: TeacherPlannerRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CurriculumViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CurriculumViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
