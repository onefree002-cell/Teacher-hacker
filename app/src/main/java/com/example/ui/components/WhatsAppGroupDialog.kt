package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.StudentEntity
import com.example.data.local.entity.TeacherEntity
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.EmeraldSuccessContainer
import com.example.ui.theme.NavyPrimary
import com.example.util.WhatsAppHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppGroupDialog(
    group: GroupEntity,
    students: List<StudentEntity>,
    teacher: TeacherEntity?,
    onSaveGroupLink: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var groupLink by remember { mutableStateOf(group.whatsappGroupLink) }
    var isEditingLink by remember { mutableStateOf(group.whatsappGroupLink.isBlank()) }
    var showBroadcastDialog by remember { mutableStateOf(false) }

    val parentsWithPhone = students.filter { it.parentPhone.isNotBlank() || it.phone.isNotBlank() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(EmeraldSuccessContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Groups, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("جروب واتساب المجموعة", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(group.name, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Link Status & Action Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = if (groupLink.isNotBlank()) Color(0xFFF0FDF4) else Color(0xFFFFFBEB))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (groupLink.isNotBlank()) Icons.Filled.CheckCircle else Icons.Filled.LinkOff,
                                contentDescription = null,
                                tint = if (groupLink.isNotBlank()) EmeraldSuccess else Color(0xFFD97706),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (groupLink.isNotBlank()) "تم ربط رابط جروب الواتساب" else "لم يتم تعيين رابط للجروب بعد",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = if (groupLink.isNotBlank()) EmeraldSuccess else Color(0xFFD97706)
                            )
                        }

                        if (isEditingLink || groupLink.isBlank()) {
                            OutlinedTextField(
                                value = groupLink,
                                onValueChange = { groupLink = it.trim() },
                                label = { Text("رابط دعوة الجروب (https://chat.whatsapp.com/...)") },
                                placeholder = { Text("الصق رابط دعوة جروب الواتساب هنا") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("whatsapp_link_input")
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = {
                                        onSaveGroupLink(groupLink)
                                        isEditingLink = false
                                        Toast.makeText(context, "تم حفظ وتحديث رابط الجروب بنجاح", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                                ) {
                                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("حفظ الرابط")
                                }
                            }
                        } else {
                            Text(groupLink, style = MaterialTheme.typography.bodySmall, color = NavyPrimary)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { isEditingLink = true },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تعديل الرابط", style = MaterialTheme.typography.labelSmall)
                                }
                                Button(
                                    onClick = {
                                        WhatsAppHelper.openWhatsAppGroupLink(context, groupLink)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("فتح الجروب", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                // Parents & Students Info Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "إضافة أولياء الأمور (${parentsWithPhone.size} من ${students.size} طالب مسجل):",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            "يمكنك نسخ جميع أرقام هواتف أولياء الأمور دفعة واحدة لإضافتهم للجروب بسهولة، أو إرسال رابط الدعوة لهم مباشرة عبر رسائل واتساب سريعة.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val numbers = parentsWithPhone.map {
                                        val p = it.parentPhone.ifEmpty { it.phone }
                                        "${it.name}: $p"
                                    }.joinToString("\n")
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("أرقام أولياء الأمور", numbers))
                                    Toast.makeText(context, "تم نسخ أرقام أولياء الأمور إلى الحافظة (${parentsWithPhone.size} رقم)", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.CopyAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("نسخ الأرقام", style = MaterialTheme.typography.labelSmall)
                            }

                            if (groupLink.isNotBlank()) {
                                Button(
                                    onClick = { showBroadcastDialog = true },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إرسال الدعوة لهم", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("تم")
            }
        }
    )

    if (showBroadcastDialog) {
        val recipients = students.map { s ->
            RecipientItem(
                student = s,
                parentPhone = s.parentPhone.ifEmpty { s.phone },
                customInfo = "عضو مجموعة ${group.name}",
                isSelected = true
            )
        }
        MultiParentMessagingDialog(
            title = "إرسال رابط الجروب لأولياء الأمور",
            groupName = group.name,
            teacher = teacher,
            initialRecipients = recipients,
            defaultMessageType = "group_invite",
            groupLink = groupLink,
            onDismiss = { showBroadcastDialog = false }
        )
    }
}
