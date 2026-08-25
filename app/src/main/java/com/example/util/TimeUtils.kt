package com.example.util

import java.text.SimpleDateFormat
import java.util.Locale

object TimeUtils {

    /**
     * Converts Arabic-Indic digits (٠١٢٣٤٥٦٧٨٩) to standard ASCII digits (0-9).
     */
    fun normalizeDigits(input: String): String {
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        var result = input
        for (i in arabicDigits.indices) {
            result = result.replace(arabicDigits[i], (i + '0'.code).toChar())
        }
        // Persian digits as well
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        for (i in persianDigits.indices) {
            result = result.replace(persianDigits[i], (i + '0'.code).toChar())
        }
        return result
    }

    /**
     * Parses time string in various formats ("HH:mm", "hh:mm a", "04:30 م", "8:00 ص", "16:00")
     * and returns the number of minutes from midnight (0..1439).
     * Returns 9999 if time cannot be parsed.
     */
    fun timeToMinutes(timeStr: String): Int {
        if (timeStr.isBlank()) return 9999
        val normalized = normalizeDigits(timeStr).trim().lowercase()

        // Check for Arabic/English AM/PM indicators
        val isPM = normalized.contains("م") || normalized.contains("pm") || normalized.contains("مساء")
        val isAM = normalized.contains("ص") || normalized.contains("am") || normalized.contains("صباح")

        // Extract hour and minute digits
        val clean = normalized
            .replace("م", "")
            .replace("ص", "")
            .replace("pm", "")
            .replace("am", "")
            .replace("مساءً", "")
            .replace("مساء", "")
            .replace("صباحاً", "")
            .replace("صباح", "")
            .trim()

        val parts = clean.split(Regex("[:.]"))
        if (parts.isNotEmpty()) {
            val h = parts[0].trim().toIntOrNull() ?: return 9999
            val m = if (parts.size > 1) parts[1].trim().toIntOrNull() ?: 0 else 0

            var finalHour = h
            if (isPM && finalHour < 12) {
                finalHour += 12
            } else if (isAM && finalHour == 12) {
                finalHour = 0
            }
            return finalHour * 60 + m
        }
        return 9999
    }

    /**
     * Formats 24h time ("16:00") into friendly Arabic display string ("04:00 مساءً").
     */
    fun formatTimeArabic(timeStr: String): String {
        if (timeStr.isBlank()) return ""
        val minutes = timeToMinutes(timeStr)
        if (minutes == 9999) return timeStr

        val h = minutes / 60
        val m = minutes % 60
        val isPM = h >= 12
        val displayHour = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        val period = if (isPM) "مساءً" else "صباحاً"
        val mStr = if (m < 10) "0$m" else "$m"
        val hStr = if (displayHour < 10) "0$displayHour" else "$displayHour"
        return "$hStr:$mStr $period"
    }

    /**
     * Day index for week sorting (Saturday = 0 .. Friday = 6).
     */
    fun dayOrderIndex(dayName: String): Int {
        val d = dayName.trim()
        return when {
            d.contains("سبت") -> 0
            d.contains("أحد") || d.contains("احد") -> 1
            d.contains("إثنين") || d.contains("اثنين") -> 2
            d.contains("ثلاثاء") || d.contains("تلات") -> 3
            d.contains("أربعاء") || d.contains("اربعاء") -> 4
            d.contains("خميس") -> 5
            d.contains("جمعة") || d.contains("جمعه") -> 6
            else -> 7
        }
    }
}
