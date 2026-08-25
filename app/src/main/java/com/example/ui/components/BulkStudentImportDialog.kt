package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.StudentEntity
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NavyPrimary
import com.example.util.ContactPickerHelper

data class ParsedStudentRow(
    val name: String,
    val phone: String = "",
    val parentPhone: String = "",
    val isValid: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkStudentImportDialog(
    groups: List<GroupEntity>,
    initialGroupId: Long = 0L,
    onDismiss: () -> Unit,
    onImport: (List<StudentEntity>) -> Unit
) {
    val context = LocalContext.current
    var rawText by remember { mutableStateOf("") }
    var selectedGroupId by remember { mutableStateOf(if (initialGroupId != 0L) initialGroupId else (groups.firstOrNull()?.id ?: 0L)) }
    var selectedGrade by remember { mutableStateOf(groups.find { it.id == selectedGroupId }?.grade ?: "الصف الأول الثانوي") }
    var groupDropdownExpanded by remember { mutableStateOf(false) }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        if (uri != null) {
            val contactInfo = ContactPickerHelper.extractContact(context, uri)
            if (contactInfo != null) {
                val newEntry = "${contactInfo.name} ${contactInfo.phone}".trim()
                rawText = if (rawText.isBlank()) newEntry else "$rawText\n$newEntry"
                Toast.makeText(context, "تمت إضافة جهة الاتصال إلى القائمة", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val parsedList = remember(rawText) {
        if (rawText.isBlank()) emptyList()
        else {
            rawText.lines()
                .filter { it.isNotBlank() }
                .map { line ->
                    parseStudentLine(line)
                }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ContentPaste, contentDescription = null, tint = NavyPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "إدخال واستيراد الطلاب دفعة واحدة",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "الصق قائمة الأسماء وأرقام الهواتف أو أضفها من جهات اتصال هاتفك:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                // Select Group
                if (groups.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = groupDropdownExpanded,
                        onExpandedChange = { groupDropdownExpanded = it }
                    ) {
                        val gName = groups.find { it.id == selectedGroupId }?.name ?: "اختر المجموعة"
                        OutlinedTextField(
                            value = gName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("إضافة الطلاب إلى مجموعة") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = groupDropdownExpanded,
                            onDismissRequest = { groupDropdownExpanded = false }
                        ) {
                            groups.forEach { g ->
                                DropdownMenuItem(
                                    text = { Text(g.name) },
                                    onClick = {
                                        selectedGroupId = g.id
                                        selectedGrade = g.grade
                                        groupDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Grade / Stage Selector Field
                GradeStageSelectorField(
                    selectedGrade = selectedGrade,
                    onGradeSelected = { selectedGrade = it },
                    modifier = Modifier.fillMaxWidth()
                )

                // Add from contacts button row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            try {
                                contactPickerLauncher.launch(null)
                            } catch (e: Exception) {
                                Toast.makeText(context, "تعذر فتح جهات الاتصال", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.PersonAddAlt1, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة من جهات اتصال الهاتف", style = MaterialTheme.typography.labelMedium)
                    }

                    if (rawText.isNotEmpty()) {
                        TextButton(
                            onClick = { rawText = "" },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("مسح الكل", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                // Input Text Area
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    placeholder = {
                        Text("مثال:\nأحمد محمود 01012345678 01123456789\nسارة محمد - 01234567890\nخالد إبراهيم")
                    },
                    label = { Text("النص المنسوخ") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth().testTag("bulk_paste_input")
                )

                // Live Preview Counter
                if (parsedList.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "تم التعرف على: ${parsedList.size} طالب",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = NavyPrimary
                            )
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Preview list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp)
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(parsedList) { idx, s ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (idx % 2 == 0) Color.White else Color(0xFFF8FAFC))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${idx + 1}. ${s.name}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                Text(
                                    text = if (s.parentPhone.isNotEmpty()) "${s.phone} | ${s.parentPhone}" else s.phone.ifEmpty { "بدون هاتف" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val students = parsedList.filter { it.name.isNotBlank() }.mapIndexed { idx, row ->
                        val code = "STD-${(System.currentTimeMillis() + idx) % 1000000}"
                        StudentEntity(
                            name = row.name,
                            groupId = selectedGroupId,
                            grade = selectedGrade,
                            phone = ContactPickerHelper.cleanPhoneNumber(row.phone),
                            parentPhone = ContactPickerHelper.cleanPhoneNumber(row.parentPhone),
                            barcodeCode = code,
                            status = "active"
                        )
                    }
                    if (students.isNotEmpty()) {
                        onImport(students)
                    }
                },
                enabled = parsedList.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                modifier = Modifier.testTag("confirm_bulk_import_btn")
            ) {
                Icon(Icons.Filled.GroupAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("إضافة ${parsedList.size} طالب الآن")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

/**
 * Parses a single line containing a student name and optional phone numbers.
 * Supports Arabic digits, delimiters like whitespace, comma, dash, tab, pipe.
 */
private fun parseStudentLine(line: String): ParsedStudentRow {
    val normalizedLine = com.example.util.TimeUtils.normalizeDigits(line)
    val phoneRegex = Regex("""(\+201[0125][0-9]{8}|00201[0125][0-9]{8}|01[0125][0-9]{8}|[0-9]{10,11})""")
    val phones = phoneRegex.findAll(normalizedLine).map { it.value }.toList()

    var namePart = normalizedLine
    for (ph in phones) {
        namePart = namePart.replace(ph, "")
    }
    // Clean name from leftover separators
    namePart = namePart.replace(Regex("[-–—,|/:;،]"), " ").trim().replace(Regex("\\s+"), " ")

    val studentPhone = phones.getOrNull(0)?.let { ContactPickerHelper.cleanPhoneNumber(it) } ?: ""
    val parentPhone = phones.getOrNull(1)?.let { ContactPickerHelper.cleanPhoneNumber(it) } ?: ""

    return ParsedStudentRow(
        name = namePart.ifEmpty { "طالب بدون اسم" },
        phone = studentPhone,
        parentPhone = parentPhone
    )
}

