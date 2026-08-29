package com.example.ui.screens.schedule

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.SessionEntity
import com.example.data.repository.ExamWithGroup
import com.example.data.repository.SessionWithGroup
import com.example.ui.components.EmptyStateWidget
import com.example.ui.theme.*
import com.example.util.SessionNotificationHelper
import com.example.util.TimeUtils
import com.example.util.WhatsAppHelper
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MonthlyCalendarView(
    state: ScheduleUiState,
    onDateSelected: (String) -> Unit,
    onAddSessionForDate: (String, String) -> Unit, // (dayName, dateStr)
    onEditSession: (SessionEntity) -> Unit,
    onToggleCompletion: (SessionEntity) -> Unit,
    onDeleteSession: (SessionEntity) -> Unit,
    onTakeAttendance: ((groupId: Long, date: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedCalendarDay by remember { mutableStateOf(Calendar.getInstance()) }
    var isCalendarGridVisible by remember { mutableStateOf(true) }
    var selectedViewFilter by remember { mutableIntStateOf(0) } // 0=Selected Day, 1=All Month Sessions, 2=All Month Exams

    var showPostponeDialog by remember { mutableStateOf<SessionWithGroup?>(null) }
    var showCancelDialog by remember { mutableStateOf<SessionWithGroup?>(null) }

    val monthFormatter = SimpleDateFormat("MMMM yyyy", Locale("ar"))
    val dayNameFormatter = SimpleDateFormat("EEEE", Locale("ar"))
    val fullDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val displayDateFormatter = SimpleDateFormat("EEEE، d MMMM yyyy", Locale("ar"))
    val monthNumberFormatter = SimpleDateFormat("yyyy-MM", Locale.getDefault())

    val currentMonthPrefix = monthNumberFormatter.format(currentCalendar.time)
    val todayStr = fullDateFormatter.format(Date())
    val selectedDateStr = fullDateFormatter.format(selectedCalendarDay.time)
    val selectedDayName = dayNameFormatter.format(selectedCalendarDay.time)

    // Calculate calendar days for grid
    val daysInGrid = remember(currentCalendar.get(Calendar.YEAR), currentCalendar.get(Calendar.MONTH), state.sessionsWithGroups, state.exams) {
        val cal = currentCalendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sunday, 7=Saturday
        // Adjust for Arabic week starting on Saturday (Saturday = 7 -> index 0, Sunday = 1 -> index 1, etc.)
        val offset = (firstDayOfWeek % 7) // Saturday is 0
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val grid = mutableListOf<CalendarDayInfo?>()
        for (i in 0 until offset) {
            grid.add(null) // Empty preceding cells
        }
        for (day in 1..maxDays) {
            val dayCal = cal.clone() as Calendar
            dayCal.set(Calendar.DAY_OF_MONTH, day)
            val dStr = fullDateFormatter.format(dayCal.time)
            val dName = dayNameFormatter.format(dayCal.time)

            val sessionCount = state.sessionsWithGroups.count {
                it.session.date == dStr || (it.session.date.isBlank() && (it.session.day.contains(dName) || dName.contains(it.session.day)))
            }
            val examCount = state.exams.count { it.exam.date == dStr }

            grid.add(
                CalendarDayInfo(
                    dayNumber = day,
                    dateString = dStr,
                    dayName = dName,
                    calendar = dayCal,
                    isToday = dStr == todayStr,
                    sessionCount = sessionCount,
                    examCount = examCount
                )
            )
        }
        grid
    }

    // Sessions and exams for currently selected date
    val selectedDateSessions = remember(selectedDateStr, selectedDayName, state.sessionsWithGroups) {
        state.sessionsWithGroups.filter {
            it.session.date == selectedDateStr || (it.session.date.isBlank() && (it.session.day.contains(selectedDayName) || selectedDayName.contains(it.session.day)))
        }.sortedBy { TimeUtils.timeToMinutes(it.session.time) }
    }

    val selectedDateExams = remember(selectedDateStr, state.exams) {
        state.exams.filter { it.exam.date == selectedDateStr }
    }

    // All Sessions in the current calendar month
    val monthAllSessions = remember(currentMonthPrefix, state.sessionsWithGroups) {
        state.sessionsWithGroups.filter {
            it.session.date.startsWith(currentMonthPrefix) || it.session.date.isBlank()
        }.sortedWith(compareBy({ it.session.date }, { TimeUtils.timeToMinutes(it.session.time) }))
    }

    val monthAllExams = remember(currentMonthPrefix, state.exams) {
        state.exams.filter {
            it.exam.date.startsWith(currentMonthPrefix)
        }.sortedBy { it.exam.date }
    }

    // Entire screen is ONE unified LazyColumn to guarantee 100% smooth scrolling
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp, top = 6.dp)
    ) {
        // 1. Month Navigation Header Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val next = currentCalendar.clone() as Calendar
                                next.add(Calendar.MONTH, -1)
                                currentCalendar = next
                            }
                        ) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = "الشهر السابق")
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = monthFormatter.format(currentCalendar.time),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            AssistChip(
                                onClick = {
                                    val now = Calendar.getInstance()
                                    currentCalendar = now
                                    selectedCalendarDay = now
                                    onDateSelected(fullDateFormatter.format(now.time))
                                },
                                label = { Text("اليوم", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { isCalendarGridVisible = !isCalendarGridVisible },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCalendarGridVisible) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                    contentDescription = if (isCalendarGridVisible) "طي التقويم" else "إظهار التقويم",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = {
                                    val next = currentCalendar.clone() as Calendar
                                    next.add(Calendar.MONTH, 1)
                                    currentCalendar = next
                                }
                            ) {
                                Icon(Icons.Filled.ChevronLeft, contentDescription = "الشهر التالي")
                            }
                        }
                    }

                    // Expandable / Collapsible Calendar Grid
                    AnimatedVisibility(visible = isCalendarGridVisible) {
                        Column {
                            // Days of Week Header
                            val weekDays = listOf("سبت", "أحد", "إثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة")
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                weekDays.forEach { wDay ->
                                    Text(
                                        text = wDay,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            // Calendar Days Grid (7 columns)
                            val rows = daysInGrid.chunked(7)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                rows.forEach { weekRow ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceAround
                                    ) {
                                        for (i in 0 until 7) {
                                            val dayInfo = weekRow.getOrNull(i)
                                            if (dayInfo != null) {
                                                val isSelected = dayInfo.dateString == selectedDateStr
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .aspectRatio(1.05f)
                                                        .padding(1.5.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(
                                                            when {
                                                                isSelected -> MaterialTheme.colorScheme.primary
                                                                dayInfo.isToday -> MaterialTheme.colorScheme.primaryContainer
                                                                else -> Color.Transparent
                                                            }
                                                        )
                                                        .border(
                                                            width = if (dayInfo.isToday && !isSelected) 1.5.dp else 0.dp,
                                                            color = if (dayInfo.isToday && !isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .clickable {
                                                            selectedCalendarDay = dayInfo.calendar
                                                            onDateSelected(dayInfo.dateString)
                                                            selectedViewFilter = 0 // Switch to selected day view
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        Text(
                                                            text = dayInfo.dayNumber.toString(),
                                                            style = MaterialTheme.typography.bodySmall.copy(
                                                                fontWeight = if (isSelected || dayInfo.isToday) FontWeight.Bold else FontWeight.Normal,
                                                                fontSize = 12.sp
                                                            ),
                                                            color = when {
                                                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                                                dayInfo.isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                                                else -> MaterialTheme.colorScheme.onSurface
                                                            }
                                                        )

                                                        // Indicator Dots (Sessions & Exams)
                                                        if (dayInfo.sessionCount > 0 || dayInfo.examCount > 0) {
                                                            Row(
                                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                modifier = Modifier.padding(top = 1.dp)
                                                            ) {
                                                                if (dayInfo.sessionCount > 0) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .size(5.dp)
                                                                            .clip(CircleShape)
                                                                            .background(if (isSelected) Color.White else Color(0xFF2563EB))
                                                                    )
                                                                }
                                                                if (dayInfo.examCount > 0) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .size(5.dp)
                                                                            .clip(CircleShape)
                                                                            .background(if (isSelected) Color(0xFFFDE047) else Color(0xFFDC2626))
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.weight(1f))
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

        // 2. View Mode Filter Tabs (Selected Day vs All Month Sessions vs All Month Exams)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedViewFilter == 0,
                    onClick = { selectedViewFilter = 0 },
                    label = { Text("حصص اليوم (${selectedDateSessions.size})", fontSize = 11.sp) },
                    leadingIcon = if (selectedViewFilter == 0) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null,
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = selectedViewFilter == 1,
                    onClick = { selectedViewFilter = 1 },
                    label = { Text("حصص الشهر (${monthAllSessions.size})", fontSize = 11.sp) },
                    leadingIcon = if (selectedViewFilter == 1) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null,
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = selectedViewFilter == 2,
                    onClick = { selectedViewFilter = 2 },
                    label = { Text("الامتحانات (${monthAllExams.size})", fontSize = 11.sp) },
                    leadingIcon = if (selectedViewFilter == 2) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Header Banner with quick "+ Add Session"
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (selectedViewFilter == 0) displayDateFormatter.format(selectedCalendarDay.time) else "حصص ومواعيد شهر ${monthFormatter.format(currentCalendar.time)}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                        Text(
                            text = when (selectedViewFilter) {
                                0 -> "${selectedDateSessions.size} حصص مجدولة • ${selectedDateExams.size} امتحانات"
                                1 -> "إجمالي ${monthAllSessions.size} حصة طوال هذا الشهر"
                                else -> "إجمالي ${monthAllExams.size} امتحان خلال هذا الشهر"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            onAddSessionForDate(selectedDayName, selectedDateStr)
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("add_session_for_selected_date_btn")
                    ) {
                        Icon(Icons.Filled.AddAlarm, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة حصة +", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 4. List Items based on selectedViewFilter
        when (selectedViewFilter) {
            0 -> {
                // View: Selected Date Sessions & Exams
                if (selectedDateExams.isNotEmpty()) {
                    item {
                        Text(
                            text = "📝 الامتحانات في هذا اليوم (${selectedDateExams.size})",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = CrimsonError
                        )
                    }
                    items(selectedDateExams, key = { "exam_${it.exam.id}" }) { ex ->
                        ExamCalendarCard(ex = ex)
                    }
                }

                if (selectedDateSessions.isNotEmpty()) {
                    item {
                        Text(
                            text = "⏰ حصص اليوم (${selectedDateSessions.size})",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = NavyPrimary
                        )
                    }
                    items(selectedDateSessions, key = { "session_${it.session.id}" }) { item ->
                        CalendarSessionCard(
                            item = item,
                            displayDate = selectedDateStr,
                            teacherName = state.teacher.name.ifBlank { "عبده أيمن" },
                            onToggleCompletion = { onToggleCompletion(item.session) },
                            onEdit = { onEditSession(item.session) },
                            onDelete = { onDeleteSession(item.session) },
                            onTakeAttendance = {
                                val sessionDate = if (item.session.date.isNotBlank()) item.session.date else selectedDateStr
                                onTakeAttendance?.invoke(item.session.groupId, sessionDate)
                            },
                            onRemind30Min = {
                                SessionNotificationHelper.showSessionUpcomingReminder(
                                    context = context,
                                    session = item.session,
                                    groupName = item.groupName,
                                    location = item.location
                                )
                                Toast.makeText(context, "تم ضبط وإرسال تذكير الحصة قبل الموعد بـ 30 دقيقة ⏰", Toast.LENGTH_SHORT).show()
                            },
                            onPostpone = { showPostponeDialog = item },
                            onCancel = { showCancelDialog = item }
                        )
                    }
                }

                if (selectedDateSessions.isEmpty() && selectedDateExams.isEmpty()) {
                    item {
                        EmptyStateWidget(
                            title = "لا توجد حصص أو امتحانات مجدولة في هذا اليوم",
                            description = "اضغط على زر (إضافة حصة +) بالأعلى لجدولة حصة جديدة ليوم $selectedDayName ($selectedDateStr)",
                            icon = Icons.Filled.EventAvailable
                        )
                    }
                }
            }
            1 -> {
                // View: All Month Sessions
                if (monthAllSessions.isNotEmpty()) {
                    items(monthAllSessions, key = { "all_session_${it.session.id}" }) { item ->
                        CalendarSessionCard(
                            item = item,
                            displayDate = item.session.date.ifBlank { item.session.day },
                            teacherName = state.teacher.name.ifBlank { "عبده أيمن" },
                            onToggleCompletion = { onToggleCompletion(item.session) },
                            onEdit = { onEditSession(item.session) },
                            onDelete = { onDeleteSession(item.session) },
                            onTakeAttendance = {
                                val sessionDate = if (item.session.date.isNotBlank()) item.session.date else selectedDateStr
                                onTakeAttendance?.invoke(item.session.groupId, sessionDate)
                            },
                            onRemind30Min = {
                                SessionNotificationHelper.showSessionUpcomingReminder(
                                    context = context,
                                    session = item.session,
                                    groupName = item.groupName,
                                    location = item.location
                                )
                                Toast.makeText(context, "تم ضبط وإرسال تذكير الحصة قبل الموعد بـ 30 دقيقة ⏰", Toast.LENGTH_SHORT).show()
                            },
                            onPostpone = { showPostponeDialog = item },
                            onCancel = { showCancelDialog = item }
                        )
                    }
                } else {
                    item {
                        EmptyStateWidget(
                            title = "لا توجد حصص مسجلة لهذا الشهر",
                            description = "يمكنك إضافة وتوزيع مواعيد الحصص بسهولة",
                            icon = Icons.Filled.CalendarMonth
                        )
                    }
                }
            }
            2 -> {
                // View: All Month Exams
                if (monthAllExams.isNotEmpty()) {
                    items(monthAllExams, key = { "all_exam_${it.exam.id}" }) { ex ->
                        ExamCalendarCard(ex = ex)
                    }
                } else {
                    item {
                        EmptyStateWidget(
                            title = "لا توجد امتحانات مسجلة لهذا الشهر",
                            description = "أضف مواعيد الاختبارات لتظهر مباشرة في جدول الشهر",
                            icon = Icons.Filled.Assignment
                        )
                    }
                }
            }
        }
    }

    // Postpone Session Dialog
    showPostponeDialog?.let { sess ->
        SessionPostponeDialog(
            sessionWithGroup = sess,
            teacherName = state.teacher.name.ifBlank { "عبده أيمن" },
            onDismiss = { showPostponeDialog = null }
        )
    }

    // Cancel Session Dialog
    showCancelDialog?.let { sess ->
        SessionCancellationDialog(
            sessionWithGroup = sess,
            teacherName = state.teacher.name.ifBlank { "عبده أيمن" },
            onDismiss = { showCancelDialog = null }
        )
    }
}

private data class CalendarDayInfo(
    val dayNumber: Int,
    val dateString: String,
    val dayName: String,
    val calendar: Calendar,
    val isToday: Boolean,
    val sessionCount: Int,
    val examCount: Int
)

@Composable
private fun ExamCalendarCard(ex: ExamWithGroup) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CrimsonError),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Assignment, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = ex.exam.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "مجموعة: ${ex.groupName} • 📅 التاريخ: ${ex.exam.date} • الدرجة العظمى: ${ex.exam.maxScore.toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            AssistChip(
                onClick = {},
                label = { Text(ex.exam.term, fontSize = 10.sp) }
            )
        }
    }
}

@Composable
fun CalendarSessionCard(
    item: SessionWithGroup,
    displayDate: String = "",
    teacherName: String,
    onToggleCompletion: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTakeAttendance: () -> Unit,
    onRemind30Min: () -> Unit,
    onPostpone: () -> Unit,
    onCancel: () -> Unit
) {
    val session = item.session
    val context = LocalContext.current
    val effectiveDate = if (session.date.isNotBlank()) session.date else displayDate

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (session.completed) Color(0xFFF1F5F9) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Group name, Day & Exact Date Badge, Edit & Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Checkbox(
                        checked = session.completed,
                        onCheckedChange = { onToggleCompletion() }
                    )
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = item.groupName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = if (session.completed) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                ),
                                color = if (session.completed) Color.Gray else MaterialTheme.colorScheme.onSurface
                            )
                            if (effectiveDate.isNotBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "📅 $effectiveDate",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "⏰ ${session.day} الساعة ${session.time} (${session.durationMinutes} دقيقة) • 📍 ${item.location.ifBlank { session.location.ifBlank { "المقر المعتاد" } }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "تعديل", tint = NavyPrimary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف", tint = CrimsonError, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Homework Section with specific session date
            if (session.homeworkTitle.isNotBlank() || session.homeworkNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = AmberGold.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberGold.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.MenuBook, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "📚 واجب الحصة (${effectiveDate.ifBlank { session.day }}): ${session.homeworkTitle}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (session.homeworkNotes.isNotBlank() && session.homeworkNotes != session.homeworkTitle) {
                                Text(
                                    text = session.homeworkNotes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Action Buttons: Take Attendance for this exact date, 30-min Reminder, Postpone, Cancel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Take Attendance Button
                Button(
                    onClick = onTakeAttendance,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp).weight(1.2f)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تسجيل الحضور 📋", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                // 30-min Reminder
                FilledTonalButton(
                    onClick = onRemind30Min,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp).weight(1f)
                ) {
                    Icon(Icons.Filled.Alarm, contentDescription = null, modifier = Modifier.size(13.dp), tint = NavyPrimary)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("تنبيه 30د", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // Postpone
                OutlinedButton(
                    onClick = onPostpone,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp).weight(0.9f)
                ) {
                    Icon(Icons.Filled.Update, contentDescription = null, modifier = Modifier.size(13.dp), tint = AmberGold)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("تأجيل", fontSize = 10.sp)
                }

                // Cancel
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonError),
                    modifier = Modifier.height(30.dp).weight(0.9f)
                ) {
                    Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(13.dp), tint = CrimsonError)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("اعتذار", fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun SessionPostponeDialog(
    sessionWithGroup: SessionWithGroup,
    teacherName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var newDateTime by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Update, contentDescription = null, tint = AmberGold)
                Text("إشعار تأجيل حصة [${sessionWithGroup.groupName}]", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "الموعد الحالي: ${sessionWithGroup.session.day} (${sessionWithGroup.session.date.ifBlank { "اليوم" }}) الساعة ${sessionWithGroup.session.time}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray
                )
                OutlinedTextField(
                    value = newDateTime,
                    onValueChange = { newDateTime = it },
                    label = { Text("الموعد البديل الجديد *") },
                    placeholder = { Text("مثال: الأربعاء القادم الساعة 4:00 عصراً") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("سبب التأجيل (اختياري)") },
                    placeholder = { Text("مثال: لظروف طارئة / امتحانات مدرسية") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newDateTime.isNotBlank()) {
                        val message = SessionNotificationHelper.generatePostponementMessage(
                            teacherName = teacherName,
                            groupName = sessionWithGroup.groupName,
                            oldDateOrTime = "${sessionWithGroup.session.day} - ${sessionWithGroup.session.time}",
                            newDateOrTime = newDateTime,
                            reason = reason
                        )
                        WhatsAppHelper.openWhatsApp(context, "", message)
                        onDismiss()
                    } else {
                        Toast.makeText(context, "يرجى كتابة الموعد الجديد البديل", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AmberGold)
            ) {
                Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("إرسال للطلاب عبر واتساب")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun SessionCancellationDialog(
    sessionWithGroup: SessionWithGroup,
    teacherName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Cancel, contentDescription = null, tint = CrimsonError)
                Text("إشعار إلغاء/اعتذار عن حصة [${sessionWithGroup.groupName}]", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "سيتم إنشاء رسالة اعتذار رسمية وإرسالها لطلاب المجموعة وأولياء الأمور.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("سبب الإلغاء أو الاعتذار") },
                    placeholder = { Text("مثال: وعكة صحية طارئة / صيانة بالسنتر") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val message = SessionNotificationHelper.generateCancellationMessage(
                        teacherName = teacherName,
                        groupName = sessionWithGroup.groupName,
                        sessionTime = sessionWithGroup.session.time,
                        sessionDate = "${sessionWithGroup.session.day} (${sessionWithGroup.session.date.ifBlank { "اليوم" }})",
                        reason = reason
                    )
                    WhatsAppHelper.openWhatsApp(context, "", message)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonError)
            ) {
                Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("إرسال تنبيه الإلغاء للطلاب")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}
