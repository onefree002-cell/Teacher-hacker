package com.example.ui.screens.students

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.export.PdfReportExporter
import com.example.data.local.entity.HomeworkSubmissionEntity
import com.example.data.local.entity.MaterialDeliveryEntity
import com.example.data.local.entity.StudentEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StudentDetailScreen(
    studentId: Long,
    viewModel: StudentsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateHome: (() -> Unit)? = null,
    onNavigateToReportBuilder: (Long) -> Unit,
    onNavigateToCertificateDesigner: (Long) -> Unit,
    onNavigateToHomeworkScanner: ((Long) -> Unit)? = null,
    onOpenHomeworkInPdfViewer: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showEditDialog by remember { mutableStateOf(false) }
    var showWhatsAppDialog by remember { mutableStateOf(false) }
    var showAddDeliveryDialog by remember { mutableStateOf(false) }
    var homeworkToEdit by remember { mutableStateOf<HomeworkSubmissionEntity?>(null) }
    var showQuickAddHomeworkDialog by remember { mutableStateOf(false) }

    LaunchedEffect(studentId) {
        viewModel.loadStudentDetails(studentId)
    }

    val details = state.selectedStudentDetails

    Scaffold(
        topBar = {
            AppTopBar(
                title = details?.student?.name ?: "تفاصيل الطالب",
                subtitle = "سجل الطلاب",
                onNavigateBack = onNavigateBack,
                onNavigateHome = onNavigateHome,
                showHomeButton = true,
                actions = {
                    IconButton(
                        onClick = {
                            if (details != null) {
                                try {
                                    val pdfFile = PdfReportExporter().generateStudentIdCardsPdf(
                                        context = context,
                                        teacher = state.teacher,
                                        group = details.group,
                                        students = listOf(details.student)
                                    )
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "application/pdf")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "عرض وطباعة كارنيه الطالب"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "خطأ في إنشاء الكارنيه: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.testTag("student_id_card_btn")
                    ) {
                        Icon(Icons.Filled.Badge, contentDescription = "طباعة كارنيه الطالب")
                    }
                    IconButton(
                        onClick = { onNavigateToReportBuilder(studentId) },
                        modifier = Modifier.testTag("student_export_report_btn")
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = "تقرير PDF")
                    }
                    IconButton(
                        onClick = { onNavigateToCertificateDesigner(studentId) },
                        modifier = Modifier.testTag("student_certificate_btn")
                    ) {
                        Icon(Icons.Filled.WorkspacePremium, contentDescription = "شهادة تقدير")
                    }
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "تعديل")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (details != null) {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // 1. Header Profile Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .border(2.dp, MaterialTheme.colorScheme.onPrimaryContainer, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!details.student.photoUri.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(details.student.photoUri)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = details.student.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            text = details.student.name.take(1),
                                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = details.student.name,
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "${details.group?.name ?: "بدون مجموعة"} • ${details.student.grade}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                                StatusBadge(status = details.student.status)
                            }

                            // Student Code & Edit Barcode row
                            val studentCode = details.student.barcodeCode.ifEmpty { "STD-${details.student.id}" }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.QrCode, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "كود الطالب: $studentCode",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = NavyPrimary
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                                val clip = android.content.ClipData.newPlainText("كود الطالب", studentCode)
                                                clipboard?.setPrimaryClip(clip)
                                                Toast.makeText(context, "تم نسخ كود الطالب: $studentCode", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Filled.ContentCopy, contentDescription = "نسخ الكود", tint = NavyPrimary, modifier = Modifier.size(18.dp))
                                        }
                                        FilledTonalButton(
                                            onClick = { showEditDialog = true },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("تعديل البيانات والكود", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 1.5. Student Detailed Information Card (بطاقة البيانات الكاملة)
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "البيانات والمعلومات الأساسية",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = NavyPrimary
                                )
                                TextButton(
                                    onClick = { showEditDialog = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Filled.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تعديل", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // Phone Numbers
                            if (details.student.phone.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("هاتف الطالب (واتساب): ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text(details.student.phone, style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            if (details.student.parentPhone.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.ContactPhone, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("هاتف ولي الأمر: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text(details.student.parentPhone, style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            // Address
                            if (details.student.address.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("العنوان: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text(details.student.address, style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            // Exemption & Discount
                            if (details.student.isExempt || details.student.discountPercent > 0.0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.CardGiftcard, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("الحالة المالية: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        if (details.student.isExempt) "معفى من المصاريف (منحة كاملة) 🌟"
                                        else "خصم خاص بنسبة ${details.student.discountPercent}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = EmeraldSuccess
                                    )
                                }
                            }

                            // Tags
                            if (details.student.tags.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Label, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("الوسوم: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        details.student.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { tag ->
                                            Surface(
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = tag,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Notes
                            if (details.student.notes.isNotBlank()) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(Icons.Filled.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("ملاحظات: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text(details.student.notes, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                // 2. Direct Communication & Homework Actions
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showWhatsAppDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("واتساب ولي الأمر")
                            }

                            if (details.student.phone.isNotEmpty() || details.student.parentPhone.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = {
                                        val phoneToCall = details.student.parentPhone.ifEmpty { details.student.phone }
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneToCall"))
                                        context.startActivity(intent)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("اتصال هاتف")
                                }
                            }
                        }

                        // 1-Tap Homework Capture & PDF Export Button
                        if (onNavigateToHomeworkScanner != null) {
                            Button(
                                onClick = { onNavigateToHomeworkScanner(details.student.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("student_capture_homework_pdf_btn")
                            ) {
                                Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = AmberGold, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("📸 تصوير وحفظ واجب الطالب (PDF)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 3. Financial Snapshot
                item {
                    Text(
                        text = "الملخص المالي للطالب",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("المدفوع", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${details.totalPaid} ج.م", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = EmeraldSuccess)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("المطلوب", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${details.totalRequired} ج.م", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = NavyPrimary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("المتبقي", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "${details.remainingBalance} ج.م",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (details.remainingBalance > 0) CrimsonError else EmeraldSuccess
                                )
                            }
                        }
                    }
                }

                // 4. Academic & Attendance Snapshot
                item {
                    Text(
                        text = "الأداء الأكاديمي والحضور",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            title = "نسبة الحضور",
                            value = "${details.attendanceRate}%",
                            icon = Icons.Filled.CheckCircle,
                            contentColor = EmeraldSuccess,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "متوسط الدرجات",
                            value = "${String.format(java.util.Locale.US, "%.1f", details.averageScore)}",
                            icon = Icons.Filled.Grade,
                            contentColor = AmberGold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 4.5. Student Homework Submissions & Corrections (سجل واجبات الطالب وتصحيحها)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.AutoStories,
                                contentDescription = null,
                                tint = NavyPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "سجل واجبات الطالب والتصحيح (${state.studentHomeworks.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        FilledTonalButton(
                            onClick = {
                                if (onNavigateToHomeworkScanner != null) {
                                    onNavigateToHomeworkScanner(details.student.id)
                                } else {
                                    showQuickAddHomeworkDialog = true
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة واجب", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (state.studentHomeworks.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            state.studentHomeworks.forEach { hw ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = hw.title,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "تاريخ الواجب: ${hw.assignedDate}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            // Homework Status & Evaluation Badge
                                            Surface(
                                                color = if (hw.rating.contains("ممتاز") || hw.rating.contains("كامل")) EmeraldSuccessContainer
                                                else if (hw.rating.contains("ناقص") || hw.rating.contains("جيد")) AmberGold.copy(alpha = 0.2f)
                                                else CrimsonErrorContainer,
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (hw.rating.contains("ممتاز") || hw.rating.contains("كامل")) EmeraldSuccess
                                                    else if (hw.rating.contains("ناقص") || hw.rating.contains("جيد")) AmberGold
                                                    else CrimsonError
                                                )
                                            ) {
                                                Text(
                                                    text = hw.rating,
                                                    color = if (hw.rating.contains("ممتاز") || hw.rating.contains("كامل")) EmeraldSuccess
                                                    else if (hw.rating.contains("ناقص") || hw.rating.contains("جيد")) NavyPrimary
                                                    else CrimsonError,
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                )
                                            }
                                        }

                                        if (hw.feedbackNote.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = "ملاحظات وتصحيح المدرس: ${hw.feedbackNote}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(8.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Action buttons for Homework (Open in PDF Viewer for corrections, WhatsApp, Edit, Delete)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // 1. Open in PDF Viewer / Annotator
                                            Button(
                                                onClick = {
                                                    val path = hw.photoUri
                                                    if (path.isNotBlank()) {
                                                        onOpenHomeworkInPdfViewer?.invoke(path, hw.title)
                                                    } else {
                                                        Toast.makeText(context, "لا يوجد ملف مرفق لهذا الواجب", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1.3f)
                                            ) {
                                                Icon(Icons.Filled.Draw, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("تصحيح في عارض PDF", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            }

                                            // 2. WhatsApp Share to Parent
                                            OutlinedButton(
                                                onClick = {
                                                    try {
                                                        val msg = "السلام عليكم ولي أمر الطالب ${details.student.name}،\n" +
                                                                "نود إبلاغكم بنتيجة واجب: ${hw.title}\n" +
                                                                "حالة الواجب والتقييم: ${hw.rating}\n" +
                                                                if (hw.feedbackNote.isNotBlank()) "ملاحظة المدرس: ${hw.feedbackNote}\n" else "" +
                                                                "— منصة المعلم الذكي"
                                                        val pPhone = details.student.parentPhone.ifBlank { details.student.phone }
                                                        val cleanNum = pPhone.replace("+", "").replace(" ", "").trim()
                                                        val finalNum = if (!cleanNum.startsWith("20") && cleanNum.startsWith("0")) "2$cleanNum" else cleanNum
                                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                                            data = Uri.parse("https://api.whatsapp.com/send?phone=$finalNum&text=${Uri.encode(msg)}")
                                                        }
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "تعذر فتح تطبيق واتساب", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Filled.Send, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("واتساب", style = MaterialTheme.typography.labelSmall)
                                            }

                                            // 3. Edit Score Dialog
                                            IconButton(
                                                onClick = { homeworkToEdit = hw },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(Icons.Filled.Edit, contentDescription = "تعديل الدرجة", tint = NavyPrimary, modifier = Modifier.size(18.dp))
                                            }

                                            // 4. Delete Homework
                                            IconButton(
                                                onClick = { viewModel.deleteHomework(hw) },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف الواجب", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Filled.FolderOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "لا توجد واجبات محفوظة لملف هذا الطالب حتى الآن",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        if (onNavigateToHomeworkScanner != null) {
                                            onNavigateToHomeworkScanner(details.student.id)
                                        } else {
                                            showQuickAddHomeworkDialog = true
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("📸 تصوير وإضافة أول واجب الآن")
                                }
                            }
                        }
                    }
                }

                // 5. Materials & Books Tracking (المذكرات والكتب المستلمة)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "المذكرات والكتب المستلمة (${state.studentDeliveries.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        TextButton(onClick = { showAddDeliveryDialog = true }) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تسليم مذكرة/كتاب")
                        }
                    }
                    if (state.studentDeliveries.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.studentDeliveries.forEach { del ->
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(del.materialName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                            Text("تاريخ التسليم: ${del.deliveryDate} • السعر: ${del.price} ج.م", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                color = if (del.isPaid) EmeraldSuccessContainer else CrimsonErrorContainer,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = if (del.isPaid) "مدفوع ✓" else "غير مدفوع ✕",
                                                    color = if (del.isPaid) EmeraldSuccess else CrimsonError,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                            IconButton(onClick = { viewModel.deleteMaterialDelivery(del) }) {
                                                Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Text("لم يتم تسجيل تسليم أي مذكرات أو كتب للطالب بعد", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // 6. Recent Payments
                item {
                    SectionHeader(title = "سجل المدفوعات الأخيرة (${state.studentPayments.size})")
                    if (state.studentPayments.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.studentPayments.take(5).forEach { p ->
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("${p.amount} ج.م", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = EmeraldSuccess)
                                            Text("${p.type} • ${p.date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if (p.note.isNotEmpty()) {
                                            Text(p.note, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Text("لا توجد دفعات مسجلة حتى الآن", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }

    if (showEditDialog && details != null) {
        AddEditStudentDialog(
            student = details.student,
            groups = state.groups,
            onDismiss = { showEditDialog = false },
            onSave = {
                viewModel.addOrUpdateStudent(it)
                showEditDialog = false
            }
        )
    }

    if (showWhatsAppDialog && details != null) {
        var scoreVal = details.averageScore
        var maxVal = 100.0
        val lastScore = details.lastExamScore
        if (lastScore.contains("/")) {
            val parts = lastScore.split("/")
            scoreVal = parts.getOrNull(0)?.trim()?.toDoubleOrNull() ?: details.averageScore
            maxVal = parts.getOrNull(1)?.trim()?.toDoubleOrNull() ?: 100.0
        }
        WhatsAppSenderDialog(
            student = details.student,
            groupName = details.group?.name ?: "",
            teacher = state.teacher,
            examScore = scoreVal,
            examMaxScore = maxVal,
            remainingAmount = details.remainingBalance,
            onDismiss = { showWhatsAppDialog = false }
        )
    }

    // Add Material Delivery Dialog
    if (showAddDeliveryDialog && details != null) {
        var matName by remember { mutableStateOf("") }
        var matPrice by remember { mutableStateOf("50") }
        var isPaid by remember { mutableStateOf(true) }
        var deliveryDate by remember {
            mutableStateOf(SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date()))
        }

        AlertDialog(
            onDismissRequest = { showAddDeliveryDialog = false },
            title = { Text("تسليم مذكرة / كتاب جديد", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = matName,
                        onValueChange = { matName = it },
                        label = { Text("اسم المذكرة أو الكتاب") },
                        placeholder = { Text("مثال: مذكرة الفصل الأول - جبر") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = matPrice,
                        onValueChange = { matPrice = it },
                        label = { Text("السعر (ج.م)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = isPaid,
                            onCheckedChange = { isPaid = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تم سداد قيمة المذكرة نقداً")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (matName.isNotEmpty()) {
                            val p = matPrice.toDoubleOrNull() ?: 0.0
                            viewModel.recordMaterialDelivery(
                                MaterialDeliveryEntity(
                                    studentId = details.student.id,
                                    materialName = matName,
                                    price = p,
                                    isPaid = isPaid,
                                    deliveryDate = deliveryDate
                                )
                            )
                            showAddDeliveryDialog = false
                            Toast.makeText(context, "تم تسجيل تسليم المذكرة بنجاح", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("حفظ وتسليم")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDeliveryDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Edit Homework Rating & Status Dialog (بدون درجات رقمية)
    homeworkToEdit?.let { hw ->
        var ratingStr by remember(hw.id) { mutableStateOf(hw.rating.ifBlank { "حل كامل وممتاز 🌟" }) }
        var noteStr by remember(hw.id) { mutableStateOf(hw.feedbackNote) }

        AlertDialog(
            onDismissRequest = { homeworkToEdit = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.EditNote, contentDescription = null, tint = NavyPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تعديل حالة وتقييم الواجب", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(hw.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))

                    // Rating Chips
                    Text("حالة الواجب والتقييم:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("حل كامل وممتاز 🌟", "حل جزئي (ناقص) ⚠️", "لم يحل الواجب ❌", "معفى من الواجب ⚪").forEach { r ->
                            FilterChip(
                                selected = ratingStr == r,
                                onClick = { ratingStr = r },
                                label = { Text(r, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = noteStr,
                        onValueChange = { noteStr = it },
                        label = { Text("ملاحظات وتوجيهات التصحيح") },
                        placeholder = { Text("مثال: ممتاز جداً، انتبه للمسألة الأخيرة") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateHomework(
                            hw.copy(
                                rating = ratingStr,
                                feedbackNote = noteStr
                            )
                        )
                        homeworkToEdit = null
                        Toast.makeText(context, "تم تحديث تقييم الواجب بنجاح", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("حفظ التعديل")
                }
            },
            dismissButton = {
                TextButton(onClick = { homeworkToEdit = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Quick Add Homework Dialog (بدون درجات رقمية)
    if (showQuickAddHomeworkDialog && details != null) {
        var hwTitle by remember { mutableStateOf("واجب الحصة " + SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date())) }
        var hwRating by remember { mutableStateOf("حل كامل وممتاز 🌟") }
        var hwNote by remember { mutableStateOf("") }
        val dateStr = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date()) }

        AlertDialog(
            onDismissRequest = { showQuickAddHomeworkDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PostAdd, contentDescription = null, tint = NavyPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تسجيل واجب جديد للطالب", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = hwTitle,
                        onValueChange = { hwTitle = it },
                        label = { Text("عنوان أو موضوع الواجب") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Rating Chips
                    Text("حالة الواجب:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("حل كامل وممتاز 🌟", "حل جزئي (ناقص) ⚠️", "لم يحل الواجب ❌", "معفى من الواجب ⚪").forEach { r ->
                            FilterChip(
                                selected = hwRating == r,
                                onClick = { hwRating = r },
                                label = { Text(r, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = hwNote,
                        onValueChange = { hwNote = it },
                        label = { Text("ملاحظة أو تعليق المدرس") },
                        placeholder = { Text("أحسنت / يُرجى إعادة حل المسألة 3") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveHomework(
                            HomeworkSubmissionEntity(
                                studentId = details.student.id,
                                title = hwTitle,
                                assignedDate = dateStr,
                                score = 10.0,
                                maxScore = 10.0,
                                rating = hwRating,
                                feedbackNote = hwNote,
                                photoUri = ""
                            )
                        )
                        showQuickAddHomeworkDialog = false
                        Toast.makeText(context, "تم حفظ الواجب في ملف الطالب بنجاح", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("حفظ في ملف الطالب")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickAddHomeworkDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}