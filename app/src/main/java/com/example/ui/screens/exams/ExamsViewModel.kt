package com.example.ui.screens.exams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.*
import com.example.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class StudentGradeEntry(
    val student: StudentEntity,
    val gradeId: Long = 0L,
    val scoreText: String = "0",
    val score: Double = 0.0,
    val percentage: Double = 0.0,
    val gradeTitle: String = "مقبول",
    val note: String = ""
)

data class ExamsUiState(
    val exams: List<ExamWithGroup> = emptyList(),
    val groups: List<GroupEntity> = emptyList(),
    val teacher: TeacherEntity? = null,
    val selectedExam: ExamEntity? = null,
    val selectedExamGroup: GroupEntity? = null,
    val studentGradesList: List<StudentGradeEntry> = emptyList(),
    val examAverage: Double = 0.0,
    val highestScore: Double = 0.0,
    val lowestScore: Double = 0.0,
    val passCount: Int = 0,
    val failCount: Int = 0,
    val isLoading: Boolean = false
)

class ExamsViewModel(private val repository: TeacherPlannerRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ExamsUiState())
    val uiState: StateFlow<ExamsUiState> = _uiState.asStateFlow()

    init {
        loadExams()
    }

    private fun loadExams() {
        viewModelScope.launch {
            combine(
                repository.allExams,
                repository.allGroups,
                repository.teacher
            ) { exams, groups, teacher ->
                val groupMap = groups.associateBy { it.id }
                val examList = exams.map { ex ->
                    ExamWithGroup(
                        exam = ex,
                        groupName = groupMap[ex.groupId]?.name ?: "غير محدد"
                    )
                }
                _uiState.value = _uiState.value.copy(
                    exams = examList,
                    groups = groups,
                    teacher = teacher,
                    isLoading = false
                )
            }.collect {
                // state updated
            }
        }
    }

    fun addOrUpdateExam(exam: ExamEntity, onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            if (exam.id == 0L) {
                val newId = repository.insertExam(exam)
                onSaved(newId)
            } else {
                repository.updateExam(exam)
                onSaved(exam.id)
            }
        }
    }

    fun deleteExam(exam: ExamEntity) {
        viewModelScope.launch {
            repository.deleteExam(exam)
        }
    }

    fun loadExamDetails(examId: Long) {
        viewModelScope.launch {
            val exam = repository.allExams.first().firstOrNull { it.id == examId }
            if (exam != null) {
                val group = repository.allGroups.first().firstOrNull { it.id == exam.groupId }
                val students = repository.getStudentsByGroup(exam.groupId).first()
                val existingGrades = repository.getGradesByExam(examId).first().associateBy { it.studentId }

                val gradeEntries = students.map { s ->
                    val gr = existingGrades[s.id]
                    val scoreVal = gr?.score ?: 0.0
                    val maxScore = if (exam.maxScore > 0) exam.maxScore else 100.0
                    val pct = (scoreVal / maxScore) * 100.0
                    val title = when {
                        pct >= 85 -> "ممتاز"
                        pct >= 75 -> "جيد جداً"
                        pct >= 65 -> "جيد"
                        pct >= 50 -> "مقبول"
                        else -> "يحتاج تحسين"
                    }
                    StudentGradeEntry(
                        student = s,
                        gradeId = gr?.id ?: 0L,
                        scoreText = if (gr != null) scoreVal.toString() else "",
                        score = scoreVal,
                        percentage = pct,
                        gradeTitle = title,
                        note = gr?.note ?: ""
                    )
                }

                recalculateStats(exam, gradeEntries)
                _uiState.value = _uiState.value.copy(
                    selectedExam = exam,
                    selectedExamGroup = group,
                    studentGradesList = gradeEntries
                )
            }
        }
    }

    fun updateStudentScore(studentId: Long, scoreText: String) {
        val exam = _uiState.value.selectedExam ?: return
        val maxScore = if (exam.maxScore > 0) exam.maxScore else 100.0
        val numScore = scoreText.toDoubleOrNull() ?: 0.0

        val updated = _uiState.value.studentGradesList.map {
            if (it.student.id == studentId) {
                val pct = (numScore / maxScore) * 100.0
                val title = when {
                    pct >= 85 -> "ممتاز"
                    pct >= 75 -> "جيد جداً"
                    pct >= 65 -> "جيد"
                    pct >= 50 -> "مقبول"
                    else -> "يحتاج تحسين"
                }
                it.copy(
                    scoreText = scoreText,
                    score = numScore,
                    percentage = pct,
                    gradeTitle = title
                )
            } else it
        }

        recalculateStats(exam, updated)
        _uiState.value = _uiState.value.copy(studentGradesList = updated)
    }

    private fun recalculateStats(exam: ExamEntity, list: List<StudentGradeEntry>) {
        if (list.isEmpty()) return
        val scores = list.map { it.score }
        val avg = scores.average()
        val highest = scores.maxOrNull() ?: 0.0
        val lowest = scores.minOrNull() ?: 0.0
        val pass = list.count { it.score >= exam.passScore }
        val fail = list.count { it.score < exam.passScore }

        _uiState.value = _uiState.value.copy(
            examAverage = avg,
            highestScore = highest,
            lowestScore = lowest,
            passCount = pass,
            failCount = fail
        )
    }

    fun saveAllGrades(onSuccess: () -> Unit) {
        val exam = _uiState.value.selectedExam ?: return
        viewModelScope.launch {
            val entities = _uiState.value.studentGradesList.map {
                ExamGradeEntity(
                    id = it.gradeId,
                    examId = exam.id,
                    studentId = it.student.id,
                    score = it.score,
                    note = it.note
                )
            }
            repository.insertAllGrades(entities)
            onSuccess()
        }
    }
}
