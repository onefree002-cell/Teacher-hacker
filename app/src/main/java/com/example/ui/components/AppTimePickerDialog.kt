package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavyPrimaryContainer
import java.util.Locale

@Composable
fun AppTimePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "وقت الحصة",
    modifier: Modifier = Modifier,
    testTag: String = "time_picker_field"
) {
    var showDialog by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value.ifEmpty { "اضغط لتحديد الوقت" },
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            IconButton(onClick = { showDialog = true }) {
                Icon(
                    Icons.Filled.AccessTime,
                    contentDescription = "اختيار الوقت",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        modifier = modifier
            .testTag(testTag)
            .clickable { showDialog = true },
        enabled = true
    )

    if (showDialog) {
        AppTimePickerDialog(
            initialTime = value,
            onDismiss = { showDialog = false },
            onConfirm = { selectedTime ->
                onValueChange(selectedTime)
                showDialog = false
            }
        )
    }
}

@Composable
fun AppTimePickerDialog(
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    // Parse initial time e.g., "16:30" or "04:30"
    val parsedHour: Int
    val parsedMinute: Int
    val isInitPm: Boolean

    if (initialTime.contains(":")) {
        val parts = initialTime.split(":")
        val h = parts.getOrNull(0)?.filter { it.isDigit() }?.toIntOrNull() ?: 16
        val m = parts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        if (h >= 12) {
            parsedHour = if (h == 12) 12 else h - 12
            isInitPm = true
        } else {
            parsedHour = if (h == 0) 12 else h
            isInitPm = false
        }
        parsedMinute = m
    } else {
        parsedHour = 4
        parsedMinute = 0
        isInitPm = true
    }

    var selectedHour by remember { mutableIntStateOf(parsedHour.coerceIn(1, 12)) }
    var selectedMinute by remember { mutableIntStateOf(parsedMinute) }
    var isPm by remember { mutableStateOf(isInitPm) }

    // Quick presets (Common tutoring session hours)
    val presets = listOf(
        Triple(9, 0, false) to "09:00 ص",
        Triple(10, 30, false) to "10:30 ص",
        Triple(12, 0, true) to "12:00 م",
        Triple(1, 30, true) to "01:30 م",
        Triple(3, 0, true) to "03:00 م",
        Triple(4, 0, true) to "04:00 م",
        Triple(4, 30, true) to "04:30 م",
        Triple(5, 30, true) to "05:30 م",
        Triple(6, 0, true) to "06:00 م",
        Triple(7, 30, true) to "07:30 م",
        Triple(8, 0, true) to "08:00 م"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تحديد وقت الحصة والموعد",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Time Display Box with AM/PM toggle
                val h24 = if (isPm) {
                    if (selectedHour == 12) 12 else selectedHour + 12
                } else {
                    if (selectedHour == 12) 0 else selectedHour
                }
                val formattedTime24 = String.format(Locale.US, "%02d:%02d", h24, selectedMinute)
                val formattedTime12 = String.format(
                    Locale.US,
                    "%02d:%02d %s",
                    selectedHour,
                    selectedMinute,
                    if (isPm) "مساءً" else "صباحاً"
                )

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = formattedTime12,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Text(
                                text = "نظام 24 ساعة: $formattedTime24",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // AM / PM Switcher
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (!isPm) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { isPm = false }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "ص",
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isPm) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isPm) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { isPm = true }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "م",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPm) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Hours Selector (1 to 12)
                Text(
                    text = "الساعة",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items((1..12).toList()) { hour ->
                        val isSelected = selectedHour == hour
                        Box(
                            modifier = Modifier
                                .aspectRatio(1.2f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable { selectedHour = hour },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$hour",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Minutes Selector (00, 15, 30, 45)
                Text(
                    text = "الدقيقة",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0, 15, 30, 45).forEach { min ->
                        val isSelected = selectedMinute == min
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable { selectedMinute = min },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = String.format(Locale.US, ":%02d", min),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Presets Carousel
                Text(
                    text = "مواعيد وحصص شائعة (سريعة):",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))

                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(presets) { (presetData, label) ->
                        val (h, m, pm) = presetData
                        val isCurrent = selectedHour == h && selectedMinute == m && isPm == pm
                        FilterChip(
                            selected = isCurrent,
                            onClick = {
                                selectedHour = h
                                selectedMinute = m
                                isPm = pm
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(formattedTime24) },
                        modifier = Modifier.testTag("confirm_time_picker_button")
                    ) {
                        Text("تأكيد الموعد ($formattedTime24)")
                    }
                }
            }
        }
    }
}
