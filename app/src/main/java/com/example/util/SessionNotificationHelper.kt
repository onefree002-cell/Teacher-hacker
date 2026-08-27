package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.entity.SessionEntity

object SessionNotificationHelper {

    const val CHANNEL_ID_SESSIONS = "teacher_planner_sessions_channel_v2"
    private const val CHANNEL_NAME_SESSIONS = "🔔 تنبيهات ومواعيد الحصص (جرس المدرسة)"
    private const val CHANNEL_DESC_SESSIONS = "تنبيهات بمواعيد الحصص ورنين جرس المدرسة وإشعارات المجموعات"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                CHANNEL_ID_SESSIONS,
                CHANNEL_NAME_SESSIONS,
                importance
            ).apply {
                description = CHANNEL_DESC_SESSIONS
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Sends a reminder notification for an upcoming session with School Bell alert.
     */
    fun showSessionUpcomingReminder(
        context: Context,
        session: SessionEntity,
        groupName: String,
        location: String,
        minutesBefore: Int = 15
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_NAVIGATE_TO", "schedule")
            putExtra("EXTRA_SESSION_ID", session.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            session.id.toInt() + 100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Attendance action
        val attendanceIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_NAVIGATE_TO", "attendance")
            putExtra("EXTRA_GROUP_ID", session.groupId)
        }
        val attendancePendingIntent = PendingIntent.getActivity(
            context,
            session.id.toInt() + 200,
            attendanceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val locationText = if (location.isNotBlank()) " | المقر: $location" else ""
        val timingText = if (minutesBefore > 0) "تبدأ بعد $minutesBefore دقيقة" else "تبدأ الآن"
        val titleText = "🔔 جرس الحصة: مجموعة $groupName ($timingText)"

        val contentBody = "تنبيه الحصة: موعد مجموعة [$groupName] في تمام ${session.time} يوم ${session.day}.\n" +
                "المكان: ${location.ifBlank { session.location.ifBlank { "المقر المعتاد" } }}\n" +
                "مدة الحصة: ${session.durationMinutes} دقيقة" +
                if (session.homeworkTitle.isNotBlank()) "\nالواجب المطلوب: ${session.homeworkTitle}" else ""

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_SESSIONS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(titleText)
            .setContentText("الموعد: ${session.time} | مجموعة $groupName$locationText")
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentBody))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "تسجيل الحضور 📋", attendancePendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "عرض الجدول 📅", pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify((session.id.toInt() + 5000), builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    /**
     * Test notification demonstrating the school bell class alarm.
     */
    fun showTestSessionAlarm(context: Context, soundTitle: String = "جرس المدرسة الكلاسيكي 🔔") {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            9999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_SESSIONS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🔔 تجربة تنبيه الحصة: رنين $soundTitle")
            .setContentText("موعد الحصة القادمة: 04:30 مساءً | مجموعة المتفوقين 3 ثانوي")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "تم إطلاق تجربة تنبيه موعد الحصة بنجاح مع نغمة ($soundTitle)!\n" +
                            "سيصلك هذا التنبيه التلقائي مع رنين الجرس قبل كل حصة بجدولك."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(9999, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    /**
     * Composes automated notification message for students/parents when a session is postponed.
     */
    fun generatePostponementMessage(
        teacherName: String,
        groupName: String,
        oldDateOrTime: String,
        newDateOrTime: String,
        reason: String
    ): String {
        return """
            📢 *تنبيه هام: تأجيل موعد الحصة*
            
            أولياء الأمور الكرام والطلاب الأعزاء بمجموعة *[$groupName]*،
            نود إحاطتكم بأنه تم تأجيل موعد الحصة المقرر:
            ⏰ *الموعد السابق:* $oldDateOrTime
            📅 *الموعد الجديد البديل:* $newDateOrTime
            ${if (reason.isNotBlank()) "📝 *السبب:* $reason\n" else ""}
            شاكرين حسن تفهمكم وتعاونكم الدائم.
            
            مع تحيات: *$teacherName*
        """.trimIndent()
    }

    /**
     * Composes automated notification message for students/parents when a session is cancelled.
     */
    fun generateCancellationMessage(
        teacherName: String,
        groupName: String,
        sessionTime: String,
        sessionDate: String,
        reason: String
    ): String {
        return """
            ⚠️ *تنبيه عاجل: إلغاء / اعتذار عن حصة اليوم*
            
            الطلاب الأعزاء وأولياء الأمور بمجموعة *[$groupName]*،
            نعتذر عن عدم إقامة حصة اليوم:
            📅 *التاريخ:* $sessionDate
            ⏰ *الساعة:* $sessionTime
            ${if (reason.isNotBlank()) "📝 *السبب:* $reason\n" else ""}
            سيتم إبلاغكم بالموعد التعويضي في أقرب وقت بإذن الله.
            
            خالص التحيات: *$teacherName*
        """.trimIndent()
    }
}
