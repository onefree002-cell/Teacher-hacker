package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.SessionEntity
import com.example.receiver.SessionAlarmReceiver
import java.text.SimpleDateFormat
import java.util.*

object SessionAlarmScheduler {

    const val ACTION_SESSION_ALARM = "com.example.ACTION_SESSION_ALARM"

    const val EXTRA_SESSION_ID = "extra_session_id"
    const val EXTRA_GROUP_ID = "extra_group_id"
    const val EXTRA_GROUP_NAME = "extra_group_name"
    const val EXTRA_SESSION_TIME = "extra_session_time"
    const val EXTRA_SESSION_DAY = "extra_session_day"
    const val EXTRA_LOCATION = "extra_location"
    const val EXTRA_DURATION = "extra_duration"
    const val EXTRA_MINUTES_BEFORE = "extra_minutes_before"

    /**
     * Schedules an exact alarm before the session start time.
     */
    fun scheduleSessionAlarm(
        context: Context,
        session: SessionEntity,
        groupName: String,
        minutesBefore: Int = AppPreferencesManager.sessionAlertMinutesBefore.value
    ) {
        if (!AppPreferencesManager.sessionAlertsEnabled.value) {
            return
        }

        val triggerMillis = calculateNextTriggerMillis(
            dayName = session.day,
            dateStr = session.date,
            timeStr = session.time,
            minutesBefore = minutesBefore
        )

        if (triggerMillis <= System.currentTimeMillis()) {
            Log.w("SessionAlarmScheduler", "Calculated trigger time is in the past for session ${session.id}")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, SessionAlarmReceiver::class.java).apply {
            action = ACTION_SESSION_ALARM
            putExtra(EXTRA_SESSION_ID, session.id)
            putExtra(EXTRA_GROUP_ID, session.groupId)
            putExtra(EXTRA_GROUP_NAME, groupName)
            putExtra(EXTRA_SESSION_TIME, session.time)
            putExtra(EXTRA_SESSION_DAY, session.day)
            putExtra(EXTRA_LOCATION, session.location)
            putExtra(EXTRA_DURATION, session.durationMinutes)
            putExtra(EXTRA_MINUTES_BEFORE, minutesBefore)
        }

        val requestCode = (session.id % 50000).toInt() + 1000
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
            Log.d("SessionAlarmScheduler", "Alarm scheduled successfully for session ${session.id} at timestamp $triggerMillis")
        } catch (e: SecurityException) {
            Log.e("SessionAlarmScheduler", "SecurityException scheduling alarm: ${e.message}")
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } catch (ex: Exception) {
                Log.e("SessionAlarmScheduler", "Fallback alarm failed", ex)
            }
        } catch (e: Exception) {
            Log.e("SessionAlarmScheduler", "Error scheduling alarm", e)
        }
    }

    /**
     * Cancels a scheduled session alarm.
     */
    fun cancelSessionAlarm(context: Context, sessionId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, SessionAlarmReceiver::class.java).apply {
            action = ACTION_SESSION_ALARM
        }
        val requestCode = (sessionId % 50000).toInt() + 1000
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("SessionAlarmScheduler", "Cancelled alarm for session $sessionId")
        }
    }

    /**
     * Reschedules alarms for all active sessions.
     */
    fun rescheduleAllSessions(
        context: Context,
        sessions: List<SessionEntity>,
        groups: List<GroupEntity>
    ) {
        if (!AppPreferencesManager.sessionAlertsEnabled.value) {
            return
        }
        val groupMap = groups.associateBy { it.id }
        val minutesBefore = AppPreferencesManager.sessionAlertMinutesBefore.value
        for (session in sessions) {
            val gName = groupMap[session.groupId]?.name ?: "مجموعة دراسية"
            scheduleSessionAlarm(context, session, gName, minutesBefore)
        }
    }

    /**
     * Calculates the exact epoch milliseconds for the upcoming trigger time.
     */
    fun calculateNextTriggerMillis(
        dayName: String,
        dateStr: String,
        timeStr: String,
        minutesBefore: Int
    ): Long {
        val minutesFromMidnight = TimeUtils.timeToMinutes(timeStr)
        val validMinutes = if (minutesFromMidnight in 0..1439) minutesFromMidnight else 16 * 60 // Default 4:00 PM if unparseable
        val hour = validMinutes / 60
        val minute = validMinutes % 60

        val now = Calendar.getInstance()

        // 1. If explicit date is provided ("yyyy-MM-dd")
        if (dateStr.isNotBlank()) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val parsedDate = sdf.parse(dateStr)
                if (parsedDate != null) {
                    val cal = Calendar.getInstance().apply {
                        time = parsedDate
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        add(Calendar.MINUTE, -minutesBefore)
                    }
                    if (cal.timeInMillis > now.timeInMillis) {
                        return cal.timeInMillis
                    }
                }
            } catch (e: Exception) {
                // fallback to day-of-week
            }
        }

        // 2. Day-of-week based recurring schedule
        val targetDayOfWeek = getDayOfWeekCalendarConstant(dayName)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, -minutesBefore)
        }

        if (targetDayOfWeek != null) {
            val currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK)
            var daysDifference = (targetDayOfWeek - currentDayOfWeek + 7) % 7
            cal.add(Calendar.DAY_OF_YEAR, daysDifference)

            // If it resolved to today but time is already past, jump to next week
            if (cal.timeInMillis <= now.timeInMillis + 30000) {
                cal.add(Calendar.DAY_OF_YEAR, 7)
            }
        } else {
            // No specific day match, if time passed today add 1 day
            if (cal.timeInMillis <= now.timeInMillis + 30000) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        return cal.timeInMillis
    }

    private fun getDayOfWeekCalendarConstant(dayName: String): Int? {
        val d = dayName.trim()
        return when {
            d.contains("سبت") -> Calendar.SATURDAY
            d.contains("أحد") || d.contains("احد") -> Calendar.SUNDAY
            d.contains("إثنين") || d.contains("اثنين") -> Calendar.MONDAY
            d.contains("ثلاثاء") || d.contains("تلات") -> Calendar.TUESDAY
            d.contains("أربعاء") || d.contains("اربعاء") -> Calendar.WEDNESDAY
            d.contains("خميس") -> Calendar.THURSDAY
            d.contains("جمعة") || d.contains("جمعه") -> Calendar.FRIDAY
            else -> null
        }
    }
}
