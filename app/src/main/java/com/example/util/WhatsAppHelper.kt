package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder

object WhatsAppHelper {

    private fun formatNumber(num: Double): String {
        return if (num % 1.0 == 0.0) "${num.toInt()}" else String.format(java.util.Locale.US, "%.1f", num)
    }

    fun formatPhoneNumber(phone: String): String {
        var clean = TimeUtils.normalizeDigits(phone)
            .replace("+", "")
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
            .trim()
        
        // Convert local Egyptian number like 010... or 011... to 2010... / 2011...
        if (clean.startsWith("01")) {
            clean = "2$clean"
        }
        return clean
    }

    fun openWhatsApp(context: Context, phoneNumber: String, messageText: String) {
        val formattedNumber = formatPhoneNumber(phoneNumber)
        try {
            val encodedMessage = URLEncoder.encode(messageText, "UTF-8")
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedNumber&text=$encodedMessage")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح تطبيق واتساب. تأكد من تثبيته على جهازك", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWhatsAppGroupLink(context: Context, groupUrl: String) {
        try {
            val url = if (!groupUrl.startsWith("http://") && !groupUrl.startsWith("https://")) {
                "https://$groupUrl"
            } else {
                groupUrl
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح رابط الجروب. يرجى التأكد من الرابط", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendMessage(context: Context, phoneNumber: String, messageText: String) {
        openWhatsApp(context, phoneNumber, messageText)
    }

    fun createWhatsAppGroupInviteMessage(
        groupName: String,
        groupLink: String,
        teacherName: String,
        subject: String
    ): String {
        return """
السلام عليكم ورحمة الله وبركاته،
السادة أولياء أمور وطلاب مجموعة *$groupName* المحترمين 🌸

نرجو التكرم بالانضمام إلى جروب الواتساب الرسمي للمجموعة لمتابعة المواعيد، الواجبات، نتائج الاختبارات والإعلانات الهامة:

🔗 *رابط الانضمام للجروب:*
$groupLink

نتمنى لأبنائنا دوام التوفيق والتفوق 🌟
أستاذ المادة: *$teacherName* ($subject)
        """.trimIndent()
    }

    fun createGeneralFollowUpMessage(
        studentName: String,
        messageContent: String,
        teacherName: String,
        gender: String = "boy"
    ): String {
        val isGirl = gender == "girl" || gender == "female"
        val guardianSalutation = if (isGirl) "ولي أمر الطالبة المحترمة / $studentName" else "ولي أمر الطالب المحترم / $studentName"
        return """
السلام عليكم ورحمة الله وبركاته،
$guardianSalutation

$messageContent

مع خالص تحيات وتقدير،
أستاذ المادة: $teacherName
        """.trimIndent()
    }

    // ==========================================
    // DYNAMIC CONTEXT-AWARE PRE-MADE TEMPLATES
    // ==========================================
    fun createAbsenceMessage(
        studentName: String,
        date: String,
        groupName: String,
        teacherName: String,
        gender: String = "boy"
    ): String {
        val isGirl = gender == "girl" || gender == "female"
        val guardianSalutation = if (isGirl) "ولي أمر الطالبة المحترمة / $studentName" else "ولي أمر الطالب المحترم / $studentName"
        val absenceBody = if (isGirl) {
            "نحيط سيادتكم علماً بأن الطالبة قد تغيبت اليوم ($date) عن حضور حصة مادة المنهج في مجموعة ($groupName).\n\nيرجى التواصل معنا لتنسيق تعويض الحصة ومعرفة سبب الغياب، حرصاً على مستوى الطالبة وعدم تراكم الدروس عليها."
        } else {
            "نحيط سيادتكم علماً بأن الطالب قد تغيب اليوم ($date) عن حضور حصة مادة المنهج في مجموعة ($groupName).\n\nيرجى التواصل معنا لتنسيق تعويض الحصة ومعرفة سبب الغياب، حرصاً على مستوى الطالب وعدم تراكم الدروس عليه."
        }

        return """
السلام عليكم ورحمة الله وبركاته،
$guardianSalutation

$absenceBody

شاكرين لسيادتكم دوام المتابعة والحرص،
أستاذ المادة: $teacherName
        """.trimIndent()
    }

    fun createExamGradeMessage(
        studentName: String,
        examTitle: String,
        score: Double,
        maxScore: Double,
        teacherName: String,
        gender: String = "boy"
    ): String {
        val isGirl = gender == "girl" || gender == "female"
        val validMax = if (maxScore > 0) maxScore else 100.0
        val pct = (score / validMax) * 100.0
        val scoreStr = formatNumber(score)
        val maxScoreStr = formatNumber(validMax)
        val pctFormatted = String.format(java.util.Locale.US, "%.0f", pct)

        val rating: String
        val dynamicFeedback: String

        when {
            pct >= 85.0 -> {
                rating = if (isGirl) "ممتازة ومتفوقة 🌟🥇" else "ممتاز ومتفوق 🌟🥇"
                dynamicFeedback = if (isGirl) {
                    "ما شاء الله تبارك الله! أداء رائع ومتميز جداً يعكس الجهد والتركيز العالي، متمنيين لها دوام النجاح والتفوق الباهر والصدارة دائماً 👏🎉"
                } else {
                    "ما شاء الله تبارك الله! أداء رائع ومتميز جداً يعكس الجهد والتركيز العالي، متمنيين له دوام النجاح والتفوق الباهر والصدارة دائماً 👏🎉"
                }
            }
            pct >= 65.0 -> {
                rating = "جيد جداً 👍"
                dynamicFeedback = "مستوى طيب ومشجع مع إمكانية الوصول للامتياز بالتركيز على حل مزيد من التمارين والمراجعة المستمرة 👏📚"
            }
            pct >= 50.0 -> {
                rating = "مقبول ويحتاج لمزيد من الاجتهاد ⚠️"
                dynamicFeedback = "مستوى مقبول ولكن نرجو الاهتمام أكثر بالمذاكرة وحل التدريبات والواجبات بانتظام لرفع المستوى في الامتحانات القادمة 📝"
            }
            else -> {
                rating = "ضعيف ويحتاج لمتابعة عاجلة 🔴"
                dynamicFeedback = "نحيط سيادتكم علماً بضرورة المتابعة والحرص والاهتمام لتعويض القصور ورفع المستوى في الحصص القادمة، والتأكد من مراجعة الأخطاء وحل الواجبات بانتظام ⚠️🔴"
            }
        }

        val guardianSalutation = if (isGirl) "ولي أمر الطالبة الكريمة / $studentName" else "ولي أمر الطالب الكريم / $studentName"

        return """
السلام عليكم ورحمة الله وبركاته،
$guardianSalutation

يسرنا إبلاغكم بنتيجة الامتحان الأخير:
📝 *الامتحان:* $examTitle
📊 *الدرجة:* $scoreStr من $maxScoreStr ($pctFormatted%)
🏆 *التقييم:* $rating

$dynamicFeedback

شاكرين لسيادتكم حسن التعاون والاهتمام،
أستاذ المادة: $teacherName
        """.trimIndent()
    }

    fun createPaymentReminderMessage(
        studentName: String,
        remainingAmount: Double,
        monthName: String,
        teacherName: String,
        gender: String = "boy"
    ): String {
        val isGirl = gender == "girl" || gender == "female"
        val amountStr = formatNumber(remainingAmount)
        val guardianSalutation = if (isGirl) "ولي أمر الطالبة الكريمة / $studentName" else "ولي أمر الطالب الكريم / $studentName"
        val targetStudent = if (isGirl) "للطالبة" else "للطالب"

        return """
السلام عليكم ورحمة الله وبركاته،
$guardianSalutation

نود تذكير سيادتكم بلطف بوجود مستحقات دراسية متبقية بقيمة:
💰 *المبلغ المستحق:* $amountStr ج.م ($monthName)

يرجى التكرم بسداد المبلغ في الحصة القادمة لتحديث السجل المالي $targetStudent.

شاكرين حسن تعاونكم،
أستاذ المادة: $teacherName
        """.trimIndent()
    }

    fun createExcellenceMessage(
        studentName: String,
        achievementText: String,
        teacherName: String,
        gender: String = "boy"
    ): String {
        val isGirl = gender == "girl" || gender == "female"
        val guardianSalutation = if (isGirl) "يسعدنا ويشرفنا تهنئة ولي أمر الطالبة المتميزة / $studentName" else "يسعدنا ويشرفنا تهنئة ولي أمر الطالب المتميز / $studentName"
        val wish = if (isGirl) "نتمنى لها دائماً دوام التفوق والصدارة في المراتب الأولى!" else "نتمنى له دائماً دوام التفوق والصدارة في المراتب الأولى!"

        return """
🎉 *شهادة تميز وتقدير* 🎉

السلام عليكم ورحمة الله وبركاته،
$guardianSalutation
تقديراً لـ: $achievementText 🌟

$wish

مع خالص تحيات وتقدير،
أستاذ المادة: $teacherName
        """.trimIndent()
    }

    fun createHomeworkReminderMessage(
        studentName: String,
        homeworkTitle: String,
        pages: String,
        deadline: String,
        teacherName: String,
        gender: String = "boy"
    ): String {
        val isGirl = gender == "girl" || gender == "female"
        val studentSalutation = if (isGirl) "عزيزتي الطالبة / $studentName" else "عزيزي الطالب / $studentName"

        return """
السلام عليكم ورحمة الله وبركاته،
$studentSalutation

تذكير بواجب الحصة القادمة:
📚 *الواجب:* $homeworkTitle
📄 *الصفحات والمسائل:* ${if (pages.isNotBlank()) pages else "المحددة بالحصة"}
⏰ *موعد التسليم:* ${if (deadline.isNotBlank()) deadline else "الحصة القادمة"}

يرجى الالتزام بالحل الكامل والمتقن وعدم التأجيل.

أستاذ المادة: $teacherName
        """.trimIndent()
    }
}
