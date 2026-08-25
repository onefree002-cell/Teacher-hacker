package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import androidx.camera.core.ImageProxy
import com.google.zxing.*
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.nio.ByteBuffer
import java.util.EnumMap

object QrBarcodeUtils {

    /**
     * Generates a crystal clear standard QR code bitmap
     */
    fun generateQrBitmap(
        content: String,
        size: Int = 400,
        darkColor: Int = Color.BLACK,
        lightColor: Int = Color.WHITE,
        margin: Int = 1
    ): Bitmap {
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H) // High error correction
            put(EncodeHintType.MARGIN, margin)
        }

        val writer = QRCodeWriter()
        val bitMatrix: BitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) darkColor else lightColor
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    /**
     * Decodes QR or Barcode from ImageProxy (CameraX frame)
     */
    fun decodeCameraFrame(image: ImageProxy): String? {
        val plane = image.planes[0]
        val buffer: ByteBuffer = plane.buffer
        val width = image.width
        val height = image.height
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride

        val yBytes = ByteArray(width * height)
        val rowBuffer = ByteArray(rowStride)

        buffer.rewind()
        for (row in 0 until height) {
            val offset = row * rowStride
            if (offset + width <= buffer.capacity()) {
                buffer.position(offset)
                if (pixelStride == 1) {
                    buffer.get(yBytes, row * width, width)
                } else {
                    buffer.get(rowBuffer, 0, minOf(rowStride, buffer.remaining()))
                    for (col in 0 until width) {
                        yBytes[row * width + col] = rowBuffer[col * pixelStride]
                    }
                }
            }
        }

        val rotationDegrees = image.imageInfo.rotationDegrees

        val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
            put(
                DecodeHintType.POSSIBLE_FORMATS,
                listOf(
                    BarcodeFormat.QR_CODE,
                    BarcodeFormat.CODE_128,
                    BarcodeFormat.CODE_39,
                    BarcodeFormat.CODE_93,
                    BarcodeFormat.EAN_13,
                    BarcodeFormat.EAN_8,
                    BarcodeFormat.UPC_A,
                    BarcodeFormat.UPC_E,
                    BarcodeFormat.DATA_MATRIX,
                    BarcodeFormat.ITF
                )
            )
            put(DecodeHintType.TRY_HARDER, java.lang.Boolean.TRUE)
            put(DecodeHintType.CHARACTER_SET, "UTF-8")
        }

        val reader = MultiFormatReader()

        // Apply rotation if needed
        val source = if (rotationDegrees == 90 || rotationDegrees == 270) {
            val rotatedBytes = ByteArray(width * height)
            if (rotationDegrees == 90) {
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        rotatedBytes[x * height + (height - y - 1)] = yBytes[y * width + x]
                    }
                }
            } else {
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        rotatedBytes[(width - x - 1) * height + y] = yBytes[y * width + x]
                    }
                }
            }
            PlanarYUVLuminanceSource(rotatedBytes, height, width, 0, 0, height, width, false)
        } else if (rotationDegrees == 180) {
            val rotatedBytes = ByteArray(width * height)
            for (i in 0 until width * height) {
                rotatedBytes[i] = yBytes[width * height - 1 - i]
            }
            PlanarYUVLuminanceSource(rotatedBytes, width, height, 0, 0, width, height, false)
        } else {
            PlanarYUVLuminanceSource(yBytes, width, height, 0, 0, width, height, false)
        }

        try {
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            return reader.decode(binaryBitmap, hints).text
        } catch (_: Exception) {}

        try {
            val invertedSource = source.invert()
            val invertedBitmap = BinaryBitmap(HybridBinarizer(invertedSource))
            return reader.decode(invertedBitmap, hints).text
        } catch (_: Exception) {}

        if (rotationDegrees != 0) {
            try {
                val rawSource = PlanarYUVLuminanceSource(yBytes, width, height, 0, 0, width, height, false)
                val rawBitmap = BinaryBitmap(HybridBinarizer(rawSource))
                return reader.decode(rawBitmap, hints).text
            } catch (_: Exception) {}
        }

        return null
    }

    /**
     * Decodes text from a Bitmap
     */
    fun decodeBitmap(bitmap: Bitmap): String? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val source = RGBLuminanceSource(width, height, pixels)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

        val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
            put(DecodeHintType.TRY_HARDER, java.lang.Boolean.TRUE)
            put(DecodeHintType.CHARACTER_SET, "UTF-8")
        }

        return try {
            val reader = MultiFormatReader()
            reader.decode(binaryBitmap, hints).text
        } catch (e: Exception) {
            try {
                val reader = MultiFormatReader()
                val invertedBitmap = BinaryBitmap(HybridBinarizer(source.invert()))
                reader.decode(invertedBitmap, hints).text
            } catch (_: Exception) {
                null
            }
        }
    }
}
