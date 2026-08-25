package com.example.ui.screens.questions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.QuestionEntity

@Composable
fun AddEditQuestionDialog(
    question: QuestionEntity?,
    onSave: (QuestionEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var subject by remember { mutableStateOf(question?.subject ?: "الرياضيات") }
    var grade by remember { mutableStateOf(question?.grade ?: "الثالث الثانوي") }
    var unitLesson by remember { mutableStateOf(question?.unitLesson ?: "") }
    var questionText by remember { mutableStateOf(question?.questionText ?: "") }
    var questionType by remember { mutableStateOf(question?.questionType ?: "mcq") }
    var optionA by remember { mutableStateOf(question?.optionA ?: "") }
    var optionB by remember { mutableStateOf(question?.optionB ?: "") }
    var optionC by remember { mutableStateOf(question?.optionC ?: "") }
    var optionD by remember { mutableStateOf(question?.optionD ?: "") }
    var correctAnswer by remember { mutableStateOf(question?.correctAnswer ?: "") }
    var difficulty by remember { mutableStateOf(question?.difficulty ?: "متوسط") }
    var marksText by remember { mutableStateOf(question?.marks?.toString() ?: "5.0") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("add_edit_question_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (question == null) "إضافة سؤال جديد لبنك الأسئلة" else "تعديل السؤال",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "إغلاق")
                    }
                }

                HorizontalDivider()

                // Subject & Grade Selector
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("المادة") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    com.example.ui.components.GradeStageSelectorField(
                        selectedGrade = grade,
                        onGradeSelected = { grade = it },
                        modifier = Modifier.weight(1.4f)
                    )
                }

                // Unit & Lesson
                OutlinedTextField(
                    value = unitLesson,
                    onValueChange = { unitLesson = it },
                    label = { Text("الوحدة / الدرس / الفصل") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Question Type
                Text("نوع السؤال:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = questionType == "mcq",
                        onClick = { questionType = "mcq" },
                        label = { Text("اختيار من متعدد") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = questionType == "true_false",
                        onClick = { questionType = "true_false" },
                        label = { Text("صح أو خطأ") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = questionType == "essay",
                        onClick = { questionType = "essay" },
                        label = { Text("مقالي") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Question Text
                OutlinedTextField(
                    value = questionText,
                    onValueChange = { questionText = it },
                    label = { Text("نص السؤال *") },
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("question_text_input")
                )

                // MCQ Options if MCQ
                if (questionType == "mcq") {
                    OutlinedTextField(
                        value = optionA,
                        onValueChange = { optionA = it },
                        label = { Text("الخيار (أ)") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = optionB,
                        onValueChange = { optionB = it },
                        label = { Text("الخيار (ب)") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = optionC,
                        onValueChange = { optionC = it },
                        label = { Text("الخيار (ج)") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = optionD,
                        onValueChange = { optionD = it },
                        label = { Text("الخيار (د)") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Correct Answer
                OutlinedTextField(
                    value = correctAnswer,
                    onValueChange = { correctAnswer = it },
                    label = { Text("الإجابة النموذجية / الحل الصحيح") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Difficulty & Marks
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = difficulty,
                        onValueChange = { difficulty = it },
                        label = { Text("مستوى الصعوبة (سهل/متوسط/صعب)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                        value = marksText,
                        onValueChange = { marksText = it },
                        label = { Text("الدرجة") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(0.8f)
                    )
                }

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
                            if (questionText.isNotBlank()) {
                                onSave(
                                    QuestionEntity(
                                        id = question?.id ?: 0L,
                                        subject = subject,
                                        grade = grade,
                                        unitLesson = unitLesson,
                                        questionText = questionText,
                                        questionType = questionType,
                                        optionA = optionA,
                                        optionB = optionB,
                                        optionC = optionC,
                                        optionD = optionD,
                                        correctAnswer = correctAnswer,
                                        difficulty = difficulty,
                                        marks = marksText.toDoubleOrNull() ?: 5.0
                                    )
                                )
                            }
                        },
                        enabled = questionText.isNotBlank(),
                        modifier = Modifier.weight(1.5f).testTag("save_question_btn")
                    ) {
                        Text("حفظ السؤال")
                    }
                }
            }
        }
    }
}
