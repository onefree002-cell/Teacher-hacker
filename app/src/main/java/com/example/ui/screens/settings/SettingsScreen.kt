package com.example.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToVenues: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToCertificates: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToCurriculum: () -> Unit = {},
    onNavigateToQuestionBank: () -> Unit = {},
    onNavigateToSmartPrep: () -> Unit = {},
    onNavigateToPoster: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val themeState by ThemeManager.themeState.collectAsState()
    val currentLanguage by com.example.util.LocaleManager.currentLanguage.collectAsState()
    val isPinSet by com.example.util.PinLockManager.isPinSet.collectAsState()
    var showPinDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showTourDialog by remember { mutableStateOf(false) }
    var showAlarmSettingsDialog by remember { mutableStateOf(false) }
    var pinInputValue by remember { mutableStateOf("") }
    var pinErrorMessage by remember { mutableStateOf<String?>(null) }

    if (showAlarmSettingsDialog) {
        com.example.ui.screens.schedule.SessionAlarmSettingsDialog(
            onDismiss = { showAlarmSettingsDialog = false },
            onSettingsChanged = {
                // Settings updated
            }
        )
    }

    if (showLanguageDialog) {
        com.example.ui.components.LanguageSelectionDialog(onDismiss = { showLanguageDialog = false })
    }

    if (showTourDialog) {
        com.example.ui.components.AppGuidedTourDialog(onDismiss = { showTourDialog = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "الإعدادات العامة",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            // ==========================================
            // THEME & DISPLAY CUSTOMIZER (ثيمات العرض)
            // ==========================================
            item {
                SectionHeader(title = "مظهر وثيمات التطبيق (Theme & Colors)")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Quick Switch for Dark Mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (themeState.mode == AppThemeMode.DARK) Color(0xFF334155) else Color(0xFFFEF3C7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (themeState.mode == AppThemeMode.DARK) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                        contentDescription = null,
                                        tint = if (themeState.mode == AppThemeMode.DARK) Color(0xFFFBBF24) else Color(0xFFD97706),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "الوضع الليلي (Dark Mode)",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "راحة لعين المعلم أثناء التحضير والاستخدام الليلي",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = themeState.mode == AppThemeMode.DARK,
                                onCheckedChange = { isDark ->
                                    ThemeManager.setMode(if (isDark) AppThemeMode.DARK else AppThemeMode.LIGHT)
                                },
                                modifier = Modifier.testTag("dark_mode_quick_switch")
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "تخصيص نمط الإضاءة والألوان",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        // Mode Selector (System, Light, Dark)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeModeChip(
                                title = "نهاري ☀️",
                                isSelected = themeState.mode == AppThemeMode.LIGHT,
                                onClick = { ThemeManager.setMode(AppThemeMode.LIGHT) },
                                modifier = Modifier.weight(1f).testTag("theme_mode_light")
                            )
                            ThemeModeChip(
                                title = "ليلي 🌙",
                                isSelected = themeState.mode == AppThemeMode.DARK,
                                onClick = { ThemeManager.setMode(AppThemeMode.DARK) },
                                modifier = Modifier.weight(1f).testTag("theme_mode_dark")
                            )
                            ThemeModeChip(
                                title = "تلقائي ⚙️",
                                isSelected = themeState.mode == AppThemeMode.SYSTEM,
                                onClick = { ThemeManager.setMode(AppThemeMode.SYSTEM) },
                                modifier = Modifier.weight(1f).testTag("theme_mode_system")
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Text(
                            "اختر ثيم الألوان المفضل لديك:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // 6 Theme Palettes
                        AppThemePalette.entries.forEach { palette ->
                            ThemePaletteCard(
                                palette = palette,
                                isSelected = themeState.palette == palette,
                                onClick = { ThemeManager.setPalette(palette) }
                            )
                        }
                    }
                }
            }

            // ==========================================
            // LANGUAGE SELECTION (تغيير لغة التطبيق)
            // ==========================================
            item {
                SectionHeader(title = "لغة التطبيق (App Language)")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SettingsItemRow(
                        icon = Icons.Filled.Language,
                        title = "لغة واجهة التطبيق",
                        subtitle = "اللغة الحالية: ${currentLanguage.flag} ${currentLanguage.displayName} (انقر للتغيير)",
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = { showLanguageDialog = true },
                        tag = "settings_language_row"
                    )
                }
            }

            // ==========================================
            // DATA & ACCOUNT MANAGEMENT
            // ==========================================
            item {
                SectionHeader(title = "إدارة البيانات والحساب")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        SettingsItemRow(
                            icon = Icons.Filled.Person,
                            title = "بيانات المعلم والسنتر",
                            subtitle = "تعديل الاسم والتخصص والمسمى وأرقام الهواتف",
                            iconTint = MaterialTheme.colorScheme.primary,
                            onClick = onNavigateToProfile,
                            tag = "settings_profile_row"
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        SettingsItemRow(
                            icon = Icons.Filled.NotificationsActive,
                            title = "تنبيهات وجرس الحصص (School Bell)",
                            subtitle = "تخصيص رنين جرس المدرسة، أوقات التنبيه المسبقة، والاهتزاز",
                            iconTint = NavyPrimary,
                            onClick = { showAlarmSettingsDialog = true },
                            tag = "settings_session_alarms_row"
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        SettingsItemRow(
                            icon = Icons.Filled.LocationCity,
                            title = "أماكن وقاعات الدروس والسناتر",
                            subtitle = "إدارة مقرات التدريس ونسب وإيجار السناتر",
                            iconTint = AmberGoldDark,
                            onClick = onNavigateToVenues,
                            tag = "settings_venues_row"
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        SettingsItemRow(
                            icon = Icons.Filled.Backup,
                            title = "النسخ الاحتياطي وتليجرام",
                            subtitle = "النسخ التلقائي، الإرسال لتليجرام، واستعادة البيانات",
                            iconTint = EmeraldGreen,
                            onClick = onNavigateToBackup,
                            tag = "settings_backup_row"
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        SettingsItemRow(
                            icon = Icons.Filled.Search,
                            title = "البحث الشامل في التطبيق",
                            subtitle = "البحث الفوري في كافة الأقسام والبيانات",
                            iconTint = MaterialTheme.colorScheme.secondary,
                            onClick = onNavigateToSearch,
                            tag = "settings_search_row"
                        )
                    }
                }
            }

            // ==========================================
            // REPORTS & EXPORTS
            // ==========================================
            item {
                SectionHeader(title = "التقارير والمطبوعات")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        SettingsItemRow(
                            icon = Icons.Filled.Assessment,
                            title = "مركز التقارير وتصدير Excel",
                            subtitle = "توليد كشوفات PDF، ملفات الطلاب المجمعة، وكشف الدرجات",
                            iconTint = MaterialTheme.colorScheme.primary,
                            onClick = onNavigateToReports,
                            tag = "settings_reports_row"
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        SettingsItemRow(
                            icon = Icons.Filled.MenuBook,
                            title = "خطة المنهج وتوزيع الدروس",
                            subtitle = "إدارة الوحدات، الدروس، نسب الإنجاز، وربطها بالسنوات والمواد",
                            iconTint = EmeraldSuccess,
                            onClick = onNavigateToCurriculum,
                            tag = "settings_curriculum_row"
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        SettingsItemRow(
                            icon = Icons.Filled.Psychology,
                            title = "التحضير الذكي للدروس وتوليد الأفكار",
                            subtitle = "إدارة خطط الدروس، الأهداف، التمهيد، خريطة الشرح، والتقويم بضغطة زر",
                            iconTint = NavyPrimary,
                            onClick = onNavigateToSmartPrep,
                            tag = "settings_smart_prep_row"
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        SettingsItemRow(
                            icon = Icons.Filled.Quiz,
                            title = "بنك الأسئلة وتوليد الشيتات والامتحانات",
                            subtitle = "تخزين أسئلة اختيار من متعدد ومقالية، مستويات صعوبة، وتوليد فوري",
                            iconTint = PurpleAccent,
                            onClick = onNavigateToQuestionBank,
                            tag = "settings_question_bank_row"
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        SettingsItemRow(
                            icon = Icons.Filled.WorkspacePremium,
                            title = "مصمم شهادات التقدير والتفوق (5 ثيمات)",
                            subtitle = "تخصيص القوالب، الشعار، حذف الخلفية، وطباعة مجمعة",
                            iconTint = AmberGoldDark,
                            onClick = onNavigateToCertificates,
                            tag = "settings_cert_row"
                        )
                    }
                }
            }

            // ==========================================
            // SECURITY & APP LOCK (قفل التطبيق برمز PIN)
            // ==========================================
            item {
                SectionHeader(title = "الأمان وقفل التطبيق")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isPinSet) EmeraldSuccessContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            if (isPinSet) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                            contentDescription = null,
                                            tint = if (isPinSet) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "رمز القفل السري (PIN Lock)",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        if (isPinSet) "رمز الحماية مفعل حالياً" else "غير مفعل - انقر لتعيين رمز قفل",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isPinSet) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = isPinSet,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        pinInputValue = ""
                                        pinErrorMessage = null
                                        showPinDialog = true
                                    } else {
                                        com.example.util.PinLockManager.disablePin()
                                    }
                                },
                                modifier = Modifier.testTag("pin_lock_switch")
                            )
                        }
                    }
                }
            }

            // ==========================================
            // ABOUT APP & FEATURES
            // ==========================================
            item {
                SectionHeader(title = "المميزات وعن التطبيق")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "هاكر التدريس (The Hacker)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "المنظومة الرقمية الشاملة للمعلم الذكي",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "نظام احترافي لإدارة المعلم والمجموعات التعليمية والسناتر بالكامل محلياً وبكل أمان وسرعة وبأحدث التقنيات.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { showTourDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("settings_start_tour_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AmberGoldDark)
                        ) {
                            Icon(Icons.Filled.Tour, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("الجولة التعريفية الشاملة بالتطبيق 🚀", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        FilledTonalButton(
                            onClick = onNavigateToPoster,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_view_poster_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("المميزات: عرض واستكشاف كافة مميزات المنظومة 🌟", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Direct Developer Contact Card (Telegram)
                        val context = LocalContext.current
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF229ED9).copy(alpha = 0.12f)
                            ),
                            border = BorderStroke(1.2.dp, Color(0xFF229ED9).copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/abdoaiman01"))
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Ignore or fallback
                                    }
                                }
                                .testTag("contact_developer_telegram_card")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF229ED9)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.Send,
                                            contentDescription = "Telegram",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            "تواصل مع مطور التطبيق 💬",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "تيليجرام: @abdoaiman01 (دعم فني واقتراحات)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF0284C7)
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF229ED9)
                                ) {
                                    Text(
                                        "مراسلة",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            icon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("تعيين رمز القفل السري (PIN)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("أدخل رمز PIN مكون من 4 إلى 6 أرقام لحماية التطبيق عند الفتح:", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = pinInputValue,
                        onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) pinInputValue = it },
                        label = { Text("رمز PIN (أرقام فقط)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinErrorMessage != null) {
                        Text(pinErrorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInputValue.length in 4..6) {
                            com.example.util.PinLockManager.setPin(pinInputValue)
                            showPinDialog = false
                        } else {
                            pinErrorMessage = "يجب أن يتكون الرمز من 4 إلى 6 أرقام"
                        }
                    }
                ) {
                    Text("حفظ وتفعيل")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun ThemeModeChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
        ),
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ThemePaletteCard(
    palette: AppThemePalette,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) palette.containerColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) palette.primaryColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("theme_card_${palette.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color Swatches
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(palette.primaryColor)
                )
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(palette.secondaryColor)
                )
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(palette.containerColor)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = palette.titleArabic,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) palette.primaryColor else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = palette.description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "مفعل",
                    tint = palette.primaryColor,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                RadioButton(
                    selected = false,
                    onClick = onClick,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color,
    onClick: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Icon(Icons.Filled.ChevronLeft, contentDescription = null, tint = Color.Gray)
    }
}

