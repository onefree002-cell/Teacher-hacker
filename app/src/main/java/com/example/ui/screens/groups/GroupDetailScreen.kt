package com.example.ui.screens.groups

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.example.data.export.PdfReportExporter
import com.example.data.local.entity.GroupEntity
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: Long,
    viewModel: GroupsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateHome: (() -> Unit)? = null,
    onNavigateToStudentDetail: (Long) -> Unit,
    onNavigateToAttendance: () -> Unit,
    onNavigateToStudyFiles: (grade: String) -> Unit = {},
    onOpenFileInViewer: (filePath: String, title: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0 = Students, 1 = Sessions, 2 = Exams
    var showNewTermDialog by remember { mutableStateOf(false) }
    var showEditGroupDialog by remember { mutableStateOf(false) }
    var showWhatsAppGroupDialog by remember { mutableStateOf(false) }
    var showMultiParentDialog by remember { mutableStateOf(false) }
    var filterTermOnlyCurrent by remember { mutableStateOf(false) }

    LaunchedEffect(groupId) {
        viewModel.loadGroupDetails(groupId)
    }

    val details = state.selectedGroupDetails

    Scaffold(
        topBar = {
            AppTopBar(
                title = details?.group?.name ?: "تفاصيل المجموعة",
                subtitle = "المجموعات الدراسية",
                onNavigateBack = onNavigateBack,
                onNavigateHome = onNavigateHome,
                showHomeButton = true,
                actions = {
                    IconButton(
                        onClick = { showWhatsAppGroupDialog = true },
                        modifier = Modifier.testTag("group_whatsapp_action_btn")
                    ) {
                        Icon(Icons.Filled.Groups, contentDescription = "جروب واتساب المجموعة", tint = EmeraldSuccess)
                    }
                    IconButton(
                        onClick = {
                            if (state.groupStudents.isEmpty()) {
                                Toast.makeText(context, "لا يوجد طلاب في المجموعة لطباعة الكارنيهات", Toast.LENGTH_SHORT).show()
                                return@IconButton
                            }
                            try {
                                val pdfFile = PdfReportExporter().generateStudentIdCardsPdf(
                                    context = context,
                                    teacher = state.teacher,
                                    group = details?.group,
                                    students = state.groupStudents
                                )
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "application/pdf")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "عرض وطباعة كارنيهات المجموعة"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "تعذر إنشاء ملف الكارنيهات: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("group_print_id_cards_btn")
                    ) {
                        Icon(Icons.Filled.Badge, contentDescription = "طباعة كارنيهات المجموعة", tint = NavyPrimary)
                    }
                    IconButton(onClick = { showEditGroupDialog = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "تعديل المجموعة")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        if (details != null) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Group Header Banner Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = details.group.name,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${details.group.stage.ifEmpty { "المرحلة" }} • ${details.group.grade} • كود: ${details.group.groupNumber.ifEmpty { "G-$groupId" }}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                            Button(
                                onClick = onNavigateToAttendance,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Filled.FactCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تسجيل حضور")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Terms & Schedule Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.EventRepeat,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "الترم النشط: ${details.group.currentTerm}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Text(
                                text = "💰 ${details.group.monthlyPrice} ج.م",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📅 ${details.group.sessionDays} الساعة ${com.example.util.TimeUtils.formatTimeArabic(details.group.sessionTime)} (${details.group.location})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )

                            // Button to transition to a new term
                            OutlinedButton(
                                onClick = { showNewTermDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Filled.Autorenew, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("الانتقال لترم جديد", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // WhatsApp Group & Multi-Parent Quick Actions Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { showWhatsAppGroupDialog = true },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.Groups, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldSuccess)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("جروب واتساب", style = MaterialTheme.typography.labelSmall)
                            }

                            FilledTonalButton(
                                onClick = { showMultiParentDialog = true },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = NavyPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("رسائل أولياء الأمور", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // Tab Row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("الطلاب (${state.groupStudents.size})", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("الحصص (${state.groupSessions.size})", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("الامتحانات (${state.groupExams.size})", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("الكتب والمذكرات 📚 (${state.groupStudyFiles.size})", fontSize = 11.sp, fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) }
                    )
                }

                // Content for selected tab
                when (selectedTab) {
                    0 -> {
                        if (state.groupStudents.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(state.groupStudents) { student ->
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onNavigateToStudentDetail(student.id) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
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
                                                    Text(student.name, fontWeight = FontWeight.SemiBold)
                                                    Text(student.phone.ifEmpty { "بدون هاتف" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            StatusBadge(status = student.status)
                                        }
                                    }
                                }
                            }
                        } else {
                            EmptyStateWidget(
                                title = "لا يوجد طلاب في هذه المجموعة",
                                description = "أضف طلاباً لهذه المجموعة لبدء تسجيل الحضور والدرجات",
                                icon = Icons.Filled.People
                            )
                        }
                    }
                    1 -> {
                        val displayedSessions = if (filterTermOnlyCurrent) {
                            state.groupSessions.filter { it.term.isEmpty() || it.term == details.group.currentTerm }
                        } else {
                            state.groupSessions
                        }

                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = !filterTermOnlyCurrent,
                                    onClick = { filterTermOnlyCurrent = false },
                                    label = { Text("جميع الأترام (${state.groupSessions.size})", style = MaterialTheme.typography.labelSmall) }
                                )
                                FilterChip(
                                    selected = filterTermOnlyCurrent,
                                    onClick = { filterTermOnlyCurrent = true },
                                    label = { Text("الترم الحالي فقط (${details.group.currentTerm})", style = MaterialTheme.typography.labelSmall) }
                                )
                            }

                            if (displayedSessions.isNotEmpty()) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(displayedSessions) { session ->
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text("حصة ${session.day} - ${session.time}", fontWeight = FontWeight.Bold)
                                                    Text("${session.location} • ${session.durationMinutes} دقيقة • ${session.term.ifEmpty { details.group.currentTerm }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                if (session.completed) {
                                                    Text("مكتملة ✓", color = EmeraldSuccess, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                                } else {
                                                    Text("قادمة", color = AmberGold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                EmptyStateWidget(
                                    title = "لا توجد حصص لهذا الفلتر",
                                    description = "يمكنك إضافة حصص من صفحة الجدول الدراسي",
                                    icon = Icons.Filled.CalendarMonth
                                )
                            }
                        }
                    }
                    2 -> {
                        val displayedExams = if (filterTermOnlyCurrent) {
                            state.groupExams.filter { it.term.isEmpty() || it.term == details.group.currentTerm }
                        } else {
                            state.groupExams
                        }

                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = !filterTermOnlyCurrent,
                                    onClick = { filterTermOnlyCurrent = false },
                                    label = { Text("جميع الأترام (${state.groupExams.size})", style = MaterialTheme.typography.labelSmall) }
                                )
                                FilterChip(
                                    selected = filterTermOnlyCurrent,
                                    onClick = { filterTermOnlyCurrent = true },
                                    label = { Text("الترم الحالي فقط (${details.group.currentTerm})", style = MaterialTheme.typography.labelSmall) }
                                )
                            }

                            if (displayedExams.isNotEmpty()) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(displayedExams) { exam ->
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text(exam.title, fontWeight = FontWeight.Bold)
                                                    Text("التاريخ: ${exam.date} • ${exam.term.ifEmpty { details.group.currentTerm }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Text("العظمى: ${exam.maxScore.toInt()}", fontWeight = FontWeight.Bold, color = AmberGold)
                                            }
                                        }
                                    }
                                }
                            } else {
                                EmptyStateWidget(
                                    title = "لا توجد امتحانات لهذه المجموعة في هذا الترم",
                                    description = "أنشئ امتحاناً لتقييم درجات طلاب هذه المجموعة",
                                    icon = Icons.Filled.Assignment
                                )
                            }
                        }
                    }
                    3 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Header action row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "كتب ومذكرات ${details.group.grade}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Button(
                                    onClick = { onNavigateToStudyFiles(details.group.grade) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إدارة ورفع ملفات", fontSize = 11.sp)
                                }
                            }

                            if (state.groupStudyFiles.isNotEmpty()) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(state.groupStudyFiles) { file ->
                                        Card(
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                onOpenFileInViewer(file.localFilePath, file.title)
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                                        modifier = Modifier.size(40.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                Icons.Filled.PictureAsPdf,
                                                                contentDescription = null,
                                                                tint = NavyPrimary,
                                                                modifier = Modifier.size(22.dp)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column {
                                                        Text(file.title, fontWeight = FontWeight.Bold, maxLines = 1)
                                                        Text("${file.category} • ${file.grade}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }

                                                FilledTonalButton(
                                                    onClick = {
                                                        onOpenFileInViewer(file.localFilePath, file.title)
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(14.dp), tint = NavyPrimary)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("فتح وشرح 📐", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                EmptyStateWidget(
                                    title = "لا توجد ملفات أو كتب مرفوعة لـ ${details.group.grade}",
                                    description = "ارفع كتاب الوزارة أو المذكرات لتتمكن من الشرح عليها وحل المسائل الهندسية مع طلاب هذه المجموعة",
                                    icon = Icons.Filled.LibraryBooks,
                                    actionText = "رفع ملف جديد للصف",
                                    onActionClick = { onNavigateToStudyFiles(details.group.grade) }
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // TRANSITION TO NEW TERM DIALOG
            // ==========================================
            if (showNewTermDialog) {
                var newTermInput by remember {
                    mutableStateOf(
                        when (details.group.currentTerm) {
                            "الترم الأول" -> "الترم الثاني"
                            "الترم الثاني" -> "الترم الثالث"
                            else -> "الترم الجديد"
                        }
                    )
                }

                AlertDialog(
                    onDismissRequest = { showNewTermDialog = false },
                    icon = { Icon(Icons.Filled.Autorenew, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    title = {
                        Text(
                            "الانتقال إلى ترم دراسي جديد",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "💡 سيتم بدء دورة حضور وامتحانات ومدفوعات جديدة للترم الجديد، مع الاحتفاظ التام بأرشيف وبيانات الترم السابق (${details.group.currentTerm}) والرجوع إليها أو طباعتها في أي وقت.",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }

                            OutlinedTextField(
                                value = newTermInput,
                                onValueChange = { newTermInput = it },
                                label = { Text("مسمى الترم الجديد") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Quick suggestions
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf("الترم الأول", "الترم الثاني", "الترم الثالث", "الفصل الصيفي").forEach { tName ->
                                    SuggestionChip(
                                        onClick = { newTermInput = tName },
                                        label = { Text(tName, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (newTermInput.isNotBlank()) {
                                    viewModel.transitionToNewTerm(details.group.id, newTermInput.trim())
                                    showNewTermDialog = false
                                }
                            }
                        ) {
                            Text("تأكيد وبدء الترم الجديد")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showNewTermDialog = false }) {
                            Text("إلغاء")
                        }
                    }
                )
            }

            // ==========================================
            // EDIT GROUP DIALOG
            // ==========================================
            if (showEditGroupDialog) {
                AddEditGroupDialog(
                    group = details.group,
                    venues = state.venues,
                    onAddNewVenue = { venue, onSaved -> viewModel.addNewVenue(venue, onSaved) },
                    onDismiss = { showEditGroupDialog = false },
                    onSave = { updatedGroup ->
                        viewModel.addOrUpdateGroup(updatedGroup)
                        showEditGroupDialog = false
                    }
                )
            }

            // ==========================================
            // WHATSAPP GROUP DIALOG
            // ==========================================
            if (showWhatsAppGroupDialog) {
                WhatsAppGroupDialog(
                    group = details.group,
                    students = state.groupStudents,
                    teacher = state.teacher,
                    onDismiss = { showWhatsAppGroupDialog = false },
                    onSaveGroupLink = { updatedLink ->
                        viewModel.addOrUpdateGroup(details.group.copy(whatsappGroupLink = updatedLink))
                    }
                )
            }

            // ==========================================
            // MULTI-PARENT MESSAGING DIALOG
            // ==========================================
            if (showMultiParentDialog) {
                MultiParentMessagingDialog(
                    students = state.groupStudents,
                    teacherName = state.teacher.name,
                    groupName = details.group.name,
                    onDismiss = { showMultiParentDialog = false }
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}
