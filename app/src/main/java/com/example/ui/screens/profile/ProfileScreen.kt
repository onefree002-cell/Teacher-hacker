package com.example.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.updateField(logoUri = uri.toString())
        }
    }

    var showShareDialog by remember { mutableStateOf(false) }

    if (showShareDialog) {
        com.example.ui.components.TeacherProfileShareDialog(
            teacher = state.teacher,
            onDismiss = { showShareDialog = false }
        )
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            snackbarHostState.showSnackbar("تم حفظ بيانات المعلم وإعدادات المطبوعات بنجاح")
            viewModel.resetSaved()
        }
    }

    LaunchedEffect(state.logoStatusMessage) {
        state.logoStatusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearLogoStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "الملف التعريفي للمعلم والسنتر",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            // Profile Header Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Surface(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape)
                                    .clickable { imagePickerLauncher.launch("image/*") },
                                shape = CircleShape,
                                color = NavyPrimary.copy(alpha = 0.1f),
                                border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                if (!state.teacher.logoUri.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = state.teacher.logoUri,
                                        contentDescription = "لوجو المعلم / السنتر",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Filled.Person,
                                            contentDescription = null,
                                            tint = NavyPrimary,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                }
                            }

                            FilledIconButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.PhotoCamera, contentDescription = "تغيير اللوجو", modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = state.teacher.name.ifEmpty { "عبده أيمن" },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${state.teacher.title.ifEmpty { "أستاذ المادة والمشرف الأكاديمي" }} • ${state.teacher.subject.ifEmpty { "جميع المواد" }}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = state.teacher.centerName.ifEmpty { "سنتر التفوق والتميز" },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Large Share Portfolio Button
                        FilledTonalButton(
                            onClick = { showShareDialog = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFFFEF3C7),
                                contentColor = Color(0xFF92400E)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("share_teacher_portfolio_btn")
                        ) {
                            Icon(Icons.Filled.Badge, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "مشاركة كارت وبورتفوليو المعلم (PDF / كروت / واتساب)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // ==========================================
            // TEACHER PORTFOLIO & BIO (الملف التعريفي والنبذة)
            // ==========================================
            item {
                SectionHeader(title = "البورتفوليو والنبذة الأكاديمية (للمشاركة)")
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
                        OutlinedTextField(
                            value = state.teacher.title,
                            onValueChange = { viewModel.updateField(title = it) },
                            label = { Text("المسمى الأكاديمي / الوصف") },
                            placeholder = { Text("مثال: خبير أول المادة والمشرف التربوي") },
                            leadingIcon = { Icon(Icons.Filled.WorkspacePremium, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("teacher_title_input")
                        )

                        OutlinedTextField(
                            value = state.teacher.experienceYears,
                            onValueChange = { viewModel.updateField(experienceYears = it) },
                            label = { Text("سنوات الخبرة") },
                            placeholder = { Text("مثال: خبرة 12 عاماً في تدريس الثانوية العامة") },
                            leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("teacher_experience_input")
                        )

                        OutlinedTextField(
                            value = state.teacher.degrees,
                            onValueChange = { viewModel.updateField(degrees = it) },
                            label = { Text("المؤهلات والشهادات العلمية") },
                            placeholder = { Text("مثال: ليسانس آداب وتربية ودبلوم طرق تدريس") },
                            leadingIcon = { Icon(Icons.Filled.School, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("teacher_degrees_input")
                        )

                        OutlinedTextField(
                            value = state.teacher.stagesTaught,
                            onValueChange = { viewModel.updateField(stagesTaught = it) },
                            label = { Text("المراحل والمناهج الدراسية") },
                            placeholder = { Text("مثال: المرحلة الثانوية والإعدادية (عام ولغات)") },
                            leadingIcon = { Icon(Icons.Filled.Class, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("teacher_stages_input")
                        )

                        OutlinedTextField(
                            value = state.teacher.bio,
                            onValueChange = { viewModel.updateField(bio = it) },
                            label = { Text("النبذة التعريفية ورسالة المعلم") },
                            placeholder = { Text("اكتب نبذة مختصرة عن أسلوبك وطريقتك في الشرح والتبسيط...") },
                            leadingIcon = { Icon(Icons.Filled.FormatQuote, contentDescription = null) },
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth().testTag("teacher_bio_input")
                        )

                        OutlinedTextField(
                            value = state.teacher.teachingFeatures,
                            onValueChange = { viewModel.updateField(teachingFeatures = it) },
                            label = { Text("مميزات نظام الشرح والمتابعة") },
                            placeholder = { Text("مثال: متابعة دورية • بنك أسئلة شامل • تقارير ولي أمر") },
                            leadingIcon = { Icon(Icons.Filled.CheckCircleOutline, contentDescription = null) },
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth().testTag("teacher_features_input")
                        )
                    }
                }
            }

            // ==========================================
            // LOGO & PRINTOUT SETTINGS (شعار المعلم والمطبوعات)
            // ==========================================
            item {
                SectionHeader(title = "شعار المعلم والمطبوعات")
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "إدراج اللوجو في جميع المطبوعات",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    "يظهر الشعار في التقارير الشهرية، الشهادات، كشوف الدرجات وبطاقات الطلاب",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = state.teacher.showLogoInPrintouts,
                                onCheckedChange = { viewModel.updateField(showLogoInPrintouts = it) }
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (state.teacher.logoUri != null) "تغيير اللوجو" else "رفع لوجو المعلم / السنتر")
                            }

                            if (state.teacher.logoUri != null) {
                                IconButton(
                                    onClick = { viewModel.updateField(logoUri = "") }
                                ) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف اللوجو", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        if (!state.teacher.logoUri.isNullOrEmpty()) {
                            FilledTonalButton(
                                onClick = { viewModel.removeLogoBackground(context) },
                                enabled = !state.isProcessingLogo,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = AmberGoldLight,
                                    contentColor = AmberGoldDark
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("remove_logo_bg_btn")
                            ) {
                                if (state.isProcessingLogo) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = AmberGoldDark)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("جارِ حذف وتفريغ الخلفية...", style = MaterialTheme.typography.bodyMedium)
                                } else {
                                    Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("حذف وتفريغ خلفية اللوجو (جعلها شفافة)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }
            }

            // Teacher Info Inputs
            item {
                SectionHeader(title = "المعلومات الأساسية")
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
                        OutlinedTextField(
                            value = state.teacher.name,
                            onValueChange = { viewModel.updateField(name = it) },
                            label = { Text("الاسم بالكامل (الأستاذ)") },
                            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("teacher_name_input")
                        )

                        OutlinedTextField(
                            value = state.teacher.phone,
                            onValueChange = { viewModel.updateField(phone = it) },
                            label = { Text("رقم هاتف التواصل") },
                            leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("teacher_phone_input")
                        )

                        OutlinedTextField(
                            value = state.teacher.whatsapp,
                            onValueChange = { viewModel.updateField(whatsapp = it) },
                            label = { Text("رقم الواتساب (لإرسال التقارير والرسائل)") },
                            leadingIcon = { Icon(Icons.Filled.Chat, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("teacher_whatsapp_input")
                        )

                        OutlinedTextField(
                            value = state.teacher.subject,
                            onValueChange = { viewModel.updateField(subject = it) },
                            label = { Text("المادة أو التخصص التدريسي") },
                            leadingIcon = { Icon(Icons.Filled.Book, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("teacher_subject_input")
                        )

                        OutlinedTextField(
                            value = state.teacher.centerName,
                            onValueChange = { viewModel.updateField(centerName = it) },
                            label = { Text("اسم السنتر / الأكاديمية / القاعة") },
                            leadingIcon = { Icon(Icons.Filled.School, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("teacher_center_input")
                        )

                        OutlinedTextField(
                            value = state.teacher.address,
                            onValueChange = { viewModel.updateField(address = it) },
                            label = { Text("عنوان المقر أو السنتر") },
                            leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("teacher_address_input")
                        )
                    }
                }
            }

            // Save Action
            item {
                Button(
                    onClick = { viewModel.saveProfile() },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("save_teacher_profile_btn")
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حفظ التغييرات في ملف المعلم")
                }
            }
        }
    }
}
