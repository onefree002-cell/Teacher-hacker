package com.example.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val selectedIcon: ImageVector? = null, val unselectedIcon: ImageVector? = null) {
    fun getLocalizedTitle(): String {
        return when (this) {
            is Dashboard -> com.example.util.L.dashboard()
            is Schedule -> com.example.util.L.schedule()
            is Attendance -> com.example.util.L.attendance()
            is Students -> com.example.util.L.students()
            is Finance -> com.example.util.L.finance()
            is Groups -> com.example.util.L.groups()
            is Exams -> com.example.util.L.exams()
            is Reports -> com.example.util.L.reports()
            is Certificates -> com.example.util.L.certificates()
            is Profile -> com.example.util.L.profile()
            is Backup -> com.example.util.L.backup()
            is Venues -> com.example.util.L.venues()
            is Curriculum -> com.example.util.L.curriculum()
            is SmartPrep -> com.example.util.L.smartPrep()
            is QuestionBank -> com.example.util.L.questionBank()
            is Search -> com.example.util.L.search()
            is TeacherTools -> com.example.util.L.teacherTools()
            is Poster -> com.example.util.L.poster()
            is StudyFiles -> com.example.util.L.studyFiles()
            is Settings -> com.example.util.L.settings()
            else -> title
        }
    }

    // Bottom Bar Destinations
    object Dashboard : Screen("dashboard", "الرئيسية", Icons.Filled.Dashboard, Icons.Outlined.Dashboard)
    object Schedule : Screen("schedule", "الجدول", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
    object Attendance : Screen("attendance", "الحضور", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle)
    object Students : Screen("students", "الطلاب", Icons.Filled.People, Icons.Outlined.People)
    object Finance : Screen("finance", "المالية", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet)

    // Sub Screens
    object Groups : Screen("groups", "المجموعات", Icons.Filled.Class, Icons.Outlined.Class)
    object GroupDetail : Screen("group_detail/{groupId}", "تفاصيل المجموعة") {
        fun createRoute(groupId: Long) = "group_detail/$groupId"
    }
    object StudentDetail : Screen("student_detail/{studentId}", "تفاصيل الطالب") {
        fun createRoute(studentId: Long) = "student_detail/$studentId"
    }
    object Exams : Screen("exams", "الامتحانات", Icons.Filled.Assignment, Icons.Outlined.Assignment)
    object ExamDetail : Screen("exam_detail/{examId}", "تفاصيل الامتحان والدرجات") {
        fun createRoute(examId: Long) = "exam_detail/$examId"
    }
    object Reports : Screen("reports?studentId={studentId}", "التقارير", Icons.Filled.Assessment, Icons.Outlined.Assessment) {
        fun createRoute(studentId: Long = 0L) = if (studentId > 0) "reports?studentId=$studentId" else "reports?studentId=0"
    }
    object Certificates : Screen("certificates?studentId={studentId}", "الشهادات", Icons.Filled.WorkspacePremium, Icons.Outlined.WorkspacePremium) {
        fun createRoute(studentId: Long = 0L) = if (studentId > 0) "certificates?studentId=$studentId" else "certificates?studentId=0"
    }
    object Profile : Screen("profile", "الملف الشخصي", Icons.Filled.Person, Icons.Outlined.Person)
    object Backup : Screen("backup", "النسخ والترحيل", Icons.Filled.Backup, Icons.Outlined.Backup)
    object Venues : Screen("venues", "أماكن وقاعات الدروس", Icons.Filled.LocationCity, Icons.Outlined.LocationCity)
    object Curriculum : Screen("curriculum", "خطة المنهج والدروس", Icons.Filled.MenuBook, Icons.Outlined.MenuBook)
    object SmartPrep : Screen("smart_prep", "التحضير الذكي للدروس", Icons.Filled.Psychology, Icons.Outlined.Psychology)
    object QuestionBank : Screen("question_bank", "بنك الأسئلة والشيتات", Icons.Filled.Quiz, Icons.Outlined.Quiz)
    object Search : Screen("search", "البحث الشامل", Icons.Filled.Search, Icons.Outlined.Search)
    object TeacherTools : Screen("teacher_tools?studentId={studentId}&tabIndex={tabIndex}", "أدوات المعلم", Icons.Filled.AutoFixHigh, Icons.Outlined.AutoFixHigh) {
        fun createRoute(studentId: Long = 0L, tabIndex: Int = -1) = "teacher_tools?studentId=$studentId&tabIndex=$tabIndex"
    }
    object Poster : Screen("app_features", "مميزات التطبيق", Icons.Filled.Star, Icons.Outlined.Star)
    object StudyFiles : Screen("study_files?grade={grade}", "كتب ومذكرات", Icons.Filled.LibraryBooks, Icons.Outlined.LibraryBooks) {
        fun createRoute(grade: String = "") = if (grade.isNotBlank()) "study_files?grade=$grade" else "study_files?grade="
    }
    object AiChat : Screen("ai_chat", "المساعد الذكي (Gemini)", Icons.Filled.Psychology, Icons.Outlined.Psychology)
    object PdfViewer : Screen("pdf_viewer?filePath={filePath}&title={title}", "السبورة الهندسية وعارض الكتب") {
        fun createRoute(filePath: String, title: String) = "pdf_viewer?filePath=${java.net.URLEncoder.encode(filePath, "UTF-8")}&title=${java.net.URLEncoder.encode(title, "UTF-8")}"
    }
    object Onboarding : Screen("onboarding", "تسجيل البيانات والترحيب")
    object Settings : Screen("settings", "الإعدادات", Icons.Filled.Settings, Icons.Outlined.Settings)

    companion object {
        val bottomNavItems: List<Screen>
            get() = listOf(
                Dashboard,
                Schedule,
                Attendance,
                Groups,
                Students,
                TeacherTools
            )
    }
}
