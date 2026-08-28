package com.example.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToStudents: () -> Unit,
    onNavigateToGroups: () -> Unit,
    onNavigateToSchedule: () -> Unit,
    onNavigateToAttendance: () -> Unit,
    onNavigateToFinance: () -> Unit,
    onNavigateToExams: () -> Unit,
    onNavigateToVenues: () -> Unit = {},
    onNavigateToCurriculum: () -> Unit = {},
    onNavigateToQuestionBank: () -> Unit = {},
    onNavigateToReports: () -> Unit,
    onNavigateToCertificates: () -> Unit,
    onNavigateToSmartPrep: () -> Unit = {},
    onNavigateToTeacherTools: () -> Unit = {},
    onNavigateToPoster: () -> Unit = {},
    onNavigateToStudyFiles: () -> Unit = {},
    onNavigateToProfile: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val currentLang by com.example.util.LocaleManager.currentLanguage.collectAsState()
    var showLanguageDialog by remember { mutableStateOf(false) }

    if (showLanguageDialog) {
        LanguageSelectionDialog(onDismiss = { showLanguageDialog = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = com.example.util.L.appName(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = com.example.util.L.dashboard(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // 5-Step Interactive Guided Tour Button
                    IconButton(
                        onClick = { com.example.util.AppPreferencesManager.setHasSeenTour(false) },
                        modifier = Modifier.testTag("dashboard_tour_button")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = AmberGold.copy(alpha = 0.2f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.RocketLaunch,
                                    contentDescription = "الجولة التفاعلية",
                                    tint = AmberGoldDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    // Language Switcher Button on Main Screen
                    IconButton(
                        onClick = { showLanguageDialog = true },
                        modifier = Modifier.testTag("dashboard_language_button")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = currentLang.flag,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = onNavigateToSearch,
                        modifier = Modifier.testTag("dashboard_search_button")
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = "بحث")
                    }
                    IconButton(
                        onClick = onNavigateToTeacherTools,
                        modifier = Modifier.testTag("dashboard_teacher_tools_button")
                    ) {
                        Badge(containerColor = AmberGold) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = "أدوات المعلم", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(
                        onClick = onNavigateToProfile,
                        modifier = Modifier.testTag("dashboard_profile_button")
                    ) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "الملف الشخصي")
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("dashboard_settings_button")
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = "الإعدادات")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // 1. Teacher Header Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clickable { onNavigateToProfile() }
                        .testTag("teacher_header_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.School,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.teacher?.name ?: "مرحباً بك يا أستاذ",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${state.teacher?.subject ?: "جميع المواد"} • ${state.teacher?.centerName?.ifEmpty { "التعليم الخاص" } ?: "السنتر"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.ChevronLeft,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // 1.5. PROMINENT DIRECT CREATION BAR (إضافة مجموعة - إضافة طالب - إضافة حصة)
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (com.example.util.L.isArabic()) "⚡ إجراءات سريعة فورية" else "⚡ Quick Actions",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. ADD GROUP BUTTON
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = IndigoExam.copy(alpha = 0.12f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, IndigoExam.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigateToGroups() }
                                .testTag("dashboard_quick_add_group")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(IndigoExam),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.GroupAdd,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (com.example.util.L.isArabic()) "+ إضافة مجموعة" else "+ Add Group",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = IndigoExam,
                                    maxLines = 1
                                )
                            }
                        }

                        // 2. ADD STUDENT BUTTON
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = EmeraldSuccess.copy(alpha = 0.12f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, EmeraldSuccess.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigateToStudents() }
                                .testTag("dashboard_quick_add_student")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldSuccess),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.PersonAdd,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (com.example.util.L.isArabic()) "+ إضافة طالب" else "+ Add Student",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = EmeraldSuccess,
                                    maxLines = 1
                                )
                            }
                        }

                        // 3. ADD SESSION BUTTON
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = NavyPrimary.copy(alpha = 0.12f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, NavyPrimary.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigateToSchedule() }
                                .testTag("dashboard_quick_add_session")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(NavyPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.CalendarMonth,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (com.example.util.L.isArabic()) "+ إضافة حصة" else "+ Add Session",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = NavyPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // 2. Important Alerts (if any)
            if (state.alerts.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = AmberGoldContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = AmberGold,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "تنبيهات وتذكيرات",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = AmberGold
                                )
                                Text(
                                    text = state.alerts.first(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimaryLight
                                )
                            }
                        }
                    }
                }
            }

            // 3. Homework PDF Scanner Hero Banner (1-Tap Direct Capture)
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, NavyPrimaryLight.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToTeacherTools() }
                        .testTag("hero_homework_scanner_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF3B82F6).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.CameraAlt,
                                    contentDescription = null,
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = com.example.util.L.homeworkPdfScanner(),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "تصوير كشكول الواجب وتوليد PDF باسم 'واجب - الطالب - التاريخ'",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NavyPrimary,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = "تصوير 📸",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // 4. Quick Actions Carousel
            item {
                Text(
                    text = com.example.util.L.quickActions(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        QuickActionItem(
                            title = com.example.util.L.homeworkPdfScanner(),
                            icon = Icons.Filled.CameraAlt,
                            color = Color(0xFF3B82F6),
                            onClick = onNavigateToTeacherTools
                        )
                    }
                    item {
                        QuickActionItem(
                            title = com.example.util.L.studyFiles(),
                            icon = Icons.Filled.LibraryBooks,
                            color = NavyPrimary,
                            onClick = onNavigateToStudyFiles
                        )
                    }
                    item {
                        QuickActionItem(
                            title = com.example.util.L.teacherTools(),
                            icon = Icons.Filled.AutoAwesome,
                            color = AmberGold,
                            onClick = onNavigateToTeacherTools
                        )
                    }
                    item {
                        QuickActionItem(
                            title = com.example.util.L.addStudent(),
                            icon = Icons.Filled.PersonAdd,
                            color = NavyPrimaryLight,
                            onClick = onNavigateToStudents
                        )
                    }
                    item {
                        QuickActionItem(
                            title = com.example.util.L.addGroup(),
                            icon = Icons.Filled.GroupAdd,
                            color = IndigoExam,
                            onClick = onNavigateToGroups
                        )
                    }
                    item {
                        QuickActionItem(
                            title = com.example.util.L.recordAttendance(),
                            icon = Icons.Filled.FactCheck,
                            color = EmeraldSuccess,
                            onClick = onNavigateToAttendance
                        )
                    }
                    item {
                        QuickActionItem(
                            title = com.example.util.L.addExam(),
                            icon = Icons.Filled.AssignmentTurnedIn,
                            color = AmberGold,
                            onClick = onNavigateToExams
                        )
                    }
                    item {
                        QuickActionItem(
                            title = com.example.util.L.finance(),
                            icon = Icons.Filled.AddCard,
                            color = EmeraldSuccess,
                            onClick = onNavigateToFinance
                        )
                    }
                    item {
                        QuickActionItem(
                            title = com.example.util.L.smartPrep(),
                            icon = Icons.Filled.Psychology,
                            color = NavyPrimary,
                            onClick = onNavigateToSmartPrep
                        )
                    }
                    item {
                        QuickActionItem(
                            title = com.example.util.L.questionBank(),
                            icon = Icons.Filled.Quiz,
                            color = PurpleAccent,
                            onClick = onNavigateToQuestionBank
                        )
                    }
                    item {
                        QuickActionItem(
                            title = com.example.util.L.reports(),
                            icon = Icons.Filled.PictureAsPdf,
                            color = NavyPrimary,
                            onClick = onNavigateToReports
                        )
                    }
                    item {
                        QuickActionItem(
                            title = com.example.util.L.certificates(),
                            icon = Icons.Filled.WorkspacePremium,
                            color = AmberGold,
                            onClick = onNavigateToCertificates
                        )
                    }
                }
            }

            // 5. Next Session Hero Card
            item {
                SectionHeader(
                    title = com.example.util.L.nextSession(),
                    actionText = com.example.util.L.schedule(),
                    onActionClick = onNavigateToSchedule
                )
                if (state.nextSession != null && state.nextSessionGroup != null) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth().testTag("next_session_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = state.nextSessionGroup!!.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(NavyPrimaryContainer)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${state.nextSession!!.time}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = NavyPrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = state.nextSessionGroup!!.location.ifEmpty { "سنتر التفوق" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Icon(
                                    Icons.Filled.Timer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${state.nextSession!!.durationMinutes} دقيقة",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Homework assigned in next session (if any)
                            val nextHw = state.nextSession!!.homeworkTitle
                            val nextPages = state.nextSession!!.homeworkPages
                            if (nextHw.isNotEmpty() || nextPages.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AmberGoldContainer.copy(alpha = 0.5f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.MenuBook,
                                            contentDescription = null,
                                            tint = AmberGoldDark,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "الواجب المطلوب: " + if (nextHw.isNotEmpty()) nextHw else "صفحات $nextPages",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = AmberGoldDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onNavigateToAttendance,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).testTag("start_attendance_btn")
                                ) {
                                    Icon(Icons.Filled.FactCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تسجيل الحضور")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.completeSession(state.nextSession!!.id) },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("تمت الحصة")
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "لا توجد حصص مجدولة حالياً",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(onClick = onNavigateToSchedule) {
                                Text("+ إضافة موعد حصة جديد")
                            }
                        }
                    }
                }
            }

            // 5. Four Key Metrics
            item {
                Text(
                    text = "الإحصائيات العامة",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "الطلاب",
                        value = "${state.studentCount}",
                        icon = Icons.Filled.People,
                        contentColor = NavyPrimaryLight,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToStudents
                    )
                    MetricCard(
                        title = "المجموعات",
                        value = "${state.groupCount}",
                        icon = Icons.Filled.Class,
                        contentColor = IndigoExam,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToGroups
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "حصص اليوم",
                        value = "${state.todaySessionsCount}",
                        icon = Icons.Filled.CalendarToday,
                        contentColor = EmeraldSuccess,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToSchedule
                    )
                    MetricCard(
                        title = "الغياب اليوم",
                        value = "${state.todayAbsentsCount}",
                        icon = Icons.Filled.PersonOff,
                        contentColor = CrimsonError,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToAttendance
                    )
                }
            }

            // 5.5. Weekly Analytics & Performance Charts
            item {
                DashboardChartsCard(
                    studentData = state.weeklyStudentData,
                    attendanceData = state.attendanceSummaryData,
                    examData = state.examPerformanceData
                )
            }

            // 6. Financial Summary Card
            item {
                SectionHeader(
                    title = "المركز المالي",
                    actionText = "تفاصيل المالية",
                    onActionClick = onNavigateToFinance
                )
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().testTag("financial_summary_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "صافي الدخل",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${state.netProfit} ج.م",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (state.netProfit >= 0) EmeraldSuccess else CrimsonError
                                    )
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldSuccessContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = EmeraldSuccess
                                )
                            }
                        }
                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("إجمالي المدفوعات", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${state.totalRevenue} ج.م", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = EmeraldSuccess)
                            }
                            Column {
                                Text("إجمالي المصروفات", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${state.totalExpenses} ج.م", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = CrimsonError)
                            }
                        }
                    }
                }
            }

            // 7. Upcoming Exams Section
            item {
                SectionHeader(
                    title = "الامتحانات القادمة",
                    actionText = "كل الامتحانات",
                    onActionClick = onNavigateToExams
                )
                if (state.upcomingExams.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.upcomingExams.forEach { examItem ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth().clickable { onNavigateToExams() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = examItem.exam.title,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${examItem.groupName} • ${examItem.exam.date}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(AmberGoldContainer)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "الدرجة: ${examItem.exam.maxScore.toInt()}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = AmberGold
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    EmptyStateWidget(
                        title = "لا توجد امتحانات مسجلة",
                        description = "يمكنك إنشاء اختبار وتحديد الدرجات وتقييم مستوى الطلاب",
                        icon = Icons.Filled.Assignment,
                        actionText = "إضافة امتحان",
                        onActionClick = onNavigateToExams
                    )
                }
            }

            // 8. Sample data button if empty
            if (state.studentCount == 0 && state.groupCount == 0) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = NavyPrimaryContainer),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "مرحباً بك في ${com.example.util.L.appName()}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = NavyPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "هل ترغب في ملء بيانات تجريبية (مجموعات، طلاب، حصص، امتحانات، مدفوعات) لاستكشاف التطبيق وتجربته؟",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = TextPrimaryLight
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.loadSampleData() },
                                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Filled.AutoFixHigh, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تعبئة بيانات توضيحية كاملة")
                            }
                        }
                    }
                }
            }
        }
    }
}
