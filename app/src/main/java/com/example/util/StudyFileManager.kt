package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.example.data.local.entity.StudyFileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object StudyFileManager {

    const val APP_DISPLAY_NAME = "هاكر التدريس"
    const val APP_MAIN_FOLDER = "هاكر_التدريس"
    private const val OLD_MAIN_FOLDER = "RemixTeacherPlannerPro"
    const val SUBFOLDER_PDF_COPIES = "PDF_SafeCopies"
    const val SUBFOLDER_ANNOTATIONS = "Saved_Annotations"
    const val SUBFOLDER_HOMEWORK = "Homework_Exports"

    /**
     * Root App Files Folder inside DOCUMENTS / TEACHER HACKER
     */
    fun getAppRootDirectory(context: Context): File {
        val dir = TeacherHackerDirectoryManager.getTeacherHackerRootDir(context)
        if (!dir.exists()) {
            dir.mkdirs()
        }

        // Migrate any previous files from old internal folder if exists
        try {
            val oldInternal = File(context.filesDir, APP_MAIN_FOLDER)
            if (oldInternal.exists() && oldInternal.isDirectory) {
                oldInternal.listFiles()?.forEach { file ->
                    val target = File(dir, file.name)
                    if (!target.exists()) {
                        file.copyRecursively(target, overwrite = true)
                    }
                }
            }
            val oldDir = File(context.filesDir, OLD_MAIN_FOLDER)
            if (oldDir.exists() && oldDir.isDirectory) {
                oldDir.listFiles()?.forEach { file ->
                    val target = File(dir, file.name)
                    if (!target.exists()) {
                        file.copyRecursively(target, overwrite = true)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return dir
    }

    /**
     * Subfolder specifically for storing isolated safe copies of imported PDFs
     */
    fun getPdfSafeCopiesDirectory(context: Context): File {
        val dir = File(getAppRootDirectory(context), SUBFOLDER_PDF_COPIES)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Subfolder specifically for storing saved whiteboard / PDF annotations
     */
    fun getSavedAnnotationsDirectory(context: Context): File {
        val dir = File(getAppRootDirectory(context), SUBFOLDER_ANNOTATIONS)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Subfolder specifically for homework export pages
     */
    fun getHomeworkCopiesDirectory(context: Context): File {
        val dir = File(getAppRootDirectory(context), SUBFOLDER_HOMEWORK)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Backward-compatible alias for existing files
     */
    fun getStudyFilesDirectory(context: Context): File {
        return getPdfSafeCopiesDirectory(context)
    }

    /**
     * Saves a complete isolated safe copy of a selected PDF / Document into the dedicated app folder.
     * The original source file remains untouched.
     */
    suspend fun saveUriToInternalStorage(
        context: Context,
        uri: Uri,
        customTitle: String,
        grade: String,
        category: String,
        subject: String = "",
        notes: String = ""
    ): StudyFileEntity = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        var originalFileName = "doc_${System.currentTimeMillis()}.pdf"
        var fileSizeBytes = 0L

        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex != -1) {
                    originalFileName = cursor.getString(nameIndex) ?: originalFileName
                }
                if (sizeIndex != -1) {
                    fileSizeBytes = cursor.getLong(sizeIndex)
                }
            }
        }

        val cleanName = originalFileName.substringBeforeLast('.')
            .replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF_-]"), "_")
            .take(40)
        val extension = originalFileName.substringAfterLast('.', "pdf").lowercase(Locale.ROOT)
        
        // Dedicated safe copy filename
        val safeFileName = "نسخة_تطبيق_${cleanName}_${System.currentTimeMillis()}.$extension"
        val destinationFile = File(getPdfSafeCopiesDirectory(context), safeFileName)

        contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(destinationFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        if (fileSizeBytes <= 0) {
            fileSizeBytes = destinationFile.length()
        }

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        StudyFileEntity(
            title = customTitle.ifBlank { originalFileName.substringBeforeLast('.') },
            grade = grade,
            category = category,
            subject = subject,
            localFilePath = destinationFile.absolutePath,
            originalFileName = originalFileName,
            fileExtension = extension,
            fileSizeBytes = fileSizeBytes,
            notes = notes.ifBlank { "نسخة محفوظة بأمان في مجلد التطبيق ($APP_MAIN_FOLDER)" },
            dateAdded = today,
            createdAt = System.currentTimeMillis()
        )
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${String.format(Locale.US, "%.1f", bytes / 1024.0)} KB"
            else -> "${String.format(Locale.US, "%.1f", bytes / (1024.0 * 1024.0))} MB"
        }
    }

    fun shareFile(context: Context, fileEntity: StudyFileEntity) {
        val file = File(fileEntity.localFilePath)
        if (!file.exists()) return

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = getMimeType(fileEntity.fileExtension)
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileEntity.title)
            putExtra(Intent.EXTRA_TEXT, "ملف تعليمي: ${fileEntity.title}\nالصف: ${fileEntity.grade}\nالتصنيف: ${fileEntity.category}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة الملف عبر:"))
    }

    fun getMimeType(extension: String): String {
        return when (extension.lowercase(Locale.ROOT)) {
            "pdf" -> "application/pdf"
            "doc", "docx" -> "application/msword"
            "ppt", "pptx" -> "application/vnd.ms-powerpoint"
            "xls", "xlsx" -> "application/vnd.ms-excel"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            else -> "*/*"
        }
    }

    /**
     * Generates a rich sample PDF study guide if teacher wants to test immediately
     */
    suspend fun createSampleStudyPdf(
        context: Context,
        title: String,
        grade: String,
        category: String,
        subject: String
    ): StudyFileEntity = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Background
        val bgPaint = Paint().apply { color = Color.rgb(248, 250, 252) }
        canvas.drawRect(0f, 0f, 595f, 842f, bgPaint)

        // Header Box
        val headerPaint = Paint().apply { color = Color.rgb(15, 23, 42) }
        canvas.drawRect(20f, 20f, 575f, 120f, headerPaint)

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(title, 595f / 2, 60f, titlePaint)

        val subPaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            textSize = 12f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("$grade • $category • $subject", 595f / 2, 95f, subPaint)

        // Content Area with Grid & Geometric Demo
        val cardPaint = Paint().apply { color = Color.WHITE }
        canvas.drawRect(20f, 135f, 575f, 800f, cardPaint)

        val textPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 14f
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("📐 مذكرة الشرح والتمارين الهندسية التفاعلية", 550f, 170f, textPaint)

        val bodyPaint = Paint().apply {
            color = Color.rgb(71, 85, 105)
            textSize = 11f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("• يمكنك استخدام المسطرة والبرجل والمنقلة لرسم وقياس الأشكال مباشرة فوق هذه الصفحة.", 550f, 200f, bodyPaint)
        canvas.drawText("• يتوفر أيضاً قوالب المجسمات ثلاثية الأبعاد والأشكال المستوية ثنائية الأبعاد.", 550f, 225f, bodyPaint)
        canvas.drawText("• استخدم أقلام التأشير والممحاة لكتابة وحل المسائل وحفظ الشروحات للطلاب.", 550f, 250f, bodyPaint)

        // Drawing a sample coordinate grid
        val gridPaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
        }
        for (x in 50..545 step 25) {
            canvas.drawLine(x.toFloat(), 290f, x.toFloat(), 760f, gridPaint)
        }
        for (y in 290..760 step 25) {
            canvas.drawLine(50f, y.toFloat(), 545f, y.toFloat(), gridPaint)
        }

        // Axes
        val axisPaint = Paint().apply {
            color = Color.rgb(37, 99, 235)
            strokeWidth = 2.5f
        }
        canvas.drawLine(50f, 525f, 545f, 525f, axisPaint) // X axis
        canvas.drawLine(297f, 290f, 297f, 760f, axisPaint) // Y axis

        pdfDocument.finishPage(page)

        val destinationFile = File(getStudyFilesDirectory(context), "sample_${System.currentTimeMillis()}.pdf")
        FileOutputStream(destinationFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        StudyFileEntity(
            title = title,
            grade = grade,
            category = category,
            subject = subject,
            localFilePath = destinationFile.absolutePath,
            originalFileName = "$title.pdf",
            fileExtension = "pdf",
            fileSizeBytes = destinationFile.length(),
            notes = "ملف متاح للشرح والحل وتجربة الأدوات الهندسية",
            dateAdded = today,
            createdAt = System.currentTimeMillis()
        )
    }

    /**
     * Saves an annotated copy of a PDF or Whiteboard session into the dedicated app folder
     */
    suspend fun saveAnnotatedCopy(
        context: Context,
        pdfDocument: PdfDocument,
        baseTitle: String,
        pageIndex: Int,
        isHomework: Boolean = false
    ): File = withContext(Dispatchers.IO) {
        val targetFolder = if (isHomework) getHomeworkCopiesDirectory(context) else getSavedAnnotationsDirectory(context)
        val cleanTitle = baseTitle.replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF_-]"), "_").take(30)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
        val fileName = if (isHomework) {
            "واجب_${cleanTitle}_صفحة_${pageIndex + 1}_$timeStamp.pdf"
        } else {
            "شرح_${cleanTitle}_صفحة_${pageIndex + 1}_$timeStamp.pdf"
        }
        val targetFile = File(targetFolder, fileName)
        FileOutputStream(targetFile).use { out ->
            pdfDocument.writeTo(out)
        }
        targetFile
    }

    /**
     * Saves or overwrites a single persistent master copy of an annotated file
     * without cluttering storage with endless duplicates.
     */
    suspend fun saveSingleMasterCopy(
        context: Context,
        pdfDocument: PdfDocument,
        baseTitle: String
    ): File = withContext(Dispatchers.IO) {
        val targetFolder = getSavedAnnotationsDirectory(context)
        val cleanTitle = baseTitle.replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF_-]"), "_").take(40)
        val fileName = "${cleanTitle}_annotated_master.pdf"
        val targetFile = File(targetFolder, fileName)
        FileOutputStream(targetFile).use { out ->
            pdfDocument.writeTo(out)
        }
        targetFile
    }
}
