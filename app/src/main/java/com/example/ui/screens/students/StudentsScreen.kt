package com.example.ui.screens.students

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.export.PdfReportExporter
import com.example.data.local.entity.StudentEntity
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsScreen(
    viewModel: StudentsViewModel,
    onNavigateToStudentDetail: (Long) -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    onNavigateHome: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var showBulkImportDialog by remember { mutableStateOf(false) }
    var studentToEdit by remember { mutableStateOf<StudentEntity?>(null) }
    var studentToDelete by remember { mutableStateOf<StudentEntity?>(null) }
    var studentToTransfer by remember { mutableStateOf<StudentEntity?>(null) }
    var studentForWhatsApp by remember { mutableStateOf<StudentEntity?>(null) }
    var showMultiParentDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "إدارة الطلاب (${state.filteredStudents.size})",
                subtitle = "سجل وبيانات الطلاب",
                onNavigateBack = onNavigateBack,
                onNavigateHome = onNavigateHome,
                showHomeButton = true,
                actions = {
                    IconButton(
                        onClick = {
                            if (state.filteredStudents.isEmpty()) {
                                Toast.makeText(context, "لا يوجد طلاب لطباعة الكارنيهات لهم", Toast.LENGTH_SHORT).show()
                                return@IconButton
                            }
                            try {
                                val selectedGroup = state.groups.firstOrNull { it.id == state.selectedGroupId }
                                val pdfFile = PdfReportExporter().generateStudentIdCardsPdf(
                                    context = context,
                                    teacher = state.teacher,
                                    group = selectedGroup,
                                    students = state.filteredStudents
                                )
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "application/pdf")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "عرض وطباعة كارنيهات الطلاب"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "تعذر إنشاء ملف الكارنيهات: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("export_id_cards_btn")
                    ) {
                        Icon(Icons.Filled.Badge, contentDescription = "طباعة كارنيهات الطلاب PDF", tint = NavyPrimary)
                    }
                    IconButton(
                        onClick = {
                            if (state.filteredStudents.isEmpty()) {
                                Toast.makeText(context, "لا يوجد طلاب لإرسال رسائل متابعة لهم", Toast.LENGTH_SHORT).show()
                            } else {
                                showMultiParentDialog = true
                            }
                        },
                        modifier = Modifier.testTag("broadcast_parents_btn")
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "إرسال رسائل متابعة لأولياء الأمور", tint = EmeraldSuccess)
                    }
                    IconButton(
                        onClick = { showBulkImportDialog = true },
                        modifier = Modifier.testTag("bulk_import_btn")
                    ) {
                        Icon(Icons.Filled.ContentPaste, contentDescription = "استيراد دفعة طلاب", tint = NavyPrimary)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_student_fab")
            ) {
                Icon(Icons.Filled.PersonAdd, contentDescription = "إضافة طالب")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("بحث باسم الطالب أو رقم الهاتف أو الكود...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "مسح")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("student_search_bar")
            )

            // Filter Chips: Groups Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.selectedGroupId == 0L,
                        onClick = { viewModel.onGroupFilterSelected(0L) },
                        label = { Text("كل المجموعات") },
                        modifier = Modifier.testTag("filter_all_groups")
                    )
                }
                items(state.groups) { group ->
                    FilterChip(
                        selected = state.selectedGroupId == group.id,
                        onClick = { viewModel.onGroupFilterSelected(group.id) },
                        label = { Text(group.name) }
                    )
                }
            }

            // Filter Chips: Smart Tags Row
            val smartTags = listOf(
                "all" to "كل الطلاب 👥",
                "متميز" to "متميز 🌟",
                "يحتاج متابعة" to "يحتاج متابعة ⚠️",
                "متأخر بالمصروفات" to "متأخرات ⏳",
                "منحة" to "منحة/إعفاء 🎓"
            )
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(smartTags) { (tagKey, tagLabel) ->
                    FilterChip(
                        selected = state.selectedTag == tagKey,
                        onClick = { viewModel.onTagFilterSelected(tagKey) },
                        label = { Text(tagLabel, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Students List
            if (state.filteredStudents.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.filteredStudents, key = { it.id }) { student ->
                        val groupName = state.groups.firstOrNull { it.id == student.groupId }?.name ?: "غير محدد"
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToStudentDetail(student.id) }
                                .testTag("student_item_${student.id}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(NavyPrimaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!student.photoUri.isNullOrEmpty()) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context)
                                                        .data(student.photoUri)
                                                        .crossfade(true)
                                                        .build(),
                                                    contentDescription = student.name,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                )
                                            } else {
                                                Text(
                                                    text = student.name.take(1),
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = NavyPrimary
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = student.name,
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = NavyPrimaryContainer,
                                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, NavyPrimary.copy(alpha = 0.3f))
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Icon(Icons.Filled.Groups, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(12.dp))
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text(
                                                            text = groupName,
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                            color = NavyPrimary
                                                        )
                                                    }
                                                }
                                                if (student.isExempt) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("منحة/إعفاء", style = MaterialTheme.typography.labelSmall, color = EmeraldSuccess)
                                                }
                                            }
                                            Text(
                                                text = "${student.grade} • كود: ${student.barcodeCode.ifEmpty { "STD-${student.id}" }}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    StatusBadge(status = student.status)
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // WhatsApp Action Button
                                    FilledTonalButton(
                                        onClick = { studentForWhatsApp = student },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = EmeraldSuccessContainer, contentColor = EmeraldSuccess)
                                    ) {
                                        Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("واتساب", style = MaterialTheme.typography.labelSmall)
                                    }

                                    if (student.phone.isNotEmpty()) {
                                        OutlinedButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${student.phone}"))
                                                context.startActivity(intent)
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("اتصال", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }

                                    Spacer(modifier = Modifier.weight(1f))

                                    // Transfer group
                                    IconButton(
                                        onClick = { studentToTransfer = student },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Filled.SwapHoriz, contentDescription = "نقل مجموعة", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }

                                    // Edit
                                    IconButton(
                                        onClick = { studentToEdit = student },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }

                                    // Delete
                                    IconButton(
                                        onClick = { studentToDelete = student },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                EmptyStateWidget(
                    title = if (state.searchQuery.isNotEmpty()) "لا توجد نتائج للبحث" else "لا يوجد طلاب حتى الآن",
                    description = if (state.searchQuery.isNotEmpty()) "جرب البحث بكلمات أخرى" else "ابدأ بإضافة طلابك لمتابعة الدرجات والغياب والمصروفات",
                    icon = Icons.Filled.People,
                    actionText = "+ إضافة طالب",
                    onActionClick = { showAddDialog = true }
                )
            }
        }
    }

    // Add dialog
    if (showAddDialog) {
        AddEditStudentDialog(
            groups = state.groups,
            onDismiss = { showAddDialog = false },
            onSave = { viewModel.addOrUpdateStudent(it) }
        )
    }

    // Bulk Import dialog
    if (showBulkImportDialog) {
        BulkStudentImportDialog(
            groups = state.groups,
            initialGroupId = state.selectedGroupId,
            onDismiss = { showBulkImportDialog = false },
            onImport = { students ->
                viewModel.importStudentsBatch(students)
                showBulkImportDialog = false
            }
        )
    }

    // Edit dialog
    studentToEdit?.let { student ->
        AddEditStudentDialog(
            student = student,
            groups = state.groups,
            onDismiss = { studentToEdit = null },
            onSave = { viewModel.addOrUpdateStudent(it) }
        )
    }

    // Delete confirm dialog
    studentToDelete?.let { student ->
        ConfirmDeleteDialog(
            title = "حذف الطالب",
            message = "هل أنت متأكد من رغبتك في حذف الطالب (${student.name})؟ سيتم حذف جميع سجلاته المرتبطة به.",
            onConfirm = { viewModel.deleteStudent(student) },
            onDismiss = { studentToDelete = null }
        )
    }

    // Transfer Group Dialog
    studentToTransfer?.let { student ->
        var targetGroupId by remember { mutableStateOf(student.groupId) }
        AlertDialog(
            onDismissRequest = { studentToTransfer = null },
            title = { Text("نقل الطالب لمجموعة أخرى", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("اختر المجموعة الجديدة للطالب: ${student.name}")
                    state.groups.forEach { group ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { targetGroupId = group.id }
                                .padding(8.dp)
                        ) {
                            RadioButton(
                                selected = targetGroupId == group.id,
                                onClick = { targetGroupId = group.id }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(group.name, fontWeight = if (targetGroupId == group.id) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.transferStudent(student.id, targetGroupId)
                    studentToTransfer = null
                    Toast.makeText(context, "تم نقل الطالب بنجاح", Toast.LENGTH_SHORT).show()
                }) {
                    Text("تأكيد النقل")
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToTransfer = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // WhatsApp Quick Message Dialog
    studentForWhatsApp?.let { student ->
        val groupName = state.groups.firstOrNull { it.id == student.groupId }?.name ?: ""
        WhatsAppSenderDialog(
            student = student,
            groupName = groupName,
            teacher = state.teacher,
            onDismiss = { studentForWhatsApp = null }
        )
    }

    // Multi-Parent Broadcast Dialog
    if (showMultiParentDialog) {
        val selectedGroupName = state.groups.firstOrNull { it.id == state.selectedGroupId }?.name ?: "جميع الطلاب"
        val selectedGroupLink = state.groups.firstOrNull { it.id == state.selectedGroupId }?.whatsappGroupLink ?: ""
        val recipients = state.filteredStudents.map { s ->
            RecipientItem(
                student = s,
                parentPhone = s.parentPhone.ifEmpty { s.phone },
                customInfo = "الصف: ${s.grade}",
                isSelected = true
            )
        }
        MultiParentMessagingDialog(
            title = "إرسال رسائل المتابعة لأولياء الأمور",
            groupName = selectedGroupName,
            teacher = state.teacher,
            initialRecipients = recipients,
            defaultMessageType = "general",
            groupLink = selectedGroupLink,
            onDismiss = { showMultiParentDialog = false }
        )
    }
}