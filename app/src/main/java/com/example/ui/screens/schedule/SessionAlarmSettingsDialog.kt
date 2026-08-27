package com.example.ui.screens.schedule

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NavyPrimary
import com.example.util.AppPreferencesManager
import com.example.util.SchoolBellSoundManager
import com.example.util.SessionNotificationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionAlarmSettingsDialog(
    onDismiss: () -> Unit,
    onSettingsChanged: () -> Unit
) {
    val context = LocalContext.current

    var isEnabled by remember { mutableStateOf(AppPreferencesManager.sessionAlertsEnabled.value) }
    var minutesBefore by remember { mutableStateOf(AppPreferencesManager.sessionAlertMinutesBefore.value) }
    var selectedSound by remember { mutableStateOf(AppPreferencesManager.sessionAlertSound.value) }
    var isVibrationEnabled by remember { mutableStateOf(AppPreferencesManager.sessionAlertVibration.value) }

    val currentlyPlayingSound by SchoolBellSoundManager.currentlyPlayingSoundId.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            SchoolBellSoundManager.stopSound()
        }
    }

    val timingPresets = listOf(
        0 to "عند بداية الحصة فقط (0 دقيقة - بدون تنبيه مسبق)",
        5 to "قبل الحصة بـ 5 دقائق",
        10 to "قبل الحصة بـ 10 دقائق",
        15 to "قبل الحصة بـ 15 دقيقة (الافتراضي الموصى به ⭐)",
        20 to "قبل الحصة بـ 20 دقيقة",
        30 to "قبل الحصة بـ 30 دقيقة (نصف ساعة)",
        45 to "قبل الحصة بـ 45 دقيقة",
        60 to "قبل الحصة بساعة (60 دقيقة)"
    )

    var isCustomMinutes by remember {
        mutableStateOf(timingPresets.none { it.first == minutesBefore })
    }
    var customMinutesInput by remember {
        mutableStateOf(if (isCustomMinutes) minutesBefore.toString() else "15")
    }

    Dialog(
        onDismissRequest = {
            SchoolBellSoundManager.stopSound()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("session_alarm_settings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = NavyPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "تنبيهات وجرس الحصص",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "رنين جرس المدرسة وتنبيهات مواعيد المجموعات",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            SchoolBellSoundManager.stopSound()
                            onDismiss()
                        }
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "إغلاق")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Enable / Disable Switch Card
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isEnabled)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "تفعيل تنبيهات الحصص التلقائية",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "إطلاق رنين جرس الحصة وإشعار تذكيري قبل كل موعد",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isEnabled,
                                    onCheckedChange = { isEnabled = it },
                                    modifier = Modifier.testTag("toggle_session_alerts")
                                )
                            }
                        }
                    }

                    // 2. Timing options
                    item {
                        AnimatedVisibility(visible = isEnabled) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "⏰ موعد إطلاق تنبيه الحصة:",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )

                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        timingPresets.forEach { (mins, label) ->
                                            val isSelected = !isCustomMinutes && minutesBefore == mins
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent)
                                                    .clickable {
                                                        isCustomMinutes = false
                                                        minutesBefore = mins
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected = isSelected,
                                                        onClick = {
                                                            isCustomMinutes = false
                                                            minutesBefore = mins
                                                        }
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = label,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                        ),
                                                        color = if (isSelected) NavyPrimary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }

                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                        // Custom minutes option
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isCustomMinutes) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent)
                                                .clickable { isCustomMinutes = true }
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = isCustomMinutes,
                                                onClick = { isCustomMinutes = true }
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "تحديد وقت مخصص بالدقائق:",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isCustomMinutes) FontWeight.Bold else FontWeight.Normal
                                                ),
                                                color = if (isCustomMinutes) NavyPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        AnimatedVisibility(visible = isCustomMinutes) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = customMinutesInput,
                                                    onValueChange = { input ->
                                                        if (input.all { it.isDigit() } && input.length <= 3) {
                                                            customMinutesInput = input
                                                            val num = input.toIntOrNull() ?: 0
                                                            minutesBefore = num.coerceIn(0, 180)
                                                        }
                                                    },
                                                    label = { Text("عدد الدقائق قبل الحصة") },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(10.dp),
                                                    singleLine = true,
                                                    leadingIcon = { Icon(Icons.Filled.Timer, contentDescription = null, tint = NavyPrimary) }
                                                )
                                                Text(
                                                    text = if ((customMinutesInput.toIntOrNull() ?: 0) == 0) "عند البدء فقط" else "${customMinutesInput.ifBlank { "0" }} دقيقة قبل الموعد",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = NavyPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Sound Selector Section
                    item {
                        AnimatedVisibility(visible = isEnabled) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🔔 اختيار صوت ورنين التنبيه:",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    if (currentlyPlayingSound != null) {
                                        TextButton(
                                            onClick = { SchoolBellSoundManager.stopSound() },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Filled.Stop, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("إيقاف الصوت", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SchoolBellSoundManager.soundOptions.forEach { option ->
                                        val isSelected = selectedSound == option.id
                                        val isPlaying = currentlyPlayingSound == option.id

                                        // Pulsing animation when playing
                                        val infiniteTransition = rememberInfiniteTransition()
                                        val pulseScale by infiniteTransition.animateFloat(
                                            initialValue = 1f,
                                            targetValue = if (isPlaying) 1.08f else 1f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(400, easing = LinearEasing),
                                                repeatMode = RepeatMode.Reverse
                                            )
                                        )

                                        Card(
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected)
                                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                                else
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                            ),
                                            border = if (isSelected)
                                                androidx.compose.foundation.BorderStroke(1.5.dp, NavyPrimary)
                                            else null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedSound = option.id
                                                    SchoolBellSoundManager.playAlertSound(context, option.id, vibrate = false)
                                                }
                                                .testTag("sound_option_${option.id}")
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected = isSelected,
                                                        onClick = {
                                                            selectedSound = option.id
                                                            SchoolBellSoundManager.playAlertSound(context, option.id, vibrate = false)
                                                        }
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Column {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(
                                                                text = option.title,
                                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                                color = if (isSelected) NavyPrimary else MaterialTheme.colorScheme.onSurface
                                                            )
                                                            if (option.id == SchoolBellSoundManager.SOUND_SCHOOL_BELL) {
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Surface(
                                                                    color = EmeraldSuccess.copy(alpha = 0.2f),
                                                                    shape = RoundedCornerShape(6.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "الأصلي ⭐",
                                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                                        color = EmeraldSuccess,
                                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        Text(
                                                            text = option.subtitle,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }

                                                // Listen / Play Preview Button
                                                FilledTonalIconButton(
                                                    onClick = {
                                                        if (isPlaying) {
                                                            SchoolBellSoundManager.stopSound()
                                                        } else {
                                                            selectedSound = option.id
                                                            SchoolBellSoundManager.playAlertSound(context, option.id, vibrate = false)
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .scale(if (isPlaying) pulseScale else 1f),
                                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                        containerColor = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                        contentColor = if (isPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                ) {
                                                    Icon(
                                                        if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                                        contentDescription = if (isPlaying) "إيقاف" else "تجربة الصوت",
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. Vibration Switch
                    item {
                        AnimatedVisibility(visible = isEnabled) {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Vibration, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("اهتزاز الهاتف مع التنبيه", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                            Text("اهتزاز متكرر لضمان الانتباه", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Switch(
                                        checked = isVibrationEnabled,
                                        onCheckedChange = { isVibrationEnabled = it }
                                    )
                                }
                            }
                        }
                    }

                    // 5. Test Live Bell Alarm Now Button
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = NavyPrimary.copy(alpha = 0.08f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NavyPrimary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.Campaign, contentDescription = null, tint = NavyPrimary)
                                    Text(
                                        text = "تجربة إشعار ورنين جرس الحصة",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = NavyPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "اضغط للتأكد من سماع رنين جرس المدرسة وظهور الإشعار على شاشة جهازك بشكل سليم.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        val soundOpt = SchoolBellSoundManager.getSoundOption(selectedSound)
                                        SchoolBellSoundManager.playAlertSound(context, selectedSound, isVibrationEnabled)
                                        SessionNotificationHelper.showTestSessionAlarm(context, soundOpt.title)
                                        Toast.makeText(context, "🔔 جاري رنين ${soundOpt.title} وإرسال إشعار تجريبي!", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("🔔 تجربة رنين جرس الحصة الآن", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Bottom Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            SchoolBellSoundManager.stopSound()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إلغاء")
                    }

                    Button(
                        onClick = {
                            SchoolBellSoundManager.stopSound()
                            AppPreferencesManager.setSessionAlertsEnabled(isEnabled)
                            AppPreferencesManager.setSessionAlertMinutesBefore(minutesBefore)
                            AppPreferencesManager.setSessionAlertSound(selectedSound)
                            AppPreferencesManager.setSessionAlertVibration(isVibrationEnabled)

                            onSettingsChanged()
                            Toast.makeText(context, "تم حفظ إعدادات تنبيهات وجرس الحصص بنجاح 🔔", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ التفضيلات", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
