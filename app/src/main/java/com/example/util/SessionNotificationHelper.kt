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
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.SessionEntity
import com.example.data.local.entity.StudentEntity

object SessionNotificationHelper {

    private const val CHANNEL_ID_SESSIONS = "teacher_planner_sessions_channel"
    private const val CHANNEL_NAME_SESSIONS = "تنبيهات ومواعيد الحصص"
    private const val CHANNEL_DESC_SESSIONS = "تنبيه المعلم قبل بدء الحصة بـ 30 دقيقة وإشعارات التأجيل والإلغاء"

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
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Sends an immediate reminder notification for a session starting in 30 minutes.
     */
    fun showSessionUpcomingReminder(
        context: Context,
        session: SessionEntity,
        groupName: String,
        location: String
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            session.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val locationText = if (location.isNotBlank()) " | المكان: $location" else ""
        val builder = NotificationCompat.Builder(context, CHANNEL_ID_SESSIONS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⏰ تذكير: حصة بعد 30 دقيقة!")
            .setContentText("مجموعة: $groupName | الموعد: ${session.time}$locationText")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "تذكير للمعلم: موعد حصة مجموعة [$groupName] سيبدأ بعد 30 دقيقة في تمام الساعة ${session.time}.\n" +
                            "المكان: ${location.ifBlank { "السنتر المعتاد" }}\n" +
                            "مدة الحصة: ${session.durationMinutes} دقيقة."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(session.id.toInt() + 1000, builder.build())
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
