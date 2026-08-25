package com.example.ui.screens.reports

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.export.ExcelExporter
import com.example.data.export.PdfReportExporter
import com.example.data.local.entity.*
import com.example.data.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class ReportsUiState(
    val teacher: TeacherEntity? = null,
    val students: List<StudentEntity> = emptyList(),
    val groups: List<GroupEntity> = emptyList(),
    val selectedStudentId: Long = 0L,
    val selectedGroupId: Long = 0L,
    val reportSetting: ReportSettingEntity = ReportSettingEntity(),
    val selectedStudentDetails: StudentWithDetails? = null,
    val selectedStudentGrades: List<StudentGradeItem> = emptyList(),
    val generatedPdfFile: File? = null,
    val generatedCsvFile: File? = null,
    val isExporting: Boolean = false,
    val successMessage: String? = null
)

class ReportsViewModel(
    private val repository: TeacherPlannerRepository,
    private val pdfExporter: PdfReportExporter,
    private val excelExporter: ExcelExporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                repository.teacher,
                repository.allStudents,
                repository.allGroups,
                repository.reportSettings
            ) { teacher, students, groups, setting ->
                val sId = if (_uiState.value.selectedStudentId == 0L && students.isNotEmpty()) students.first().id else _uiState.value.selectedStudentId
                val gId = if (_uiState.value.selectedGroupId == 0L && groups.isNotEmpty()) groups.first().id else _uiState.value.selectedGroupId
                _uiState.value = _uiState.value.copy(
                    teacher = teacher,
                    students = students,
                    groups = groups,
                    selectedStudentId = sId,
                    selectedGroupId = gId,
                    reportSetting = setting ?: ReportSettingEntity()
                )
                if (sId != 0L) {
                    loadStudentForReport(sId)
                }
            }.collect {}
        }
    }

    fun selectStudent(studentId: Long) {
        _uiState.value = _uiState.value.copy(selectedStudentId = studentId)
        loadStudentForReport(studentId)
    }

    fun selectGroup(groupId: Long) {
        _uiState.value = _uiState.value.copy(selectedGroupId = groupId)
    }

    private fun loadStudentForReport(studentId: Long) {
        viewModelScope.launch {
            val details = repository.getStudentDetails(studentId)
            val allExams = repository.allExams.first()
            val examMap = allExams.associateBy { it.id }
            val gradesList = repository.getGradesByExam(0).first()
            val gradeItems = gradesList.filter { it.studentId == studentId }.map { gr ->
                val ex = examMap[gr.examId]
                val max = ex?.maxScore ?: 100.0
                val pct = if (max > 0) (gr.score / max) * 100.0 else 0.0
                val title = when {
                    pct >= 85 -> "ممتاز"
                    pct >= 75 -> "جيد جداً"
                    pct >= 65 -> "جيد"
                    pct >= 50 -> "مقبول"
                    else -> "يحتاج تحسين"
                }
                StudentGradeItem(
                    student = details?.student ?: StudentEntity(name = ""),
                    grade = gr,
                    score = gr.score,
                    maxScore = max,
                    percentage = pct,
                    gradeTitle = title
                )
            }

            _uiState.value = _uiState.value.copy(
                selectedStudentDetails = details,
                selectedStudentGrades = gradeItems
            )
        }
    }

    fun updateReportSetting(newSetting: ReportSettingEntity) {
        viewModelScope.launch {
            repository.updateReportSettings(newSetting)
            _uiState.value = _uiState.value.copy(reportSetting = newSetting)
        }
    }

    fun generatePdfReport(context: Context, onReady: (File) -> Unit) {
        val details = _uiState.value.selectedStudentDetails ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            val file = withContext(Dispatchers.IO) {
                pdfExporter.generateStudentReportPdf(
                    context = context,
                    teacher = _uiState.value.teacher,
                    studentDetails = details,
                    grades = _uiState.value.selectedStudentGrades,
                    settings = _uiState.value.reportSetting
                )
            }
            _uiState.value = _uiState.value.copy(generatedPdfFile = file, isExporting = false, successMessage = "تم إنشاء التقرير الفردي بنجاح")
            onReady(file)
        }
    }

    /**
     * Generates a multi-page dossier report for an entire group at the same time.
     */
    fun generateGroupDossierBatch(context: Context, groupId: Long, onReady: (File) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            val targetStudents = if (groupId == 0L) _uiState.value.students else _uiState.value.students.filter { it.groupId == groupId }
            val group = _uiState.value.groups.firstOrNull { it.id == groupId }
            val allExams = repository.allExams.first()
            val examMap = allExams.associateBy { it.id }
            val allGrades = repository.getGradesByExam(0).first()

            val pairList = targetStudents.map { student ->
                val details = repository.getStudentDetails(student.id) ?: StudentWithDetails(
                    student = student,
                    group = group,
                    totalPaid = 0.0,
                    totalRequired = 0.0,
                    remainingBalance = 0.0,
                    attendanceRate = 100,
                    averageScore = 0.0,
                    lastExamScore = "-",
                    lastPaymentDate = "-"
                )
                val sGrades = allGrades.filter { it.studentId == student.id }.map { gr ->
                    val ex = examMap[gr.examId]
                    val max = ex?.maxScore ?: 100.0
                    val pct = if (max > 0) (gr.score / max) * 100.0 else 0.0
                    val title = if (pct >= 85) "ممتاز" else if (pct >= 75) "جيد جداً" else if (pct >= 65) "جيد" else if (pct >= 50) "مقبول" else "يحتاج تحسين"
                    StudentGradeItem(student, gr, gr.score, max, pct, title)
                }
                Pair(details, sGrades)
            }

            val file = withContext(Dispatchers.IO) {
                pdfExporter.generateBatchStudentReportsPdf(
                    context = context,
                    teacher = _uiState.value.teacher,
                    group = group,
                    studentListWithDetails = pairList,
                    settings = _uiState.value.reportSetting
                )
            }

            _uiState.value = _uiState.value.copy(generatedPdfFile = file, isExporting = false, successMessage = "تم إنشاء الملف التجميعي الشامل بنجاح")
            onReady(file)
        }
    }

    /**
     * Generates a matrix table sheet for group exam grades.
     */
    fun generateGradeMatrixSheet(context: Context, groupId: Long, onReady: (File) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            val targetStudents = if (groupId == 0L) _uiState.value.students else _uiState.value.students.filter { it.groupId == groupId }
            val group = _uiState.value.groups.firstOrNull { it.id == groupId }
            val groupExams = if (groupId == 0L) repository.allExams.first() else repository.getExamsByGroup(groupId).first()
            val allGrades = repository.getGradesByExam(0).first()

            val studentGrades = targetStudents.map { student ->
                val map = allGrades.filter { it.studentId == student.id }.associate { it.examId to it.score }
                Pair(student.name, map)
            }

            val file = withContext(Dispatchers.IO) {
                pdfExporter.generateGroupGradeSheetPdf(
                    context = context,
                    teacher = _uiState.value.teacher,
                    group = group,
                    exams = groupExams,
                    studentsGrades = studentGrades
                )
            }

            _uiState.value = _uiState.value.copy(generatedPdfFile = file, isExporting = false, successMessage = "تم إنشاء كشف الدرجات المجمع بنجاح")
            onReady(file)
        }
    }

    fun generateExcelExport(context: Context, onReady: (File) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            val file = withContext(Dispatchers.IO) {
                excelExporter.generateComprehensiveCsv(context)
            }
            _uiState.value = _uiState.value.copy(generatedCsvFile = file, isExporting = false, successMessage = "تم تصدير ملف Excel الشامل بنجاح")
            onReady(file)
        }
    }
}
