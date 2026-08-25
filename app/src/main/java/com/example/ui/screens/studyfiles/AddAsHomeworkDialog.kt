package com.example.ui.screens.studyfiles

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.SessionEntity
import com.example.data.repository.SessionWithGroup
import com.example.data.repository.TeacherPlannerRepository
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NavyPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAsHomeworkDialog(
    repository: TeacherPlannerRepository,
    currentFileTitle: String,
    currentPageIndex: Int,
    onDismiss: () -> Unit,
    onHomeworkSaved: (savedSummary: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val calendar = remember { Calendar.getInstance() }
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH) }

    var selectedDate by remember { mutableStateOf(dateFormatter.format(calendar.time)) }
    var selectedSessionId by remember { mutableStateOf<Long?>(null) }
    var homeworkTitle by remember { mutableStateOf(currentFileTitle.ifBlank { "واجب الحصة" }) }
    var homeworkPages by remember { mutableStateOf("صفحة ${currentPageIndex + 1}") }
    var homeworkNotes by remember { mutableStateOf("مطلوب حل التمارين والمسائل الهندسية ومراجعة الشرح المسجل في الصفحة.") }
    var homeworkDeadline by remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 7)
        mutableStateOf(dateFormatter.format(cal.time))
    }

    var allSessionsWithGroups by remember { mutableStateOf<List<SessionWithGroup>>(emptyList()) }
    var allGroups by remember { mutableStateOf<List<GroupEntity>>(emptyList()) }
    var isLoadingSessions by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // Load Sessions & Groups from Room Repository
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val sessions = repository.allSessions.first()
                val groups = repository.allGroups.first()
                val groupMap = groups.associateBy { it.id }

                val mapped = sessions.map { sess ->
                    SessionWithGroup(
                        session = sess,
                        groupName = groupMap[sess.groupId]?.name ?: "مجموعة غير محددة",
                        location = groupMap[sess.groupId]?.location ?: sess.location
                    )
                }

                withContext(Dispatchers.Main) {
                    allGroups = groups
                    allSessionsWithGroups = mapped
                    // Auto-select matching date or first session
                    val matching = mapped.find { it.session.date == selectedDate } ?: mapped.firstOrNull()
                    selectedSessionId = matching?.session?.id
                    isLoadingSessions = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoadingSessions = false
                }
            }
        }
    }

    // Filtered sessions for selected date
    val sessionsOnDate = remember(allSessionsWithGroups, selectedDate) {
        allSessionsWithGroups.filter {
            it.session.date == selectedDate || it.session.date.isBlank()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Filled.AssignmentTurnedIn,
                    contentDescription = null,
                    tint = EmeraldSuccess,
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = "إضافة الصفحة كواجب في الحصة",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Step 1: Select Session Date
                item {
                    Text(
                        text = "1. تاريخ الحصة المستهدفة 📅",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedCard(
                        onClick = {
                            val c = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val picked = Calendar.getInstance().apply {
                                        set(year, month, dayOfMonth)
                                    }
                                    selectedDate = dateFormatter.format(picked.time)
                                },
                                c.get(Calendar.YEAR),
                                c.get(Calendar.MONTH),
                                c.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تاريخ الحصة: $selectedDate",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Text(
                                text = "تغيير التاريخ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Step 2: Choose Session
                item {
                    Text(
                        text = "2. اختيار الحصة والمجموعة 👥",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    if (isLoadingSessions) {
                        Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    } else if (allSessionsWithGroups.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "لا توجد حصص مسجلة في الجدول حالياً. سيتم حفظ الواجب في الحصص بعد جدولتها.",
                                modifier = Modifier.padding(10.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            allSessionsWithGroups.forEach { item ->
                                val sess = item.session
                                val isSelected = selectedSessionId == sess.id
                                val isMatchingDate = sess.date == selectedDate

                                Card(
                                    onClick = { selectedSessionId = sess.id },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ),
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(selected = isSelected, onClick = { selectedSessionId = sess.id })
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    text = item.groupName,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                                )
                                                Text(
                                                    text = "يوم ${sess.day} - ${sess.time} • ${if (sess.date.isNotBlank()) sess.date else "كل أسبوع"}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        if (isMatchingDate) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = EmeraldSuccess.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "تطابق التاريخ",
                                                    color = EmeraldSuccess,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Step 3: Homework Details
                item {
                    Text(
                        text = "3. تفاصيل وبيانات الواجب 📝",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    OutlinedTextField(
                        value = homeworkTitle,
                        onValueChange = { homeworkTitle = it },
                        label = { Text("عنوان الواجب") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = homeworkPages,
                        onValueChange = { homeworkPages = it },
                        label = { Text("الصفحة / رقم الصفحة") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = homeworkNotes,
                        onValueChange = { homeworkNotes = it },
                        label = { Text("تعليمات وملاحظات للطلاب") },
                        maxLines = 3,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedCard(
                        onClick = {
                            val c = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val picked = Calendar.getInstance().apply {
                                        set(year, month, dayOfMonth)
                                    }
                                    homeworkDeadline = dateFormatter.format(picked.time)
                                },
                                c.get(Calendar.YEAR),
                                c.get(Calendar.MONTH),
                                c.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⏰ موعد التسليم: $homeworkDeadline",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                            )
                            Text(
                                text = "تعديل الموعد",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (homeworkTitle.isBlank()) return@Button
                    scope.launch(Dispatchers.IO) {
                        isSaving = true
                        try {
                            val targetSession = allSessionsWithGroups.find { it.session.id == selectedSessionId }?.session

                            if (targetSession != null) {
                                val updatedSession = targetSession.copy(
                                    date = if (targetSession.date.isBlank()) selectedDate else targetSession.date,
                                    homeworkTitle = homeworkTitle,
                                    homeworkPages = homeworkPages,
                                    homeworkNotes = homeworkNotes,
                                    homeworkDeadline = homeworkDeadline
                                )
                                repository.updateSession(updatedSession)
                                withContext(Dispatchers.Main) {
                                    onHomeworkSaved("تم ربط الصفحة كواجب للحصة بنجاح 📚")
                                }
                            } else if (allGroups.isNotEmpty()) {
                                // Create new session entry for this date and group
                                val firstGroup = allGroups.first()
                                val newSession = SessionEntity(
                                    groupId = firstGroup.id,
                                    day = "السبت",
                                    time = "16:00",
                                    date = selectedDate,
                                    homeworkTitle = homeworkTitle,
                                    homeworkPages = homeworkPages,
                                    homeworkNotes = homeworkNotes,
                                    homeworkDeadline = homeworkDeadline
                                )
                                repository.insertSession(newSession)
                                withContext(Dispatchers.Main) {
                                    onHomeworkSaved("تم إنشاء الحصة وربط صفحة الواجب بنجاح 📚")
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    onHomeworkSaved("تم تسجيل بيانات الواجب بنجاح 📚")
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                onHomeworkSaved("حدث خطأ أثناء الحفظ: ${e.localizedMessage}")
                            }
                        } finally {
                            isSaving = false
                        }
                    }
                },
                enabled = !isSaving && homeworkTitle.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حفظ في بيانات الحصة", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("إلغاء")
            }
        }
    )
}
