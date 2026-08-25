package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.data.local.entity.StudentEntity
import com.example.data.local.entity.TeacherEntity
import com.example.ui.theme.*
import com.example.util.WhatsAppHelper

data class RecipientItem(
    val student: StudentEntity,
    val parentPhone: String,
    val customInfo: String = "", // e.g. "غائب اليوم", "درجة الامتحان: 45/50", "واجب ناقص"
    val isSelected: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiParentMessagingDialog(
    title: String = "إرسال رسائل المتابعة لأولياء الأمور",
    groupName: String = "",
    teacher: TeacherEntity?,
    initialRecipients: List<RecipientItem>,
    defaultMessageType: String = "general", // "general", "absence", "homework", "exam", "group_invite"
    groupLink: String = "",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var messageType by remember { mutableStateOf(defaultMessageType) }
    var customText by remember {
        mutableStateOf(
            when (defaultMessageType) {
                "absence" -> "نحيطكم علماً بغياب الطالب اليوم عن الحصة، يرجى التواصل معنا للاطمئنان عليه وتنسيق التعويض."
                "homework" -> "تذكير بضرورة إنجاز واجب الحصة القادمة والمتابعة لضمان استيعاب الدرس."
                "group_invite" -> ""
                else -> "نود إحاطتكم علماً بمتابعة المستوى الدراسي للطالب والحرص على الحضور في المواعيد المحددة."
            }
        )
    }

    var recipients by remember { mutableStateOf(initialRecipients) }
    var currentIndex by remember { mutableStateOf(0) }
    val selectedRecipients = recipients.filter { it.isSelected && it.parentPhone.isNotBlank() }

    val teacherName = teacher?.name?.ifEmpty { "أستاذ المادة" } ?: "أستاذ المادة"
    val subject = teacher?.subject?.ifEmpty { "المادة الدراسية" } ?: "المادة الدراسية"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(EmeraldSuccessContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Send, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    if (groupName.isNotBlank()) {
                        Text(groupName, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Message Type Selector Chips
                Text("نوع رسالة المتابعة:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = messageType == "general",
                        onClick = {
                            messageType = "general"
                            customText = "نود إحاطتكم علماً بمتابعة المستوى الدراسي للطالب والحرص على الحضور والمذاكرة أولاً بأول."
                        },
                        label = { Text("عامة") }
                    )
                    FilterChip(
                        selected = messageType == "absence",
                        onClick = {
                            messageType = "absence"
                            customText = "نحيطكم علماً بغياب الطالب اليوم عن الحصة، يرجى التواصل معنا للاطمئنان عليه وتنسيق التعويض."
                        },
                        label = { Text("غياب") }
                    )
                    FilterChip(
                        selected = messageType == "homework",
                        onClick = {
                            messageType = "homework"
                            customText = "تذكير بضرورة إنجاز واجب الحصة القادمة والمتابعة لضمان استيعاب الدرس."
                        },
                        label = { Text("واجب") }
                    )
                    if (groupLink.isNotBlank()) {
                        FilterChip(
                            selected = messageType == "group_invite",
                            onClick = { messageType = "group_invite" },
                            label = { Text("دعوة جروب") }
                        )
                    }
                }

                // 2. Message Body
                if (messageType != "group_invite") {
                    OutlinedTextField(
                        value = customText,
                        onValueChange = { customText = it },
                        label = { Text("نص الرسالة الموحدة") },
                        minLines = 3,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth().testTag("multi_msg_input")
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE8F5E9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("سيتم إرسال رابط جروب الواتساب التالي لجميع أولياء الأمور المحددين:", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(groupLink, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = EmeraldSuccess)
                        }
                    }
                }

                // 3. Selection Summary and Select All Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "أولياء الأمور المحددين (${selectedRecipients.size} من ${recipients.size}):",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = NavyPrimary
                    )
                    TextButton(
                        onClick = {
                            val allSelected = recipients.all { it.isSelected }
                            recipients = recipients.map { it.copy(isSelected = !allSelected) }
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(if (recipients.all { it.isSelected }) "إلغاء تحديد الكل" else "تحديد الكل", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // 4. Recipients List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(recipients) { idx, item ->
                        val hasPhone = item.parentPhone.isNotBlank()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (item.isSelected) Color(0xFFF0FDF4) else Color.Transparent)
                                .clickable(enabled = hasPhone) {
                                    recipients = recipients.toMutableList().also { list ->
                                        list[idx] = item.copy(isSelected = !item.isSelected)
                                    }
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Checkbox(
                                    checked = item.isSelected && hasPhone,
                                    onCheckedChange = { chk ->
                                        recipients = recipients.toMutableList().also { list ->
                                            list[idx] = item.copy(isSelected = chk)
                                        }
                                    },
                                    enabled = hasPhone,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(item.student.name, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                    if (item.customInfo.isNotBlank()) {
                                        Text(item.customInfo, style = MaterialTheme.typography.labelSmall, color = Color(0xFFD97706))
                                    }
                                }
                            }
                            Text(
                                text = if (hasPhone) item.parentPhone else "لا يوجد رقم",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hasPhone) Color.DarkGray else Color.Red
                            )
                        }
                    }
                }

                // 5. Actions bar (Copy all numbers / Direct launch)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val numbersList = selectedRecipients.map { it.parentPhone }.distinct().joinToString("\n")
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("أرقام أولياء الأمور", numbersList))
                            Toast.makeText(context, "تم نسخ أرقام أولياء الأمور إلى الحافظة (${selectedRecipients.size} رقم)", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("نسخ الأرقام", style = MaterialTheme.typography.labelSmall)
                    }

                    if (selectedRecipients.isNotEmpty()) {
                        Text(
                            "طالب $currentIndex / ${selectedRecipients.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedRecipients.isEmpty()) {
                        Toast.makeText(context, "يرجى تحديد ولي أمر واحد على الأقل", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val target = selectedRecipients[currentIndex % selectedRecipients.size]
                    val finalMsg = if (messageType == "group_invite" && groupLink.isNotBlank()) {
                        WhatsAppHelper.createWhatsAppGroupInviteMessage(
                            groupName = groupName.ifEmpty { "المجموعة الدراسية" },
                            groupLink = groupLink,
                            teacherName = teacherName,
                            subject = subject
                        )
                    } else {
                        val header = "السلام عليكم ورحمة الله وبركاته،\nولي أمر الطالب المحترم / ${target.student.name}\n"
                        val infoPart = if (target.customInfo.isNotBlank()) "\nالحالة: ${target.customInfo}\n" else ""
                        "$header\n$customText$infoPart\n\nأستاذ المادة: $teacherName"
                    }

                    WhatsAppHelper.openWhatsApp(context, target.parentPhone, finalMsg)
                    if (currentIndex < selectedRecipients.size - 1) {
                        currentIndex++
                    } else {
                        Toast.makeText(context, "تم فتح إرسال الرسائل لجميع أولياء الأمور المحددين", Toast.LENGTH_LONG).show()
                    }
                },
                enabled = selectedRecipients.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                modifier = Modifier.testTag("send_next_parent_btn")
            ) {
                Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                val targetName = selectedRecipients.getOrNull(currentIndex % (if (selectedRecipients.isNotEmpty()) selectedRecipients.size else 1))?.student?.name ?: ""
                Text(
                    if (selectedRecipients.size == 1) "إرسال عبر واتساب"
                    else "إرسال إلى: ${targetName.take(10)} (${currentIndex + 1}/${selectedRecipients.size})"
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

@Composable
fun MultiParentMessagingDialog(
    students: List<StudentEntity>,
    teacherName: String = "",
    groupName: String = "",
    groupLink: String = "",
    customTemplate: String? = null,
    onDismiss: () -> Unit
) {
    val recipients = remember(students) {
        students.map { student ->
            RecipientItem(
                student = student,
                parentPhone = student.parentPhone.ifEmpty { student.phone },
                customInfo = "",
                isSelected = true
            )
        }
    }

    MultiParentMessagingDialog(
        title = if (groupName.isNotEmpty()) "رسائل أولياء أمور: $groupName" else "إرسال رسائل المتابعة لأولياء الأمور",
        groupName = groupName,
        teacher = TeacherEntity(name = teacherName),
        initialRecipients = recipients,
        defaultMessageType = if (customTemplate != null) "general" else if (groupLink.isNotEmpty()) "group_invite" else "general",
        groupLink = groupLink,
        onDismiss = onDismiss
    )
}
