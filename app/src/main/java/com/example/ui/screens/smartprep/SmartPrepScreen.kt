package com.example.ui.screens.smartprep

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.LessonPlanEntity
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateWidget
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AmberGoldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.EmeraldSuccessContainer
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavyPrimaryContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartPrepScreen(
    viewModel: SmartPrepViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var planToEdit by remember { mutableStateOf<LessonPlanEntity?>(null) }
    var planToDelete by remember { mutableStateOf<LessonPlanEntity?>(null) }

    var selectedStatusFilter by remember { mutableStateOf("ALL") } // ALL, PENDING, COMPLETED

    val displayedPlans = remember(state.filteredPlans, selectedStatusFilter) {
        when (selectedStatusFilter) {
            "PENDING" -> state.filteredPlans.filter { !it.isCompleted }
            "COMPLETED" -> state.filteredPlans.filter { it.isCompleted }
            else -> state.filteredPlans
        }
    }

    val totalCount = state.plans.size
    val completedCount = state.plans.count { it.isCompleted }
    val pendingCount = totalCount - completedCount

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("التحضير الذكي للدروس", fontWeight = FontWeight.Bold)
                        Text(
                            "تنظيم الأفكار والأهداف واستراتيجيات الشرح",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { showAddDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("add_lesson_plan_top_btn")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تحضير جديد", style = MaterialTheme.typography.labelMedium)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = NavyPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_lesson_plan_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "إضافة تحضير")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // 1. Search Bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("بحث في التحضيرات والأهداف والأفكار...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "مسح")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )

            // 2. Statistics & Filter Row
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChipItem(
                        title = "الكل",
                        count = totalCount,
                        isSelected = selectedStatusFilter == "ALL",
                        onClick = { selectedStatusFilter = "ALL" }
                    )
                    FilterChipItem(
                        title = "قيد الشرح",
                        count = pendingCount,
                        isSelected = selectedStatusFilter == "PENDING",
                        color = AmberGold,
                        onClick = { selectedStatusFilter = "PENDING" }
                    )
                    FilterChipItem(
                        title = "تم الشرح",
                        count = completedCount,
                        isSelected = selectedStatusFilter == "COMPLETED",
                        color = EmeraldSuccess,
                        onClick = { selectedStatusFilter = "COMPLETED" }
                    )
                }
            }

            // 3. Lesson Plans List
            if (displayedPlans.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp, top = 4.dp)
                ) {
                    items(displayedPlans, key = { it.id }) { plan ->
                        LessonPlanCard(
                            plan = plan,
                            onToggleCompleted = { viewModel.toggleCompleted(plan) },
                            onEdit = { planToEdit = plan },
                            onDelete = { planToDelete = plan },
                            onShare = {
                                val shareText = buildShareableLessonText(plan)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, plan.title)
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(intent, "مشاركة خطة الدرس"))
                            }
                        )
                    }
                }
            } else {
                EmptyStateWidget(
                    title = if (state.searchQuery.isNotEmpty()) "لا توجد نتائج مطابقة للبحث" else "لا توجد تحضيرات للدروس بعد",
                    description = if (state.searchQuery.isNotEmpty()) "جرب البحث بمصطلحات أخرى" else "استخدم التحضير الذكي لتنظيم أفكار دروسك وتوليد الأهداف والأسئلة بضغطة زر واحدة!",
                    icon = Icons.Filled.Psychology,
                    actionText = "+ إنشاء تحضير درس ذكي",
                    onActionClick = { showAddDialog = true }
                )
            }
        }
    }

    // Add Plan Dialog
    if (showAddDialog) {
        AddEditLessonPlanDialog(
            groups = state.groups,
            teacher = state.teacher,
            onGenerateSmart = { sub, gr, top -> viewModel.generateSmartTemplate(sub, gr, top) },
            onGenerateAi = { sub, gr, top, dur -> viewModel.generateAiPlan(sub, gr, top, dur) },
            onDismiss = { showAddDialog = false },
            onSave = { viewModel.savePlan(it) }
        )
    }

    // Edit Plan Dialog
    planToEdit?.let { plan ->
        AddEditLessonPlanDialog(
            plan = plan,
            groups = state.groups,
            teacher = state.teacher,
            onGenerateSmart = { sub, gr, top -> viewModel.generateSmartTemplate(sub, gr, top) },
            onGenerateAi = { sub, gr, top, dur -> viewModel.generateAiPlan(sub, gr, top, dur) },
            onDismiss = { planToEdit = null },
            onSave = { viewModel.savePlan(it) }
        )
    }

    // Delete confirmation dialog
    planToDelete?.let { plan ->
        ConfirmDeleteDialog(
            title = "حذف تحضير الدرس",
            message = "هل أنت متأكد من رغبتك في حذف تحضير (${plan.title})؟",
            onConfirm = {
                viewModel.deletePlan(plan)
                planToDelete = null
                Toast.makeText(context, "تم حذف التحضير بنجاح", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { planToDelete = null }
        )
    }
}

@Composable
private fun FilterChipItem(
    title: String,
    count: Int,
    isSelected: Boolean,
    color: Color = NavyPrimary,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, color) else null,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                shape = CircleShape,
                color = if (isSelected) color else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun LessonPlanCard(
    plan: LessonPlanEntity,
    onToggleCompleted: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (plan.isCompleted) EmeraldSuccessContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Checkbox, Title, Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = plan.isCompleted,
                    onCheckedChange = { onToggleCompleted() },
                    colors = CheckboxDefaults.colors(checkedColor = EmeraldSuccess)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plan.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (plan.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (plan.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${plan.subject} • ${plan.grade} • تاريخ: ${plan.targetDate} (${plan.durationMinutes} دقيقة)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) "طي" else "توسيع",
                        tint = NavyPrimary
                    )
                }
            }

            // Quick Peek Objectives when collapsed
            if (!isExpanded && plan.objectives.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = plan.objectives.lines().firstOrNull() ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            // Expanded Detailed Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Divider()

                    // 1. Objectives
                    if (plan.objectives.isNotBlank()) {
                        PlanDetailSection(
                            icon = Icons.Filled.CheckCircle,
                            title = "الأهداف التعليمية ومخرجات التعلم",
                            content = plan.objectives,
                            tint = EmeraldSuccess
                        )
                    }

                    // 2. Warmup Hook
                    if (plan.warmupHook.isNotBlank()) {
                        PlanDetailSection(
                            icon = Icons.Filled.Lightbulb,
                            title = "التهيئة والعصف الذهني (التمهيد)",
                            content = plan.warmupHook,
                            tint = AmberGold
                        )
                    }

                    // 3. Key Points & Concept Map
                    if (plan.keyPoints.isNotBlank()) {
                        PlanDetailSection(
                            icon = Icons.Filled.FormatListNumbered,
                            title = "عناصر الدرس وشرح الأفكار الأساسية",
                            content = plan.keyPoints,
                            tint = NavyPrimary
                        )
                    }

                    // 4. Teaching Aids & Activities
                    if (plan.teachingAids.isNotBlank() || plan.activities.isNotBlank()) {
                        PlanDetailSection(
                            icon = Icons.Filled.Psychology,
                            title = "الوسائل والأنشطة واستراتيجيات التدريس",
                            content = buildString {
                                if (plan.teachingAids.isNotBlank()) appendLine("• الوسائل: ${plan.teachingAids}")
                                if (plan.activities.isNotBlank()) append(plan.activities)
                            }.trim(),
                            tint = NavyPrimary
                        )
                    }

                    // 5. Assessment Questions
                    if (plan.assessmentQuestions.isNotBlank()) {
                        PlanDetailSection(
                            icon = Icons.Filled.Quiz,
                            title = "أسئلة التقويم وقياس الفهم",
                            content = plan.assessmentQuestions,
                            tint = AmberGold
                        )
                    }

                    // 6. Homework & Notes
                    if (plan.homework.isNotBlank() || plan.notes.isNotBlank()) {
                        PlanDetailSection(
                            icon = Icons.Filled.Assignment,
                            title = "التكليفات والواجب والملاحظات",
                            content = buildString {
                                if (plan.homework.isNotBlank()) appendLine("الواجب: ${plan.homework}")
                                if (plan.notes.isNotBlank()) appendLine("ملاحظات: ${plan.notes}")
                            }.trim(),
                            tint = EmeraldSuccess
                        )
                    }
                }
            }

            // Bottom Action Bar
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Share action
                FilledTonalButton(
                    onClick = onShare,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مشاركة الخطة", style = MaterialTheme.typography.labelSmall)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "تعديل", tint = NavyPrimary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanDetailSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: String,
    tint: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = tint
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun buildShareableLessonText(plan: LessonPlanEntity): String {
    return buildString {
        appendLine("📋 【${plan.title}】")
        appendLine("📚 المادة: ${plan.subject} | الصف: ${plan.grade}")
        appendLine("📅 تاريخ الشرح: ${plan.targetDate} | المدة: ${plan.durationMinutes} دقيقة")
        appendLine("━━━━━━━━━━━━━━━━━━━")
        if (plan.objectives.isNotBlank()) {
            appendLine("🎯 أهداف الدرس:")
            appendLine(plan.objectives)
            appendLine()
        }
        if (plan.warmupHook.isNotBlank()) {
            appendLine("💡 التهيئة والتمهيد:")
            appendLine(plan.warmupHook)
            appendLine()
        }
        if (plan.keyPoints.isNotBlank()) {
            appendLine("📝 عناصر الدرس ونقاط الشرح:")
            appendLine(plan.keyPoints)
            appendLine()
        }
        if (plan.teachingAids.isNotBlank() || plan.activities.isNotBlank()) {
            appendLine("🛠️ الوسائل والأنشطة:")
            if (plan.teachingAids.isNotBlank()) appendLine("• الوسائل: ${plan.teachingAids}")
            if (plan.activities.isNotBlank()) appendLine(plan.activities)
            appendLine()
        }
        if (plan.assessmentQuestions.isNotBlank()) {
            appendLine("❓ أسئلة التقويم:")
            appendLine(plan.assessmentQuestions)
            appendLine()
        }
        if (plan.homework.isNotBlank()) {
            appendLine("📌 الواجب المنزلي: ${plan.homework}")
        }
        if (plan.notes.isNotBlank()) {
            appendLine("✍️ ملاحظات: ${plan.notes}")
        }
        appendLine("━━━━━━━━━━━━━━━━━━━")
        appendLine("تم التحضير بواسطة تطبيق هاكر التدريس (The Hacker)")
    }
}
