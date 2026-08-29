package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.StudentEntity
import com.example.ui.screens.attendance.StudentAttendanceState
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Unified File & Folder Architecture for Teacher Hacker (هاكر التدريس).
 * Manages saving all backups, session directories, PDFs, and homework in:
 * DOCUMENTS / TEACHER HACKER / [YYYY-MM-DD - اسم المجموعة]
 */
object TeacherHackerDirectoryManager {

    const val ROOT_FOLDER_NAME = "TEACHER HACKER"
    const val FOLDER_BACKUPS = "النسخ_الاحتياطية_Backups"
    const val FOLDER_SESSIONS = "حصص_المجموعات"
    const val FOLDER_REPORTS_PDF = "تقارير_وكارنيهات_PDF"
    const val FOLDER_CERTIFICATES = "شهادات_التقدير_Certificates"
    const val FOLDER_STUDY_FILES = "الكتب_والمذكرات_StudyFiles"
    const val FOLDER_HOMEWORK_SCANS = "تصوير_الواجب_Scans"

    /**
     * Certificates Directory: DOCUMENTS / TEACHER HACKER / شهادات_التقدير_Certificates
     */
    fun getCertificatesDir(context: Context): File {
        val root = getTeacherHackerRootDir(context)
        val dir = File(root, FOLDER_CERTIFICATES)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Primary root directory: DOCUMENTS / TEACHER HACKER
     * Auto-creates the folder if not already existing.
     */
    fun getTeacherHackerRootDir(context: Context): File {
        var baseDir: File? = null
        try {
            val publicDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (publicDocs != null) {
                val dir = File(publicDocs, ROOT_FOLDER_NAME)
                if (dir.exists() || dir.mkdirs()) {
                    baseDir = dir
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (baseDir == null || !baseDir.canWrite()) {
            val extDocs = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            val dir = File(extDocs ?: context.filesDir, ROOT_FOLDER_NAME)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            baseDir = dir
        }

        // Also ensure app-scoped folder is synced
        try {
            val appScopedDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), ROOT_FOLDER_NAME)
            if (!appScopedDir.exists()) {
                appScopedDir.mkdirs()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return baseDir
    }

    /**
     * Returns human-readable display path for the user UI
     */
    fun getDisplayRootPath(context: Context): String {
        val root = getTeacherHackerRootDir(context)
        return "Documents / $ROOT_FOLDER_NAME (${root.absolutePath})"
    }

    /**
     * Backups directory: DOCUMENTS / TEACHER HACKER / النسخ_الاحتياطية_Backups
     */
    fun getBackupsDir(context: Context): File {
        val root = getTeacherHackerRootDir(context)
        val dir = File(root, FOLDER_BACKUPS)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Session Directory: DOCUMENTS / TEACHER HACKER / [YYYY-MM-DD - اسم المجموعة]
     * Auto-created if not already existing.
     */
    fun getSessionDir(context: Context, groupName: String, date: String): File {
        val cleanDate = if (date.isNotBlank()) date.trim().replace("/", "-") else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val cleanGroup = if (groupName.isNotBlank()) groupName.trim().replace("/", "-").replace(":", "-") else "مجموعة_عامة"
        val folderName = "$cleanDate - $cleanGroup"

        val root = getTeacherHackerRootDir(context)
        val sessionDir = File(root, folderName)
        if (!sessionDir.exists()) {
            sessionDir.mkdirs()
        }

        // Also ensure sub-archive inside FOLDER_SESSIONS exists
        try {
            val subGroupDir = File(File(root, FOLDER_SESSIONS), cleanGroup)
            if (!subGroupDir.exists()) subGroupDir.mkdirs()
            val nestedSessionDir = File(subGroupDir, folderName)
            if (!nestedSessionDir.exists()) nestedSessionDir.mkdirs()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return sessionDir
    }

    /**
     * PDF Exports Directory: DOCUMENTS / TEACHER HACKER / تقارير_وكارنيهات_PDF
     */
    fun getReportsPdfDir(context: Context): File {
        val root = getTeacherHackerRootDir(context)
        val dir = File(root, FOLDER_REPORTS_PDF)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Study Files Directory: DOCUMENTS / TEACHER HACKER / الكتب_والمذكرات_StudyFiles
     */
    fun getStudyFilesDir(context: Context): File {
        val root = getTeacherHackerRootDir(context)
        val dir = File(root, FOLDER_STUDY_FILES)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Homework Scans Directory: DOCUMENTS / TEACHER HACKER / تصوير_الواجب_Scans
     */
    fun getHomeworkScansDir(context: Context): File {
        val root = getTeacherHackerRootDir(context)
        val dir = File(root, FOLDER_HOMEWORK_SCANS)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Automatically saves a structured summary text file inside the session folder
     */
    fun saveSessionSummaryReport(
        context: Context,
        group: GroupEntity?,
        date: String,
        attendanceList: List<StudentAttendanceState>?,
        topic: String = "",
        homework: String = "",
        notes: String = ""
    ): File? {
        return try {
            val groupName = group?.name ?: "المجموعة"
            val sessionDir = getSessionDir(context, groupName, date)
            val summaryFile = File(sessionDir, "تقرير_وملاحظات_الحصة_$date.txt")

            val presentCount = attendanceList?.count { it.status == "present" } ?: 0
            val absentCount = attendanceList?.count { it.status == "absent" } ?: 0
            val lateCount = attendanceList?.count { it.status == "late" } ?: 0
            val excusedCount = attendanceList?.count { it.status == "excused" } ?: 0
            val hwCompleted = attendanceList?.count { it.homeworkStatus == "completed" } ?: 0
            val hwNotDone = attendanceList?.count { it.homeworkStatus == "not_done" } ?: 0

            val sb = StringBuilder()
            sb.append("====================================================\n")
            sb.append("📘 تقرير وملاحظات الحصة - هاكر التدريس (Teacher Hacker)\n")
            sb.append("====================================================\n")
            sb.append("👥 المجموعة: $groupName (${group?.grade ?: ""})\n")
            sb.append("📅 التاريخ: $date\n")
            sb.append("📍 المكان والموعد: ${group?.location ?: ""} - ${group?.sessionTime ?: ""}\n")
            sb.append("🎯 الترم: ${group?.currentTerm ?: "الترم الأول"}\n")
            sb.append("----------------------------------------------------\n")
            if (topic.isNotBlank()) {
                sb.append("📖 موضوع ودرس الحصة:\n$topic\n\n")
            }
            if (homework.isNotBlank()) {
                sb.append("📝 الواجب المطلوب والتكليفات:\n$homework\n\n")
            }
            if (notes.isNotBlank()) {
                sb.append("💡 ملاحظات وتوجيهات المعلم على الحصة:\n$notes\n\n")
            }
            sb.append("----------------------------------------------------\n")
            sb.append("📊 إحصائيات الحضور والواجب:\n")
            sb.append("• إجمالي المقيدين: ${attendanceList?.size ?: 0}\n")
            sb.append("• عدد الحاضرين: $presentCount\n")
            sb.append("• عدد الغائبين: $absentCount\n")
            sb.append("• عدد المتأخرين: $lateCount\n")
            sb.append("• عدد الأعذار: $excusedCount\n")
            sb.append("• من أكمل الواجب: $hwCompleted\n")
            sb.append("• من لم يحل الواجب: $hwNotDone\n")
            sb.append("----------------------------------------------------\n")
            sb.append("📋 كشف الطلاب التفصيلي وملاحظات كل طالب:\n")
            attendanceList?.forEachIndexed { index, item ->
                val statusStr = when (item.status) {
                    "present" -> "حاضر ✓"
                    "absent" -> "غائب ✗"
                    "late" -> "متأخر ⏳"
                    "excused" -> "بعذر 📩"
                    else -> item.status
                }
                val hwStr = when (item.homeworkStatus) {
                    "completed" -> "حل الواجب كاملاً ⭐"
                    "partial" -> "واجب ناقص ⚠️"
                    "not_done" -> "لم يحل الواجب ❌"
                    "exempt" -> "معفى من الواجب"
                    else -> ""
                }
                sb.append("${index + 1}. ${item.student.name} | $statusStr | $hwStr")
                if (item.note.isNotBlank()) {
                    sb.append(" | ملاحظة: ${item.note}")
                }
                sb.append("\n")
            }
            sb.append("====================================================\n")
            sb.append("تم الحفظ تلقائياً في مسار: DOCUMENTS / TEACHER HACKER / $date - $groupName\n")

            FileOutputStream(summaryFile).use { fos ->
                fos.write(sb.toString().toByteArray())
            }
            summaryFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Opens or shares a session folder / file in device file manager
     */
    fun openSessionFolder(context: Context, groupName: String, date: String) {
        try {
            val sessionDir = getSessionDir(context, groupName, date)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", sessionDir)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "فتح مجلد الحصة: $groupName ($date)"))
        } catch (e: Exception) {
            Toast.makeText(context, "مسار المجلد: DOCUMENTS/TEACHER HACKER/$date - $groupName", Toast.LENGTH_LONG).show()
        }
    }
}
