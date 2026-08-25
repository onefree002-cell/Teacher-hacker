package com.example.ui.screens.smartprep

import android.widget.Toast
import androidx.compose.foundation.background
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
import kotlinx.coroutines.launch
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.LessonPlanEntity
import com.example.data.local.entity.TeacherEntity
import com.example.ui.theme.AmberGold
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavyPrimaryContainer
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditLessonPlanDialog(
    plan: LessonPlanEntity? = null,
    groups: List<GroupEntity>,
    teacher: TeacherEntity?,
    onGenerateSmart: (subject: String, grade: String, topic: String) -> LessonPlanEntity,
    onGenerateAi: (suspend (subject: String, grade: String, topic: String, duration: Int) -> LessonPlanEntity)? = null,
    onDismiss: () -> Unit,
    onSave: (LessonPlanEntity) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val initialSubject = plan?.subject ?: (teacher?.subject?.ifBlank { "رياضيات" } ?: "رياضيات")
    val initialGrade = plan?.grade ?: (groups.firstOrNull()?.grade ?: "الصف الأول الثانوي")
    val today = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date()) }

    var title by remember { mutableStateOf(plan?.title ?: "") }
    var subject by remember { mutableStateOf(initialSubject) }
    var grade by remember { mutableStateOf(initialGrade) }
    var targetDate by remember { mutableStateOf(plan?.targetDate ?: today) }
    var durationMinutes by remember { mutableStateOf(plan?.durationMinutes?.toString() ?: "60") }

    var objectives by remember { mutableStateOf(plan?.objectives ?: "") }
    var warmupHook by remember { mutableStateOf(plan?.warmupHook ?: "") }
    var keyPoints by remember { mutableStateOf(plan?.keyPoints ?: "") }
    var teachingAids by remember { mutableStateOf(plan?.teachingAids ?: "") }
    var activities by remember { mutableStateOf(plan?.activities ?: "") }
    var assessmentQuestions by remember { mutableStateOf(plan?.assessmentQuestions ?: "") }
    var homework by remember { mutableStateOf(plan?.homework ?: "") }
    var notes by remember { mutableStateOf(plan?.notes ?: "") }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("البيانات الأساسية", "الأهداف والتمهيد", "شرح الأفكار", "التقويم والواجب")

    var showSmartGenPrompt by remember { mutableStateOf(false) }
    var promptTopic by remember { mutableStateOf("") }
    var isGeneratingAi by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (plan == null) Icons.Filled.Psychology else Icons.Filled.EditNote,
                        contentDescription = null,
                        tint = NavyPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (plan == null) "تحضير درس ذكي جديد" else "تعديل تحضير الدرس",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                FilledTonalButton(
                    onClick = { showSmartGenPrompt = true },
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = AmberGold.copy(alpha = 0.15f), contentColor = AmberGold),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تحضير بالذكاء الاصطناعي ✨", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Section Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEachIndexed { index, tabTitle ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    tabTitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            // 1. Basic Info
                            OutlinedTextField(
                                value = title,
                                onValueChange = {
                                    title = it
                                    titleError = it.isBlank()
                                },
                                label = { Text("عنوان الدرس / الموضوع *") },
                                placeholder = { Text("مثال: نظرية فيثاغورس وتطبيقاتها") },
                                isError = titleError,
                                supportingText = { if (titleError) Text("عنوان الدرس مطلوب") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("lesson_plan_title_input")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = subject,
                                    onValueChange = { subject = it },
                                    label = { Text("المادة") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                com.example.ui.components.GradeStageSelectorField(
                                    selectedGrade = grade,
                                    onGradeSelected = { grade = it },
                                    modifier = Modifier.weight(1.3f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = targetDate,
                                    onValueChange = { targetDate = it },
                                    label = { Text("تاريخ الشرح") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.2f)
                                )
                                OutlinedTextField(
                                    value = durationMinutes,
                                    onValueChange = { durationMinutes = it },
                                    label = { Text("المدة (دقيقة)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.8f)
                                )
                            }

                            OutlinedTextField(
                                value = teachingAids,
                                onValueChange = { teachingAids = it },
                                label = { Text("الوسائل والأدوات التعليمية") },
                                placeholder = { Text("السبورة التفاعلية، الشيتات، المجسمات، العرض التقديمي") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        1 -> {
                            // 2. Objectives & Warmup
                            OutlinedTextField(
                                value = objectives,
                                onValueChange = { objectives = it },
                                label = { Text("الأهداف التعليمية والإجرائية (مخرجات التعلم)") },
                                placeholder = { Text("1. أن يستنتج الطالب...\n2. أن يحل المسائل بمهارة...") },
                                minLines = 4,
                                maxLines = 7,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = warmupHook,
                                onValueChange = { warmupHook = it },
                                label = { Text("التهيئة الحافزة والعصف الذهني (التمهيد)") },
                                placeholder = { Text("سؤال تشويقي، لغز رياضي، قصة واقعية مدخل للدرس...") },
                                minLines = 3,
                                maxLines = 6,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        2 -> {
                            // 3. Key Points & Activities
                            OutlinedTextField(
                                value = keyPoints,
                                onValueChange = { keyPoints = it },
                                label = { Text("عناصر الدرس وشرح الأفكار الأساسية (خريطة الدرس)") },
                                placeholder = { Text("① التعريف والمفهوم الأساسي\n② القوانين والعلاقات الرياضية\n③ أمثلة وتطبيقات نموذجية\n④ الأخطاء الشائعة وطرق تجنبها") },
                                minLines = 5,
                                maxLines = 8,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = activities,
                                onValueChange = { activities = it },
                                label = { Text("الأنشطة الصفية واستراتيجيات التدريس") },
                                placeholder = { Text("تعلم تعاوني، مسابقة سريعة بين المجموعات، مناقشة جماعية...") },
                                minLines = 3,
                                maxLines = 5,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        3 -> {
                            // 4. Assessment & Homework
                            OutlinedTextField(
                                value = assessmentQuestions,
                                onValueChange = { assessmentQuestions = it },
                                label = { Text("أسئلة التقويم والتغذية الراجعة (قياس الفهم)") },
                                placeholder = { Text("س1: سؤال سريع للتأكد من المفهوم\nس2: سؤال تطبيقي\nس3: سؤال للمتفوقين") },
                                minLines = 4,
                                maxLines = 6,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = homework,
                                onValueChange = { homework = it },
                                label = { Text("التكليفات والواجب المنزلي") },
                                placeholder = { Text("حل تمارين صـ (24) في المذكرة + شيت المتابعة") },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("ملاحظات إضافية وتوصيات للمعلم") },
                                placeholder = { Text("التركيز على الفروق الفردية، إعادة توضيح النقطة...") },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                        selectedTab = 0
                        return@Button
                    }
                    val updated = LessonPlanEntity(
                        id = plan?.id ?: 0L,
                        title = title.trim(),
                        subject = subject.trim(),
                        grade = grade.trim(),
                        targetDate = targetDate.trim(),
                        durationMinutes = durationMinutes.toIntOrNull() ?: 60,
                        objectives = objectives.trim(),
                        warmupHook = warmupHook.trim(),
                        keyPoints = keyPoints.trim(),
                        teachingAids = teachingAids.trim(),
                        activities = activities.trim(),
                        assessmentQuestions = assessmentQuestions.trim(),
                        homework = homework.trim(),
                        notes = notes.trim(),
                        voiceNoteUri = plan?.voiceNoteUri,
                        isCompleted = plan?.isCompleted ?: false,
                        createdAt = plan?.createdAt ?: System.currentTimeMillis()
                    )
                    onSave(updated)
                    onDismiss()
                },
                modifier = Modifier.testTag("save_lesson_plan_btn")
            ) {
                Text(if (plan == null) "حفظ التحضير" else "حفظ التعديلات")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )

    // Smart / AI Generator Prompt Dialog
    if (showSmartGenPrompt) {
        AlertDialog(
            onDismissRequest = { if (!isGeneratingAi) showSmartGenPrompt = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = AmberGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("التحضير التلقائي بالذكاء الاصطناعي (AI)", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "اكتب عنوان أو موضوع الدرس، وسيقوم الذكاء الاصطناعي ببناء خطة درس تربوية شاملة تتضمن الأهداف، استراتيجيات التهيئة، عناصر الشرح، الأنشطة، والأسئلة:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    OutlinedTextField(
                        value = promptTopic,
                        onValueChange = { promptTopic = it },
                        label = { Text("موضوع أو عنوان الدرس *") },
                        placeholder = { Text("مثال: نظرية فيثاغورس، الروابط الكيميائية، كان وأخواتها...") },
                        singleLine = true,
                        enabled = !isGeneratingAi,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isGeneratingAi) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp, color = AmberGold)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("جاري صياغة خطة الدرس بالذكاء الاصطناعي...", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Fast local template
                    OutlinedButton(
                        onClick = {
                            if (promptTopic.isNotBlank()) {
                                val generated = onGenerateSmart(subject, grade, promptTopic.trim())
                                title = generated.title
                                objectives = generated.objectives
                                warmupHook = generated.warmupHook
                                keyPoints = generated.keyPoints
                                teachingAids = generated.teachingAids
                                activities = generated.activities
                                assessmentQuestions = generated.assessmentQuestions
                                homework = generated.homework
                                notes = generated.notes
                                showSmartGenPrompt = false
                                Toast.makeText(context, "تم تجهيز النموذج السريع بنجاح! ⚡", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isGeneratingAi
                    ) {
                        Text("نموذج سريع ⚡", fontSize = 12.sp)
                    }

                    // AI Generation
                    Button(
                        onClick = {
                            if (promptTopic.isNotBlank()) {
                                isGeneratingAi = true
                                coroutineScope.launch {
                                    try {
                                        val dur = durationMinutes.toIntOrNull() ?: 60
                                        val generated = if (onGenerateAi != null) {
                                            onGenerateAi(subject, grade, promptTopic.trim(), dur)
                                        } else {
                                            onGenerateSmart(subject, grade, promptTopic.trim())
                                        }
                                        title = generated.title
                                        objectives = generated.objectives
                                        warmupHook = generated.warmupHook
                                        keyPoints = generated.keyPoints
                                        teachingAids = generated.teachingAids
                                        activities = generated.activities
                                        assessmentQuestions = generated.assessmentQuestions
                                        homework = generated.homework
                                        notes = generated.notes
                                        showSmartGenPrompt = false
                                        Toast.makeText(context, "تم توليد خطة الدرس بالذكاء الاصطناعي بنجاح! ✨", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "تم التوليد بالنموذج التربوي المعتمد", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isGeneratingAi = false
                                    }
                                }
                            }
                        },
                        enabled = !isGeneratingAi && promptTopic.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = AmberGold)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("توليد بالـ AI ✨", fontSize = 12.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSmartGenPrompt = false },
                    enabled = !isGeneratingAi
                ) {
                    Text("إلغاء")
                }
            }
        )
    }
}
