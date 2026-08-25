package com.example.ui.screens.questions

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.core.content.FileProvider
import com.example.data.export.PdfReportExporter
import com.example.data.local.entity.QuestionEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionBankScreen(
    viewModel: QuestionBankViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val questions by viewModel.filteredQuestions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedDifficulty by viewModel.selectedDifficulty.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val teacher by viewModel.teacher.collectAsState(initial = null)
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var showGenerateExamDialog by remember { mutableStateOf(false) }
    var editingQuestion by remember { mutableStateOf<QuestionEntity?>(null) }
    var questionToDelete by remember { mutableStateOf<QuestionEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("بنك الأسئلة وتوليد الامتحانات", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowForward, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (questions.isEmpty()) {
                                Toast.makeText(context, "أضف بعض الأسئلة أولاً لتوليد الامتحان", Toast.LENGTH_SHORT).show()
                            } else {
                                showGenerateExamDialog = true
                            }
                        },
                        modifier = Modifier.testTag("btn_export_exam_pdf")
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = "توليد امتحان PDF")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingQuestion = null
                    showAddDialog = true
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("إضافة سؤال") },
                modifier = Modifier.testTag("fab_add_question")
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("بحث في الأسئلة والدروس...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "مسح")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("search_questions_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter chips (Difficulty)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("الكل", "سهل", "متوسط", "صعب").forEach { diff ->
                    FilterChip(
                        selected = selectedDifficulty == diff,
                        onClick = { viewModel.setDifficulty(diff) },
                        label = { Text(diff, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("filter_diff_$diff")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Questions List
            if (questions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Quiz,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "لا توجد أسئلة مسجلة حالياً",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Gray
                        )
                        Text(
                            "اضغط على زر (+ إضافة سؤال) لبدء تكوين بنك الأسئلة وتوليد الشيتات والامتحانات",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(questions, key = { it.id }) { q ->
                        QuestionCard(
                            question = q,
                            onEdit = {
                                editingQuestion = q
                                showAddDialog = true
                            },
                            onDelete = {
                                questionToDelete = q
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditQuestionDialog(
            question = editingQuestion,
            onSave = {
                viewModel.saveQuestion(it)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    if (questionToDelete != null) {
        AlertDialog(
            onDismissRequest = { questionToDelete = null },
            title = { Text("حذف السؤال") },
            text = { Text("هل أنت متأكد من حذف هذا السؤال من بنك الأسئلة؟") },
            confirmButton = {
                Button(
                    onClick = {
                        questionToDelete?.let { viewModel.deleteQuestion(it) }
                        questionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { questionToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Generate Exam Dialog
    if (showGenerateExamDialog) {
        var examTitle by remember { mutableStateOf("اختبار تقييمي شامل") }
        var examGrade by remember { mutableStateOf(teacher?.subject?.let { "مادة $it" } ?: "الصف الدراسي") }
        var instructions by remember { mutableStateOf("يُرجى الإجابة عن جميع الأسئلة والتركيز التام") }
        var includeAnswerKey by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showGenerateExamDialog = false },
            title = { Text("توليد وطباعة امتحان / شيت PDF", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = examTitle,
                        onValueChange = { examTitle = it },
                        label = { Text("عنوان الاختبار / الشيت") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = examGrade,
                        onValueChange = { examGrade = it },
                        label = { Text("الصف والمادة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = instructions,
                        onValueChange = { instructions = it },
                        label = { Text("تعليمات الاختبار للطلاب") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = includeAnswerKey,
                            onCheckedChange = { includeAnswerKey = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إرفاق صفحة نموذج الإجابة النموذجي")
                    }
                    Text(
                        text = "عدد الأسئلة المحددة: ${questions.size} أسئلة (إجمالي الدرجات: ${questions.sumOf { it.marks.toInt() }} درجة)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val totalMarks = questions.sumOf { it.marks }
                            val pdfFile = PdfReportExporter().generateExamSheetPdf(
                                context = context,
                                teacher = teacher,
                                title = examTitle,
                                grade = examGrade,
                                questions = questions,
                                instructions = instructions,
                                totalMarks = totalMarks,
                                includeAnswerKey = includeAnswerKey
                            )
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", pdfFile)
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "عرض وطباعة الامتحان"))
                            showGenerateExamDialog = false
                        } catch (e: Exception) {
                            Toast.makeText(context, "تعذر توليد ملف الامتحان: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("توليد وطباعة PDF")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGenerateExamDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun QuestionCard(
    question: QuestionEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            if (question.unitLesson.isNotBlank()) question.unitLesson else "سؤال عام",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (question.difficulty) {
                            "صعب" -> Color(0xFFEF4444).copy(alpha = 0.15f)
                            "متوسط" -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                            else -> Color(0xFF10B981).copy(alpha = 0.15f)
                        }
                    ) {
                        Text(
                            question.difficulty,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = when (question.difficulty) {
                                "صعب" -> Color(0xFFB91C1C)
                                "متوسط" -> Color(0xFFB45309)
                                else -> Color(0xFF047857)
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = Color.Red, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = question.questionText,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )

            if (question.questionType == "mcq" && (question.optionA.isNotBlank() || question.optionB.isNotBlank())) {
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (question.optionA.isNotBlank()) Text("أ) ${question.optionA}", style = MaterialTheme.typography.bodySmall)
                    if (question.optionB.isNotBlank()) Text("ب) ${question.optionB}", style = MaterialTheme.typography.bodySmall)
                    if (question.optionC.isNotBlank()) Text("ج) ${question.optionC}", style = MaterialTheme.typography.bodySmall)
                    if (question.optionD.isNotBlank()) Text("د) ${question.optionD}", style = MaterialTheme.typography.bodySmall)
                }
            }

            if (question.correctAnswer.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "💡 الحل: ${question.correctAnswer}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF059669)
                )
            }
        }
    }
}