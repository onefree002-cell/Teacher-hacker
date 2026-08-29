package com.example.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.data.backup.BackupManager
import com.example.data.backup.OldAppMigrationManager
import com.example.data.export.ExcelExporter
import com.example.data.export.PdfReportExporter
import com.example.data.local.AppDatabase
import com.example.data.repository.TeacherPlannerRepository
import com.example.navigation.Screen
import com.example.ui.screens.attendance.AttendanceScreen
import com.example.ui.screens.attendance.AttendanceViewModel
import com.example.ui.screens.backup.BackupScreen
import com.example.ui.screens.backup.BackupViewModel
import com.example.ui.screens.certificates.CertificatesScreen
import com.example.ui.screens.certificates.CertificatesViewModel
import com.example.ui.screens.curriculum.CurriculumScreen
import com.example.ui.screens.curriculum.CurriculumViewModel
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.dashboard.DashboardViewModel
import com.example.ui.screens.exams.ExamDetailScreen
import com.example.ui.screens.exams.ExamsScreen
import com.example.ui.screens.exams.ExamsViewModel
import com.example.ui.screens.studyfiles.PdfViewerScreen
import com.example.ui.screens.studyfiles.StudyFilesScreen
import com.example.ui.screens.studyfiles.StudyFilesViewModel
import com.example.ui.screens.finance.FinanceScreen
import com.example.ui.screens.finance.FinanceViewModel
import com.example.ui.screens.groups.GroupDetailScreen
import com.example.ui.screens.groups.GroupsScreen
import com.example.ui.screens.groups.GroupsViewModel
import com.example.ui.screens.onboarding.OnboardingScreen
import com.example.ui.screens.poster.AppPosterScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.profile.ProfileViewModel
import com.example.ui.screens.questions.QuestionBankScreen
import com.example.ui.screens.questions.QuestionBankViewModel
import com.example.ui.screens.reports.ReportsScreen
import com.example.ui.screens.reports.ReportsViewModel
import com.example.ui.screens.schedule.ScheduleScreen
import com.example.ui.screens.schedule.ScheduleViewModel
import com.example.ui.screens.search.SearchScreen
import com.example.ui.screens.search.SearchViewModel
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.smartprep.SmartPrepScreen
import com.example.ui.screens.smartprep.SmartPrepViewModel
import com.example.ui.screens.students.StudentDetailScreen
import com.example.ui.screens.students.StudentsScreen
import com.example.ui.screens.students.StudentsViewModel
import com.example.ui.screens.tools.TeacherToolsScreen
import com.example.ui.screens.tools.TeacherToolsViewModel
import com.example.ui.screens.venues.VenuesScreen
import com.example.ui.screens.venues.VenuesViewModel
import com.example.util.AppPreferencesManager
import com.example.util.AudioPlayerManager
import com.example.util.AudioRecordManager

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getInstance(context) }
    val repository = remember { TeacherPlannerRepository(database) }
    val pdfExporter = remember { PdfReportExporter() }
    val excelExporter = remember { ExcelExporter(database) }
    val backupManager = remember { BackupManager(database) }
    val oldAppMigrationManager = remember { OldAppMigrationManager(database) }
    val audioRecordManager = remember { AudioRecordManager(context) }
    val audioPlayerManager = remember { AudioPlayerManager(context) }

    // Instantiate ViewModels
    val dashboardViewModel = remember { DashboardViewModel(repository) }
    val groupsViewModel = remember { GroupsViewModel(repository) }
    val studentsViewModel = remember { StudentsViewModel(repository) }
    val scheduleViewModel = remember { ScheduleViewModel(repository) }
    val attendanceViewModel = remember { AttendanceViewModel(repository) }
    val examsViewModel = remember { ExamsViewModel(repository) }
    val financeViewModel = remember { FinanceViewModel(repository) }
    val reportsViewModel = remember { ReportsViewModel(repository, pdfExporter, excelExporter) }
    val certificatesViewModel = remember { CertificatesViewModel(repository, pdfExporter) }
    val venuesViewModel = remember { VenuesViewModel(repository) }
    val curriculumViewModel = remember { CurriculumViewModel(repository) }
    val questionBankViewModel = remember { QuestionBankViewModel(repository) }
    val profileViewModel = remember { ProfileViewModel(repository) }
    val smartPrepViewModel = remember { SmartPrepViewModel(repository) }
    val backupViewModel = remember { BackupViewModel(repository, backupManager, oldAppMigrationManager) }
    val searchViewModel = remember { SearchViewModel(repository) }
    val teacherToolsViewModel = remember { TeacherToolsViewModel(repository, pdfExporter, audioRecordManager, audioPlayerManager) }
    val studyFilesViewModel = remember { StudyFilesViewModel(repository) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val hasSeenTour by AppPreferencesManager.hasSeenTour.collectAsState()
    var showGuidedTourDialog by remember { mutableStateOf(false) }

    LaunchedEffect(hasSeenTour, currentRoute) {
        if (!hasSeenTour && currentRoute == Screen.Dashboard.route) {
            showGuidedTourDialog = true
        }
    }

    if (showGuidedTourDialog) {
        com.example.ui.components.AppGuidedTourDialog(
            repository = repository,
            groupsViewModel = groupsViewModel,
            studentsViewModel = studentsViewModel,
            attendanceViewModel = attendanceViewModel,
            onDismiss = {
                showGuidedTourDialog = false
                AppPreferencesManager.setHasSeenTour(true)
            },
            onNavigateToScreen = { route ->
                showGuidedTourDialog = false
                AppPreferencesManager.setHasSeenTour(true)
                navController.navigate(route)
            }
        )
    }

    val startDestination = Screen.Dashboard.route

    val shouldShowBottomBar = currentRoute != null &&
            currentRoute != Screen.Onboarding.route &&
            (Screen.bottomNavItems.any { it.route == currentRoute } || currentRoute.startsWith("teacher_tools"))

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    Screen.bottomNavItems.forEach { screen ->
                        val isSelected = when (screen) {
                            is Screen.TeacherTools -> currentRoute?.startsWith("teacher_tools") == true
                            else -> currentRoute == screen.route
                        }
                        val navIcon = if (isSelected) (screen.selectedIcon ?: screen.unselectedIcon) else (screen.unselectedIcon ?: screen.selectedIcon)
                        NavigationBarItem(
                            icon = {
                                if (navIcon != null) {
                                    Icon(
                                        imageVector = navIcon,
                                        contentDescription = screen.getLocalizedTitle()
                                    )
                                }
                            },
                            label = { Text(screen.getLocalizedTitle(), maxLines = 1) },
                            selected = isSelected,
                            onClick = {
                                if (screen is Screen.Dashboard) {
                                    navController.navigate(Screen.Dashboard.route) {
                                        popUpTo(Screen.Dashboard.route) {
                                            inclusive = false
                                        }
                                        launchSingleTop = true
                                    }
                                } else {
                                    val targetRoute = when (screen) {
                                        is Screen.TeacherTools -> Screen.TeacherTools.createRoute()
                                        else -> screen.route
                                    }
                                    if (!isSelected) {
                                        navController.navigate(targetRoute) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.testTag("nav_item_${screen.route.substringBefore('?')}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            val navigateToHome: () -> Unit = {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(0) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }

            // 0. Onboarding (First-time Profile Setup)
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    repository = repository,
                    onComplete = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Onboarding.route) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }

            // 1. Dashboard
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToStudents = { navController.navigate(Screen.Students.route) },
                    onNavigateToGroups = { navController.navigate(Screen.Groups.route) },
                    onNavigateToSchedule = { navController.navigate(Screen.Schedule.route) },
                    onNavigateToAttendance = { navController.navigate(Screen.Attendance.route) },
                    onNavigateToFinance = { navController.navigate(Screen.Finance.route) },
                    onNavigateToExams = { navController.navigate(Screen.Exams.route) },
                    onNavigateToVenues = { navController.navigate(Screen.Venues.route) },
                    onNavigateToCurriculum = { navController.navigate(Screen.Curriculum.route) },
                    onNavigateToQuestionBank = { navController.navigate(Screen.QuestionBank.route) },
                    onNavigateToReports = { navController.navigate(Screen.Reports.createRoute()) },
                    onNavigateToCertificates = { navController.navigate(Screen.Certificates.createRoute()) },
                    onNavigateToSmartPrep = { navController.navigate(Screen.SmartPrep.route) },
                    onNavigateToTeacherTools = { navController.navigate(Screen.TeacherTools.route) },
                    onNavigateToPoster = { navController.navigate(Screen.Poster.route) },
                    onNavigateToStudyFiles = { navController.navigate(Screen.StudyFiles.createRoute()) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    onNavigateToBackup = { navController.navigate(Screen.Backup.route) },
                    onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            // 2. Schedule
            composable(Screen.Schedule.route) {
                ScheduleScreen(
                    viewModel = scheduleViewModel,
                    onNavigateToAttendance = { groupId, date ->
                        navController.navigate(Screen.Attendance.createRoute(groupId, date))
                    },
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateHome = navigateToHome
                )
            }

            // 3. Attendance
            composable(
                route = Screen.Attendance.route,
                arguments = listOf(
                    navArgument("groupId") {
                        type = NavType.LongType
                        defaultValue = 0L
                    },
                    navArgument("date") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val initialGroupId = backStackEntry.arguments?.getLong("groupId") ?: 0L
                val initialDate = backStackEntry.arguments?.getString("date") ?: ""
                AttendanceScreen(
                    viewModel = attendanceViewModel,
                    initialGroupId = initialGroupId,
                    initialDate = initialDate,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateHome = navigateToHome,
                    onNavigateToGroup = { gId ->
                        navController.navigate(Screen.GroupDetail.createRoute(gId))
                    }
                )
            }

            // 4. Students
            composable(Screen.Students.route) {
                StudentsScreen(
                    viewModel = studentsViewModel,
                    onNavigateToStudentDetail = { id ->
                        navController.navigate(Screen.StudentDetail.createRoute(id))
                    },
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateHome = navigateToHome
                )
            }

            // 5. Finance
            composable(Screen.Finance.route) {
                FinanceScreen(
                    viewModel = financeViewModel
                )
            }

            // 6. Groups List
            composable(Screen.Groups.route) {
                GroupsScreen(
                    viewModel = groupsViewModel,
                    onNavigateToGroupDetail = { id ->
                        navController.navigate(Screen.GroupDetail.createRoute(id))
                    },
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateHome = navigateToHome
                )
            }

            // 7. Student Detail
            composable(
                route = Screen.StudentDetail.route,
                arguments = listOf(navArgument("studentId") { type = NavType.LongType })
            ) { backStackEntry ->
                val studentId = backStackEntry.arguments?.getLong("studentId") ?: 0L
                StudentDetailScreen(
                    studentId = studentId,
                    viewModel = studentsViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateHome = navigateToHome,
                    onNavigateToReportBuilder = { sId ->
                        navController.navigate(Screen.Reports.createRoute(sId))
                    },
                    onNavigateToCertificateDesigner = { sId ->
                        navController.navigate(Screen.Certificates.createRoute(sId))
                    },
                    onNavigateToHomeworkScanner = { sId ->
                        navController.navigate(Screen.TeacherTools.createRoute(studentId = sId, tabIndex = 0))
                    },
                    onOpenHomeworkInPdfViewer = { filePath, title ->
                        navController.navigate(Screen.PdfViewer.createRoute(filePath, title))
                    },
                    onNavigateToGroup = { gId ->
                        navController.navigate(Screen.GroupDetail.createRoute(gId))
                    }
                )
            }

            // 8. Group Detail
            composable(
                route = Screen.GroupDetail.route,
                arguments = listOf(navArgument("groupId") { type = NavType.LongType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getLong("groupId") ?: 0L
                GroupDetailScreen(
                    groupId = groupId,
                    viewModel = groupsViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateHome = navigateToHome,
                    onNavigateToStudentDetail = { studentId ->
                        navController.navigate(Screen.StudentDetail.createRoute(studentId))
                    },
                    onNavigateToAttendance = { navController.navigate(Screen.Attendance.route) },
                    onNavigateToStudyFiles = { grade ->
                        navController.navigate(Screen.StudyFiles.createRoute(grade))
                    },
                    onOpenFileInViewer = { filePath, title ->
                        navController.navigate(Screen.PdfViewer.createRoute(filePath, title))
                    }
                )
            }

            // 9. Exams List
            composable(Screen.Exams.route) {
                ExamsScreen(
                    viewModel = examsViewModel,
                    onNavigateToExamDetail = { id ->
                        navController.navigate(Screen.ExamDetail.createRoute(id))
                    }
                )
            }

            // 10. Exam Detail
            composable(
                route = Screen.ExamDetail.route,
                arguments = listOf(navArgument("examId") { type = NavType.LongType })
            ) { backStackEntry ->
                val examId = backStackEntry.arguments?.getLong("examId") ?: 0L
                ExamDetailScreen(
                    examId = examId,
                    viewModel = examsViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCertificate = { sId ->
                        navController.navigate(Screen.Certificates.createRoute(sId))
                    }
                )
            }

            // 11. Reports Center
            composable(
                route = Screen.Reports.route,
                arguments = listOf(navArgument("studentId") {
                    type = NavType.LongType
                    defaultValue = 0L
                })
            ) { backStackEntry ->
                val studentId = backStackEntry.arguments?.getLong("studentId") ?: 0L
                ReportsScreen(
                    viewModel = reportsViewModel,
                    initialStudentId = studentId
                )
            }

            // 12. Certificates Designer
            composable(
                route = Screen.Certificates.route,
                arguments = listOf(navArgument("studentId") {
                    type = NavType.LongType
                    defaultValue = 0L
                })
            ) { backStackEntry ->
                val studentId = backStackEntry.arguments?.getLong("studentId") ?: 0L
                CertificatesScreen(
                    viewModel = certificatesViewModel,
                    initialStudentId = studentId
                )
            }

            // 13. Profile
            composable(Screen.Profile.route) {
                ProfileScreen(
                    viewModel = profileViewModel
                )
            }

            // 14. Backup & Migration
            composable(Screen.Backup.route) {
                BackupScreen(
                    viewModel = backupViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateHome = navigateToHome
                )
            }

            // 15. Search
            composable(Screen.Search.route) {
                SearchScreen(
                    viewModel = searchViewModel,
                    onNavigateToStudent = { id -> navController.navigate(Screen.StudentDetail.createRoute(id)) },
                    onNavigateToGroup = { id -> navController.navigate(Screen.GroupDetail.createRoute(id)) },
                    onNavigateToExam = { id -> navController.navigate(Screen.ExamDetail.createRoute(id)) }
                )
            }

            // 16. Venues (أماكن وقاعات الدروس)
            composable(Screen.Venues.route) {
                VenuesScreen(
                    viewModel = venuesViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 17. Curriculum (خطة المنهج والدروس)
            composable(Screen.Curriculum.route) {
                CurriculumScreen(
                    viewModel = curriculumViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 18. Question Bank & Sheet Generator (بنك الأسئلة والشيتات)
            composable(Screen.QuestionBank.route) {
                QuestionBankScreen(
                    viewModel = questionBankViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 19. Settings
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    onNavigateToBackup = { navController.navigate(Screen.Backup.route) },
                    onNavigateToVenues = { navController.navigate(Screen.Venues.route) },
                    onNavigateToCurriculum = { navController.navigate(Screen.Curriculum.route) },
                    onNavigateToQuestionBank = { navController.navigate(Screen.QuestionBank.route) },
                    onNavigateToReports = { navController.navigate(Screen.Reports.createRoute()) },
                    onNavigateToCertificates = { navController.navigate(Screen.Certificates.createRoute()) },
                    onNavigateToSmartPrep = { navController.navigate(Screen.SmartPrep.route) },
                    onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                    onNavigateToPoster = { navController.navigate(Screen.Poster.route) }
                )
            }

            // 20. Teacher Tools Hub (أدوات المعلم)
            composable(
                route = Screen.TeacherTools.route,
                arguments = listOf(
                    navArgument("studentId") {
                        type = NavType.LongType
                        defaultValue = 0L
                    },
                    navArgument("tabIndex") {
                        type = NavType.IntType
                        defaultValue = -1
                    }
                )
            ) { backStackEntry ->
                val sId = backStackEntry.arguments?.getLong("studentId") ?: 0L
                val tabIdx = backStackEntry.arguments?.getInt("tabIndex") ?: -1
                val targetTab = remember(tabIdx) {
                    if (tabIdx >= 0) {
                        com.example.ui.screens.tools.TeacherToolsTab.entries.getOrNull(tabIdx)
                    } else null
                }
                LaunchedEffect(sId) {
                    if (sId > 0L) {
                        teacherToolsViewModel.setHwStudent(sId)
                    }
                }
                TeacherToolsScreen(
                    viewModel = teacherToolsViewModel,
                    initialTab = targetTab,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSchedule = { navController.navigate(Screen.Schedule.route) },
                    onNavigateToStudents = { navController.navigate(Screen.Students.route) },
                    onNavigateToSmartPrep = { navController.navigate(Screen.SmartPrep.route) },
                    onNavigateToAiChat = { navController.navigate(Screen.AiChat.route) },
                    onNavigateToStudyFiles = { navController.navigate(Screen.StudyFiles.createRoute()) },
                    onNavigateToFinance = { navController.navigate(Screen.Finance.route) },
                    onNavigateToGroups = { navController.navigate(Screen.Groups.route) }
                )
            }

            // 21. Smart Lesson Preparation (التحضير الذكي للدروس)
            composable(Screen.SmartPrep.route) {
                SmartPrepScreen(
                    viewModel = smartPrepViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 22. App Features Showcase & Poster (المميزات)
            composable(Screen.Poster.route) {
                AppPosterScreen(
                    repository = repository,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateHome = navigateToHome,
                    onNavigateToAttendance = { navController.navigate(Screen.Attendance.route) },
                    onNavigateToStudents = { navController.navigate(Screen.Students.route) },
                    onNavigateToGroups = { navController.navigate(Screen.Groups.route) },
                    onNavigateToSchedule = { navController.navigate(Screen.Schedule.route) },
                    onNavigateToFinance = { navController.navigate(Screen.Finance.route) },
                    onNavigateToExams = { navController.navigate(Screen.Exams.route) },
                    onNavigateToReports = { navController.navigate(Screen.Reports.createRoute()) },
                    onNavigateToCertificates = { navController.navigate(Screen.Certificates.createRoute()) },
                    onNavigateToSmartPrep = { navController.navigate(Screen.SmartPrep.route) },
                    onNavigateToQuestionBank = { navController.navigate(Screen.QuestionBank.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    onNavigateToBackup = { navController.navigate(Screen.Backup.route) },
                    onNavigateToStudyFiles = { navController.navigate(Screen.StudyFiles.createRoute()) }
                )
            }

            // 23. Study Files & Books (كتب ومذكرات وملفات)
            composable(
                route = Screen.StudyFiles.route,
                arguments = listOf(navArgument("grade") {
                    type = NavType.StringType
                    defaultValue = ""
                })
            ) { backStackEntry ->
                val grade = backStackEntry.arguments?.getString("grade") ?: ""
                StudyFilesScreen(
                    viewModel = studyFilesViewModel,
                    initialGradeFilter = grade,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateHome = navigateToHome,
                    onOpenFileInViewer = { filePath, title ->
                        navController.navigate(Screen.PdfViewer.createRoute(filePath, title))
                    }
                )
            }

            // 24. PDF Viewer & Smart Whiteboard with Geometric Tools
            composable(
                route = Screen.PdfViewer.route,
                arguments = listOf(
                    navArgument("filePath") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("title") {
                        type = NavType.StringType
                        defaultValue = "السبورة الهندسية"
                    }
                )
            ) { backStackEntry ->
                val rawFilePath = backStackEntry.arguments?.getString("filePath") ?: ""
                val rawTitle = backStackEntry.arguments?.getString("title") ?: "السبورة الهندسية"
                val filePath = java.net.URLDecoder.decode(rawFilePath, "UTF-8")
                val title = java.net.URLDecoder.decode(rawTitle, "UTF-8")

                PdfViewerScreen(
                    filePath = filePath,
                    title = title,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 25. AI Chat Assistant (Gemini Multi-turn)
            composable(route = Screen.AiChat.route) {
                com.example.ui.screens.tools.AiChatScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
