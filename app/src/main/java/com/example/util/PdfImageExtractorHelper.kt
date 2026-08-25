package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Image Extraction Quality Levels - From Fast Standard up to Ultra 4K (600 DPI)
 */
enum class PdfImageQuality(
    val title: String,
    val description: String,
    val scaleFactor: Float,
    val iconText: String,
    val tag: String,
    val dpiText: String
) {
    ULTRA_HD("دقة فائقة خارقة (Ultra 4K - 600 DPI)", "أعلى وضوح كريستالي ممكن للطباعة والتكبير العملاق", 4.0f, "👑", "ultra_4k", "600 DPI"),
    HIGH("جودة فائقة عالية (High - 300 DPI)", "دقة ممتازة للطباعة وعرض التفاصيل الدقيقة والشاشات الكبيرة", 2.8f, "💎", "high_300dpi", "300 DPI"),
    MEDIUM("جودة متوازنة (Medium - 150 DPI)", "حجم مثالي وسريع جداً للإرسال عبر واتساب وتيليجرام", 1.8f, "⚡", "medium_150dpi", "150 DPI"),
    STANDARD("جودة قياسية خفيفة (Standard - 96 DPI)", "حجم ملف مضغوط وخفيف جداً للحفظ السريع", 1.0f, "📦", "standard_96dpi", "96 DPI")
}

enum class PdfImageFormat(val extension: String, val mimeType: String, val compressFormat: Bitmap.CompressFormat) {
    PNG("png", "image/png", Bitmap.CompressFormat.PNG),
    JPEG("jpg", "image/jpeg", Bitmap.CompressFormat.JPEG)
}

enum class WatermarkPosition(val title: String) {
    BOTTOM_RIGHT("أسفل اليمين"),
    BOTTOM_LEFT("أسفل اليسار"),
    TOP_RIGHT("أعلى اليمين"),
    TOP_LEFT("أعلى اليسار"),
    BOTTOM_CENTER("أسفل الوسط"),
    CENTER("وسط الصفحة (شارة مائية)"),
    DIAGONAL("مائل بطول الصفحة")
}

data class WatermarkConfig(
    val isEnabled: Boolean = false,
    val text: String = "الأستاذ / هاكر التدريس",
    val subText: String = "حقوق الطبع والملكية محفوظة",
    val position: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,
    val opacity: Float = 0.85f,
    val logoBitmap: Bitmap? = null,
    val includePageNumber: Boolean = true
)

object PdfImageExtractorHelper {

    private const val EXPORT_FOLDER = "Extracted_PDF_Images"

    fun getExportImagesDirectory(context: Context): File {
        val appRoot = StudyFileManager.getAppRootDirectory(context)
        val dir = File(appRoot, EXPORT_FOLDER)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Renders a specific page of a PDF into an Android Bitmap at requested scale and quality with optional watermark & drawings
     */
    suspend fun renderPageToBitmap(
        pdfFile: File,
        pageIndex: Int,
        totalPages: Int = 1,
        quality: PdfImageQuality = PdfImageQuality.HIGH,
        overlayBitmap: Bitmap? = null,
        watermarkConfig: WatermarkConfig? = null
    ): Bitmap = withContext(Dispatchers.IO) {
        val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)

        val page = renderer.openPage(pageIndex.coerceIn(0, renderer.pageCount - 1))
        val targetWidth = (page.width * quality.scaleFactor).toInt().coerceAtLeast(100)
        val targetHeight = (page.height * quality.scaleFactor).toInt().coerceAtLeast(100)

        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        renderer.close()
        pfd.close()

        // If teacher has drawn overlays/annotations, draw them on top with scaling
        if (overlayBitmap != null) {
            val scaleX = targetWidth.toFloat() / overlayBitmap.width.toFloat()
            val scaleY = targetHeight.toFloat() / overlayBitmap.height.toFloat()
            val overlayCanvas = Canvas(bitmap)
            overlayCanvas.scale(scaleX, scaleY)
            overlayCanvas.drawBitmap(overlayBitmap, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
        }

        // Apply Watermark / Teacher Logo if enabled
        if (watermarkConfig != null && watermarkConfig.isEnabled) {
            applyWatermark(canvas, targetWidth, targetHeight, pageIndex, totalPages, watermarkConfig)
        }

        bitmap
    }

    /**
     * Draws fixed watermark / logo on the canvas
     */
    private fun applyWatermark(
        canvas: Canvas,
        width: Int,
        height: Int,
        pageIndex: Int,
        totalPages: Int,
        config: WatermarkConfig
    ) {
        val alphaInt = (config.opacity * 255).toInt().coerceIn(10, 255)
        
        when (config.position) {
            WatermarkPosition.BOTTOM_RIGHT, WatermarkPosition.BOTTOM_LEFT, WatermarkPosition.BOTTOM_CENTER -> {
                val bannerHeight = (height * 0.045f).coerceIn(40f, 100f)
                val bgPaint = Paint().apply {
                    color = Color.argb((alphaInt * 0.85f).toInt(), 15, 23, 42) // Dark sleek badge
                    isAntiAlias = true
                }
                val textPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = bannerHeight * 0.42f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                val subTextPaint = Paint().apply {
                    color = Color.argb(220, 241, 245, 249)
                    textSize = bannerHeight * 0.28f
                    isAntiAlias = true
                }

                val rect = when (config.position) {
                    WatermarkPosition.BOTTOM_RIGHT -> {
                        val bannerWidth = (width * 0.42f).coerceIn(240f, 500f)
                        RectF(width - bannerWidth - 20f, height - bannerHeight - 20f, width - 20f, height - 20f)
                    }
                    WatermarkPosition.BOTTOM_LEFT -> {
                        val bannerWidth = (width * 0.42f).coerceIn(240f, 500f)
                        RectF(20f, height - bannerHeight - 20f, bannerWidth + 20f, height - 20f)
                    }
                    else -> {
                        val bannerWidth = (width * 0.50f).coerceIn(280f, 600f)
                        RectF((width - bannerWidth) / 2f, height - bannerHeight - 20f, (width + bannerWidth) / 2f, height - 20f)
                    }
                }

                // Draw rounded badge
                canvas.drawRoundRect(rect, 14f, 14f, bgPaint)

                // Optional Logo Bitmap inside badge
                var textStartX = rect.left + 16f
                if (config.logoBitmap != null) {
                    val logoSize = bannerHeight * 0.75f
                    val destRect = RectF(textStartX, rect.centerY() - logoSize / 2f, textStartX + logoSize, rect.centerY() + logoSize / 2f)
                    val logoPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply { alpha = alphaInt }
                    canvas.drawBitmap(config.logoBitmap, null, destRect, logoPaint)
                    textStartX += logoSize + 12f
                }

                // Draw Text & Subtext
                val titleY = if (config.subText.isNotBlank()) rect.top + bannerHeight * 0.42f else rect.centerY() + bannerHeight * 0.15f
                canvas.drawText(config.text, textStartX, titleY, textPaint)

                if (config.subText.isNotBlank()) {
                    val pageStr = if (config.includePageNumber && totalPages > 1) " • صـ ${pageIndex + 1}/$totalPages" else ""
                    canvas.drawText("${config.subText}$pageStr", textStartX, rect.bottom - bannerHeight * 0.22f, subTextPaint)
                }
            }

            WatermarkPosition.TOP_RIGHT, WatermarkPosition.TOP_LEFT -> {
                val bannerHeight = (height * 0.045f).coerceIn(40f, 90f)
                val bgPaint = Paint().apply {
                    color = Color.argb((alphaInt * 0.85f).toInt(), 30, 58, 138) // Deep Blue
                    isAntiAlias = true
                }
                val textPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = bannerHeight * 0.45f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                val bannerWidth = (width * 0.38f).coerceIn(220f, 450f)
                val rect = if (config.position == WatermarkPosition.TOP_RIGHT) {
                    RectF(width - bannerWidth - 20f, 20f, width - 20f, bannerHeight + 20f)
                } else {
                    RectF(20f, 20f, bannerWidth + 20f, bannerHeight + 20f)
                }

                canvas.drawRoundRect(rect, 12f, 12f, bgPaint)
                canvas.drawText(config.text, rect.left + 16f, rect.centerY() + bannerHeight * 0.15f, textPaint)
            }

            WatermarkPosition.CENTER -> {
                val textPaint = Paint().apply {
                    color = Color.argb((alphaInt * 0.25f).toInt(), 30, 41, 59)
                    textSize = (width * 0.065f).coerceIn(32f, 80f)
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText(config.text, width / 2f, height / 2f, textPaint)
            }

            WatermarkPosition.DIAGONAL -> {
                canvas.save()
                canvas.rotate(-35f, width / 2f, height / 2f)
                val textPaint = Paint().apply {
                    color = Color.argb((alphaInt * 0.22f).toInt(), 37, 99, 235)
                    textSize = (width * 0.075f).coerceIn(36f, 90f)
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText(config.text, width / 2f, height / 2f, textPaint)
                if (config.subText.isNotBlank()) {
                    val subPaint = Paint().apply {
                        color = Color.argb((alphaInt * 0.18f).toInt(), 71, 85, 105)
                        textSize = textPaint.textSize * 0.5f
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    canvas.drawText(config.subText, width / 2f, height / 2f + textPaint.textSize * 0.7f, subPaint)
                }
                canvas.restore()
            }
        }
    }

    /**
     * Extracts and saves the current PDF page as a standalone image file with watermark support
     */
    suspend fun exportCurrentPageAsImage(
        context: Context,
        pdfFile: File,
        pageIndex: Int,
        totalPages: Int = 1,
        docTitle: String,
        quality: PdfImageQuality = PdfImageQuality.HIGH,
        format: PdfImageFormat = PdfImageFormat.PNG,
        overlayBitmap: Bitmap? = null,
        watermarkConfig: WatermarkConfig? = null
    ): File = withContext(Dispatchers.IO) {
        val bitmap = renderPageToBitmap(
            pdfFile = pdfFile,
            pageIndex = pageIndex,
            totalPages = totalPages,
            quality = quality,
            overlayBitmap = overlayBitmap,
            watermarkConfig = watermarkConfig
        )
        val cleanTitle = docTitle.replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF_-]"), "_").take(30)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
        val fileName = "${cleanTitle}_صفحة_${pageIndex + 1}_${quality.tag}_$timeStamp.${format.extension}"

        val destinationFile = File(getExportImagesDirectory(context), fileName)
        FileOutputStream(destinationFile).use { out ->
            val qualityInt = if (format == PdfImageFormat.JPEG) 95 else 100
            bitmap.compress(format.compressFormat, qualityInt, out)
        }
        destinationFile
    }

    /**
     * Converts all pages of a PDF file into individual image files with progress reporting & watermark
     */
    suspend fun convertFullPdfToImages(
        context: Context,
        pdfFile: File,
        docTitle: String,
        quality: PdfImageQuality = PdfImageQuality.HIGH,
        format: PdfImageFormat = PdfImageFormat.JPEG,
        watermarkConfig: WatermarkConfig? = null,
        onProgress: (current: Int, total: Int) -> Unit
    ): List<File> = withContext(Dispatchers.IO) {
        val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val totalPages = renderer.pageCount

        val cleanTitle = docTitle.replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF_-]"), "_").take(25)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
        val outputDir = File(getExportImagesDirectory(context), "${cleanTitle}_$timeStamp")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        val generatedFiles = mutableListOf<File>()

        for (i in 0 until totalPages) {
            val page = renderer.openPage(i)
            val targetWidth = (page.width * quality.scaleFactor).toInt().coerceAtLeast(100)
            val targetHeight = (page.height * quality.scaleFactor).toInt().coerceAtLeast(100)

            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            // Apply Watermark to each page
            if (watermarkConfig != null && watermarkConfig.isEnabled) {
                applyWatermark(canvas, targetWidth, targetHeight, i, totalPages, watermarkConfig)
            }

            val pageFileName = "${cleanTitle}_صفحة_${i + 1}.${format.extension}"
            val pageFile = File(outputDir, pageFileName)
            FileOutputStream(pageFile).use { out ->
                val qualityInt = if (format == PdfImageFormat.JPEG) 92 else 100
                bitmap.compress(format.compressFormat, qualityInt, out)
            }
            bitmap.recycle()
            generatedFiles.add(pageFile)

            withContext(Dispatchers.Main) {
                onProgress(i + 1, totalPages)
            }
        }

        renderer.close()
        pfd.close()

        generatedFiles
    }

    /**
     * Bundles a list of extracted images into a single ZIP archive for effortless sharing
     */
    suspend fun createZipArchive(
        context: Context,
        files: List<File>,
        zipBaseName: String
    ): File = withContext(Dispatchers.IO) {
        val cleanName = zipBaseName.replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF_-]"), "_").take(25)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
        val zipFile = File(getExportImagesDirectory(context), "${cleanName}_صور_كاملة_$timeStamp.zip")

        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            val buffer = ByteArray(8192)
            files.forEach { file ->
                if (file.exists()) {
                    FileInputStream(file).use { fis ->
                        val entry = ZipEntry(file.name)
                        zos.putNextEntry(entry)
                        var length: Int
                        while (fis.read(buffer).also { length = it } > 0) {
                            zos.write(buffer, 0, length)
                        }
                        zos.closeEntry()
                    }
                }
            }
        }
        zipFile
    }

    /**
     * Shares a single image file via Android System Share Sheet (WhatsApp, Telegram, etc.)
     */
    fun shareSingleImage(context: Context, imageFile: File, title: String = "صورة الصفحة") {
        if (!imageFile.exists()) return

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (imageFile.extension.equals("png", ignoreCase = true)) "image/png" else "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "تم استخراج هذه الصفحة من: $title عبر تطبيق ${StudyFileManager.APP_DISPLAY_NAME}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة صورة الصفحة عبر:"))
    }

    /**
     * Shares a ZIP archive of all pages
     */
    fun shareZipFile(context: Context, zipFile: File, title: String) {
        if (!zipFile.exists()) return

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            zipFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "$title - كافة الصفحات")
            putExtra(Intent.EXTRA_TEXT, "ملف مضغوط يحتوي على صور كافة صفحات: $title بجودة عالية عبر تطبيق ${StudyFileManager.APP_DISPLAY_NAME}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة الأرشيف المضغوط عبر:"))
    }

    /**
     * Shares multiple images directly
     */
    fun shareMultipleImages(context: Context, imageFiles: List<File>, title: String) {
        val uris = ArrayList<Uri>()
        imageFiles.forEach { file ->
            if (file.exists()) {
                uris.add(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
            }
        }

        if (uris.isEmpty()) return

        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "تم تحويل صفحات $title إلى صور عبر تطبيق ${StudyFileManager.APP_DISPLAY_NAME}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة صور الصفحات عبر:"))
    }
}
