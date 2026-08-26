package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import com.example.util.L

data class GradeStageCategory(
    val stageName: String,
    val iconText: String,
    val grades: List<String>,
    val badgeColor: Color
)

object EducationalStages {
    val PRIMARY_GRADES = listOf(
        "الصف الأول الابتدائي",
        "الصف الثاني الابتدائي",
        "الصف الثالث الابتدائي",
        "الصف الرابع الابتدائي",
        "الصف الخامس الابتدائي",
        "الصف السادس الابتدائي"
    )

    val PREPARATORY_GRADES = listOf(
        "الصف الأول الإعدادي",
        "الصف الثاني الإعدادي",
        "الصف الثالث الإعدادي"
    )

    val SECONDARY_GRADES = listOf(
        "الصف الأول الثانوي",
        "الصف الثاني الثانوي",
        "الصف الثالث الثانوي"
    )

    val OTHER_GRADES = listOf(
        "رياض أطفال (KG)",
        "تأسيس وتقوية",
        "تعليم حر / دورات",
        "جامعي / دبلوم",
        "عام / غير محدد"
    )

    fun getStages(): List<GradeStageCategory> = listOf(
        GradeStageCategory(L.primaryStage(), "🎒", PRIMARY_GRADES, Color(0xFF10B981)),
        GradeStageCategory(L.preparatoryStage(), "📚", PREPARATORY_GRADES, Color(0xFF3B82F6)),
        GradeStageCategory(L.secondaryStage(), "🎓", SECONDARY_GRADES, Color(0xFF8B5CF6)),
        GradeStageCategory(L.otherStage(), "🌟", OTHER_GRADES, Color(0xFFF59E0B))
    )

    val STAGES = listOf(
        GradeStageCategory("المرحلة الابتدائية (1 - 6)", "🎒", PRIMARY_GRADES, Color(0xFF10B981)),
        GradeStageCategory("المرحلة الإعدادية (1 - 3)", "📚", PREPARATORY_GRADES, Color(0xFF3B82F6)),
        GradeStageCategory("المرحلة الثانوية (1 - 3)", "🎓", SECONDARY_GRADES, Color(0xFF8B5CF6)),
        GradeStageCategory("أخرى / عام", "🌟", OTHER_GRADES, Color(0xFFF59E0B))
    )

    val ALL_GRADES: List<String> by lazy {
        PRIMARY_GRADES + PREPARATORY_GRADES + SECONDARY_GRADES + OTHER_GRADES
    }

    fun getStageForGrade(grade: String): String {
        return when {
            PRIMARY_GRADES.any { grade.contains(it) || it.contains(grade) } -> "الابتدائية"
            PREPARATORY_GRADES.any { grade.contains(it) || it.contains(grade) } -> "الإعدادية"
            SECONDARY_GRADES.any { grade.contains(it) || it.contains(grade) } -> "الثانوية"
            else -> "عام"
        }
    }
}

/**
 * Modern Outlined/Filled Dropdown Field for Grade/Stage Selection with grouped dialog
 */
@Composable
fun GradeStageSelectorField(
    selectedGrade: String,
    onGradeSelected: (String) -> Unit,
    label: String = "الصف الدراسي / المرحلة",
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    var showDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        OutlinedCard(
            onClick = { showDialog = true },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(
                1.dp,
                if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("grade_stage_selector_field")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (selectedGrade.isNotBlank()) L.localizedGrade(selectedGrade) else "اختر الصف الدراسي...",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (selectedGrade.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            )
        }
    }

    if (showDialog) {
        GradeStagePickerDialog(
            selectedGrade = selectedGrade,
            onGradeSelected = {
                onGradeSelected(it)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

/**
 * Dialog presenting all educational stages clearly with tabs and easy selection
 */
@Composable
fun GradeStagePickerDialog(
    selectedGrade: String,
    onGradeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val stages = remember { EducationalStages.getStages() }
    var selectedStageIndex by remember {
        val idx = stages.indexOfFirst { stage ->
            stage.grades.any { it == selectedGrade || it.contains(selectedGrade) }
        }
        mutableIntStateOf(if (idx >= 0) idx else 0)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("grade_stage_picker_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.Class,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "تحديد الصف الدراسي 🏫",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "اختر المرحلة والصف المناسب بسهولة",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "إغلاق")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Stage Tab Selector
                ScrollableTabRow(
                    selectedTabIndex = selectedStageIndex,
                    edgePadding = 0.dp,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    divider = {}
                ) {
                    stages.forEachIndexed { index, stage ->
                        val isSelected = selectedStageIndex == index
                        Tab(
                            selected = isSelected,
                            onClick = { selectedStageIndex = index },
                            text = {
                                Text(
                                    text = "${stage.iconText} ${stage.stageName.substringBefore('(').trim()}",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            }
                        )
                    }
                }

                val currentStage = stages[selectedStageIndex]

                Text(
                    text = "صفوف ${currentStage.stageName}:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = currentStage.badgeColor
                )

                // Grades List under Selected Stage
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    currentStage.grades.forEach { gradeName ->
                        val isSelected = selectedGrade == gradeName
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) currentStage.badgeColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(
                                1.5.dp,
                                if (isSelected) currentStage.badgeColor else Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onGradeSelected(gradeName)
                                }
                                .testTag("grade_option_${gradeName.replace(" ", "_")}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.School,
                                        contentDescription = null,
                                        tint = if (isSelected) currentStage.badgeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = L.localizedGrade(gradeName),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isSelected) currentStage.badgeColor else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (isSelected) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = currentStage.badgeColor
                                    ) {
                                        Text(
                                            text = "محدد",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Filter bar with chips for choosing grade in list screens
 */
@Composable
fun GradeStageFilterBar(
    selectedGrade: String,
    onGradeSelected: (String) -> Unit,
    showAllOption: Boolean = true,
    modifier: Modifier = Modifier
) {
    val options = remember {
        if (showAllOption) listOf("الكل") + EducationalStages.ALL_GRADES else EducationalStages.ALL_GRADES
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(options) { grade ->
            val isSelected = selectedGrade == grade || (selectedGrade.isBlank() && grade == "الكل")
            FilterChip(
                selected = isSelected,
                onClick = { onGradeSelected(grade) },
                label = {
                    Text(
                        text = L.localizedGrade(grade),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null,
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}
