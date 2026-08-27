package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.navigation.Screen
import com.example.ui.theme.*
import com.example.util.AppPreferencesManager
import com.example.util.L
import com.example.util.LocaleManager

data class TourStep(
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val badge: String,
    val highlights: List<String>,
    val targetScreenRoute: String? = null
)

@Composable
fun AppGuidedTourDialog(
    onDismiss: () -> Unit,
    onNavigateToScreen: ((String) -> Unit)? = null
) {
    val isArabic = L.isArabic()
    var currentStepIndex by remember { mutableIntStateOf(0) }

    val tourSteps = remember(isArabic) {
        listOf(
            TourStep(
                title = if (isArabic) "لوحة التحكم الذكية والإحصائيات" else "Smart Dashboard & Overview",
                subtitle = if (isArabic) "نظرة شاملة ولحظية على كل تفاصيل عملك" else "Real-time summary of students, revenue & tasks",
                description = if (isArabic) "تمنحك لوحة التحكم إحصائيات سريعة عن عدد الطلاب المسجلين، حضور اليوم، الحصص القادمة، والإيرادات الشهرية بضغطة زر واحدة." else "Get instantaneous insights into active students, daily attendance, upcoming lessons, and monthly earnings in one tap.",
                icon = Icons.Filled.Dashboard,
                color = NavyPrimary,
                badge = "📊 1",
                highlights = listOf(
                    if (isArabic) "إحصائيات مباشرة وفورية لحصص اليوم" else "Live statistics for today's scheduled classes",
                    if (isArabic) "أزرار وصول سريع لتسجيل الحضور وإضافة الطلاب" else "Quick action shortcuts for faster workflows",
                    if (isArabic) "تنبيهات تلقائية بالحصص القادمة والمتأخرات" else "Automated alerts for pending tasks & overdue fees"
                ),
                targetScreenRoute = Screen.Dashboard.route
            ),
            TourStep(
                title = if (isArabic) "سجل الطلاب والتواصل الفوري (واتساب)" else "Students Directory & WhatsApp Connect",
                subtitle = if (isArabic) "إدارة ملفات الطلاب وباركود QR والرسائل الذكية" else "Complete student records, QR codes & messaging",
                description = if (isArabic) "سجل كامل لكل طالب يشمل بيانات الاتصال، ولي الأمر، الباركود المخصص لبطاقة الطالب، مع إرسال رسائل واتساب وتقارير درجات فورية بنقرة واحدة." else "Store complete profiles, emergency contacts, student QR IDs, with 1-click WhatsApp messaging and automated score cards.",
                icon = Icons.Filled.People,
                color = Color(0xFF0284C7),
                badge = "👥 2",
                highlights = listOf(
                    if (isArabic) "إرسال تقرير الحضور والدرجات عبر واتساب بدون حفظ الرقم" else "Direct WhatsApp notifications without saving numbers",
                    if (isArabic) "توليد وطباعة بطاقات QR وكود الباركود لكل طالب" else "Auto-generate student ID cards with printable QR",
                    if (isArabic) "تتبع التقييم السلوكي والملاحظات الشخصية" else "Track behavioral notes and performance history"
                ),
                targetScreenRoute = Screen.Students.route
            ),
            TourStep(
                title = if (isArabic) "المجموعات والمراحل التعليمية" else "Classes & Educational Stages",
                subtitle = if (isArabic) "تنظيم المجموعات حسب المرحلة والصف والسعر" else "Organize groups by stage, grade & tuition rate",
                description = if (isArabic) "أنشئ مجموعاتك الدراسية مع تحديد المرحلة التعليمية (ابتدائي، إعدادي، ثانوي)، أيام الحصص، الطاقة الاستيعابية، وسعر الحصة أو الشهر." else "Group your classes by education level (Primary, Prep, Secondary), setting schedules, student capacity, and monthly rates.",
                icon = Icons.Filled.Groups,
                color = Color(0xFF7C3AED),
                badge = "🏫 3",
                highlights = listOf(
                    if (isArabic) "دعم كامل لجميع المراحل والسنتر والمجموعات الخاصة" else "Full support for centers, private & online groups",
                    if (isArabic) "تحديد سعة القاعة لمنع التكدس" else "Capacity limits to avoid room overcrowding",
                    if (isArabic) "تصفية سريعة للمجموعات بضغطة زر" else "Instant stage-based filtering"
                ),
                targetScreenRoute = Screen.Groups.route
            ),
            TourStep(
                title = if (isArabic) "جدول المواعيد والحصص الأسبوعي" else "Weekly Schedule & Timetable",
                subtitle = if (isArabic) "تنظيم مواعيد الحصص والقاعات بدقة لمنع التعارض" else "Schedule classes and venues without conflict",
                description = if (isArabic) "جدول منظم لأيام الأسبوع يوضح مواعيد كل حصة، القاعة أو السنتر، عدد الطلاب، مع إمكانية إلغاء أو تأجيل الحصة وإرسال إشعار فوري للطلاب." else "Interactive weekly timetable showing lesson timings, venues, and student rosters, with 1-tap class cancellation alerts.",
                icon = Icons.Filled.CalendarMonth,
                color = Color(0xFF059669),
                badge = "📅 4",
                highlights = listOf(
                    if (isArabic) "عرض زمني واضح لجميع أيام الأسبوع" else "Clear day-by-day timetable grid",
                    if (isArabic) "تنبيه ذكي عند وجود تعارض في المواعيد أو القاعات" else "Smart collision detection for overlapping slots",
                    if (isArabic) "تأجيل أو تقديم الحصص مع إشعار المجموعة" else "Reschedule lessons with group broadcast"
                ),
                targetScreenRoute = Screen.Schedule.route
            ),
            TourStep(
                title = if (isArabic) "تسجيل الحضور السريع والباركود" else "Fast Attendance & QR Scanner",
                subtitle = if (isArabic) "حضور، غياب، تأخير، مع مسح الكاميرا السريع" else "1-tap status, camera QR scan & auto SMS",
                description = if (isArabic) "سجل حضور طلابك بلمسة واحدة أو عبر توجيه الكاميرا لباركود الطالب. يحفظ التطبيق تاريخ ووقت الحضور ويرسل إشعاراً لولي الأمر في حال الغياب." else "Record attendance in seconds by tapping or scanning student cards. Instantly notifies parents if a student is absent.",
                icon = Icons.Filled.FactCheck,
                color = EmeraldGreen,
                badge = "✅ 5",
                highlights = listOf(
                    if (isArabic) "أزرار واضحة وسريعة: حاضر ✔️ / غائب ❌ / متأخر ⏳" else "One-tap buttons: Present, Absent, Late",
                    if (isArabic) "ماسح باركود وQR فائق السرعة عبر الكاميرا" else "Ultra-fast QR/Barcode camera scanner",
                    if (isArabic) "إرسال إشعار فوري لولي الأمر عند الغياب" else "Immediate automated absence alert to parents"
                ),
                targetScreenRoute = Screen.Attendance.route
            ),
            TourStep(
                title = if (isArabic) "تصوير ورصد الواجب المنزلي" else "Homework Scanner & Grading",
                subtitle = if (isArabic) "رصد كامل/ناقص/لم يحل مع تصوير كشكول الواجب" else "Homework check & quick camera snapshot",
                description = if (isArabic) "قسّم متابعة الحصة بسهولة: سجّل حالة أداء الواجب (كامل 💯، ناقص ⚠️، لم يحل ❌) مع زر تصوير سريع لتوثيق صفحة الواجب أو الكشكول." else "Seamlessly log homework performance (Complete, Incomplete, Not Done) with a quick camera snapshot to archive assignments.",
                icon = Icons.Filled.PhotoCamera,
                color = Color(0xFFD97706),
                badge = "📸 6",
                highlights = listOf(
                    if (isArabic) "تقييم حالة الواجب بضغطة زر (كامل، ناقص، لم يحل)" else "1-tap homework grading chips",
                    if (isArabic) "زر كاميرا سريع لالتقاط صورة الواجب وحفظها" else "Quick camera shutter to photograph notebook pages",
                    if (isArabic) "تنبيه تلقائي لولي الأمر عند إهمال الواجب" else "Automatic parent alert for incomplete homework"
                ),
                targetScreenRoute = Screen.Attendance.route
            ),
            TourStep(
                title = if (isArabic) "بنك الأسئلة والشيتات والاختبارات" else "Question Bank & Exam Maker",
                subtitle = if (isArabic) "إنشاء شيتات وامتحانات وتحديد الإجابة الصحيحة" else "Create exams, sheets, formulas & 2D/3D shapes",
                description = if (isArabic) "بنك أسئلة متكامل يدعم الاختيار من متعدد، صح وخطأ، الأسئلة المقالية، إدراج الرموز والمعادلات الرياضية، وإرفاق أشكال هندسية 2D و 3D مع توليد شيتات PDF جاهزة للطباعة." else "Comprehensive question repository supporting MCQ answer keys, True/False, essay questions, math equations, 2D/3D geometry diagrams, and printable PDF exams.",
                icon = Icons.Filled.Quiz,
                color = Color(0xFFDC2626),
                badge = "📝 7",
                highlights = listOf(
                    if (isArabic) "تحديد الإجابة النموذجية الصحيحة في أسئلة الاختيارات والصح والخطأ" else "Mark correct answers for MCQ & True/False",
                    if (isArabic) "شريط إدراج المعادلات والرموز الرياضية والأشكال الهندسية" else "Math formula inserter and 2D/3D geometry presets",
                    if (isArabic) "تصدير امتحانات وشيتات منسقة PDF مع نموذج الإجابة" else "Export formatted PDF sheets with answer key"
                ),
                targetScreenRoute = Screen.QuestionBank.route
            ),
            TourStep(
                title = if (isArabic) "الحسابات والاشتراكات والمصروفات" else "Financial Accounts & Fees",
                subtitle = if (isArabic) "تحصيل المصروفات، متابعة المتبقي، وإيصالات الدفع" else "Track student fees, balances & payment receipts",
                description = if (isArabic) "متابعة دقيقة لاشتراكات الطلاب الشهرية، المبالغ المدفوعة والمتبقية، تسجيل المصروفات الشخصية والسنتر، مع طباعة إيصالات استلام نقدية منسقة." else "Full ledger tracking monthly dues, collected payments, outstanding balances, center expenses, and printable receipts.",
                icon = Icons.Filled.AccountBalanceWallet,
                color = AmberGoldDark,
                badge = "💰 8",
                highlights = listOf(
                    if (isArabic) "تسجيل الدفع الكامل أو الجزئي بلمسة واحدة" else "Log full or partial fee payments instantly",
                    if (isArabic) "كشف حساب مفصل بالمبالغ المتبقية على كل طالب" else "Overdue fees breakdown and payment reminders",
                    if (isArabic) "تقارير أرباح شهرية وصافية قابلة للتصدير Excel و PDF" else "Monthly net profit reports in Excel & PDF"
                ),
                targetScreenRoute = Screen.Finance.route
            ),
            TourStep(
                title = if (isArabic) "أدوات المعلم وحاسبة كاسيو العلمية" else "Teacher Tools & Casio Scientific fx",
                subtitle = if (isArabic) "حاسبة كاسيو fx-991ES، سبورة رسم، وأدوات هندسية" else "Casio fx-991ES, whiteboard & geometric tools",
                description = if (isArabic) "حاسبة كاسيو العلمية الواقعية المطابقة لدليل PDF باتجاه LTR ثابت، مسطرة دقيقة، منقلة، برجل، سبورة بيضاء ذكية، وعارض الكتب والمذكرات مع استخراج الصفحات." else "Authentic Casio fx-991ES PLUS scientific calculator with fixed LTR layout, interactive whiteboard, geometry ruler/compass, and PDF book page extractor.",
                icon = Icons.Filled.AutoFixHigh,
                color = Color(0xFF9333EA),
                badge = "📐 9",
                highlights = listOf(
                    if (isArabic) "حاسبة كاسيو fx-991ES العلمية بدقة فائقة وشاشة V.P.A.M" else "Casio fx-991ES PLUS calculator emulator",
                    if (isArabic) "أدوات هندسية تفاعلية (مسطرة، منقلة 360°، برجل)" else "Interactive geometry tools on drawing canvas",
                    if (isArabic) "زر وصول سريع دائم في شريط التنقل السفلي" else "Direct dedicated tab in the bottom navigation bar"
                ),
                targetScreenRoute = Screen.TeacherTools.route
            ),
            TourStep(
                title = if (isArabic) "النسخ الاحتياطي وتليجرام والأمان" else "Auto Backup, Telegram & Security",
                subtitle = if (isArabic) "حماية بياناتك للأبد وقفل التطبيق برقم سري" else "Keep data safe forever with PIN lock & backups",
                description = if (isArabic) "حفظ نسخة احتياطية من كل بياناتك محلياً أو إرسالها تلقائياً إلى رقم تليجرام الخاص بك، مع إمكانية قفل التطبيق برقم PIN سري لحماية خصوصيتك." else "Safeguard data locally or auto-send backups to Telegram, plus PIN security lock to keep records private.",
                icon = Icons.Filled.Backup,
                color = Color(0xFF0284C7),
                badge = "💾 10",
                highlights = listOf(
                    if (isArabic) "نسخ احتياطي تلقائي يرسل إلى تليجرام" else "Automatic backup to Telegram chat",
                    if (isArabic) "استعادة بياناتك بالكامل في أي وقت" else "Complete 1-tap data restoration",
                    if (isArabic) "قفل التطبيق برمز PIN لحماية البيانات" else "PIN code protection for total security"
                ),
                targetScreenRoute = Screen.Backup.route
            )
        )
    }

    val currentStep = tourSteps[currentStepIndex]
    val totalSteps = tourSteps.size

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.94f)
                .padding(6.dp)
                .testTag("app_guided_tour_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = currentStep.color.copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = currentStep.icon,
                                    contentDescription = null,
                                    tint = currentStep.color,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = L.appGuidedTour(),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = L.stepOf(currentStepIndex + 1, totalSteps),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = currentStep.color
                            )
                        }
                    }

                    // Skip button
                    TextButton(
                        onClick = {
                            AppPreferencesManager.setHasSeenTour(true)
                            onDismiss()
                        },
                        modifier = Modifier.testTag("tour_skip_btn")
                    ) {
                        Text(
                            text = L.skipTour(),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Step Progress Bar
                LinearProgressIndicator(
                    progress = { (currentStepIndex + 1).toFloat() / totalSteps.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = currentStep.color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Feature Banner Card
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = currentStep.color.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = currentStep.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp
                                    ),
                                    color = currentStep.color,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = currentStep.color
                                ) {
                                    Text(
                                        text = currentStep.badge,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Text(
                                text = currentStep.subtitle,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = currentStep.description,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    lineHeight = 21.sp,
                                    fontSize = 14.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // -------------------------------------------------------------
                    // HANDS-ON INTERACTIVE PLAYGROUND (المستخدم يجرب بجد)
                    // -------------------------------------------------------------
                    InteractiveStepPlayground(
                        stepIndex = currentStepIndex,
                        isArabic = isArabic,
                        accentColor = currentStep.color
                    )

                    // Key Highlights
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isArabic) "✨ أهم ما يميز هذا القسم:" else "✨ Key Highlights:",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            currentStep.highlights.forEach { highlight ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = currentStep.color,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = highlight,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Interactive Live Visit Page Shortcut
                    if (currentStep.targetScreenRoute != null && onNavigateToScreen != null) {
                        FilledTonalButton(
                            onClick = {
                                AppPreferencesManager.setHasSeenTour(true)
                                onNavigateToScreen(currentStep.targetScreenRoute)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tour_visit_page_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = currentStep.color.copy(alpha = 0.15f),
                                contentColor = currentStep.color
                            )
                        ) {
                            Icon(Icons.Filled.Launch, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isArabic) "تجربة والانتقال لصفحة ${currentStep.title} الآن 🚀" else "Go & Test ${currentStep.title} Now 🚀",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Step indicator dots
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(totalSteps) { index ->
                        val isCurrent = currentStepIndex == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (isCurrent) 10.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isCurrent) currentStep.color else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                .clickable {
                                    currentStepIndex = index
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Button
                    if (currentStepIndex > 0) {
                        OutlinedButton(
                            onClick = { currentStepIndex-- },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("tour_prev_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = L.previous(),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    // Next / Finish Button
                    Button(
                        onClick = {
                            if (currentStepIndex < totalSteps - 1) {
                                currentStepIndex++
                            } else {
                                AppPreferencesManager.setHasSeenTour(true)
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .weight(if (currentStepIndex > 0) 1.5f else 1f)
                            .height(48.dp)
                            .testTag("tour_next_finish_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentStepIndex == totalSteps - 1) EmeraldGreen else currentStep.color
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (currentStepIndex == totalSteps - 1) L.finishTour() else L.next(),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Hands-on interactive playground for each tour step allowing the teacher to test features live
 */
@Composable
private fun InteractiveStepPlayground(
    stepIndex: Int,
    isArabic: Boolean,
    accentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.35f)),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.TouchApp,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) "تجربة تفاعلية مباشرة (اضغط وجرب الآن):" else "Interactive Hands-on Trial:",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = accentColor
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isArabic) "حي وتفاعلي ⚡" else "Live Tryout ⚡",
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            when (stepIndex) {
                0 -> {
                    // Step 1: Dashboard Interactive Trial
                    var quickActionTapped by remember { mutableStateOf<String?>(null) }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (isArabic) "جرب الضغط على أحد أزرار الإجراء السريع للوحة التحكم:" else "Tap any quick action button below:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            FilledTonalButton(
                                onClick = { quickActionTapped = if (isArabic) "تم فتح تحضير الحضور بنجاح! ⚡" else "Attendance Opened!" },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.FactCheck, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isArabic) "حضور اليوم" else "Attendance", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            FilledTonalButton(
                                onClick = { quickActionTapped = if (isArabic) "تم فتح نافذة إضافة طالب جديد! ➕" else "New Student Dialog Opened!" },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isArabic) "طالب جديد" else "New Student", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (quickActionTapped != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EmeraldSuccessContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = quickActionTapped!!,
                                    color = EmeraldSuccess,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Step 2: Students & WhatsApp Interactive Trial
                    var isQrVisible by remember { mutableStateOf(false) }
                    var whatsAppSent by remember { mutableStateOf(false) }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("محمد أحمد علي", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("الصف الثالث الثانوي - مجموعة السبت", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    IconButton(
                                        onClick = { isQrVisible = !isQrVisible },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Filled.QrCode2, contentDescription = null, tint = NavyPrimary)
                                    }
                                    IconButton(
                                        onClick = { whatsAppSent = true },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Filled.Send, contentDescription = null, tint = Color(0xFF25D366))
                                    }
                                }
                            }
                        }
                        if (isQrVisible) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF0F172A),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "||| ||||| |||| ||||| STU-2026-0042 [QR CODE]",
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        if (whatsAppSent) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFDCFCE7),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isArabic) "💬 مرحباً ولي أمر الطالب محمد، نود إبلاغكم بحصوله على 10/10 في اختبار اليوم 🌟" else "WhatsApp message preview sent!",
                                    color = Color(0xFF166534),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // Step 3: Groups & Stages Interactive Trial
                    var selectedStageIndex by remember { mutableIntStateOf(2) } // Secondary
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf("🎒 ابتدائي", "📘 إعدادي", "🎓 ثانوي").forEachIndexed { index, title ->
                                val isSelected = selectedStageIndex == index
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFF7C3AED) else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedStageIndex = index }
                                ) {
                                    Text(
                                        text = title,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = when (selectedStageIndex) {
                                        0 -> "مجموعة المتفوقين (الصف الخامس) - 24 طالب"
                                        1 -> "مجموعة الأبطال (الصف الثاني الإعدادي) - 30 طالب"
                                        else -> "مجموعة النخبة (الثالث الثانوي) - 35 طالب"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text("250 ج.م", fontWeight = FontWeight.Black, color = Color(0xFF7C3AED), fontSize = 12.sp)
                            }
                        }
                    }
                }
                3 -> {
                    // Step 4: Schedule Interactive Trial
                    var selectedDay by remember { mutableStateOf("السبت") }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("السبت", "الأحد", "الاثنين", "الثلاثاء").forEach { day ->
                                val isChosen = selectedDay == day
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isChosen) Color(0xFF059669) else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedDay = day }
                                ) {
                                    Text(
                                        text = day,
                                        color = if (isChosen) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFECFDF5),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("حصة الرياضيات (سنتر الأوائل)", fontWeight = FontWeight.Bold, color = Color(0xFF065F46), fontSize = 12.sp)
                                    Text("04:00 م - 06:00 م ($selectedDay)", fontSize = 11.sp, color = Color(0xFF047857))
                                }
                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF059669)) {
                                    Text("مؤكدة ⏰", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(6.dp, 2.dp))
                                }
                            }
                        }
                    }
                }
                4 -> {
                    // Step 5: Attendance Interactive Trial
                    var attendanceStatus by remember { mutableStateOf("present") }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (isArabic) "جرّب تغيير حالة حضور الطالب بضغطة زر:" else "Tap a status to mark attendance:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            // Present
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (attendanceStatus == "present") EmeraldSuccessContainer else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(if (attendanceStatus == "present") 1.5.dp else 0.dp, EmeraldSuccess),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { attendanceStatus = "present" }
                            ) {
                                Text(
                                    text = "حاضر ✔️",
                                    fontWeight = FontWeight.Bold,
                                    color = if (attendanceStatus == "present") EmeraldSuccess else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                            // Late
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (attendanceStatus == "late") AmberGoldContainer else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(if (attendanceStatus == "late") 1.5.dp else 0.dp, AmberGoldDark),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { attendanceStatus = "late" }
                            ) {
                                Text(
                                    text = "متأخر ⏳",
                                    fontWeight = FontWeight.Bold,
                                    color = if (attendanceStatus == "late") AmberGoldDark else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                            // Absent
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (attendanceStatus == "absent") CrimsonErrorContainer else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(if (attendanceStatus == "absent") 1.5.dp else 0.dp, CrimsonError),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { attendanceStatus = "absent" }
                            ) {
                                Text(
                                    text = "غائب ❌",
                                    fontWeight = FontWeight.Bold,
                                    color = if (attendanceStatus == "absent") CrimsonError else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                5 -> {
                    // Step 6: Homework Scanner Interactive Trial
                    var homeworkState by remember { mutableStateOf("full") }
                    var photoTaken by remember { mutableStateOf(false) }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                                listOf("full" to "كامل 💯", "partial" to "ناقص ⚠️", "none" to "لم يحل ❌").forEach { (st, label) ->
                                    val isCur = homeworkState == st
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isCur) Color(0xFFD97706) else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { homeworkState = st }
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCur) Color.White else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(vertical = 6.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { photoTaken = !photoTaken },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFFEF3C7), RoundedCornerShape(8.dp))
                            ) {
                                Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = Color(0xFFD97706))
                            }
                        }
                        if (photoTaken) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFEF3C7),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isArabic) "📸 تم التقاط وحفظ صورة صفحة الواجب بنجاح!" else "Homework photo captured successfully!",
                                    color = Color(0xFF92400E),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                6 -> {
                    // Step 7: Question Bank Interactive Trial
                    var chosenOption by remember { mutableStateOf<String?>(null) }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "سؤال تجريبي: ما هي قيمة √144 + 5² ؟",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf("أ) 37", "ب) 29", "ج) 49", "د) 17").forEach { opt ->
                                val isSelected = chosenOption == opt
                                val isCorrect = opt.startsWith("أ")
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = when {
                                        isSelected && isCorrect -> EmeraldSuccessContainer
                                        isSelected && !isCorrect -> CrimsonErrorContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    border = BorderStroke(
                                        if (isSelected) 1.5.dp else 0.dp,
                                        if (isSelected && isCorrect) EmeraldSuccess else CrimsonError
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { chosenOption = opt }
                                ) {
                                    Text(
                                        text = opt,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            isSelected && isCorrect -> EmeraldSuccess
                                            isSelected && !isCorrect -> CrimsonError
                                            else -> MaterialTheme.colorScheme.onSurface
                                        },
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        if (chosenOption != null) {
                            Text(
                                text = if (chosenOption!!.startsWith("أ")) "إجابة صحيحة! أحسنت 🎯 (12 + 25 = 37)" else "إجابة غير صحيحة، جرّب الخيار (أ)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (chosenOption!!.startsWith("أ")) EmeraldSuccess else CrimsonError
                            )
                        }
                    }
                }
                7 -> {
                    // Step 8: Finance Interactive Trial
                    var collectedAmount by remember { mutableIntStateOf(150) }
                    val totalFee = 300
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("اشتراك الطالب: $totalFee ج.م", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("المدفوع: $collectedAmount ج.م | المتبقي: ${totalFee - collectedAmount} ج.م", fontSize = 11.sp, color = AmberGoldDark, fontWeight = FontWeight.Bold)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { collectedAmount = totalFee },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AmberGoldDark)
                            ) {
                                Text("تسجيل دفع كامل (300 ج.م)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { collectedAmount = 150 },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("تسجيل نصف المبلغ (150)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                8 -> {
                    // Step 9: Casio Scientific Calculator Interactive Trial
                    var casioLcd by remember { mutableStateOf("sin(30)") }
                    var casioResult by remember { mutableStateOf("0.5") }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF8E9F88),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(casioLcd, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 12.sp)
                                Text("= $casioResult", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, color = Color(0xFF0F172A), fontSize = 14.sp)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf("√144" to "12", "5²" to "25", "sin(30)" to "0.5", "log(100)" to "2").forEach { (exp, res) ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF1E242B),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            casioLcd = exp
                                            casioResult = res
                                        }
                                ) {
                                    Text(
                                        text = exp,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
                9 -> {
                    // Step 10: Backup & PIN Security Interactive Trial
                    var pinCode by remember { mutableStateOf("") }
                    val isUnlocked = pinCode == "1234"
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isUnlocked) "تم إلغاء القفل بنجاح! 🔓 (بياناتك آمنة)" else "أدخل الرمز التجريبي (1 2 3 4):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUnlocked) EmeraldSuccess else MaterialTheme.colorScheme.onSurface
                            )
                            TextButton(onClick = { pinCode = "" }) {
                                Text("مسح", fontSize = 11.sp)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf("1", "2", "3", "4").forEach { digit ->
                                Button(
                                    onClick = { if (pinCode.length < 4) pinCode += digit },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                                ) {
                                    Text(digit, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
