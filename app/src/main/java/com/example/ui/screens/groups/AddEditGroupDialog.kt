package com.example.ui.screens.groups

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.SessionEntity
import com.example.data.local.entity.VenueEntity
import com.example.ui.components.AppTimePickerField
import com.example.ui.components.VenueDropdownSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditGroupDialog(
    group: GroupEntity? = null,
    venues: List<VenueEntity> = emptyList(),
    onAddNewVenue: (VenueEntity, (VenueEntity) -> Unit) -> Unit = { v, cb -> cb(v) },
    onDismiss: () -> Unit,
    onSave: (GroupEntity) -> Unit = {},
    onSaveWithSessions: ((GroupEntity, List<SessionEntity>) -> Unit)? = null
) {
    val isArabic = com.example.util.L.isArabic()
    var name by remember { mutableStateOf(group?.name ?: "") }
    var groupNumber by remember { mutableStateOf(group?.groupNumber ?: "") }
    
    // Stage & Grade Selection
    var selectedStage by remember {
        mutableStateOf(
            group?.stage?.ifEmpty {
                when {
                    group?.grade?.contains("ابتدائي") == true -> "ابتدائي"
                    group?.grade?.contains("إعدادي") == true -> "إعدادي"
                    group?.grade?.contains("ثانوي") == true -> "ثانوي"
                    else -> "ثانوي"
                }
            } ?: "ثانوي"
        )
    }

    val primaryGrades = listOf(
        "الصف الأول الابتدائي (أولى ابتدائي)",
        "الصف الثاني الابتدائي (تانية ابتدائي)",
        "الصف الثالث الابتدائي (تالتة ابتدائي)",
        "الصف الرابع الابتدائي (رابعة ابتدائي)",
        "الصف الخامس الابتدائي (خامسة ابتدائي)",
        "الصف السادس الابتدائي (ستة ابتدائي)"
    )

    val preparatoryGrades = listOf(
        "الصف الأول الإعدادي (أولى إعدادي)",
        "الصف الثاني الإعدادي (تانية إعدادي)",
        "الصف الثالث الإعدادي (تالتة إعدادي)"
    )

    val secondaryGrades = listOf(
        "الصف الأول الثانوي (أولى ثانوي)",
        "الصف الثاني الثانوي (تانية ثانوي)",
        "الصف الثالث الثانوي (تالتة ثانوي)"
    )

    var grade by remember {
        mutableStateOf(
            group?.grade?.ifEmpty { "الصف الأول الثانوي (أولى ثانوي)" } ?: "الصف الأول الثانوي (أولى ثانوي)"
        )
    }

    var customGrade by remember { mutableStateOf(if (selectedStage == "أخرى") group?.grade ?: "" else "") }

    // Terms System
    var termsCount by remember { mutableStateOf(group?.termsCount ?: 2) } // 1, 2, 3
    var currentTerm by remember { mutableStateOf(group?.currentTerm?.ifEmpty { "الترم الأول" } ?: "الترم الأول") }

    var pricingType by remember { mutableStateOf(group?.pricingType ?: "monthly") } // monthly or session
    var price by remember { mutableStateOf(group?.monthlyPrice?.toString()?.removeSuffix(".0") ?: "300") }
    var sessionDays by remember { mutableStateOf(group?.sessionDays ?: "السبت والاربعاء") }
    var sessionTime by remember { mutableStateOf(group?.sessionTime ?: "16:00") }
    var durationMinutes by remember { mutableStateOf(group?.durationMinutes?.toString() ?: "90") }
    var location by remember { mutableStateOf(group?.location ?: "سنتر التفوق - قاعة 1") }
    var whatsappGroupLink by remember { mutableStateOf(group?.whatsappGroupLink ?: "") }
    var notes by remember { mutableStateOf(group?.notes ?: "") }

    // Direct Session Creation Inside Group
    var autoCreateSessions by remember { mutableStateOf(group == null) }
    val allWeekDays = listOf("السبت", "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة")
    
    // Parse initial selected days
    var selectedDaysSet by remember {
        mutableStateOf(
            allWeekDays.filter { day -> sessionDays.contains(day) }.toSet().ifEmpty { setOf("السبت", "الأربعاء") }
        )
    }

    val dayPresets = listOf(
        "السبت والثلاثاء",
        "الأحد والأربعاء",
        "الإثنين والخميس",
        "الجمعة",
        "السبت والاثنين والاربعاء",
        "الأحد والثلاثاء والخميس"
    )

    var nameError by remember { mutableStateOf(false) }

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
                        modifier = Modifier.size(32.dp).testTag("dialog_back_btn")
                    ) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = if (isArabic) "رجوع" else "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (group == null) {
                            if (isArabic) "إضافة مجموعة دراسية" else "Add New Group"
                        } else {
                            if (isArabic) "تعديل بيانات المجموعة" else "Edit Group Details"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp).testTag("dialog_close_btn")
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = if (isArabic) "إغلاق" else "Close",
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = it.isBlank()
                    },
                    label = { Text(if (isArabic) "اسم المجموعة * (مثال: مجموعة أوائل الثانوية)" else "Group Name * (e.g. Grade 10 Advanced)") },
                    isError = nameError,
                    supportingText = { if (nameError) Text(if (isArabic) "الاسم مطلوب" else "Name is required") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("group_name_input")
                )

                OutlinedTextField(
                    value = groupNumber,
                    onValueChange = { groupNumber = it },
                    label = { Text(if (isArabic) "كود أو رقم المجموعة (مثال: G-101)" else "Group Code / ID (e.g. G-101)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // ==========================================
                // 1. STAGE & GRADE SELECTOR
                // ==========================================
                com.example.ui.components.GradeStageSelectorField(
                    selectedGrade = grade,
                    onGradeSelected = { grade = it },
                    modifier = Modifier.fillMaxWidth()
                )

                // ==========================================
                // 2. TERMS SYSTEM
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        if (isArabic) "نظام الفصول الدراسية (الأترام):" else "Academic Terms System:",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Pair(1, if (isArabic) "ترم واحد" else "1 Term"),
                            Pair(2, if (isArabic) "ترمان (فصلان)" else "2 Terms"),
                            Pair(3, if (isArabic) "ثلاثة أترام" else "3 Terms")
                        ).forEach { (count, label) ->
                            FilterChip(
                                selected = termsCount == count,
                                onClick = { termsCount = count },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = currentTerm,
                        onValueChange = { currentTerm = it },
                        label = { Text(if (isArabic) "مسمى الترم الحالي (مثال: الترم الأول 2025/2026)" else "Current Term Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick suggestions for term name
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            if (isArabic) "الترم الأول" else "Term 1",
                            if (isArabic) "الترم الثاني" else "Term 2",
                            if (isArabic) "الترم الثالث" else "Term 3",
                            if (isArabic) "الفصل الصيفي" else "Summer Term"
                        ).forEach { termSugg ->
                            SuggestionChip(
                                onClick = { currentTerm = termSugg },
                                label = { Text(termSugg, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                // Pricing Type Row
                Text(if (isArabic) "نظام المحاسبة والتسعير:" else "Billing & Pricing Model:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = pricingType == "monthly",
                        onClick = { pricingType = "monthly" },
                        label = { Text(if (isArabic) "اشتراك شهري" else "Monthly") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = pricingType == "session",
                        onClick = { pricingType = "session" },
                        label = { Text(if (isArabic) "حساب بالحصة" else "Per Session") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text(if (pricingType == "monthly") (if (isArabic) "قيمة الاشتراك الشهري (ج.م)" else "Monthly Fee") else (if (isArabic) "سعر الحصة الواحدة (ج.م)" else "Session Fee")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("group_price_input")
                )

                // ==========================================
                // 3. SCHEDULE & DIRECT SESSION CREATION
                // ==========================================
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isArabic) "مواعيد الحصص الأسبوعية" else "Weekly Schedule Days",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Text(
                            text = if (isArabic) "اختر أيام الحصص لتثبيتها في الجدول مباشرة:" else "Select class days to schedule automatically:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Multi-select Day Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            allWeekDays.take(4).forEach { dayName ->
                                val isSelected = selectedDaysSet.contains(dayName)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedDaysSet = if (isSelected) selectedDaysSet - dayName else selectedDaysSet + dayName
                                        sessionDays = selectedDaysSet.joinToString(" و ")
                                    },
                                    label = { Text(dayName, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            allWeekDays.drop(4).forEach { dayName ->
                                val isSelected = selectedDaysSet.contains(dayName)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedDaysSet = if (isSelected) selectedDaysSet - dayName else selectedDaysSet + dayName
                                        sessionDays = selectedDaysSet.joinToString(" و ")
                                    },
                                    label = { Text(dayName, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Presets
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(dayPresets) { preset ->
                                SuggestionChip(
                                    onClick = {
                                        sessionDays = preset
                                        selectedDaysSet = allWeekDays.filter { preset.contains(it) }.toSet()
                                    },
                                    label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppTimePickerField(
                                value = sessionTime,
                                onValueChange = { sessionTime = it },
                                label = if (isArabic) "توقيت الحصة" else "Class Time",
                                modifier = Modifier.weight(1.3f)
                            )
                            OutlinedTextField(
                                value = durationMinutes,
                                onValueChange = { durationMinutes = it.filter { ch -> ch.isDigit() } },
                                label = { Text(if (isArabic) "المدة (دقيقة)" else "Duration (min)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        VenueDropdownSelector(
                            selectedVenueName = location,
                            onVenueSelected = { location = it },
                            venues = venues,
                            onAddNewVenue = onAddNewVenue,
                            label = if (isArabic) "مكان الحصة / السنتر / القاعة" else "Class Venue / Room"
                        )

                        // Auto-create sessions toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { autoCreateSessions = !autoCreateSessions }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = autoCreateSessions,
                                onCheckedChange = { autoCreateSessions = it }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isArabic) "إنشاء وتثبيت الحصص فوراً في جدول المواعيد" else "Auto-create weekly sessions in schedule",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // WhatsApp Group Link Field
                OutlinedTextField(
                    value = whatsappGroupLink,
                    onValueChange = { whatsappGroupLink = it.trim() },
                    label = { Text(if (isArabic) "رابط جروب واتساب المجموعة (اختياري)" else "Group WhatsApp Link (Optional)") },
                    placeholder = { Text("https://chat.whatsapp.com/...") },
                    leadingIcon = {
                        Icon(Icons.Filled.Link, contentDescription = null, tint = com.example.ui.theme.EmeraldSuccess)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("group_whatsapp_link_input")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(if (isArabic) "ملاحظات إضافية" else "Additional Notes") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    val finalGrade = if (selectedStage == "أخرى") customGrade.trim().ifEmpty { "عام" } else grade.trim()
                    val daysText = if (selectedDaysSet.isNotEmpty()) selectedDaysSet.joinToString(" و ") else sessionDays.trim()
                    val updated = GroupEntity(
                        id = group?.id ?: 0L,
                        name = name.trim(),
                        groupNumber = groupNumber.trim(),
                        stage = selectedStage,
                        grade = finalGrade,
                        termsCount = termsCount,
                        currentTerm = currentTerm.trim().ifEmpty { "الترم الأول" },
                        pricingType = pricingType,
                        monthlyPrice = price.toDoubleOrNull() ?: 0.0,
                        sessionDays = daysText,
                        sessionTime = sessionTime.trim(),
                        durationMinutes = durationMinutes.toIntOrNull() ?: 90,
                        location = location.trim(),
                        whatsappGroupLink = whatsappGroupLink.trim(),
                        notes = notes.trim(),
                        createdAt = group?.createdAt ?: System.currentTimeMillis()
                    )

                    val targetDays = if (selectedDaysSet.isNotEmpty()) selectedDaysSet.toList() else sessionDays.split(" و ", "،", ",").map { it.trim() }.filter { it.isNotEmpty() }
                    val generatedSessions = if (autoCreateSessions) {
                        targetDays.map { dayName ->
                            SessionEntity(
                                groupId = group?.id ?: 0L,
                                day = dayName,
                                time = sessionTime.trim().ifEmpty { "16:00" },
                                durationMinutes = durationMinutes.toIntOrNull() ?: 90,
                                location = location.trim(),
                                term = currentTerm.trim().ifEmpty { "الترم الأول" },
                                completed = false
                            )
                        }
                    } else emptyList()

                    if (onSaveWithSessions != null) {
                        onSaveWithSessions(updated, generatedSessions)
                    } else {
                        onSave(updated)
                    }
                    onDismiss()
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_group_button")
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (group == null) {
                        if (isArabic) "إضافة المجموعة والحصص" else "Add Group & Schedule"
                    } else {
                        if (isArabic) "حفظ التعديلات" else "Save Changes"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("dismiss_group_button")
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isArabic) "رجوع / إلغاء" else "Cancel")
            }
        }
    )
}

