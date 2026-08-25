package com.example.ui.screens.finance

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.data.export.PdfReportExporter
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.PaymentEntity
import com.example.data.local.entity.StudentEntity
import com.example.data.local.entity.TeacherEntity
import java.io.File
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0 = Payments, 1 = Expenses, 2 = Balances

    var showAddPaymentDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var paymentToDelete by remember { mutableStateOf<PaymentEntity?>(null) }
    var expenseToDelete by remember { mutableStateOf<ExpenseEntity?>(null) }
    var receiptPaymentToShare by remember { mutableStateOf<PaymentEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "الإدارة المالية والخزينة",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExtendedFloatingActionButton(
                    onClick = { showAddExpenseDialog = true },
                    containerColor = CrimsonError,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Filled.MoneyOff, contentDescription = null) },
                    text = { Text("مصروف") },
                    modifier = Modifier.testTag("add_expense_fab")
                )
                ExtendedFloatingActionButton(
                    onClick = { showAddPaymentDialog = true },
                    containerColor = EmeraldSuccess,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Filled.AttachMoney, contentDescription = null) },
                    text = { Text("قبض اشتراك") },
                    modifier = Modifier.testTag("add_payment_fab")
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Overview Financial Cards
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("صافي الأرباح (الخزينة)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${state.netProfit} ج.م",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.netProfit >= 0) EmeraldSuccess else CrimsonError
                                )
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(EmeraldSuccessContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = EmeraldSuccess)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("التحصيل", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${state.totalRevenue} ج.م", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = EmeraldSuccess)
                        }
                        Column {
                            Text("المصروفات", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${state.totalExpenses} ج.م", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = CrimsonError)
                        }
                        Column {
                            Text("متأخرات الطلاب", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${state.totalPendingBalance} ج.م", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = AmberGold)
                        }
                    }
                }
            }

            // Tabs Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("المدفوعات (${state.payments.size})") },
                    modifier = Modifier.testTag("tab_payments")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("المصروفات (${state.expenses.size})") },
                    modifier = Modifier.testTag("tab_expenses")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("المتأخرات والديون") },
                    modifier = Modifier.testTag("tab_balances")
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    if (state.payments.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(state.payments, key = { it.payment.id }) { item ->
                                val p = item.payment
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.studentName, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = "${item.groupName} • ${p.monthName.ifEmpty { p.type }} • ${p.date}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (p.note.isNotEmpty()) {
                                                Text(p.note, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "+${p.amount} ج.م",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = EmeraldSuccess
                                            )
                                            IconButton(onClick = { receiptPaymentToShare = p }) {
                                                Icon(Icons.Filled.ReceiptLong, contentDescription = "فاتورة إلكترونية PDF", tint = NavyPrimary, modifier = Modifier.size(22.dp))
                                            }
                                            IconButton(onClick = { paymentToDelete = p }) {
                                                Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        EmptyStateWidget(
                            title = "لا توجد مدفوعات مسجلة",
                            description = "سجل اشتراكات الطلاب الشهرية أو اليومية لمتابعة الإيرادات",
                            icon = Icons.Filled.AddCard,
                            actionText = "قبض اشتراك",
                            onActionClick = { showAddPaymentDialog = true }
                        )
                    }
                }
                1 -> {
                    if (state.expenses.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(state.expenses, key = { it.id }) { exp ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(exp.title, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = "${exp.category} • ${exp.date}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (exp.note.isNotEmpty()) {
                                                Text(exp.note, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "-${exp.amount} ج.م",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = CrimsonError
                                            )
                                            IconButton(onClick = { expenseToDelete = exp }) {
                                                Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        EmptyStateWidget(
                            title = "لا توجد مصروفات مسجلة",
                            description = "سجل نفقاتك من إيجار وطباعة وضيافة لحساب صافي الأرباح",
                            icon = Icons.Filled.Paid,
                            actionText = "إضافة مصروف",
                            onActionClick = { showAddExpenseDialog = true }
                        )
                    }
                }
                2 -> {
                    val pendingStudents = state.studentBalances.filter { it.remainingBalance > 0 }
                    if (pendingStudents.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(pendingStudents, key = { it.student.id }) { bal ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(bal.student.name, fontWeight = FontWeight.Bold)
                                            Text("${bal.groupName} • هاتف: ${bal.student.phone.ifEmpty { "لا يوجد" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("دفع: ${bal.totalPaid} ج.م من أصل ${bal.totalRequired} ج.م", style = MaterialTheme.typography.bodySmall, color = NavyPrimaryLight)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("متبقي: ${bal.remainingBalance} ج.م", fontWeight = FontWeight.Bold, color = CrimsonError)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            FilledTonalButton(
                                                onClick = { showAddPaymentDialog = true },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text("تسجيل دفع", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        EmptyStateWidget(
                            title = "لا توجد متأخرات على الطلاب",
                            description = "جميع الطلاب المسجلين قاموا بسداد مستحقاتهم بالكامل",
                            icon = Icons.Filled.CheckCircle
                        )
                    }
                }
            }
        }
    }

    if (showAddPaymentDialog) {
        AddPaymentDialog(
            students = state.students,
            groups = state.groups,
            onDismiss = { showAddPaymentDialog = false },
            onSave = { newPayment ->
                viewModel.addPayment(newPayment)
                showAddPaymentDialog = false
                receiptPaymentToShare = newPayment
            }
        )
    }

    if (showAddExpenseDialog) {
        AddExpenseDialog(
            onDismiss = { showAddExpenseDialog = false },
            onSave = {
                viewModel.addExpense(it)
                showAddExpenseDialog = false
            }
        )
    }

    paymentToDelete?.let { p ->
        ConfirmDeleteDialog(
            title = "حذف الدفعة",
            message = "هل أنت متأكد من رغبتك في حذف هذه الدفعة بقيمة ${p.amount} ج.م؟",
            onConfirm = { viewModel.deletePayment(p) },
            onDismiss = { paymentToDelete = null }
        )
    }

    expenseToDelete?.let { exp ->
        ConfirmDeleteDialog(
            title = "حذف المصروف",
            message = "هل أنت متأكد من حذف المصروف (${exp.title}) بقيمة ${exp.amount} ج.م؟",
            onConfirm = { viewModel.deleteExpense(exp) },
            onDismiss = { expenseToDelete = null }
        )
    }

    receiptPaymentToShare?.let { payment ->
        val student = state.students.firstOrNull { it.id == payment.studentId } ?: StudentEntity(id = payment.studentId, name = "طالب")
        val group = state.groups.firstOrNull { it.id == payment.groupId }
        val studentBalance = state.studentBalances.firstOrNull { it.student.id == payment.studentId }?.remainingBalance ?: 0.0

        PaymentReceiptDialog(
            payment = payment,
            student = student,
            group = group,
            teacher = state.teacher,
            remainingBalance = studentBalance,
            onDismiss = { receiptPaymentToShare = null }
        )
    }
}

@Composable
fun PaymentReceiptDialog(
    payment: PaymentEntity,
    student: StudentEntity,
    group: GroupEntity?,
    teacher: TeacherEntity?,
    remainingBalance: Double,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var generatedFile by remember { mutableStateOf<File?>(null) }
    var isGenerating by remember { mutableStateOf(false) }

    LaunchedEffect(payment) {
        isGenerating = true
        try {
            val file = PdfReportExporter.generatePaymentReceiptPdf(
                context = context,
                teacher = teacher,
                student = student,
                group = group,
                payment = payment,
                remainingBalance = remainingBalance
            )
            generatedFile = file
        } catch (_: Exception) {}
        isGenerating = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(26.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "فاتورة سداد إلكترونية (PDF)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الطالب:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text(student.name, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("المبلغ المدفوع:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text("+${payment.amount} ج.م", fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("عن الفترة:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text(payment.monthName.ifEmpty { payment.type }, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("المتبقي بعد السداد:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text(
                                if (remainingBalance > 0) "$remainingBalance ج.م" else "خالص السداد",
                                fontWeight = FontWeight.Bold,
                                color = if (remainingBalance > 0) CrimsonError else EmeraldSuccess
                            )
                        }
                    }
                }

                if (isGenerating) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("جاري توليد ملف الفاتورة PDF...", style = MaterialTheme.typography.bodySmall)
                    }
                } else if (generatedFile != null) {
                    Text(
                        text = "تم إنشاء الفاتورة الرسمية بنجاح 📄 يمكنك مشاركتها مباشرة مع الطالب أو ولي الأمر:",
                        style = MaterialTheme.typography.bodySmall
                    )

                    // Share Buttons
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                generatedFile?.let { file ->
                                    val targetPhone = student.parentPhone.ifEmpty { student.phone }
                                    val caption = "السلام عليكم، مرفق لسيادتكم إيصال سداد الطالب ${student.name} بقيمة ${payment.amount} ج.م مع خالص التحيات والتقدير."
                                    PdfReportExporter.sharePdfToWhatsApp(context, file, caption, targetPhone)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("share_invoice_whatsapp")
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("مشاركة عبر واتساب (WhatsApp)", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                generatedFile?.let { file ->
                                    val caption = "إيصال سداد الطالب: ${student.name} - المبلغ: ${payment.amount} ج.م"
                                    PdfReportExporter.sharePdfToTelegram(context, file, caption)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088CC)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("share_invoice_telegram")
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("مشاركة عبر تليجرام (Telegram)", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                generatedFile?.let { file ->
                                    PdfReportExporter.sharePdf(context, file, "فاتورة إلكترونية - ${student.name}")
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("مشاركة عامة أو طباعة 🖨️")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}
