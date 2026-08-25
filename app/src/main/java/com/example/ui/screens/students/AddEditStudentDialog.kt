package com.example.ui.screens.students

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import coil.request.ImageRequest
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.StudentEntity
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavyPrimaryContainer
import com.example.util.ContactPickerHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditStudentDialog(
    student: StudentEntity? = null,
    groups: List<GroupEntity>,
    onDismiss: () -> Unit,
    onSave: (StudentEntity) -> Unit
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf(student?.name ?: "") }
    var photoUri by remember { mutableStateOf(student?.photoUri) }
    var selectedGroupId by remember { mutableStateOf(student?.groupId ?: (groups.firstOrNull()?.id ?: 0L)) }
    var grade by remember { mutableStateOf(student?.grade ?: (groups.firstOrNull { it.id == selectedGroupId }?.grade ?: "")) }
    var phone by remember { mutableStateOf(student?.phone ?: "") }
    var parentPhone by remember { mutableStateOf(student?.parentPhone ?: "") }
    var address by remember { mutableStateOf(student?.address ?: "") }
    var status by remember { mutableStateOf(student?.status ?: "active") }
    var isExempt by remember { mutableStateOf(student?.isExempt ?: false) }
    var discountPercent by remember { mutableStateOf(student?.discountPercent?.toString() ?: "0") }
    var notes by remember { mutableStateOf(student?.notes ?: "") }

    var groupDropdownExpanded by remember { mutableStateOf(false) }
    var statusDropdownExpanded by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            photoUri = uri.toString()
            Toast.makeText(context, "تم اختيار صورة الطالب بنجاح", Toast.LENGTH_SHORT).show()
        }
    }

    // Student phone contact picker
    val studentContactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        if (uri != null) {
            val contactInfo = ContactPickerHelper.extractContact(context, uri)
            if (contactInfo != null) {
                if (contactInfo.phone.isNotBlank()) {
                    phone = contactInfo.phone
                }
                if (name.isBlank() && contactInfo.name.isNotBlank()) {
                    name = contactInfo.name
                }
                Toast.makeText(context, "تم استيراد بيانات الطالب من جهات الاتصال", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "تعذر قراءة رقم الهاتف من جهة الاتصال", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Parent phone contact picker
    val parentContactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        if (uri != null) {
            val contactInfo = ContactPickerHelper.extractContact(context, uri)
            if (contactInfo != null && contactInfo.phone.isNotBlank()) {
                parentPhone = contactInfo.phone
                Toast.makeText(context, "تم استيراد رقم ولي الأمر: ${contactInfo.phone}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "تعذر قراءة رقم الهاتف من جهة الاتصال", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp).testTag("student_dialog_back_btn")
                    ) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (student == null) "إضافة طالب جديد" else "تعديل بيانات الطالب",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "إغلاق",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Student Photo Selector Box
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(NavyPrimaryContainer)
                        .border(2.dp, NavyPrimary, CircleShape)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (!photoUri.isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(photoUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "صورة الطالب",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Filled.AddAPhoto,
                                contentDescription = "إضافة صورة",
                                tint = NavyPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                "صورة",
                                style = MaterialTheme.typography.labelSmall,
                                color = NavyPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Photo actions row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("اختر صورة للطالب", style = MaterialTheme.typography.labelSmall)
                    }
                    if (!photoUri.isNullOrEmpty()) {
                        TextButton(
                            onClick = { photoUri = null },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("حذف الصورة", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // 2. Name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = it.isBlank()
                    },
                    label = { Text("اسم الطالب بالكامل *") },
                    isError = nameError,
                    supportingText = { if (nameError) Text("الاسم مطلوب") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("student_name_input")
                )

                // 3. Group selector
                ExposedDropdownMenuBox(
                    expanded = groupDropdownExpanded,
                    onExpandedChange = { groupDropdownExpanded = it }
                ) {
                    val currentGroupName = groups.firstOrNull { it.id == selectedGroupId }?.name ?: "اختر المجموعة"
                    OutlinedTextField(
                        value = currentGroupName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("المجموعة الدراسية") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth().testTag("student_group_selector")
                    )
                    ExposedDropdownMenu(
                        expanded = groupDropdownExpanded,
                        onDismissRequest = { groupDropdownExpanded = false }
                    ) {
                        groups.forEach { group ->
                            DropdownMenuItem(
                                text = { Text("${group.name} (${group.grade})") },
                                onClick = {
                                    selectedGroupId = group.id
                                    if (grade.isBlank()) grade = group.grade
                                    groupDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // 4. Grade / Stage
                com.example.ui.components.GradeStageSelectorField(
                    selectedGrade = grade,
                    onGradeSelected = { grade = it },
                    modifier = Modifier.fillMaxWidth()
                )

                // 5. Phone with Contacts Picker Button
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم هاتف الطالب (واتساب)") },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                try {
                                    studentContactPickerLauncher.launch(null)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "تعذر فتح جهات الاتصال", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("pick_student_contact_btn")
                        ) {
                            Icon(
                                Icons.Filled.ContactPhone,
                                contentDescription = "اختيار من جهات الاتصال",
                                tint = NavyPrimary
                            )
                        }
                    },
                    supportingText = { Text("اضغط أيقونة جهات الاتصال لاختيار الرقم تلقائياً") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("student_phone_input")
                )

                // 6. Parent Phone with Contacts Picker Button
                OutlinedTextField(
                    value = parentPhone,
                    onValueChange = { parentPhone = it },
                    label = { Text("رقم هاتف ولي الأمر") },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                try {
                                    parentContactPickerLauncher.launch(null)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "تعذر فتح جهات الاتصال", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("pick_parent_contact_btn")
                        ) {
                            Icon(
                                Icons.Filled.Contacts,
                                contentDescription = "اختيار رقم ولي الأمر من جهات الاتصال",
                                tint = NavyPrimary
                            )
                        }
                    },
                    supportingText = { Text("اختر رقم ولي الأمر مباشرة من الهاتف") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("student_parent_phone_input")
                )

                // 7. Address
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("العنوان / السكن") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 8. Status
                ExposedDropdownMenuBox(
                    expanded = statusDropdownExpanded,
                    onExpandedChange = { statusDropdownExpanded = it }
                ) {
                    val statusText = when (status) {
                        "active" -> "نشط (منتظم)"
                        "inactive" -> "غير نشط"
                        "suspended" -> "موقوف"
                        else -> status
                    }
                    OutlinedTextField(
                        value = statusText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("حالة القيد") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = statusDropdownExpanded,
                        onDismissRequest = { statusDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("نشط (منتظم)") },
                            onClick = { status = "active"; statusDropdownExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("غير نشط") },
                            onClick = { status = "inactive"; statusDropdownExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("موقوف") },
                            onClick = { status = "suspended"; statusDropdownExpanded = false }
                        )
                    }
                }

                // 9. Exempt switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("إعفاء الطالب من المصاريف (منحة)", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = isExempt,
                        onCheckedChange = { isExempt = it },
                        modifier = Modifier.testTag("student_exempt_switch")
                    )
                }

                // 10. Discount Percent
                if (!isExempt) {
                    OutlinedTextField(
                        value = discountPercent,
                        onValueChange = { discountPercent = it },
                        label = { Text("نسبة الخصم % (0 = لا يوجد)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 11. Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات خاصة عن الطالب") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    val cleanPhone = ContactPickerHelper.cleanPhoneNumber(phone)
                    val cleanParentPhone = ContactPickerHelper.cleanPhoneNumber(parentPhone)
                    val generatedBarcode = student?.barcodeCode?.ifEmpty { "STD-${System.currentTimeMillis() % 1000000}" } 
                        ?: "STD-${System.currentTimeMillis() % 1000000}"

                    val updated = StudentEntity(
                        id = student?.id ?: 0L,
                        name = name.trim(),
                        photoUri = photoUri,
                        groupId = selectedGroupId,
                        grade = grade.trim(),
                        phone = cleanPhone,
                        parentPhone = cleanParentPhone,
                        address = address.trim(),
                        status = status,
                        notes = notes.trim(),
                        isExempt = isExempt,
                        discountPercent = discountPercent.toDoubleOrNull() ?: 0.0,
                        barcodeCode = generatedBarcode,
                        tags = student?.tags ?: "",
                        audioNoteUri = student?.audioNoteUri,
                        createdAt = student?.createdAt ?: System.currentTimeMillis()
                    )
                    onSave(updated)
                    onDismiss()
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_student_button")
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (student == null) "إضافة الطالب" else "حفظ التعديلات", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("dismiss_student_button")
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("رجوع / إلغاء")
            }
        }
    )
}

