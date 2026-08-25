package com.example.ui.screens.attendance

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.WhatsAppHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    viewModel: AttendanceViewModel,
    onNavigateBack: (() -> Unit)? = null,
    onNavigateHome: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var groupDropdownExpanded by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }
    var showHomeworkSection by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterTab by remember { mutableStateOf(0) } // 0=All, 1=Present, 2=Absent, 3=Homework incomplete
    var showAbsentBroadcastDialog by remember { mutableStateOf(false) }

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
                title = "تسجيل الحضور والغياب والواجب",
                onNavigateBack = onNavigateBack,
                onNavigateHome = onNavigateHome,
                actions = {
                    IconButton(
                        onClick = { showQrScanner = true },
                        modifier = Modifier.testTag("open_qr_scanner_btn")
                    ) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "مسح باركود / QR", tint = NavyPrimary)
                    }
                }
            )
        },
        bottomBar = {
            if (state.studentsAttendanceList.isNotEmpty()) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("إجمالي المسجلين: ${state.studentsAttendanceList.size}", style = MaterialTheme.typography.bodySmall)
                            Text("حاضر: ${state.presentCount} | غائب: ${state.absentCount}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        Button(
                            onClick = {
                                viewModel.saveAttendance {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("تم حفظ سجل الحضور والواجب بنجاح")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("save_attendance_btn")
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حفظ الحضور والواجب")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Group and Date Selector Row
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Group Selector Dropdown
                        ExposedDropdownMenuBox(
                            expanded = groupDropdownExpanded,
                            onExpandedChange = { groupDropdownExpanded = it }
                        ) {
                            val currentGroup = state.groups.firstOrNull { it.id == state.selectedGroupId }
                            OutlinedTextField(
                                value = currentGroup?.name ?: "اختر المجموعة الدراسية",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("المجموعة") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupDropdownExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth().testTag("attendance_group_picker")
                            )
                            ExposedDropdownMenu(
                                expanded = groupDropdownExpanded,
                                onDismissRequest = { groupDropdownExpanded = false }
                            ) {
                                state.groups.forEach { g ->
                                    DropdownMenuItem(
                                        text = { Text("${g.name} (${g.grade})") },
                                        onClick = {
                                            viewModel.onGroupSelected(g.id)
                                            groupDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Date selector input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = state.selectedDate,
                                onValueChange = { viewModel.onDateSelected(it) },
                                label = { Text("التاريخ (yyyy-MM-dd)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("attendance_date_input")
                            )
                            Button(
                                onClick = {
                                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                    viewModel.onDateSelected(today)
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("اليوم")
                            }
                        }
                    }
                }
            }

            // Session Homework & Topic Card (تحديد الواجب في كل حصة)
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showHomeworkSection = !showHomeworkSection },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.MenuBook, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تحديد واجب الحصة والدرس المطلوب",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                if (showHomeworkSection) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null
                            )
                        }

                        if (showHomeworkSection) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = state.sessionHomework,
                                onValueChange = { viewModel.onSessionHomeworkChanged(it) },
                                placeholder = { Text("مثال: حل صـ 42 من 1 إلى 15 + تسميع كلمات الوحدة الثانية") },
                                label = { Text("واجب هذه الحصة لجميع الطلاب") },
                                singleLine = false,
                                maxLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Stats Count & Bulk Actions Banner
            if (state.studentsAttendanceList.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Attendance Stats
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(EmeraldSuccessContainer)
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("حاضر: ${state.presentCount}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = EmeraldSuccess)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CrimsonErrorContainer)
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("غائب: ${state.absentCount}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = CrimsonError)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AmberGoldContainer)
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("متأخر: ${state.lateCount}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AmberGold)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NavyPrimaryContainer)
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("بعذر: ${state.excusedCount}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = NavyPrimary)
                            }
                        }

                        // Bulk Buttons & Absent Broadcast
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { viewModel.markAllAs("present") },
                                modifier = Modifier.weight(1f).testTag("mark_all_present_btn"),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("تحضير الكل", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = { viewModel.markAllHomeworkAs("completed") },
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("الواجب كامل", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = { viewModel.markAllAs("absent") },
                                modifier = Modifier.weight(0.9f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("تغييب الكل", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        if (state.absentCount > 0) {
                            FilledTonalButton(
                                onClick = { showAbsentBroadcastDialog = true },
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = CrimsonErrorContainer, contentColor = CrimsonError),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("broadcast_absent_parents_btn")
                            ) {
                                Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إرسال إنذار غياب لأولياء أمور الغائبين (${state.absentCount}) بالواتساب", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }

            // Quick Search & Filter Tabs
            if (state.studentsAttendanceList.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("بحث عن طالب بالاسم، الهاتف، أو الكود...") },
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Filled.Clear, contentDescription = "مسح")
                                        }
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("attendance_student_search")
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedFilterTab == 0,
                                        onClick = { selectedFilterTab = 0 },
                                        label = { Text("الكل (${state.studentsAttendanceList.size})", style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = selectedFilterTab == 1,
                                        onClick = { selectedFilterTab = 1 },
                                        label = { Text("الحاضرون (${state.presentCount})", style = MaterialTheme.typography.labelSmall) },
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
                                        label = { Text("الغائبون (${state.absentCount})", style = MaterialTheme.typography.labelSmall) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = CrimsonErrorContainer,
                                            selectedLabelColor = CrimsonError
                                        )
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = selectedFilterTab == 3,
                                        onClick = { selectedFilterTab = 3 },
                                        label = { Text("واجب غير مكتمل (${state.notDoneHomeworkCount + state.partialHomeworkCount})", style = MaterialTheme.typography.labelSmall) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = AmberGoldContainer,
                                            selectedLabelColor = AmberGold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Students Attendance List Items
            if (filteredList.isNotEmpty()) {
                items(filteredList, key = { it.student.id }) { item ->
                    val student = item.student
                    val group = state.groups.firstOrNull { it.id == state.selectedGroupId }
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                            .testTag("attendance_student_${student.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(NavyPrimaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(student.name.take(1), fontWeight = FontWeight.Bold, color = NavyPrimary)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(student.name, fontWeight = FontWeight.Bold)
                                        val phone = student.parentPhone.ifEmpty { student.phone }
                                        if (phone.isNotEmpty()) {
                                            Text(phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }

                                // WhatsApp Report Button
                                val contactPhone = student.parentPhone.ifEmpty { student.phone }
                                if (contactPhone.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            val statusArabic = when (item.status) {
                                                "present" -> "حاضر ✅"
                                                "absent" -> "غائب ❌"
                                                "late" -> "متأخر ⏰"
                                                "excused" -> "غائب بعذر ⚪"
                                                else -> item.status
                                            }
                                            val hwArabic = when (item.homeworkStatus) {
                                                "completed" -> "حل كامل وممتاز 🌟"
                                                "partial" -> "حل ناقص وغير مكتمل ⚠️"
                                                "not_done" -> "لم يقم بحل الواجب 🔴"
                                                "exempt" -> "معفى من الواجب ⚪"
                                                else -> "لم يتم التقييم"
                                            }
                                            val hwText = if (state.sessionHomework.isNotBlank()) "\n📖 *واجب الحصة:* ${state.sessionHomework}" else ""
                                            val msg = """
                                                السلام عليكم ورحمة الله وبركاته،
                                                تقرير متابعة الطالب: *${student.name}*
                                                📅 التاريخ: ${state.selectedDate}
                                                👥 المجموعة: ${group?.name ?: ""}
                                                
                                                📍 *حالة الحضور:* $statusArabic
                                                📚 *تقييم الواجب:* $hwArabic$hwText
                                                ${if (item.note.isNotEmpty()) "\n📝 *ملاحظات:* " + item.note else ""}
                                                
                                                شاكرين لسيادتكم دوام المتابعة والحرص.
                                            """.trimIndent()
                                            WhatsAppHelper.openWhatsApp(context, contactPhone, msg)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Chat,
                                            contentDescription = "إرسال تقرير بالواتساب",
                                            tint = EmeraldSuccess,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Status Selector Chips
                            Text(
                                text = "حالة الحضور:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Present Chip
                                FilterChip(
                                    selected = item.status == "present",
                                    onClick = { viewModel.updateStudentStatus(student.id, "present") },
                                    label = { Text("حاضر", style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldSuccessContainer,
                                        selectedLabelColor = EmeraldSuccess
                                    ),
                                    modifier = Modifier.weight(1f).testTag("status_present_${student.id}")
                                )
                                // Absent Chip
                                FilterChip(
                                    selected = item.status == "absent",
                                    onClick = { viewModel.updateStudentStatus(student.id, "absent") },
                                    label = { Text("غائب", style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CrimsonErrorContainer,
                                        selectedLabelColor = CrimsonError
                                    ),
                                    modifier = Modifier.weight(1f).testTag("status_absent_${student.id}")
                                )
                                // Late Chip
                                FilterChip(
                                    selected = item.status == "late",
                                    onClick = { viewModel.updateStudentStatus(student.id, "late") },
                                    label = { Text("متأخر", style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AmberGoldContainer,
                                        selectedLabelColor = AmberGold
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                // Excused Chip
                                FilterChip(
                                    selected = item.status == "excused",
                                    onClick = { viewModel.updateStudentStatus(student.id, "excused") },
                                    label = { Text("بعذر", style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NavyPrimaryContainer,
                                        selectedLabelColor = NavyPrimary
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Homework Evaluation Selector Chips
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "تقييم أداء الواجب (Homework):",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                FilterChip(
                                    selected = item.homeworkStatus == "completed",
                                    onClick = { viewModel.updateStudentHomeworkStatus(student.id, "completed") },
                                    label = { Text("حل كامل ✅", style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldSuccessContainer,
                                        selectedLabelColor = EmeraldSuccess
                                    ),
                                    modifier = Modifier.weight(1.1f).testTag("hw_completed_${student.id}")
                                )
                                FilterChip(
                                    selected = item.homeworkStatus == "partial",
                                    onClick = { viewModel.updateStudentHomeworkStatus(student.id, "partial") },
                                    label = { Text("ناقص ⚠️", style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AmberGoldContainer,
                                        selectedLabelColor = AmberGold
                                    ),
                                    modifier = Modifier.weight(1f).testTag("hw_partial_${student.id}")
                                )
                                FilterChip(
                                    selected = item.homeworkStatus == "not_done",
                                    onClick = { viewModel.updateStudentHomeworkStatus(student.id, "not_done") },
                                    label = { Text("لم يحل ❌", style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CrimsonErrorContainer,
                                        selectedLabelColor = CrimsonError
                                    ),
                                    modifier = Modifier.weight(1f).testTag("hw_notdone_${student.id}")
                                )
                                FilterChip(
                                    selected = item.homeworkStatus == "exempt",
                                    onClick = { viewModel.updateStudentHomeworkStatus(student.id, "exempt") },
                                    label = { Text("معفى ⚪", style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NavyPrimaryContainer,
                                        selectedLabelColor = NavyPrimary
                                    ),
                                    modifier = Modifier.weight(0.9f)
                                )
                            }

                            if (item.status != "present" || item.homeworkStatus == "not_done" || item.homeworkStatus == "partial") {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = item.note,
                                    onValueChange = { viewModel.updateStudentNote(student.id, it) },
                                    placeholder = { Text("ملاحظة عن الغياب أو الواجب...") },
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
                        description = "جرب البحث باسم آخر أو تغيير فلتر الحضور/الواجب",
                        icon = Icons.Filled.SearchOff
                    )
                }
            }
        }
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
        val currentGroup = state.groups.firstOrNull { it.id == state.selectedGroupId }
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
}
