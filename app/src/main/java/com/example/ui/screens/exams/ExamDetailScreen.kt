package com.example.ui.screens.exams

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.data.export.PdfReportExporter
import com.example.data.repository.LeaderboardItem
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamDetailScreen(
    examId: Long,
    viewModel: ExamsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCertificate: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var showWhatsAppCustomDialog by remember { mutableStateOf<StudentGradeEntry?>(null) }

    LaunchedEffect(examId) {
        viewModel.loadExamDetails(examId)
    }

    val exam = state.selectedExam

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(exam?.title ?: "رصد درجات الامتحان") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowForward, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (state.studentGradesList.isEmpty()) {
                                Toast.makeText(context, "لا توجد درجات مرصودة لطباعة لوحة الشرف", Toast.LENGTH_SHORT).show()
                                return@IconButton
                            }
                            try {
                                val topEntries = state.studentGradesList
                                    .filter { it.score > 0 }
                                    .sortedByDescending { it.score }
                                val leaderboard = topEntries.mapIndexed { index, entry ->
                                    LeaderboardItem(
                                        student = entry.student,
                                        groupName = state.selectedExamGroup?.name ?: "",
                                        rank = index + 1,
                                        averageScore = entry.score,
                                        attendanceRate = entry.percentage.toInt(),
                                        totalExamsCount = 1,
                                        overallScore = entry.percentage
                                    )
                                }
                                val pdfFile = PdfReportExporter().generateHonorRollPdf(
                                    context = context,
                                    teacher = state.teacher,
                                    group = state.selectedExamGroup,
                                    topStudents = leaderboard,
                                    posterTitle = "لوحة شرف الأوائل - ${exam?.title ?: ""}"
                                )
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", pdfFile)
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "application/pdf")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "عرض وطباعة بوستر لوحة الشرف"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "تعذر إنشاء بوستر لوحة الشرف: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("honor_roll_poster_btn")
                    ) {
                        Icon(Icons.Filled.EmojiEvents, contentDescription = "لوحة شرف الأوائل PDF", tint = AmberGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            if (state.studentGradesList.isNotEmpty()) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("الطلاب المرصود لهم: ${state.studentGradesList.size}", style = MaterialTheme.typography.bodySmall)
                            Text("المتوسط: ${String.format(java.util.Locale.US, "%.1f", state.examAverage)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        Button(
                            onClick = {
                                viewModel.saveAllGrades {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("تم حفظ درجات الامتحان بنجاح")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("save_exam_grades_btn")
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حفظ جميع الدرجات")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (exam != null) {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                // Exam Header Overview
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = exam.title,
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "${state.selectedExamGroup?.name ?: "المجموعة"} • ${exam.date}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(AmberGoldContainer)
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "العظمى: ${exam.maxScore.toInt()}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = AmberGold
                                    )
                                }
                            }
                        }
                    }
                }

                // Performance Summary Bar
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricCard(
                            title = "متوسط الدرجات",
                            value = "${String.format(java.util.Locale.US, "%.1f", state.examAverage)}",
                            icon = Icons.Filled.Analytics,
                            contentColor = IndigoExam,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "أعلى درجة",
                            value = "${state.highestScore.toInt()}",
                            icon = Icons.Filled.Star,
                            contentColor = EmeraldSuccess,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "الناجحين",
                            value = "${state.passCount}",
                            icon = Icons.Filled.Check,
                            contentColor = EmeraldSuccess,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    SectionHeader(title = "قائمة درجات الطلاب (${state.studentGradesList.size})")
                }

                // Student Grades list
                if (state.studentGradesList.isNotEmpty()) {
                    items(state.studentGradesList, key = { it.student.id }) { entry ->
                        val student = entry.student
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(NavyPrimaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(student.name.take(1), fontWeight = FontWeight.Bold, color = NavyPrimary)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(student.name, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = entry.gradeTitle,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = when (entry.gradeTitle) {
                                                    "ممتاز" -> EmeraldSuccess
                                                    "جيد جداً" -> NavyPrimaryLight
                                                    "جيد" -> AmberGold
                                                    "مقبول" -> Color.Gray
                                                    else -> CrimsonError
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("(${String.format(java.util.Locale.US, "%.0f", entry.percentage)}%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }

                                // Score Input Field
                                OutlinedTextField(
                                    value = entry.scoreText,
                                    onValueChange = { viewModel.updateStudentScore(student.id, it) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .width(85.dp)
                                        .testTag("grade_input_${student.id}")
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // WhatsApp Send Result
                                    IconButton(
                                        onClick = { showWhatsAppCustomDialog = entry },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Filled.Chat, contentDescription = "إرسال النتيجة واتساب", tint = EmeraldSuccess)
                                    }

                                    if (entry.percentage >= 85) {
                                        IconButton(
                                            onClick = { onNavigateToCertificate(student.id) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Filled.WorkspacePremium, contentDescription = "شهادة تقدير", tint = AmberGold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    item {
                        EmptyStateWidget(
                            title = "لا يوجد طلاب في هذه المجموعة",
                            description = "أضف طلاباً للمجموعة للتمكن من رصد درجاتهم",
                            icon = Icons.Filled.People
                        )
                    }
                }
            }
        }
    }

    // WhatsApp Dialog for Exam Result
    showWhatsAppCustomDialog?.let { entry ->
        val groupName = state.selectedExamGroup?.name ?: ""
        val examTitle = exam?.title ?: "الاختبار"
        val maxScore = exam?.maxScore ?: 100.0
        val customMsg = "نحيطكم علماً بأن الطالب/ة (${entry.student.name}) حصل على درجة (${entry.score} من ${maxScore.toInt()}) بنسبة (${String.format(java.util.Locale.US, "%.0f", entry.percentage)}%) وتقدير (${entry.gradeTitle}) في ${examTitle}."

        WhatsAppSenderDialog(
            student = entry.student,
            groupName = groupName,
            teacher = state.teacher,
            initialTemplateIndex = 1, // Exam result template
            examTitle = examTitle,
            examScore = entry.score,
            examMaxScore = maxScore,
            onDismiss = { showWhatsAppCustomDialog = null }
        )
    }
}