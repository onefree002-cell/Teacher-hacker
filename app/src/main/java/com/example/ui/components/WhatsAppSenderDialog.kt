package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.util.WhatsAppHelper

enum class WhatsAppTemplateType(val title: String) {
    ABSENCE("إشعار غياب"),
    EXAM_GRADE("نتيجة امتحان"),
    PAYMENT_REMINDER("تذكير بالمصروفات"),
    HOMEWORK("تذكير بالواجب"),
    EXCELLENCE("تهنئة بالتفوق"),
    CUSTOM("رسالة مخصصة")
}

@Composable
fun WhatsAppSenderDialog(
    student: com.example.data.local.entity.StudentEntity,
    groupName: String = "",
    teacher: com.example.data.local.entity.TeacherEntity? = null,
    initialTemplateIndex: Int = 0,
    examTitle: String = "",
    examScore: Double = 0.0,
    examMaxScore: Double = 100.0,
    remainingAmount: Double = 0.0,
    monthName: String = "",
    homeworkTitle: String = "",
    homeworkPages: String = "",
    homeworkDeadline: String = "",
    onDismiss: () -> Unit
) {
    val templates = listOf(
        WhatsAppTemplateType.ABSENCE,
        WhatsAppTemplateType.EXAM_GRADE,
        WhatsAppTemplateType.PAYMENT_REMINDER,
        WhatsAppTemplateType.HOMEWORK,
        WhatsAppTemplateType.EXCELLENCE,
        WhatsAppTemplateType.CUSTOM
    )
    val template = templates.getOrElse(initialTemplateIndex) { WhatsAppTemplateType.ABSENCE }
    WhatsAppSenderDialog(
        studentName = student.name,
        studentPhone = student.phone,
        parentPhone = student.parentPhone,
        teacherName = teacher?.name ?: "المعلم",
        groupName = groupName,
        examTitle = examTitle,
        examScore = examScore,
        examMaxScore = examMaxScore,
        remainingAmount = remainingAmount,
        monthName = monthName,
        homeworkTitle = homeworkTitle,
        homeworkPages = homeworkPages,
        homeworkDeadline = homeworkDeadline,
        gender = student.gender,
        defaultTemplate = template,
        onDismiss = onDismiss
    )
}

@Composable
fun WhatsAppSenderDialog(
    studentName: String,
    studentPhone: String,
    parentPhone: String,
    teacherName: String,
    groupName: String = "",
    examTitle: String = "",
    examScore: Double = 0.0,
    examMaxScore: Double = 100.0,
    remainingAmount: Double = 0.0,
    monthName: String = "",
    homeworkTitle: String = "",
    homeworkPages: String = "",
    homeworkDeadline: String = "",
    gender: String = "boy",
    defaultTemplate: WhatsAppTemplateType = WhatsAppTemplateType.ABSENCE,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTemplate by remember { mutableStateOf(defaultTemplate) }
    var selectedRecipient by remember {
        mutableStateOf(if (parentPhone.isNotBlank()) "parent" else "student")
    }

    var messageBody by remember {
        mutableStateOf(
            generateInitialMessage(
                template = defaultTemplate,
                studentName = studentName,
                groupName = groupName,
                teacherName = teacherName,
                examTitle = examTitle,
                examScore = examScore,
                examMaxScore = examMaxScore,
                remainingAmount = remainingAmount,
                monthName = monthName,
                homeworkTitle = homeworkTitle,
                homeworkPages = homeworkPages,
                homeworkDeadline = homeworkDeadline,
                gender = gender
            )
        )
    }

    // Update message body when template changes
    LaunchedEffect(selectedTemplate) {
        messageBody = generateInitialMessage(
            template = selectedTemplate,
            studentName = studentName,
            groupName = groupName,
            teacherName = teacherName,
            examTitle = examTitle,
            examScore = examScore,
            examMaxScore = examMaxScore,
            remainingAmount = remainingAmount,
            monthName = monthName,
            homeworkTitle = homeworkTitle,
            homeworkPages = homeworkPages,
            homeworkDeadline = homeworkDeadline,
            gender = gender
        )
    }

    val targetPhone = if (selectedRecipient == "parent") parentPhone else studentPhone

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .testTag("whatsapp_sender_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF25D366).copy(alpha = 0.15f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.Send,
                                    contentDescription = null,
                                    tint = Color(0xFF128C7E),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "إرسال رسالة واتساب سريعة",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                "للطالب: $studentName",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "إغلاق")
                    }
                }

                HorizontalDivider()

                // Recipient Selector
                Text(
                    "إرسال الرسالة إلى:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = selectedRecipient == "parent",
                        onClick = { selectedRecipient = "parent" },
                        label = {
                            Text("ولي الأمر (${if (parentPhone.isNotBlank()) parentPhone else "غير مسجل"})")
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.FamilyRestroom, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedRecipient == "student",
                        onClick = { selectedRecipient = "student" },
                        label = {
                            Text("الطالب (${if (studentPhone.isNotBlank()) studentPhone else "غير مسجل"})")
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Templates Selector
                Text(
                    "اختر نوع الرسالة والقالب:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val chunked = WhatsAppTemplateType.entries.chunked(3)
                    chunked.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            row.forEach { tmpl ->
                                val isSel = selectedTemplate == tmpl
                                Surface(
                                    onClick = { selectedTemplate = tmpl },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                    ) {
                                        Text(
                                            tmpl.title,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 11.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Message Text Field
                Text(
                    "نص الرسالة (يمكنك تعديل أي جزء قبل الإرسال):",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                OutlinedTextField(
                    value = messageBody,
                    onValueChange = { messageBody = it },
                    minLines = 6,
                    maxLines = 10,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("whatsapp_message_input")
                )

                // Actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء")
                    }

                    Button(
                        onClick = {
                            if (targetPhone.isNotBlank()) {
                                WhatsAppHelper.openWhatsApp(context, targetPhone, messageBody)
                                onDismiss()
                            }
                        },
                        enabled = targetPhone.isNotBlank() && messageBody.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        modifier = Modifier.weight(1.5f).testTag("send_whatsapp_confirm_btn")
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إرسال عبر واتساب", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun generateInitialMessage(
    template: WhatsAppTemplateType,
    studentName: String,
    groupName: String,
    teacherName: String,
    examTitle: String,
    examScore: Double,
    examMaxScore: Double,
    remainingAmount: Double,
    monthName: String,
    homeworkTitle: String,
    homeworkPages: String,
    homeworkDeadline: String,
    gender: String = "boy"
): String {
    val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    val isGirl = gender == "girl" || gender == "female"
    return when (template) {
        WhatsAppTemplateType.ABSENCE -> WhatsAppHelper.createAbsenceMessage(
            studentName = studentName,
            date = date,
            groupName = if (groupName.isNotBlank()) groupName else "المجموعة التعليمية",
            teacherName = teacherName,
            gender = gender
        )
        WhatsAppTemplateType.EXAM_GRADE -> WhatsAppHelper.createExamGradeMessage(
            studentName = studentName,
            examTitle = if (examTitle.isNotBlank()) examTitle else "الامتحان الشهري",
            score = examScore,
            maxScore = examMaxScore,
            teacherName = teacherName,
            gender = gender
        )
        WhatsAppTemplateType.PAYMENT_REMINDER -> WhatsAppHelper.createPaymentReminderMessage(
            studentName = studentName,
            remainingAmount = if (remainingAmount > 0) remainingAmount else 100.0,
            monthName = if (monthName.isNotBlank()) monthName else "الشهر الحالي",
            teacherName = teacherName,
            gender = gender
        )
        WhatsAppTemplateType.HOMEWORK -> WhatsAppHelper.createHomeworkReminderMessage(
            studentName = studentName,
            homeworkTitle = if (homeworkTitle.isNotBlank()) homeworkTitle else "حل أسئلة الدرس",
            pages = homeworkPages,
            deadline = homeworkDeadline,
            teacherName = teacherName,
            gender = gender
        )
        WhatsAppTemplateType.EXCELLENCE -> WhatsAppHelper.createExcellenceMessage(
            studentName = studentName,
            achievementText = "التفوق والالتزام المتميز في الحصص والامتحانات",
            teacherName = teacherName,
            gender = gender
        )
        WhatsAppTemplateType.CUSTOM -> """
السلام عليكم ورحمة الله وبركاته،
${if (isGirl) "ولي أمر الطالبة المحترمة / $studentName" else "ولي أمر الطالب المحترم / $studentName"}

نود إحاطتكم بالتالي:


مع أطيب تحيات،
أستاذ المادة: $teacherName
        """.trimIndent()
    }
}
