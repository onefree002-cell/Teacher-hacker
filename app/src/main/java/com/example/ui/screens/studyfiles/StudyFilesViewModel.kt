package com.example.ui.screens.studyfiles

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.StudyFileEntity
import com.example.data.repository.TeacherPlannerRepository
import com.example.util.StudyFileManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class StudyFilesUiState(
    val files: List<StudyFileEntity> = emptyList(),
    val filteredFiles: List<StudyFileEntity> = emptyList(),
    val selectedGrade: String = "الكل",
    val selectedCategory: String = "الكل",
    val searchQuery: String = "",
    val availableGrades: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val statusMessage: String? = null
)

class StudyFilesViewModel(
    private val repository: TeacherPlannerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudyFilesUiState())
    val uiState: StateFlow<StudyFilesUiState> = _uiState.asStateFlow()

    init {
        loadFiles()
        loadGradesFromGroups()
    }

    private fun loadFiles() {
        viewModelScope.launch {
            repository.getAllStudyFiles().collect { filesList ->
                _uiState.update { state ->
                    val filtered = applyFilter(filesList, state.selectedGrade, state.selectedCategory, state.searchQuery)
                    state.copy(files = filesList, filteredFiles = filtered)
                }
            }
        }
    }

    private fun loadGradesFromGroups() {
        viewModelScope.launch {
            repository.allGroups.collect { groups ->
                val groupGrades = groups.map { it.grade }.filter { it.isNotBlank() }.distinct()
                val defaultGrades = listOf(
                    "الصف الأول الابتدائي",
                    "الصف الثاني الابتدائي",
                    "الصف الثالث الابتدائي",
                    "الصف الرابع الابتدائي",
                    "الصف الخامس الابتدائي",
                    "الصف السادس الابتدائي",
                    "الصف الأول الإعدادي",
                    "الصف الثاني الإعدادي",
                    "الصف الثالث الإعدادي",
                    "الصف الأول الثانوي",
                    "الصف الثاني الثانوي",
                    "الصف الثالث الثانوي"
                )
                val allGrades = (defaultGrades + groupGrades).distinct()
                _uiState.update { it.copy(availableGrades = allGrades) }
            }
        }
    }

    fun selectGrade(grade: String) {
        _uiState.update { state ->
            val filtered = applyFilter(state.files, grade, state.selectedCategory, state.searchQuery)
            state.copy(selectedGrade = grade, filteredFiles = filtered)
        }
    }

    fun selectCategory(category: String) {
        _uiState.update { state ->
            val filtered = applyFilter(state.files, state.selectedGrade, category, state.searchQuery)
            state.copy(selectedCategory = category, filteredFiles = filtered)
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { state ->
            val filtered = applyFilter(state.files, state.selectedGrade, state.selectedCategory, query)
            state.copy(searchQuery = query, filteredFiles = filtered)
        }
    }

    private fun applyFilter(
        list: List<StudyFileEntity>,
        grade: String,
        category: String,
        query: String
    ): List<StudyFileEntity> {
        return list.filter { file ->
            val matchGrade = (grade == "الكل" || file.grade.contains(grade, ignoreCase = true) || grade.contains(file.grade, ignoreCase = true))
            val matchCat = (category == "الكل" || file.category == category)
            val matchQuery = query.isBlank() || file.title.contains(query, ignoreCase = true) || file.notes.contains(query, ignoreCase = true) || file.category.contains(query, ignoreCase = true)
            matchGrade && matchCat && matchQuery
        }
    }

    fun addFileFromUri(
        context: Context,
        uri: Uri,
        title: String,
        grade: String,
        category: String,
        notes: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val fileEntity = StudyFileManager.saveUriToInternalStorage(
                    context = context,
                    uri = uri,
                    customTitle = title,
                    grade = grade,
                    category = category,
                    notes = notes
                )
                repository.insertStudyFile(fileEntity)
                _uiState.update { it.copy(isLoading = false, statusMessage = "تم حفظ الملف بنجاح في مكتبة الصف 📚") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, statusMessage = "تعذر حفظ الملف: ${e.localizedMessage}") }
            }
        }
    }

    fun generateSamplePdf(
        context: Context,
        title: String,
        grade: String,
        category: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val fileEntity = StudyFileManager.createSampleStudyPdf(
                    context = context,
                    title = title,
                    grade = grade,
                    category = category,
                    subject = "الرياضيات والعلوم"
                )
                repository.insertStudyFile(fileEntity)
                _uiState.update { it.copy(isLoading = false, statusMessage = "تم إنشاء نموذج المذكرة التفاعلية بنجاح 📄") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, statusMessage = "حدث خطأ: ${e.localizedMessage}") }
            }
        }
    }

    fun deleteFile(file: StudyFileEntity) {
        viewModelScope.launch {
            repository.deleteStudyFile(file)
            _uiState.update { it.copy(statusMessage = "تم حذف الملف بنجاح 🗑️") }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
