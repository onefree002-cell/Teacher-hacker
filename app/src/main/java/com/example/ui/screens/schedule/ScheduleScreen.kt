package com.example.ui.screens.schedule

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.example.data.local.entity.SessionEntity
import com.example.data.repository.SessionWithGroup
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.WhatsAppHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel,
    onNavigateToAttendance: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    onNavigateHome: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddDialog by remember { mutableStateOf(false) }
    var initialDayForAdd by remember { mutableStateOf<String?>(null) }
    var initialDateForAdd by remember { mutableStateOf<String?>(null) }
    var sessionToEdit by remember { mutableStateOf<SessionEntity?>(null) }
    var sessionToDelete by remember { mutableStateOf<SessionEntity?>(null) }

    val daysOfWeek = listOf(
        "all" to "الكل",
        "السبت" to "السبت",
        "الأحد" to "الأحد",
        "الإثنين" to "الإثنين",
        "الثلاثاء" to "الثلاثاء",
        "الأربعاء" to "الأربعاء",
        "الخميس" to "الخميس",
        "الجمعة" to "الجمعة"
    )

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = "جدول الحصص والمواعيد (${state.filteredSessions.size})",
                subtitle = if (state.selectedDay == "all") "جميع أيام الأسبوع" else "جدول يوم ${state.selectedDay}",
                onNavigateBack = onNavigateBack,
                onNavigateHome = onNavigateHome,
                showHomeButton = true,
                actions = {
                    // View Mode Switcher (Weekly Timetable vs Calendar vs Cards)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.setViewMode(ScheduleViewMode.WEEKLY_TIMETABLE) },
                            modifier = Modifier.size(32.dp).testTag("mode_weekly_timetable")
                        ) {
                            Icon(
                                Icons.Filled.TableChart,
                                contentDescription = "عرض أسبوعي",
                                tint = if (state.viewMode == ScheduleViewMode.WEEKLY_TIMETABLE) MaterialTheme.colorScheme.primary else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.setViewMode(ScheduleViewMode.MONTHLY_CALENDAR) },
                            modifier = Modifier.size(32.dp).testTag("mode_monthly_calendar")
                        ) {
                            Icon(
                                Icons.Filled.CalendarMonth,
                                contentDescription = "عرض التقويم",
                                tint = if (state.viewMode == ScheduleViewMode.MONTHLY_CALENDAR) MaterialTheme.colorScheme.primary else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.setViewMode(ScheduleViewMode.CARDS) },
                            modifier = Modifier.size(32.dp).testTag("mode_cards")
                        ) {
                            Icon(
                                Icons.Filled.ViewAgenda,
                                contentDescription = "عرض البطاقات",
                                tint = if (state.viewMode == ScheduleViewMode.CARDS) MaterialTheme.colorScheme.primary else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Print / Share Schedule PDF button
                    IconButton(
                        onClick = { viewModel.printSchedulePdf(context) },
                        enabled = !state.isExportingPdf && state.filteredSessions.isNotEmpty(),
                        modifier = Modifier.testTag("print_schedule_pdf_btn")
                    ) {
                        if (state.isExportingPdf) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                Icons.Filled.PictureAsPdf,
                                contentDescription = "طباعة ومشاركة الجدول PDF",
                                tint = if (state.filteredSessions.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_session_fab")
            ) {
                Icon(Icons.Filled.AddAlarm, contentDescription = "إضافة حصة")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Days selector row with count badges
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(daysOfWeek) { (key, label) ->
                    val count = if (key == "all") {
                        state.sessionsWithGroups.size
                    } else {
                        state.sessionsWithGroups.count { it.session.day.contains(key) }
                    }
                    FilterChip(
                        selected = state.selectedDay == key,
                        onClick = { viewModel.onDaySelected(key) },
                        label = {
                            Text(if (count > 0) "$label ($count)" else label)
                        },
                        leadingIcon = if (state.selectedDay == key) {
                            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        modifier = Modifier.testTag("schedule_day_$key")
                    )
                }
            }

            // Summary Info Header Bar
            val totalMinutes = state.filteredSessions.sumOf { it.session.durationMinutes }
            val totalHours = String.format("%.1f", totalMinutes / 60.0)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${state.filteredSessions.size} حصص • $totalHours ساعة",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Quick Share All Day Schedule on WhatsApp
                        TextButton(
                            onClick = {
                                val scheduleText = buildString {
                                    appendLine("📅 *جدول الحصص والمواعيد*")
                                    appendLine("👨‍🏫 المعلم: ${state.teacher.name.ifEmpty { "عبده أيمن" }}")
                                    if (state.selectedDay != "all") appendLine("📆 يوم: ${state.selectedDay}")
                                    appendLine("───────────────")
                                    state.filteredSessions.forEachIndexed { i, item ->
                                        appendLine("${i + 1}. *${item.groupName}*")
                                        appendLine("   ⏰ الموعد: ${item.session.day} - ${item.session.time} (${item.session.durationMinutes} دقيقة)")
                                        appendLine("   📍 المقر: ${item.location.ifEmpty { item.session.location }}")
                                        if (item.session.homeworkTitle.isNotEmpty()) {
                                            appendLine("   📚 الواجب: ${item.session.homeworkTitle}")
                                        }
                                        appendLine("")
                                    }
                                    appendLine("📞 للتواصل: ${state.teacher.phone.ifEmpty { "01206150946" }}")
                                }
                                WhatsAppHelper.openWhatsApp(context, "", scheduleText)
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مشاركة الجدول", style = MaterialTheme.typography.labelSmall, color = EmeraldSuccess)
                        }

                        // Print PDF action button in banner
                        FilledTonalButton(
                            onClick = { viewModel.printSchedulePdf(context) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("طباعة PDF", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            when (state.viewMode) {
                ScheduleViewMode.MONTHLY_CALENDAR -> {
                    MonthlyCalendarView(
                        state = state,
                        onDateSelected = { dateStr ->
                            viewModel.setSelectedCalendarDate(dateStr)
                        },
                        onAddSessionForDate = { dayName, dateStr ->
                            initialDayForAdd = dayName
                            initialDateForAdd = dateStr
                            showAddDialog = true
                        },
                        onEditSession = { sessionToEdit = it },
                        onToggleCompletion = { viewModel.toggleSessionCompletion(it) },
                        onDeleteSession = { sessionToDelete = it },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                ScheduleViewMode.CARDS -> {
                    if (state.filteredSessions.isNotEmpty()) {
                        // 1. Cards View
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(state.filteredSessions, key = { it.session.id }) { item ->
                                val sess = item.session
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("session_item_${sess.id}")
                                ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.groupName,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "يوم ${sess.day}${if (sess.date.isNotBlank()) " (${sess.date})" else ""} • ${item.location.ifEmpty { sess.location }}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(NavyPrimaryContainer)
                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = "${com.example.util.TimeUtils.formatTimeArabic(sess.time)} (${sess.durationMinutes} د)",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = NavyPrimary
                                            )
                                        }
                                    }

                                    if (sess.note.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "ملاحظة: ${sess.note}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Homework block
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (sess.homeworkTitle.isNotEmpty() || sess.homeworkPages.isNotEmpty())
                                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Filled.MenuBook,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = if (sess.homeworkTitle.isNotEmpty()) "الواجب: ${sess.homeworkTitle}" else "واجب الحصة",
                                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }

                                                if (sess.homeworkTitle.isNotEmpty() || sess.homeworkPages.isNotEmpty()) {
                                                    Row {
                                                        IconButton(
                                                            onClick = {
                                                                val hwText = buildString {
                                                                    appendLine("📚 *واجب حصة ${item.groupName}*")
                                                                    if (sess.homeworkTitle.isNotEmpty()) appendLine("📝 الموضوع: ${sess.homeworkTitle}")
                                                                    if (sess.homeworkPages.isNotEmpty()) appendLine("📄 الصفحات والمسائل: ${sess.homeworkPages}")
                                                                    if (sess.homeworkDeadline.isNotEmpty()) appendLine("⏰ موعد التسليم: ${sess.homeworkDeadline}")
                                                                    if (sess.homeworkNotes.isNotEmpty()) appendLine("💡 تعليمات: ${sess.homeworkNotes}")
                                                                }
                                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                                clipboard.setPrimaryClip(ClipData.newPlainText("Homework", hwText))
                                                                Toast.makeText(context, "تم نسخ تفاصيل الواجب", Toast.LENGTH_SHORT).show()
                                                            },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(Icons.Filled.ContentCopy, contentDescription = "نسخ الواجب", modifier = Modifier.size(16.dp))
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                val hwText = buildString {
                                                                    appendLine("السلام عليكم ورحمة الله وبركاته،")
                                                                    appendLine("طلاب مجموعة *${item.groupName}* الكرام،")
                                                                    appendLine("")
                                                                    appendLine("📚 *واجب الحصة:*")
                                                                    if (sess.homeworkTitle.isNotEmpty()) appendLine("📝 الموضوع: ${sess.homeworkTitle}")
                                                                    if (sess.homeworkPages.isNotEmpty()) appendLine("📄 الصفحات / المسائل: ${sess.homeworkPages}")
                                                                    if (sess.homeworkDeadline.isNotEmpty()) appendLine("⏰ موعد التسليم: ${sess.homeworkDeadline}")
                                                                    if (sess.homeworkNotes.isNotEmpty()) appendLine("💡 تعليمات: ${sess.homeworkNotes}")
                                                                    appendLine("")
                                                                    appendLine("يرجى الالتزام بالحل والمتابعة بالتوفيق!")
                                                                }
                                                                WhatsAppHelper.openWhatsApp(context, "", hwText)
                                                            },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(Icons.Filled.Share, contentDescription = "مشاركة الواجب", tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                                                        }
                                                    }
                                                }
                                            }

                                            if (sess.homeworkPages.isNotEmpty() || sess.homeworkDeadline.isNotEmpty() || sess.homeworkNotes.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                if (sess.homeworkPages.isNotEmpty()) {
                                                    Text(
                                                        text = "📄 الصفحات: ${sess.homeworkPages}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                if (sess.homeworkDeadline.isNotEmpty()) {
                                                    Text(
                                                        text = "⏰ التسليم: ${sess.homeworkDeadline}",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                if (sess.homeworkNotes.isNotEmpty()) {
                                                    Text(
                                                        text = "💡 ملاحظات: ${sess.homeworkNotes}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            } else if (sess.homeworkTitle.isEmpty()) {
                                                Text(
                                                    text = "لم يتم تحديد واجب بعد (اضغط تعديل لإضافة واجب الحصة)",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Divider()
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = sess.completed,
                                                onCheckedChange = { viewModel.toggleSessionCompletion(sess) }
                                            )
                                            Text(
                                                text = if (sess.completed) "تمت الحصة" else "غير مكتملة",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }

                                        Row {
                                            FilledTonalButton(
                                                onClick = onNavigateToAttendance,
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Filled.FactCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("الحضور", style = MaterialTheme.typography.labelSmall)
                                            }
                                            IconButton(onClick = { sessionToEdit = sess }) {
                                                Icon(Icons.Filled.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            }
                                            IconButton(onClick = { sessionToDelete = sess }) {
                                                Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    } else {
                        EmptyStateWidget(
                            title = "لا توجد حصص مجدولة",
                            description = "أضف حصص ومواعيد مجموعاتك لتنظيم جدولك ومنع التعارضات",
                            icon = Icons.Filled.CalendarMonth,
                            actionText = "+ إضافة حصة",
                            onActionClick = { showAddDialog = true }
                        )
                    }
                }
                ScheduleViewMode.WEEKLY_TIMETABLE -> {
                    if (state.filteredSessions.isNotEmpty()) {
                        // 2. Weekly Timetable Matrix View
                        val dayOrder = listOf("السبت", "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة")
                        val groupedByDay = state.filteredSessions.groupBy { it.session.day }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                        // Quick Print & Share Timetable Banner
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
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
                                        Text("📅 عرض الجدول الأسبوعي المنظم", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                        Text("طباعة الجدول كاملاً أو مشاركته مع الطلاب وأولياء الأمور", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Button(
                                        onClick = { viewModel.printSchedulePdf(context) },
                                        enabled = !state.isExportingPdf,
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("طباعة الجدول PDF", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }

                        val activeDays = if (state.selectedDay == "all") {
                            dayOrder.filter { groupedByDay.containsKey(it) }
                        } else {
                            listOf(state.selectedDay).filter { groupedByDay.containsKey(it) }
                        }

                        items(activeDays) { day ->
                            val sessionsInDay = groupedByDay[day] ?: emptyList()
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
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
                                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Filled.CalendarMonth,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "يوم $day",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                            Text("${sessionsInDay.size} حصص", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(10.dp))

                                    sessionsInDay.forEachIndexed { idx, sItem ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (idx % 2 == 0) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else Color.Transparent)
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(if (sItem.session.completed) EmeraldSuccess else NavyPrimary)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = sItem.groupName,
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                Text(
                                                    text = "📍 ${sItem.location.ifEmpty { sItem.session.location }}${if (sItem.session.date.isNotBlank()) " • 📅 ${sItem.session.date}" else ""}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(NavyPrimaryContainer)
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "${com.example.util.TimeUtils.formatTimeArabic(sItem.session.time)} (${sItem.session.durationMinutes}د)",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = NavyPrimary
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(6.dp))

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = onNavigateToAttendance,
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Filled.FactCheck, contentDescription = "حضور", tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                                                }
                                                IconButton(
                                                    onClick = { sessionToEdit = sItem.session },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Filled.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                }
                                                IconButton(
                                                    onClick = { sessionToDelete = sItem.session },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                        if (idx < sessionsInDay.size - 1) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    } else {
                        EmptyStateWidget(
                            title = "لا توجد حصص مجدولة",
                            description = "أضف حصص ومواعيد مجموعاتك لتنظيم جدولك ومنع التعارضات",
                            icon = Icons.Filled.CalendarMonth,
                            actionText = "+ إضافة حصة",
                            onActionClick = { showAddDialog = true }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditSessionDialog(
            initialDay = initialDayForAdd,
            initialDate = initialDateForAdd,
            groups = state.groups,
            venues = state.venues,
            onAddNewVenue = { venue, onSaved -> viewModel.addNewVenue(venue, onSaved) },
            onDismiss = {
                showAddDialog = false
                initialDayForAdd = null
                initialDateForAdd = null
            },
            onSave = { sess, onConflict ->
                viewModel.addOrUpdateSession(sess, onConflict) {
                    showAddDialog = false
                    initialDayForAdd = null
                    initialDateForAdd = null
                }
            }
        )
    }

    sessionToEdit?.let { sess ->
        AddEditSessionDialog(
            session = sess,
            groups = state.groups,
            venues = state.venues,
            onAddNewVenue = { venue, onSaved -> viewModel.addNewVenue(venue, onSaved) },
            onDismiss = { sessionToEdit = null },
            onSave = { updated, onConflict ->
                viewModel.addOrUpdateSession(updated, onConflict) {
                    sessionToEdit = null
                }
            }
        )
    }

    sessionToDelete?.let { sess ->
        ConfirmDeleteDialog(
            title = "حذف الحصة",
            message = "هل تريد حذف هذه الحصة من الجدول؟",
            onConfirm = { viewModel.deleteSession(sess) },
            onDismiss = { sessionToDelete = null }
        )
    }
}
