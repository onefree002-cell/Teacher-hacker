package com.example.ui.screens.certificates

import android.content.Intent
import android.graphics.BitmapFactory
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CertificatesScreen(
    viewModel: CertificatesViewModel,
    initialStudentId: Long? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var studentDropdownExpanded by remember { mutableStateOf(false) }
    var groupDropdownExpanded by remember { mutableStateOf(false) }
    var removeBgSwitch by remember { mutableStateOf(state.setting.removeLogoBackground) }

    // Smooth local state for form text inputs to prevent typing lag / resets
    var localStudentName by remember(state.selectedStudentName) { mutableStateOf(state.selectedStudentName) }
    var localSchoolName by remember(state.setting.schoolName) { mutableStateOf(state.setting.schoolName) }
    var localTitle by remember(state.setting.title) { mutableStateOf(state.setting.title) }
    var localBodyTemplate by remember(state.setting.bodyTemplate) { mutableStateOf(state.setting.bodyTemplate) }
    var localSignatureName by remember(state.setting.signatureName) { mutableStateOf(state.setting.signatureName) }
    var localSealText by remember(state.setting.sealText) { mutableStateOf(state.setting.sealText) }

    // Debounce save settings to database without interrupting typing
    LaunchedEffect(localSchoolName, localTitle, localBodyTemplate, localSignatureName, localSealText) {
        delay(400)
        val updated = state.setting.copy(
            schoolName = localSchoolName,
            title = localTitle,
            bodyTemplate = localBodyTemplate,
            signatureName = localSignatureName,
            sealText = localSealText
        )
        if (updated != state.setting) {
            viewModel.updateSetting(updated)
        }
    }

    // Logo image picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setLogoImage(context, uri, removeBgSwitch)
        }
    }

    LaunchedEffect(initialStudentId) {
        if (initialStudentId != null && initialStudentId != 0L) {
            viewModel.selectStudent(initialStudentId)
        }
    }

    val themes = listOf(
        ThemeOption("classic_gold", "الملكي الذهبي", Color(0xFFD97706), Color(0xFF1E3A8A), Color(0xFFFDFCF7)),
        ThemeOption("modern_navy", "الأزرق العصري", Color(0xFF0284C7), Color(0xFF1E3A8A), Color(0xFFFFFFFF)),
        ThemeOption("emerald_luxury", "الزمردي الراقي", Color(0xFF059669), Color(0xFFD97706), Color(0xFFF0FDF4)),
        ThemeOption("imperial_burgundy", "العنابي الإمبراطوري", Color(0xFF881337), Color(0xFFCA8A04), Color(0xFFFFF1F2)),
        ThemeOption("dark_onyx_gold", "الأسود والذهبي", Color(0xFFF59E0B), Color(0xFF111827), Color(0xFF1F2937))
    )

    val currentTheme = themes.find { it.id == state.setting.templateId } ?: themes[0]
    val isGirl = state.selectedGender == "girl" || state.selectedGender == "female"

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "شهادات التقدير والتكريم",
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
            // Live Certificate Preview Card (Gender-Aware)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = currentTheme.bgColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(3.dp, currentTheme.accentColor, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Logo / Emblem Preview
                        if (state.setting.logoUri != null && File(state.setting.logoUri!!).exists()) {
                            val bitmap = remember(state.setting.logoUri) {
                                BitmapFactory.decodeFile(state.setting.logoUri)
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "شعار الشهادة",
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }
                        } else {
                            Icon(
                                imageVector = when (state.setting.presetLogo) {
                                    "trophy" -> Icons.Filled.EmojiEvents
                                    "medal" -> Icons.Filled.MilitaryTech
                                    "quill" -> Icons.Filled.HistoryEdu
                                    "book" -> Icons.Filled.MenuBook
                                    else -> Icons.Filled.WorkspacePremium
                                },
                                contentDescription = null,
                                tint = currentTheme.accentColor,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = localSchoolName.ifEmpty { state.teacher?.centerName ?: "أكاديمية التفوق التعليمية" },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (currentTheme.id == "dark_onyx_gold") currentTheme.accentColor else Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = localTitle.ifEmpty { "شهادة تفوق وتقدير" },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (currentTheme.id == "dark_onyx_gold") Color.White else currentTheme.primaryColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isGirl) "تُمنح هذه الشهادة للطالبة المتميزة:" else "تُمنح هذه الشهادة للطالب المتميز:",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (currentTheme.id == "dark_onyx_gold") Color.LightGray else Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Student Name Box
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = currentTheme.accentColor.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, currentTheme.accentColor),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = localStudentName.ifEmpty { "اسم الطالب" },
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = currentTheme.accentColor,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                            )
                        }

                        if (state.selectedGroupName.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isGirl) "المقيدة بـ: ${state.selectedGroupName}" else "المقيد بـ: ${state.selectedGroupName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (currentTheme.id == "dark_onyx_gold") Color.Gray else Color.DarkGray
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val defaultPraiseText = if (isGirl) "تقديراً لجهودها المتميزة وتفوقها الدراسي والأخلاقي في المادة" else "تقديراً لجهوده المتميزة وتفوقه الدراسي والأخلاقي في المادة"
                        Text(
                            text = localBodyTemplate.ifEmpty { defaultPraiseText },
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = if (currentTheme.id == "dark_onyx_gold") Color.LightGray else Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ختم الاعتماد: ${localSealText.ifEmpty { "معتمد" }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = currentTheme.accentColor
                            )
                            Text(
                                text = "التوقيع: ${localSignatureName.ifEmpty { state.teacher?.name ?: "أستاذ المادة" }}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = if (currentTheme.id == "dark_onyx_gold") Color.White else currentTheme.primaryColor
                            )
                        }
                    }
                }
            }

            // Student Selection & Gender Card
            item {
                SectionHeader(title = "بيانات الطالب والنوع (ولد / بنت)")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Gender Selector Buttons (ولد / بنت)
                        Text(
                            text = "نوع الطالب (لصياغة الخطاب المؤنث أو المذكر):",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilterChip(
                                selected = !isGirl,
                                onClick = { viewModel.setGender("boy") },
                                label = { Text("👦 طالب (مذكر)") },
                                leadingIcon = {
                                    if (!isGirl) Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                modifier = Modifier.weight(1f).testTag("gender_boy_cert")
                            )
                            FilterChip(
                                selected = isGirl,
                                onClick = { viewModel.setGender("girl") },
                                label = { Text("👧 طالبة (مؤنث)") },
                                leadingIcon = {
                                    if (isGirl) Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                modifier = Modifier.weight(1f).testTag("gender_girl_cert")
                            )
                        }

                        Divider(color = Color.LightGray.copy(alpha = 0.4f))

                        // Student Name Editable Field
                        OutlinedTextField(
                            value = localStudentName,
                            onValueChange = {
                                localStudentName = it
                                viewModel.setStudentName(it)
                            },
                            label = { Text("اسم الطالب المكرم (يمكن كتابته مباشرة أو اختياره من القائمة)") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { studentDropdownExpanded = !studentDropdownExpanded }) {
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = "قائمة الطلاب")
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("cert_student_name_input")
                        )

                        // Quick Pick Student Dropdown
                        if (studentDropdownExpanded && state.students.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)
                            ) {
                                LazyColumn {
                                    items(state.students) { s ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(s.name, fontWeight = FontWeight.SemiBold)
                                                    Text(
                                                        if (s.gender == "girl") "👧 طالبة" else "👦 طالب",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.Gray
                                                    )
                                                }
                                            },
                                            onClick = {
                                                viewModel.selectStudent(s.id)
                                                localStudentName = s.name
                                                studentDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Theme Selector Carousel
            item {
                SectionHeader(title = "اختر ثيم وتصميم الشهادة (5 ثيمات فاخرة)")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(themes) { th ->
                        val isSelected = state.setting.templateId == th.id
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = th.bgColor),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) NavyPrimary else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier
                                .width(130.dp)
                                .clickable { viewModel.setThemeTemplate(th.id) }
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(th.primaryColor))
                                    Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(th.accentColor))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = th.name,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                    color = if (th.id == "dark_onyx_gold") Color.White else Color.Black,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Logo & Emblem Customization + Background Remover
            item {
                SectionHeader(title = "شعار الشهادة وحذف الخلفية")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "إضافة لوجو المدرس / السنتر:",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )

                        // Upload buttons & Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { photoPickerLauncher.launch("image/*") },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                modifier = Modifier.weight(1f).testTag("pick_logo_btn")
                            ) {
                                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("اختر لوجو من المعرض")
                            }

                            if (state.setting.logoUri != null) {
                                OutlinedButton(
                                    onClick = { viewModel.clearLogo() },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonError)
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("حذف")
                                }
                            }
                        }

                        // Remove Background Toggle Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("حذف خلفية الشعار تلقائياً (شفاف)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                Text("تفريغ وإزالة الخلفية البيضاء أو الملونة لتظهر الشهادة باحترافية", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                            Switch(
                                checked = removeBgSwitch,
                                onCheckedChange = {
                                    removeBgSwitch = it
                                    viewModel.updateSetting(state.setting.copy(removeLogoBackground = it))
                                },
                                modifier = Modifier.testTag("remove_bg_switch")
                            )
                        }

                        // Preset Emblems Row
                        Text("أو اختر شعاراً تعليمياً جاهزاً:", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val presetList = listOf(
                                Pair("crown", "تاج التفوق"),
                                Pair("trophy", "كأس المركز الأول"),
                                Pair("medal", "وسام التكريم"),
                                Pair("quill", "ريشة العلم"),
                                Pair("book", "كتاب المعرفة")
                            )
                            presetList.forEach { (key, label) ->
                                val isSelected = state.setting.presetLogo == key && state.setting.logoUri == null
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setPresetLogo(key) },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }
            }

            // Certificate Details Customizer
            item {
                SectionHeader(title = "تخصيص نصوص وتوقيع الشهادة")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = localSchoolName,
                            onValueChange = { localSchoolName = it },
                            label = { Text("اسم السنتر / الأكاديمية أعلى الشهادة") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = localTitle,
                            onValueChange = { localTitle = it },
                            label = { Text("عنوان الشهادة الرئيسي") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("cert_title_input")
                        )

                        OutlinedTextField(
                            value = localBodyTemplate,
                            onValueChange = { localBodyTemplate = it },
                            label = { Text("نص الثناء والتقدير") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth().testTag("cert_body_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = localSignatureName,
                                onValueChange = { localSignatureName = it },
                                label = { Text("اسم المدرس") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("cert_signature_input")
                            )
                            OutlinedTextField(
                                value = localSealText,
                                onValueChange = { localSealText = it },
                                label = { Text("نص الختم") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Single Export Button
            item {
                Button(
                    onClick = {
                        // Ensure settings are synced before export
                        viewModel.updateSetting(
                            state.setting.copy(
                                schoolName = localSchoolName,
                                title = localTitle,
                                bodyTemplate = localBodyTemplate,
                                signatureName = localSignatureName,
                                sealText = localSealText
                            )
                        )
                        viewModel.setStudentName(localStudentName)
                        viewModel.generateAndShareCertificate(context)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("تم إنشاء وحفظ شهادة التقدير بنجاح")
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberGoldDark),
                    modifier = Modifier.fillMaxWidth().testTag("generate_cert_btn")
                ) {
                    Icon(Icons.Filled.WorkspacePremium, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إنشاء وتحميل شهادة هذا الطالب PDF")
                }
            }

            // Batch Group Printing Button
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldSuccessContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "طباعة شهادات التقدير للمجموعة كاملة دفعة واحدة",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldSuccess
                        )
                        Text(
                            text = "توليد ملف PDF مجمع يحتوي على شهادات لكل طلاب المجموعة المحددة بنقرة واحدة لتوفير الوقت والطباعة السريعة بمراعاة نوع كل طالب.",
                            style = MaterialTheme.typography.bodySmall
                        )

                        if (state.groups.isNotEmpty()) {
                            ExposedDropdownMenuBox(
                                expanded = groupDropdownExpanded,
                                onExpandedChange = { groupDropdownExpanded = it }
                            ) {
                                val gName = state.groups.find { it.id == state.selectedGroupId }?.name ?: "كل المجموعات"
                                OutlinedTextField(
                                    value = gName,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("اختر المجموعة لتوليد شهاداتها") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupDropdownExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = groupDropdownExpanded,
                                    onDismissRequest = { groupDropdownExpanded = false }
                                ) {
                                    state.groups.forEach { g ->
                                        DropdownMenuItem(
                                            text = { Text(g.name) },
                                            onClick = {
                                                viewModel.selectGroupForBatch(g.id)
                                                groupDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.generateBatchCertificatesForGroup(context, state.selectedGroupId) { file ->
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("تم إنشاء ملف الشهادات المجمع بنجاح")
                                    }
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "مشاركة وطباعة الشهادات المجمعة"))
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                            modifier = Modifier.fillMaxWidth().testTag("batch_cert_btn")
                        ) {
                            Icon(Icons.Filled.Print, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("طباعة كروت وشهادات المجموعة دفعة واحدة (PDF)")
                        }
                    }
                }
            }
        }
    }
}

private data class ThemeOption(
    val id: String,
    val name: String,
    val primaryColor: Color,
    val accentColor: Color,
    val bgColor: Color
)
