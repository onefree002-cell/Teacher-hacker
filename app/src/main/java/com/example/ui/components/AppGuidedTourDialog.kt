package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.SessionEntity
import com.example.data.local.entity.StudentEntity
import com.example.data.repository.TeacherPlannerRepository
import com.example.navigation.Screen
import com.example.ui.screens.attendance.AttendanceViewModel
import com.example.ui.screens.groups.GroupsViewModel
import com.example.ui.screens.students.StudentsViewModel
import com.example.ui.theme.*
import com.example.util.AppPreferencesManager
import com.example.util.L
import kotlinx.coroutines.launch

/**
 * دليل المعلم التفاعلي وأكاديمية المهام التدريبية (Teacher Academy & Interactive Guide)
 * يقدم:
 * 1. مهام المعلم التفاعلية مع خطوات التنفيذ وزر انتقال مباشر لكل شاشة.
 * 2. خريطة تفصيلية لجميع مكونات وشاشات التطبيق ووظيفتها.
 * 3. حيل ونصائح المعلم المحترف (Pro Tips) لأفضل استخدام يومي.
 */
@Composable
fun AppGuidedTourDialog(
    repository: TeacherPlannerRepository? = null,
    groupsViewModel: GroupsViewModel? = null,
    studentsViewModel: StudentsViewModel? = null,
    attendanceViewModel: AttendanceViewModel? = null,
    onDismiss: () -> Unit,
    onNavigateToScreen: ((String) -> Unit)? = null
) {
    val isArabic = L.isArabic()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Active Navigation Tab: 0 = مهام المعلم التفاعلية, 1 = خريطة مكونات التطبيق, 2 = حيل ونصائح الاستخدام
    var selectedTab by remember { mutableIntStateOf(0) }

    // State for interactive sandbox demo inside tasks
    var demoGroupName by remember { mutableStateOf("مجموعة أوائل الثانوية") }
    var isDemoGroupCreated by remember { mutableStateOf(false) }

    var demoStudentName by remember { mutableStateOf("أحمد محمود علي") }
    var isDemoStudentCreated by remember { mutableStateOf(false) }

    var demoAttendanceStatus by remember { mutableStateOf<String?>(null) }
    var isDemoAttendanceRecorded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
                .padding(4.dp)
                .testTag("app_guided_tour_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // =============================================================
                // 1. TOP HEADER: Title, Icon & Close Action
                // =============================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.School,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isArabic) "أكاديمية المعلم ودليل المهام 🚀" else "Teacher Academy & Task Guide 🚀",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isArabic) "تعلم كيفية تنفيذ المهام واستكشف جميع مكونات التطبيق" else "Learn how to do tasks & explore all app features",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            AppPreferencesManager.setHasSeenTour(true)
                            onDismiss()
                        },
                        modifier = Modifier.testTag("tour_close_btn")
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = if (isArabic) "إغلاق" else "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // =============================================================
                // 2. NAVIGATION TABS (المهام التفاعلية / خريطة المكونات / نصائح المحترفين)
                // =============================================================
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = if (isArabic) "⚡ مهام المعلم العملية" else "⚡ Action Tasks",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        },
                        icon = {
                            Icon(Icons.Filled.Checklist, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = if (isArabic) "🗺️ خريطة المكونات" else "🗺️ App Modules",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        },
                        icon = {
                            Icon(Icons.Filled.GridView, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Text(
                                text = if (isArabic) "💡 حيل ونصائح" else "💡 Pro Tips",
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        },
                        icon = {
                            Icon(Icons.Filled.Lightbulb, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // =============================================================
                // 3. TAB CONTENT
                // =============================================================
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> TeacherTasksGuideTab(
                            isArabic = isArabic,
                            demoGroupName = demoGroupName,
                            onDemoGroupNameChange = { demoGroupName = it },
                            isDemoGroupCreated = isDemoGroupCreated,
                            onCreateDemoGroup = {
                                coroutineScope.launch {
                                    val group = GroupEntity(
                                        name = demoGroupName.trim(),
                                        grade = "الثالث الثانوي",
                                        pricingType = "monthly",
                                        monthlyPrice = 300.0,
                                        sessionDays = "السبت والأربعاء",
                                        sessionTime = "16:00",
                                        durationMinutes = 90,
                                        location = "سنتر الأوائل",
                                        currentTerm = "الترم الأول"
                                    )
                                    if (repository != null) {
                                        repository.insertGroup(group)
                                    } else if (groupsViewModel != null) {
                                        groupsViewModel.addOrUpdateGroup(group)
                                    }
                                    isDemoGroupCreated = true
                                    Toast.makeText(context, if (isArabic) "✅ تم إنشاء المجموعة وحفظها في قاعدة البيانات!" else "Group created!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            demoStudentName = demoStudentName,
                            onDemoStudentNameChange = { demoStudentName = it },
                            isDemoStudentCreated = isDemoStudentCreated,
                            onCreateDemoStudent = {
                                coroutineScope.launch {
                                    val student = StudentEntity(
                                        name = demoStudentName.trim(),
                                        phone = "01012345678",
                                        parentPhone = "01198765432",
                                        groupId = 1L,
                                        grade = "الثالث الثانوي",
                                        barcodeCode = "STU-${(1000..9999).random()}",
                                        tags = "طالب متميز"
                                    )
                                    if (repository != null) {
                                        repository.insertStudent(student)
                                    } else if (studentsViewModel != null) {
                                        studentsViewModel.addOrUpdateStudent(student)
                                    }
                                    isDemoStudentCreated = true
                                    Toast.makeText(context, if (isArabic) "✅ تم إضافة الطالب وتوليد باركود QR بنجاح!" else "Student created with QR!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            demoAttendanceStatus = demoAttendanceStatus,
                            isDemoAttendanceRecorded = isDemoAttendanceRecorded,
                            onRecordDemoAttendance = { st ->
                                demoAttendanceStatus = st
                                isDemoAttendanceRecorded = true
                                Toast.makeText(context, if (isArabic) "✅ تم تسجيل الحضور وتحديث لوحة التحكم!" else "Attendance marked!", Toast.LENGTH_SHORT).show()
                            },
                            onNavigateTo = { route ->
                                AppPreferencesManager.setHasSeenTour(true)
                                onDismiss()
                                onNavigateToScreen?.invoke(route)
                            }
                        )
                        1 -> AppModulesGuideTab(
                            isArabic = isArabic,
                            onNavigateTo = { route ->
                                AppPreferencesManager.setHasSeenTour(true)
                                onDismiss()
                                onNavigateToScreen?.invoke(route)
                            }
                        )
                        2 -> ProTipsGuideTab(
                            isArabic = isArabic,
                            onNavigateTo = { route ->
                                AppPreferencesManager.setHasSeenTour(true)
                                onDismiss()
                                onNavigateToScreen?.invoke(route)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // =============================================================
                // 4. BOTTOM BAR: Finish & Dismiss Action
                // =============================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabic) "💡 يمكنك فتح هذا الدليل مجدداً من أيقونة الصاروخ 🚀 بالأعلى" else "💡 Reopen guide anytime via 🚀 icon on top",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            AppPreferencesManager.setHasSeenTour(true)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(if (isArabic) "فهمت وابدأ العمل الآن ✓" else "Got it! Start Working ✓", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// =============================================================================
// TAB 1: TEACHER INTERACTIVE TASKS (مهام المعلم العملية مع الشرح والتنفيذ)
// =============================================================================
@Composable
private fun TeacherTasksGuideTab(
    isArabic: Boolean,
    demoGroupName: String,
    onDemoGroupNameChange: (String) -> Unit,
    isDemoGroupCreated: Boolean,
    onCreateDemoGroup: () -> Unit,
    demoStudentName: String,
    onDemoStudentNameChange: (String) -> Unit,
    isDemoStudentCreated: Boolean,
    onCreateDemoStudent: () -> Unit,
    demoAttendanceStatus: String?,
    isDemoAttendanceRecorded: Boolean,
    onRecordDemoAttendance: (String) -> Unit,
    onNavigateTo: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Banner explaining how to execute tasks
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.DirectionsWalk, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isArabic)
                        "إليك أهم 6 مهام يومية يحتاجها المعلم لإدارة دروسه، مع شرح الخطوات وإمكانية الذهاب المباشر لتنفيذها في التطبيق أو تجربتها هنا فوراً:"
                    else
                        "Here are the top 6 core tasks for teachers with step-by-step instructions and 1-tap shortcuts to the actual app screens:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // =====================================================================
        // TASK 1: CREATE A GROUP (إنشاء مجموعة دراسية)
        // =====================================================================
        TaskActionCard(
            number = "1",
            title = if (isArabic) "كيفية إنشاء مجموعة دراسية وتثبيت مواعيدها" else "How to Create a Study Group & Schedule",
            badge = if (isArabic) "أساسي" else "Essential",
            badgeColor = IndigoExam,
            icon = Icons.Filled.Class,
            whatIsIt = if (isArabic)
                "المجموعة هي الحاوية الأساسية التي تضم طلابك، وتحدد الصف الدراسي، السنتر/القاعة، نظام المحاسبة (شهري أو بالحصة)، وأيام الحصص."
            else
                "A group organizes students by grade, pricing model (monthly or per session), venue/room, and weekly schedule days.",
            steps = listOf(
                if (isArabic) "اضغط على زر (+ إضافة مجموعة) من الشاشة الرئيسية أو افتح شاشة (المجموعات)." else "Tap (+ Add Group) from Dashboard or open Groups screen.",
                if (isArabic) "أدخل اسم المجموعة، الصف الدراسي، والمكان/السنتر وقيمة الاشتراك." else "Enter group name, grade, venue, and monthly fee.",
                if (isArabic) "حدد أيام الأسبوع وتوقيت الحصة لتثبيتها تلقائياً في جدول المواعيد." else "Select days and class time to auto-schedule into weekly calendar.",
                if (isArabic) "اضغط (إضافة المجموعة والحصص) لحفظها في قاعدة البيانات فوراً." else "Tap (Add Group & Schedule) to save into database."
            ),
            directButtonText = if (isArabic) "🚀 خذني لشاشة المجموعات لنفّذ الآن" else "🚀 Open Groups Screen",
            onDirectClick = { onNavigateTo(Screen.Groups.route) }
        ) {
            // Mini Sandbox for Task 1
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isArabic) "🧪 تجربة سريعة: أنشئ مجموعة تجريبية بنقرة واحدة:" else "🧪 Quick Trial: Create sample group in 1 tap:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = IndigoExam
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = demoGroupName,
                        onValueChange = onDemoGroupNameChange,
                        singleLine = true,
                        label = { Text(if (isArabic) "اسم المجموعة" else "Group Name") },
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = onCreateDemoGroup,
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDemoGroupCreated) EmeraldSuccess else IndigoExam),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isDemoGroupCreated) (if (isArabic) "تم الحفظ ✓" else "Saved ✓") else (if (isArabic) "أنشئ الآن" else "Create"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // =====================================================================
        // TASK 2: ADD STUDENTS & QR CODES (إضافة الطلاب وتوليد بطاقات الـ QR)
        // =====================================================================
        TaskActionCard(
            number = "2",
            title = if (isArabic) "كيفية إضافة الطلاب وتوليد بطاقات الـ QR وطباعتها" else "How to Add Students & Generate QR Cards",
            badge = if (isArabic) "يومي" else "Daily",
            badgeColor = EmeraldSuccess,
            icon = Icons.Filled.PersonAdd,
            whatIsIt = if (isArabic)
                "تسجيل بيانات الطلاب، هواتف أولياء الأمور لتفعيل رسائل الواتساب، وتوليد كود باركود QR فريد لكل طالب لطباعة كارنيه العضوية."
            else
                "Save student profiles, parent phone numbers for WhatsApp alerts, and generate printable QR membership cards.",
            steps = listOf(
                if (isArabic) "افتح شاشة (الطلاب) واضغط على زر (+ إضافة طالب)." else "Open Students screen and tap (+ Add Student).",
                if (isArabic) "أدخل اسم الطالب، هاتف الطالب، ورقم ولي الأمر (ضروري لرسائل المتابعة والغياب)." else "Enter student name, phone, and parent phone for WhatsApp alerts.",
                if (isArabic) "اختر المجموعة الدراسية التابع لها الطالب." else "Select the assigned study group.",
                if (isArabic) "اضغط حفظ لتوليد باركود QR فوراً، ويمكنك من صفحة الطالب الضغط على (طباعة كارنيه QR)." else "Save to generate QR code, and tap (Print QR ID) to export PDF."
            ),
            directButtonText = if (isArabic) "🚀 خذني لشاشة الطلاب لنفّذ الآن" else "🚀 Open Students Screen",
            onDirectClick = { onNavigateTo(Screen.Students.route) }
        ) {
            // Mini Sandbox for Task 2
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isArabic) "🧪 تجربة سريعة: سجّل طالب تجريبي وولد باركود QR له:" else "🧪 Quick Trial: Add sample student with QR:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = EmeraldSuccess
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = demoStudentName,
                        onValueChange = onDemoStudentNameChange,
                        singleLine = true,
                        label = { Text(if (isArabic) "اسم الطالب" else "Student Name") },
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = onCreateDemoStudent,
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDemoStudentCreated) EmeraldSuccess else EmeraldSuccess),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isDemoStudentCreated) (if (isArabic) "تم التسجيل ✓" else "Saved ✓") else (if (isArabic) "سجّل الآن" else "Add"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // =====================================================================
        // TASK 3: ATTENDANCE & HOMEWORK PHOTO (رصد الحضور وتصوير الواجب)
        // =====================================================================
        TaskActionCard(
            number = "3",
            title = if (isArabic) "كيفية رصد الحضور بالباركود وتصوير كشكول الواجب" else "How to Mark Attendance & Photo Homework",
            badge = if (isArabic) "سريع وذكي" else "Fast & Smart",
            badgeColor = AmberGoldDark,
            icon = Icons.Filled.CheckCircle,
            whatIsIt = if (isArabic)
                "تسجيل حضور المجموعة في ثوانٍ معدودة عبر مسح باركود بطاقة الطالب بكاميرا الهاتف، أو بلمسة واحدة يدوياً، مع إمكانية تصوير كشكول الواجب."
            else
                "Scan student QR cards with phone camera in seconds, or tap to toggle status with instant homework notebook camera capture.",
            steps = listOf(
                if (isArabic) "افتح شاشة (الحضور) واختر المجموعة والتاريخ المطلوب." else "Open Attendance screen and choose group and date.",
                if (isArabic) "اضغط على زر (مسح QR بالكاميرا) لمسح بطاقات الطلاب واحداً تلو الآخر تلقائياً." else "Tap (Scan QR) for automatic ultra-fast attendance scanning.",
                if (isArabic) "أو اضغط مباشرة على بطاقة الطالب لتغيير حالته (حاضر / غائب / متأخر)." else "Or tap student row to toggle (Present / Absent / Late).",
                if (isArabic) "اضغط على أيقونة الكاميرا 📸 لالتقاط صورة الواجب وحفظها بملف الطالب." else "Tap 📸 camera icon to photograph homework notebook."
            ),
            directButtonText = if (isArabic) "🚀 خذني لشاشة الحضور والغياب" else "🚀 Open Attendance Screen",
            onDirectClick = { onNavigateTo(Screen.Attendance.route) }
        ) {
            // Mini Sandbox for Task 3
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isArabic) "🧪 جرب تبديل حالة الحضور بلمسة واحدة:" else "🧪 Test 1-tap attendance toggle:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = AmberGoldDark
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "present" to (if (isArabic) "حاضر ✔️" else "Present ✔️"),
                        "late" to (if (isArabic) "متأخر ⏳" else "Late ⏳"),
                        "absent" to (if (isArabic) "غائب ❌" else "Absent ❌")
                    ).forEach { (statusKey, label) ->
                        val isSel = demoAttendanceStatus == statusKey
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) when (statusKey) {
                                "present" -> EmeraldSuccess
                                "late" -> AmberGoldDark
                                else -> CrimsonError
                            } else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onRecordDemoAttendance(statusKey) }
                        ) {
                            Text(
                                text = label,
                                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // =====================================================================
        // TASK 4: WHATSAPP REPORTS & NOTIFICATIONS (رسائل الواتساب والتقارير)
        // =====================================================================
        TaskActionCard(
            number = "4",
            title = if (isArabic) "كيفية إرسال تقارير المتابعة والغياب لأولياء الأمور عبر WhatsApp" else "How to Send WhatsApp Reports to Parents",
            badge = if (isArabic) "تواصل فوري" else "Instant Chat",
            badgeColor = Color(0xFF25D366),
            icon = Icons.Filled.Send,
            whatIsIt = if (isArabic)
                "إرسال رسائل آلية منسقة بضغطة زر واحدة لإبلاغ ولي الأمر بغياب الطالب، درجات الامتحانات الشهرية، أو تقييم الواجب والسلوك."
            else
                "1-click automated structured WhatsApp messaging to notify parents about absence, test scores, or monthly progress.",
            steps = listOf(
                if (isArabic) "من شاشة الحضور أو تفاصيل الطالب، اضغط على أيقونة (WhatsApp 💬)." else "From Attendance or Student Detail, tap WhatsApp 💬 icon.",
                if (isArabic) "يتم توليد رسالة رسمية مخصصة باسم الطالب، التاريخ، والتفاصيل تلقائياً." else "A customized formal Arabic/English message is prepared automatically.",
                if (isArabic) "يفتح التطبيق محادثة ولي الأمر مباشرة بدون الحاجة لحفظ رقمه في جهات الاتصال." else "Directly opens WhatsApp chat with parent without saving contact.",
                if (isArabic) "يمكنك أيضاً إرسال تنبيه جماعي لكل طلاب المجموعة بضغطة واحدة." else "Bulk send announcements to whole group WhatsApp in 1 tap."
            ),
            directButtonText = if (isArabic) "🚀 خذني لتقارير الطلاب" else "🚀 Open Student Reports",
            onDirectClick = { onNavigateTo(Screen.Reports.createRoute(0L)) }
        )

        // =====================================================================
        // TASK 5: EXAMS & RANKING (الامتحانات ورصد الدرجات وترتيب الأوائل)
        // =====================================================================
        TaskActionCard(
            number = "5",
            title = if (isArabic) "كيفية إنشاء امتحان ورصد الدرجات واستخراج كشف الأوائل" else "How to Record Exams & Rank Top Students",
            badge = if (isArabic) "تقييم" else "Assessment",
            badgeColor = Color(0xFF9333EA),
            icon = Icons.Filled.Assignment,
            whatIsIt = if (isArabic)
                "تسجيل الامتحانات الشهرية والشيتات الأسبوعية، رصد الدرجة العظمى والصغرى، وتوليد كشف الأوائل ولوحة الشرف تلقائياً."
            else
                "Manage monthly exams and weekly quizzes, score limits, top ranking leaderboard, and export honor roll certificates.",
            steps = listOf(
                if (isArabic) "افتح شاشة (الامتحانات) واضغط (+ إضافة امتحان جديد)." else "Open Exams screen and tap (+ Add New Exam).",
                if (isArabic) "حدد اسم الامتحان، المجموعة، والدرجة النهائية (مثال 50 درجة)." else "Set exam title, target group, and max score (e.g. 50).",
                if (isArabic) "ادخل درجات الطلاب بسهولة في الجدول التفاعلي السريع." else "Enter student marks quickly in the interactive grid.",
                if (isArabic) "اضغط على (لوحة الشرف) لتوليد شهادات تقدير PDF للطلاب المتميزين." else "Tap (Honor Roll) to generate PDF certificates for top achievers."
            ),
            directButtonText = if (isArabic) "🚀 خذني لشاشة الامتحانات" else "🚀 Open Exams Screen",
            onDirectClick = { onNavigateTo(Screen.Exams.route) }
        )

        // =====================================================================
        // TASK 6: TEACHER TOOLS (السبورة التفاعلية + حاسبة Casio + المذكرات)
        // =====================================================================
        TaskActionCard(
            number = "6",
            title = if (isArabic) "استخدام أدوات المعلم (السبورة، حاسبة Casio fx-991ES، وبنك الأسئلة)" else "How to Use Teacher Tools (Casio Calculator, Whiteboard)",
            badge = if (isArabic) "أدوات ذكية" else "Smart Suite",
            badgeColor = NavyPrimary,
            icon = Icons.Filled.AutoFixHigh,
            whatIsIt = if (isArabic)
                "حقيبة متكاملة للمعلم داخل الحصة: سبورة تفاعلية للشرح الهندسي، محاكي كامل لآلة Casio fx-991ES العلمية، مسجل صوتي للحصص، وعارض مذكرات PDF."
            else
                "All-in-one in-class utility suite: geometric whiteboard, realistic Casio fx-991ES calculator, audio session recorder, and PDF viewer.",
            steps = listOf(
                if (isArabic) "افتح تبويب (أدوات المعلم) من شريط التنقل السفلي." else "Open (Teacher Tools) tab from bottom navigation bar.",
                if (isArabic) "اختر (الآلة الحاسبة العلمية Casio) لإجراء الحسابات المعقدة والكسور." else "Choose (Scientific Calculator) for complex math in class.",
                if (isArabic) "اختر (السبورة الهندسية) للرسم والشرح التفاعلي على الشاشات الذكية." else "Select (Engineering Whiteboard) to draw and explain concepts.",
                if (isArabic) "افتح (المذكرات والكتب) لاستعراض شيتات الشرح ومشاركتها مع الطلاب." else "Open (Study Files) to view and share curriculum handouts."
            ),
            directButtonText = if (isArabic) "🚀 خذني لأدوات المعلم الآن" else "🚀 Open Teacher Tools",
            onDirectClick = { onNavigateTo(Screen.TeacherTools.createRoute()) }
        )
    }
}

// =============================================================================
// TAB 2: APP MODULES ARCHITECTURE GUIDE (خريطة وشرح جميع مكونات التطبيق)
// =============================================================================
@Composable
private fun AppModulesGuideTab(
    isArabic: Boolean,
    onNavigateTo: (String) -> Unit
) {
    val modules = listOf(
        AppModuleItem(
            name = if (isArabic) "لوحة التحكم الرئيسية (Dashboard)" else "Dashboard & Analytics",
            icon = Icons.Filled.Dashboard,
            color = Color(0xFF1E88E5),
            route = Screen.Dashboard.route,
            description = if (isArabic)
                "مركز القيادة اليومي: يعرض إجمالي الطلاب، حصص اليوم القادمة، نسبة الحضور الأسبوعية، المتأخرات المالية، وشريط الإجراءات السريعة الفورية."
            else
                "Daily command center: total students, today's classes, attendance rate, unpaid dues, and instant quick action shortcuts."
        ),
        AppModuleItem(
            name = if (isArabic) "المجموعات والفصول (Groups)" else "Groups & Classes",
            icon = Icons.Filled.Class,
            color = IndigoExam,
            route = Screen.Groups.route,
            description = if (isArabic)
                "تنظيم المجموعات الدراسية بحسب المرحلة (ابتدائي، إعدادي، ثانوي)، تحديد أسعار الاشتراكات (شهري / بالحصة)، وتعيين السنتر والقاعة."
            else
                "Manage groups by academic grade, billing model, pricing, venues, and weekly class times."
        ),
        AppModuleItem(
            name = if (isArabic) "الطلاب وسجل الدرجات (Students)" else "Students Directory",
            icon = Icons.Filled.People,
            color = EmeraldSuccess,
            route = Screen.Students.route,
            description = if (isArabic)
                "قاعدة بيانات شاملة لكل طالب: رقم الطالب وولي الأمر، كود باركود QR، سجل الغياب والدرجات، ملاحظات المعلم، وتصدير بطاقات العضوية."
            else
                "Complete student database: contact info, parent phone, QR barcode cards, attendance history, and notes."
        ),
        AppModuleItem(
            name = if (isArabic) "الجدول الأسبوعي والمواعيد (Schedule)" else "Weekly Timetable",
            icon = Icons.Filled.CalendarMonth,
            color = NavyPrimary,
            route = Screen.Schedule.route,
            description = if (isArabic)
                "جدول تفاعلي زمني لتنظيم الحصص بحسب أيام الأسبوع والمراكز التعليمية مع تنبيهات ذكية لمنع أي تعارض في المواعيد أو القاعات."
            else
                "Interactive weekly timetable with conflict detection across study centers and rooms."
        ),
        AppModuleItem(
            name = if (isArabic) "الحضور والغياب والباركود (Attendance)" else "Smart Attendance",
            icon = Icons.Filled.CheckCircle,
            color = AmberGoldDark,
            route = Screen.Attendance.route,
            description = if (isArabic)
                "رصد الحضور والغياب فائق السرعة عبر كاميرا مسح الباركود، أو التبديل اليدوي، مع تصوير كشكول الواجب ورصد تقييم حل الواجب."
            else
                "Ultra-fast QR scanner camera attendance, manual 1-tap toggling, and notebook homework photo capture."
        ),
        AppModuleItem(
            name = if (isArabic) "الامتحانات والشيتات (Exams)" else "Exams & Quizzes",
            icon = Icons.Filled.Assignment,
            color = Color(0xFF9333EA),
            route = Screen.Exams.route,
            description = if (isArabic)
                "إدارة الامتحانات وتحديد الدرجات العظمى، رصد الدرجات لطلاب المجموعة، استخراج كشف الأوائل، وتوليد تقارير الأداء بصيغة PDF."
            else
                "Create exams, grade assignments, generate top student leaderboards, and export PDF score sheets."
        ),
        AppModuleItem(
            name = if (isArabic) "المالية والحسابات (Finance)" else "Finance & Billing",
            icon = Icons.Filled.AccountBalanceWallet,
            color = Color(0xFF047857),
            route = Screen.Finance.route,
            description = if (isArabic)
                "تتبع تحصيل اشتراكات الطلاب، كشف المتأخرات المالية، تسجيل مصروفات السنتر والطباعة، وإحصائيات الدخل الشهري بدقة."
            else
                "Track student fee payments, unpaid balances, printing/center expenses, and monthly financial summaries."
        ),
        AppModuleItem(
            name = if (isArabic) "أدوات المعلم والحاسبة العلمية (Teacher Tools)" else "Teacher Smart Suite",
            icon = Icons.Filled.AutoFixHigh,
            color = Color(0xFFD97706),
            route = Screen.TeacherTools.createRoute(),
            description = if (isArabic)
                "الآلة الحاسبة العلمية Casio fx-991ES، السبورة الهندسية التفاعلية، مسجل الحصص الصوتي، عارض ملفات PDF والمذكرات المدرسية."
            else
                "Casio fx-991ES scientific calculator, engineering interactive whiteboard, audio recorder, and PDF curriculum viewer."
        ),
        AppModuleItem(
            name = if (isArabic) "الشهادات والتقارير الشهرية (Certificates & Reports)" else "Certificates & Reports",
            icon = Icons.Filled.WorkspacePremium,
            color = Color(0xFFE11D48),
            route = Screen.Certificates.createRoute(0L),
            description = if (isArabic)
                "توليد شهادات تقدير ملونة جاهزة للطباعة، وتقارير أداء شاملة لكل طالب لمشاركتها مع أولياء الأمور عبر WhatsApp."
            else
                "Export colorful printable student appreciation certificates and detailed monthly PDF progress cards."
        ),
        AppModuleItem(
            name = if (isArabic) "أماكن وقاعات الدروس (Venues)" else "Study Venues & Rooms",
            icon = Icons.Filled.LocationCity,
            color = Color(0xFF0284C7),
            route = Screen.Venues.route,
            description = if (isArabic)
                "دليل السناتر والمراكز التعليمية والقاعات، تتبع نسب السنتر ونظام التأجير، وأرقام تواصل مسؤولي السناتر."
            else
                "Manage education centers, classroom rooms, commission rates, and center manager contacts."
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = if (isArabic) "خريطة أقسام وشاشات التطبيق الكاملة:" else "Complete App Architecture & Navigation Modules:",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        modules.forEach { module ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, module.color.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = module.color.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(module.icon, contentDescription = null, tint = module.color, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = module.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Button(
                            onClick = { onNavigateTo(module.route) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = module.color)
                        ) {
                            Text(if (isArabic) "فتح ↗" else "Open ↗", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        text = module.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

// =============================================================================
// TAB 3: PRO TIPS & BEST PRACTICES (حيل وأسرار المعلم المحترف)
// =============================================================================
@Composable
private fun ProTipsGuideTab(
    isArabic: Boolean,
    onNavigateTo: (String) -> Unit
) {
    val tips = listOf(
        ProTipItem(
            title = if (isArabic) "💡 مسح باركود الـ QR بدون إنترنت" else "💡 Offline QR Attendance Scanning",
            text = if (isArabic)
                "تطبيق المعلم يعمل بنسبة 100% بدون اتصال إنترنت. يمكنك طباعة بطاقات الـ QR للطلاب واستخدام كاميرا التطبيق لمسح الحضور حتى داخل القاعات المعزولة عن الشبكة."
            else
                "The app is 100% offline-first. You can scan student QR cards and record full attendance anywhere without needing Wi-Fi or mobile data."
        ),
        ProTipItem(
            title = if (isArabic) "📸 تصوير كشكول الواجب وتوثيق المتابعة" else "📸 Photographing Homework Notebooks",
            text = if (isArabic)
                "عند رصد الواجب، اضغط على زر الكاميرا بجانب اسم الطالب. يلتقط التطبيق صورة سريعة للواجب ويربطها بتاريخ الحصة، لتقديم دليل موثق لولي الأمر في حال اعتراضه."
            else
                "When checking homework, tap the camera icon to snap a quick photo. It is automatically saved and attached to the student session record."
        ),
        ProTipItem(
            title = if (isArabic) "💬 فتح محادثات واتساب بدون حفظ الرقم" else "💬 1-Tap Direct WhatsApp Messaging",
            text = if (isArabic)
                "لا داعي لحفظ مئات أرقام أولياء الأمور في جهات اتصال هاتفك. زر واتساب في التطبيق يفتح المحادثة مباشرة مع رسالة مجهزة ومخصصة باسم الطالب وحالته."
            else
                "No need to clutter your personal phone contacts with hundreds of parent numbers. Tap WhatsApp in-app to start direct chat with pre-filled message."
        ),
        ProTipItem(
            title = if (isArabic) "🧮 شاشة الآلة الحاسبة Casio fx-991ES أثناء الحصة" else "🧮 Full Casio fx-991ES Natural Display in Class",
            text = if (isArabic)
                "افتح شاشة أدوات المعلم واختر الآلة الحاسبة العلمية. صُممت لتحاكي بدقة أزرار وشاشة Casio الأصلية لحل المعادلات والكسور والتفاضل أمام الطلاب بسهولة."
            else
                "Access the realistic Casio fx-991ES calculator from Teacher Tools to solve fractions, quadratic equations, and complex calculus on screen."
        ),
        ProTipItem(
            title = if (isArabic) "🛡️ حفظ نسخة احتياطية من بياناتك (Backup)" else "🛡️ 1-Click Database Backup & Restore",
            text = if (isArabic)
                "من شاشة الإعدادات، يمكنك تصدير نسخة احتياطية مشفرة بضغطة زر وحفظها على Google Drive أو إرسالها لبريدك، لاستعادتها فوراً على أي هاتف جديد."
            else
                "From Settings -> Backup, export a safe database backup to Google Drive or email to safely restore all students and grades onto any new device."
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = if (isArabic) "حيل ونصائح لتسريع عملك اليومي كمعلم محترف:" else "Pro Tips to Accelerate Your Daily Teaching Workflow:",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        tips.forEach { tip ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AmberGoldContainer.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = tip.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = AmberGoldDark
                    )
                    Text(
                        text = tip.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

// =============================================================================
// HELPER COMPONENT: TASK ACTION CARD
// =============================================================================
@Composable
private fun TaskActionCard(
    number: String,
    title: String,
    badge: String,
    badgeColor: Color,
    icon: ImageVector,
    whatIsIt: String,
    steps: List<String>,
    directButtonText: String,
    onDirectClick: () -> Unit,
    sandboxContent: (@Composable () -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.2.dp, badgeColor.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = badgeColor,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(number, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = badge,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Description / What is it
            Text(
                text = whatIsIt,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            // Steps Checklist
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (L.isArabic()) "📌 خطوات التنفيذ بالتفصيل:" else "📌 Step-by-Step Instructions:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                steps.forEachIndexed { idx, stepText ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = "${idx + 1}. ",
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            fontSize = 11.sp
                        )
                        Text(
                            text = stepText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Optional Sandbox Trial
            sandboxContent?.invoke()

            // Direct Go-To Button
            Button(
                onClick = onDirectClick,
                colors = ButtonDefaults.buttonColors(containerColor = badgeColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(directButtonText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

private data class AppModuleItem(
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val route: String,
    val description: String
)

private data class ProTipItem(
    val title: String,
    val text: String
)
