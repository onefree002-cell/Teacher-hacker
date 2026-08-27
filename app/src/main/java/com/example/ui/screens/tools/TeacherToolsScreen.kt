package com.example.ui.screens.tools

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.export.PdfReportExporter
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.HomeworkSubmissionEntity
import com.example.data.local.entity.StudentEntity
import com.example.data.local.entity.VoiceNoteEntity
import com.example.ui.components.CasioCalculatorContent
import com.example.ui.components.EmptyStateWidget
import com.example.ui.theme.*
import com.example.util.MediaCaptureHelper
import com.example.util.WhatsAppHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TeacherToolsTab(
    val defaultTitle: String,
    val subtitle: String,
    val category: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val badgeColor: Color
) {
    HOMEWORK_SCANNER("تصوير وتصحيح الواجب", "مسح واجبات الطلاب وتوثيقها بالكاميرا", "الحصة والتدريس", Icons.Filled.CameraAlt, Color(0xFF3B82F6)),
    CASIO_CALC("آلة كاسيو العلمية 8-MODES", "حاسبة fx-991ES Plus العلمية بكافة المودات", "الرياضيات والحساب", Icons.Filled.Calculate, Color(0xFF0284C7)),
    VOICE_STUDIO("تسجيل الملاحظات الصوتية", "استوديو لتسجيل وتوثيق شرح الحصة والملاحظات", "الحصة والتدريس", Icons.Filled.Mic, Color(0xFFEF4444)),
    LUCKY_PICKER("القرعة الذكية للطلاب", "اختيار عشوائي للطلاب للإجابة والتفاعل", "الحصة والتدريس", Icons.Filled.Casino, AmberGold),
    CLASS_TIMER("مؤقت الحصة التفاعلي", "ضبط أوقات الاختبارات والأنشطة والمهام", "الحصة والتدريس", Icons.Filled.Timer, TealAccent),
    GRADE_CALC("حاسبة الدرجات والنسب", "حساب الدرجات المئوية والتقديرات وتوزيع الدرجات", "الرياضيات والحساب", Icons.Filled.Calculate, Color(0xFF8B5CF6)),
    TRANSLATOR("مترجم المصطلحات التربوي", "قاموس ومترجم لمصطلحات الرياضيات والعلوم واللغات", "الرياضيات والحساب", Icons.Filled.Translate, Color(0xFF2563EB)),
    BOOKLET_TRACKER("سجل تسليم المذكرات", "متابعة تسليم واستلام الشيتات والكتب والمصاريف", "الإدارة والطباعة", Icons.Filled.MenuBook, EmeraldSuccess),
    PORTFOLIO_CARDS("بورتفوليو وبطاقة المعلم", "شيت كروت شخصية وباركود تعريفي فاخر للطباعة", "الإدارة والطباعة", Icons.Filled.Badge, Color(0xFFD97706)),
    PRINT_HUB("مركز الطباعة السريع", "طباعة كارنيهات الطلاب، لوحة الشرف، وجداول الحصص", "الإدارة والطباعة", Icons.Filled.Print, NavyPrimary),
    TEMPLATES("قوالب رسائل الواتساب", "رسائل تشجيع وترحيب ومتابعة جاهزة للمشاركة", "الإدارة والطباعة", Icons.Filled.Send, Color(0xFFEC4899));

    fun getTitle(): String = when (this) {
        VOICE_STUDIO -> com.example.util.L.voiceStudio()
        HOMEWORK_SCANNER -> com.example.util.L.homeworkScanner()
        TRANSLATOR -> com.example.util.L.smartTranslator()
        LUCKY_PICKER -> com.example.util.L.luckyPicker()
        CLASS_TIMER -> com.example.util.L.classTimer()
        GRADE_CALC -> com.example.util.L.gradeCalculator()
        BOOKLET_TRACKER -> com.example.util.L.bookletTracker()
        PORTFOLIO_CARDS -> com.example.util.L.portfolioCards()
        CASIO_CALC -> com.example.util.L.casioCalculator()
        PRINT_HUB -> com.example.util.L.printHub()
        TEMPLATES -> com.example.util.L.templates()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherToolsScreen(
    viewModel: TeacherToolsViewModel,
    onNavigateBack: () -> Unit,
    initialTab: TeacherToolsTab? = null,
    onNavigateToSchedule: () -> Unit = {},
    onNavigateToStudents: () -> Unit = {},
    onNavigateToSmartPrep: () -> Unit = {},
    onNavigateToAiChat: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    // UI Mode: Hub Directory vs Active Tool View (Starts with Hub grid by default)
    var isHubView by remember(initialTab) { mutableStateOf(initialTab == null) }
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab ?: TeacherToolsTab.CASIO_CALC) }
    var isSidebarOpen by remember { mutableStateOf(false) }
    var hubSearchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("الكل") }

    // Intercept Back button: return to Hub directory if inside any specific tool
    androidx.activity.compose.BackHandler(enabled = !isHubView) {
        isHubView = true
    }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isHubView) "قائمة أدوات المعلم 🎓" else selectedTab.getTitle(),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            if (!isHubView) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = selectedTab.badgeColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = selectedTab.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = selectedTab.badgeColor,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (isHubView) "اختر الأداة المطلوبة أو تنقل عبر القائمة الجانبية" else selectedTab.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (!isHubView) {
                                isHubView = true
                            } else {
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier.testTag("teacher_tools_back_btn")
                    ) {
                        Icon(
                            imageVector = if (!isHubView) Icons.Filled.GridView else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (!isHubView) "كل الأدوات" else "رجوع"
                        )
                    }
                },
                actions = {
                    // Sidebar Toggle Button when in tool view
                    if (!isHubView) {
                        FilledTonalIconButton(
                            onClick = { isSidebarOpen = !isSidebarOpen },
                            modifier = Modifier.testTag("toggle_tools_sidebar_btn")
                        ) {
                            Icon(
                                imageVector = if (isSidebarOpen) Icons.Filled.Close else Icons.Filled.Menu,
                                contentDescription = "قائمة الأدوات الجانبية",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = onNavigateToAiChat,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(Icons.Filled.SmartToy, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF8B5CF6))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(if (com.example.util.L.isArabic()) "شات الذكاء" else "AI Chat", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = onNavigateToSmartPrep,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Icon(Icons.Filled.Psychology, contentDescription = null, modifier = Modifier.size(16.dp), tint = NavyPrimary)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(if (com.example.util.L.isArabic()) "التحضير الذكي" else "Smart Prep", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isHubView) {
                // ==========================================
                // 1. TOOLS HUB DIRECTORY (قائمة استكشاف كافة الأدوات)
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    // Search Bar
                    OutlinedTextField(
                        value = hubSearchQuery,
                        onValueChange = { hubSearchQuery = it },
                        placeholder = { Text("بحث في أدوات المعلم (كاسيو، مؤقت، تصوير، طباعة...)") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (hubSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { hubSearchQuery = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = null)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("tools_hub_search_bar")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Category filter chips
                    val categories = listOf("الكل", "الحصة والتدريس", "الرياضيات والحساب", "الإدارة والطباعة")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { cat ->
                            val isSel = selectedCategoryFilter == cat
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedCategoryFilter = cat },
                                label = { Text(cat, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filtered Tools Grid/List
                    val filteredTools = TeacherToolsTab.values().filter { tab ->
                        val matchesCat = selectedCategoryFilter == "الكل" || tab.category == selectedCategoryFilter
                        val matchesSearch = hubSearchQuery.isBlank() ||
                                tab.getTitle().contains(hubSearchQuery, ignoreCase = true) ||
                                tab.subtitle.contains(hubSearchQuery, ignoreCase = true) ||
                                tab.defaultTitle.contains(hubSearchQuery, ignoreCase = true)
                        matchesCat && matchesSearch
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredTools) { tab ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedTab = tab
                                        isHubView = false
                                    }
                                    .testTag("tool_card_${tab.name}")
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
                                                .size(46.dp)
                                                .clip(CircleShape)
                                                .background(tab.badgeColor.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = null,
                                                tint = tab.badgeColor,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = tab.getTitle(),
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = tab.badgeColor.copy(alpha = 0.1f)
                                                ) {
                                                    Text(
                                                        text = tab.category,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = tab.badgeColor,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = tab.subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "فتح الأداة",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF229ED9).copy(alpha = 0.12f)
                                ),
                                border = BorderStroke(1.dp, Color(0xFF229ED9).copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/abdoaiman01"))
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            // ignore
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF229ED9)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Filled.Send, contentDescription = "Telegram", tint = Color.White, modifier = Modifier.size(18.dp))
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                "تواصل مع صاحب التطبيق (تيليجرام)",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                "https://t.me/abdoaiman01",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF0284C7)
                                            )
                                        }
                                    }
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF229ED9))
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }
                }
            } else {
                // ==========================================
                // 2. ACTIVE TOOL WORKSPACE WITH SIDEBAR
                // ==========================================
                Row(modifier = Modifier.fillMaxSize()) {
                    // Main Tool Workspace
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        when (selectedTab) {
                            TeacherToolsTab.VOICE_STUDIO -> VoiceNotesStudioView(state, viewModel, context)
                            TeacherToolsTab.HOMEWORK_SCANNER -> HomeworkScannerView(state, viewModel, context)
                            TeacherToolsTab.TRANSLATOR -> SmartEducationalTranslatorView(state, viewModel, context)
                            TeacherToolsTab.LUCKY_PICKER -> LuckyStudentPickerView(state, viewModel, context)
                            TeacherToolsTab.CLASS_TIMER -> ClassroomTimerView(state, viewModel)
                            TeacherToolsTab.GRADE_CALC -> QuickGradeCalculatorView(state, viewModel)
                            TeacherToolsTab.BOOKLET_TRACKER -> BookletTrackerView(state, viewModel, context)
                            TeacherToolsTab.PORTFOLIO_CARDS -> TeacherPortfolioAndCardsView(state, context)
                            TeacherToolsTab.CASIO_CALC -> CasioCalculatorToolView()
                            TeacherToolsTab.PRINT_HUB -> QuickPrintHubView(state, viewModel, context, onNavigateToSchedule)
                            TeacherToolsTab.TEMPLATES -> MotivationTemplatesView(state, context)
                        }
                    }

                    // SIDEBAR DRAWER (الشريط الجانبي للتنقل الفوري بين الأدوات)
                    AnimatedVisibility(
                        visible = isSidebarOpen,
                        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier
                                .width(240.dp)
                                .fillMaxHeight()
                                .shadow(8.dp)
                                .testTag("teacher_tools_sidebar"),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            ) {
                                // Sidebar Header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "الأدوات الأخرى 🧰",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(
                                        onClick = { isSidebarOpen = false },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = "إغلاق الشريط الجانبي", modifier = Modifier.size(18.dp))
                                    }
                                }

                                // All Tools Hub Link Button
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            isHubView = true
                                            isSidebarOpen = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.GridView, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "كل الأدوات (دليل الأدوات)",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                Spacer(modifier = Modifier.height(6.dp))

                                // List of all Tools in Sidebar
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(TeacherToolsTab.values()) { tab ->
                                        val isCurrent = selectedTab == tab
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isCurrent) tab.badgeColor.copy(alpha = 0.18f) else Color.Transparent,
                                            border = if (isCurrent) BorderStroke(1.dp, tab.badgeColor) else null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedTab = tab
                                                    isSidebarOpen = false
                                                }
                                                .testTag("sidebar_tool_item_${tab.name}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(30.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isCurrent) tab.badgeColor else tab.badgeColor.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = tab.icon,
                                                        contentDescription = null,
                                                        tint = if (isCurrent) Color.White else tab.badgeColor,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(8.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = tab.getTitle(),
                                                        style = MaterialTheme.typography.labelMedium.copy(
                                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                                        ),
                                                        color = if (isCurrent) tab.badgeColor else MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = tab.category,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 9.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 1. VOICE NOTES & AUDIO STUDIO VIEW
// ==========================================
@Composable
private fun VoiceNotesStudioView(
    state: TeacherToolsUiState,
    viewModel: TeacherToolsViewModel,
    context: Context
) {
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            viewModel.startAudioRecording(context)
        } else {
            Toast.makeText(context, "يرجى منح إذن الميكروفون لتسجيل الصوت", Toast.LENGTH_SHORT).show()
        }
    }

    val categories = listOf("شرح درس", "توجيه لأولياء الأمور", "ملاحظة واجب", "تسجيل عام")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("voice_notes_studio_view"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero / Recording Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.isRecordingAudio) Color(0xFFFEE2E2) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = if (state.isRecordingAudio) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFEF4444)) else null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Mic, contentDescription = null, tint = Color(0xFFEF4444))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (state.isRecordingAudio) "🔴 جاري تسجيل الصوت..." else "استوديو التسجيلات الصوتية",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (state.isRecordingAudio) Color(0xFFB91C1C) else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (state.isRecordingAudio) "تحدث الآن بوضوح في الميكروفون" else "سجل شروحات وملاحظات للطلاب وأولياء الأمور",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (state.isRecordingAudio) {
                            val mins = state.recordingElapsedSeconds / 60
                            val secs = state.recordingElapsedSeconds % 60
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFDC2626)
                            ) {
                                Text(
                                    text = String.format(Locale.ENGLISH, "%02d:%02d", mins, secs),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title & Category Inputs (shown when not recording)
                    if (!state.isRecordingAudio) {
                        OutlinedTextField(
                            value = state.voiceNoteTitle,
                            onValueChange = { viewModel.setVoiceTitle(it) },
                            label = { Text("عنوان التسجيل (مثال: شرح مسألة صفحة 12)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("voice_note_title_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Category Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(categories) { cat ->
                                FilterChip(
                                    selected = state.voiceCategory == cat,
                                    onClick = { viewModel.setVoiceCategory(cat) },
                                    label = { Text(cat) },
                                    leadingIcon = {
                                        if (state.voiceCategory == cat) {
                                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Group Selector
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterChip(
                                    selected = state.voiceGroupId == 0L,
                                    onClick = { viewModel.setVoiceGroup(0L) },
                                    label = { Text("عام (كافة المجموعات)") }
                                )
                            }
                            items(state.groups) { g ->
                                FilterChip(
                                    selected = state.voiceGroupId == g.id,
                                    onClick = { viewModel.setVoiceGroup(g.id) },
                                    label = { Text(g.name) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                        // Dynamic Waveform Visualizer
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val amp = (state.recordingAmplitude / 1000).coerceIn(4, 45)
                            for (i in 0 until 16) {
                                val barHeight = (amp * ((i % 4) + 1) / 3).coerceIn(6, 45).dp
                                Box(
                                    modifier = Modifier
                                        .width(5.dp)
                                        .height(barHeight)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color(0xFFEF4444))
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Record Button with Animation
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = if (state.isRecordingAudio) 1.15f else 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseScale"
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (state.isRecordingAudio) {
                                    viewModel.stopAudioRecording(saveToDb = true)
                                } else {
                                    if (hasMicPermission) {
                                        viewModel.startAudioRecording(context)
                                    } else {
                                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.isRecordingAudio) Color(0xFFDC2626) else Color(0xFFEF4444)
                            ),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                            modifier = Modifier
                                .scale(pulseScale)
                                .testTag("record_audio_toggle_btn")
                        ) {
                            Icon(
                                if (state.isRecordingAudio) Icons.Filled.Stop else Icons.Filled.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (state.isRecordingAudio) "إيقاف وحفظ التسجيل" else "بدء التسجيل الآن",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        if (state.isRecordingAudio) {
                            OutlinedButton(
                                onClick = { viewModel.stopAudioRecording(saveToDb = false) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("إلغاء", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        // Voice Notes List Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🎙️ التسجيلات الصوتية المحفوظة (${state.voiceNotes.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        if (state.voiceNotes.isEmpty()) {
            item {
                EmptyStateWidget(
                    title = "لا توجد تسجيلات صوتية حتى الآن",
                    description = "اضغط على زر بدء التسجيل لتسجيل شروحات الحصص أو التوجيهات وإرسالها للطلاب عبر واتساب",
                    icon = Icons.Filled.MicNone
                )
            }
        } else {
            items(state.voiceNotes, key = { it.id }) { note ->
                VoiceNoteItemCard(
                    note = note,
                    state = state,
                    viewModel = viewModel,
                    context = context
                )
            }
        }
    }
}

@Composable
private fun VoiceNoteItemCard(
    note: VoiceNoteEntity,
    state: TeacherToolsUiState,
    viewModel: TeacherToolsViewModel,
    context: Context
) {
    val isCurrentPlaying = state.playingAudioPath == note.filePath && state.isPlayingAudio
    val isThisAudioSelected = state.playingAudioPath == note.filePath

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = { viewModel.playAudio(note.filePath, context) }) {
                            Icon(
                                if (isCurrentPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "تشغيل",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = note.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "⏱️ ${note.durationSeconds} ثانية • ${note.date}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { viewModel.shareVoiceNote(context, note) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "مشاركة عبر واتساب", tint = EmeraldSuccess, modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = { viewModel.deleteVoiceNote(note) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = "حذف", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Playback Slider (when this file is active)
            if (isThisAudioSelected && state.playbackTotalDurationMs > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = state.playbackPositionMs.toFloat(),
                    onValueChange = { viewModel.seekAudio(it.toInt()) },
                    valueRange = 0f..state.playbackTotalDurationMs.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFEF4444),
                        activeTrackColor = Color(0xFFEF4444)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ==========================================
// 2. HOMEWORK CAMERA SCANNER & GRADER VIEW
// ==========================================
@Composable
private fun HomeworkScannerView(
    state: TeacherToolsUiState,
    viewModel: TeacherToolsViewModel,
    context: Context
) {
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var viewingPhotoPath by remember { mutableStateOf<String?>(null) }
    var showPdfSuccessDialog by remember { mutableStateOf<File?>(null) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Camera Capture Launcher
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            val permanentPath = MediaCaptureHelper.saveImageUriToLocalStorage(context, tempCameraUri!!, "HW_CAM")
            if (permanentPath != null) {
                viewModel.addHwPhotoPath(permanentPath)
                Toast.makeText(context, "تم التقاط صفحة من الواجب بنجاح 📸", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            val pair = MediaCaptureHelper.createTempCameraImageUri(context)
            if (pair != null) {
                tempCameraUri = pair.second
                takePictureLauncher.launch(pair.second)
            }
        } else {
            Toast.makeText(context, "يرجى منح إذن الكاميرا لتصوير الواجبات", Toast.LENGTH_SHORT).show()
        }
    }

    // Gallery / File Picker Launcher
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                val permanentPath = MediaCaptureHelper.saveImageUriToLocalStorage(context, uri, "HW_FILE")
                if (permanentPath != null) {
                    viewModel.addHwPhotoPath(permanentPath)
                }
            }
            Toast.makeText(context, "تم إضافة ${uris.size} صورة للواجب 📁", Toast.LENGTH_SHORT).show()
        }
    }

    // Single file fallback
    val pickSingleImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val permanentPath = MediaCaptureHelper.saveImageUriToLocalStorage(context, uri, "HW_FILE")
            if (permanentPath != null) {
                viewModel.addHwPhotoPath(permanentPath)
                Toast.makeText(context, "تم اختيار صورة الواجب 📁", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val ratingOptions = listOf("حل كامل وممتاز 🌟", "حل جزئي (ناقص) ⚠️", "لم يحل الواجب ❌", "معفى من الواجب ⚪")

    val activeStudents = state.students.filter {
        it.status == "active" && (state.hwGroupId == 0L || it.groupId == state.hwGroupId)
    }

    // Success PDF Dialog
    showPdfSuccessDialog?.let { pdfFile ->
        val selectedStudent = state.students.find { it.id == state.hwStudentId }
        AlertDialog(
            onDismissRequest = { showPdfSuccessDialog = null },
            icon = {
                Icon(
                    Icons.Filled.PictureAsPdf,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "تم إنشاء ملف PDF للواجب بنجاح! 🎉",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "📄 اسم الملف:\n${pdfFile.name}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    Text(
                        text = "يمكنك الآن استعراض ملف الواجب، إرساله مباشرة لواتساب ولي الأمر، أو مشاركته عبر التطبيقات الأخرى.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parentPhone = selectedStudent?.parentPhone?.ifEmpty { selectedStudent.phone } ?: ""
                        val teacherName = state.teacher.name.ifEmpty { "معلم المادة" }
                        val caption = "📄 *ملف واجب الطالب: ${selectedStudent?.name ?: ""}*\nتم تصحيح وتوثيق الواجب بنجاح.\nمعلم المادة: $teacherName"
                        PdfReportExporter.sharePdfToWhatsApp(context, pdfFile, caption, parentPhone)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إرسال لواتساب ولي الأمر")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            PdfReportExporter.sharePdf(context, pdfFile, pdfFile.name)
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مشاركة")
                    }
                    TextButton(onClick = { showPdfSuccessDialog = null }) {
                        Text("تم")
                    }
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("homework_scanner_view"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Form & Capture Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3B82F6).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Color(0xFF3B82F6))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "تصوير وتوثيق واجب الطالب (PDF)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "تصوير كشكول الواجب وحفظه كملف PDF باسم 'واجب - اسم الطالب - تاريخ الحصة'",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Group Selector
                    Text("المجموعة:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = state.hwGroupId == 0L,
                                onClick = { viewModel.setHwGroup(0L) },
                                label = { Text("الكل") }
                            )
                        }
                        items(state.groups) { g ->
                            FilterChip(
                                selected = state.hwGroupId == g.id,
                                onClick = { viewModel.setHwGroup(g.id) },
                                label = { Text(g.name) }
                            )
                        }
                    }

                    // Student Selector
                    Text("الطالب المعني (مطلوب):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(activeStudents) { std ->
                            FilterChip(
                                selected = state.hwStudentId == std.id,
                                onClick = { viewModel.setHwStudent(std.id) },
                                label = { Text(std.name) },
                                leadingIcon = {
                                    if (state.hwStudentId == std.id) {
                                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            )
                        }
                    }

                    // Title & Date
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = state.hwTitle,
                            onValueChange = { viewModel.setHwTitle(it) },
                            label = { Text("عنوان الواجب") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.3f)
                        )
                        OutlinedTextField(
                            value = if (state.hwLessonDate.isBlank()) SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) else state.hwLessonDate,
                            onValueChange = { viewModel.setHwLessonDate(it) },
                            label = { Text("تاريخ الحصة") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Photo Action Buttons (Multi-Page Support)
                    Text("تصوير صفحات الواجب بالكاميرا أو المعرض:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (hasCameraPermission) {
                                    val pair = MediaCaptureHelper.createTempCameraImageUri(context)
                                    if (pair != null) {
                                        tempCameraUri = pair.second
                                        takePictureLauncher.launch(pair.second)
                                    }
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("capture_camera_btn")
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("التقاط صورة 📸")
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    pickImageLauncher.launch("image/*")
                                } catch (_: Exception) {
                                    pickSingleImageLauncher.launch("image/*")
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_gallery_btn")
                        ) {
                            Icon(Icons.Filled.Image, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("من المعرض 📁")
                        }
                    }

                    // Photos Thumbnails List (if captured)
                    val currentPhotos = if (state.hwPhotoPaths.isNotEmpty()) state.hwPhotoPaths else listOfNotNull(state.hwPhotoPath)
                    if (currentPhotos.isNotEmpty()) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📄 صفحات الواجب الملتقطة (${currentPhotos.size})",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = EmeraldSuccess
                                    )
                                    TextButton(onClick = { viewModel.clearHwPhotos() }) {
                                        Text("مسح الكل", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(currentPhotos.indices.toList()) { index ->
                                        val p = currentPhotos[index]
                                        Box(modifier = Modifier.size(74.dp)) {
                                            AsyncImage(
                                                model = File(p),
                                                contentDescription = "صفحة ${index + 1}",
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { viewingPhotoPath = p },
                                                contentScale = ContentScale.Crop
                                            )
                                            Surface(
                                                color = Color.Black.copy(alpha = 0.65f),
                                                shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
                                                modifier = Modifier.align(Alignment.TopStart)
                                            ) {
                                                Text(
                                                    text = "${index + 1}",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { viewModel.removeHwPhotoPath(index) },
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .align(Alignment.TopEnd)
                                                    .background(Color.Red, CircleShape)
                                            ) {
                                                Icon(Icons.Filled.Close, contentDescription = "حذف", tint = Color.White, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Homework Status / Rating Chips (تقييم حالة الواجب بدون درجات رقمية)
                    Text("حالة الواجب والتقييم:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(ratingOptions) { r ->
                            FilterChip(
                                selected = state.hwRating == r,
                                onClick = { viewModel.setHwRating(r) },
                                label = { Text(r) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = state.hwFeedbackNote,
                        onValueChange = { viewModel.setHwFeedbackNote(it) },
                        label = { Text("ملاحظات المعلم وتوجيهات التصحيح") },
                        placeholder = { Text("أحسنت في المسائل، انتبه لخطوات التعويض...") },
                        maxLines = 3,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Primary Button: Capture & Save Homework as PDF
                    Button(
                        onClick = {
                            viewModel.saveAndGenerateHomeworkPdf(context) { generatedFile ->
                                showPdfSuccessDialog = generatedFile
                            }
                        },
                        enabled = !state.isSavingHomework,
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_homework_btn")
                    ) {
                        if (state.isSavingHomework) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري توليد ملف PDF...")
                        } else {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = AmberGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("📸 تصوير وحفظ كملف PDF", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }

        // Submissions List Header
        item {
            Text(
                text = "📚 سجل الواجبات المصححة (${state.homeworkSubmissions.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (state.homeworkSubmissions.isEmpty()) {
            item {
                EmptyStateWidget(
                    title = "لا توجد واجبات مصححة في السجل",
                    description = "التقط صورة الواجب بالكاميرا أو اختر من الملفات ثم اضغط حفظ لتوليد ملف PDF وحفظ التقييم",
                    icon = Icons.Filled.Assignment
                )
            }
        } else {
            items(state.homeworkSubmissions, key = { it.id }) { hw ->
                val student = state.students.find { it.id == hw.studentId }
                val isPdf = hw.photoUri.endsWith(".pdf", ignoreCase = true)
                val targetFile = if (hw.photoUri.isNotEmpty()) File(hw.photoUri) else null

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            if (isPdf) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFDC2626).copy(alpha = 0.12f),
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clickable {
                                            if (targetFile != null && targetFile.exists()) {
                                                PdfReportExporter.sharePdf(context, targetFile, targetFile.name)
                                            }
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.PictureAsPdf, contentDescription = "PDF", tint = Color(0xFFDC2626), modifier = Modifier.size(28.dp))
                                    }
                                }
                            } else if (hw.photoUri.isNotEmpty() && File(hw.photoUri).exists()) {
                                AsyncImage(
                                    model = File(hw.photoUri),
                                    contentDescription = "معاينة",
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { viewingPhotoPath = hw.photoUri },
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF3B82F6).copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Assignment, contentDescription = null, tint = Color(0xFF3B82F6))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(student?.name ?: "طالب #${hw.studentId}", fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${hw.title} • ${hw.rating}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "تاريخ: ${hw.assignedDate}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (hw.feedbackNote.isNotEmpty()) {
                                    Text(
                                        text = hw.feedbackNote,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (targetFile != null && targetFile.exists()) {
                                IconButton(
                                    onClick = {
                                        val parentPhone = student?.parentPhone?.ifEmpty { student.phone } ?: ""
                                        val teacherName = state.teacher.name.ifEmpty { "معلم المادة" }
                                        val caption = "📄 *واجب الطالب: ${student?.name ?: ""}*\nالتقييم: ${hw.rating}\nمعلم المادة: $teacherName"
                                        if (isPdf) {
                                            PdfReportExporter.sharePdfToWhatsApp(context, targetFile, caption, parentPhone)
                                        } else {
                                            viewModel.shareHomeworkFeedback(context, hw)
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Filled.Send, contentDescription = "واتساب ولي الأمر", tint = EmeraldSuccess, modifier = Modifier.size(20.dp))
                                }
                                IconButton(
                                    onClick = {
                                        PdfReportExporter.sharePdf(context, targetFile, targetFile.name)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Filled.Share, contentDescription = "مشاركة", tint = NavyPrimary, modifier = Modifier.size(20.dp))
                                }
                            }
                            IconButton(
                                onClick = { viewModel.deleteHomework(hw) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Photo Fullscreen Zoom Dialog
    viewingPhotoPath?.let { path ->
        Dialog(onDismissRequest = { viewingPhotoPath = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📸 معاينة صورة الواجب", fontWeight = FontWeight.Bold)
                        IconButton(onClick = { viewingPhotoPath = null }) {
                            Icon(Icons.Filled.Close, contentDescription = "إغلاق")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = File(path),
                        contentDescription = "صورة الواجب كاملة",
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

// ==========================================
// 3. LUCKY STUDENT PICKER VIEW
// ==========================================
@Composable
private fun LuckyStudentPickerView(
    state: TeacherToolsUiState,
    viewModel: TeacherToolsViewModel,
    context: Context
) {
    val activeGroupStudents = if (state.pickerGroupId == 0L) {
        state.students.filter { it.status == "active" }
    } else {
        state.students.filter { it.groupId == state.pickerGroupId && it.status == "active" }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Casino, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "قرعة الطالب المحظوظ في الحصة 🎲",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "اختيار عشوائي عادل للطلاب للإجابة على الأسئلة أو التكريم",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Group Filter
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "اختر المجموعة:",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = state.pickerGroupId == 0L,
                            onClick = { viewModel.setPickerGroup(0L) },
                            label = { Text("كافة المجموعات (${state.students.count { it.status == "active" }})") }
                        )
                    }
                    items(state.groups) { group ->
                        val count = state.students.count { it.groupId == group.id && it.status == "active" }
                        FilterChip(
                            selected = state.pickerGroupId == group.id,
                            onClick = { viewModel.setPickerGroup(group.id) },
                            label = { Text("${group.name} ($count)") }
                        )
                    }
                }
            }
        }

        // Lucky Wheel / Display Box
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (state.selectedLuckyStudent != null) {
                        val student = state.selectedLuckyStudent
                        val grpName = state.groups.find { it.id == student.groupId }?.name ?: ""

                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(NavyPrimary, TealAccent))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = student.name.take(1),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = student.name,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )

                        if (grpName.isNotEmpty()) {
                            Text(
                                text = grpName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // WhatsApp Congrats Button
                        if (student.phone.isNotEmpty() || student.parentPhone.isNotEmpty()) {
                            val targetPhone = student.phone.ifEmpty { student.parentPhone }
                            FilledTonalButton(
                                onClick = {
                                    val teacherName = state.teacher.name.ifEmpty { "معلم المادة" }
                                    val msg = "🎉 مبروك للبطل المتميز *${student.name}* فوزه في قرعة الطالب المحظوظ في حصة أستاذ: *$teacherName* 🌟"
                                    WhatsAppHelper.sendMessage(context, targetPhone, msg)
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إرسال تهنئة عبر واتساب 💬")
                            }
                        }
                    } else {
                        Icon(
                            Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = AmberGold,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "اضغط على الزر أدناه لبدء السحب العشوائي",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { viewModel.pickLuckyStudent() },
                        enabled = !state.isPickingStudent && activeGroupStudents.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp),
                        modifier = Modifier.testTag("btn_spin_lucky_student")
                    ) {
                        Icon(Icons.Filled.Casino, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (state.isPickingStudent) "جاري السحب العشوائي... 🎲" else "سحب اسم الطالب الآن 🎯",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. CLASSROOM COUNTDOWN TIMER VIEW
// ==========================================
@Composable
private fun ClassroomTimerView(
    state: TeacherToolsUiState,
    viewModel: TeacherToolsViewModel
) {
    val totalSec = state.timerTotalSeconds
    val remSec = state.timerRemainingSeconds
    val progress = if (totalSec > 0) remSec.toFloat() / totalSec.toFloat() else 0f

    val minutes = remSec / 60
    val seconds = remSec % 60

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "مؤقت الحصة والاختبارات القصيرة ⏱️",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "ضبط الوقت لحل المسائل، الكويزات، أو وقت الاستراحة",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Timer Display
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.isTimerFinished) AmberGoldContainer else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = String.format(Locale.ENGLISH, "%02d:%02d", minutes, seconds),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 64.sp
                        ),
                        color = if (state.isTimerFinished) AmberGold else if (remSec < 60 && remSec > 0) Color.Red else MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = if (remSec < 60) Color.Red else NavyPrimary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Controls Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (state.isTimerRunning) viewModel.pauseTimer() else viewModel.startTimer()
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.isTimerRunning) AmberGold else NavyPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Icon(if (state.isTimerRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (state.isTimerRunning) "إيقاف مؤقت" else "بدء المؤقت", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.resetTimer() },
                            shape = CircleShape
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إعادة ضبط")
                        }
                    }
                }
            }
        }

        // Quick Preset Durations
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "أوقات سريعة جاهزة:",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                val presets = listOf(
                    "5 دقائق" to 300,
                    "10 دقائق" to 600,
                    "15 دقيقة" to 900,
                    "30 دقيقة" to 1800,
                    "45 دقيقة" to 2700,
                    "60 دقيقة" to 3600
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presets) { (title, sec) ->
                        FilterChip(
                            selected = state.timerTotalSeconds == sec && !state.isTimerRunning,
                            onClick = { viewModel.setTimerDuration(sec) },
                            label = { Text(title) }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. QUICK GRADE CALCULATOR VIEW
// ==========================================
@Composable
private fun QuickGradeCalculatorView(
    state: TeacherToolsUiState,
    viewModel: TeacherToolsViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "حاسبة الدرجات والنسب المئوية 🧮",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "تحويل درجة الطالب إلى نسبة مئوية وتقدير فوري أثناء التصحيح",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Inputs Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = state.calcRawScore,
                            onValueChange = { viewModel.setCalcRawScore(it) },
                            label = { Text("درجة الطالب") },
                            placeholder = { Text("مثال: 45") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = state.calcMaxScore,
                            onValueChange = { viewModel.setCalcMaxScore(it) },
                            label = { Text("الدرجة العظمى") },
                            placeholder = { Text("مثال: 50") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Common Max Presets
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val maxPresets = listOf("10", "20", "30", "40", "50", "60", "80", "100")
                        items(maxPresets) { p ->
                            SuggestionChip(
                                onClick = { viewModel.setCalcMaxScore(p) },
                                label = { Text("من $p") }
                            )
                        }
                    }
                }
            }
        }

        // Result Card
        if (state.calcPercentage != null && state.calcRating != null) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.calcPercentage >= 85.0) EmeraldSuccessContainer else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = String.format(Locale.ENGLISH, "%.1f%%", state.calcPercentage),
                            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (state.calcPercentage >= 85.0) EmeraldSuccess else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.calcRating,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. BOOKLET & MATERIALS DELIVERY TRACKER VIEW
// ==========================================
@Composable
private fun BookletTrackerView(
    state: TeacherToolsUiState,
    viewModel: TeacherToolsViewModel,
    context: Context
) {
    val targetStudents = state.students.filter { it.status == "active" && (state.materialSelectedGroupId == 0L || it.groupId == state.materialSelectedGroupId) }
    val deliveredCount = targetStudents.count { std ->
        state.deliveries.any { it.studentId == std.id && it.materialName == state.materialTitle && it.isDelivered }
    }
    val paidCount = targetStudents.count { std ->
        state.deliveries.any { it.studentId == std.id && it.materialName == state.materialTitle && it.isPaid }
    }
    val unitPrice = state.materialPrice.toDoubleOrNull() ?: 0.0
    val totalCollected = paidCount * unitPrice

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "سجل تسليم واستلام المذكرات والكتب",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "متابعة من استلم المذكرة ومن سدد قيمتها بكل دقة",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Material Config Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = state.materialTitle,
                            onValueChange = { viewModel.setMaterialTitle(it) },
                            label = { Text("اسم المذكرة / الشيت") },
                            singleLine = true,
                            modifier = Modifier.weight(1.5f)
                        )
                        OutlinedTextField(
                            value = state.materialPrice,
                            onValueChange = { viewModel.setMaterialPrice(it) },
                            label = { Text("السعر (ج.م)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Group Selector
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = state.materialSelectedGroupId == 0L,
                                onClick = { viewModel.setMaterialGroup(0L) },
                                label = { Text("الكل") }
                            )
                        }
                        items(state.groups) { g ->
                            FilterChip(
                                selected = state.materialSelectedGroupId == g.id,
                                onClick = { viewModel.setMaterialGroup(g.id) },
                                label = { Text(g.name) }
                            )
                        }
                    }

                    // Stats summary strip
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("تم الاستلام", style = MaterialTheme.typography.labelSmall)
                                Text("$deliveredCount / ${targetStudents.size}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("تم السداد", style = MaterialTheme.typography.labelSmall)
                                Text("$paidCount / ${targetStudents.size}", fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("إجمالي المحصل", style = MaterialTheme.typography.labelSmall)
                                Text("$totalCollected ج.م", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                }
            }
        }

        // Students List
        items(targetStudents, key = { it.id }) { student ->
            val delivery = state.deliveries.find { it.studentId == student.id && it.materialName == state.materialTitle }
            val isDelivered = delivery?.isDelivered ?: false
            val isPaid = delivery?.isPaid ?: false

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(student.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Text(
                            text = if (student.phone.isNotEmpty()) student.phone else "كود #${student.id}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Received Checkbox
                        FilterChip(
                            selected = isDelivered,
                            onClick = { viewModel.toggleDelivery(student.id, state.materialTitle) },
                            label = { Text(if (isDelivered) "مستلم ✓" else "لم يستلم") },
                            leadingIcon = {
                                Icon(
                                    if (isDelivered) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                                    contentDescription = null,
                                    tint = if (isDelivered) MaterialTheme.colorScheme.primary else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )

                        // Paid Checkbox
                        FilterChip(
                            selected = isPaid,
                            onClick = { viewModel.togglePayment(student.id, state.materialTitle) },
                            label = { Text(if (isPaid) "مدفوع ✓" else "غير مدفوع") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldSuccessContainer,
                                selectedLabelColor = EmeraldSuccess
                            ),
                            leadingIcon = {
                                Icon(
                                    if (isPaid) Icons.Filled.Paid else Icons.Outlined.Paid,
                                    contentDescription = null,
                                    tint = if (isPaid) EmeraldSuccess else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 7. QUICK PRINT HUB VIEW
// ==========================================
@Composable
private fun QuickPrintHubView(
    state: TeacherToolsUiState,
    viewModel: TeacherToolsViewModel,
    context: Context,
    onNavigateToSchedule: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Print, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "مركز الطباعة السريع والتصدير PDF 🖨️",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "تصدير كروت الطلاب، لوحة الشرف، كشوفات الحضور بضغطة واحدة",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Student ID Cards PDF
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(NavyPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Badge, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("🪪 طباعة كروت وكارنيهات الطلاب الذكية (QR / Barcode)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("كروت عضوية مزودة بباركود لكل طالب لتسجيل الحضور السريع", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.printStudentIdCards(context, 0L) },
                        enabled = !state.isExportingPdf,
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (state.isExportingPdf) "جاري إعداد الكارنيهات..." else "طباعة ومشاركة كارنيهات كافة الطلاب PDF")
                    }
                }
            }
        }

        // Honor Roll Poster PDF
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AmberGoldContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = AmberGold, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("👑 بوستر لوحة شرف أوائل الطلاب (Honor Roll)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("بوستر تكريمي ملون قابل للطباعة يبرز أفضل الطلاب أداءً والتزاماً", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.printHonorRoll(context, 0L) },
                        enabled = !state.isExportingPdf,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (state.isExportingPdf) "جاري إعداد البوستر..." else "توليد ومشاركة بوستر لوحة الشرف PDF")
                    }
                }
            }
        }

        // Weekly Schedule PDF
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(TealAccentContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = TealAccent, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("📅 جدول الحصص والمواعيد الأسبوعي", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("جدول تفصيلي مطبوع بالمواعيد والمقرات وأرقام الهواتف", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    FilledTonalButton(
                        onClick = onNavigateToSchedule,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("الانتقال لصفحة الجدول والطباعة")
                    }
                }
            }
        }
    }
}

// ==========================================
// 8. MOTIVATION & WHATSAPP TEMPLATES VIEW
// ==========================================
@Composable
private fun MotivationTemplatesView(
    state: TeacherToolsUiState,
    context: Context
) {
    val teacherName = state.teacher.name.ifEmpty { "عبده أيمن" }
    val teacherPhone = state.teacher.phone.ifEmpty { "01206150946" }

    val templates = listOf(
        Triple(
            "🌟 رسالة ترحيبية وتشجيعية بداية العام / الفصل",
            "السلام عليكم ورحمة الله وبركاته،\nأهلاً وسهلاً بكم أعزائي الطلاب وأولياء الأمور الكرام 🌸\nيسعدنا انطلاق الفصل الدراسي الجديد مع أستاذ المادة: *$teacherName*.\nنعدكم بعام مليء بالتميز والشرح المبسط والمتابعة المستمرة بإذن الله.\n📞 للتواصل والاستفسار: $teacherPhone\nنسأل الله التوفيق والنجاح الباهر لجميع أبنائنا!",
            EmeraldSuccess
        ),
        Triple(
            "🔥 تحفيز وتشجيع ناري قبل الامتحان",
            "أبطالنا المتميزين 🌟\nتذكروا دائماً: *من زرع حصد، والنجاح لا يأتي بالصدفة بل بالإصرار والعمل* 💪\nراجعوا بهدوء، ثقوا في أنفسكم وفي مجهودكم، والمستقبل المشرق بانتظاركم.\nمعلمكم وفخور بكم دائماً: *$teacherName*",
            NavyPrimary
        ),
        Triple(
            "👑 تهنئة الطلاب المتفوقين في الامتحان",
            "ألف مبروك لأبطالنا المتفوقين في الاختبار الأخير! 🥇🥈🥉\nفخور جداً بمستواكم والتزامكم الرائع، ونتمنى لكم دوام الصدارة والتميز الدائم.\nأستاذ المادة: *$teacherName*",
            AmberGold
        ),
        Triple(
            "📚 تنبيه حل الواجبات والمذاكرة الدورية",
            "السلام عليكم ورحمة الله،\nتذكير لأبنائنا الطلاب الكرام بضرورة إنهاء الواجبات وتسليمها في موعد الحصة القادمة، والمذاكرة أولاً بأول للحفاظ على المستوى المتميز.\nبالتوفيق دوماً! ✍️",
            TealAccent
        )
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Send, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "قوالب ورسائل المعلم الجاهزة 💬",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "رسائل ترحيب وتشجيع وتنبيهات جاهزة للنسخ والمشاركة عبر واتساب",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        items(templates) { (title, body, color) ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = color)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = body,
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Template", body)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ نص الرسالة للحافظة 📋", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("نسخ النص")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { WhatsAppHelper.sendMessage(context, "", body) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مشاركة عبر واتساب")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 9. TEACHER PORTFOLIO, PROFILE & BUSINESS CARDS VIEW
// ==========================================
@Composable
private fun TeacherPortfolioAndCardsView(
    state: TeacherToolsUiState,
    context: Context
) {
    var showShareModal by remember { mutableStateOf(false) }

    if (showShareModal) {
        com.example.ui.components.TeacherProfileShareDialog(
            teacher = state.teacher,
            onDismiss = { showShareModal = false }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F172A),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Brush.horizontalGradient(listOf(Color(0xFFD97706), Color(0xFFFDE68A))), RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFD97706),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Badge, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        "بطاقة وبورتفوليو المعلم التعريفي الفاخر",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "مشاركة بروفايلك الأكاديمي مع أولياء الأمور والزملاء في ورقة A4 منسقة وشيت كروت شخصية قابلة للطباعة",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showShareModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("open_teacher_share_dialog_btn")
                    ) {
                        Icon(Icons.Filled.QrCode, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("فتح وتصدير البورتفوليو والـ QR للطباعة والمشاركة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "✨ مميزات بطاقة وبورتفوليو المعلم:",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text("• ورقة A4 بتصميم ملكي فاخر باللون الكحلي والذهبي مع إطار مزخرف وشعار السنتر.")
                    Text("• رمز QR عالي الدقة يوجه ولي الأمر مباشرة لمحادثة الواتساب أو حفظ بطاقة الاتصال فوراً.")
                    Text("• كشف كامل بالمراحل والمناهج الدراسية، أسلوب التدريس الحديث، ونظام متابعة الواجبات والامتحانات.")
                    Text("• إمكانية طباعة شيت كامل يحتوي على 8 كروت عمل شخصية في صفحة واحدة للقص والتوزيع على الطلاب.")
                }
            }
        }
    }
}

// ==========================================
// 10. SMART EDUCATIONAL TRANSLATOR VIEW (مترجم المعلم الذكي)
// ==========================================
@Composable
private fun SmartEducationalTranslatorView(
    state: TeacherToolsUiState,
    viewModel: TeacherToolsViewModel,
    context: Context
) {
    val clipboardManager = remember {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    val subjects = remember {
        listOf("الكل", "الرياضيات", "الفيزياء", "الكيمياء", "الأحياء", "تقييمات المعلم", "اللغة العربية", "قواعد اللغات")
    }

    val filteredGlossary = remember(state.translatorSelectedSubjectFilter) {
        if (state.translatorSelectedSubjectFilter == "الكل") {
            com.example.util.SmartTranslatorHelper.educationalGlossary
        } else {
            com.example.util.SmartTranslatorHelper.educationalGlossary.filter {
                it.subject == state.translatorSelectedSubjectFilter
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("smart_translator_view"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Translation Control Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header with language selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Source Language Chip
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val next = when (state.translatorSourceLang) {
                                        com.example.util.AppLanguage.ARABIC -> com.example.util.AppLanguage.ENGLISH
                                        com.example.util.AppLanguage.ENGLISH -> com.example.util.AppLanguage.FRENCH
                                        com.example.util.AppLanguage.FRENCH -> com.example.util.AppLanguage.ARABIC
                                    }
                                    viewModel.setTranslatorLanguages(next, state.translatorTargetLang)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(state.translatorSourceLang.flag, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    state.translatorSourceLang.displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // Swap Button
                        IconButton(
                            onClick = { viewModel.swapTranslatorLanguages() },
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .testTag("swap_translator_langs_btn")
                        ) {
                            Icon(
                                Icons.Filled.SwapHoriz,
                                contentDescription = "تبديل اللغات",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Target Language Chip
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val next = when (state.translatorTargetLang) {
                                        com.example.util.AppLanguage.ENGLISH -> com.example.util.AppLanguage.ARABIC
                                        com.example.util.AppLanguage.ARABIC -> com.example.util.AppLanguage.FRENCH
                                        com.example.util.AppLanguage.FRENCH -> com.example.util.AppLanguage.ENGLISH
                                    }
                                    viewModel.setTranslatorLanguages(state.translatorSourceLang, next)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(state.translatorTargetLang.flag, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    state.translatorTargetLang.displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    // Input Text Area
                    OutlinedTextField(
                        value = state.translatorInputText,
                        onValueChange = { viewModel.setTranslatorInput(it) },
                        label = { Text("النص أو المصطلح المراد ترجمته") },
                        placeholder = { Text("اكتب مصطلحاً، سؤالاً، أو توجيهاً تعليمياً...") },
                        minLines = 3,
                        maxLines = 6,
                        trailingIcon = {
                            if (state.translatorInputText.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setTranslatorInput("") }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "مسح")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("translator_input_field")
                    )

                    // Translated Result Box
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "الترجمة المعتمدة (${state.translatorTargetLang.displayName}):",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Row {
                                    // Copy Button
                                    IconButton(
                                        onClick = {
                                            if (state.translatorTranslatedText.isNotBlank()) {
                                                val clip = ClipData.newPlainText("Translation", state.translatorTranslatedText)
                                                clipboardManager.setPrimaryClip(clip)
                                                Toast.makeText(context, "تم نسخ الترجمة بنجاح 📋", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.ContentCopy,
                                            contentDescription = "نسخ",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // Share Button
                                    IconButton(
                                        onClick = {
                                            if (state.translatorTranslatedText.isNotBlank()) {
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(
                                                        Intent.EXTRA_TEXT,
                                                        "📚 ترجمة تعليمية من ${state.teacher.name.ifEmpty { "المعلم" }}:\n\n${state.translatorInputText}\n⬇️\n${state.translatorTranslatedText}"
                                                    )
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "مشاركة الترجمة"))
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Share,
                                            contentDescription = "مشاركة",
                                            tint = EmeraldSuccess,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = if (state.translatorTranslatedText.isNotBlank()) state.translatorTranslatedText else "ستظهر الترجمة الأكاديمية الدقيقة هنا فورياً...",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (state.translatorTranslatedText.isNotBlank()) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (state.translatorTranslatedText.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        // Subject Filter Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "📖 القاموس والمصطلحات الأكاديمية المصنفة:",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(subjects) { subj ->
                        val isSelected = state.translatorSelectedSubjectFilter == subj
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setTranslatorSubjectFilter(subj) },
                            label = { Text(subj, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }
        }

        // Glossary Cards List
        items(filteredGlossary) { item ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val input = when (state.translatorSourceLang) {
                            com.example.util.AppLanguage.ARABIC -> item.arabic
                            com.example.util.AppLanguage.ENGLISH -> item.english
                            com.example.util.AppLanguage.FRENCH -> item.french
                        }
                        viewModel.setTranslatorInput(input)
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Text(
                                    item.subject,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                item.arabic,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "🇬🇧 ${item.english}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "🇫🇷 ${item.french}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            val clip = ClipData.newPlainText("Term", "${item.arabic} = ${item.english} = ${item.french}")
                            clipboardManager.setPrimaryClip(clip)
                            Toast.makeText(context, "تم نسخ المصطلح بكل اللغات 📋", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            contentDescription = "نسخ المصطلح",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ==========================================
// 11. CASIO FX SCIENTIFIC CALCULATOR VIEW (Full Screen)
// ==========================================
@Composable
private fun CasioCalculatorToolView() {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("teacher_tools_casio_calculator"),
        color = Color(0xFF1B2228),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "CASIO",
                        fontWeight = FontWeight.Black,
                        fontSize = 19.sp,
                        color = Color(0xFFF1F5F9),
                        letterSpacing = 2.5.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF334155)
                    ) {
                        Text(
                            text = "fx-991ES PLUS",
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Solar Panel Simulation (TWO WAY POWER)
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = Color(0xFF2C1810),
                        border = BorderStroke(1.dp, Color(0xFF4A3525)),
                        modifier = Modifier.size(width = 44.dp, height = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(4) {
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .fillMaxHeight(0.7f)
                                        .background(Color(0xFF6B4C35))
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF1E3A8A).copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, Color(0xFF3B82F6))
                    ) {
                        Text(
                            text = "NATURAL-V.P.A.M.",
                            color = Color(0xFF93C5FD),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            CasioCalculatorContent()
        }
    }
}
