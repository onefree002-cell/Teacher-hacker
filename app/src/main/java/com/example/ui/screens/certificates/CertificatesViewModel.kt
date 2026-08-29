package com.example.ui.screens.certificates

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.export.PdfReportExporter
import com.example.data.local.entity.CertificateSettingEntity
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.StudentEntity
import com.example.data.local.entity.TeacherEntity
import com.example.data.repository.TeacherPlannerRepository
import com.example.util.ImageBackgroundRemover
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class CertificatesUiState(
    val teacher: TeacherEntity? = null,
    val students: List<StudentEntity> = emptyList(),
    val groups: List<GroupEntity> = emptyList(),
    val selectedStudentId: Long = 0L,
    val selectedStudentName: String = "",
    val selectedGender: String = "boy",
    val selectedGroupId: Long = 0L,
    val selectedGroupName: String = "",
    val setting: CertificateSettingEntity = CertificateSettingEntity(),
    val generatedPdf: File? = null,
    val isGenerating: Boolean = false,
    val isProcessingLogo: Boolean = false,
    val statusMessage: String? = null
)

class CertificatesViewModel(
    private val repository: TeacherPlannerRepository,
    private val pdfExporter: PdfReportExporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(CertificatesUiState())
    val uiState: StateFlow<CertificatesUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                repository.teacher,
                repository.allStudents,
                repository.allGroups,
                repository.certificateSettings
            ) { teacher, students, groups, setting ->
                val sId = if (_uiState.value.selectedStudentId == 0L && students.isNotEmpty()) students.first().id else _uiState.value.selectedStudentId
                val student = students.firstOrNull { it.id == sId }
                val group = groups.firstOrNull { it.id == student?.groupId }
                _uiState.value = _uiState.value.copy(
                    teacher = teacher,
                    students = students,
                    groups = groups,
                    selectedStudentId = sId,
                    selectedStudentName = if (_uiState.value.selectedStudentName.isBlank()) (student?.name ?: "") else _uiState.value.selectedStudentName,
                    selectedGender = student?.gender ?: _uiState.value.selectedGender,
                    selectedGroupId = group?.id ?: _uiState.value.selectedGroupId,
                    selectedGroupName = group?.name ?: _uiState.value.selectedGroupName,
                    setting = setting ?: CertificateSettingEntity(signatureName = teacher?.name ?: "أستاذ المادة")
                )
            }.collect {}
        }
    }

    fun setStudentName(name: String) {
        _uiState.value = _uiState.value.copy(selectedStudentName = name)
    }

    fun setGender(gender: String) {
        _uiState.value = _uiState.value.copy(selectedGender = gender)
    }

    fun selectStudent(studentId: Long) {
        val student = _uiState.value.students.firstOrNull { it.id == studentId }
        val group = _uiState.value.groups.firstOrNull { it.id == student?.groupId }
        _uiState.value = _uiState.value.copy(
            selectedStudentId = studentId,
            selectedStudentName = student?.name ?: "",
            selectedGender = student?.gender ?: "boy",
            selectedGroupId = group?.id ?: 0L,
            selectedGroupName = group?.name ?: ""
        )
    }

    fun selectGroupForBatch(groupId: Long) {
        val group = _uiState.value.groups.firstOrNull { it.id == groupId }
        _uiState.value = _uiState.value.copy(
            selectedGroupId = groupId,
            selectedGroupName = group?.name ?: ""
        )
    }

    fun setThemeTemplate(templateId: String) {
        val updated = _uiState.value.setting.copy(templateId = templateId)
        updateSetting(updated)
    }

    fun setPresetLogo(preset: String) {
        val updated = _uiState.value.setting.copy(presetLogo = preset, logoUri = if (preset == "custom") _uiState.value.setting.logoUri else null)
        updateSetting(updated)
    }

    fun updateSetting(newSetting: CertificateSettingEntity) {
        viewModelScope.launch {
            repository.updateCertificateSettings(newSetting)
            _uiState.value = _uiState.value.copy(setting = newSetting)
        }
    }

    fun setLogoImage(context: Context, uri: Uri, removeBackground: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessingLogo = true)
            val processedFile = withContext(Dispatchers.IO) {
                if (removeBackground) {
                    ImageBackgroundRemover.processAndSaveTransparentLogo(context, uri)
                } else {
                    // Copy raw
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val dir = File(context.filesDir, "logos")
                    if (!dir.exists()) dir.mkdirs()
                    val outFile = File(dir, "logo_raw_${System.currentTimeMillis()}.png")
                    inputStream?.use { input ->
                        outFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    outFile
                }
            }

            if (processedFile != null) {
                val updated = _uiState.value.setting.copy(
                    logoUri = processedFile.absolutePath,
                    presetLogo = "custom",
                    removeLogoBackground = removeBackground
                )
                updateSetting(updated)
                _uiState.value = _uiState.value.copy(
                    isProcessingLogo = false,
                    statusMessage = if (removeBackground) "تم تحميل اللوجو وإزالة الخلفية بنجاح" else "تم تحميل اللوجو بنجاح"
                )
            } else {
                _uiState.value = _uiState.value.copy(isProcessingLogo = false, statusMessage = "تعذر معالجة اللوجو")
            }
        }
    }

    fun clearLogo() {
        val updated = _uiState.value.setting.copy(logoUri = null, presetLogo = "crown")
        updateSetting(updated)
    }

    fun generateAndShareCertificate(context: Context) {
        val studentName = _uiState.value.selectedStudentName.ifEmpty { "طالب متميز" }
        val gender = _uiState.value.selectedGender
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true)
            val file = pdfExporter.generateCertificatePdf(
                context = context,
                studentName = studentName,
                groupName = _uiState.value.selectedGroupName,
                teacher = _uiState.value.teacher,
                settings = _uiState.value.setting,
                gender = gender
            )
            _uiState.value = _uiState.value.copy(generatedPdf = file, isGenerating = false)
            pdfExporter.sharePdf(context, file, "شهادة تقدير - $studentName")
        }
    }

    fun generateBatchCertificatesForGroup(context: Context, groupId: Long, onComplete: (File) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true)
            val students = if (groupId == 0L) _uiState.value.students else _uiState.value.students.filter { it.groupId == groupId }
            val group = _uiState.value.groups.firstOrNull { it.id == groupId }
            val groupName = group?.name ?: "المجموعة"

            val list = students.map { Pair(it.name, groupName) }
            val genderMap = students.associate { it.name to it.gender }
            val file = withContext(Dispatchers.IO) {
                pdfExporter.generateBatchCertificatesPdf(
                    context = context,
                    teacher = _uiState.value.teacher,
                    studentsWithGroup = list,
                    settings = _uiState.value.setting,
                    genderMap = genderMap
                )
            }
            _uiState.value = _uiState.value.copy(isGenerating = false, generatedPdf = file)
            onComplete(file)
        }
    }
}
