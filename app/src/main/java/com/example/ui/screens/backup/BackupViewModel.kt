package com.example.ui.screens.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupManager
import com.example.data.backup.BackupMetadata
import com.example.data.backup.MigrationPreview
import com.example.data.backup.OldAppMigrationManager
import com.example.data.repository.TeacherPlannerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

data class BackupUiState(
    val isLoading: Boolean = false,
    val backupJson: String? = null,
    val backupFile: File? = null,
    val previewMetadata: BackupMetadata? = null,
    val oldAppPreview: MigrationPreview? = null,
    val isRestoring: Boolean = false,
    val isMigrating: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
)

class BackupViewModel(
    private val repository: TeacherPlannerRepository,
    private val backupManager: BackupManager,
    private val oldAppMigrationManager: OldAppMigrationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun createBackup(context: Context, onShare: (File) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, message = null)
            try {
                val json = backupManager.createBackupJson()
                val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val cacheDir = File(context.cacheDir, "backups")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                val backupFile = File(cacheDir, "TeacherPlannerPro_Backup_$dateStr.json")
                FileOutputStream(backupFile).use { fos ->
                    fos.write(json.toByteArray())
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    backupJson = json,
                    backupFile = backupFile,
                    message = "تم إنشاء النسخة الاحتياطية بنجاح"
                )
                onShare(backupFile)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isError = true,
                    message = "فشل إنشاء النسخة: ${e.localizedMessage}"
                )
            }
        }
    }

    fun inspectBackupFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).readText()
                } ?: ""
                val metadata = backupManager.inspectBackup(content)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    backupJson = content,
                    previewMetadata = metadata
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isError = true,
                    message = "فشل قراءة الملف: ${e.localizedMessage}"
                )
            }
        }
    }

    fun restoreBackup() {
        val json = _uiState.value.backupJson ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRestoring = true)
            val success = backupManager.restoreBackup(json)
            _uiState.value = _uiState.value.copy(
                isRestoring = false,
                isError = !success,
                previewMetadata = null,
                backupJson = null,
                message = if (success) "تم استعادة البيانات بنجاح" else "فشلت عملية الاستعادة"
            )
        }
    }

    fun inspectOldAppBackupFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).readText()
                } ?: ""
                val preview = oldAppMigrationManager.parseAndPreviewOldBackup(content)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    oldAppPreview = preview
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isError = true,
                    message = "فشل قراءة ملف النسخة السابقة: ${e.localizedMessage}"
                )
            }
        }
    }

    fun executeOldAppMigration() {
        val preview = _uiState.value.oldAppPreview ?: return
        val parsedData = preview.parsedData ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isMigrating = true)
            val result = oldAppMigrationManager.executeMigration(parsedData)
            _uiState.value = _uiState.value.copy(
                isMigrating = false,
                oldAppPreview = null,
                isError = !result.success,
                message = if (result.success) "تم ترحيل البيانات القديمة بنجاح (${result.importedStudents} طالب، ${result.importedGroups} مجموعة)" else result.errorMessage
            )
        }
    }

    fun shareBackup(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "نسخة احتياطية - هاكر التدريس")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "حفظ أو إرسال النسخة الاحتياطية"))
    }

    fun sendBackupToTelegram(context: Context, file: File, telegramUserOrPhone: String?) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val dateFormatted = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date())
            val textCaption = "📦 *نسخة احتياطية شاملة لقاعدة بيانات المعلم - هاكر التدريس (The Hacker)*\n" +
                    "📅 التاريخ: $dateFormatted\n" +
                    "🔐 صيغة الملف: JSON آمنة ومشفرة\n" +
                    (if (!telegramUserOrPhone.isNullOrBlank()) "👤 رقم/معرف المعلم: $telegramUserOrPhone" else "")

            val telegramIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, textCaption)
                putExtra(Intent.EXTRA_SUBJECT, "نسخة احتياطية - هاكر التدريس")
                `package` = "org.telegram.messenger"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(telegramIntent)
            com.example.util.AppPreferencesManager.recordBackupPerformed()
        } catch (e: Exception) {
            // Telegram app package not found, open general chooser with Telegram pre-filter or share sheet
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val dateFormatted = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date())
            val textCaption = "📦 نسخة احتياطية - هاكر التدريس ($dateFormatted)"
            val chooserIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, textCaption)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(chooserIntent, "إرسال النسخة الاحتياطية إلى تليجرام"))
            com.example.util.AppPreferencesManager.recordBackupPerformed()
        }
    }

    fun createAndSendToTelegram(context: Context, telegramUserOrPhone: String?) {
        createBackup(context) { file ->
            sendBackupToTelegram(context, file, telegramUserOrPhone)
        }
    }

    fun populateSampleData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.populateSampleData()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                message = "تمت إضافة البيانات التجريبية بنجاح"
            )
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.clearAllData()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                message = "تم مسح جميع بيانات التطبيق"
            )
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, isError = false)
    }
}
