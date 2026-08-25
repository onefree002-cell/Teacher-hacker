package com.example.ui.screens.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.TeacherEntity
import com.example.data.repository.TeacherPlannerRepository
import com.example.utils.ImageUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val teacher: TeacherEntity = TeacherEntity(),
    val isSaved: Boolean = false,
    val isProcessingLogo: Boolean = false,
    val logoStatusMessage: String? = null
)

class ProfileViewModel(private val repository: TeacherPlannerRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.teacher.collect { t ->
                _uiState.value = _uiState.value.copy(teacher = t ?: TeacherEntity())
            }
        }
    }

    fun updateField(
        name: String? = null,
        title: String? = null,
        subject: String? = null,
        phone: String? = null,
        whatsapp: String? = null,
        centerName: String? = null,
        address: String? = null,
        notes: String? = null,
        bio: String? = null,
        experienceYears: String? = null,
        degrees: String? = null,
        stagesTaught: String? = null,
        teachingFeatures: String? = null,
        logoUri: String? = null,
        showLogoInPrintouts: Boolean? = null
    ) {
        val cur = _uiState.value.teacher
        _uiState.value = _uiState.value.copy(
            teacher = cur.copy(
                name = name ?: cur.name,
                title = title ?: cur.title,
                subject = subject ?: cur.subject,
                phone = phone ?: cur.phone,
                whatsapp = whatsapp ?: cur.whatsapp,
                centerName = centerName ?: cur.centerName,
                address = address ?: cur.address,
                notes = notes ?: cur.notes,
                bio = bio ?: cur.bio,
                experienceYears = experienceYears ?: cur.experienceYears,
                degrees = degrees ?: cur.degrees,
                stagesTaught = stagesTaught ?: cur.stagesTaught,
                teachingFeatures = teachingFeatures ?: cur.teachingFeatures,
                logoUri = if (logoUri != null) (if (logoUri.isEmpty()) null else logoUri) else cur.logoUri,
                showLogoInPrintouts = showLogoInPrintouts ?: cur.showLogoInPrintouts
            )
        )
    }

    fun removeLogoBackground(context: Context) {
        val currentUriStr = _uiState.value.teacher.logoUri ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessingLogo = true)
            try {
                val inputUri = Uri.parse(currentUriStr)
                val transparentUri = ImageUtils.removeBackground(context, inputUri)
                if (transparentUri != null) {
                    val updated = _uiState.value.teacher.copy(logoUri = transparentUri.toString())
                    _uiState.value = _uiState.value.copy(
                        teacher = updated,
                        isProcessingLogo = false,
                        logoStatusMessage = "تم حذف خلفية اللوجو وجعله شفافاً بنجاح"
                    )
                    repository.updateTeacher(updated)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isProcessingLogo = false,
                        logoStatusMessage = "تعذر تفريغ الخلفية من هذه الصورة"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessingLogo = false,
                    logoStatusMessage = "حدث خطأ أثناء معالجة الصورة: ${e.localizedMessage}"
                )
            }
        }
    }

    fun clearLogoStatusMessage() {
        _uiState.value = _uiState.value.copy(logoStatusMessage = null)
    }

    fun saveProfile() {
        viewModelScope.launch {
            repository.updateTeacher(_uiState.value.teacher)
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }

    fun resetSaved() {
        _uiState.value = _uiState.value.copy(isSaved = false)
    }
}
