package com.example.ui.screens.studyfiles

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.StudyFileEntity
import com.example.ui.components.AppTopBar
import com.example.ui.screens.tools.MATH_EDU_URL
import com.example.ui.screens.tools.InAppMathEduBrowserDialog
import com.example.ui.theme.*
import com.example.util.StudyFileManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyFilesScreen(
    viewModel: StudyFilesViewModel,
    initialGradeFilter: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateHome: (() -> Unit)? = null,
    onOpenFileInViewer: (filePath: String, title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showAddFileDialog by remember { mutableStateOf(false) }
    var showMathEduBrowserDialog by remember { mutableStateOf(false) }
    var selectedFileToDelete by remember { mutableStateOf<StudyFileEntity?>(null) }

    LaunchedEffect(initialGradeFilter) {
        if (!initialGradeFilter.isNullOrBlank() && initialGradeFilter != "الكل") {
            viewModel.selectGrade(initialGradeFilter)
        }
    }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearStatusMessage()
        }
    }

    // System File Picker for PDF / Docs / Images
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            showAddFileDialog = true
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "كتب ومذكرات وملفات المعلم 📚",
                subtitle = if (state.selectedGrade != "الكل") state.selectedGrade else "المكتبة الدراسية الشاملة",
                onNavigateBack = onNavigateBack,
                onNavigateHome = onNavigateHome,
                showHomeButton = true,
                actions = {
                    IconButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.testTag("add_study_file_btn")
                    ) {
                        Icon(Icons.Filled.AddCircle, contentDescription = "إضافة ملف جديد", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { filePickerLauncher.launch("*/*") },
                icon = { Icon(Icons.Filled.UploadFile, contentDescription = null) },
                text = { Text("رفع ملف جديد للصف 📥", fontWeight = FontWeight.Bold) },
                containerColor = NavyPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_upload_study_file")
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar & Stats
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text("ابحث في الكتب والمذكرات وأوراق العمل...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "مسح")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("study_files_search_input")
                    )

                    // Safe App Folder Indicator
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.FolderSpecial,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مجلد التطبيق المعزول: هاكر_التدريس (يتم حفظ نسخ آمنة لجميع الكتب والمذكرات بدون تعديل الملف الأصلي)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // External Math Books Portal Card (mathedu03.eyoo.org)
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F766E).copy(alpha = 0.08f)),
                        border = BorderStroke(1.dp, Color(0xFF0D9488).copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth().testTag("math_books_portal_banner")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = Color(0xFF0D9488),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "موقع تحميل كتب ومذكرات الرياضيات الخارجية 🌐",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF0F766E)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF0D9488).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "mathedu03.eyoo.org",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF0F766E),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = "حمل كتب المعاصر والأضواء وسلاح التلميذ والشامل ومذكرات التوجيه بصيغة PDF ثم اضغط رفع لحفظها ومشاركتها في حصصك.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { showMathEduBrowserDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0D9488),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(32.dp).weight(1.2f).testTag("browse_math_edu_btn")
                                ) {
                                    Icon(Icons.Filled.Language, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تصفح وتحميل الكتب 📱", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(MATH_EDU_URL))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "تعذر فتح المتصفح", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(32.dp).weight(0.9f)
                                ) {
                                    Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("المتصفح", fontSize = 11.sp)
                                }

                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Math Edu Books URL", MATH_EDU_URL)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "تم نسخ رابط الموقع 📋", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "نسخ", tint = Color(0xFF0D9488), modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = {
                                        try {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    "📚 موقع كتب ومذكرات الرياضيات الخارجية:\n$MATH_EDU_URL"
                                                )
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة رابط موقع الرياضيات"))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "تعذر المشاركة", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.Share, contentDescription = "مشاركة", tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // Grade Filters
                    Text("اختر الصف الدراسي:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    com.example.ui.components.GradeStageFilterBar(
                        selectedGrade = state.selectedGrade,
                        onGradeSelected = { viewModel.selectGrade(it) }
                    )

                    // Category Filters
                    val categories = listOf("الكل", "كتاب الوزارة", "مذكرة الشرح", "ملخص ومراجعة", "امتحانات سابقة", "أوراق عمل ومتابعة")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { cat ->
                            val isSelected = state.selectedCategory == cat
                            SuggestionChip(
                                onClick = { viewModel.selectCategory(cat) },
                                label = { Text(cat, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            }

            // Files List or Empty State
            if (state.filteredFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.LibraryBooks, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Text(
                            "لا توجد ملفات أو كتب محفوظة لهذا الصف حالياً",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            "يمكنك رفع أي كتاب مدرسي أو ملزمة بصيغة PDF أو مستند، وستحفظ نسخة دائمة على جهازك للشرح عليها باستخدام الأدوات الهندسية الذكية.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { filePickerLauncher.launch("*/*") },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                            ) {
                                Icon(Icons.Filled.UploadFile, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("رفع ملف من الهاتف")
                            }

                            OutlinedButton(
                                onClick = {
                                    val grade = if (state.selectedGrade != "الكل") state.selectedGrade else "الصف الأول الثانوي"
                                    viewModel.generateSamplePdf(
                                        context = context,
                                        title = "مذكرة الشرح والمسائل الهندسية",
                                        grade = grade,
                                        category = "مذكرة الشرح"
                                    )
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إنشاء نموذج تجريبي")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.filteredFiles, key = { it.id }) { file ->
                        StudyFileCard(
                            file = file,
                            onOpen = {
                                onOpenFileInViewer(file.localFilePath, file.title)
                            },
                            onShare = {
                                StudyFileManager.shareFile(context, file)
                            },
                            onDelete = {
                                selectedFileToDelete = file
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }
            }
        }
    }

    // Add / Save File Metadata Dialog
    if (showAddFileDialog && selectedUri != null) {
        var fileTitle by remember { mutableStateOf("") }
        var fileGrade by remember { mutableStateOf(if (state.selectedGrade != "الكل") state.selectedGrade else "الصف الأول الثانوي") }
        var fileCategory by remember { mutableStateOf("كتاب الوزارة") }
        var fileNotes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = {
                showAddFileDialog = false
                selectedUri = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.BookmarkAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حفظ الملف في مكتبة الصف 📁")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = fileTitle,
                        onValueChange = { fileTitle = it },
                        label = { Text("عنوان الملف أو اسم الكتاب") },
                        placeholder = { Text("مثال: كتاب الرياضيات الترم الأول") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    com.example.ui.components.GradeStageSelectorField(
                        selectedGrade = fileGrade,
                        onGradeSelected = { fileGrade = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("تصنيف الملف:", style = MaterialTheme.typography.labelSmall)
                    val catOptions = listOf("كتاب الوزارة", "مذكرة الشرح", "ملخص ومراجعة", "امتحانات سابقة", "أوراق عمل ومتابعة")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(catOptions) { c ->
                            FilterChip(
                                selected = fileCategory == c,
                                onClick = { fileCategory = c },
                                label = { Text(c, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = fileNotes,
                        onValueChange = { fileNotes = it },
                        label = { Text("ملاحظات إضافية (اختياري)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = selectedUri ?: return@Button
                        viewModel.addFileFromUri(
                            context = context,
                            uri = uri,
                            title = fileTitle,
                            grade = fileGrade,
                            category = fileCategory,
                            notes = fileNotes
                        )
                        showAddFileDialog = false
                        selectedUri = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                ) {
                    Text("حفظ في التطبيق")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddFileDialog = false
                    selectedUri = null
                }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    selectedFileToDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { selectedFileToDelete = null },
            title = { Text("حذف الملف") },
            text = { Text("هل أنت متأكد من رغبتك في حذف ملف \"${file.title}\" من مكتبة التطبيق؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFile(file)
                        selectedFileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedFileToDelete = null }) { Text("إلغاء") }
            }
        )
    }

    if (showMathEduBrowserDialog) {
        InAppMathEduBrowserDialog(
            url = MATH_EDU_URL,
            onDismiss = { showMathEduBrowserDialog = false }
        )
    }
}

@Composable
fun StudyFileCard(
    file: StudyFileEntity,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().clickable { onOpen() }
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = when (file.category) {
                            "كتاب الوزارة" -> Color(0xFF1E3A8A).copy(alpha = 0.12f)
                            "مذكرة الشرح" -> Color(0xFF047857).copy(alpha = 0.12f)
                            "ملخص ومراجعة" -> Color(0xFFB45309).copy(alpha = 0.12f)
                            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (file.fileExtension.lowercase()) {
                                    "pdf" -> Icons.Filled.PictureAsPdf
                                    "doc", "docx" -> Icons.Filled.Description
                                    "ppt", "pptx" -> Icons.Filled.Slideshow
                                    "png", "jpg", "jpeg" -> Icons.Filled.Image
                                    else -> Icons.Filled.InsertDriveFile
                                },
                                contentDescription = null,
                                tint = when (file.category) {
                                    "كتاب الوزارة" -> NavyPrimary
                                    "مذكرة الشرح" -> EmeraldSuccess
                                    "ملخص ومراجعة" -> Color(0xFFB45309)
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = file.grade,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "• ${file.category}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // File Size Badge
                Text(
                    text = StudyFileManager.formatFileSize(file.fileSizeBytes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (file.notes.isNotBlank()) {
                Text(
                    text = "✍️ ${file.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Open in Canvas/PDF Viewer with Geometric Tools
                Button(
                    onClick = onOpen,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("فتح والشرح بالأدوات الهندسية 📐", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Share, contentDescription = "مشاركة", tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
