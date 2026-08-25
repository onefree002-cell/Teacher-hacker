package com.example.ui.screens.students

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import com.example.data.local.entity.MaterialDeliveryEntity
import com.example.data.local.entity.StudentEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDetailScreen(
    studentId: Long,
    viewModel: StudentsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateHome: (() -> Unit)? = null,
    onNavigateToReportBuilder: (Long) -> Unit,
    onNavigateToCertificateDesigner: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showEditDialog by remember { mutableStateOf(false) }
    var showWhatsAppDialog by remember { mutableStateOf(false) }
    var showAddDeliveryDialog by remember { mutableStateOf(false) }

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
                        Column(modifier = Modifier.padding(16.dp)) {
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
                                        text = "${details.group?.name ?: "بدون مجموعة"} • ${details.student.grade} • كود: ${details.student.barcodeCode.ifEmpty { "STD-${details.student.id}" }}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                                StatusBadge(status = details.student.status)
                            }
                        }
                    }
                }

                // 2. Direct Communication Actions
                item {
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
}