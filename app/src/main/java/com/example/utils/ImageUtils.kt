package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.LinkedList
import java.util.Queue
import kotlin.math.abs

object ImageUtils {

    /**
     * Removes solid/near-solid background from an image bitmap (such as white, light gray, black, or custom color).
     * Automatically identifies corner/border background colors and floods or converts pixels to transparent alpha.
     */
    suspend fun removeBackground(
        context: Context,
        inputUri: Uri,
        tolerance: Int = 35
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(inputUri) ?: return@withContext null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) return@withContext null

            val width = originalBitmap.width
            val height = originalBitmap.height

            // Ensure mutable ARGB_8888 bitmap
            val transparentBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)

            // Sample corners to detect background color
            val c1 = originalBitmap.getPixel(0, 0)
            val c2 = originalBitmap.getPixel(width - 1, 0)
            val c3 = originalBitmap.getPixel(0, height - 1)
            val c4 = originalBitmap.getPixel(width - 1, height - 1)

            // Target background color (most common corner color or white/near-white by default)
            val targetColor = c1

            // Flood-fill / edge-based alpha transparency
            val visited = Array(width) { BooleanArray(height) }
            val queue: Queue<Pair<Int, Int>> = LinkedList()

            // Seed queue with all borders
            for (x in 0 until width) {
                if (colorMatches(originalBitmap.getPixel(x, 0), targetColor, tolerance)) {
                    queue.add(Pair(x, 0))
                    visited[x][0] = true
                }
                if (colorMatches(originalBitmap.getPixel(x, height - 1), targetColor, tolerance)) {
                    queue.add(Pair(x, height - 1))
                    visited[x][height - 1] = true
                }
            }
            for (y in 0 until height) {
                if (colorMatches(originalBitmap.getPixel(0, y), targetColor, tolerance)) {
                    queue.add(Pair(0, y))
                    visited[0][y] = true
                }
                if (colorMatches(originalBitmap.getPixel(width - 1, y), targetColor, tolerance)) {
                    queue.add(Pair(width - 1, y))
                    visited[width - 1][y] = true
                }
            }

            val dx = intArrayOf(1, -1, 0, 0)
            val dy = intArrayOf(0, 0, 1, -1)

            // Process BFS to turn connected background pixels transparent
            while (queue.isNotEmpty()) {
                val (cx, cy) = queue.poll() ?: continue
                transparentBitmap.setPixel(cx, cy, Color.TRANSPARENT)

                for (i in 0 until 4) {
                    val nx = cx + dx[i]
                    val ny = cy + dy[i]

                    if (nx in 0 until width && ny in 0 until height && !visited[nx][ny]) {
                        visited[nx][ny] = true
                        val pixel = originalBitmap.getPixel(nx, ny)
                        if (colorMatches(pixel, targetColor, tolerance) || isNearWhiteOrLight(pixel, tolerance)) {
                            queue.add(Pair(nx, ny))
                        }
                    }
                }
            }

            // Also remove any remaining near-white pixels if target was white
            if (isNearWhiteOrLight(targetColor, 40)) {
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        val p = transparentBitmap.getPixel(x, y)
                        if (p != Color.TRANSPARENT && isNearWhiteOrLight(p, tolerance)) {
                            transparentBitmap.setPixel(x, y, Color.TRANSPARENT)
                        }
                    }
                }
            }

            // Save transparent PNG to local app storage
            val logoDir = File(context.filesDir, "logos")
            if (!logoDir.exists()) logoDir.mkdirs()
            val outputFile = File(logoDir, "teacher_logo_transparent_${System.currentTimeMillis()}.png")

            FileOutputStream(outputFile).use { fos ->
                transparentBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }

            Uri.fromFile(outputFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun colorMatches(c1: Int, c2: Int, tolerance: Int): Boolean {
        val rDiff = abs(Color.red(c1) - Color.red(c2))
        val gDiff = abs(Color.green(c1) - Color.green(c2))
        val bDiff = abs(Color.blue(c1) - Color.blue(c2))
        return rDiff <= tolerance && gDiff <= tolerance && bDiff <= tolerance
    }

    private fun isNearWhiteOrLight(color: Int, tolerance: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val threshold = 255 - tolerance
        return r >= threshold && g >= threshold && b >= threshold
    }
}
