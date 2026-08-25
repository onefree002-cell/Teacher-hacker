package com.example.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entity.TeacherEntity
import com.example.data.repository.TeacherPlannerRepository
import com.example.ui.theme.*
import com.example.util.AppPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    repository: TeacherPlannerRepository,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var name by remember { mutableStateOf("مستر عبده أيمن") }
    var subject by remember { mutableStateOf("الرياضيات") }
    var title by remember { mutableStateOf("أستاذ المادة والمشرف الأكاديمي") }
    var centerName by remember { mutableStateOf("سنتر التفوق والتميز") }
    var phone by remember { mutableStateOf("01206150946") }
    var whatsapp by remember { mutableStateOf("01206150946") }
    var telegram by remember { mutableStateOf("01206150946") }
    var autoBackupInterval by remember { mutableStateOf("daily") } // "daily", "weekly", "monthly", "on_change", "disabled"

    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val existing = repository.getTeacherSync()
            if (existing != null) {
                withContext(Dispatchers.Main) {
                    name = existing.name
                    subject = existing.subject
                    title = existing.title
                    centerName = existing.centerName
                    phone = existing.phone
                    whatsapp = existing.whatsapp
                    telegram = existing.telegram.ifEmpty { existing.phone }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        NavyPrimary,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // App Logo & Avatar Header
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A))
                    .border(3.dp, EmeraldGreenLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.hacker_app_icon_1787359847395),
                    contentDescription = "شعار هاكر التدريس",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "مرحباً بك في هاكر التدريس",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "The Hacker | المنظومة الرقمية الشاملة للمعلم الذكي",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AmberGoldLight,
                    textAlign = TextAlign.Center
                )
            }

            // Welcome Info Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "سجل بياناتك الأساسية للبدء",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        "تُستخدم هذه البيانات في طباعة كروت الطلاب وشهادات التقدير والتقارير الرسمية والتواصل مع أولياء الأمور.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Full Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم المعلم / الأستاذ") },
                        leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_input_name")
                    )

                    // Subject
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("المادة التخصصية") },
                        leadingIcon = { Icon(Icons.Filled.School, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_input_subject")
                    )

                    // Center or School Name
                    OutlinedTextField(
                        value = centerName,
                        onValueChange = { centerName = it },
                        label = { Text("اسم السنتر أو القاعة أو المدرسة") },
                        leadingIcon = { Icon(Icons.Filled.LocationCity, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Phone & WhatsApp
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("رقم الهاتف") },
                            leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = whatsapp,
                            onValueChange = { whatsapp = it },
                            label = { Text("رقم الواتساب") },
                            leadingIcon = { Icon(Icons.Filled.Chat, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Telegram Recipient for Backups
                    OutlinedTextField(
                        value = telegram,
                        onValueChange = { telegram = it },
                        label = { Text("رقم أو معرف تليجرام (للنسخ الاحتياطي)") },
                        leadingIcon = { Icon(Icons.Filled.Send, contentDescription = null) },
                        supportingText = { Text("يُستخدم لإرسال النسخ الاحتياطية تلقائياً أو يدوياً إلى حسابك على تليجرام") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_input_telegram")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Auto Backup Frequency Selector
                    Text(
                        "تكرار النسخ الاحتياطي التلقائي:",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val intervals = listOf(
                        "daily" to "يومي 📅",
                        "weekly" to "أسبوعي 🗓️",
                        "monthly" to "شهري 📆",
                        "disabled" to "معطل ❌"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        intervals.forEach { (key, label) ->
                            val isSelected = autoBackupInterval == key
                            FilterChip(
                                selected = isSelected,
                                onClick = { autoBackupInterval = key },
                                label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }

            // Action Buttons
            Button(
                onClick = {
                    if (isSaving) return@Button
                    isSaving = true
                    coroutineScope.launch {
                        val currentTeacher = repository.getTeacherSync() ?: TeacherEntity()
                        val updatedTeacher = currentTeacher.copy(
                            name = name.ifBlank { "مستر عبده أيمن" },
                            subject = subject.ifBlank { "الرياضيات" },
                            title = title.ifBlank { "أستاذ المادة" },
                            centerName = centerName.ifBlank { "سنتر التفوق" },
                            phone = phone.ifBlank { "01206150946" },
                            whatsapp = whatsapp.ifBlank { "01206150946" },
                            telegram = telegram.ifBlank { phone }
                        )
                        repository.updateTeacher(updatedTeacher)
                        AppPreferencesManager.setAutoBackupSettings(
                            interval = autoBackupInterval,
                            target = "telegram",
                            telegram = telegram.ifBlank { phone }
                        )
                        AppPreferencesManager.setFirstLaunchCompleted(true)
                        onComplete()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("onboarding_save_btn"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
            ) {
                Icon(Icons.Filled.RocketLaunch, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("حفظ البيانات وبدء الاستخدام 🚀", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            TextButton(
                onClick = {
                    AppPreferencesManager.setAutoBackupSettings(
                        interval = "daily",
                        target = "telegram",
                        telegram = phone
                    )
                    AppPreferencesManager.setFirstLaunchCompleted(true)
                    onComplete()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_skip_btn")
            ) {
                Text(
                    "تخطي والبدء فوراً (يمكنك التعديل لاحقاً من الإعدادات)",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
