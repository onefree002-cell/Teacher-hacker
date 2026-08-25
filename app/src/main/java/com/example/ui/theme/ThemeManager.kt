package com.example.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemePalette(
    val id: String,
    val titleArabic: String,
    val description: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val tertiaryColor: Color,
    val containerColor: Color,
    val darkPrimaryColor: Color
) {
    ROYAL_NAVY(
        id = "royal_navy",
        titleArabic = "الأزرق الملكي",
        description = "الثيم الكلاسيكي الراقي مع لمسات ذهبية",
        primaryColor = Color(0xFF1E3A8A),
        secondaryColor = Color(0xFFD97706),
        tertiaryColor = Color(0xFF059669),
        containerColor = Color(0xFFDBEAFE),
        darkPrimaryColor = Color(0xFF3B82F6)
    ),
    EMERALD_GREEN(
        id = "emerald_green",
        titleArabic = "الزمردي الراقي",
        description = "أخضر زمردي هادئ ومريح للعين مع أصفر دافئ",
        primaryColor = Color(0xFF065F46),
        secondaryColor = Color(0xFFD97706),
        tertiaryColor = Color(0xFF0284C7),
        containerColor = Color(0xFFD1FAE5),
        darkPrimaryColor = Color(0xFF10B981)
    ),
    IMPERIAL_PURPLE(
        id = "imperial_purple",
        titleArabic = "البنفسجي الإمبراطوري",
        description = "أرجواني ملكي فاخر يمنح الواجهة طابعاً تعليمياً مميزاً",
        primaryColor = Color(0xFF6B21A8),
        secondaryColor = Color(0xFFF59E0B),
        tertiaryColor = Color(0xFF2563EB),
        containerColor = Color(0xFFF3E8FF),
        darkPrimaryColor = Color(0xFFA855F7)
    ),
    OCEAN_BLUE(
        id = "ocean_blue",
        titleArabic = "الأزرق المحيطي",
        description = "تدرجات التيل والمحيط الحيوية العصرية",
        primaryColor = Color(0xFF0E7490),
        secondaryColor = Color(0xFF0284C7),
        tertiaryColor = Color(0xFF059669),
        containerColor = Color(0xFFCFFAFE),
        darkPrimaryColor = Color(0xFF06B6D4)
    ),
    WARM_SUNSET(
        id = "warm_sunset",
        titleArabic = "الغروب الدافئ",
        description = "ألوان تيراكوتا والنحاس الدافئة المليئة بالطاقة",
        primaryColor = Color(0xFFC2410C),
        secondaryColor = Color(0xFFD97706),
        tertiaryColor = Color(0xFF059669),
        containerColor = Color(0xFFFFEDD5),
        darkPrimaryColor = Color(0xFFFB923C)
    ),
    DARK_ONYX(
        id = "dark_onyx",
        titleArabic = "الأونيكس والذهب الفاخر",
        description = "ثيم عالي التباين داكن وفاخر بلمسات ذهبية متألقة",
        primaryColor = Color(0xFFD97706),
        secondaryColor = Color(0xFFF59E0B),
        tertiaryColor = Color(0xFF10B981),
        containerColor = Color(0xFFFEF3C7),
        darkPrimaryColor = Color(0xFFFBBF24)
    );

    companion object {
        fun fromId(id: String): AppThemePalette {
            return entries.find { it.id == id } ?: ROYAL_NAVY
        }
    }
}

enum class AppThemeMode(val id: String, val titleArabic: String) {
    SYSTEM("system", "تلقائي مع النظام"),
    LIGHT("light", "الوضع النهاري"),
    DARK("dark", "الوضع الليلي");

    companion object {
        fun fromId(id: String): AppThemeMode {
            return entries.find { it.id == id } ?: SYSTEM
        }
    }
}

data class ThemeState(
    val palette: AppThemePalette = AppThemePalette.ROYAL_NAVY,
    val mode: AppThemeMode = AppThemeMode.SYSTEM
)

object ThemeManager {
    private const val PREFS_NAME = "teacher_planner_theme_prefs"
    private const val KEY_PALETTE = "pref_theme_palette"
    private const val KEY_MODE = "pref_theme_mode"

    private var prefs: SharedPreferences? = null
    private val _themeState = MutableStateFlow(ThemeState())
    val themeState: StateFlow<ThemeState> = _themeState.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedPaletteId = prefs?.getString(KEY_PALETTE, AppThemePalette.ROYAL_NAVY.id) ?: AppThemePalette.ROYAL_NAVY.id
            val savedModeId = prefs?.getString(KEY_MODE, AppThemeMode.SYSTEM.id) ?: AppThemeMode.SYSTEM.id
            _themeState.value = ThemeState(
                palette = AppThemePalette.fromId(savedPaletteId),
                mode = AppThemeMode.fromId(savedModeId)
            )
        }
    }

    fun setPalette(palette: AppThemePalette) {
        _themeState.value = _themeState.value.copy(palette = palette)
        prefs?.edit()?.putString(KEY_PALETTE, palette.id)?.apply()
    }

    fun setMode(mode: AppThemeMode) {
        _themeState.value = _themeState.value.copy(mode = mode)
        prefs?.edit()?.putString(KEY_MODE, mode.id)?.apply()
    }
}
