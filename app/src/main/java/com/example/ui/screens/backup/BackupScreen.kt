package com.example.ui.screens.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppTopBar
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.util.AppPreferencesManager
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: BackupViewModel,
    onNavigateBack: () -> Unit = {},
    onNavigateHome: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val autoBackupInterval by AppPreferencesManager.autoBackupInterval.collectAsState()
    val telegramNumber by AppPreferencesManager.telegramNumber.collectAsState()
    val lastBackupTime by AppPreferencesManager.lastBackupTime.collectAsState()

    var showClearDialog by remember { mutableStateOf(false) }
    var selectedInterval by remember(autoBackupInterval) { mutableStateOf(autoBackupInterval) }
    var telegramInput by remember(telegramNumber) { mutableStateOf(telegramNumber) }

    val restoreFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.inspectBackupFile(context, it) }
    }

    val oldAppMigrationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.inspectOldAppBackupFile(context, it) }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = "النسخ الاحتياطي والترحيل",
                subtitle = "حفظ البيانات والأمان",
                onNavigateBack = onNavigateBack,
                onNavigateHome = onNavigateHome
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Telegram Instant Backup Card (Featured)
            item {
                SectionHeader(title = "حفظ النسخة الاحتياطية على تليجرام ✈️")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Send,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "إرسال النسخة لقناتك أو رسائلك المحفوظة في تليجرام",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            "احفظ نسخة احتياطية مشفرة فورية من كافة بيانات الطلاب والمجموعات والدرجات في حسابك بتليجرام لحمايتها من الضياع والوصول إليها من أي جهاز.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = telegramInput,
                            onValueChange = {
                                telegramInput = it
                                AppPreferencesManager.setTelegramNumber(it)
                            },
                            label = { Text("رقم هاتفك أو معرف تليجرام (@username)") },
                            leadingIcon = { Icon(Icons.Filled.AlternateEmail, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                viewModel.createAndSendToTelegram(context, telegramInput)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("send_backup_telegram_btn")
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("إرسال النسخة الاحتياطية إلى تليجرام الآن ✈️", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Auto-Backup Configuration Card
            item {
                val isAutoBackupEnabled = selectedInterval != "disabled"
                SectionHeader(title = "إعدادات النسخ الاحتياطي التلقائي ⚙️")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "تفعيل النسخ الاحتياطي التلقائي",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    if (isAutoBackupEnabled) "النسخ التلقائي مفعل ($selectedInterval)" else "النسخ التلقائي معطل حالياً",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isAutoBackupEnabled) EmeraldSuccess else MaterialTheme.colorScheme.error
                                )
                            }
                            Switch(
                                checked = isAutoBackupEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        selectedInterval = "daily"
                                        AppPreferencesManager.setAutoBackupSettings("daily", "telegram", telegramInput)
                                    } else {
                                        selectedInterval = "disabled"
                                        AppPreferencesManager.setAutoBackupSettings("disabled", "telegram", telegramInput)
                                    }
                                },
                                modifier = Modifier.testTag("auto_backup_toggle_switch")
                            )
                        }

                        if (!isAutoBackupEnabled) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.DoNotDisturb,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "تم إلغاء النسخ الاحتياطي التلقائي. بياناتك محفوظة محلياً على جهازك ويمكنك إعادة تفعيله أو إنشاء نسخة يدوية بأي وقت.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        } else {
                            Text(
                                "حدد معدل تكرار إنشاء النسخ الاحتياطية تلقائياً:",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val intervals = listOf(
                                "daily" to "يومي 📅",
                                "every_3_days" to "كل 3 أيام ⏱️",
                                "weekly" to "أسبوعي 🗓️",
                                "monthly" to "شهري 📆",
                                "on_change" to "مع كل تعديل ⚡"
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                intervals.take(3).forEach { (key, label) ->
                                    val isSelected = selectedInterval == key
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedInterval = key
                                            AppPreferencesManager.setAutoBackupSettings(key, "telegram", telegramInput)
                                        },
                                        label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                intervals.drop(3).forEach { (key, label) ->
                                    val isSelected = selectedInterval == key
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedInterval = key
                                            AppPreferencesManager.setAutoBackupSettings(key, "telegram", telegramInput)
                                        },
                                        label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    selectedInterval = "disabled"
                                    AppPreferencesManager.setAutoBackupSettings("disabled", "telegram", telegramInput)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth().testTag("disable_auto_backup_btn")
                            ) {
                                Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إلغاء وتعطيل النسخ الاحتياطي التلقائي 🚫", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (lastBackupTime > 0L) {
                            Text(
                                "آخر نسخة تم حفظها: ${SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.getDefault()).format(Date(lastBackupTime))}",
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldGreenLight,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // General Backup Section
            item {
                SectionHeader(title = "إنشاء ومشاركة نسخة محلية")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "احفظ كافة بياناتك في ملف آمن يمكنك مشاركته عبر الواتساب أو حفظه على Google Drive أو مجلد التحميلات.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = {
                                viewModel.createBackup(context) { file ->
                                    viewModel.shareBackup(context, file)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("create_backup_btn")
                        ) {
                            Icon(Icons.Filled.CloudUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("مشاركة ملف النسخة الاحتياطية (JSON)")
                        }
                    }
                }
            }

            // Restore Preview Box if file selected
            state.previewMetadata?.let { meta ->
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = EmeraldGreenLight),
                        modifier = Modifier.fillMaxWidth().border(1.dp, EmeraldGreen, RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "معاينة النسخة الاحتياطية المحددة",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = EmeraldGreenDark
                            )
                            Text("تاريخ النسخة: ${meta.backupDate}", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "المحتويات: ${meta.studentsCount} طالب | ${meta.groupsCount} مجموعة | ${meta.attendanceCount} سجل حضور | ${meta.examsCount} امتحان | ${meta.paymentsCount} دفعة مالية",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.restoreBackup() },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).testTag("confirm_restore_btn")
                                ) {
                                    Text("تأكيد الاستعادة الآن")
                                }
                            }
                        }
                    }
                }
            }

            // Restore from JSON
            item {
                SectionHeader(title = "استعادة البيانات من ملف")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "استرجع بياناتك المحفوظة مسبقاً من أي ملف JSON تم تصديره من التطبيق.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { restoreFileLauncher.launch("application/json") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("select_backup_file_btn")
                        ) {
                            Icon(Icons.Filled.Restore, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("اختيار ملف نسخة احتياطية لاستعادته")
                        }
                    }
                }
            }

            // Old App Migration Preview if selected
            state.oldAppPreview?.let { preview ->
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        modifier = Modifier.fillMaxWidth().border(1.dp, AmberGold, RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "معاينة بيانات النسخة السابقة (v18 HTML)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = AmberGoldDark
                            )
                            Text(
                                "تم العثور على: ${preview.studentsCount} طالب، ${preview.groupsCount} مجموعة، ${preview.attendanceCount} حضور، ${preview.examsCount} امتحان",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Button(
                                onClick = { viewModel.executeOldAppMigration() },
                                colors = ButtonDefaults.buttonColors(containerColor = AmberGoldDark),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("confirm_migration_btn")
                            ) {
                                Text("تنفيذ ترحيل البيانات الآن")
                            }
                        }
                    }
                }
            }

            // Migration from Old App
            item {
                SectionHeader(title = "استيراد وترحيل البيانات من النسخ السابقة")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "إذا كان لديك نسخة احتياطية من الإصدار السابق، يمكنك استيرادها بالكامل بنقرة زر واحدة والتحويل لقاعدة بيانات هاكر التدريس Native Room.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { oldAppMigrationLauncher.launch("*/*") },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NavySecondary),
                            modifier = Modifier.fillMaxWidth().testTag("migrate_old_app_btn")
                        ) {
                            Icon(Icons.Filled.SyncAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("استيراد وترحيل ملف الإصدار السابق")
                        }
                    }
                }
            }

            // Sample Data & Database Reset
            item {
                SectionHeader(title = "أدوات متقدمة وقاعدة البيانات")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.populateSampleData() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("populate_sample_btn")
                        ) {
                            Icon(Icons.Filled.AutoFixHigh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("توليد بيانات تجريبية واقعية (طلاب، مجموعات، درجات)")
                        }

                        Button(
                            onClick = { showClearDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("clear_all_data_btn")
                        ) {
                            Icon(Icons.Filled.DeleteForever, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("مسح كافة البيانات وإعادة ضبط المصنع")
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("تأكيد مسح كافة البيانات") },
            text = { Text("هل أنت متأكد من رغبتك في حذف كافة الطلاب والمجموعات والحصص والدرجات والماليات؟ هذا الإجراء لا يمكن التراجع عنه.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
                ) {
                    Text("نعم، امسح كل شيء")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
