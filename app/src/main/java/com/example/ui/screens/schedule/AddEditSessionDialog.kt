package com.example.ui.screens.schedule

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.SessionEntity
import com.example.data.local.entity.VenueEntity
import com.example.ui.components.AppTimePickerField
import com.example.ui.components.VenueDropdownSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSessionDialog(
    session: SessionEntity? = null,
    initialDay: String? = null,
    initialDate: String? = null,
    groups: List<GroupEntity>,
    venues: List<VenueEntity> = emptyList(),
    onAddNewVenue: (VenueEntity, (VenueEntity) -> Unit) -> Unit = { v, cb -> cb(v) },
    onDismiss: () -> Unit,
    onSave: (SessionEntity, (String) -> Unit) -> Unit
) {
    var selectedGroupId by remember { mutableStateOf(session?.groupId ?: (groups.firstOrNull()?.id ?: 0L)) }
    var day by remember { mutableStateOf(session?.day ?: initialDay ?: "السبت") }
    var date by remember { mutableStateOf(session?.date ?: initialDate ?: "") }
    var time by remember { mutableStateOf(session?.time ?: "16:00") }
    var durationMinutes by remember { mutableStateOf(session?.durationMinutes?.toString() ?: "90") }
    var location by remember { mutableStateOf(session?.location ?: (groups.firstOrNull { it.id == selectedGroupId }?.location ?: "")) }
    var note by remember { mutableStateOf(session?.note ?: "") }
    var homeworkTitle by remember { mutableStateOf(session?.homeworkTitle ?: "") }
    var homeworkPages by remember { mutableStateOf(session?.homeworkPages ?: "") }
    var homeworkDeadline by remember { mutableStateOf(session?.homeworkDeadline ?: "") }
    var homeworkNotes by remember { mutableStateOf(session?.homeworkNotes ?: "") }

    var groupDropdownExpanded by remember { mutableStateOf(false) }
    var dayDropdownExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val daysOfWeek = listOf("السبت", "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp).testTag("session_dialog_back_btn")
                    ) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (session == null) "إضافة حصة دراسية" else "تعديل موعد الحصة",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "إغلاق",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Group selector
                ExposedDropdownMenuBox(
                    expanded = groupDropdownExpanded,
                    onExpandedChange = { groupDropdownExpanded = it }
                ) {
                    val currentGroupName = groups.firstOrNull { it.id == selectedGroupId }?.name ?: "اختر المجموعة"
                    OutlinedTextField(
                        value = currentGroupName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("المجموعة") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth().testTag("session_group_selector")
                    )
                    ExposedDropdownMenu(
                        expanded = groupDropdownExpanded,
                        onDismissRequest = { groupDropdownExpanded = false }
                    ) {
                        groups.forEach { g ->
                            DropdownMenuItem(
                                text = { Text(g.name) },
                                onClick = {
                                    selectedGroupId = g.id
                                    if (location.isBlank()) location = g.location
                                    groupDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Day selector
                ExposedDropdownMenuBox(
                    expanded = dayDropdownExpanded,
                    onExpandedChange = { dayDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = day,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("اليوم") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = dayDropdownExpanded,
                        onDismissRequest = { dayDropdownExpanded = false }
                    ) {
                        daysOfWeek.forEach { d ->
                            DropdownMenuItem(
                                text = { Text(d) },
                                onClick = {
                                    day = d
                                    dayDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Time and Duration with enhanced picker
                AppTimePickerField(
                    value = time,
                    onValueChange = { time = it },
                    label = "موعد الحصة (اضغط للاختيار)",
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "session_time_input"
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = durationMinutes,
                            onValueChange = { durationMinutes = it },
                            label = { Text("المدة (دقيقة)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            listOf("60" to "ساعة", "90" to "ساعة ونصف", "120" to "ساعتان").forEach { (min, label) ->
                                FilterChip(
                                    selected = durationMinutes == min,
                                    onClick = { durationMinutes = min },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.padding(0.dp)
                                )
                            }
                        }
                    }
                }

                // Venue selector with inline add new venue
                VenueDropdownSelector(
                    selectedVenueName = location,
                    onVenueSelected = { location = it },
                    venues = venues,
                    onAddNewVenue = onAddNewVenue,
                    label = "المكان / السنتر / القاعة"
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("ملاحظات عامة عن الحصة") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                // Dedicated Homework Section Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "📚 تحديد واجب الحصة (Homework)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        OutlinedTextField(
                            value = homeworkTitle,
                            onValueChange = { homeworkTitle = it },
                            label = { Text("عنوان موضوع الواجب (مثال: تمارين الدرس الأول)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("session_homework_title_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = homeworkPages,
                                onValueChange = { homeworkPages = it },
                                label = { Text("الصفحات / أرقام المسائل") },
                                placeholder = { Text("ص 45 ت (1 إلى 8)") },
                                singleLine = true,
                                modifier = Modifier.weight(1.2f).testTag("session_homework_pages_input")
                            )

                            OutlinedTextField(
                                value = homeworkDeadline,
                                onValueChange = { homeworkDeadline = it },
                                label = { Text("موعد التسليم") },
                                placeholder = { Text("الحصة القادمة") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("session_homework_deadline_input")
                            )
                        }

                        OutlinedTextField(
                            value = homeworkNotes,
                            onValueChange = { homeworkNotes = it },
                            label = { Text("تعليمات خاصة بالواجب") },
                            placeholder = { Text("مثال: كتابة خطوات الحل كاملة في كشكول الواجب") },
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s = SessionEntity(
                        id = session?.id ?: 0L,
                        groupId = selectedGroupId,
                        day = day,
                        time = time.trim(),
                        date = "",
                        durationMinutes = durationMinutes.toIntOrNull() ?: 90,
                        location = location.trim(),
                        completed = session?.completed ?: false,
                        homeworkTitle = homeworkTitle.trim(),
                        homeworkPages = homeworkPages.trim(),
                        homeworkDeadline = homeworkDeadline.trim(),
                        homeworkNotes = homeworkNotes.trim(),
                        note = note.trim()
                    )
                    onSave(s) { err ->
                        errorMessage = err
                    }
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_session_button")
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (session == null) "إضافة الحصة" else "حفظ التعديل", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("dismiss_session_button")
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("رجوع / إلغاء")
            }
        }
    )
}
