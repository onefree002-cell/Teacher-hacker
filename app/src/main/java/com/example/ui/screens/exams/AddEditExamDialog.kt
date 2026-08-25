package com.example.ui.screens.exams

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.ExamEntity
import com.example.data.local.entity.GroupEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditExamDialog(
    exam: ExamEntity? = null,
    groups: List<GroupEntity>,
    onDismiss: () -> Unit,
    onSave: (ExamEntity) -> Unit
) {
    var title by remember { mutableStateOf(exam?.title ?: "") }
    var selectedGroupId by remember { mutableStateOf(exam?.groupId ?: (groups.firstOrNull()?.id ?: 0L)) }
    var maxScore by remember { mutableStateOf(exam?.maxScore?.toString() ?: "100") }
    var passScore by remember { mutableStateOf(exam?.passScore?.toString() ?: "50") }
    var date by remember { mutableStateOf(exam?.date ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var note by remember { mutableStateOf(exam?.note ?: "") }

    var groupDropdownExpanded by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (exam == null) "إنشاء امتحان / اختبار جديد" else "تعديل بيانات الامتحان",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = it.isBlank()
                    },
                    label = { Text("عنوان الامتحان * (مثال: اختبار الوحدة الأولى)") },
                    isError = titleError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("exam_title_input")
                )

                // Group selector
                ExposedDropdownMenuBox(
                    expanded = groupDropdownExpanded,
                    onExpandedChange = { groupDropdownExpanded = it }
                ) {
                    val currentGroup = groups.firstOrNull { it.id == selectedGroupId }
                    OutlinedTextField(
                        value = currentGroup?.name ?: "اختر المجموعة الدراسية",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("المجموعة") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth().testTag("exam_group_picker")
                    )
                    ExposedDropdownMenu(
                        expanded = groupDropdownExpanded,
                        onDismissRequest = { groupDropdownExpanded = false }
                    ) {
                        groups.forEach { g ->
                            DropdownMenuItem(
                                text = { Text("${g.name} (${g.grade})") },
                                onClick = {
                                    selectedGroupId = g.id
                                    groupDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = maxScore,
                        onValueChange = { maxScore = it },
                        label = { Text("الدرجة العظمى") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("exam_max_score_input")
                    )
                    OutlinedTextField(
                        value = passScore,
                        onValueChange = { passScore = it },
                        label = { Text("درجة النجاح") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("تاريخ الامتحان (yyyy-MM-dd)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("ملاحظات أو موضوعات الامتحان") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                        return@Button
                    }
                    val e = ExamEntity(
                        id = exam?.id ?: 0L,
                        groupId = selectedGroupId,
                        title = title.trim(),
                        maxScore = maxScore.toDoubleOrNull() ?: 100.0,
                        passScore = passScore.toDoubleOrNull() ?: 50.0,
                        date = date.trim(),
                        note = note.trim()
                    )
                    onSave(e)
                    onDismiss()
                },
                modifier = Modifier.testTag("save_exam_button")
            ) {
                Text(if (exam == null) "إنشاء الامتحان" else "حفظ")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
