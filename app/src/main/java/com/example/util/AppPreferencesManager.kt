package com.example.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

object AppPreferencesManager {
    private const val PREFS_NAME = "the_hacker_app_prefs"
    private const val KEY_FIRST_LAUNCH_COMPLETED = "pref_first_launch_completed"
    private const val KEY_HAS_SEEN_TOUR = "pref_has_seen_tour"
    private const val KEY_AUTO_BACKUP_INTERVAL = "pref_auto_backup_interval" // "daily", "weekly", "monthly", "on_change", "disabled"
    private const val KEY_AUTO_BACKUP_TARGET = "pref_auto_backup_target" // "telegram", "local", "both"
    private const val KEY_TELEGRAM_NUMBER = "pref_telegram_number"
    private const val KEY_LAST_BACKUP_TIME = "pref_last_backup_time"
    private const val KEY_SESSION_ALERTS_ENABLED = "pref_session_alerts_enabled"
    private const val KEY_SESSION_ALERT_MINUTES_BEFORE = "pref_session_alert_minutes_before"
    private const val KEY_SESSION_ALERT_SOUND = "pref_session_alert_sound"
    private const val KEY_SESSION_ALERT_VIBRATION = "pref_session_alert_vibration"

    private var prefs: SharedPreferences? = null

    private val _isFirstLaunchCompleted = MutableStateFlow(false)
    val isFirstLaunchCompleted: StateFlow<Boolean> = _isFirstLaunchCompleted.asStateFlow()

    private val _hasSeenTour = MutableStateFlow(false)
    val hasSeenTour: StateFlow<Boolean> = _hasSeenTour.asStateFlow()

    private val _autoBackupInterval = MutableStateFlow("daily")
    val autoBackupInterval: StateFlow<String> = _autoBackupInterval.asStateFlow()

    private val _autoBackupTarget = MutableStateFlow("telegram")
    val autoBackupTarget: StateFlow<String> = _autoBackupTarget.asStateFlow()

    private val _telegramNumber = MutableStateFlow("")
    val telegramNumber: StateFlow<String> = _telegramNumber.asStateFlow()

    private val _lastBackupTime = MutableStateFlow(0L)
    val lastBackupTime: StateFlow<Long> = _lastBackupTime.asStateFlow()

    private val _sessionAlertsEnabled = MutableStateFlow(true)
    val sessionAlertsEnabled: StateFlow<Boolean> = _sessionAlertsEnabled.asStateFlow()

    private val _sessionAlertMinutesBefore = MutableStateFlow(15)
    val sessionAlertMinutesBefore: StateFlow<Int> = _sessionAlertMinutesBefore.asStateFlow()

    private val _sessionAlertSound = MutableStateFlow("school_bell")
    val sessionAlertSound: StateFlow<String> = _sessionAlertSound.asStateFlow()

    private val _sessionAlertVibration = MutableStateFlow(true)
    val sessionAlertVibration: StateFlow<Boolean> = _sessionAlertVibration.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            _isFirstLaunchCompleted.value = prefs?.getBoolean(KEY_FIRST_LAUNCH_COMPLETED, false) ?: false
            _hasSeenTour.value = prefs?.getBoolean(KEY_HAS_SEEN_TOUR, false) ?: false
            _autoBackupInterval.value = prefs?.getString(KEY_AUTO_BACKUP_INTERVAL, "daily") ?: "daily"
            _autoBackupTarget.value = prefs?.getString(KEY_AUTO_BACKUP_TARGET, "telegram") ?: "telegram"
            _telegramNumber.value = prefs?.getString(KEY_TELEGRAM_NUMBER, "") ?: ""
            _lastBackupTime.value = prefs?.getLong(KEY_LAST_BACKUP_TIME, 0L) ?: 0L
            _sessionAlertsEnabled.value = prefs?.getBoolean(KEY_SESSION_ALERTS_ENABLED, true) ?: true
            _sessionAlertMinutesBefore.value = prefs?.getInt(KEY_SESSION_ALERT_MINUTES_BEFORE, 15) ?: 15
            _sessionAlertSound.value = prefs?.getString(KEY_SESSION_ALERT_SOUND, "school_bell") ?: "school_bell"
            _sessionAlertVibration.value = prefs?.getBoolean(KEY_SESSION_ALERT_VIBRATION, true) ?: true
        }
    }

    fun isFirstLaunch(): Boolean {
        return !(prefs?.getBoolean(KEY_FIRST_LAUNCH_COMPLETED, false) ?: false)
    }

    fun setFirstLaunchCompleted(completed: Boolean) {
        prefs?.edit()?.putBoolean(KEY_FIRST_LAUNCH_COMPLETED, completed)?.apply()
        _isFirstLaunchCompleted.value = completed
    }

    fun hasSeenTour(): Boolean {
        return prefs?.getBoolean(KEY_HAS_SEEN_TOUR, false) ?: false
    }

    fun setHasSeenTour(seen: Boolean) {
        prefs?.edit()?.putBoolean(KEY_HAS_SEEN_TOUR, seen)?.apply()
        _hasSeenTour.value = seen
    }

    fun setAutoBackupSettings(
        interval: String,
        target: String = _autoBackupTarget.value,
        telegram: String = _telegramNumber.value
    ) {
        prefs?.edit()
            ?.putString(KEY_AUTO_BACKUP_INTERVAL, interval)
            ?.putString(KEY_AUTO_BACKUP_TARGET, target)
            ?.putString(KEY_TELEGRAM_NUMBER, telegram)
            ?.apply()
        _autoBackupInterval.value = interval
        _autoBackupTarget.value = target
        _telegramNumber.value = telegram
    }

    fun setTelegramNumber(number: String) {
        prefs?.edit()?.putString(KEY_TELEGRAM_NUMBER, number)?.apply()
        _telegramNumber.value = number
    }

    fun recordBackupPerformed() {
        val now = System.currentTimeMillis()
        prefs?.edit()?.putLong(KEY_LAST_BACKUP_TIME, now)?.apply()
        _lastBackupTime.value = now
    }

    fun setSessionAlertsEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_SESSION_ALERTS_ENABLED, enabled)?.apply()
        _sessionAlertsEnabled.value = enabled
    }

    fun setSessionAlertMinutesBefore(minutes: Int) {
        prefs?.edit()?.putInt(KEY_SESSION_ALERT_MINUTES_BEFORE, minutes)?.apply()
        _sessionAlertMinutesBefore.value = minutes
    }

    fun setSessionAlertSound(sound: String) {
        prefs?.edit()?.putString(KEY_SESSION_ALERT_SOUND, sound)?.apply()
        _sessionAlertSound.value = sound
    }

    fun setSessionAlertVibration(vibration: Boolean) {
        prefs?.edit()?.putBoolean(KEY_SESSION_ALERT_VIBRATION, vibration)?.apply()
        _sessionAlertVibration.value = vibration
    }

    fun shouldPerformScheduledBackup(): Boolean {
        val interval = _autoBackupInterval.value
        if (interval == "disabled") return false
        val lastTime = _lastBackupTime.value
        if (lastTime == 0L) return true

        val now = System.currentTimeMillis()
        val diffMillis = now - lastTime
        return when (interval) {
            "daily" -> diffMillis >= TimeUnit.DAYS.toMillis(1)
            "weekly" -> diffMillis >= TimeUnit.DAYS.toMillis(7)
            "monthly" -> diffMillis >= TimeUnit.DAYS.toMillis(30)
            "on_change" -> true
            else -> false
        }
    }
}
