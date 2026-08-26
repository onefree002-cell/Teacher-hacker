package com.example.ui.screens.questions

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.QuestionEntity
import com.example.ui.theme.*

data class DrawPoint(val offset: Offset, val color: Color, val strokeWidth: Float, val isNewPath: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
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

    // MCQ selected option tracker (A, B, C, D)
    var selectedMcqOption by remember {
        mutableStateOf(
            when {
                question?.correctAnswer?.startsWith("أ") == true || question?.correctAnswer == question?.optionA && question?.optionA?.isNotBlank() == true -> "A"
                question?.correctAnswer?.startsWith("ب") == true || question?.correctAnswer == question?.optionB && question?.optionB?.isNotBlank() == true -> "B"
                question?.correctAnswer?.startsWith("ج") == true || question?.correctAnswer == question?.optionC && question?.optionC?.isNotBlank() == true -> "C"
                question?.correctAnswer?.startsWith("د") == true || question?.correctAnswer == question?.optionD && question?.optionD?.isNotBlank() == true -> "D"
                else -> ""
            }
        )
    }

    // Geometry Drawing Board / Math Equation Dialog State
    var showMathToolbar by remember { mutableStateOf(false) }
    var showGeometryBoard by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(8.dp)
                .testTag("add_edit_question_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = NavyPrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (question == null) Icons.Filled.AddComment else Icons.Filled.EditNote,
                                    contentDescription = null,
                                    tint = NavyPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                if (question == null) "إضافة سؤال لبنك الأسئلة" else "تعديل السؤال",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "تحديد نوع السؤال والإجابة الصحيحة والمعادلات",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "إغلاق")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Form Fields (Scrollable)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
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

                    // Question Type Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "نوع السؤال:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            FilterChip(
                                selected = questionType == "mcq",
                                onClick = { questionType = "mcq" },
                                label = { Text("اختيار من متعدد", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Filled.Checklist, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.weight(1f).testTag("chip_type_mcq")
                            )
                            FilterChip(
                                selected = questionType == "true_false",
                                onClick = {
                                    questionType = "true_false"
                                    if (correctAnswer != "صح" && correctAnswer != "خطأ") {
                                        correctAnswer = "صح"
                                    }
                                },
                                label = { Text("صح أو خطأ", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.weight(1f).testTag("chip_type_tf")
                            )
                            FilterChip(
                                selected = questionType == "essay",
                                onClick = { questionType = "essay" },
                                label = { Text("سؤال مقالي", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Filled.Draw, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.weight(1f).testTag("chip_type_essay")
                            )
                        }
                    }

                    // Question Text with Toolbar Quick Buttons
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "نص السؤال *",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilledTonalButton(
                                    onClick = { showMathToolbar = !showMathToolbar },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = if (showMathToolbar) NavyPrimary else NavyPrimaryContainer.copy(alpha = 0.5f),
                                        contentColor = if (showMathToolbar) Color.White else NavyPrimary
                                    ),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Filled.Calculate, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("معادلات رياضية", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                FilledTonalButton(
                                    onClick = { showGeometryBoard = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = EmeraldSuccessContainer,
                                        contentColor = EmeraldSuccess
                                    ),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Filled.Category, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("شكل هندسي 2D/3D", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Math Symbols Quick Bar
                        AnimatedVisibility(visible = showMathToolbar) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("اضغط لإدراج الرمز الرياضي:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf(
                                            "√x", "x²", "x³", "xʸ", "½", "¾", "π", "θ", "α", "β", "Δ",
                                            "∫", "∑", "≤", "≥", "≠", "±", "∞", "sin(x)", "cos(x)", "tan(x)",
                                            "log(x)", "ln(x)", "△ABC", "∠θ", "⟂", "∥", "≈", "x̄", "→"
                                        ).forEach { sym ->
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                                modifier = Modifier.clickable {
                                                    questionText += " $sym "
                                                }
                                            ) {
                                                Text(
                                                    text = sym,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = questionText,
                            onValueChange = { questionText = it },
                            placeholder = { Text("اكتب نص السؤال هنا بالتفصيل...") },
                            minLines = 3,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("question_text_input")
                        )
                    }

                    // 1. MCQ Options with Explicit Radio Selection for Correct Choice
                    if (questionType == "mcq") {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "الخيارات (حدد الدائرة بجانب الخيار الصحيح 🎯):",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Option A
                                McqOptionRow(
                                    label = "أ",
                                    optionText = optionA,
                                    onTextChanged = {
                                        optionA = it
                                        if (selectedMcqOption == "A") correctAnswer = "أ) $it"
                                    },
                                    isSelected = selectedMcqOption == "A",
                                    onSelect = {
                                        selectedMcqOption = "A"
                                        correctAnswer = "أ) $optionA"
                                    }
                                )

                                // Option B
                                McqOptionRow(
                                    label = "ب",
                                    optionText = optionB,
                                    onTextChanged = {
                                        optionB = it
                                        if (selectedMcqOption == "B") correctAnswer = "ب) $it"
                                    },
                                    isSelected = selectedMcqOption == "B",
                                    onSelect = {
                                        selectedMcqOption = "B"
                                        correctAnswer = "ب) $optionB"
                                    }
                                )

                                // Option C
                                McqOptionRow(
                                    label = "ج",
                                    optionText = optionC,
                                    onTextChanged = {
                                        optionC = it
                                        if (selectedMcqOption == "C") correctAnswer = "ج) $it"
                                    },
                                    isSelected = selectedMcqOption == "C",
                                    onSelect = {
                                        selectedMcqOption = "C"
                                        correctAnswer = "ج) $optionC"
                                    }
                                )

                                // Option D
                                McqOptionRow(
                                    label = "د",
                                    optionText = optionD,
                                    onTextChanged = {
                                        optionD = it
                                        if (selectedMcqOption == "D") correctAnswer = "د) $it"
                                    },
                                    isSelected = selectedMcqOption == "D",
                                    onSelect = {
                                        selectedMcqOption = "D"
                                        correctAnswer = "د) $optionD"
                                    }
                                )
                            }
                        }
                    }

                    // 2. True / False Direct Answer Selector
                    if (questionType == "true_false") {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "تحديد الإجابة النموذجية للسؤال (صح أم خطأ):",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // True Card
                                    val isTrueSelected = correctAnswer == "صح" || correctAnswer.contains("صح") || correctAnswer.contains("صحيح")
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isTrueSelected) EmeraldSuccessContainer else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(
                                            if (isTrueSelected) 2.dp else 1.dp,
                                            if (isTrueSelected) EmeraldSuccess else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                correctAnswer = "صح"
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            RadioButton(
                                                selected = isTrueSelected,
                                                onClick = { correctAnswer = "صح" },
                                                colors = RadioButtonDefaults.colors(selectedColor = EmeraldSuccess)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "صح (صواب) ✔️",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = if (isTrueSelected) EmeraldSuccess else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    // False Card
                                    val isFalseSelected = correctAnswer == "خطأ" || correctAnswer.contains("خطأ") || correctAnswer.contains("خاطئ")
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isFalseSelected) CrimsonErrorContainer else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(
                                            if (isFalseSelected) 2.dp else 1.dp,
                                            if (isFalseSelected) CrimsonError else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                correctAnswer = "خطأ"
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            RadioButton(
                                                selected = isFalseSelected,
                                                onClick = { correctAnswer = "خطأ" },
                                                colors = RadioButtonDefaults.colors(selectedColor = CrimsonError)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "خطأ (خاطئة) ❌",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = if (isFalseSelected) CrimsonError else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Essay & Model Answer / Solution
                    OutlinedTextField(
                        value = correctAnswer,
                        onValueChange = { correctAnswer = it },
                        label = { Text("الإجابة النموذجية / خطوات الحل والتفسير") },
                        placeholder = { Text("اكتب الإجابة النموذجية أو خطوات الحل الكاملة...") },
                        minLines = if (questionType == "essay") 3 else 1,
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
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إلغاء", fontWeight = FontWeight.Bold)
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
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        modifier = Modifier.weight(1.5f).testTag("save_question_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ السؤال", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Interactive 2D / 3D Geometry Board Dialog
    if (showGeometryBoard) {
        GeometryDrawingBoardDialog(
            onInsertDiagramDescription = { diagramDesc ->
                questionText += "\n[شكل توضيحي: $diagramDesc]"
                showGeometryBoard = false
            },
            onDismiss = { showGeometryBoard = false }
        )
    }
}

@Composable
private fun McqOptionRow(
    label: String,
    optionText: String,
    onTextChanged: (String) -> Unit,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) EmeraldSuccessContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) EmeraldSuccess else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Radio Button to mark as correct answer
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(selectedColor = EmeraldSuccess)
            )

            Surface(
                shape = CircleShape,
                color = if (isSelected) EmeraldSuccess else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = optionText,
                onValueChange = onTextChanged,
                placeholder = { Text("الخيار ($label)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isSelected) EmeraldSuccess else MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent
                ),
                modifier = Modifier.weight(1f)
            )

            if (isSelected) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = EmeraldSuccess,
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Text(
                        text = "الإجابة الصحيحة ✔️",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

/**
 * 2D and 3D Geometry Shape Selector & Drawing Sketch Board Dialog
 */
@Composable
fun GeometryDrawingBoardDialog(
    onInsertDiagramDescription: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedShapeCategory by remember { mutableIntStateOf(0) } // 0: 2D Shapes, 1: 3D Shapes, 2: Free Drawing
    val points = remember { mutableStateListOf<DrawPoint>() }
    var selectedColor by remember { mutableStateOf(Color(0xFF1E3A8A)) }
    var strokeWidth by remember { mutableFloatStateOf(4f) }
    var selectedShapeName by remember { mutableStateOf("") }
    var shapeDimensionsNote by remember { mutableStateOf("") }

    val shapes2D = listOf(
        "مثلث قائم الزاوية (Right Triangle)" to "📐 مثلث قائم الزاوية ABC طول القاعدة b والارتفاع h",
        "مثلث متساوي الأضلاع (Equilateral)" to "🔺 مثلث متساوي الأضلاع طول ضلعه L",
        "دائرة ونصف قطر (Circle)" to "⭕ دائرة مركزها M ونصف قطرها r",
        "مستطيل ومربع (Rectangle & Square)" to "🔲 مستطيل أبعاده الطول L والعرض W",
        "متوازي أضلاع (Parallelogram)" to "▰ متوازي أضلاع قاعدته b وارتفاعه h",
        "شبه منحرف (Trapezoid)" to "⏢ شبه منحرف قاعدتاه المتوازيتان a و b والارتفاع h",
        "شبكة إحداثيات (Cartesian Axes)" to "➕ مستوى إحداثي متعامد محوري السينات والصادات (X-Y)"
    )

    val shapes3D = listOf(
        "مكعب (Cube)" to "🎲 مكعب ثلاثي الأبعاد طول حرفه a وحجمه V = a³",
        "متوازي مستطيلات (Cuboid)" to "📦 متوازي مستطيلات أبعاده x, y, z ومساحته الكلية",
        "أسطوانة دائرية (Cylinder)" to "🥫 أسطوانة دائرية قائمة نصف قطر قاعدتها r وارتفاعها h",
        "مخروط دائري (Cone)" to "🍦 مخروط دائري قائم نصف قطر قاعدته r وارتفاعه h وراسمه L",
        "هرم رباعي (Pyramid)" to "🔺 هرم رباعي منتظم قاعدته مربعة طول ضلعها a والارتفاع h",
        "كرة ثلاثية الأبعاد (Sphere)" to "🌐 مجسم كرة نصف قطرها R وحجمها 4/3πR³"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
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
                            color = AmberGold.copy(alpha = 0.15f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Category, contentDescription = null, tint = AmberGoldDark)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "الأشكال الهندسية والرسومات (2D & 3D)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                "اختر شكلاً هندسياً أو ارسم مسألة مخصصة",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs: 2D, 3D, Freehand Canvas
                TabRow(
                    selectedTabIndex = selectedShapeCategory,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedShapeCategory == 0,
                        onClick = { selectedShapeCategory = 0 },
                        text = { Text("أشكال ثنائية 2D", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        icon = { Icon(Icons.Filled.SquareFoot, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedShapeCategory == 1,
                        onClick = { selectedShapeCategory = 1 },
                        text = { Text("مجسمات ثلاثية 3D", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        icon = { Icon(Icons.Filled.ViewInAr, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedShapeCategory == 2,
                        onClick = { selectedShapeCategory = 2 },
                        text = { Text("لوحة رسم حرة", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        icon = { Icon(Icons.Filled.Draw, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Content Based on Selected Tab
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedShapeCategory) {
                        0 -> {
                            // 2D Shapes List
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                shapes2D.forEach { (title, desc) ->
                                    val isChosen = selectedShapeName == title
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isChosen) NavyPrimaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        border = BorderStroke(if (isChosen) 2.dp else 1.dp, if (isChosen) NavyPrimary else Color.Transparent),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedShapeName = title
                                                shapeDimensionsNote = desc
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = isChosen,
                                                onClick = {
                                                    selectedShapeName = title
                                                    shapeDimensionsNote = desc
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            // 3D Shapes List
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                shapes3D.forEach { (title, desc) ->
                                    val isChosen = selectedShapeName == title
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isChosen) AmberGoldContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        border = BorderStroke(if (isChosen) 2.dp else 1.dp, if (isChosen) AmberGoldDark else Color.Transparent),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedShapeName = title
                                                shapeDimensionsNote = desc
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = isChosen,
                                                onClick = {
                                                    selectedShapeName = title
                                                    shapeDimensionsNote = desc
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            // Freehand Sketch Canvas Board
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Color & Tool Selector
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(
                                            Color(0xFF1E3A8A), Color(0xFFDC2626), Color(0xFF059669),
                                            Color(0xFFD97706), Color(0xFF1E293B)
                                        ).forEach { c ->
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(CircleShape)
                                                    .background(c)
                                                    .border(
                                                        if (selectedColor == c) 2.dp else 0.dp,
                                                        Color.White,
                                                        CircleShape
                                                    )
                                                    .clickable { selectedColor = c }
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        IconButton(onClick = { points.clear() }) {
                                            Icon(Icons.Filled.DeleteSweep, contentDescription = "مسح اللوحة", tint = Color.Red)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Drawing Canvas Box
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .pointerInput(Unit) {
                                                detectDragGestures(
                                                    onDragStart = { offset ->
                                                        points.add(DrawPoint(offset, selectedColor, strokeWidth, true))
                                                    },
                                                    onDrag = { change, _ ->
                                                        points.add(DrawPoint(change.position, selectedColor, strokeWidth, false))
                                                    }
                                                )
                                            }
                                    ) {
                                        var currentPath = Path()
                                        points.forEachIndexed { index, point ->
                                            if (point.isNewPath) {
                                                currentPath = Path()
                                                currentPath.moveTo(point.offset.x, point.offset.y)
                                            } else {
                                                currentPath.lineTo(point.offset.x, point.offset.y)
                                                drawPath(
                                                    path = currentPath,
                                                    color = point.color,
                                                    style = Stroke(width = point.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Optional Dimensions / Notes Input
                OutlinedTextField(
                    value = shapeDimensionsNote,
                    onValueChange = { shapeDimensionsNote = it },
                    label = { Text("أبعاد أو ملاحظات الشكل الهندسي") },
                    placeholder = { Text("مثال: نصف القطر r = 7 سم، زاوية القطاع = 60°") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إلغاء")
                    }

                    Button(
                        onClick = {
                            val result = if (shapeDimensionsNote.isNotBlank()) shapeDimensionsNote else selectedShapeName.ifBlank { "رسم تخطيطي هندسي" }
                            onInsertDiagramDescription(result)
                        },
                        enabled = shapeDimensionsNote.isNotBlank() || selectedShapeName.isNotBlank() || points.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إدراج في السؤال", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
