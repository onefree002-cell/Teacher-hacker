package com.example.ui.screens.attendance

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.export.HomeworkPdfExporter
import com.example.data.local.entity.StudentEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.L
import com.example.util.LocaleManager
import com.example.util.WhatsAppHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    viewModel: AttendanceViewModel,
    onNavigateBack: (() -> Unit)? = null,
    onNavigateHome: (() -> Unit)? = null,
    onNavigateToGroup: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // 0 = Attendance (تسجيل الحضور والغياب), 1 = Homework Scanner (تصوير وتوثيق الواجب), 2 = Extra Tools (أدوات إضافية)
    var selectedMainTab by remember { mutableIntStateOf(0) }

    var groupDropdownExpanded by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterTab by remember { mutableIntStateOf(0) } // 0=All, 1=Present, 2=Absent, 3=Homework incomplete
    var showAbsentBroadcastDialog by remember { mutableStateOf(false) }
    var showRandomPicker by remember { mutableStateOf(false) }
    var showSessionTimer by remember { mutableStateOf(false) }
    var showCasioCalculator by remember { mutableStateOf(false) }

    // State for Homework Scanner Tab
    var selectedHomeworkStudentId by remember { mutableStateOf<Long?>(null) }
    var studentDropdownExpanded by remember { mutableStateOf(false) }
    var homeworkStatusEvaluation by remember { mutableStateOf("completed") }
    var homeworkScore by remember { mutableStateOf("") }
    var homeworkNotes by remember { mutableStateOf("") }
    var homeworkTopic by remember { mutableStateOf("") }
    val capturedHomeworkBitmaps = remember { mutableStateListOf<Bitmap>() }
    var showCamScannerDialog by remember { mutableStateOf(false) }
    var generatedPdfFile by remember { mutableStateOf<File?>(null) }
    var showPdfSuccessDialog by remember { mutableStateOf(false) }
    var isGeneratingPdf by remember { mutableStateOf(false) }

    // Image Pickers
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedHomeworkBitmaps.add(bitmap)
            Toast.makeText(context, "تم التقاط صورة الواجب بنجاح 📷", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch(Dispatchers.IO) {
                uris.forEach { uri ->
                    try {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val bmp = BitmapFactory.decodeStream(stream)
                            if (bmp != null) {
                                withContext(Dispatchers.Main) {
                                    capturedHomeworkBitmaps.add(bmp)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "تمت إضافة ${uris.size} صورة من المعرض 🖼️", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val currentGroup = state.groups.firstOrNull { it.id == state.selectedGroupId }

    val filteredList = remember(state.studentsAttendanceList, searchQuery, selectedFilterTab) {
        state.studentsAttendanceList.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                    item.student.name.contains(searchQuery.trim(), ignoreCase = true) ||
                    item.student.phone.contains(searchQuery.trim()) ||
                    item.student.parentPhone.contains(searchQuery.trim()) ||
                    item.student.barcodeCode.contains(searchQuery.trim(), ignoreCase = true)

            val matchesFilter = when (selectedFilterTab) {
                1 -> item.status == "present"
                2 -> item.status == "absent"
                3 -> item.homeworkStatus == "not_done" || item.homeworkStatus == "partial"
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = when (selectedMainTab) {
                    0 -> "تسجيل الحضور والغياب"
                    1 -> "تصوير وتوثيق واجبات الطلاب"
                    else -> "أدوات الحضور الإضافية"
                },
                subtitle = if (currentGroup != null) "مجموعة: ${currentGroup.name} (${currentGroup.grade})" else "منظومة الحضور والواجب",
                onNavigateBack = onNavigateBack,
                onNavigateHome = onNavigateHome,
                actions = {
                    IconButton(
                        onClick = { showQrScanner = true },
                        modifier = Modifier.testTag("attendance_top_qr_scanner_btn")
                    ) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "مسح باركود / QR", tint = NavyPrimary)
                    }
                }
            )
        },
        bottomBar = {
            if (selectedMainTab == 0 && state.studentsAttendanceList.isNotEmpty()) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "إجمالي: ${state.studentsAttendanceList.size} طالب",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "حاضر: ${state.presentCount} | غائب: ${state.absentCount}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Button(
                            onClick = {
                                viewModel.saveAttendance(context) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("تم حفظ سجل الحضور والغياب وملخص الحصة في مجلد DOCUMENTS/TEACHER HACKER 💾")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("save_attendance_btn")
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("حفظ الحضور والتقرير", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main 3-Tab Segmented Selector (حضور وغياب | تصوير الواجب | أدوات إضافية)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                TabRow(
                    selectedTabIndex = selectedMainTab,
                    containerColor = Color.Transparent,
                    indicator = {},
                    divider = {}
                ) {
                    // Tab 0: Attendance
                    Tab(
                        selected = selectedMainTab == 0,
                        onClick = { selectedMainTab = 0 },
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedMainTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .testTag("tab_attendance")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = if (selectedMainTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "الحضور والغياب",
                                fontWeight = if (selectedMainTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedMainTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Tab 1: Homework Scanner
                    Tab(
                        selected = selectedMainTab == 1,
                        onClick = { selectedMainTab = 1 },
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedMainTab == 1) OrangeAccent else Color.Transparent)
                            .testTag("tab_homework_scanner")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Filled.DocumentScanner,
                                contentDescription = null,
                                tint = if (selectedMainTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تصوير الواجب",
                                fontWeight = if (selectedMainTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedMainTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Tab 2: Extra Tools
                    Tab(
                        selected = selectedMainTab == 2,
                        onClick = { selectedMainTab = 2 },
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedMainTab == 2) PurpleAccent else Color.Transparent)
                            .testTag("tab_extra_tools")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Filled.BuildCircle,
                                contentDescription = null,
                                tint = if (selectedMainTab == 2) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "أدوات إضافية",
                                fontWeight = if (selectedMainTab == 2) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedMainTab == 2) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Senior Usability & Quick Hint banner
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.TipsAndUpdates,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (selectedMainTab) {
                            0 -> "تحضير الطلاب بنقرة واحدة وتنبيه أولياء الأمور عبر الواتساب فوراً."
                            1 -> "التقط صورة لكشكول الواجب واستخرج ملف PDF باسم الطالب وحصته."
                            else -> "أدوات مساعدة: مسح الباركود، القرعة، المؤقت، والرسائل الجماعية."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // TAB CONTENTS
            when (selectedMainTab) {
                // ==========================================
                // TAB 0: ATTENDANCE & ABSENCE (الحضور والغياب)
                // ==========================================
                0 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Group & Date Picker Card
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    // Group Selector Row with Go To Group Action
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            ExposedDropdownMenuBox(
                                                expanded = groupDropdownExpanded,
                                                onExpandedChange = { groupDropdownExpanded = it }
                                            ) {
                                                OutlinedTextField(
                                                    value = if (currentGroup != null) "${currentGroup.name} (${L.localizedGrade(currentGroup.grade)})" else "اختر المجموعة الدراسية",
                                                    onValueChange = {},
                                                    readOnly = true,
                                                    label = { Text("المجموعة الدراسية", fontWeight = FontWeight.Bold) },
                                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupDropdownExpanded) },
                                                    modifier = Modifier
                                                        .menuAnchor()
                                                        .fillMaxWidth()
                                                        .testTag("attendance_group_picker")
                                                )
                                                ExposedDropdownMenu(
                                                    expanded = groupDropdownExpanded,
                                                    onDismissRequest = { groupDropdownExpanded = false }
                                                ) {
                                                    state.groups.forEach { g ->
                                                        DropdownMenuItem(
                                                            text = { Text("${g.name} (${L.localizedGrade(g.grade)})") },
                                                            onClick = {
                                                                viewModel.onGroupSelected(g.id)
                                                                groupDropdownExpanded = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        if (currentGroup != null && onNavigateToGroup != null) {
                                            FilledTonalButton(
                                                onClick = { onNavigateToGroup(currentGroup.id) },
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.height(52.dp)
                                            ) {
                                                Icon(Icons.Filled.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("المجموعة", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }

                                    // Session Folder & Documents Path Shortcut Banner
                                    if (currentGroup != null) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                com.example.util.TeacherHackerDirectoryManager.openSessionFolder(context, currentGroup.name, state.selectedDate)
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                    Icon(Icons.Filled.FolderSpecial, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "مجلد الحصة: DOCUMENTS / TEACHER HACKER / ${state.selectedDate} - ${currentGroup.name}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Text("فتح ➔", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    // Date Picker Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = state.selectedDate,
                                            onValueChange = { viewModel.onDateSelected(it) },
                                            label = { Text("تاريخ الحصة") },
                                            singleLine = true,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("attendance_date_input")
                                        )
                                        Button(
                                            onClick = {
                                                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                                viewModel.onDateSelected(today)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.height(52.dp)
                                        ) {
                                            Icon(Icons.Filled.Today, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("اليوم")
                                        }
                                    }
                                }
                            }
                        }

                        // Stats & Bulk Actions Card
                        if (state.studentsAttendanceList.isNotEmpty()) {
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        // Visual Stats
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(EmeraldSuccessContainer)
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("حاضر", style = MaterialTheme.typography.labelSmall, color = EmeraldSuccess)
                                                    Text("${state.presentCount}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = EmeraldSuccess)
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(CrimsonErrorContainer)
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("غائب", style = MaterialTheme.typography.labelSmall, color = CrimsonError)
                                                    Text("${state.absentCount}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = CrimsonError)
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(AmberGoldContainer)
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("متأخر", style = MaterialTheme.typography.labelSmall, color = AmberGold)
                                                    Text("${state.lateCount}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = AmberGold)
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(NavyPrimaryContainer)
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("بعذر", style = MaterialTheme.typography.labelSmall, color = NavyPrimary)
                                                    Text("${state.excusedCount}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = NavyPrimary)
                                                }
                                            }
                                        }

                                        // Fast Bulk Buttons
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            FilledTonalButton(
                                                onClick = { viewModel.markAllAs("present") },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(44.dp)
                                                    .testTag("mark_all_present_btn"),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("حاضر الكل ✅", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                            OutlinedButton(
                                                onClick = { viewModel.markAllAs("absent") },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(44.dp)
                                                    .testTag("mark_all_absent_btn"),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("غائب الكل ❌", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            // Search and Filter Bar
                            item {
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = searchQuery,
                                            onValueChange = { searchQuery = it },
                                            placeholder = { Text("بحث عن طالب بالاسم أو الهاتف...") },
                                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                                            trailingIcon = {
                                                if (searchQuery.isNotEmpty()) {
                                                    IconButton(onClick = { searchQuery = "" }) {
                                                        Icon(Icons.Filled.Clear, contentDescription = "مسح")
                                                    }
                                                }
                                            },
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("attendance_student_search")
                                        )

                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            item {
                                                FilterChip(
                                                    selected = selectedFilterTab == 0,
                                                    onClick = { selectedFilterTab = 0 },
                                                    label = { Text("الكل (${state.studentsAttendanceList.size})") }
                                                )
                                            }
                                            item {
                                                FilterChip(
                                                    selected = selectedFilterTab == 1,
                                                    onClick = { selectedFilterTab = 1 },
                                                    label = { Text("الحاضرون (${state.presentCount})") },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = EmeraldSuccessContainer,
                                                        selectedLabelColor = EmeraldSuccess
                                                    )
                                                )
                                            }
                                            item {
                                                FilterChip(
                                                    selected = selectedFilterTab == 2,
                                                    onClick = { selectedFilterTab = 2 },
                                                    label = { Text("الغائبون (${state.absentCount})") },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = CrimsonErrorContainer,
                                                        selectedLabelColor = CrimsonError
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Students List
                        if (filteredList.isNotEmpty()) {
                            items(filteredList, key = { it.student.id }) { item ->
                                val student = item.student
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 2.dp)
                                        .testTag("attendance_student_${student.id}")
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        // Student Header & Contact
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(42.dp)
                                                        .clip(CircleShape)
                                                        .background(NavyPrimaryContainer),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        student.name.take(1),
                                                        fontWeight = FontWeight.Bold,
                                                        color = NavyPrimary,
                                                        fontSize = 18.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        student.name,
                                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                                    )
                                                    val phone = student.parentPhone.ifEmpty { student.phone }
                                                    if (phone.isNotEmpty()) {
                                                        Text(
                                                            "هاتف: $phone",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }

                                            // WhatsApp Notification Button
                                            val contactPhone = student.parentPhone.ifEmpty { student.phone }
                                            if (contactPhone.isNotEmpty()) {
                                                FilledTonalIconButton(
                                                    onClick = {
                                                        val statusArabic = when (item.status) {
                                                            "present" -> "حاضر ✅"
                                                            "absent" -> "غائب ❌"
                                                            "late" -> "متأخر ⏰"
                                                            "excused" -> "غائب بعذر ⚪"
                                                            else -> item.status
                                                        }
                                                        val msg = """
                                                            السلام عليكم ورحمة الله وبركاته،
                                                            تقرير متابعة الطالب: *${student.name}*
                                                            📅 التاريخ: ${state.selectedDate}
                                                            👥 المجموعة: ${currentGroup?.name ?: ""}
                                                            
                                                            📍 *حالة الحضور:* $statusArabic
                                                            ${if (item.note.isNotEmpty()) "\n📝 *ملاحظات:* " + item.note else ""}
                                                            
                                                            شاكرين دوام المتابعة والحرص.
                                                        """.trimIndent()
                                                        WhatsAppHelper.openWhatsApp(context, contactPhone, msg)
                                                    },
                                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                        containerColor = EmeraldSuccessContainer,
                                                        contentColor = EmeraldSuccess
                                                    ),
                                                    modifier = Modifier.size(38.dp)
                                                ) {
                                                    Icon(Icons.Filled.Chat, contentDescription = "إرسال تقرير بالواتساب", modifier = Modifier.size(20.dp))
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Senior-Friendly Large Status Buttons (حاضر / غائب / متأخر / بعذر)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            // Present
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (item.status == "present") EmeraldSuccess else EmeraldSuccessContainer.copy(alpha = 0.4f),
                                                border = BorderStroke(1.dp, if (item.status == "present") EmeraldSuccess else Color.Transparent),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(44.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable { viewModel.updateStudentStatus(student.id, "present") }
                                                    .testTag("status_present_${student.id}")
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        "حاضر ✅",
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (item.status == "present") Color.White else EmeraldSuccess,
                                                        fontSize = 13.sp
                                                    )
                                                }
                                            }

                                            // Absent
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (item.status == "absent") CrimsonError else CrimsonErrorContainer.copy(alpha = 0.4f),
                                                border = BorderStroke(1.dp, if (item.status == "absent") CrimsonError else Color.Transparent),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(44.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable { viewModel.updateStudentStatus(student.id, "absent") }
                                                    .testTag("status_absent_${student.id}")
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        "غائب ❌",
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (item.status == "absent") Color.White else CrimsonError,
                                                        fontSize = 13.sp
                                                    )
                                                }
                                            }

                                            // Late
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (item.status == "late") AmberGold else AmberGoldContainer.copy(alpha = 0.4f),
                                                border = BorderStroke(1.dp, if (item.status == "late") AmberGold else Color.Transparent),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(44.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable { viewModel.updateStudentStatus(student.id, "late") }
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        "متأخر ⏰",
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (item.status == "late") Color.White else AmberGoldDark,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }

                                             // Excused
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (item.status == "excused") NavyPrimary else NavyPrimaryContainer.copy(alpha = 0.4f),
                                                border = BorderStroke(1.dp, if (item.status == "excused") NavyPrimary else Color.Transparent),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(44.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable { viewModel.updateStudentStatus(student.id, "excused") }
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        "بعذر ⚪",
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (item.status == "excused") Color.White else NavyPrimary,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                        }

                                        // Integrated Homework Status & Quick Camera Row (تسجيل الواجب وتصويره)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = "الواجب:",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )

                                                    // Completed Homework Chip
                                                    FilterChip(
                                                        selected = item.homeworkStatus == "completed",
                                                        onClick = { viewModel.updateStudentHomeworkStatus(student.id, "completed") },
                                                        label = { Text("كامل 💯", fontSize = 11.sp) },
                                                        colors = FilterChipDefaults.filterChipColors(
                                                            selectedContainerColor = EmeraldSuccessContainer,
                                                            selectedLabelColor = EmeraldSuccess
                                                        ),
                                                        modifier = Modifier.height(32.dp)
                                                    )

                                                    // Partial Homework Chip
                                                    FilterChip(
                                                        selected = item.homeworkStatus == "partial",
                                                        onClick = { viewModel.updateStudentHomeworkStatus(student.id, "partial") },
                                                        label = { Text("ناقص ⚠️", fontSize = 11.sp) },
                                                        colors = FilterChipDefaults.filterChipColors(
                                                            selectedContainerColor = AmberGoldContainer,
                                                            selectedLabelColor = AmberGoldDark
                                                        ),
                                                        modifier = Modifier.height(32.dp)
                                                    )

                                                    // Not Done Homework Chip
                                                    FilterChip(
                                                        selected = item.homeworkStatus == "not_done",
                                                        onClick = { viewModel.updateStudentHomeworkStatus(student.id, "not_done") },
                                                        label = { Text("لم يُحل ❌", fontSize = 11.sp) },
                                                        colors = FilterChipDefaults.filterChipColors(
                                                            selectedContainerColor = CrimsonErrorContainer,
                                                            selectedLabelColor = CrimsonError
                                                        ),
                                                        modifier = Modifier.height(32.dp)
                                                    )
                                                }

                                                // Quick Camera / CamScanner Button for this student
                                                FilledTonalIconButton(
                                                    onClick = {
                                                        selectedHomeworkStudentId = student.id
                                                        selectedMainTab = 1
                                                        showCamScannerDialog = true
                                                    },
                                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                        containerColor = OrangeAccent.copy(alpha = 0.15f),
                                                        contentColor = OrangeAccent
                                                    ),
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .testTag("btn_camera_student_${student.id}")
                                                ) {
                                                    Icon(
                                                        Icons.Filled.CameraAlt,
                                                        contentDescription = "تصوير واجب ${student.name} بالماسح الذكي",
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Note Field for absent or late
                                        if (item.status != "present") {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            OutlinedTextField(
                                                value = item.note,
                                                onValueChange = { viewModel.updateStudentNote(student.id, it) },
                                                placeholder = { Text("سبب الغياب أو ملاحظة...") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (state.studentsAttendanceList.isEmpty()) {
                            item {
                                EmptyStateWidget(
                                    title = "لا يوجد طلاب في هذه المجموعة",
                                    description = "اختر مجموعة تحتوي على طلاب لتسجيل الحضور والواجب",
                                    icon = Icons.Filled.FactCheck
                                )
                            }
                        } else {
                            item {
                                EmptyStateWidget(
                                    title = "لا توجد نتائج مطابقة للبحث",
                                    description = "جرب البحث باسم آخر أو إزالة الفلتر",
                                    icon = Icons.Filled.SearchOff
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // TAB 1: HOMEWORK SCANNER & PDF (تصوير وتوثيق الواجب)
                // ==========================================
                1 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Explanatory Card
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = OrangeAccent.copy(alpha = 0.08f)),
                                border = BorderStroke(1.dp, OrangeAccent.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = OrangeAccent,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Filled.CameraEnhance, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "تصوير كشكول أو ورقة واجب الطالب 📸",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = OrangeAccent
                                        )
                                        Text(
                                            text = "يحفظ تلقائياً كملف PDF باسم: واجب - اسم الطالب - تاريخ الحصة.pdf",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Step 1: Select Student Card
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "1️⃣ اختر الطالب وتفاصيل الواجب:",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    val groupStudents = state.studentsAttendanceList.map { it.student }
                                    val currentSelectedStudent = groupStudents.firstOrNull { it.id == selectedHomeworkStudentId } ?: groupStudents.firstOrNull()

                                    // Student Dropdown Selector
                                    ExposedDropdownMenuBox(
                                        expanded = studentDropdownExpanded,
                                        onExpandedChange = { studentDropdownExpanded = it }
                                    ) {
                                        OutlinedTextField(
                                            value = currentSelectedStudent?.name ?: "اختر الطالب...",
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("الطالب المراد تصوير واجبه", fontWeight = FontWeight.Bold) },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = studentDropdownExpanded) },
                                            modifier = Modifier
                                                .menuAnchor()
                                                .fillMaxWidth()
                                                .testTag("hw_student_picker")
                                        )
                                        ExposedDropdownMenu(
                                            expanded = studentDropdownExpanded,
                                            onDismissRequest = { studentDropdownExpanded = false }
                                        ) {
                                            groupStudents.forEach { s ->
                                                DropdownMenuItem(
                                                    text = { Text(s.name) },
                                                    onClick = {
                                                        selectedHomeworkStudentId = s.id
                                                        studentDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // Topic Field
                                    OutlinedTextField(
                                        value = homeworkTopic,
                                        onValueChange = { homeworkTopic = it },
                                        label = { Text("موضوع أو عنوان الواجب (اختياري)") },
                                        placeholder = { Text("مثال: تدريبات الدرس الأول صـ 25") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        // Step 2: Camera & Gallery Capture Buttons
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "2️⃣ التقاط أو اختيار صور الواجب:",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    // CamScanner Smart Scanner Primary Action
                                    Button(
                                        onClick = {
                                            showCamScannerDialog = true
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .testTag("hw_cam_scanner_btn")
                                    ) {
                                        Icon(Icons.Filled.DocumentScanner, contentDescription = null, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("ماسح المستندات الذكي (CamScanner) 📄✨", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Standard Camera Button
                                        OutlinedButton(
                                            onClick = {
                                                cameraLauncher.launch(null)
                                            },
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(50.dp)
                                                .testTag("hw_camera_btn")
                                        ) {
                                            Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("كاميرا عادية 📷", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }

                                        // Gallery Button
                                        OutlinedButton(
                                            onClick = {
                                                galleryLauncher.launch("image/*")
                                            },
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(50.dp)
                                                .testTag("hw_gallery_btn")
                                        ) {
                                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("من المعرض 🖼️", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }

                                    // Display list of captured images
                                    if (capturedHomeworkBitmaps.isNotEmpty()) {
                                        Text(
                                            text = "الصور المرفقة (${capturedHomeworkBitmaps.size} صورة):",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            itemsIndexed(capturedHomeworkBitmaps) { index, bmp ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(110.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                                ) {
                                                    Image(
                                                        bitmap = bmp.asImageBitmap(),
                                                        contentDescription = "صورة واجب $index",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )

                                                    // Delete badge
                                                    IconButton(
                                                        onClick = {
                                                            capturedHomeworkBitmaps.removeAt(index)
                                                        },
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .size(28.dp)
                                                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                                    ) {
                                                        Icon(Icons.Filled.Close, contentDescription = "حذف", tint = Color.White, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "لم يتم التقاط صور بعد. اضغط على 'تصوير بالكاميرا' لفتح الكاميرا فوراً.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(12.dp),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Step 3: Evaluation & Notes
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "3️⃣ تقييم الواجب والملاحظات:",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    // Evaluation Status Selector
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        FilterChip(
                                            selected = homeworkStatusEvaluation == "completed",
                                            onClick = { homeworkStatusEvaluation = "completed" },
                                            label = { Text("كامل ممتاز 🌟") },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = EmeraldSuccessContainer,
                                                selectedLabelColor = EmeraldSuccess
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                        FilterChip(
                                            selected = homeworkStatusEvaluation == "partial",
                                            onClick = { homeworkStatusEvaluation = "partial" },
                                            label = { Text("ناقص ⚠️") },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = AmberGoldContainer,
                                                selectedLabelColor = AmberGold
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                        FilterChip(
                                            selected = homeworkStatusEvaluation == "not_done",
                                            onClick = { homeworkStatusEvaluation = "not_done" },
                                            label = { Text("لم يحل ❌") },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = CrimsonErrorContainer,
                                                selectedLabelColor = CrimsonError
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    // Grade Score and Notes
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = homeworkScore,
                                            onValueChange = { homeworkScore = it },
                                            label = { Text("الدرجة (مثلاً 10/10)") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    OutlinedTextField(
                                        value = homeworkNotes,
                                        onValueChange = { homeworkNotes = it },
                                        label = { Text("ملاحظات المعلم لولي الأمر") },
                                        placeholder = { Text("مثال: خط جميل وتركيز ممتاز، يرجى مراجعة المسألة 5") },
                                        singleLine = false,
                                        maxLines = 3,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        // Step 4: Generate & Export PDF Button
                        item {
                            val groupStudents = state.studentsAttendanceList.map { it.student }
                            val activeStudent = groupStudents.firstOrNull { it.id == selectedHomeworkStudentId } ?: groupStudents.firstOrNull()

                            Button(
                                onClick = {
                                    if (activeStudent == null) {
                                        Toast.makeText(context, "يرجى اختيار طالب أولاً", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    isGeneratingPdf = true
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val pdf = HomeworkPdfExporter.generateHomeworkPdf(
                                            context = context,
                                            studentName = activeStudent.name,
                                            sessionDate = state.selectedDate,
                                            groupName = currentGroup?.name ?: "",
                                            teacherName = state.teacher?.name ?: "",
                                            homeworkStatus = homeworkStatusEvaluation,
                                            score = homeworkScore,
                                            topic = homeworkTopic,
                                            notes = homeworkNotes,
                                            bitmaps = capturedHomeworkBitmaps.toList()
                                        )
                                        withContext(Dispatchers.Main) {
                                            isGeneratingPdf = false
                                            generatedPdfFile = pdf
                                            showPdfSuccessDialog = true
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("generate_hw_pdf_btn"),
                                enabled = !isGeneratingPdf && activeStudent != null
                            ) {
                                if (isGeneratingPdf) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("جارٍ إنشاء ملف PDF...", fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "حفظ واستخراج ملف PDF الواجب 📄",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // TAB 2: EXTRA TOOLS (الأدوات الإضافية للحضور)
                // ==========================================
                2 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Section Header
                        item {
                            Text(
                                text = "أدوات وميزات الحضور المتقدمة ⚙️",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Tool 1: Barcode / QR Scanner
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showQrScanner = true }
                                    .testTag("tool_qr_scanner_card")
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = NavyPrimaryContainer,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(26.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "ماسح الباركود وQR السريع",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "تحضير فوري للطلاب عبر مسح كارت الطالب أو الكارنيه بالكاميرا",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(Icons.Filled.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // Tool 2: WhatsApp Broadcast for Absent Parents
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showAbsentBroadcastDialog = true }
                                    .testTag("tool_absent_broadcast_card")
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = CrimsonErrorContainer,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = CrimsonError, modifier = Modifier.size(26.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "إنذار غياب جماعي بالواتساب",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "إرسال رسالة واتساب مخصصة لأولياء أمور جميع الطلاب الغائبين في الحصة بنقرة واحدة",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(Icons.Filled.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // Tool 3: Random Student Picker (القرعة العشوائية)
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showRandomPicker = true }
                                    .testTag("tool_random_picker_card")
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = AmberGoldContainer,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Filled.Casino, contentDescription = null, tint = AmberGoldDark, modifier = Modifier.size(26.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "القرعة العشوائية لاختيار طالب",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "اختيار طالب عشوائي من الحاضرين للمشاركة والتسميع والإجابة في الحصة",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(Icons.Filled.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // Tool 4: Session Timer (مؤقت الحصة)
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showSessionTimer = true }
                                    .testTag("tool_session_timer_card")
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = PurpleAccent.copy(alpha = 0.15f),
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Filled.Timer, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(26.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "مؤقت الحصة والأنشطة التفاعلية",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "ساعة إيقاف ومؤقت تنازلي لضبط وقت الأنشطة والامتحانات التنشيطية داخل الحصة",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(Icons.Filled.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // Tool 5: Casio FX Scientific Calculator (الآلة الحاسبة العلمية Casio fx)
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showCasioCalculator = true }
                                    .testTag("tool_casio_calc_card")
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF1E293B),
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Filled.Calculate, contentDescription = null, tint = AmberGold, modifier = Modifier.size(26.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = "الآلة الحاسبة العلمية Casio fx",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = AmberGold.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "fx-991ES",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AmberGoldDark,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "محاكي آلة كاسيو العلمية الشاملة مع شاشة Natural-V.P.A.M، الدوال والكسور والمعادلات الحسابية",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(Icons.Filled.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // PDF Export Success Dialog
    if (showPdfSuccessDialog && generatedPdfFile != null) {
        val pdf = generatedPdfFile!!
        val groupStudents = state.studentsAttendanceList.map { it.student }
        val activeStudent = groupStudents.firstOrNull { it.id == selectedHomeworkStudentId } ?: groupStudents.firstOrNull()

        AlertDialog(
            onDismissRequest = { showPdfSuccessDialog = false },
            icon = {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(44.dp))
            },
            title = {
                Text(
                    text = "تم إنشاء ملف PDF بنجاح! 📄",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("اسم الملف المحفوظ:")
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = pdf.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Text(
                        "تم حفظ الملف في مجلد مستندات التطبيق، ويمكنك الآن مشاركته مع ولي الأمر أو الطالب مباشرة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val contactPhone = activeStudent?.parentPhone?.ifEmpty { activeStudent.phone } ?: ""
                        HomeworkPdfExporter.sharePdf(context, pdf, activeStudent?.name ?: "", contactPhone)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مشاركة عبر الواتساب")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        HomeworkPdfExporter.openPdf(context, pdf)
                    }
                ) {
                    Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("فتح وعرض الملف")
                }
            }
        )
    }

    // Quick QR / Barcode Scanner Dialog
    if (showQrScanner) {
        val studentsInCurrentGroup = state.studentsAttendanceList.map { it.student }
        QuickQrScannerDialog(
            groupStudents = studentsInCurrentGroup,
            allStudents = state.allStudents,
            onStudentScanned = { scannedStudent ->
                viewModel.recordScannedStudent(scannedStudent.id)
            },
            onDismiss = { showQrScanner = false }
        )
    }

    // Absent Parents WhatsApp Broadcast Dialog
    if (showAbsentBroadcastDialog) {
        val absentStudents = state.studentsAttendanceList
            .filter { it.status == "absent" }
            .map { it.student }

        MultiParentMessagingDialog(
            students = absentStudents,
            teacherName = state.teacher?.name ?: "",
            groupName = currentGroup?.name ?: "",
            customTemplate = """
                السلام عليكم ورحمة الله وبركاته،
                تحية طيبة وبعد، نحيط سيادتكم علماً بأن الطالب/ة: *{student_name}* قد غاب/ت عن حصة اليوم (${state.selectedDate}) في مجموعة *${currentGroup?.name ?: ""}*.
                
                يرجى المتابعة والحرص على تعويض ما فاته بالتواصل معنا.
                شاكرين تعاونكم معنا.
                أستاذ: ${state.teacher?.name ?: ""}
            """.trimIndent(),
            onDismiss = { showAbsentBroadcastDialog = false }
        )
    }

    // Random Student Picker Dialog
    if (showRandomPicker) {
        var pickedStudent by remember { mutableStateOf<StudentEntity?>(null) }
        var isPicking by remember { mutableStateOf(false) }
        val eligibleStudents = remember(state.studentsAttendanceList) {
            state.studentsAttendanceList.filter { it.status == "present" }.map { it.student }
                .ifEmpty { state.studentsAttendanceList.map { it.student } }
        }

        AlertDialog(
            onDismissRequest = { showRandomPicker = false },
            icon = { Icon(Icons.Filled.Casino, contentDescription = null, tint = AmberGoldDark, modifier = Modifier.size(36.dp)) },
            title = { Text("القرعة العشوائية للطلاب 🎲", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (eligibleStudents.isEmpty()) {
                        Text("لا يوجد طلاب مسجلين في هذه المجموعة لإجراء القرعة.")
                    } else {
                        if (pickedStudent != null) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = EmeraldSuccessContainer,
                                border = BorderStroke(2.dp, EmeraldSuccess),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("🎉 الطالب المختار:", style = MaterialTheme.typography.labelMedium, color = EmeraldSuccess)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = pickedStudent!!.name,
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = EmeraldSuccess
                                    )
                                }
                            }
                        } else {
                            Text(
                                "اضغط على زر السحب لاختيار طالب عشوائياً من بين (${eligibleStudents.size}) طالب حاضرين.",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (eligibleStudents.isNotEmpty()) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isPicking = true
                                repeat(10) {
                                    pickedStudent = eligibleStudents.random()
                                    delay(80)
                                }
                                isPicking = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberGoldDark),
                        enabled = !isPicking
                    ) {
                        Text("🎲 سحب عشوائي جديد", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showRandomPicker = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // Session Timer Dialog
    if (showSessionTimer) {
        var timerSeconds by remember { mutableIntStateOf(300) } // Default 5 mins
        var isRunning by remember { mutableStateOf(false) }

        LaunchedEffect(isRunning) {
            while (isRunning && timerSeconds > 0) {
                delay(1000)
                timerSeconds--
            }
            if (timerSeconds == 0) {
                isRunning = false
            }
        }

        val minutes = timerSeconds / 60
        val seconds = timerSeconds % 60
        val formattedTime = String.format(Locale.US, "%02d:%02d", minutes, seconds)

        AlertDialog(
            onDismissRequest = { showSessionTimer = false },
            icon = { Icon(Icons.Filled.Timer, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(36.dp)) },
            title = { Text("مؤقت الحصة والأنشطة ⏱️", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (timerSeconds <= 60 && isRunning) CrimsonError else PurpleAccent
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { isRunning = !isRunning },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRunning) CrimsonError else EmeraldSuccess
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isRunning) "إيقاف مؤقت ⏸️" else "تشغيل ▶️", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                isRunning = false
                                timerSeconds = 300
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إعادة ضبط 🔄")
                        }
                    }

                    // Quick duration presets
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SuggestionChip(
                            onClick = { timerSeconds = 60; isRunning = false },
                            label = { Text("1 دقيقة") }
                        )
                        SuggestionChip(
                            onClick = { timerSeconds = 180; isRunning = false },
                            label = { Text("3 دقائق") }
                        )
                        SuggestionChip(
                            onClick = { timerSeconds = 300; isRunning = false },
                            label = { Text("5 دقائق") }
                        )
                        SuggestionChip(
                            onClick = { timerSeconds = 600; isRunning = false },
                            label = { Text("10 دقائق") }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSessionTimer = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // Casio FX Scientific Calculator Fullscreen Dialog
    if (showCasioCalculator) {
        CasioScientificCalculatorDialog(
            onDismiss = { showCasioCalculator = false }
        )
    }

    // CamScanner Interactive Homework Scanner Dialog
    if (showCamScannerDialog) {
        val studentName = state.studentsAttendanceList.find { it.student.id == selectedHomeworkStudentId }?.student?.name
        CamScannerDialog(
            studentName = studentName,
            onDismiss = { showCamScannerDialog = false },
            onPagesScanned = { scannedBitmaps: List<android.graphics.Bitmap> ->
                capturedHomeworkBitmaps.addAll(scannedBitmaps)
                showCamScannerDialog = false
                Toast.makeText(context, "تمت إضافة ${scannedBitmaps.size} صفحة مصححة من الواجب 📄✨", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
