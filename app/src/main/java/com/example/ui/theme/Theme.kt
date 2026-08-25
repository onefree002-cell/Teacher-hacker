package com.example.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

fun Context.findActivity(): Activity? {
    var currentContext: Context? = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

fun buildLightColorScheme(palette: AppThemePalette): ColorScheme {
    return lightColorScheme(
        primary = palette.primaryColor,
        onPrimary = Color.White,
        primaryContainer = palette.containerColor,
        onPrimaryContainer = palette.primaryColor,
        secondary = palette.secondaryColor,
        onSecondary = Color.White,
        secondaryContainer = palette.containerColor.copy(alpha = 0.5f),
        onSecondaryContainer = palette.secondaryColor,
        tertiary = palette.tertiaryColor,
        onTertiary = Color.White,
        background = SurfaceLight,
        surface = CardSurfaceLight,
        onBackground = TextPrimaryLight,
        onSurface = TextPrimaryLight,
        error = CrimsonError,
        errorContainer = CrimsonErrorContainer
    )
}

fun buildDarkColorScheme(palette: AppThemePalette): ColorScheme {
    return darkColorScheme(
        primary = palette.darkPrimaryColor,
        onPrimary = SurfaceDark,
        primaryContainer = Color(0xFF1E293B),
        onPrimaryContainer = palette.containerColor,
        secondary = palette.secondaryColor,
        onSecondary = SurfaceDark,
        secondaryContainer = Color(0xFF334155),
        onSecondaryContainer = palette.containerColor,
        tertiary = palette.tertiaryColor,
        onTertiary = SurfaceDark,
        background = SurfaceDark,
        surface = CardSurfaceDark,
        onBackground = TextPrimaryDark,
        onSurface = TextPrimaryDark,
        error = CrimsonError,
        errorContainer = CrimsonErrorContainer
    )
}

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        ThemeManager.init(context)
    }

    val themeState by ThemeManager.themeState.collectAsState()
    val isSystemDark = isSystemInDarkTheme()

    val isDark = when (themeState.mode) {
        AppThemeMode.SYSTEM -> isSystemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val colorScheme = if (isDark) {
        buildDarkColorScheme(themeState.palette)
    } else {
        buildLightColorScheme(themeState.palette)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

