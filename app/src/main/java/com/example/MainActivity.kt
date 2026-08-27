package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PinLockScreen
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.MyApplicationTheme
import com.example.util.PinLockManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            com.example.ui.theme.ThemeManager.init(applicationContext)
            PinLockManager.init(applicationContext)
            com.example.util.AppPreferencesManager.init(applicationContext)
            com.example.util.LocaleManager.init(applicationContext)
            com.example.util.SessionNotificationHelper.createNotificationChannels(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            enableEdgeToEdge()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        setContent {
            MyApplicationTheme {
                val currentLanguage by com.example.util.LocaleManager.currentLanguage.collectAsState()
                val layoutDirection = if (currentLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                CompositionLocalProvider(
                    LocalLayoutDirection provides layoutDirection,
                    com.example.util.LocalAppLanguage provides currentLanguage
                ) {
                    key(currentLanguage) {
                        val isLocked by PinLockManager.isLocked.collectAsState()

                        if (isLocked) {
                            PinLockScreen(
                                onUnlocked = {
                                    PinLockManager.unlockApp()
                                }
                            )
                        } else {
                            Surface(modifier = Modifier.fillMaxSize()) {
                                AppNavigation()
                            }
                        }
                    }
                }
            }
        }
    }
}

