package com.example.data.export

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.util.WhatsAppHelper
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object HomeworkPdfExporter {

    /**
     * Generates a PDF file containing student homework photos and evaluation details.
     * File name format: "واجب - [اسم الطالب] - [تاريخ الحصة].pdf"
     */
    fun generateHomeworkPdf(
        context: Context,
        studentName: String,
        sessionDate: String,
        groupName: String = "",
        teacherName: String = "",
        homeworkStatus: String = "completed",
        score: String = "",
        topic: String = "",
        notes: String = "",
        bitmaps: List<Bitmap> = emptyList()
    ): File {
        val sanitizedStudent = if (studentName.isNotBlank()) studentName.trim() else "طالب"
        val sanitizedDate = if (sessionDate.isNotBlank()) sessionDate.trim() else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        // Exact naming requirement: "واجب - اسم الطالب - تاريخ الحصة.pdf"
        val fileName = "واجب - ${sanitizedStudent.replace("/", "-")} - $sanitizedDate.pdf"

        val exportDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "HomeworkReports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        val pdfFile = File(exportDir, fileName)

        val document = PdfDocument()

        val pageWidth = 595 // A4 standard width in points
        val pageHeight = 842 // A4 standard height in points

        val titlePaint = Paint().apply {
            color = Color.rgb(30, 58, 138) // Deep Blue
            textSize = 22f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val subPaint = Paint().apply {
            color = Color.rgb(71, 85, 105) // Slate gray
            textSize = 12f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val headerBoxPaint = Paint().apply {
            color = Color.rgb(241, 245, 249) // Light Slate
            style = Paint.Style.FILL
        }

        val headerBorderPaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }

        val labelPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 13f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }

        val valuePaint = Paint().apply {
            color = Color.rgb(51, 65, 85)
            textSize = 12f
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }

        val statusColor = when (homeworkStatus) {
            "completed" -> Color.rgb(16, 185, 129) // Emerald Green
            "partial" -> Color.rgb(245, 158, 11) // Amber
            "not_done" -> Color.rgb(239, 68, 68) // Crimson
            else -> Color.rgb(59, 130, 246) // Blue
        }

        val statusText = when (homeworkStatus) {
            "completed" -> "حل كامل وممتاز 🌟"
            "partial" -> "حل ناقص وغير مكتمل ⚠️"
            "not_done" -> "لم يقم بحل الواجب ❌"
            "exempt" -> "معفى من الواجب ⚪"
            else -> homeworkStatus
        }

        val statusBadgePaint = Paint().apply {
            color = statusColor
            style = Paint.Style.FILL
        }

        val statusTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 12f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        // ============================
        // PAGE 1: Evaluation and First Photos
        // ============================
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        // Decorative Header Ribbon
        val ribbonPaint = Paint().apply {
            color = Color.rgb(30, 58, 138)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 16f, ribbonPaint)

        // Title
        canvas.drawText("تقرير توثيق وتقييم واجب الطالب", pageWidth / 2f, 50f, titlePaint)
        canvas.drawText("منظومة المعلم الذكي - The Hacker Teacher Planner", pageWidth / 2f, 70f, subPaint)

        // Student Info Card
        val infoRect = RectF(30f, 90f, (pageWidth - 30).toFloat(), 245f)
        canvas.drawRoundRect(infoRect, 16f, 16f, headerBoxPaint)
        canvas.drawRoundRect(infoRect, 16f, 16f, headerBorderPaint)

        val rightX = (pageWidth - 50).toFloat()
        var currentY = 120f

        canvas.drawText("اسم الطالب: $sanitizedStudent", rightX, currentY, labelPaint)
        if (teacherName.isNotBlank()) {
            canvas.drawText("المعلم: $teacherName", 180f, currentY, valuePaint)
        }

        currentY += 26f
        canvas.drawText("تاريخ الحصة: $sanitizedDate", rightX, currentY, valuePaint)
        if (groupName.isNotBlank()) {
            canvas.drawText("المجموعة: $groupName", 180f, currentY, valuePaint)
        }

        currentY += 26f
        canvas.drawText("حالة الواجب:", rightX, currentY, labelPaint)

        // Draw Status Badge
        val badgeRect = RectF(rightX - 220f, currentY - 18f, rightX - 80f, currentY + 6f)
        canvas.drawRoundRect(badgeRect, 8f, 8f, statusBadgePaint)
        canvas.drawText(statusText, badgeRect.centerX(), currentY - 4f, statusTextPaint)

        if (score.isNotBlank()) {
            canvas.drawText("الدرجة / التقييم: $score", 180f, currentY, labelPaint)
        }

        if (topic.isNotBlank()) {
            currentY += 26f
            canvas.drawText("موضوع الواجب: $topic", rightX, currentY, valuePaint)
        }

        if (notes.isNotBlank()) {
            currentY += 26f
            canvas.drawText("ملاحظات المعلم: $notes", rightX, currentY, valuePaint)
        }

        // Draw Images
        if (bitmaps.isEmpty()) {
            val emptyRect = RectF(30f, 270f, (pageWidth - 30).toFloat(), 450f)
            canvas.drawRoundRect(emptyRect, 14f, 14f, headerBoxPaint)
            val emptyPaint = Paint().apply {
                color = Color.GRAY
                textSize = 14f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("تم تقييم وتوثيق الواجب بدون إرفاق صور إضافية.", pageWidth / 2f, 360f, emptyPaint)
            document.finishPage(page)
        } else {
            // First 2 images on Page 1 if available
            var imgY = 265f
            val maxAvailableHeightOnP1 = (pageHeight - imgY - 40f)

            if (bitmaps.size == 1) {
                val bmp = bitmaps[0]
                val targetW = (pageWidth - 60).toFloat()
                val targetH = maxAvailableHeightOnP1
                val scaledBmp = getScaledBitmap(bmp, targetW.toInt(), targetH.toInt())
                val drawX = (pageWidth - scaledBmp.width) / 2f
                canvas.drawBitmap(scaledBmp, drawX, imgY, null)
                document.finishPage(page)
            } else {
                // Draw first image
                val bmp1 = bitmaps[0]
                val halfH = (maxAvailableHeightOnP1 / 2f) - 10f
                val targetW = (pageWidth - 60).toFloat()
                val scaledBmp1 = getScaledBitmap(bmp1, targetW.toInt(), halfH.toInt())
                val drawX1 = (pageWidth - scaledBmp1.width) / 2f
                canvas.drawBitmap(scaledBmp1, drawX1, imgY, null)

                imgY += halfH + 15f
                val bmp2 = bitmaps[1]
                val scaledBmp2 = getScaledBitmap(bmp2, targetW.toInt(), halfH.toInt())
                val drawX2 = (pageWidth - scaledBmp2.width) / 2f
                canvas.drawBitmap(scaledBmp2, drawX2, imgY, null)
                document.finishPage(page)

                // Additional Pages for extra images
                var remainingIndex = 2
                while (remainingIndex < bitmaps.size) {
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas

                    canvas.drawRect(0f, 0f, pageWidth.toFloat(), 12f, ribbonPaint)
                    canvas.drawText("تابع واجب الطالب: $sanitizedStudent (${sanitizedDate}) - صفحة $pageNumber", pageWidth / 2f, 35f, subPaint)

                    val pImgY = 50f
                    val pAvailH = (pageHeight - pImgY - 40f)
                    val remainingCount = bitmaps.size - remainingIndex

                    if (remainingCount == 1) {
                        val bmp = bitmaps[remainingIndex]
                        val scaledBmp = getScaledBitmap(bmp, (pageWidth - 60), pAvailH.toInt())
                        val drawX = (pageWidth - scaledBmp.width) / 2f
                        canvas.drawBitmap(scaledBmp, drawX, pImgY, null)
                        remainingIndex++
                    } else {
                        val halfHeight = (pAvailH / 2f) - 10f
                        val bmpA = bitmaps[remainingIndex]
                        val scaledBmpA = getScaledBitmap(bmpA, (pageWidth - 60), halfHeight.toInt())
                        val drawXA = (pageWidth - scaledBmpA.width) / 2f
                        canvas.drawBitmap(scaledBmpA, drawXA, pImgY, null)

                        val bmpB = bitmaps[remainingIndex + 1]
                        val scaledBmpB = getScaledBitmap(bmpB, (pageWidth - 60), halfHeight.toInt())
                        val drawXB = (pageWidth - scaledBmpB.width) / 2f
                        canvas.drawBitmap(scaledBmpB, drawXB, pImgY + halfHeight + 15f, null)
                        remainingIndex += 2
                    }
                    document.finishPage(page)
                }
            }
        }

        try {
            val outputStream = FileOutputStream(pdfFile)
            document.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            document.close()
        }

        return pdfFile
    }

    private fun getScaledBitmap(src: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val srcW = src.width
        val srcH = src.height
        if (srcW <= 0 || srcH <= 0) return src

        val ratio = (srcW.toFloat() / srcH.toFloat())
        var targetW = maxWidth
        var targetH = (maxWidth / ratio).toInt()

        if (targetH > maxHeight) {
            targetH = maxHeight
            targetW = (maxHeight * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(src, targetW, targetH, true)
    }

    fun getFileUri(context: Context, file: File): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    fun sharePdf(context: Context, file: File, studentName: String = "", parentPhone: String = "") {
        try {
            val uri = getFileUri(context, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "واجب الطالب $studentName")
                putExtra(Intent.EXTRA_TEXT, "مرفق ملف PDF لتوثيق وتقييم واجب الطالب: $studentName.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "مشاركة ملف PDF الواجب"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openPdf(context: Context, file: File) {
        try {
            val uri = getFileUri(context, file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
