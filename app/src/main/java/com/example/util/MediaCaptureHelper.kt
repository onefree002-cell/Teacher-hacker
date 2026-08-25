package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object MediaCaptureHelper {

    fun createTempCameraImageUri(context: Context): Pair<File, Uri>? {
        return try {
            val photosDir = File(context.cacheDir, "homework_photos").apply {
                if (!exists()) mkdirs()
            }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
            val imageFile = File(photosDir, "HW_IMG_$timeStamp.jpg")
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, imageFile)
            Pair(imageFile, uri)
        } catch (e: Exception) {
            Log.e("MediaCaptureHelper", "Failed to create temp camera uri", e)
            null
        }
    }

    fun saveImageUriToLocalStorage(context: Context, sourceUri: Uri, prefix: String = "HOMEWORK"): String? {
        return try {
            val internalPhotosDir = File(context.filesDir, "homework_photos").apply {
                if (!exists()) mkdirs()
            }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
            val targetFile = File(internalPhotosDir, "${prefix}_$timeStamp.jpg")

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            targetFile.absolutePath
        } catch (e: Exception) {
            Log.e("MediaCaptureHelper", "Failed to copy image to local storage", e)
            null
        }
    }

    fun shareMediaWithWhatsApp(
        context: Context,
        phoneNumber: String?,
        messageText: String,
        mediaFilePath: String? = null
    ) {
        try {
            val cleanPhone = phoneNumber?.replace(Regex("[^0-9]"), "") ?: ""
            val formattedPhone = when {
                cleanPhone.startsWith("01") -> "2$cleanPhone"
                cleanPhone.startsWith("1") && cleanPhone.length == 10 -> "20$cleanPhone"
                cleanPhone.startsWith("+") -> cleanPhone.substring(1)
                else -> cleanPhone
            }

            val intent = Intent(Intent.ACTION_SEND).apply {
                if (mediaFilePath != null && File(mediaFilePath).exists()) {
                    val file = File(mediaFilePath)
                    val authority = "${context.packageName}.fileprovider"
                    val contentUri = FileProvider.getUriForFile(context, authority, file)
                    
                    val mime = when {
                        file.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp") -> "image/*"
                        file.extension.lowercase() in listOf("m4a", "aac", "mp3", "3gp", "wav") -> "audio/*"
                        else -> "*/*"
                    }
                    type = mime
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    type = "text/plain"
                }

                putExtra(Intent.EXTRA_TEXT, messageText)
                if (formattedPhone.isNotEmpty()) {
                    putExtra("jid", "$formattedPhone@s.whatsapp.net")
                    setPackage("com.whatsapp")
                }
            }

            val chooser = Intent.createChooser(intent, "إرسال عبر واتساب أو التطبيقات")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e("MediaCaptureHelper", "Error sharing media", e)
            // Fallback: send text only or open browser
            WhatsAppHelper.sendMessage(context, phoneNumber ?: "", messageText)
        }
    }
}
