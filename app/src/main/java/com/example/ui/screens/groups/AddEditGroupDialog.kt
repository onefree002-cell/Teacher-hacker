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
import com.example.data.local.entity.GroupEntity
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
    onSave: (GroupEntity) -> Unit
) {
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
    var price by remember { mutableStateOf(group?.monthlyPrice?.toString() ?: "300") }
    var sessionDays by remember { mutableStateOf(group?.sessionDays ?: "السبت والاربعاء") }
    var sessionTime by remember { mutableStateOf(group?.sessionTime ?: "16:00") }
    var durationMinutes by remember { mutableStateOf(group?.durationMinutes?.toString() ?: "90") }
    var location by remember { mutableStateOf(group?.location ?: "سنتر التفوق - قاعة 1") }
    var whatsappGroupLink by remember { mutableStateOf(group?.whatsappGroupLink ?: "") }
    var notes by remember { mutableStateOf(group?.notes ?: "") }

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
                            contentDescription = "رجوع",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (group == null) "إضافة مجموعة دراسية" else "تعديل بيانات المجموعة",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp).testTag("dialog_close_btn")
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = it.isBlank()
                    },
                    label = { Text("اسم المجموعة * (مثال: مجموعة أوائل الثانوية)") },
                    isError = nameError,
                    supportingText = { if (nameError) Text("الاسم مطلوب") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("group_name_input")
                )

                OutlinedTextField(
                    value = groupNumber,
                    onValueChange = { groupNumber = it },
                    label = { Text("كود أو رقم المجموعة (مثال: G-101)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // ==========================================
                // 1. STAGE & GRADE SELECTOR (المرحلة والصف)
                // ==========================================
                com.example.ui.components.GradeStageSelectorField(
                    selectedGrade = grade,
                    onGradeSelected = { grade = it },
                    modifier = Modifier.fillMaxWidth()
                )

                // ==========================================
                // 2. TERMS SYSTEM (نظام فصول وأترام المجموعة)
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
                        "نظام الفصول الدراسية (الأترام):",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Pair(1, "ترم واحد"),
                            Pair(2, "ترمان (فصلان)"),
                            Pair(3, "ثلاثة أترام")
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
                        label = { Text("مسمى الترم الحالي (مثال: الترم الأول 2025/2026)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick suggestions for term name
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("الترم الأول", "الترم الثاني", "الترم الثالث", "الفصل الصيفي").forEach { termSugg ->
                            SuggestionChip(
                                onClick = { currentTerm = termSugg },
                                label = { Text(termSugg, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                // Pricing Type Row
                Text("نظام المحاسبة والتسعير:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = pricingType == "monthly",
                        onClick = { pricingType = "monthly" },
                        label = { Text("اشتراك شهري") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = pricingType == "session",
                        onClick = { pricingType = "session" },
                        label = { Text("حساب بالحصة") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text(if (pricingType == "monthly") "قيمة الاشتراك الشهري (ج.م)" else "سعر الحصة الواحدة (ج.م)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("group_price_input")
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = sessionDays,
                        onValueChange = { sessionDays = it },
                        label = { Text("أيام الحصص (مثال: الأحد والثلاثاء)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(dayPresets) { preset ->
                            SuggestionChip(
                                onClick = { sessionDays = preset },
                                label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
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
                        label = "توقيت الحصة",
                        modifier = Modifier.weight(1.3f)
                    )
                    OutlinedTextField(
                        value = durationMinutes,
                        onValueChange = { durationMinutes = it },
                        label = { Text("المدة (دقيقة)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                VenueDropdownSelector(
                    selectedVenueName = location,
                    onVenueSelected = { location = it },
                    venues = venues,
                    onAddNewVenue = onAddNewVenue,
                    label = "مكان الحصة / السنتر / القاعة"
                )

                // WhatsApp Group Link Field
                OutlinedTextField(
                    value = whatsappGroupLink,
                    onValueChange = { whatsappGroupLink = it.trim() },
                    label = { Text("رابط جروب واتساب المجموعة (اختياري)") },
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
                    label = { Text("ملاحظات إضافية") },
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
                        sessionDays = sessionDays.trim(),
                        sessionTime = sessionTime.trim(),
                        durationMinutes = durationMinutes.toIntOrNull() ?: 90,
                        location = location.trim(),
                        whatsappGroupLink = whatsappGroupLink.trim(),
                        notes = notes.trim(),
                        createdAt = group?.createdAt ?: System.currentTimeMillis()
                    )
                    onSave(updated)
                    onDismiss()
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_group_button")
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (group == null) "إضافة المجموعة" else "حفظ التعديلات", fontWeight = FontWeight.Bold)
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
                Text("رجوع / إلغاء")
            }
        }
    )
}
