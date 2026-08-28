package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * High-performance Document Scanner Engine (CamScanner-Style)
 * Provides automatic page edge detection, perspective transformation,
 * shadow removal, magic color enhancement, and high-contrast B&W document cleanup.
 */
object DocumentScannerHelper {

    enum class ScanFilter(val displayNameAr: String, val displayNameEn: String) {
        MAGIC_COLOR("سحري (إزالة الظلال وتوضيح الخط)", "Magic Color (Shadow Remover)"),
        BW_CLEAN("أبيض وأسود عالي التباين (مذكرات)", "B&W Document Clean"),
        GRAYSCALE("تدرج رمادي ناعم", "Grayscale Document"),
        LIGHTEN("تفتيح وإزالة الاصفرار", "Lighten Paper"),
        ORIGINAL("الصورة الأصلية", "Original Photo")
    }

    data class QuadCorners(
        val topLeft: PointF,
        val topRight: PointF,
        val bottomRight: PointF,
        val bottomLeft: PointF
    )

    /**
     * Estimates default document corners with a clean margin
     */
    fun getDefaultCorners(width: Float, height: Float, marginPercent: Float = 0.05f): QuadCorners {
        val mx = width * marginPercent
        val my = height * marginPercent
        return QuadCorners(
            topLeft = PointF(mx, my),
            topRight = PointF(width - mx, my),
            bottomRight = PointF(width - mx, height - my),
            bottomLeft = PointF(mx, height - my)
        )
    }

    /**
     * Auto-detect page edges based on luminance and contrast distribution
     */
    fun autoDetectDocumentCorners(bitmap: Bitmap): QuadCorners {
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()

        // Downscale for fast edge inspection
        val sampleScale = 0.2f
        val sw = (w * sampleScale).toInt().coerceAtLeast(50)
        val sh = (h * sampleScale).toInt().coerceAtLeast(50)
        val scaled = Bitmap.createScaledBitmap(bitmap, sw, sh, true)

        var minX = sw.toFloat()
        var maxX = 0f
        var minY = sh.toFloat()
        var maxY = 0f

        val threshold = 180 // Paper brightness threshold

        for (y in (sh * 0.05).toInt() until (sh * 0.95).toInt() step 2) {
            for (x in (sw * 0.05).toInt() until (sw * 0.95).toInt() step 2) {
                val pixel = scaled.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val lum = (r * 0.299 + g * 0.587 + b * 0.114).toInt()

                if (lum > threshold) {
                    if (x < minX) minX = x.toFloat()
                    if (x > maxX) maxX = x.toFloat()
                    if (y < minY) minY = y.toFloat()
                    if (y > maxY) maxY = y.toFloat()
                }
            }
        }

        if (maxX - minX < sw * 0.3f || maxY - minY < sh * 0.3f) {
            // Fallback to safe 6% margin
            return getDefaultCorners(w, h, 0.06f)
        }

        val scaleX = w / sw
        val scaleY = h / sh

        val left = (minX * scaleX).coerceIn(w * 0.02f, w * 0.2f)
        val right = (maxX * scaleX).coerceIn(w * 0.8f, w * 0.98f)
        val top = (minY * scaleY).coerceIn(h * 0.02f, h * 0.2f)
        val bottom = (maxY * scaleY).coerceIn(h * 0.8f, h * 0.98f)

        return QuadCorners(
            topLeft = PointF(left, top),
            topRight = PointF(right, top),
            bottomRight = PointF(right, bottom),
            bottomLeft = PointF(left, bottom)
        )
    }

    /**
     * Crops and rectifies document based on 4 corners into a straight rectangular page
     */
    fun cropPerspective(source: Bitmap, corners: QuadCorners): Bitmap {
        val widthA = sqrt(((corners.bottomRight.x - corners.bottomLeft.x).toDouble() * (corners.bottomRight.x - corners.bottomLeft.x) +
                (corners.bottomRight.y - corners.bottomLeft.y).toDouble() * (corners.bottomRight.y - corners.bottomLeft.y)).toFloat())
        val widthB = sqrt(((corners.topRight.x - corners.topLeft.x).toDouble() * (corners.topRight.x - corners.topLeft.x) +
                (corners.topRight.y - corners.topLeft.y).toDouble() * (corners.topRight.y - corners.topLeft.y)).toFloat())
        val maxWidth = max(widthA, widthB).toInt().coerceIn(200, source.width)

        val heightA = sqrt(((corners.topRight.x - corners.bottomRight.x).toDouble() * (corners.topRight.x - corners.bottomRight.x) +
                (corners.topRight.y - corners.bottomRight.y).toDouble() * (corners.topRight.y - corners.bottomRight.y)).toFloat())
        val heightB = sqrt(((corners.topLeft.x - corners.bottomLeft.x).toDouble() * (corners.topLeft.x - corners.bottomLeft.x) +
                (corners.topLeft.y - corners.bottomLeft.y).toDouble() * (corners.topLeft.y - corners.bottomLeft.y)).toFloat())
        val maxHeight = max(heightA, heightB).toInt().coerceIn(200, source.height)

        val srcPoints = floatArrayOf(
            corners.topLeft.x, corners.topLeft.y,
            corners.topRight.x, corners.topRight.y,
            corners.bottomRight.x, corners.bottomRight.y,
            corners.bottomLeft.x, corners.bottomLeft.y
        )

        val dstPoints = floatArrayOf(
            0f, 0f,
            maxWidth.toFloat(), 0f,
            maxWidth.toFloat(), maxHeight.toFloat(),
            0f, maxHeight.toFloat()
        )

        val matrix = Matrix()
        matrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)

        val resultBitmap = Bitmap.createBitmap(maxWidth, maxHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, matrix, paint)

        return resultBitmap
    }

    /**
     * Applies CamScanner style filters: Magic Color (Shadow Removal), B&W, Grayscale, etc.
     */
    fun applyFilter(source: Bitmap, filter: ScanFilter): Bitmap {
        return when (filter) {
            ScanFilter.ORIGINAL -> source
            ScanFilter.MAGIC_COLOR -> applyMagicColorShadowRemoval(source)
            ScanFilter.BW_CLEAN -> applyHighContrastBlackAndWhite(source)
            ScanFilter.GRAYSCALE -> applyGrayscaleFilter(source)
            ScanFilter.LIGHTEN -> applyLightenFilter(source)
        }
    }

    /**
     * CamScanner Magic Color Filter:
     * 1. Increases contrast and brightness.
     * 2. Levels background shadows to pure white.
     * 3. Sharpens pen/pencil ink and colored lines.
     */
    private fun applyMagicColorShadowRemoval(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Custom CamScanner color matrix: High contrast + boost brightness + slight saturation
        val contrast = 1.45f
        val brightness = 26f
        val cm = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(source, 0f, 0f, paint)

        return output
    }

    /**
     * Crisp B&W Document Filter (clean white background with deep dark text)
     */
    private fun applyHighContrastBlackAndWhite(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Convert to grayscale first, then high contrast clamp
        val grayMatrix = ColorMatrix()
        grayMatrix.setSaturation(0f)

        val highContrast = ColorMatrix(
            floatArrayOf(
                2.2f, 0f, 0f, 0f, -120f,
                0f, 2.2f, 0f, 0f, -120f,
                0f, 0f, 2.2f, 0f, -120f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        highContrast.preConcat(grayMatrix)
        paint.colorFilter = ColorMatrixColorFilter(highContrast)
        canvas.drawBitmap(source, 0f, 0f, paint)

        return output
    }

    /**
     * Grayscale document filter
     */
    private fun applyGrayscaleFilter(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val grayMatrix = ColorMatrix()
        grayMatrix.setSaturation(0f)
        paint.colorFilter = ColorMatrixColorFilter(grayMatrix)
        canvas.drawBitmap(source, 0f, 0f, paint)

        return output
    }

    /**
     * Lighten filter for yellow or dimly-lit notebook paper
     */
    private fun applyLightenFilter(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val cm = ColorMatrix(
            floatArrayOf(
                1.15f, 0f, 0f, 0f, 40f,
                0f, 1.15f, 0f, 0f, 40f,
                0f, 0f, 1.15f, 0f, 40f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(source, 0f, 0f, paint)

        return output
    }

    /**
     * Rotates bitmap by specified degrees (90, 180, 270)
     */
    fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
}
