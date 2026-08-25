package com.example.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PinLockManager {
    private const val PREFS_NAME = "teacher_planner_pin_prefs"
    private const val KEY_PIN_ENABLED = "pref_pin_enabled"
    private const val KEY_PIN_CODE = "pref_pin_code"

    private var prefs: SharedPreferences? = null
    
    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _isPinSet = MutableStateFlow(false)
    val isPinSet: StateFlow<Boolean> = _isPinSet.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val enabled = prefs?.getBoolean(KEY_PIN_ENABLED, false) ?: false
            val pin = prefs?.getString(KEY_PIN_CODE, "") ?: ""
            _isPinSet.value = pin.isNotEmpty() && enabled
            _isLocked.value = _isPinSet.value
        }
    }

    fun isPinEnabled(): Boolean {
        return prefs?.getBoolean(KEY_PIN_ENABLED, false) ?: false
    }

    fun checkPin(inputPin: String): Boolean {
        val savedPin = prefs?.getString(KEY_PIN_CODE, "") ?: ""
        return if (savedPin == inputPin) {
            _isLocked.value = false
            true
        } else {
            false
        }
    }

    fun setPin(pin: String) {
        prefs?.edit()
            ?.putString(KEY_PIN_CODE, pin)
            ?.putBoolean(KEY_PIN_ENABLED, true)
            ?.apply()
        _isPinSet.value = true
        _isLocked.value = false
    }

    fun disablePin() {
        prefs?.edit()
            ?.putBoolean(KEY_PIN_ENABLED, false)
            ?.putString(KEY_PIN_CODE, "")
            ?.apply()
        _isPinSet.value = false
        _isLocked.value = false
    }

    fun lockApp() {
        if (isPinEnabled()) {
            _isLocked.value = true
        }
    }

    fun unlockApp() {
        _isLocked.value = false
    }
}
