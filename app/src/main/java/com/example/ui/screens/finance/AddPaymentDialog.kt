package com.example.ui.screens.finance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.PaymentEntity
import com.example.data.local.entity.StudentEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPaymentDialog(
    students: List<StudentEntity>,
    groups: List<GroupEntity>,
    onDismiss: () -> Unit,
    onSave: (PaymentEntity) -> Unit
) {
    var selectedStudentId by remember { mutableStateOf(students.firstOrNull()?.id ?: 0L) }
    val selectedStudent = students.firstOrNull { it.id == selectedStudentId }
    val defaultGroupId = selectedStudent?.groupId ?: (groups.firstOrNull()?.id ?: 0L)
    val defaultPrice = groups.firstOrNull { it.id == defaultGroupId }?.monthlyPrice?.toString() ?: "300"

    var selectedGroupId by remember { mutableStateOf(defaultGroupId) }
    var amount by remember { mutableStateOf(defaultPrice) }
    var type by remember { mutableStateOf("monthly") } // monthly, session, book, other
    var monthName by remember {
        val currentMonth = SimpleDateFormat("MMMM yyyy", Locale("ar")).format(Date())
        mutableStateOf(currentMonth)
    }
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var note by remember { mutableStateOf("") }

    var studentDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("تسجيل دفعة / اشتراك جديد", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Student selector
                ExposedDropdownMenuBox(
                    expanded = studentDropdownExpanded,
                    onExpandedChange = { studentDropdownExpanded = it }
                ) {
                    val sName = students.firstOrNull { it.id == selectedStudentId }?.name ?: "اختر الطالب"
                    OutlinedTextField(
                        value = sName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("الطالب") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = studentDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth().testTag("payment_student_picker")
                    )
                    ExposedDropdownMenu(
                        expanded = studentDropdownExpanded,
                        onDismissRequest = { studentDropdownExpanded = false }
                    ) {
                        students.forEach { s ->
                            val gName = groups.firstOrNull { it.id == s.groupId }?.name ?: ""
                            DropdownMenuItem(
                                text = { Text("${s.name} ($gName)") },
                                onClick = {
                                    selectedStudentId = s.id
                                    selectedGroupId = s.groupId
                                    val grpPrice = groups.firstOrNull { it.id == s.groupId }?.monthlyPrice ?: 300.0
                                    amount = grpPrice.toString()
                                    studentDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Payment Type Chips
                Text("نوع الدفعة:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = type == "monthly",
                        onClick = { type = "monthly" },
                        label = { Text("شهري") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = type == "session",
                        onClick = { type = "session" },
                        label = { Text("حصة") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = type == "book",
                        onClick = { type = "book" },
                        label = { Text("مذكرات") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Amount
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("المبلغ المدفوع (ج.م) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("payment_amount_input")
                )

                // Month
                OutlinedTextField(
                    value = monthName,
                    onValueChange = { monthName = it },
                    label = { Text("عن شهر / فترة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Date
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("تاريخ الدفع (yyyy-MM-dd)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Note
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("ملاحظات إيصال الدفع") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = PaymentEntity(
                        studentId = selectedStudentId,
                        groupId = selectedGroupId,
                        amount = amount.toDoubleOrNull() ?: 0.0,
                        date = date.trim(),
                        type = type,
                        monthName = monthName.trim(),
                        note = note.trim()
                    )
                    onSave(p)
                    onDismiss()
                },
                modifier = Modifier.testTag("save_payment_btn")
            ) {
                Text("تسجيل الدفعة")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
