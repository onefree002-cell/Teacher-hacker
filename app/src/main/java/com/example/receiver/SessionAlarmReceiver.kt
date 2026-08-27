package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.entity.SessionEntity
import com.example.util.AppPreferencesManager
import com.example.util.SchoolBellSoundManager
import com.example.util.SessionAlarmScheduler
import com.example.util.SessionNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SessionAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        Log.d("SessionAlarmReceiver", "Received action: $action")

        AppPreferencesManager.init(context)

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getInstance(context)
                        val sessions = db.sessionDao().getAllSessionsList()
                        val groups = db.groupDao().getAllGroupsList()
                        SessionAlarmScheduler.rescheduleAllSessions(context, sessions, groups)
                    } catch (e: Exception) {
                        Log.e("SessionAlarmReceiver", "Error on boot reschedule", e)
                    }
                }
            }
            SessionAlarmScheduler.ACTION_SESSION_ALARM -> {
                val sessionId = intent.getLongExtra(SessionAlarmScheduler.EXTRA_SESSION_ID, 0L)
                val groupId = intent.getLongExtra(SessionAlarmScheduler.EXTRA_GROUP_ID, 0L)
                val groupName = intent.getStringExtra(SessionAlarmScheduler.EXTRA_GROUP_NAME) ?: "مجموعة دراسية"
                val sessionTime = intent.getStringExtra(SessionAlarmScheduler.EXTRA_SESSION_TIME) ?: ""
                val sessionDay = intent.getStringExtra(SessionAlarmScheduler.EXTRA_SESSION_DAY) ?: ""
                val location = intent.getStringExtra(SessionAlarmScheduler.EXTRA_LOCATION) ?: ""
                val duration = intent.getIntExtra(SessionAlarmScheduler.EXTRA_DURATION, 60)
                val minutesBefore = intent.getIntExtra(SessionAlarmScheduler.EXTRA_MINUTES_BEFORE, 15)

                if (!AppPreferencesManager.sessionAlertsEnabled.value) {
                    Log.d("SessionAlarmReceiver", "Session alerts are disabled by user")
                    return
                }

                val soundId = AppPreferencesManager.sessionAlertSound.value
                val vibrate = AppPreferencesManager.sessionAlertVibration.value

                // 1. Play chosen school bell sound
                SchoolBellSoundManager.playAlertSound(context, soundId, vibrate)

                // 2. Show notification
                val dummySession = SessionEntity(
                    id = sessionId,
                    groupId = groupId,
                    day = sessionDay,
                    time = sessionTime,
                    durationMinutes = duration,
                    location = location
                )
                SessionNotificationHelper.showSessionUpcomingReminder(
                    context = context,
                    session = dummySession,
                    groupName = groupName,
                    location = location,
                    minutesBefore = minutesBefore
                )

                // 3. Reschedule for next week
                SessionAlarmScheduler.scheduleSessionAlarm(
                    context = context,
                    session = dummySession,
                    groupName = groupName,
                    minutesBefore = minutesBefore
                )
            }
        }
    }
}
