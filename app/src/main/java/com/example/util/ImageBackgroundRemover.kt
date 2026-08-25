package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.ArrayDeque
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object ImageBackgroundRemover {

    /**
     * Loads a bitmap from URI and removes its background (e.g. white, light gray, or solid corner color),
     * returning a transparent PNG file and Bitmap.
     */
    fun processAndSaveTransparentLogo(context: Context, imageUri: Uri, tolerance: Int = 40): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
            inputStream.close()

            val transparentBitmap = removeBackground(originalBitmap, tolerance)

            val dir = File(context.filesDir, "logos")
            if (!dir.exists()) dir.mkdirs()

            val outFile = File(dir, "logo_transparent_${System.currentTimeMillis()}.png")
            FileOutputStream(outFile).use { fos ->
                transparentBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            outFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Converts background colors (white, near-white, or corners color) into transparent pixels.
     * Uses color distance formula with soft edge feathering.
     */
    fun removeBackground(src: Bitmap, tolerance: Int = 40): Bitmap {
        val width = src.width
        val height = src.height

        val workingBitmap = if (src.config == Bitmap.Config.ARGB_8888 && src.isMutable) {
            src
        } else {
            src.copy(Bitmap.Config.ARGB_8888, true)
        }

        val pixels = IntArray(width * height)
        workingBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Sample 4 corners to detect background color
        val corner1 = pixels[0]
        val corner2 = pixels[width - 1]
        val corner3 = pixels[(height - 1) * width]
        val corner4 = pixels[width * height - 1]

        val bgR = (Color.red(corner1) + Color.red(corner2) + Color.red(corner3) + Color.red(corner4)) / 4
        val bgG = (Color.green(corner1) + Color.green(corner2) + Color.green(corner3) + Color.green(corner4)) / 4
        val bgB = (Color.blue(corner1) + Color.blue(corner2) + Color.blue(corner3) + Color.blue(corner4)) / 4

        val tolSq = (tolerance * tolerance * 3).toDouble()
        val featherTolSq = ((tolerance + 30) * (tolerance + 30) * 3).toDouble()

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val a = Color.alpha(pixel)
            if (a == 0) continue

            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)

            // Distance to detected corner background
            val dR = (r - bgR).toDouble()
            val dG = (g - bgG).toDouble()
            val dB = (b - bgB).toDouble()
            val distCornerSq = dR * dR + dG * dG + dB * dB

            // Also check for pure white/near white (standard for scanned/saved logos)
            val dWhiteSq = ((255 - r) * (255 - r) + (255 - g) * (255 - g) + (255 - b) * (255 - b)).toDouble()

            val effectiveDistSq = min(distCornerSq, dWhiteSq)

            if (effectiveDistSq <= tolSq) {
                pixels[i] = 0 // Fully transparent
            } else if (effectiveDistSq < featherTolSq) {
                // Soft alpha transition for smooth anti-aliased edges
                val ratio = (effectiveDistSq - tolSq) / (featherTolSq - tolSq)
                val newAlpha = (a * ratio).toInt().coerceIn(0, 255)
                pixels[i] = Color.argb(newAlpha, r, g, b)
            }
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }
}
