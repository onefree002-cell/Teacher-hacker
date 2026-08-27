package com.example.data.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.data.local.entity.CertificateSettingEntity
import com.example.data.local.entity.ExamEntity
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.PaymentEntity
import com.example.data.local.entity.QuestionEntity
import com.example.data.local.entity.ReportSettingEntity
import com.example.data.local.entity.StudentEntity
import com.example.data.local.entity.TeacherEntity
import com.example.data.repository.LeaderboardItem
import com.example.data.repository.StudentGradeItem
import com.example.data.repository.StudentWithDetails
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

private data class CertificatePalette(
    val bgCol: Int,
    val primaryCol: Int,
    val secondaryCol: Int,
    val accentCol: Int,
    val textPrimaryCol: Int,
    val textSecondaryCol: Int
)

class PdfReportExporter {

    companion object {
        fun generateTeacherPortfolioPdf(context: Context, teacher: TeacherEntity): File =
            PdfReportExporter().generateTeacherPortfolioPdf(context, teacher)

        fun generateTeacherBusinessCardsSheetPdf(context: Context, teacher: TeacherEntity): File =
            PdfReportExporter().generateTeacherBusinessCardsSheetPdf(context, teacher)

        fun generateStudentIdCardsPdf(
            context: Context,
            teacher: TeacherEntity?,
            group: GroupEntity?,
            students: List<StudentEntity>
        ): File = PdfReportExporter().generateStudentIdCardsPdf(context, teacher, group, students)

        fun generateHonorRollPdf(
            context: Context,
            teacher: TeacherEntity?,
            group: GroupEntity?,
            topStudents: List<LeaderboardItem>,
            posterTitle: String = "لوحة الشرف وتكريم الأوائل"
        ): File = PdfReportExporter().generateHonorRollPdf(context, teacher, group, topStudents, posterTitle)

        fun generateExamSheetPdf(
            context: Context,
            teacher: TeacherEntity?,
            title: String,
            grade: String,
            questions: List<QuestionEntity>,
            instructions: String,
            totalMarks: Double,
            includeAnswerKey: Boolean = true
        ): File = PdfReportExporter().generateExamSheetPdf(
            context, teacher, title, grade, questions, instructions, totalMarks, includeAnswerKey
        )

        fun generateAppFeaturePosterPdf(
            context: Context,
            teacher: TeacherEntity?
        ): File = PdfReportExporter().generateAppFeaturePosterPdf(context, teacher)

        fun generatePaymentReceiptPdf(
            context: Context,
            teacher: TeacherEntity?,
            student: StudentEntity,
            group: GroupEntity?,
            payment: PaymentEntity,
            remainingBalance: Double = 0.0
        ): File = PdfReportExporter().generatePaymentReceiptPdf(
            context, teacher, student, group, payment, remainingBalance
        )

        fun generateStudentHomeworkPdf(
            context: Context,
            teacher: TeacherEntity?,
            student: StudentEntity,
            group: GroupEntity?,
            lessonDate: String,
            homeworkTitle: String,
            imagePaths: List<String>,
            score: Double = 10.0,
            maxScore: Double = 10.0,
            rating: String = "ممتاز",
            teacherFeedback: String = ""
        ): File = PdfReportExporter().generateStudentHomeworkPdf(
            context, teacher, student, group, lessonDate, homeworkTitle, imagePaths, score, maxScore, rating, teacherFeedback
        )

        fun sharePdf(context: Context, file: File, title: String) =
            PdfReportExporter().sharePdf(context, file, title)

        fun sharePdfToWhatsApp(context: Context, file: File, caption: String = "", phoneNumber: String = "") =
            PdfReportExporter().sharePdfToWhatsApp(context, file, caption, phoneNumber)

        fun sharePdfToTelegram(context: Context, file: File, caption: String = "") =
            PdfReportExporter().sharePdfToTelegram(context, file, caption)
    }

    private fun drawTeacherLogoOrEmblem(
        canvas: Canvas,
        teacher: TeacherEntity?,
        cx: Float,
        cy: Float,
        radius: Float
    ) {
        if (teacher?.showLogoInPrintouts != false && !teacher?.logoUri.isNullOrEmpty()) {
            try {
                val uriStr = teacher?.logoUri ?: ""
                val filePath = when {
                    uriStr.startsWith("file://") -> uriStr.removePrefix("file://")
                    uriStr.startsWith("/") -> uriStr
                    else -> uriStr
                }
                val file = File(filePath)
                val bmp = if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null

                if (bmp != null) {
                    val dstRect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
                    val saveCount = canvas.save()
                    val clipPath = Path().apply {
                        addCircle(cx, cy, radius, Path.Direction.CCW)
                    }
                    canvas.clipPath(clipPath)
                    canvas.drawBitmap(bmp, null, dstRect, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
                    canvas.restoreToCount(saveCount)

                    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.rgb(30, 58, 138)
                        style = Paint.Style.STROKE
                        strokeWidth = 1.5f
                    }
                    canvas.drawCircle(cx, cy, radius, borderPaint)
                    return
                }
            } catch (_: Exception) {}
        }

        // Draw elegant circular emblem badge
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(224, 231, 255)
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 58, 138)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 58, 138)
            textSize = radius * 0.9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawCircle(cx, cy, radius, bgPaint)
        canvas.drawCircle(cx, cy, radius, borderPaint)
        val initial = teacher?.name?.trim()?.take(1)?.ifEmpty { "ع" } ?: "ع"
        canvas.drawText(initial, cx, cy + radius * 0.32f, textPaint)
    }

    // ==========================================
    // 1. SINGLE STUDENT REPORT
    // ==========================================
    fun generateStudentReportPdf(
        context: Context,
        teacher: TeacherEntity?,
        studentDetails: StudentWithDetails,
        grades: List<StudentGradeItem>,
        settings: ReportSettingEntity
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        renderSingleStudentReportPage(page.canvas, teacher, studentDetails, grades, settings)
        pdfDocument.finishPage(page)

        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val exportDir = File(context.cacheDir, "reports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val reportFile = File(exportDir, "Student_Report_${studentDetails.student.name.replace(" ", "_")}_$dateStr.pdf")

        FileOutputStream(reportFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()
        return reportFile
    }

    // ==========================================
    // 2. BATCH STUDENT DOSSIER (ALL STUDENTS IN GROUP)
    // ==========================================
    fun generateBatchStudentReportsPdf(
        context: Context,
        teacher: TeacherEntity?,
        group: GroupEntity?,
        studentListWithDetails: List<Pair<StudentWithDetails, List<StudentGradeItem>>>,
        settings: ReportSettingEntity
    ): File {
        val pdfDocument = PdfDocument()

        // Page 1: Group Summary Overview
        val summaryPageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val summaryPage = pdfDocument.startPage(summaryPageInfo)
        renderGroupSummaryPage(summaryPage.canvas, teacher, group, studentListWithDetails)
        pdfDocument.finishPage(summaryPage)

        // Subsequent pages: Individual Student Reports
        var pageNumber = 2
        for ((studentDetails, grades) in studentListWithDetails) {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
            val page = pdfDocument.startPage(pageInfo)
            renderSingleStudentReportPage(page.canvas, teacher, studentDetails, grades, settings)
            pdfDocument.finishPage(page)
            pageNumber++
        }

        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val exportDir = File(context.cacheDir, "batch_reports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val gName = group?.name?.replace(" ", "_") ?: "All_Students"
        val batchFile = File(exportDir, "Group_Dossier_${gName}_$dateStr.pdf")

        FileOutputStream(batchFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()
        return batchFile
    }

    // ==========================================
    // 3. SINGLE CERTIFICATE PDF
    // ==========================================
    fun generateCertificatePdf(
        context: Context,
        teacher: TeacherEntity?,
        studentName: String,
        groupName: String,
        settings: CertificateSettingEntity
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(842, 595, 1).create() // A4 Landscape
        val page = pdfDocument.startPage(pageInfo)
        renderCertificateLandscape(page.canvas, teacher, studentName, groupName, settings)
        pdfDocument.finishPage(page)

        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val exportDir = File(context.cacheDir, "certificates")
        if (!exportDir.exists()) exportDir.mkdirs()
        val certFile = File(exportDir, "Certificate_${studentName.replace(" ", "_")}_$dateStr.pdf")

        FileOutputStream(certFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()
        return certFile
    }

    // ==========================================
    // 4. BATCH MULTI-CERTIFICATES PDF (WHOLE GROUP)
    // ==========================================
    fun generateBatchCertificatesPdf(
        context: Context,
        teacher: TeacherEntity?,
        studentsWithGroup: List<Pair<String, String>>, // List of Pair(studentName, groupName)
        settings: CertificateSettingEntity
    ): File {
        val pdfDocument = PdfDocument()
        var pageNum = 1

        for ((studentName, groupName) in studentsWithGroup) {
            val pageInfo = PdfDocument.PageInfo.Builder(842, 595, pageNum).create()
            val page = pdfDocument.startPage(pageInfo)
            renderCertificateLandscape(page.canvas, teacher, studentName, groupName, settings)
            pdfDocument.finishPage(page)
            pageNum++
        }

        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val exportDir = File(context.cacheDir, "certificates")
        if (!exportDir.exists()) exportDir.mkdirs()
        val batchCertFile = File(exportDir, "Batch_Certificates_${pageNum - 1}_Students_$dateStr.pdf")

        FileOutputStream(batchCertFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()
        return batchCertFile
    }

    // ==========================================
    // 5. BATCH GROUP GRADE SHEET MATRIX
    // ==========================================
    fun generateGroupGradeSheetPdf(
        context: Context,
        teacher: TeacherEntity?,
        group: GroupEntity?,
        exams: List<ExamEntity>,
        studentsGrades: List<Pair<String, Map<Long, Double>>> // StudentName to Map(examId -> score)
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(842, 595, 1).create() // Landscape
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Background
        val bgPaint = Paint().apply { color = Color.rgb(248, 250, 252) }
        canvas.drawRect(0f, 0f, 842f, 595f, bgPaint)

        // Header Banner
        val headerPaint = Paint().apply { color = Color.rgb(30, 58, 138) }
        canvas.drawRoundRect(20f, 20f, 822f, 75f, 10f, 10f, headerPaint)

        drawTeacherLogoOrEmblem(canvas, teacher, 55f, 47f, 20f)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(224, 231, 255)
            textSize = 11f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("كشف الدرجات المجمع - ${group?.name ?: "المجموعة"}", 842f / 2, 45f, titlePaint)
        canvas.drawText("${teacher?.name ?: "عبده أيمن"} | ${teacher?.centerName ?: ""} | تاريخ الطباعة: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}", 842f / 2, 65f, subPaint)

        // Draw Table Header
        var currentY = 95f
        val tableBg = Paint().apply { color = Color.WHITE }
        val tableBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val colHeaderBg = Paint().apply { color = Color.rgb(241, 245, 249) }
        val colHeaderText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val cellText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(51, 65, 85)
            textSize = 10f
            textAlign = Paint.Align.CENTER
        }
        val nameCellText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        val rowHeight = 24f
        val nameColWidth = 160f
        val startX = 25f
        val tableWidth = 792f
        val availableExamWidth = tableWidth - nameColWidth
        val examColWidth = if (exams.isNotEmpty()) (availableExamWidth / exams.size.coerceAtMost(8)) else 80f

        // Draw header row
        canvas.drawRect(startX, currentY, startX + tableWidth, currentY + rowHeight, colHeaderBg)
        canvas.drawRect(startX, currentY, startX + tableWidth, currentY + rowHeight, tableBorder)
        canvas.drawText("اسم الطالب", startX + tableWidth - 10f, currentY + 16f, colHeaderText.apply { textAlign = Paint.Align.RIGHT })

        val shownExams = exams.take(8)
        shownExams.forEachIndexed { index, ex ->
            val eX = startX + (shownExams.size - 1 - index) * examColWidth + examColWidth / 2f
            canvas.drawText("${ex.title} (${ex.maxScore.toInt()})", eX, currentY + 16f, colHeaderText.apply { textAlign = Paint.Align.CENTER })
        }

        currentY += rowHeight

        // Rows
        for ((studentName, gradeMap) in studentsGrades.take(18)) {
            val isEven = (currentY.toInt() / rowHeight.toInt()) % 2 == 0
            val rowBg = if (isEven) Color.WHITE else Color.rgb(248, 250, 252)
            canvas.drawRect(startX, currentY, startX + tableWidth, currentY + rowHeight, Paint().apply { color = rowBg })
            canvas.drawRect(startX, currentY, startX + tableWidth, currentY + rowHeight, tableBorder)

            canvas.drawText(studentName, startX + tableWidth - 10f, currentY + 16f, nameCellText)

            shownExams.forEachIndexed { index, ex ->
                val eX = startX + (shownExams.size - 1 - index) * examColWidth + examColWidth / 2f
                val score = gradeMap[ex.id]
                val scoreText = if (score != null) "${score.toInt()}" else "-"
                val scoreColor = if (score != null) {
                    if (score >= ex.passScore) Color.rgb(5, 150, 105) else Color.rgb(220, 38, 38)
                } else Color.GRAY
                canvas.drawText(scoreText, eX, currentY + 16f, cellText.apply { color = scoreColor })
            }
            currentY += rowHeight
        }

        pdfDocument.finishPage(page)

        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val exportDir = File(context.cacheDir, "reports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val sheetFile = File(exportDir, "GradeSheet_${group?.name?.replace(" ", "_") ?: "Group"}_$dateStr.pdf")

        FileOutputStream(sheetFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()
        return sheetFile
    }

    // ==========================================
    // 6. STUDENT ID CARDS SHEET (8 CARDS PER A4 PAGE)
    // ==========================================
    fun generateStudentIdCardsPdf(
        context: Context,
        teacher: TeacherEntity?,
        group: GroupEntity?,
        students: List<StudentEntity>
    ): File {
        val pdfDocument = PdfDocument()
        val cardsPerPage = 8
        val chunks = students.chunked(cardsPerPage)

        chunks.forEachIndexed { pageIndex, pageStudents ->
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageIndex + 1).create() // A4 Portrait
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Background
            canvas.drawRect(0f, 0f, 595f, 842f, Paint().apply { color = Color.WHITE })

            // Page Header
            val topTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(71, 85, 105)
                textSize = 9f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("بطاقات تعريف الطلاب (كارنيهات الحضور والباركود) - ${group?.name ?: "كل الطلاب"} - صفحة ${pageIndex + 1}", 595f / 2, 20f, topTextPaint)

            // Grid Layout: 2 Columns x 4 Rows
            val marginX = 25f
            val marginY = 32f
            val cardWidth = 260f
            val cardHeight = 185f
            val gapX = 25f
            val gapY = 16f

            pageStudents.forEachIndexed { index, student ->
                val col = index % 2
                val row = index / 2
                val left = marginX + col * (cardWidth + gapX)
                val top = marginY + row * (cardHeight + gapY)
                val right = left + cardWidth
                val bottom = top + cardHeight

                renderSingleStudentIdCard(canvas, teacher, group, student, left, top, right, bottom)
            }

            pdfDocument.finishPage(page)
        }

        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val exportDir = File(context.cacheDir, "id_cards")
        if (!exportDir.exists()) exportDir.mkdirs()
        val gName = group?.name?.replace(" ", "_") ?: "All_Students"
        val idCardsFile = File(exportDir, "Student_ID_Cards_${gName}_$dateStr.pdf")

        FileOutputStream(idCardsFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()
        return idCardsFile
    }

    private fun renderSingleStudentIdCard(
        canvas: Canvas,
        teacher: TeacherEntity?,
        group: GroupEntity?,
        student: StudentEntity,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ) {
        val width = right - left
        val height = bottom - top

        // Cut Guideline (Dashed Border)
        val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1f
            pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
        }
        canvas.drawRect(left - 2f, top - 2f, right + 2f, bottom + 2f, dashPaint)

        // Card Container with Soft Shadow & Border
        val cardBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val cardBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 58, 138)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        val cardInnerBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(248, 250, 252) }
        canvas.drawRoundRect(left, top, right, bottom, 10f, 10f, cardBg)
        canvas.drawRoundRect(left, top, right, bottom, 10f, 10f, cardInnerBg)
        canvas.drawRoundRect(left, top, right, bottom, 10f, 10f, cardBorder)

        // Top Header Banner
        val headerBanner = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(30, 58, 138) }
        val headerPath = Path().apply {
            moveTo(left, top + 10f)
            quadTo(left, top, left + 10f, top)
            lineTo(right - 10f, top)
            quadTo(right, top, right, top + 10f)
            lineTo(right, top + 38f)
            lineTo(left, top + 38f)
            close()
        }
        canvas.drawPath(headerPath, headerBanner)

        val headerText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subHeaderText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(224, 231, 255)
            textSize = 7.5f
            textAlign = Paint.Align.CENTER
        }
        val centerTitle = teacher?.centerName?.ifEmpty { "أكاديمية التفوق التعليمية" } ?: "أكاديمية التفوق"
        drawTeacherLogoOrEmblem(canvas, teacher, left + 20f, top + 19f, 12f)
        canvas.drawText(centerTitle, left + width / 2f + 5f, top + 17f, headerText)
        canvas.drawText("المدرس: ${teacher?.name ?: "عبده أيمن"} • ${teacher?.subject ?: ""}", left + width / 2f + 5f, top + 30f, subHeaderText)

        // Student Info Content
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            textSize = 8.5f
            textAlign = Paint.Align.RIGHT
        }
        val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 58, 138)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        // Student Name
        canvas.drawText(student.name, right - 12f, top + 56f, namePaint)

        // Group & Grade
        val gName = group?.name ?: student.grade.ifEmpty { "مجموعة عامة" }
        canvas.drawText("المجموعة:", right - 12f, top + 72f, labelPaint)
        canvas.drawText(gName, right - 55f, top + 72f, valPaint)

        canvas.drawText("الصف:", right - 12f, top + 86f, labelPaint)
        canvas.drawText(student.grade.ifEmpty { "المرحلة الدراسية" }, right - 40f, top + 86f, valPaint)

        if (student.parentPhone.isNotEmpty()) {
            canvas.drawText("ولي الأمر:", right - 12f, top + 100f, labelPaint)
            canvas.drawText(student.parentPhone, right - 55f, top + 100f, valPaint)
        }

        // Barcode / QR Code representation Box on Left Side
        val codeBoxLeft = left + 14f
        val codeBoxTop = top + 48f
        val codeBoxRight = left + 84f
        val codeBoxBottom = top + 118f

        val codeBoxBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val codeBoxBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(codeBoxLeft, codeBoxTop, codeBoxRight, codeBoxBottom, 6f, 6f, codeBoxBg)
        canvas.drawRoundRect(codeBoxLeft, codeBoxTop, codeBoxRight, codeBoxBottom, 6f, 6f, codeBoxBorder)

        // Draw crisp QR / Barcode pattern inside box
        drawIdCardQrPattern(canvas, codeBoxLeft + 5f, codeBoxTop + 5f, 60f, student.barcodeCode.ifEmpty { "STD-${student.id}" })

        // Student Code String under QR
        val codeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 7.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val displayCode = student.barcodeCode.ifEmpty { "ID: STD-${1000 + student.id}" }
        canvas.drawText(displayCode, codeBoxLeft + 35f, codeBoxBottom + 12f, codeTextPaint)

        // Bottom Footer strip
        val footerBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(241, 245, 249) }
        val footerPath = Path().apply {
            moveTo(left, bottom - 26f)
            lineTo(right, bottom - 26f)
            lineTo(right, bottom - 10f)
            quadTo(right, bottom, right - 10f, bottom)
            lineTo(left + 10f, bottom)
            quadTo(left, bottom, left, bottom - 10f)
            close()
        }
        canvas.drawPath(footerPath, footerBg)
        canvas.drawLine(left, bottom - 26f, right, bottom - 26f, cardBorder.apply { strokeWidth = 0.8f })

        val footerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            textSize = 7f
            textAlign = Paint.Align.CENTER
        }
        val emergencyPhone = teacher?.phone?.ifEmpty { teacher?.whatsapp ?: "" } ?: ""
        canvas.drawText("يُرجى إبراز الكارنيه عند الحضور • طوارئ: $emergencyPhone", left + width / 2f, bottom - 10f, footerTextPaint)
    }

    private fun drawIdCardQrPattern(canvas: Canvas, x: Float, y: Float, size: Float, code: String) {
        try {
            val qrBitmap = com.example.util.QrBarcodeUtils.generateQrBitmap(
                content = code.ifEmpty { "STD-0" },
                size = 256,
                darkColor = Color.rgb(15, 23, 42),
                lightColor = Color.WHITE
            )
            val destRect = RectF(x, y, x + size, y + size)
            canvas.drawBitmap(qrBitmap, null, destRect, null)
        } catch (e: Exception) {
            // Fallback block drawing
            val blockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(15, 23, 42) }
            canvas.drawRect(x, y, x + size, y + size, blockPaint)
        }
    }

    // ==========================================
    // 7. QUESTION BANK EXAM & SHEET BUILDER PDF
    // ==========================================
    fun generateExamSheetPdf(
        context: Context,
        teacher: TeacherEntity?,
        title: String,
        grade: String,
        questions: List<QuestionEntity>,
        instructions: String,
        totalMarks: Double,
        includeAnswerKey: Boolean = true
    ): File {
        val pdfDocument = PdfDocument()
        var pageNum = 1

        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create() // A4 Portrait
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Background
        canvas.drawRect(0f, 0f, 595f, 842f, Paint().apply { color = Color.WHITE })

        // Exam Header Box
        val headerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 58, 138)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        val headerBg = Paint().apply { color = Color.rgb(248, 250, 252) }
        canvas.drawRoundRect(25f, 25f, 570f, 115f, 10f, 10f, headerBg)
        canvas.drawRoundRect(25f, 25f, 570f, 115f, 10f, 10f, headerBorder)

        val schoolTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val teacherTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        val examTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 58, 138)
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText(teacher?.centerName?.ifEmpty { "أكاديمية التفوق التعليمية" } ?: "أكاديمية التفوق", 555f, 45f, schoolTitlePaint)
        canvas.drawText("المادة: ${teacher?.subject ?: "جميع المواد"}", 555f, 62f, schoolTitlePaint)

        canvas.drawText("أستاذ المادة: ${teacher?.name ?: "المعلم"}", 40f, 45f, teacherTitlePaint)
        canvas.drawText("الصف: ${grade.ifEmpty { "العام الدراسي" }}", 40f, 62f, teacherTitlePaint)

        canvas.drawText(title.ifEmpty { "اختبار تقييمي شامل" }, 595f / 2, 60f, examTitlePaint)

        // Student metadata fill-in line
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10f
            textAlign = Paint.Align.RIGHT
        }
        val dottedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184)
            style = Paint.Style.STROKE
            strokeWidth = 1f
            pathEffect = DashPathEffect(floatArrayOf(3f, 3f), 0f)
        }

        canvas.drawText("اسم الطالب: ..............................................................", 555f, 95f, linePaint)
        canvas.drawText("المجموعة: .....................", 280f, 95f, linePaint)
        canvas.drawText("الدرجة الكلية: (${totalMarks.toInt()} درجة)", 140f, 95f, linePaint.apply { color = Color.rgb(180, 83, 9) })

        if (instructions.isNotEmpty()) {
            val instPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(100, 116, 139)
                textSize = 8.5f
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("تعليمات: $instructions", 555f, 108f, instPaint)
        }

        // Questions List Rendering
        var currentY = 135f
        val qNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 58, 138)
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val qTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val optPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(51, 65, 85)
            textSize = 10f
            textAlign = Paint.Align.RIGHT
        }
        val markPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139)
            textSize = 9f
            textAlign = Paint.Align.LEFT
        }

        questions.forEachIndexed { qIdx, question ->
            // Question Title Row
            val qNum = "${qIdx + 1} )"
            canvas.drawText(qNum, 570f, currentY + 12f, qNumPaint)
            canvas.drawText(question.questionText, 545f, currentY + 12f, qTextPaint)
            canvas.drawText("(${question.marks.toInt()} درجات)", 30f, currentY + 12f, markPaint)

            currentY += 22f

            when (question.questionType) {
                "mcq" -> {
                    // Draw MCQ Options in 2 columns
                    val col1X = 540f
                    val col2X = 300f

                    if (question.optionA.isNotEmpty()) {
                        drawOptionBubble(canvas, col1X, currentY + 8f, "أ", question.optionA)
                    }
                    if (question.optionB.isNotEmpty()) {
                        drawOptionBubble(canvas, col2X, currentY + 8f, "ب", question.optionB)
                    }
                    currentY += 18f

                    if (question.optionC.isNotEmpty()) {
                        drawOptionBubble(canvas, col1X, currentY + 8f, "ج", question.optionC)
                    }
                    if (question.optionD.isNotEmpty()) {
                        drawOptionBubble(canvas, col2X, currentY + 8f, "د", question.optionD)
                    }
                    currentY += 24f
                }
                "true_false" -> {
                    drawOptionBubble(canvas, 520f, currentY + 8f, "✓", "صواب")
                    drawOptionBubble(canvas, 380f, currentY + 8f, "✗", "خطأ")
                    currentY += 24f
                }
                else -> { // essay
                    // Draw ruled lines for student answer
                    for (line in 0..2) {
                        canvas.drawLine(30f, currentY + 14f, 560f, currentY + 14f, dottedPaint)
                        currentY += 18f
                    }
                    currentY += 8f
                }
            }

            // Divider between questions
            canvas.drawLine(30f, currentY, 565f, currentY, Paint().apply { color = Color.rgb(241, 245, 249); strokeWidth = 1f })
            currentY += 10f
        }

        // Footer
        val footPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184)
            textSize = 8.5f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("مع أطيب التمنيات بالتفوق والنجاح • أستاذ المادة: ${teacher?.name ?: ""}", 595f / 2, 820f, footPaint)

        pdfDocument.finishPage(page)

        // Optional Model Answer Page
        if (includeAnswerKey) {
            pageNum++
            val ansPageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
            val ansPage = pdfDocument.startPage(ansPageInfo)
            val ansCanvas = ansPage.canvas

            ansCanvas.drawRect(0f, 0f, 595f, 842f, Paint().apply { color = Color.WHITE })

            val ansHeader = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(5, 150, 105)
                textSize = 16f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            ansCanvas.drawRoundRect(25f, 25f, 570f, 75f, 10f, 10f, Paint().apply { color = Color.rgb(236, 253, 245) })
            ansCanvas.drawRoundRect(25f, 25f, 570f, 75f, 10f, 10f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(5, 150, 105); style = Paint.Style.STROKE; strokeWidth = 1.5f })
            ansCanvas.drawText("نموذج الإجابة النموذجي وتوزيع الدرجات", 595f / 2, 55f, ansHeader)
            ansCanvas.drawText("$title • إجمالي الدرجات: ${totalMarks.toInt()}", 595f / 2, 70f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(6, 95, 70); textSize = 10f; textAlign = Paint.Align.CENTER })

            var aY = 100f
            questions.forEachIndexed { idx, q ->
                val qAnsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(15, 23, 42)
                    textSize = 11f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.RIGHT
                }
                val ansValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(5, 150, 105)
                    textSize = 11f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.RIGHT
                }
                ansCanvas.drawText("س ${idx + 1} : ${q.questionText}", 560f, aY + 12f, qAnsPaint)
                ansCanvas.drawText("الإجابة الصحيحة: ${q.correctAnswer.ifEmpty { "متروكة لتقدير المعلم" }} (${q.marks.toInt()} درجات)", 540f, aY + 28f, ansValPaint)
                aY += 40f
            }

            pdfDocument.finishPage(ansPage)
        }

        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val exportDir = File(context.cacheDir, "exam_sheets")
        if (!exportDir.exists()) exportDir.mkdirs()
        val examFile = File(exportDir, "Exam_Sheet_${title.replace(" ", "_")}_$dateStr.pdf")

        FileOutputStream(examFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()
        return examFile
    }

    private fun drawOptionBubble(canvas: Canvas, x: Float, y: Float, letter: String, text: String) {
        val bubbleBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val letterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 58, 138)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(51, 65, 85)
            textSize = 10f
            textAlign = Paint.Align.RIGHT
        }

        canvas.drawCircle(x - 5f, y - 3f, 7f, bubbleBorder)
        canvas.drawText(letter, x - 5f, y, letterPaint)
        canvas.drawText(text, x - 18f, y, textPaint)
    }

    // ==========================================
    // 8. HONOR ROLL & LEADERBOARD POSTER PDF
    // ==========================================
    fun generateHonorRollPdf(
        context: Context,
        teacher: TeacherEntity?,
        group: GroupEntity?,
        topStudents: List<LeaderboardItem>,
        posterTitle: String = "لوحة الشرف وتكريم الأوائل"
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Portrait
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Background Rich Royal Theme
        canvas.drawRect(0f, 0f, 595f, 842f, Paint().apply { color = Color.rgb(255, 254, 248) })

        // Outer Golden Double Border
        val borderOuter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(217, 119, 6)
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        val borderInner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 58, 138)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawRoundRect(20f, 20f, 575f, 822f, 14f, 14f, borderOuter)
        canvas.drawRoundRect(28f, 28f, 567f, 814f, 10f, 10f, borderInner)

        // Corner Stars
        val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(217, 119, 6); style = Paint.Style.FILL }
        drawStar(canvas, 42f, 42f, 5, 8f, 4f, starPaint)
        drawStar(canvas, 553f, 42f, 5, 8f, 4f, starPaint)
        drawStar(canvas, 42f, 800f, 5, 8f, 4f, starPaint)
        drawStar(canvas, 553f, 800f, 5, 8f, 4f, starPaint)

        // Header Crest
        drawPresetEmblem(canvas, 595f / 2f, 65f, "trophy", Color.rgb(217, 119, 6))

        val schoolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(teacher?.centerName?.ifEmpty { "أكاديمية التفوق التعليمية" } ?: "أكاديمية التفوق", 595f / 2f, 100f, schoolPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 58, 138)
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(posterTitle, 595f / 2f, 130f, titlePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(180, 83, 9)
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val groupLine = group?.name?.let { "أوائل مجموعة: $it" } ?: "أوائل الطلاب المتفوقين في جميع المجموعات"
        canvas.drawText(groupLine, 595f / 2f, 150f, subPaint)

        // Top 3 Podium (1st, 2nd, 3rd)
        val firstStudent = topStudents.getOrNull(0)
        val secondStudent = topStudents.getOrNull(1)
        val thirdStudent = topStudents.getOrNull(2)

        // Podium Column 2 (Left): 2nd Place
        if (secondStudent != null) {
            renderPodiumCard(canvas, 50f, 210f, 140f, 120f, "المركز الثاني 🥈", secondStudent.student.name, secondStudent.overallScore, Color.rgb(100, 116, 139))
        }

        // Podium Column 1 (Center): 1st Place (Taller)
        if (firstStudent != null) {
            renderPodiumCard(canvas, 205f, 185f, 185f, 145f, "المركز الأول 🥇", firstStudent.student.name, firstStudent.overallScore, Color.rgb(217, 119, 6), isFirst = true)
        }

        // Podium Column 3 (Right): 3rd Place
        if (thirdStudent != null) {
            renderPodiumCard(canvas, 405f, 225f, 140f, 105f, "المركز الثالث 🥉", thirdStudent.student.name, thirdStudent.overallScore, Color.rgb(180, 83, 9))
        }

        // Rest of Top Students Table (4th to 10th)
        var tableY = 350f
        val rankHeaderBg = Paint().apply { color = Color.rgb(30, 58, 138) }
        canvas.drawRoundRect(40f, tableY, 555f, tableY + 28f, 6f, 6f, rankHeaderBg)

        val tableHeadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("الترتيب", 520f, tableY + 18f, tableHeadPaint)
        canvas.drawText("اسم الطالب", 380f, tableY + 18f, tableHeadPaint)
        canvas.drawText("المجموعة", 240f, tableY + 18f, tableHeadPaint)
        canvas.drawText("نسبة الحضور", 140f, tableY + 18f, tableHeadPaint)
        canvas.drawText("المعدل العام", 75f, tableY + 18f, tableHeadPaint)

        tableY += 30f

        val rowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10f
            textAlign = Paint.Align.CENTER
        }
        val rowBgEven = Color.rgb(248, 250, 252)
        val rowBgOdd = Color.WHITE

        topStudents.take(10).forEachIndexed { idx, item ->
            val bg = if (idx % 2 == 0) rowBgEven else rowBgOdd
            canvas.drawRect(40f, tableY, 555f, tableY + 24f, Paint().apply { color = bg })
            canvas.drawRect(40f, tableY, 555f, tableY + 24f, Paint().apply { color = Color.rgb(226, 232, 240); style = Paint.Style.STROKE })

            val medal = when (idx) {
                0 -> "🥇 1"
                1 -> "🥈 2"
                2 -> "🥉 3"
                else -> "${idx + 1}"
            }
            canvas.drawText(medal, 520f, tableY + 16f, rowPaint.apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
            canvas.drawText(item.student.name, 380f, tableY + 16f, rowPaint.apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
            canvas.drawText(item.groupName, 240f, tableY + 16f, rowPaint.apply { typeface = Typeface.DEFAULT })
            canvas.drawText("${item.attendanceRate}%", 140f, tableY + 16f, rowPaint)
            canvas.drawText("${String.format(Locale.US, "%.1f", item.overallScore)}%", 75f, tableY + 16f, rowPaint.apply { color = Color.rgb(5, 150, 105); typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })

            tableY += 24f
        }

        // Teacher signature stamp at bottom
        val footerDate = "تاريخ التكريم: ${SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())}"
        canvas.drawText(footerDate, 100f, 780f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(100, 116, 139); textSize = 9.5f })
        drawSealStamp(canvas, 480f, 765f, "لوحة الشرف", Color.rgb(217, 119, 6))

        pdfDocument.finishPage(page)

        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val exportDir = File(context.cacheDir, "honor_roll")
        if (!exportDir.exists()) exportDir.mkdirs()
        val gName = group?.name?.replace(" ", "_") ?: "Top_Students"
        val posterFile = File(exportDir, "Honor_Roll_${gName}_$dateStr.pdf")

        FileOutputStream(posterFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()
        return posterFile
    }

    private fun renderPodiumCard(
        canvas: Canvas,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        rankTitle: String,
        studentName: String,
        score: Double,
        themeColor: Int,
        isFirst: Boolean = false
    ) {
        val bgPaint = Paint().apply {
            color = if (isFirst) Color.rgb(254, 243, 199) else Color.rgb(241, 245, 249)
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = themeColor
            style = Paint.Style.STROKE
            strokeWidth = if (isFirst) 2.5f else 1.5f
        }
        canvas.drawRoundRect(x, y, x + w, y + h, 12f, 12f, bgPaint)
        canvas.drawRoundRect(x, y, x + w, y + h, 12f, 12f, borderPaint)

        val rankPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = themeColor
            textSize = if (isFirst) 13f else 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(rankTitle, x + w / 2f, y + 25f, rankPaint)

        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = if (isFirst) 14f else 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(studentName, x + w / 2f, y + 60f, namePaint)

        val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(5, 150, 105)
            textSize = if (isFirst) 15f else 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("${String.format(Locale.US, "%.1f", score)}%", x + w / 2f, y + 90f, scorePaint)
    }

    // ==========================================
    // INTERNAL RENDERING: CERTIFICATE
    // ==========================================
    private fun renderCertificateLandscape(
        canvas: Canvas,
        teacher: TeacherEntity?,
        studentName: String,
        groupName: String,
        settings: CertificateSettingEntity
    ) {
        val template = settings.templateId

        // Theme-specific colors & styles
        val palette = when (template) {
            "modern_navy" -> CertificatePalette(
                Color.rgb(255, 255, 255),
                Color.rgb(30, 58, 138),
                Color.rgb(2, 132, 199),
                Color.rgb(14, 165, 233),
                Color.rgb(15, 23, 42),
                Color.rgb(71, 85, 105)
            )
            "emerald_luxury" -> CertificatePalette(
                Color.rgb(254, 255, 254),
                Color.rgb(6, 95, 70),
                Color.rgb(217, 119, 6),
                Color.rgb(16, 185, 129),
                Color.rgb(6, 78, 59),
                Color.rgb(75, 85, 99)
            )
            "imperial_burgundy" -> CertificatePalette(
                Color.rgb(255, 253, 250),
                Color.rgb(136, 19, 55),
                Color.rgb(202, 138, 4),
                Color.rgb(190, 24, 93),
                Color.rgb(76, 5, 25),
                Color.rgb(107, 114, 128)
            )
            "dark_onyx_gold" -> CertificatePalette(
                Color.rgb(17, 24, 39),
                Color.rgb(245, 158, 11),
                Color.rgb(251, 191, 36),
                Color.rgb(217, 119, 6),
                Color.rgb(255, 255, 255),
                Color.rgb(209, 213, 219)
            )
            else -> CertificatePalette( // classic_gold
                Color.rgb(255, 254, 248),
                Color.rgb(30, 58, 138),
                Color.rgb(217, 119, 6),
                Color.rgb(16, 185, 129),
                Color.rgb(27, 42, 74),
                Color.rgb(75, 85, 99)
            )
        }
        val bgCol = palette.bgCol
        val primaryCol = palette.primaryCol
        val secondaryCol = palette.secondaryCol
        val accentCol = palette.accentCol
        val textPrimaryCol = palette.textPrimaryCol
        val textSecondaryCol = palette.textSecondaryCol

        // 1. Background
        canvas.drawRect(0f, 0f, 842f, 595f, Paint().apply { color = bgCol })

        // 2. Outer Ornate Double Border
        val borderOuter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryCol
            style = Paint.Style.STROKE
            strokeWidth = if (template == "dark_onyx_gold") 4f else 6f
        }
        canvas.drawRoundRect(22f, 22f, 820f, 573f, 16f, 16f, borderOuter)

        val borderInner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryCol
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(32f, 32f, 810f, 563f, 12f, 12f, borderInner)

        // Corner Rosettes/Stars
        val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryCol
            style = Paint.Style.FILL
        }
        val corners = listOf(Pair(46f, 46f), Pair(796f, 46f), Pair(46f, 549f), Pair(796f, 549f))
        for ((cx, cy) in corners) {
            drawStar(canvas, cx, cy, 6, 9f, 4.5f, cornerPaint)
        }

        // 3. Custom Logo / Preset Emblem Rendering
        var logoDrawn = false
        val activeLogo = if (!settings.logoUri.isNullOrEmpty()) settings.logoUri else if (teacher?.showLogoInPrintouts != false) teacher?.logoUri else null
        if (!activeLogo.isNullOrEmpty()) {
            try {
                val filePath = when {
                    activeLogo.startsWith("file://") -> activeLogo.removePrefix("file://")
                    activeLogo.startsWith("/") -> activeLogo
                    else -> activeLogo
                }
                val logoFile = File(filePath)
                if (logoFile.exists()) {
                    val bmp = BitmapFactory.decodeFile(logoFile.absolutePath)
                    if (bmp != null) {
                        val dstRect = RectF(710f, 50f, 785f, 125f)
                        val saveCount = canvas.save()
                        val clipPath = Path().apply {
                            addCircle(747.5f, 87.5f, 35f, Path.Direction.CCW)
                        }
                        canvas.clipPath(clipPath)
                        canvas.drawBitmap(bmp, null, dstRect, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
                        canvas.restoreToCount(saveCount)

                        val borderP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = secondaryCol
                            style = Paint.Style.STROKE
                            strokeWidth = 2f
                        }
                        canvas.drawCircle(747.5f, 87.5f, 35f, borderP)
                        logoDrawn = true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (!logoDrawn) {
            // Draw Preset Vector Emblem (Crown / Trophy / Medal / Quill)
            drawPresetEmblem(canvas, 842f / 2f, 72f, settings.presetLogo, secondaryCol)
        }

        // 4. Center / School Header Name
        val schoolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = if (template == "dark_onyx_gold") secondaryCol else textSecondaryCol
            textAlign = Paint.Align.CENTER
        }
        val sName = settings.schoolName.ifEmpty { teacher?.centerName ?: "أكاديمية التفوق والتميز" }
        canvas.drawText(sName, 842f / 2f, 115f, schoolPaint)

        // 5. Main Title (شهادة تفوق وتقدير)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = if (template == "dark_onyx_gold") secondaryCol else primaryCol
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(settings.title.ifEmpty { "شهادة تفوق وتقدير" }, 842f / 2f, 160f, titlePaint)

        // 6. Subtitle
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12f
            color = textSecondaryCol
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(settings.subtitle.ifEmpty { "تقديراً للجهد المتميز والأداء الرائع في الفصل الدراسي" }, 842f / 2f, 188f, subPaint)

        // 7. Preamble text
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 13.5f
            color = textPrimaryCol
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("يسرنا ويسعدنا منح هذه الشهادة التقديرية إلى الطالب المتميز:", 842f / 2f, 235f, bodyPaint)

        // 8. Student Name Highlight Ribbon Card
        val nameBannerPaint = Paint().apply {
            color = if (template == "dark_onyx_gold") Color.argb(60, 245, 158, 11) else Color.argb(25, Color.red(secondaryCol), Color.green(secondaryCol), Color.blue(secondaryCol))
        }
        val nameBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryCol
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawRoundRect(140f, 260f, 702f, 322f, 16f, 16f, nameBannerPaint)
        canvas.drawRoundRect(140f, 260f, 702f, 322f, 16f, 16f, nameBorderPaint)

        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = secondaryCol
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(studentName, 842f / 2f, 301f, namePaint)

        // 9. Group and Praise Body Template
        val groupLine = if (groupName.isNotEmpty()) "المقيد بـ: $groupName" else ""
        if (groupLine.isNotEmpty()) {
            canvas.drawText(groupLine, 842f / 2f, 355f, bodyPaint.apply { textSize = 13f })
        }
        val bodyMsg = settings.bodyTemplate.ifEmpty { "تقديراً لتفوقه العلمي ومواظبته على أداء الواجبات والامتحانات بتفوق مستمر" }
        canvas.drawText(bodyMsg, 842f / 2f, 388f, bodyPaint.apply { textSize = 13f })
        canvas.drawText("متمنين له دوام النجاح والتميز والارتقاء إلى أعلى المراتب العلمية", 842f / 2f, 415f, bodyPaint)

        // 10. Golden Seal / Badge on Left/Center
        if (settings.showSeal) {
            drawSealStamp(canvas, 130f, 480f, settings.sealText, secondaryCol)
        }

        // 11. Date and Signatures
        val dateText = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = if (template == "dark_onyx_gold") secondaryCol else primaryCol
            textAlign = Paint.Align.CENTER
        }
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11.5f
            color = textSecondaryCol
            textAlign = Paint.Align.CENTER
        }

        // Date on Right
        canvas.drawText("تحريراً في: $dateText", 710f, 490f, datePaint)

        // Signature on Left-Center
        canvas.drawText("معلم المادة", 280f, 475f, footerPaint)
        canvas.drawText(settings.signatureName.ifEmpty { teacher?.name ?: "أستاذ المادة" }, 280f, 502f, titlePaint.apply { textSize = 14f })
    }

    // ==========================================
    // INTERNAL RENDERING: SINGLE STUDENT REPORT PAGE
    // ==========================================
    private fun renderSingleStudentReportPage(
        canvas: Canvas,
        teacher: TeacherEntity?,
        studentDetails: StudentWithDetails,
        grades: List<StudentGradeItem>,
        settings: ReportSettingEntity
    ) {
        val bgPaint = Paint().apply { color = Color.rgb(249, 250, 251) }
        canvas.drawRect(0f, 0f, 595f, 842f, bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(219, 234, 254)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(20f, 20f, 575f, 822f, 12f, 12f, borderPaint)

        // Top Banner
        val headerBanner = Paint().apply { color = Color.rgb(30, 58, 138) }
        canvas.drawRoundRect(20f, 20f, 575f, 95f, 12f, 12f, headerBanner)

        drawTeacherLogoOrEmblem(canvas, teacher, 55f, 58f, 24f)

        val whiteTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
        }
        val whiteSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f
            color = Color.rgb(224, 231, 255)
            textAlign = Paint.Align.CENTER
        }

        val centerText = teacher?.centerName?.ifEmpty { "أكاديمية التفوق التعليمية" } ?: "أكاديمية التفوق التعليمية"
        canvas.drawText(settings.headerTitle.ifEmpty { "تقرير متابعة الطالب الشامل" }, 595f / 2f + 10f, 52f, whiteTitle)
        canvas.drawText("${teacher?.name ?: "عبده أيمن"} | $centerText | ${SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())}", 595f / 2f + 10f, 75f, whiteSub)

        var y = 115f

        // Student Info Card
        val cardPaint = Paint().apply { color = Color.WHITE }
        canvas.drawRoundRect(35f, y, 560f, y + 95f, 8f, 8f, cardPaint)
        canvas.drawRoundRect(35f, y, 560f, y + 95f, 8f, 8f, borderPaint)

        val boldTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(17, 24, 39)
            textAlign = Paint.Align.RIGHT
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10.5f
            color = Color.rgb(31, 41, 55)
            textAlign = Paint.Align.RIGHT
        }

        if (settings.showStudentName) {
            canvas.drawText("اسم الطالب: ${studentDetails.student.name}", 545f, y + 25f, boldTextPaint)
        }
        if (settings.showGroup) {
            canvas.drawText("المجموعة: ${studentDetails.group?.name ?: "غير محدد"}", 350f, y + 25f, textPaint)
        }
        if (settings.showGrade) {
            canvas.drawText("الصف: ${studentDetails.student.grade}", 160f, y + 25f, textPaint)
        }
        if (settings.showPhone) {
            canvas.drawText("هاتف الطالب: ${studentDetails.student.phone.ifEmpty { "لا يوجد" }}", 545f, y + 52f, textPaint)
        }
        if (settings.showParentPhone) {
            canvas.drawText("هاتف ولي الأمر: ${studentDetails.student.parentPhone.ifEmpty { "لا يوجد" }}", 350f, y + 52f, textPaint)
        }
        if (settings.showStatus) {
            val st = if (studentDetails.student.status == "active") "منتظم" else "غير منتظم"
            canvas.drawText("حالة القيد: $st", 160f, y + 52f, textPaint)
        }
        if (settings.showAddress && studentDetails.student.address.isNotEmpty()) {
            canvas.drawText("العنوان: ${studentDetails.student.address}", 545f, y + 78f, textPaint)
        }

        y += 115f

        // Stats summary boxes
        val boxWidth = 160f
        val boxHeight = 52f
        val subTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10.5f
            color = Color.rgb(75, 85, 99)
            textAlign = Paint.Align.CENTER
        }

        // Attendance Box
        if (settings.showAttendance) {
            canvas.drawRoundRect(35f, y, 35f + boxWidth, y + boxHeight, 8f, 8f, cardPaint)
            canvas.drawRoundRect(35f, y, 35f + boxWidth, y + boxHeight, 8f, 8f, borderPaint)
            canvas.drawText("نسبة الحضور", 35f + boxWidth / 2, y + 18f, subTitlePaint)
            val attPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.rgb(16, 185, 129)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("${studentDetails.attendanceRate}%", 35f + boxWidth / 2, y + 39f, attPaint)
        }

        // Finance Box
        if (settings.showPayments) {
            val fX = 217f
            canvas.drawRoundRect(fX, y, fX + boxWidth, y + boxHeight, 8f, 8f, cardPaint)
            canvas.drawRoundRect(fX, y, fX + boxWidth, y + boxHeight, 8f, 8f, borderPaint)
            canvas.drawText("المبلغ المدفوع / المتبقي", fX + boxWidth / 2, y + 18f, subTitlePaint)
            val finPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.rgb(30, 58, 138)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("${studentDetails.totalPaid.toInt()} ج.م / ${studentDetails.remainingBalance.toInt()} ج.م", fX + boxWidth / 2, y + 39f, finPaint)
        }

        // Grades Average Box
        if (settings.showExams) {
            val gX = 400f
            canvas.drawRoundRect(gX, y, gX + boxWidth, y + boxHeight, 8f, 8f, cardPaint)
            canvas.drawRoundRect(gX, y, gX + boxWidth, y + boxHeight, 8f, 8f, borderPaint)
            canvas.drawText("متوسط درجات الامتحانات", gX + boxWidth / 2, y + 18f, subTitlePaint)
            val avgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.rgb(217, 119, 6)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("${"%.1f".format(studentDetails.averageScore)}%", gX + boxWidth / 2, y + 39f, avgPaint)
        }

        y += 75f

        // Exam Grades Detailed Table
        if (settings.showExams && grades.isNotEmpty()) {
            val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.rgb(30, 58, 138)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("سجل نتائج الامتحانات والاختبارات الشهرية:", 560f, y + 10f, tableHeaderPaint)
            y += 20f

            val thBg = Paint().apply { color = Color.rgb(238, 242, 255) }
            canvas.drawRect(35f, y, 560f, y + 24f, thBg)
            canvas.drawRect(35f, y, 560f, y + 24f, borderPaint)

            val thText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.rgb(30, 58, 138)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("التقدير", 80f, y + 16f, thText)
            canvas.drawText("النسبة", 180f, y + 16f, thText)
            canvas.drawText("الدرجة / العظمى", 300f, y + 16f, thText)
            canvas.drawText("اسم الامتحان", 480f, y + 16f, thText)
            y += 24f

            grades.take(8).forEach { item ->
                canvas.drawRect(35f, y, 560f, y + 22f, cardPaint)
                canvas.drawRect(35f, y, 560f, y + 22f, borderPaint)
                canvas.drawText(item.gradeTitle, 80f, y + 15f, textPaint.apply { textAlign = Paint.Align.CENTER })
                canvas.drawText("${item.percentage.toInt()}%", 180f, y + 15f, textPaint)
                canvas.drawText("${item.score} / ${item.maxScore}", 300f, y + 15f, textPaint)
                canvas.drawText("اختبار دوري", 480f, y + 15f, boldTextPaint.apply { textAlign = Paint.Align.CENTER })
                y += 22f
            }
        }

        // Footer Note
        val footerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f
            color = Color.rgb(107, 114, 128)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("هذا التقرير صادر إلكترونياً من منظومة هاكر التدريس (The Hacker) لمعلم المادة", 595f / 2, 800f, footerTextPaint)
    }

    // ==========================================
    // INTERNAL RENDERING: GROUP SUMMARY OVERVIEW
    // ==========================================
    private fun renderGroupSummaryPage(
        canvas: Canvas,
        teacher: TeacherEntity?,
        group: GroupEntity?,
        students: List<Pair<StudentWithDetails, List<StudentGradeItem>>>
    ) {
        val bgPaint = Paint().apply { color = Color.rgb(248, 250, 252) }
        canvas.drawRect(0f, 0f, 595f, 842f, bgPaint)

        // Banner
        val headerBanner = Paint().apply { color = Color.rgb(30, 58, 138) }
        canvas.drawRoundRect(20f, 20f, 575f, 100f, 12f, 12f, headerBanner)

        drawTeacherLogoOrEmblem(canvas, teacher, 55f, 60f, 25f)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(224, 231, 255)
            textSize = 11f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("الملف التجميعي الشامل للمجموعة", 595f / 2 + 10f, 50f, titlePaint)
        canvas.drawText("المجموعة: ${group?.name ?: "الكل"} | المعلم: ${teacher?.name ?: "عبده أيمن"} | عدد الطلاب: ${students.size}", 595f / 2 + 10f, 75f, subPaint)

        var y = 130f
        val cardPaint = Paint().apply { color = Color.WHITE }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(219, 234, 254)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        // Summary KPI Box
        canvas.drawRoundRect(35f, y, 560f, y + 80f, 8f, 8f, cardPaint)
        canvas.drawRoundRect(35f, y, 560f, y + 80f, 8f, 8f, borderPaint)

        val avgAtt = if (students.isNotEmpty()) students.map { it.first.attendanceRate }.average().toInt() else 100
        val avgScore = if (students.isNotEmpty()) students.map { it.first.averageScore }.average() else 0.0

        val kpiText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12f
            color = Color.rgb(30, 58, 138)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("متوسط الحضور العام: $avgAtt%", 140f, y + 45f, kpiText)
        canvas.drawText("متوسط درجات المجموعة: ${"%.1f".format(avgScore)}%", 420f, y + 45f, kpiText)

        y += 110f

        // Table of Students
        val thText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(30, 58, 138)
            textAlign = Paint.Align.CENTER
        }
        val cellText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f
            color = Color.rgb(31, 41, 55)
            textAlign = Paint.Align.CENTER
        }
        val nameText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.rgb(17, 24, 39)
            textAlign = Paint.Align.RIGHT
        }

        val rowH = 22f
        canvas.drawRect(35f, y, 560f, y + rowH, Paint().apply { color = Color.rgb(238, 242, 255) })
        canvas.drawText("ملاحظات", 70f, y + 15f, thText)
        canvas.drawText("المتوسط", 150f, y + 15f, thText)
        canvas.drawText("نسبة الحضور", 240f, y + 15f, thText)
        canvas.drawText("هاتف ولي الأمر", 360f, y + 15f, thText)
        canvas.drawText("اسم الطالب", 545f, y + 15f, thText.apply { textAlign = Paint.Align.RIGHT })
        y += rowH

        for ((details, _) in students.take(24)) {
            canvas.drawRect(35f, y, 560f, y + rowH, cardPaint)
            canvas.drawRect(35f, y, 560f, y + rowH, borderPaint)
            val st = if (details.student.status == "active") "منتظم" else "غير منتظم"
            canvas.drawText(st, 70f, y + 15f, cellText)
            canvas.drawText("${"%.1f".format(details.averageScore)}%", 150f, y + 15f, cellText)
            canvas.drawText("${details.attendanceRate}%", 240f, y + 15f, cellText)
            canvas.drawText(details.student.parentPhone.ifEmpty { "-" }, 360f, y + 15f, cellText)
            canvas.drawText(details.student.name, 545f, y + 15f, nameText)
            y += rowH
        }
    }

    // ==========================================
    // DRAW HELPER: PRESET EMBLEMS & SEALS
    // ==========================================
    private fun drawPresetEmblem(canvas: Canvas, cx: Float, cy: Float, type: String, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        when (type) {
            "trophy" -> {
                // Draw Trophy Icon
                val path = Path().apply {
                    moveTo(cx - 14f, cy - 10f)
                    lineTo(cx + 14f, cy - 10f)
                    lineTo(cx + 10f, cy + 6f)
                    lineTo(cx - 10f, cy + 6f)
                    close()
                }
                canvas.drawPath(path, paint)
                canvas.drawRect(cx - 3f, cy + 6f, cx + 3f, cy + 14f, paint)
                canvas.drawRect(cx - 10f, cy + 14f, cx + 10f, cy + 18f, paint)
                // Handles
                canvas.drawArc(RectF(cx - 20f, cy - 8f, cx - 10f, cy + 4f), 90f, 180f, false, strokePaint)
                canvas.drawArc(RectF(cx + 10f, cy - 8f, cx + 20f, cy + 4f), 270f, 180f, false, strokePaint)
            }
            "medal" -> {
                // Draw Medal / Ribbon
                val ribbonPath = Path().apply {
                    moveTo(cx - 8f, cy - 10f)
                    lineTo(cx, cy + 2f)
                    lineTo(cx + 8f, cy - 10f)
                }
                canvas.drawPath(ribbonPath, strokePaint)
                canvas.drawCircle(cx, cy + 4f, 10f, paint)
                drawStar(canvas, cx, cy + 4f, 5, 5f, 2.5f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = Color.WHITE })
            }
            "quill" -> {
                // Draw Quill / Pen
                val quillPath = Path().apply {
                    moveTo(cx - 10f, cy + 14f)
                    cubicTo(cx - 5f, cy + 5f, cx + 5f, cy - 5f, cx + 12f, cy - 14f)
                    cubicTo(cx + 8f, cy - 4f, cx + 2f, cy + 4f, cx - 10f, cy + 14f)
                }
                canvas.drawPath(quillPath, paint)
            }
            "book" -> {
                // Draw Open Book
                val leftPage = Path().apply {
                    moveTo(cx, cy + 10f)
                    lineTo(cx - 14f, cy + 6f)
                    lineTo(cx - 14f, cy - 8f)
                    lineTo(cx, cy - 4f)
                    close()
                }
                val rightPage = Path().apply {
                    moveTo(cx, cy + 10f)
                    lineTo(cx + 14f, cy + 6f)
                    lineTo(cx + 14f, cy - 8f)
                    lineTo(cx, cy - 4f)
                    close()
                }
                canvas.drawPath(leftPage, paint)
                canvas.drawPath(rightPage, paint)
            }
            else -> { // "crown" default
                val crownPath = Path().apply {
                    moveTo(cx - 16f, cy + 10f)
                    lineTo(cx + 16f, cy + 10f)
                    lineTo(cx + 14f, cy - 6f)
                    lineTo(cx + 6f, cy + 2f)
                    lineTo(cx, cy - 10f)
                    lineTo(cx - 6f, cy + 2f)
                    lineTo(cx - 14f, cy - 6f)
                    close()
                }
                canvas.drawPath(crownPath, paint)
                canvas.drawCircle(cx - 14f, cy - 7f, 2.5f, paint)
                canvas.drawCircle(cx, cy - 11f, 3f, paint)
                canvas.drawCircle(cx + 14f, cy - 7f, 2.5f, paint)
            }
        }
    }

    private fun drawSealStamp(canvas: Canvas, cx: Float, cy: Float, text: String, color: Int) {
        val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.argb(30, Color.red(color), Color.green(color), Color.blue(color))
            style = Paint.Style.FILL
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        // Draw Rosette / Serrated Sunburst Seal
        drawStar(canvas, cx, cy, 16, 26f, 21f, outerPaint)
        canvas.drawCircle(cx, cy, 20f, fillPaint)
        canvas.drawCircle(cx, cy, 20f, outerPaint.apply { strokeWidth = 1.2f })

        drawStar(canvas, cx, cy - 6f, 5, 4.5f, 2.2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
        canvas.drawText(text.ifEmpty { "اعتماد الأستاذ" }, cx, cy + 8f, textPaint)
    }

    // ==========================================
    // 7. WEEKLY SCHEDULE / TIMETABLE PDF EXPORT
    // ==========================================
    fun exportSchedulePdf(
        context: Context,
        sessionsWithGroups: List<com.example.data.repository.SessionWithGroup>,
        teacher: TeacherEntity?,
        selectedDay: String = "all"
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(842, 595, 1).create() // Landscape A4
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Background
        canvas.drawColor(Color.rgb(248, 250, 252))

        // Header Banner
        val headerPaint = Paint().apply { color = Color.rgb(30, 58, 138) }
        canvas.drawRoundRect(20f, 20f, 822f, 85f, 12f, 12f, headerPaint)

        // Logo / Emblem
        drawTeacherLogoOrEmblem(canvas, teacher, 55f, 52f, 24f)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(224, 231, 255)
            textSize = 11.5f
            textAlign = Paint.Align.CENTER
        }

        val dayTitle = if (selectedDay == "all") "جدول المواعيد والحصص الأسبوعي" else "جدول حصص يوم $selectedDay"
        canvas.drawText(dayTitle, 842f / 2f + 10f, 48f, titlePaint)
        canvas.drawText(
            "المعلم: ${teacher?.name?.ifEmpty { "الأستاذ عبده أيمن" } ?: "الأستاذ عبده أيمن"} | المادة: ${teacher?.subject?.ifEmpty { "جميع المواد" } ?: "جميع المواد"} | ${teacher?.centerName?.ifEmpty { "السنتر" } ?: "السنتر"} | تاريخ الطباعة: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}",
            842f / 2f + 10f,
            72f,
            subPaint
        )

        // Summary Stats Row
        val totalSessions = sessionsWithGroups.size
        val totalMinutes = sessionsWithGroups.sumOf { it.session.durationMinutes }
        val totalHours = String.format(Locale.US, "%.1f", totalMinutes / 60.0)
        val distinctGroups = sessionsWithGroups.map { it.groupName }.distinct().size

        val statBg = Paint().apply { color = Color.WHITE }
        val statBorder = Paint().apply {
            color = Color.rgb(226, 232, 240)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val statText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        canvas.drawRoundRect(20f, 95f, 822f, 125f, 8f, 8f, statBg)
        canvas.drawRoundRect(20f, 95f, 822f, 125f, 8f, 8f, statBorder)
        canvas.drawText(
            "📊 إجمالي الحصص: $totalSessions حصة  •  ⏱️ إجمالي الساعات: $totalHours ساعة  •  👥 عدد المجموعات: $distinctGroups مجموعة  •  📞 للتواصل: ${teacher?.phone ?: "01206150946"}",
            842f / 2f,
            113f,
            statText
        )

        // Table Headers
        var y = 140f
        val thBg = Paint().apply { color = Color.rgb(241, 245, 249) }
        val thText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 58, 138)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val borderPaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        canvas.drawRect(20f, y, 822f, y + 26f, thBg)
        canvas.drawRect(20f, y, 822f, y + 26f, borderPaint)

        // Columns: # (40) | اليوم (90) | المجموعة (180) | الموعد والمدة (130) | المقر / القاعة (140) | الواجب / الملاحظات (222)
        canvas.drawText("#", 40f, y + 17f, thText)
        canvas.drawText("اليوم", 100f, y + 17f, thText)
        canvas.drawText("اسم المجموعة", 230f, y + 17f, thText)
        canvas.drawText("الموعد والمدة", 385f, y + 17f, thText)
        canvas.drawText("المقر / القاعة", 520f, y + 17f, thText)
        canvas.drawText("الواجب وملاحظات الحصة", 700f, y + 17f, thText)

        // Sort by Day order then by time
        val dayOrder = listOf("السبت", "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة")
        val sortedSessions = sessionsWithGroups.sortedWith(
            compareBy<com.example.data.repository.SessionWithGroup> {
                val idx = dayOrder.indexOfFirst { d -> it.session.day.contains(d) }
                if (idx >= 0) idx else 99
            }.thenBy { it.session.time }
        )

        val rowText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 9.5f
            textAlign = Paint.Align.CENTER
        }
        val noteText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            textSize = 8.5f
            textAlign = Paint.Align.CENTER
        }

        y += 26f
        sortedSessions.forEachIndexed { index, item ->
            if (y > 540f) return@forEachIndexed

            val isEven = index % 2 == 0
            val rowBg = Paint().apply {
                color = if (isEven) Color.WHITE else Color.rgb(248, 250, 252)
            }
            canvas.drawRect(20f, y, 822f, y + 25f, rowBg)
            canvas.drawRect(20f, y, 822f, y + 25f, borderPaint)

            canvas.drawText("${index + 1}", 40f, y + 16f, rowText)
            canvas.drawText(item.session.day, 100f, y + 16f, rowText)
            canvas.drawText(item.groupName.take(24), 230f, y + 16f, rowText)
            canvas.drawText("${item.session.time} (${item.session.durationMinutes} د)", 385f, y + 16f, rowText)
            canvas.drawText(item.location.ifEmpty { item.session.location }.take(20), 520f, y + 16f, rowText)

            val hwDisplay = when {
                item.session.homeworkTitle.isNotEmpty() -> "واجب: ${item.session.homeworkTitle}"
                item.session.homeworkPages.isNotEmpty() -> "ص: ${item.session.homeworkPages}"
                item.session.note.isNotEmpty() -> item.session.note
                else -> "-"
            }
            canvas.drawText(hwDisplay.take(30), 700f, y + 16f, noteText)

            y += 25f
        }

        // Footer
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184)
            textSize = 8.5f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "تم إنشاء وطباعة الجدول بواسطة تطبيق إدارة وتنظيم المعلم • الأستاذ: ${teacher?.name?.ifEmpty { "عبده أيمن" } ?: "عبده أيمن"}",
            842f / 2f,
            575f,
            footerPaint
        )

        pdfDocument.finishPage(page)

        val outputDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "schedules")
        if (!outputDir.exists()) outputDir.mkdirs()
        val file = File(outputDir, "جدول_الحصص_${System.currentTimeMillis()}.pdf")

        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return file
    }

    // ==========================================
    // 8. HONOR ROLL / TOP ACHIEVERS POSTER PDF
    // ==========================================
    fun generateHonorRollPdf(
        context: Context,
        teacher: TeacherEntity?,
        groupName: String,
        leaderboardItems: List<LeaderboardItem>
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Portrait
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Luxury background
        canvas.drawColor(Color.rgb(250, 245, 255))

        // Gold decorative outer border
        val goldBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(217, 119, 6)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val innerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(124, 58, 237)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(20f, 20f, 575f, 822f, 16f, 16f, goldBorder)
        canvas.drawRoundRect(26f, 26f, 569f, 816f, 12f, 12f, innerBorder)

        // Top Header Banner
        val headerPaint = Paint().apply { color = Color.rgb(76, 29, 149) }
        canvas.drawRoundRect(35f, 35f, 560f, 115f, 12f, 12f, headerPaint)

        // Logo or Emblem
        drawTeacherLogoOrEmblem(canvas, teacher, 70f, 75f, 24f)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(254, 240, 138)
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 11.5f
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText("👑 لوحة شرف الأوائل والمتفوقين 👑", 595f / 2f + 10f, 68f, titlePaint)
        canvas.drawText(
            "المعلم: ${teacher?.name?.ifEmpty { "عبده أيمن" } ?: "عبده أيمن"} • ${groupName} • تاريخ الإصدار: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}",
            595f / 2f + 10f,
            96f,
            subPaint
        )

        // Table Header
        var y = 135f
        val thBg = Paint().apply { color = Color.rgb(237, 233, 254) }
        val thBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(196, 181, 253)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val thText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(76, 29, 149)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        canvas.drawRoundRect(35f, y, 560f, y + 30f, 6f, 6f, thBg)
        canvas.drawRoundRect(35f, y, 560f, y + 30f, 6f, 6f, thBorder)

        canvas.drawText("المركز", 65f, y + 20f, thText)
        canvas.drawText("اسم الطالب المتفوق", 220f, y + 20f, thText)
        canvas.drawText("المجموعة / الصف", 365f, y + 20f, thText)
        canvas.drawText("متوسط الدرجات", 460f, y + 20f, thText)
        canvas.drawText("الحضور", 525f, y + 20f, thText)

        y += 36f

        val rowText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 10.5f
            textAlign = Paint.Align.CENTER
        }
        val nameText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val topList = leaderboardItems.take(15)
        topList.forEachIndexed { idx, item ->
            val isEven = idx % 2 == 0
            val rowBg = Paint().apply {
                color = when (idx) {
                    0 -> Color.rgb(254, 249, 195) // Gold tint for 1st
                    1 -> Color.rgb(241, 245, 249) // Silver tint for 2nd
                    2 -> Color.rgb(255, 237, 213) // Bronze tint for 3rd
                    else -> if (isEven) Color.WHITE else Color.rgb(245, 243, 255)
                }
            }

            canvas.drawRoundRect(35f, y, 560f, y + 32f, 6f, 6f, rowBg)
            canvas.drawRoundRect(35f, y, 560f, y + 32f, 6f, 6f, thBorder)

            val rankSymbol = when (idx) {
                0 -> "🥇 1"
                1 -> "🥈 2"
                2 -> "🥉 3"
                else -> "${idx + 1}"
            }
            canvas.drawText(rankSymbol, 65f, y + 20f, nameText)
            canvas.drawText(item.student.name.take(25), 220f, y + 20f, nameText)
            canvas.drawText(item.groupName.take(18), 365f, y + 20f, rowText)
            canvas.drawText(String.format(Locale.US, "%.1f", item.averageScore), 460f, y + 20f, nameText)
            canvas.drawText("${item.attendanceRate}%", 525f, y + 20f, rowText)

            y += 36f
        }

        // Bottom Stamp & Congratulations Note
        val congratsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(76, 29, 149)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val phonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139)
            textSize = 9.5f
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText("مع أطيب تمنياتنا لجميع طلابنا بدوام التميز والتفوق الباهر 🌟", 595f / 2f, 755f, congratsPaint)
        canvas.drawText(
            "أستاذ المادة: ${teacher?.name?.ifEmpty { "عبده أيمن" } ?: "عبده أيمن"} • هاتف: ${teacher?.phone?.ifEmpty { "01206150946" } ?: "01206150946"}",
            595f / 2f,
            775f,
            phonePaint
        )

        drawSealStamp(canvas, 500f, 750f, "لوحة الشرف", Color.rgb(217, 119, 6))

        pdfDocument.finishPage(page)

        val outputDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "honor_roll")
        if (!outputDir.exists()) outputDir.mkdirs()
        val file = File(outputDir, "لوحة_شرف_${System.currentTimeMillis()}.pdf")

        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return file
    }

    private fun drawStar(canvas: Canvas, cx: Float, cy: Float, spikes: Int, outerRadius: Float, innerRadius: Float, paint: Paint) {
        val path = Path()
        val step = Math.PI / spikes
        var rot = -Math.PI / 2
        var x = cx + cos(rot).toFloat() * outerRadius
        var y = cy + sin(rot).toFloat() * outerRadius
        path.moveTo(x, y)

        for (i in 0 until spikes) {
            x = cx + cos(rot).toFloat() * outerRadius
            y = cy + sin(rot).toFloat() * outerRadius
            path.lineTo(x, y)
            rot += step

            x = cx + cos(rot).toFloat() * innerRadius
            y = cy + sin(rot).toFloat() * innerRadius
            path.lineTo(x, y)
            rot += step
        }
        path.lineTo(cx + cos(-Math.PI / 2).toFloat() * outerRadius, cy + sin(-Math.PI / 2).toFloat() * outerRadius)
        path.close()
        canvas.drawPath(path, paint)
    }

    // ==========================================
    // 16. TEACHER PROFESSIONAL PORTFOLIO & PROFILE FLYER PDF (A4)
    // ==========================================
    fun generateTeacherPortfolioPdf(context: Context, teacher: TeacherEntity): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Portrait
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // 1. Background
        val bgPaint = Paint().apply { color = Color.rgb(253, 254, 255) }
        canvas.drawRect(0f, 0f, 595f, 842f, bgPaint)

        // 2. Luxury Double Border with Gold Accents
        val outerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 58, 138) // Deep Navy
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val innerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(217, 119, 6) // Gold
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(18f, 18f, 577f, 824f, 16f, 16f, outerBorder)
        canvas.drawRoundRect(24f, 24f, 571f, 818f, 12f, 12f, innerBorder)

        // 3. Header Royal Banner
        val bannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(15, 23, 42) }
        val bannerPath = Path().apply {
            moveTo(24f, 36f)
            lineTo(571f, 36f)
            lineTo(571f, 155f)
            quadTo(297.5f, 175f, 24f, 155f)
            close()
        }
        canvas.drawPath(bannerPath, bannerPaint)

        // Gold Ribbon Accent
        val goldRibbon = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(217, 119, 6)
            strokeWidth = 2.5f
            style = Paint.Style.STROKE
        }
        val ribbonPath = Path().apply {
            moveTo(30f, 155f)
            quadTo(297.5f, 175f, 565f, 155f)
        }
        canvas.drawPath(ribbonPath, goldRibbon)

        // Center / Academy Title
        val centerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(253, 230, 138) // Soft Gold
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(teacher.centerName.ifEmpty { "أكاديمية التفوق التعليمية للمراحل الدراسية" }, 297.5f, 58f, centerTitlePaint)

        // Teacher Name
        val teacherNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("الأستاذ / ${teacher.name}", 297.5f, 94f, teacherNamePaint)

        // Teacher Title & Subject
        val teacherSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(224, 231, 255)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subText = "${teacher.title.ifEmpty { "أستاذ المادة والمشرف الأكاديمي" }} • ${teacher.subject}"
        canvas.drawText(subText, 297.5f, 120f, teacherSubPaint)

        val expBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(217, 119, 6)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("⭐ ${teacher.experienceYears.ifEmpty { "خبرة سنوات طويلة من التميز والتفوق" }} ⭐", 297.5f, 142f, expBadgePaint)

        // Draw Teacher Logo/Avatar in Header
        drawTeacherLogoOrEmblem(canvas, teacher, 70f, 95f, 28f)

        // 4. Bio / Vision Quotation Box
        val bioBoxBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(241, 245, 249) }
        val bioBoxBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(40f, 190f, 555f, 255f, 10f, 10f, bioBoxBg)
        canvas.drawRoundRect(40f, 190f, 555f, 255f, 10f, 10f, bioBoxBorder)

        val quoteTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 58, 138)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("💡 الرؤية والرسالة التعليمية:", 540f, 212f, quoteTitlePaint)

        val quoteBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(51, 65, 85)
            textSize = 9.5f
            textAlign = Paint.Align.RIGHT
        }
        val bioText = teacher.bio.ifEmpty { "نهدف إلى تقديم تجربة تعليمية فريدة ترتكز على الفهم العميق والتدريب المستمر وتأهيل الطلاب لتحقيق أعلى الدرجات والتفوق الدراسي." }
        canvas.drawText(bioText.take(110), 540f, 230f, quoteBodyPaint)
        if (bioText.length > 110) {
            canvas.drawText(bioText.substring(110).take(110), 540f, 245f, quoteBodyPaint)
        }

        // 5. Grid of Cards: Stages & Qualifications (Right Side) vs Contact & QR (Left Side)
        // Left Column (QR & Direct Connect): X = 40 to 220
        // Right Column (Details, Stages, System): X = 235 to 555

        // ================= RIGHT COLUMN =================
        // Box 1: Stages & Curricula (المراحل والمناهج)
        val cardBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val cardBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240)
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        canvas.drawRoundRect(235f, 270f, 555f, 385f, 10f, 10f, cardBg)
        canvas.drawRoundRect(235f, 270f, 555f, 385f, 10f, 10f, cardBorder)

        val sectionHeaderBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(30, 58, 138) }
        canvas.drawRoundRect(235f, 270f, 555f, 298f, 10f, 10f, sectionHeaderBg)
        val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("📚 المراحل والمناهج الدراسية المعتمدة", 540f, 289f, sectionTitlePaint)

        val itemLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val itemValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            textSize = 9f
            textAlign = Paint.Align.RIGHT
        }

        canvas.drawText("• المراحل:", 540f, 320f, itemLabelPaint)
        canvas.drawText(teacher.stagesTaught.ifEmpty { "المرحلة الثانوية والإعدادية - عربي ولغات" }, 490f, 320f, itemValPaint)

        canvas.drawText("• المادة:", 540f, 342f, itemLabelPaint)
        canvas.drawText("${teacher.subject} - شرح مكثف وحل بنوك أسئلة", 490f, 342f, itemValPaint)

        canvas.drawText("• المؤهلات:", 540f, 364f, itemLabelPaint)
        canvas.drawText(teacher.degrees.ifEmpty { "ليسانس/بكالوريوس ودبلوم مهني في طرق التدريس" }, 490f, 364f, itemValPaint)

        // Box 2: System Features (نظام المتابعة والتفوق الأكاديمي)
        canvas.drawRoundRect(235f, 400f, 555f, 545f, 10f, 10f, cardBg)
        canvas.drawRoundRect(235f, 400f, 555f, 545f, 10f, 10f, cardBorder)
        canvas.drawRoundRect(235f, 400f, 555f, 428f, 10f, 10f, sectionHeaderBg)
        canvas.drawText("🏆 مميزات نظام التدريس والمتابعة المستمرة", 540f, 419f, sectionTitlePaint)

        val features = listOf(
            "✔️ متابعة دورية أسبوعية وتسجيل دقيق للحضور والغياب",
            "✔️ امتحانات شهرية واختبارات قصيرة مع تصحيح وتحليل درجات",
            "✔️ إرسال تقارير إلكترونية دورية لولي الأمر عبر الواتساب",
            "✔️ بنك أسئلة شامل ونماذج امتحانات مطابقة للمواصفات",
            "✔️ تكريم وتشجيع الطلاب المتفوقين وتوزيع شهادات تقدير"
        )
        var featureY = 448f
        for (feat in features) {
            canvas.drawText(feat, 540f, featureY, itemValPaint)
            featureY += 20f
        }

        // Box 3: Centers & Venues (أماكن ومواعيد المجموعات)
        canvas.drawRoundRect(235f, 560f, 555f, 690f, 10f, 10f, cardBg)
        canvas.drawRoundRect(235f, 560f, 555f, 690f, 10f, 10f, cardBorder)
        canvas.drawRoundRect(235f, 560f, 555f, 588f, 10f, 10f, sectionHeaderBg)
        canvas.drawText("📍 مقرات السناتر والمجموعات الدراسية", 540f, 579f, sectionTitlePaint)

        canvas.drawText("• المركز الرئيسي: ${teacher.centerName.ifEmpty { "سنتر التفوق والتميز" }}", 540f, 610f, itemLabelPaint)
        canvas.drawText("• العنوان: ${teacher.address.ifEmpty { "شارع الجمهورية - الدور الثالث" }}", 540f, 632f, itemValPaint)
        canvas.drawText("• الحصص والمجموعات: مواعيد صباحية ومسائية تناسب الجميع", 540f, 654f, itemValPaint)
        canvas.drawText("• قاعات مكيفة ومجهزة بأحدث الوسائل التعليمية والشاشات", 540f, 676f, itemValPaint)


        // ================= LEFT COLUMN =================
        // QR Code & Contact Card (X = 40 to 220)
        canvas.drawRoundRect(40f, 270f, 220f, 545f, 10f, 10f, cardBg)
        canvas.drawRoundRect(40f, 270f, 220f, 545f, 10f, 10f, cardBorder)

        val qrHeaderBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(217, 119, 6) }
        canvas.drawRoundRect(40f, 270f, 220f, 298f, 10f, 10f, qrHeaderBg)
        val qrHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("📱 تواصل فوري مباشر", 130f, 289f, qrHeaderPaint)

        // Generate High-Res Scannable QR Code leading to WhatsApp or Contact info
        val whatsappClean = teacher.whatsapp.ifEmpty { teacher.phone }.replace("+", "").replace(" ", "")
        val qrPayload = if (whatsappClean.isNotEmpty()) {
            val waNum = if (whatsappClean.startsWith("0")) "2$whatsappClean" else whatsappClean
            "https://wa.me/$waNum?text=${Uri.encode("السلام عليكم يا مستر ${teacher.name}، أود الاستفسار عن تفاصيل ومواعيد المجموعات الدراسية.")}"
        } else {
            "MECARD:N:${teacher.name};TEL:${teacher.phone};NOTE:${teacher.subject};;"
        }

        try {
            val qrBitmap = com.example.util.QrBarcodeUtils.generateQrBitmap(
                content = qrPayload,
                size = 300,
                darkColor = Color.rgb(15, 23, 42),
                lightColor = Color.WHITE
            )
            val destRect = RectF(55f, 310f, 205f, 460f)
            canvas.drawBitmap(qrBitmap, null, destRect, null)
        } catch (e: Exception) {
            val qrFallback = Paint().apply { color = Color.rgb(30, 58, 138) }
            canvas.drawRect(55f, 310f, 205f, 460f, qrFallback)
        }

        val scanInstructionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 58, 138)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("امسح الـ QR بكاميرا هاتفك", 130f, 478f, scanInstructionPaint)
        val scanSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139)
            textSize = 7.5f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("للمحادثة الفورية وحفظ الرقم", 130f, 492f, scanSubPaint)

        // Contact Items
        canvas.drawRoundRect(40f, 560f, 220f, 690f, 10f, 10f, cardBg)
        canvas.drawRoundRect(40f, 560f, 220f, 690f, 10f, 10f, cardBorder)
        canvas.drawRoundRect(40f, 560f, 220f, 588f, 10f, 10f, sectionHeaderBg)
        canvas.drawText("📞 أرقام التواصل", 130f, 579f, qrHeaderPaint)

        val contactTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val contactVal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 58, 138)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText("الهاتف والاتصال المباشر:", 130f, 610f, contactTitle)
        canvas.drawText(teacher.phone.ifEmpty { "01206150946" }, 130f, 626f, contactVal)

        canvas.drawText("واتساب الاستفسارات:", 130f, 650f, contactTitle)
        canvas.drawText(teacher.whatsapp.ifEmpty { teacher.phone }, 130f, 666f, contactVal)


        // 6. Bottom Seal & Official Signature Ribbon
        val bottomRibbonBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(15, 23, 42) }
        canvas.drawRoundRect(40f, 710f, 555f, 800f, 12f, 12f, bottomRibbonBg)

        // Gold Seal
        val sealPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(217, 119, 6)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawCircle(95f, 755f, 28f, sealPaint)
        canvas.drawCircle(95f, 755f, 24f, sealPaint)
        val sealTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(253, 230, 138)
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("معتمد رسمياً", 95f, 752f, sealTextPaint)
        canvas.drawText("جودة وتميز", 95f, 763f, sealTextPaint)

        val sloganBottomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("✨ معاً نبني قادة المستقبل ونصنع طريق التفوق بإذن الله ✨", 535f, 742f, sloganBottomPaint)

        val signBottomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            textSize = 9f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("الأستاذ / ${teacher.name} • ${teacher.subject} • أكاديمية التفوق", 535f, 765f, signBottomPaint)
        canvas.drawText("تاريخ الإصدار: ${SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())}", 535f, 782f, signBottomPaint)

        pdfDocument.finishPage(page)

        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val exportDir = File(context.cacheDir, "teacher_profile")
        if (!exportDir.exists()) exportDir.mkdirs()
        val profileFile = File(exportDir, "Teacher_Profile_${teacher.name.replace(" ", "_")}_$dateStr.pdf")

        FileOutputStream(profileFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()
        return profileFile
    }

    // ==========================================
    // 17. TEACHER BUSINESS CARDS SHEET (8 CARDS PER A4 PAGE)
    // ==========================================
    fun generateTeacherBusinessCardsSheetPdf(context: Context, teacher: TeacherEntity): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Portrait
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawRect(0f, 0f, 595f, 842f, Paint().apply { color = Color.WHITE })

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            textSize = 9.5f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("كروت وبطاقات عمل المعلم الشخصية - ${teacher.name} - قابلة للقص والتوزيع", 595f / 2, 20f, headerPaint)

        val marginX = 25f
        val marginY = 32f
        val cardWidth = 260f
        val cardHeight = 185f
        val gapX = 25f
        val gapY = 16f

        // QR Code Payload
        val whatsappClean = teacher.whatsapp.ifEmpty { teacher.phone }.replace("+", "").replace(" ", "")
        val waNum = if (whatsappClean.startsWith("0")) "2$whatsappClean" else whatsappClean
        val qrPayload = "https://wa.me/$waNum?text=${Uri.encode("السلام عليكم يا مستر ${teacher.name}، أود الاستفسار عن تفاصيل الحصص والمجموعات.")}"

        var qrBitmap: Bitmap? = null
        try {
            qrBitmap = com.example.util.QrBarcodeUtils.generateQrBitmap(
                content = qrPayload,
                size = 200,
                darkColor = Color.rgb(15, 23, 42),
                lightColor = Color.WHITE
            )
        } catch (_: Exception) {}

        for (i in 0 until 8) {
            val col = i % 2
            val row = i / 2
            val left = marginX + col * (cardWidth + gapX)
            val top = marginY + row * (cardHeight + gapY)
            val right = left + cardWidth
            val bottom = top + cardHeight

            // Dashed Cut Line
            val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(203, 213, 225)
                style = Paint.Style.STROKE
                strokeWidth = 1f
                pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
            }
            canvas.drawRect(left - 2f, top - 2f, right + 2f, bottom + 2f, dashPaint)

            // Card Background & Borders
            val cardBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
            val cardBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 58, 138)
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
            }
            canvas.drawRoundRect(left, top, right, bottom, 10f, 10f, cardBg)
            canvas.drawRoundRect(left, top, right, bottom, 10f, 10f, cardBorder)

            // Top Navy Banner
            val topBanner = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(15, 23, 42) }
            val bannerPath = Path().apply {
                moveTo(left, top + 10f)
                quadTo(left, top, left + 10f, top)
                lineTo(right - 10f, top)
                quadTo(right, top, right, top + 10f)
                lineTo(right, top + 36f)
                lineTo(left, top + 36f)
                close()
            }
            canvas.drawPath(bannerPath, topBanner)

            // Gold Stripe
            val goldStripe = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(217, 119, 6)
                strokeWidth = 1.5f
            }
            canvas.drawLine(left, top + 36f, right, top + 36f, goldStripe)

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(253, 230, 138)
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(teacher.centerName.ifEmpty { "أكاديمية التفوق التعليمية" }, left + cardWidth / 2, top + 16f, titlePaint)

            val nameTop = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("الأستاذ / ${teacher.name} • ${teacher.subject}", left + cardWidth / 2, top + 30f, nameTop)

            // QR Code on Left
            if (qrBitmap != null) {
                val qrRect = RectF(left + 12f, top + 46f, left + 78f, top + 112f)
                canvas.drawBitmap(qrBitmap, null, qrRect, null)
                val qrBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(203, 213, 225)
                    style = Paint.Style.STROKE
                    strokeWidth = 0.8f
                }
                canvas.drawRoundRect(left + 10f, top + 44f, left + 80f, top + 114f, 4f, 4f, qrBorder)

                val qrText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(30, 58, 138)
                    textSize = 6.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("امسح للواتساب", left + 45f, top + 124f, qrText)
            }

            // Info on Right
            val infoRight = right - 12f
            val nameMain = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(teacher.name, infoRight, top + 56f, nameMain)

            val subMain = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(217, 119, 6)
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("${teacher.title.ifEmpty { "أستاذ المادة والمشرف" }} (${teacher.subject})", infoRight, top + 70f, subMain)

            val lbl = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(71, 85, 105)
                textSize = 7.5f
                textAlign = Paint.Align.RIGHT
            }
            val valTxt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 58, 138)
                textSize = 8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }

            canvas.drawText("هاتف: ${teacher.phone}", infoRight, top + 86f, valTxt)
            canvas.drawText("واتساب: ${teacher.whatsapp}", infoRight, top + 99f, valTxt)
            canvas.drawText("المراحل: ${teacher.stagesTaught.ifEmpty { "ثانوي وإعدادي" }}", infoRight, top + 112f, lbl)
            canvas.drawText("السنتر: ${teacher.address.ifEmpty { "السنتر الرئيسي" }}", infoRight, top + 125f, lbl)

            // Bottom Ribbon
            val btm = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(241, 245, 249) }
            val btmPath = Path().apply {
                moveTo(left, bottom - 22f)
                lineTo(right, bottom - 22f)
                lineTo(right, bottom - 10f)
                quadTo(right, bottom, right - 10f, bottom)
                lineTo(left + 10f, bottom)
                quadTo(left, bottom, left, bottom - 10f)
                close()
            }
            canvas.drawPath(btmPath, btm)
            canvas.drawLine(left, bottom - 22f, right, bottom - 22f, cardBorder.apply { strokeWidth = 0.6f })

            val btmTxt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(71, 85, 105)
                textSize = 7f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("✨ نحو القمة والتفوق بإذن الله • احجز مقعدك الآن ✨", left + cardWidth / 2, bottom - 8f, btmTxt)
        }

        pdfDocument.finishPage(page)

        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val exportDir = File(context.cacheDir, "teacher_cards")
        if (!exportDir.exists()) exportDir.mkdirs()
        val cardsFile = File(exportDir, "Teacher_Business_Cards_${teacher.name.replace(" ", "_")}_$dateStr.pdf")

        FileOutputStream(cardsFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()
        return cardsFile
    }

    fun generateAppFeaturePosterPdf(
        context: Context,
        teacher: TeacherEntity?
    ): File {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Background
        val bgPaint = Paint().apply { color = Color.rgb(248, 250, 252) }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

        // Top Gradient / Header Banner
        val topBanner = Paint().apply {
            shader = LinearGradient(
                0f, 0f, pageWidth.toFloat(), 130f,
                Color.rgb(15, 23, 42), Color.rgb(30, 58, 138),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 125f, topBanner)

        // Gold Accent Line
        val goldLine = Paint().apply {
            color = Color.rgb(217, 119, 6)
            strokeWidth = 3.5f
        }
        canvas.drawLine(0f, 125f, pageWidth.toFloat(), 125f, goldLine)

        // Teacher / App Title in Banner
        val teacherName = teacher?.name?.ifEmpty { "أستاذ المادة" } ?: "معلم المستقبل"
        val subject = teacher?.subject?.ifEmpty { "جميع المراحل التعليمية" } ?: "المادة التخصصية"

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("المنظومة التعليمية الرقمية لإدارة الدروس والطلاب", pageWidth - 30f, 40f, titlePaint)

        val subTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(253, 230, 138) // Light gold
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("إشراف: $teacherName - أستاذ ($subject)", pageWidth - 30f, 62f, subTitlePaint)

        val contactBannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240)
            textSize = 9.5f
            textAlign = Paint.Align.RIGHT
        }
        val phoneStr = if (!teacher?.phone.isNullOrEmpty()) "هاتف: ${teacher?.phone}  |  واتساب: ${teacher?.whatsapp}" else "نظام إلكتروني ذكي للمتابعة والتقييم الدوري"
        canvas.drawText(phoneStr, pageWidth - 30f, 82f, contactBannerPaint)

        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(217, 119, 6)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("✨ دقة • تنظيم • متابعة فورية مع أولياء الأمور • أداء احترافي ✨", pageWidth - 30f, 102f, badgePaint)

        // Teacher Logo/Emblem on Left Header
        drawTeacherLogoOrEmblem(canvas, teacher, 65f, 62f, 38f)

        // Subheader banner
        val subHeadBg = Paint().apply { color = Color.rgb(238, 242, 255) }
        canvas.drawRoundRect(24f, 135f, pageWidth - 24f, 168f, 8f, 8f, subHeadBg)
        val subHeadText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 58, 138)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("🚀 أقوى نظام تكنولوجي متكامل لإدارة الحصص، الحضور، الامتحانات والتقارير الذكية", pageWidth / 2f, 155f, subHeadText)

        // 8 Feature Cards Grid (4 rows x 2 cols)
        data class FeatureCard(
            val title: String,
            val badge: String,
            val points: List<String>,
            val color: Int
        )

        val features = listOf(
            FeatureCard(
                title = "١. نظام الباركود وكروت الهوية الذكية",
                badge = "QR & BARCODE",
                points = listOf(
                    "• مسح فوري بالهاتف مع فلاش وصوت اهتزاز",
                    "• توليد وطباعة بطاقات ID بكود استجابة سريع",
                    "• تحضير ذكي دون تكرار أو تأخير"
                ),
                color = Color.rgb(16, 185, 129) // Emerald
            ),
            FeatureCard(
                title = "٢. الحضور والغياب ومتابعة الواجبات",
                badge = "ATTENDANCE & HW",
                points = listOf(
                    "• رصد الحضور بنقرة زر (حاضر / غائب / متأخر)",
                    "• تقييم الواجب (كامل / ناقص / لم يحل)",
                    "• إنذار غياب فوري لأولياء الأمور بالواتساب"
                ),
                color = Color.rgb(59, 130, 246) // Blue
            ),
            FeatureCard(
                title = "٣. إدارة المجموعات، القاعات والسناتر",
                badge = "GROUPS & VENUES",
                points = listOf(
                    "• تنظيم الطلاب حسب المراحل والمجموعات",
                    "• حساب تكلفة إيجارات السناتر تلقائياً",
                    "• روابط مباشرة لمجموعات واتساب الطلاب"
                ),
                color = Color.rgb(139, 92, 246) // Purple
            ),
            FeatureCard(
                title = "٤. جدول المواعيد الأسبوعية الذكي",
                badge = "WEEKLY SCHEDULE",
                points = listOf(
                    "• عرض منظم لجميع مواعيد وحصص الأسبوع",
                    "• عداد الحصص والطلاب لكل يوم دراسي",
                    "• كشف وفحص تعارض المواعيد والقاعات"
                ),
                color = Color.rgb(245, 158, 11) // Amber
            ),
            FeatureCard(
                title = "٥. تقارير PDF وشهادات التقدير",
                badge = "REPORTS & HONORS",
                points = listOf(
                    "• كشوف درجات شهرية وتراكمية وتصدير Excel",
                    "• تصميم وطباعة شهادات تقدير احترافية بالألوان",
                    "• لوحة شرف تكريم الأوائل والمتفوقين"
                ),
                color = Color.rgb(236, 72, 153) // Pink
            ),
            FeatureCard(
                title = "٦. الإدارة المالية والأرباح والمصروفات",
                badge = "FINANCIAL CONTROL",
                points = listOf(
                    "• تحصيل اشتراكات الشهور والحصص والمذكرات",
                    "• جرد المتأخرات والديون والاشتراكات غير المسددة",
                    "• حساب صافي الأرباح وتقارير الإيرادات"
                ),
                color = Color.rgb(20, 184, 166) // Teal
            ),
            FeatureCard(
                title = "٧. بنك الأسئلة والتحضير الذكي للدروس",
                badge = "SMART LESSONS",
                points = listOf(
                    "• إعداد خطط الدروس والواجبات والملاحظات الصوتية",
                    "• بنك أسئلة واختبارات متنوعة جاهزة للطباعة",
                    "• توزيع الدرجات وإصدار أوراق امتحانات منسقة"
                ),
                color = Color.rgb(99, 102, 241) // Indigo
            ),
            FeatureCard(
                title = "٨. الأمان والنسخ الاحتياطي ومشاركة البروفايل",
                badge = "SECURITY & PROFILE",
                points = listOf(
                    "• قفل التطبيق برقم سري وبصمة لخصوصية البيانات",
                    "• نسخ احتياطي واسترجاع فوري لقاعدة البيانات",
                    "• كارت شخصي تسويقي للمدرس للمشاركة والطباعة"
                ),
                color = Color.rgb(30, 58, 138) // Navy
            )
        )

        val cardMargin = 24f
        val colGap = 12f
        val cardWidth = (pageWidth - (cardMargin * 2) - colGap) / 2f
        val cardHeight = 135f
        val startY = 178f
        val rowGap = 10f

        val cardBg = Paint().apply { color = Color.WHITE }
        val cardStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        features.forEachIndexed { index, feat ->
            val col = index % 2
            val row = index / 2
            val left = if (col == 0) pageWidth - cardMargin - cardWidth else cardMargin
            val right = left + cardWidth
            val top = startY + row * (cardHeight + rowGap)
            val bottom = top + cardHeight

            // Draw Card shadow/bg
            canvas.drawRoundRect(left, top, right, bottom, 10f, 10f, cardBg)
            canvas.drawRoundRect(left, top, right, bottom, 10f, 10f, cardStroke)

            // Top header band of card
            val bandPaint = Paint().apply { color = feat.color }
            val bandPath = Path().apply {
                moveTo(left, top + 8f)
                quadTo(left, top, left + 8f, top)
                lineTo(right - 8f, top)
                quadTo(right, top, right, top + 8f)
                lineTo(right, top + 26f)
                lineTo(left, top + 26f)
                close()
            }
            canvas.drawPath(bandPath, bandPaint)

            // Card Badge (English)
            val badgeTxt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 7f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText(feat.badge, left + 8f, top + 17f, badgeTxt)

            // Card Title (Arabic)
            val cardTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(feat.title, right - 8f, top + 17f, cardTitlePaint)

            // Bullet points
            val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(51, 65, 85)
                textSize = 8.5f
                textAlign = Paint.Align.RIGHT
            }

            feat.points.forEachIndexed { pIdx, point ->
                canvas.drawText(point, right - 10f, top + 48f + (pIdx * 24f), pointPaint)
            }
        }

        // Bottom Footer Bar
        val footerTop = pageHeight - 75f
        val footerBg = Paint().apply {
            shader = LinearGradient(
                0f, footerTop, pageWidth.toFloat(), pageHeight.toFloat(),
                Color.rgb(30, 58, 138), Color.rgb(15, 23, 42),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, footerTop, pageWidth.toFloat(), pageHeight.toFloat(), footerBg)

        // Footer Gold Line
        canvas.drawLine(0f, footerTop, pageWidth.toFloat(), footerTop, goldLine)

        // Footer Info Text
        val footerMainText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("🌟 منظومة تعليمية حديثة تواكب أحدث المعايير الرقمية لضمان تفوق الطالب وراحة ولي الأمر 🌟", pageWidth - 24f, footerTop + 24f, footerMainText)

        val footerSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240)
            textSize = 8.5f
            textAlign = Paint.Align.RIGHT
        }
        val tPhone = teacher?.phone.orEmpty()
        val tWhatsapp = teacher?.whatsapp.orEmpty()
        val contactLine = if (tPhone.isNotEmpty()) "للتواصل والحجز والاستفسار: هاتف: $tPhone  |  واتساب: $tWhatsapp" else "تطبيق المعلم المساعد الذكي لإدارة الحصص والمجموعات"
        canvas.drawText(contactLine, pageWidth - 24f, footerTop + 42f, footerSub)

        val copyRight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184)
            textSize = 7.5f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("جميع الحقوق محفوظة • مصمم خصيصاً لأفضل تجربة تدريس ومتابعة أكاديمية", pageWidth - 24f, footerTop + 58f, copyRight)

        // QR Code in Footer on Left
        val qrTarget = if (!teacher?.whatsapp.isNullOrEmpty()) "https://wa.me/20${teacher?.whatsapp?.removePrefix("0")}" else "https://wa.me"
        try {
            val qrBmp = com.example.util.QrBarcodeUtils.generateQrBitmap(qrTarget, 160)
            val srcRect: android.graphics.Rect? = null
            val dstRect = RectF(24f, footerTop + 8f, 24f + 58f, footerTop + 66f)
            canvas.drawBitmap(qrBmp, srcRect, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))
        } catch (_: Exception) {}

        pdfDocument.finishPage(page)

        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val exportDir = File(context.cacheDir, "app_posters")
        if (!exportDir.exists()) exportDir.mkdirs()
        val posterFile = File(exportDir, "App_Features_Poster_${teacherName.replace(" ", "_")}_$dateStr.pdf")

        FileOutputStream(posterFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()
        return posterFile
    }

    // ==========================================
    // 8. PAYMENT INVOICE / RECEIPT PDF
    // ==========================================
    fun generatePaymentReceiptPdf(
        context: Context,
        teacher: TeacherEntity?,
        student: StudentEntity,
        group: GroupEntity?,
        payment: PaymentEntity,
        remainingBalance: Double = 0.0
    ): File {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create() // A4 Portrait
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Background
        val bgPaint = Paint().apply { color = Color.rgb(248, 250, 252) }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

        // Outer Decorative Border Frame
        val outerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        canvas.drawRoundRect(20f, 20f, pageWidth - 20f, pageHeight - 20f, 16f, 16f, outerBorder)

        val innerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240)
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        canvas.drawRoundRect(25f, 25f, pageWidth - 25f, pageHeight - 25f, 12f, 12f, innerBorder)

        // Top Gradient Header
        val headerHeight = 115f
        val headerPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, pageWidth.toFloat(), headerHeight,
                Color.rgb(15, 23, 42), Color.rgb(30, 58, 138),
                Shader.TileMode.CLAMP
            )
        }
        val headerPath = Path().apply {
            moveTo(25f, 35f)
            quadTo(25f, 25f, 35f, 25f)
            lineTo(pageWidth - 35f, 25f)
            quadTo(pageWidth - 25f, 25f, pageWidth - 25f, 35f)
            lineTo(pageWidth - 25f, headerHeight)
            lineTo(25f, headerHeight)
            close()
        }
        canvas.drawPath(headerPath, headerPaint)

        // Gold Stripe under Header
        val goldLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(217, 119, 6)
            strokeWidth = 3f
        }
        canvas.drawLine(25f, headerHeight, pageWidth - 25f, headerHeight, goldLine)

        // Header Title & Teacher Details
        val centerName = teacher?.centerName?.ifEmpty { "أكاديمية التفوق التعليمية" } ?: "أكاديمية التفوق"
        val teacherName = teacher?.name?.ifEmpty { "عبده أيمن" } ?: "عبده أيمن"
        val subject = teacher?.subject?.ifEmpty { "جميع المواد الدراسية" } ?: "المادة الدراسية"

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(centerName, pageWidth - 45f, 55f, titlePaint)

        val subTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(253, 230, 138)
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("إشراف الأستاذ: $teacherName • $subject", pageWidth - 45f, 75f, subTitlePaint)

        val phonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240)
            textSize = 9f
            textAlign = Paint.Align.RIGHT
        }
        val phoneInfo = if (!teacher?.phone.isNullOrEmpty()) "هاتف: ${teacher?.phone} | واتساب: ${teacher?.whatsapp}" else "نظام الفواتير والتحصيل المالي الإلكتروني"
        canvas.drawText(phoneInfo, pageWidth - 45f, 95f, phonePaint)

        // Logo or Emblem on the Left side of header
        drawTeacherLogoOrEmblem(canvas, teacher, 75f, 70f, 28f)

        // Invoice Title Badge
        val badgeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val badgeBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 58, 138)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        val bTop = 135f
        val bBottom = 175f
        val bLeft = pageWidth / 2f - 140f
        val bRight = pageWidth / 2f + 140f
        canvas.drawRoundRect(bLeft, bTop, bRight, bBottom, 20f, 20f, badgeBg)
        canvas.drawRoundRect(bLeft, bTop, bRight, bBottom, 20f, 20f, badgeBorder)

        val invoiceTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 58, 138)
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("🧾 إيصال سداد واستلام مالي إلكتروني", pageWidth / 2f, bTop + 26f, invoiceTitle)

        // Meta Info Bar (Receipt No & Date)
        var curY = 200f
        val metaBoxBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(241, 245, 249) }
        val metaBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(40f, curY, pageWidth - 40f, curY + 45f, 8f, 8f, metaBoxBg)
        canvas.drawRoundRect(40f, curY, pageWidth - 40f, curY + 45f, 8f, 8f, metaBorder)

        val metaLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            textSize = 9.5f
            textAlign = Paint.Align.RIGHT
        }
        val metaVal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        // Receipt Number on Right
        val receiptIdStr = "#REC-${payment.id.toString().padStart(5, '0')}"
        canvas.drawText("رقم الإيصال:", pageWidth - 55f, curY + 28f, metaLabel)
        canvas.drawText(receiptIdStr, pageWidth - 120f, curY + 28f, metaVal.apply { color = Color.rgb(30, 58, 138) })

        // Date on Left
        val payDate = payment.date.ifEmpty { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(payment.createdAt)) }
        canvas.drawText("تاريخ السداد:", 170f, curY + 28f, metaLabel)
        canvas.drawText(payDate, 105f, curY + 28f, metaVal.apply { color = Color.rgb(15, 23, 42) })

        curY += 65f

        // Student Info Card (Right Column / Box)
        val sBoxBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val sBoxBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240)
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        canvas.drawRoundRect(40f, curY, pageWidth - 40f, curY + 120f, 10f, 10f, sBoxBg)
        canvas.drawRoundRect(40f, curY, pageWidth - 40f, curY + 120f, 10f, 10f, sBoxBorder)

        // Section Title: بيانات الطالب
        val sTitleBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(238, 242, 255) }
        canvas.drawRoundRect(pageWidth - 170f, curY + 10f, pageWidth - 55f, curY + 34f, 6f, 6f, sTitleBg)
        val sTitleText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 58, 138)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("👤 بيانات الطالب والمجموعة", (pageWidth - 170f + pageWidth - 55f) / 2f, curY + 26f, sTitleText)

        val fieldLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139)
            textSize = 9.5f
            textAlign = Paint.Align.RIGHT
        }
        val fieldVal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        // Line 1: Student Name & Code
        canvas.drawText("اسم الطالب:", pageWidth - 55f, curY + 56f, fieldLabel)
        canvas.drawText(student.name, pageWidth - 120f, curY + 56f, fieldVal)

        val codeLabel = student.barcodeCode.ifEmpty { "STD-${1000 + student.id}" }
        canvas.drawText("كود الطالب:", 220f, curY + 56f, fieldLabel)
        canvas.drawText(codeLabel, 150f, curY + 56f, fieldVal)

        // Line 2: Group & Grade
        val grpName = group?.name ?: student.grade.ifEmpty { "مجموعة عامة" }
        canvas.drawText("المجموعة:", pageWidth - 55f, curY + 80f, fieldLabel)
        canvas.drawText(grpName, pageWidth - 120f, curY + 80f, fieldVal.apply { color = Color.rgb(30, 58, 138) })

        canvas.drawText("الصف الدراسي:", 220f, curY + 80f, fieldLabel)
        canvas.drawText(student.grade.ifEmpty { "المرحلة الدراسية" }, 150f, curY + 80f, fieldVal)

        // Line 3: Parent Phone
        if (student.parentPhone.isNotEmpty() || student.phone.isNotEmpty()) {
            val pNum = student.parentPhone.ifEmpty { student.phone }
            canvas.drawText("هاتف ولي الأمر:", pageWidth - 55f, curY + 104f, fieldLabel)
            canvas.drawText(pNum, pageWidth - 130f, curY + 104f, fieldVal)
        }

        curY += 135f

        // Financial Details Table
        val tableTop = curY
        val tableLeft = 40f
        val tableRight = pageWidth - 40f
        val tableWidth = tableRight - tableLeft
        val rowHeight = 36f

        // Table Header
        val tblHeaderBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(30, 58, 138) }
        canvas.drawRoundRect(tableLeft, tableTop, tableRight, tableTop + rowHeight, 8f, 8f, tblHeaderBg)

        val thText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("بند الدفعة / نوع الاشتراك", tableRight - 20f, tableTop + 23f, thText.apply { textAlign = Paint.Align.RIGHT })
        canvas.drawText("الشهر / الفترة", tableRight - 220f, tableTop + 23f, thText.apply { textAlign = Paint.Align.RIGHT })
        canvas.drawText("المبلغ المسدد", tableLeft + 50f, tableTop + 23f, thText.apply { textAlign = Paint.Align.CENTER })

        // Table Row 1
        val r1Y = tableTop + rowHeight
        val r1Bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawRect(tableLeft, r1Y, tableRight, r1Y + rowHeight, r1Bg)
        canvas.drawRect(tableLeft, r1Y, tableRight, r1Y + rowHeight, metaBorder)

        val typeText = when (payment.type) {
            "month" -> "اشتراك شهر دراسي كامل"
            "session" -> "رسوم حضور حصة دراسية"
            "book", "material" -> "رسوم مذكرة / ملزمة تعليمية"
            else -> "سداد مالي / اشتراك"
        }
        val cellTxt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10f
        }
        canvas.drawText(typeText, tableRight - 20f, r1Y + 23f, cellTxt.apply { textAlign = Paint.Align.RIGHT; typeface = Typeface.DEFAULT_BOLD })

        val monthStr = payment.monthName.ifEmpty { "الشهر الحالي" }
        canvas.drawText(monthStr, tableRight - 220f, r1Y + 23f, cellTxt.apply { textAlign = Paint.Align.RIGHT; typeface = Typeface.DEFAULT })

        val amountStr = "${payment.amount.toInt()} ج.م"
        val amountTxt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(5, 150, 105) // Green
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(amountStr, tableLeft + 50f, r1Y + 24f, amountTxt)

        // Notes Row if present
        var afterTableY = r1Y + rowHeight
        if (payment.note.isNotEmpty()) {
            val noteRowY = afterTableY
            canvas.drawRect(tableLeft, noteRowY, tableRight, noteRowY + 30f, Paint().apply { color = Color.rgb(248, 250, 252) })
            canvas.drawRect(tableLeft, noteRowY, tableRight, noteRowY + 30f, metaBorder)
            canvas.drawText("ملاحظات:", tableRight - 20f, noteRowY + 19f, fieldLabel.apply { textAlign = Paint.Align.RIGHT })
            canvas.drawText(payment.note, tableRight - 75f, noteRowY + 19f, cellTxt.apply { textAlign = Paint.Align.RIGHT })
            afterTableY += 30f
        }

        // Summary Balance Cards (Paid Amount & Remaining Balance)
        curY = afterTableY + 20f

        // Card 1: المبلغ المستلم
        val c1Left = pageWidth / 2f + 10f
        val c1Right = pageWidth - 40f
        val cardHeight = 70f
        val greenCardBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(236, 253, 245) }
        val greenBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(16, 185, 129)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawRoundRect(c1Left, curY, c1Right, curY + cardHeight, 10f, 10f, greenCardBg)
        canvas.drawRoundRect(c1Left, curY, c1Right, curY + cardHeight, 10f, 10f, greenBorder)

        val card1Title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(6, 95, 70)
            textSize = 9.5f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("المبلغ المسدد في هذا الإيصال", (c1Left + c1Right) / 2f, curY + 24f, card1Title)

        val card1Val = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(5, 150, 105)
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("${payment.amount.toInt()} ج.م", (c1Left + c1Right) / 2f, curY + 54f, card1Val)

        // Card 2: المتبقي على الطالب
        val c2Left = 40f
        val c2Right = pageWidth / 2f - 10f
        val balanceColor = if (remainingBalance > 0) Color.rgb(220, 38, 38) else Color.rgb(30, 58, 138)
        val balanceBg = if (remainingBalance > 0) Color.rgb(254, 242, 242) else Color.rgb(241, 245, 249)
        val bCardBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = balanceBg }
        val bCardBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (remainingBalance > 0) Color.rgb(248, 113, 113) else Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        canvas.drawRoundRect(c2Left, curY, c2Right, curY + cardHeight, 10f, 10f, bCardBg)
        canvas.drawRoundRect(c2Left, curY, c2Right, curY + cardHeight, 10f, 10f, bCardBorder)

        val card2Title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            textSize = 9.5f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(if (remainingBalance > 0) "المتبقي بعد هذا السداد" else "حالة الحساب المالي", (c2Left + c2Right) / 2f, curY + 24f, card2Title)

        val card2Val = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = balanceColor
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val balText = if (remainingBalance > 0) "${remainingBalance.toInt()} ج.م" else "خالص السداد ✅"
        canvas.drawText(balText, (c2Left + c2Right) / 2f, curY + 54f, card2Val)

        curY += cardHeight + 25f

        // Verification & Signature Section
        val signTop = curY
        val signHeight = 100f

        // QR Code on Right
        val qrBoxBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawRoundRect(pageWidth - 135f, signTop, pageWidth - 45f, signTop + 90f, 8f, 8f, qrBoxBg)
        canvas.drawRoundRect(pageWidth - 135f, signTop, pageWidth - 45f, signTop + 90f, 8f, 8f, metaBorder)

        try {
            val qrText = "RECEIPT:${payment.id}|STUDENT:${student.name}|AMOUNT:${payment.amount}|DATE:$payDate"
            val qrBmp = com.example.util.QrBarcodeUtils.generateQrBitmap(qrText, 140)
            val dstRect = RectF(pageWidth - 130f, signTop + 5f, pageWidth - 50f, signTop + 85f)
            canvas.drawBitmap(qrBmp, null, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))
        } catch (_: Exception) {}

        // Seal / Stamp Representation in Middle
        val sealPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 58, 138)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        val sealCx = pageWidth / 2f
        val sealCy = signTop + 45f
        canvas.drawCircle(sealCx, sealCy, 32f, sealPaint)
        canvas.drawCircle(sealCx, sealCy, 28f, sealPaint.apply { strokeWidth = 0.8f })

        val sealTxt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 58, 138)
            textSize = 7f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("★ اعتماد السداد ★", sealCx, sealCy - 6f, sealTxt)
        canvas.drawText("خزينة الأستاذ", sealCx, sealCy + 6f, sealTxt)
        canvas.drawText("معتمد إلكترونياً", sealCx, sealCy + 16f, sealTxt)

        // Signature on Left
        val signText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("توقيع المستلم / إدارة السنتر", 120f, signTop + 20f, signText)
        canvas.drawLine(50f, signTop + 65f, 190f, signTop + 65f, metaBorder.apply { strokeWidth = 1.2f })
        canvas.drawText(teacherName, 120f, signTop + 80f, fieldLabel.apply { textAlign = Paint.Align.CENTER })

        // Bottom Footer Bar
        val footerTop = pageHeight - 65f
        val footerBg = Paint().apply {
            shader = LinearGradient(
                0f, footerTop, pageWidth.toFloat(), pageHeight.toFloat(),
                Color.rgb(15, 23, 42), Color.rgb(30, 58, 138),
                Shader.TileMode.CLAMP
            )
        }
        val footerPath = Path().apply {
            moveTo(25f, footerTop)
            lineTo(pageWidth - 25f, footerTop)
            lineTo(pageWidth - 25f, pageHeight - 35f)
            quadTo(pageWidth - 25f, pageHeight - 25f, pageWidth - 35f, pageHeight - 25f)
            lineTo(35f, pageHeight - 25f)
            quadTo(25f, pageHeight - 25f, 25f, pageHeight - 35f)
            close()
        }
        canvas.drawPath(footerPath, footerBg)

        val footerTxt1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("🌸 شاكرين لسيادتكم دوام التعاون والحرص على متابعة وتفوق الطالب 🌸", pageWidth / 2f, footerTop + 22f, footerTxt1)

        val footerTxt2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            textSize = 8f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("تم إنشاء هذا الإيصال آلياً من خلال تطبيق إدارة المعلم الذكي • صالح كإثبات سداد رسمي", pageWidth / 2f, footerTop + 36f, footerTxt2)

        pdfDocument.finishPage(page)

        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val exportDir = File(context.cacheDir, "invoices")
        if (!exportDir.exists()) exportDir.mkdirs()
        val invoiceFile = File(exportDir, "Invoice_${student.name.replace(" ", "_")}_$dateStr.pdf")

        FileOutputStream(invoiceFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()
        return invoiceFile
    }

    // ==========================================
    // SHARING HELPERS
    // ==========================================
    fun sharePdf(context: Context, file: File, title: String) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "تعذر مشاركة الملف: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun sharePdfToWhatsApp(context: Context, file: File, caption: String = "", phoneNumber: String = "") {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                if (caption.isNotEmpty()) putExtra(Intent.EXTRA_TEXT, caption)
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to normal chooser if WhatsApp is not directly matched
            sharePdf(context, file, "مشاركة الفاتورة عبر واتساب")
        }
    }

    fun generateStudentHomeworkPdf(
        context: Context,
        teacher: TeacherEntity?,
        student: StudentEntity,
        group: GroupEntity?,
        lessonDate: String,
        homeworkTitle: String,
        imagePaths: List<String>,
        score: Double = 10.0,
        maxScore: Double = 10.0,
        rating: String = "ممتاز",
        teacherFeedback: String = ""
    ): File {
        val cleanStudentName = student.name.replace("[\\\\/:*?\"<>|]".toRegex(), "_").trim()
        val cleanDate = (if (lessonDate.isBlank()) SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) else lessonDate)
            .replace("[\\\\/:*?\"<>|]".toRegex(), "_").trim()

        val fileName = "واجب - $cleanStudentName - $cleanDate.pdf"

        val dir = File(context.filesDir, "HomeworkPDFs").apply { mkdirs() }
        val pdfFile = File(dir, fileName)

        val doc = PdfDocument()

        val pageWidth = 595
        val pageHeight = 842
        var pageNumber = 1

        val primaryColor = Color.rgb(30, 58, 138) // Navy Blue
        val accentColor = Color.rgb(217, 119, 6) // Amber Gold
        val successColor = Color.rgb(16, 185, 129) // Emerald
        val cardBg = Color.rgb(248, 250, 252) // Light Gray
        val borderColor = Color.rgb(226, 232, 240)
        val textPrimary = Color.rgb(15, 23, 42)
        val textSecondary = Color.rgb(100, 116, 139)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Helper to start a new page
        fun createNewPage(): Pair<PdfDocument.Page, Canvas> {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas

            // Background
            canvas.drawColor(Color.WHITE)

            // Outer subtle border
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f
            paint.color = borderColor
            canvas.drawRoundRect(RectF(16f, 16f, (pageWidth - 16).toFloat(), (pageHeight - 16).toFloat()), 12f, 12f, paint)

            return Pair(page, canvas)
        }

        // --- PAGE 1: Evaluation Summary & Primary Homework Photo ---
        val (page1, canvas1) = createNewPage()

        // 1. Top Header Banner
        paint.style = Paint.Style.FILL
        paint.color = primaryColor
        canvas1.drawRoundRect(RectF(24f, 24f, (pageWidth - 24).toFloat(), 96f), 10f, 10f, paint)

        // Header Title
        textPaint.color = Color.WHITE
        textPaint.textSize = 17f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textAlign = Paint.Align.CENTER
        canvas1.drawText("سجل توثيق وتصحيح واجب الطالب 📝", (pageWidth / 2).toFloat(), 55f, textPaint)

        textPaint.textSize = 11f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val teacherLine = "${teacher?.name ?: "الأستاذ"} • ${teacher?.subject ?: "جميع المواد"} • ${teacher?.centerName?.ifEmpty { "التعليم الخاص" } ?: "السنتر"}"
        canvas1.drawText(teacherLine, (pageWidth / 2).toFloat(), 78f, textPaint)

        // 2. Student Information Card
        val cardRect = RectF(24f, 106f, (pageWidth - 24).toFloat(), 175f)
        paint.style = Paint.Style.FILL
        paint.color = cardBg
        canvas1.drawRoundRect(cardRect, 8f, 8f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = borderColor
        canvas1.drawRoundRect(cardRect, 8f, 8f, paint)

        // Student Info Text
        textPaint.color = textPrimary
        textPaint.textSize = 13f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textAlign = Paint.Align.RIGHT

        canvas1.drawText("اسم الطالب: ${student.name}", (pageWidth - 36).toFloat(), 130f, textPaint)
        canvas1.drawText("عنوان الواجب: ${homeworkTitle.ifBlank { "واجب الحصة" }}", (pageWidth - 36).toFloat(), 155f, textPaint)

        textPaint.textSize = 11f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.color = textSecondary
        canvas1.drawText("المجموعة: ${group?.name ?: "بدون مجموعة"} (${student.grade})", 260f, 130f, textPaint)
        canvas1.drawText("تاريخ الحصة: $cleanDate", 260f, 155f, textPaint)

        // 3. Status & Evaluation Box
        val scoreBox = RectF(36f, 116f, 140f, 165f)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(238, 242, 255)
        canvas1.drawRoundRect(scoreBox, 8f, 8f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = primaryColor
        paint.strokeWidth = 1.5f
        canvas1.drawRoundRect(scoreBox, 8f, 8f, paint)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = primaryColor
        textPaint.textSize = 11.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas1.drawText("حالة الواجب", 88f, 136f, textPaint)

        textPaint.textSize = 10f
        textPaint.color = if (rating.contains("ممتاز") || rating.contains("كامل")) successColor else accentColor
        val safeRating = if (rating.length > 18) rating.take(18) else rating.ifBlank { "حل كامل وممتاز 🌟" }
        canvas1.drawText(safeRating, 88f, 155f, textPaint)

        var currentY = 185f

        // 4. Teacher Feedback Box (if provided)
        if (teacherFeedback.isNotBlank()) {
            val fbRect = RectF(24f, currentY, (pageWidth - 24).toFloat(), currentY + 54f)
            paint.style = Paint.Style.FILL
            paint.color = Color.rgb(254, 243, 199)
            canvas1.drawRoundRect(fbRect, 8f, 8f, paint)
            paint.style = Paint.Style.STROKE
            paint.color = Color.rgb(245, 158, 11)
            paint.strokeWidth = 1f
            canvas1.drawRoundRect(fbRect, 8f, 8f, paint)

            textPaint.textAlign = Paint.Align.RIGHT
            textPaint.color = Color.rgb(180, 83, 9)
            textPaint.textSize = 11f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas1.drawText("💬 ملاحظات وتوجيهات المعلم:", (pageWidth - 36).toFloat(), currentY + 20f, textPaint)

            textPaint.color = textPrimary
            textPaint.textSize = 10.5f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val fbSafe = if (teacherFeedback.length > 80) teacherFeedback.take(80) + "..." else teacherFeedback
            canvas1.drawText(fbSafe, (pageWidth - 36).toFloat(), currentY + 40f, textPaint)

            currentY += 64f
        }

        // 5. Draw First Image on Page 1
        val firstImagePath = imagePaths.firstOrNull { it.isNotBlank() && File(it).exists() }
        val availablePhotoHeight = (pageHeight - currentY - 50f).coerceAtLeast(100f)

        if (firstImagePath != null) {
            try {
                val bitmap = BitmapFactory.decodeFile(firstImagePath)
                if (bitmap != null) {
                    val targetWidth = (pageWidth - 48).toFloat()
                    val scaleFactor = kotlin.math.min(targetWidth / bitmap.width.toFloat(), availablePhotoHeight / bitmap.height.toFloat())
                    val scaledW = bitmap.width * scaleFactor
                    val scaledH = bitmap.height * scaleFactor

                    val left = (pageWidth - scaledW) / 2f
                    val top = currentY + ((availablePhotoHeight - scaledH) / 2f)

                    // Photo Border Card
                    paint.style = Paint.Style.FILL
                    paint.color = Color.WHITE
                    canvas1.drawRoundRect(RectF(left - 4, top - 4, left + scaledW + 4, top + scaledH + 4), 6f, 6f, paint)
                    paint.style = Paint.Style.STROKE
                    paint.color = borderColor
                    paint.strokeWidth = 1.5f
                    canvas1.drawRoundRect(RectF(left - 4, top - 4, left + scaledW + 4, top + scaledH + 4), 6f, 6f, paint)

                    canvas1.drawBitmap(bitmap, null, RectF(left, top, left + scaledW, top + scaledH), null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Page 1 Footer
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = textSecondary
        textPaint.textSize = 9.5f
        val footerStr = "تم التوثيق بواسطة تطبيق المعلم الذكي • هاتف المعلم: ${teacher?.phone ?: ""} • صفحة 1 من ${kotlin.math.max(1, imagePaths.size)}"
        canvas1.drawText(footerStr, (pageWidth / 2).toFloat(), (pageHeight - 24).toFloat(), textPaint)

        doc.finishPage(page1)

        // --- SUBSEQUENT PAGES: Additional Scanned Photos ---
        if (imagePaths.size > 1) {
            for (i in 1 until imagePaths.size) {
                val extraPath = imagePaths[i]
                if (extraPath.isBlank() || !File(extraPath).exists()) continue

                val (extraPage, extraCanvas) = createNewPage()

                // Header Bar on Extra Pages
                paint.style = Paint.Style.FILL
                paint.color = cardBg
                extraCanvas.drawRoundRect(RectF(24f, 24f, (pageWidth - 24).toFloat(), 64f), 6f, 6f, paint)
                paint.style = Paint.Style.STROKE
                paint.color = borderColor
                paint.strokeWidth = 1f
                extraCanvas.drawRoundRect(RectF(24f, 24f, (pageWidth - 24).toFloat(), 64f), 6f, 6f, paint)

                textPaint.textAlign = Paint.Align.RIGHT
                textPaint.color = textPrimary
                textPaint.textSize = 11.5f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                extraCanvas.drawText("واجب الطالب: ${student.name} • صفحة ${i + 1}", (pageWidth - 36).toFloat(), 48f, textPaint)

                textPaint.textAlign = Paint.Align.LEFT
                textPaint.color = textSecondary
                textPaint.textSize = 10f
                extraCanvas.drawText("تاريخ: $cleanDate", 36f, 48f, textPaint)

                // Extra Page Photo
                try {
                    val bitmap = BitmapFactory.decodeFile(extraPath)
                    if (bitmap != null) {
                        val maxPhotoH = pageHeight - 120f
                        val maxPhotoW = pageWidth - 48f
                        val scale = kotlin.math.min(maxPhotoW / bitmap.width.toFloat(), maxPhotoH / bitmap.height.toFloat())
                        val sw = bitmap.width * scale
                        val sh = bitmap.height * scale

                        val left = (pageWidth - sw) / 2f
                        val top = 76f + ((maxPhotoH - sh) / 2f)

                        paint.style = Paint.Style.STROKE
                        paint.color = borderColor
                        paint.strokeWidth = 1.5f
                        extraCanvas.drawRoundRect(RectF(left - 3, top - 3, left + sw + 3, top + sh + 3), 6f, 6f, paint)

                        extraCanvas.drawBitmap(bitmap, null, RectF(left, top, left + sw, top + sh), null)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Extra Page Footer
                textPaint.textAlign = Paint.Align.CENTER
                textPaint.color = textSecondary
                textPaint.textSize = 9.5f
                extraCanvas.drawText("صفحة ${i + 1} من ${imagePaths.size}", (pageWidth / 2).toFloat(), (pageHeight - 24).toFloat(), textPaint)

                doc.finishPage(extraPage)
            }
        }

        try {
            val fos = FileOutputStream(pdfFile)
            doc.writeTo(fos)
            fos.flush()
            fos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            doc.close()
        }

        return pdfFile
    }

    fun sharePdfToTelegram(context: Context, file: File, caption: String = "") {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                if (caption.isNotEmpty()) putExtra(Intent.EXTRA_TEXT, caption)
                setPackage("org.telegram.messenger")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to normal chooser if Telegram is not directly matched
            sharePdf(context, file, "مشاركة الفاتورة عبر تليجرام")
        }
    }
}

