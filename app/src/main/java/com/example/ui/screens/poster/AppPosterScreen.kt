package com.example.ui.screens.poster

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.export.PdfReportExporter
import com.example.data.local.entity.TeacherEntity
import com.example.data.repository.TeacherPlannerRepository
import com.example.ui.components.AppTopBar
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

enum class DemoVisualType {
    BARCODE_SCAN,
    STUDY_FILES_GEOMETRIC,
    WHATSAPP_AUTO_MESSAGE,
    ATTENDANCE_RECORDING,
    FINANCE_CALCULATOR,
    EXAM_HONOR_BOARD,
    BACKUP_SYNC,
    MULTILINGUAL
}

data class DemoStep(
    val stepNumber: Int,
    val title: String,
    val description: String,
    val visualType: DemoVisualType
)

data class AppFeatureItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val icon: ImageVector,
    val primaryColor: Color,
    val containerColor: Color,
    val highlights: List<String>,
    val badge: String,
    val actionText: String,
    val demoSteps: List<DemoStep>,
    val onAction: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPosterScreen(
    repository: TeacherPlannerRepository,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateToAttendance: () -> Unit,
    onNavigateToStudents: () -> Unit,
    onNavigateToGroups: () -> Unit,
    onNavigateToSchedule: () -> Unit,
    onNavigateToFinance: () -> Unit,
    onNavigateToExams: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToCertificates: () -> Unit,
    onNavigateToSmartPrep: () -> Unit,
    onNavigateToQuestionBank: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToStudyFiles: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var teacher by remember { mutableStateOf<TeacherEntity?>(null) }
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var selectedFilterCategory by remember { mutableStateOf("الكل") }
    var activeDemoFeature by remember { mutableStateOf<AppFeatureItem?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            teacher = repository.getTeacherSync()
        }
    }

    val featureList = remember(teacher) {
        listOf(
            AppFeatureItem(
                id = "study_files_geometry",
                title = "الكتب والمذكرات والسبورة الهندسية الذكية",
                subtitle = "حفظ الكتب والملخصات لكل صف + الرسم بالمسطرة والبرجل والمنقلة والأشكال 2D/3D",
                category = "التعليم والمحتوى",
                icon = Icons.Filled.LibraryBooks,
                primaryColor = NavyPrimary,
                containerColor = NavyPrimaryContainer,
                highlights = listOf(
                    "رفع كتب الوزارة والمذكرات والملخصات وحفظ نسخة دائمة على الجهاز لكل مرحلة دراسية",
                    "عارض PDF احترافي فائق الدقة مع تدوين الملاحظات والتظليل",
                    "أدوات هندسية تفاعلية: مسطرة قياس، فرجار (برجل) لرسم الدوائر، منقلة لقياس الزوايا",
                    "رسم الأشكال الهندسية المستوية (مثلثات، متوازي، دائرة) والمجسمات الفراغية ثلاثية الأبعاد (مكعب، اسطوانة، مخروط)"
                ),
                badge = "PDF & GEOMETRY 📐",
                actionText = "فتح مكتبة الكتب والسبورة",
                demoSteps = listOf(
                    DemoStep(1, "اختر الصف الدراسي وارفع الكتاب", "افتح صفحة الكتب واختر الصف المطلوب (إعدادي أو ثانوي) ثم ارفع الكتاب أو المذكرة بنقرة واحدة.", DemoVisualType.STUDY_FILES_GEOMETRIC),
                    DemoStep(2, "افتح عارض PDF والسبورة", "انقر على زر 'فتح وشرح' لعرض صفحات الكتاب بجودة عالية مع خيارات السبورة البيضاء أو كشكول المربعات.", DemoVisualType.STUDY_FILES_GEOMETRIC),
                    DemoStep(3, "استخدم الأدوات الهندسية", "اختر المسطرة أو البرجل أو المنقلة لرسم الخطوط والدوائر والزوايا والأشكال الهندسية 2D و 3D ومشاركتها مع الطلاب.", DemoVisualType.STUDY_FILES_GEOMETRIC)
                ),
                onAction = onNavigateToStudyFiles
            ),
            AppFeatureItem(
                id = "smart_messages",
                title = "الرسائل الذكية التلقائية حسب مستوى الطالب",
                subtitle = "رسائل واتساب متغيرة آلياً: تهنئة وتفوق للدرجات العالية، أو تنبيه ومتابعة للدرجات الضعيفة",
                category = "إدارة الحصص",
                icon = Icons.Filled.Chat,
                primaryColor = EmeraldSuccess,
                containerColor = EmeraldSuccessContainer,
                highlights = listOf(
                    "صياغة رسالة واتساب ديناميكية تتغير تلقائياً حسب درجة الطالب ونسبته المئوية",
                    "إذا حصل الطالب على درجة ممتازة: يُكتب تلقائياً نص التهنئة بالتميز مع التمني بدوام النجاح والتفوق",
                    "إذا حصل الطالب على درجة ضعيفة: يُكتب تلقائياً تنبيه ولي الأمر بضرورة المتابعة والاهتمام دون الحاجة لكتابة يدوية",
                    "إمكانية تخصيص قالب الرسائل والمتغيرات ({student_name}، {score}، {max_score}، {status})"
                ),
                badge = "SMART MESSAGES 💬",
                actionText = "تجربة إرسال رسائل الامتحانات",
                demoSteps = listOf(
                    DemoStep(1, "رصد درجات الامتحان", "سجل درجات الطلاب في الامتحان بسهولة وسيقوم التطبيق بحساب النسبة المئوية.", DemoVisualType.WHATSAPP_AUTO_MESSAGE),
                    DemoStep(2, "توليد نص الرسالة الذكي", "يقارن النظام درجة الطالب: إن كانت ممتازة يضيف عبارات التهنئة والفخر، وإن كانت ضعيفة يضيف تنبيه المتابعة.", DemoVisualType.WHATSAPP_AUTO_MESSAGE),
                    DemoStep(3, "إرسال لواتساب ولي الأمر", "بنقرة واحدة، يُفتح تطبيق واتساب مع الرسالة المكتوبة بدقة متضمنة اسم الطالب ودرجته والتقييم.", DemoVisualType.WHATSAPP_AUTO_MESSAGE)
                ),
                onAction = onNavigateToExams
            ),
            AppFeatureItem(
                id = "barcode_id",
                title = "نظام الباركود وكروت الهوية الذكية",
                subtitle = "مسح فوري بالكاميرا وطباعة كروت ID للطلاب",
                category = "التنظيم والذكاء",
                icon = Icons.Filled.QrCodeScanner,
                primaryColor = IndigoExam,
                containerColor = IndigoExamContainer,
                highlights = listOf(
                    "مسح الباركود وQR بالكاميرا مع ضوء فلاش واهتزاز وصوت تأكيد",
                    "منع التسجيل المزدوج في الحصة بفاصل زمني ذكي",
                    "توليد وطباعة بطاقات ID للطلاب تحتوي على QR Code والباركود ومعلومات ولي الأمر",
                    "البحث السريع والتلقائي عن الطالب فور المسح"
                ),
                badge = "QR & ID CARDS",
                actionText = "تجربة الحضور بالباركود",
                demoSteps = listOf(
                    DemoStep(1, "طباعة بطاقة الطالب ID", "يولد التطبيق كارت رسمي لكل طالب مزود بـ QR كود والباركود.", DemoVisualType.BARCODE_SCAN),
                    DemoStep(2, "مسح الكارت عند دخول الحصة", "وجه الكاميرا لبطاقة الطالب ليتم تسجيل حضوره فورياً وإصدار صوت واهتزاز تأكيد.", DemoVisualType.BARCODE_SCAN),
                    DemoStep(3, "إرسال إشعار الدخول", "يمكن إرسال رسالة واتساب لولي الأمر تفيد بوصول الطالب للحصة بأمان.", DemoVisualType.BARCODE_SCAN)
                ),
                onAction = onNavigateToAttendance
            ),
            AppFeatureItem(
                id = "attendance_hw",
                title = "الحضور والغياب ومتابعة الواجبات",
                subtitle = "كشف ذكي، تقييم الواجب، وإنذارات واتساب فورية",
                category = "إدارة الحصص",
                icon = Icons.Filled.FactCheck,
                primaryColor = NavyPrimary,
                containerColor = NavyPrimaryContainer,
                highlights = listOf(
                    "رصد الحضور بنقرة واحدة (حاضر، غائب، متأخر، بعذر)",
                    "تقييم دقيق للواجب (حل كامل، حل ناقص، لم يحل، معفى)",
                    "إرسال تقرير المتابعة اليومي الفردي لواتساب ولي الأمر بنقرة واحدة",
                    "إرسال إنذار غياب جماعي فوري لجميع أولياء أمور الغائبين"
                ),
                badge = "SMART ATTENDANCE",
                actionText = "فتح كشف الحضور",
                demoSteps = listOf(
                    DemoStep(1, "اختيار الحصة والمجموعة", "افتح حصة اليوم لمجموعتك وسيعرض التطبيق قائمة الطلاب تلقائياً.", DemoVisualType.ATTENDANCE_RECORDING),
                    DemoStep(2, "تسجيل الحضور وتقييم الواجب", "اضغط على أزرار الحضور والواجب لكل طالب بسهولة فائقة وسرعة.", DemoVisualType.ATTENDANCE_RECORDING),
                    DemoStep(3, "إنذار الغائبين بضغطة زر", "اضغط على زر 'إنذار الغائبين' لإرسال رسائل التنبيه لجميع الغائبين دفعة واحدة.", DemoVisualType.ATTENDANCE_RECORDING)
                ),
                onAction = onNavigateToAttendance
            ),
            AppFeatureItem(
                id = "groups_venues",
                title = "المجموعات والقاعات والسناتر وتعدد الأترام",
                subtitle = "إدارة متعددة السناتر وحساب تكاليف الإيجار وترحيل الأترام تلقائياً",
                category = "التنظيم والذكاء",
                icon = Icons.Filled.Class,
                primaryColor = Color(0xFF7C3AED),
                containerColor = Color(0xFFF3E8FF),
                highlights = listOf(
                    "توزيع الطلاب على المراحل والمجموعات والسناتر الدراسية",
                    "نظام إدارة الأترام والفصول الدراسية مع ترحيل الدرجات بنقرة زر",
                    "تسجيل قاعات الدروس مع نظام تسعير الإيجار (مبلغ ثابت، نسبة، أو لكل طالب)",
                    "روابط مباشرة لمجموعات واتساب لكل مجموعة لسهولة التواصل"
                ),
                badge = "VENUES & GROUPS",
                actionText = "إدارة المجموعات والسناتر",
                demoSteps = listOf(
                    DemoStep(1, "إنشاء المجموعة وتحديد السنتر", "حدد اسم المجموعة، المرحلة، موعد الحصة، ونظام إيجار القاعة.", DemoVisualType.FINANCE_CALCULATOR),
                    DemoStep(2, "ربط الكتب والمذكرات", "تصفح الكتب والمذكرات المرفوعة لهذه المجموعة مباشرة من صفحتها.", DemoVisualType.FINANCE_CALCULATOR),
                    DemoStep(3, "ترحيل الترم", "عند انتهاء الترم الأول، انقل المجموعة للترم الثاني مع أرشفة السجلات.", DemoVisualType.FINANCE_CALCULATOR)
                ),
                onAction = onNavigateToGroups
            ),
            AppFeatureItem(
                id = "reports_certificates",
                title = "التقارير الأكاديمية وشهادات التقدير",
                subtitle = "كشوف درجات PDF، تصدير Excel، ومصمم شهادات مذهب",
                category = "التقارير والشهادات",
                icon = Icons.Filled.WorkspacePremium,
                primaryColor = Color(0xFFD97706),
                containerColor = Color(0xFFFEF3C7),
                highlights = listOf(
                    "طباعة كشوف درجات شهرية وتراكمية PDF رسمية بألوان راقية",
                    "تصدير كشوف الطلاب والدرجات لملفات Excel (.xlsx / .csv)",
                    "مصمم شهادات تقدير احترافي بقوالب مذهبة وطباعة فورية",
                    "لوحة شرف الأوائل والمتفوقين مع حساب النسبة التراكمية"
                ),
                badge = "HONOR & REPORTS",
                actionText = "تصميم شهادات وتقارير",
                demoSteps = listOf(
                    DemoStep(1, "اختر الطالب أو المجموعة", "حدد الامتحان أو الشهر المطلوب استخراج تقريره.", DemoVisualType.EXAM_HONOR_BOARD),
                    DemoStep(2, "اختر قالب الشهادة", "اختر إطار الشهادة المذهب المناسب والمسمى التكريمي.", DemoVisualType.EXAM_HONOR_BOARD),
                    DemoStep(3, "تصدير وطباعة PDF", "صدر الشهادة كملف PDF عالي الجودة أو شاركه عبر واتساب مع الطالب.", DemoVisualType.EXAM_HONOR_BOARD)
                ),
                onAction = onNavigateToCertificates
            ),
            AppFeatureItem(
                id = "backup_restore",
                title = "النسخ الاحتياطي التلقائي واليدوي وتأمين البيانات",
                subtitle = "حفظ البيانات دورياً، خيار تفعيل/إلغاء النسخ التلقائي، واستعادة فورية",
                category = "الأمان والملف",
                icon = Icons.Filled.Backup,
                primaryColor = Color(0xFF0F172A),
                containerColor = Color(0xFFF1F5F9),
                highlights = listOf(
                    "إمكانية تشغيل أو إلغاء النسخ الاحتياطي التلقائي عند إغلاق التطبيق",
                    "تحديد مسار الحفظ (الذاكرة الداخلية أو السحابية)",
                    "تصدير ملف النسخة الاحتياطية ومشاركته على جوجل درايف أو تيليجرام",
                    "استرجاع فوري لجميع الطلاب والحصص والدرجات والكتب عند تغيير الهاتف"
                ),
                badge = "AUTO BACKUP 💾",
                actionText = "إعدادات النسخ الاحتياطي",
                demoSteps = listOf(
                    DemoStep(1, "ضبط خيارات النسخ التلقائي", "فعل أو ألغِ النسخ التلقائي واختر التكرار المناسب.", DemoVisualType.BACKUP_SYNC),
                    DemoStep(2, "إنشاء نسخة احتياطية فورية", "اضغط على 'إنشاء نسخة احتياطية الآن' لحفظ ملف كامل مشفر.", DemoVisualType.BACKUP_SYNC),
                    DemoStep(3, "الاستعادة عند الحاجة", "استعد بياناتك في ثوانٍ معدودة دون فقدان أي درجة أو كشف حضور.", DemoVisualType.BACKUP_SYNC)
                ),
                onAction = onNavigateToBackup
            ),
            AppFeatureItem(
                id = "multilingual_system",
                title = "تعدد اللغات (العربية، الإنجليزية، الفرنسية)",
                subtitle = "دعم كامل للواجهة والتقارير مع الحفاظ على المعاني الدقيقة والتبديل من الشاشة الرئيسية",
                category = "التنظيم والذكاء",
                icon = Icons.Filled.Language,
                primaryColor = Color(0xFF2563EB),
                containerColor = Color(0xFFDBEAFE),
                highlights = listOf(
                    "زر فوري لتغيير لغة التطبيق في الشاشة الرئيسية (عربي، English، Français)",
                    "ترجمة شاملة لجميع المصطلحات الأكاديمية والمالية مع الحفاظ على المعنى التعليمي الصحيح",
                    "تغيير اتجاه الواجهة من اليمين لليسار (RTL/LTR) بسلاسة كاملة",
                    "تقارير درجات وكروت حضور باللغة المختارة لتناسب المدارس اللغات والدولية"
                ),
                badge = "MULTILINGUAL 🌐",
                actionText = "تغيير لغة التطبيق",
                demoSteps = listOf(
                    DemoStep(1, "زر اللغة في الشاشة الرئيسية", "انقر على زر اللغة أعلى الشاشة الرئيسية أو في الإعدادات.", DemoVisualType.MULTILINGUAL),
                    DemoStep(2, "اختر اللغة المطلوبة", "اختر من بين العربية أو الإنجليزية أو الفرنسية.", DemoVisualType.MULTILINGUAL),
                    DemoStep(3, "تطبيق فوري", "تتحول كافة شاشات التطبيق والتقارير للغة الجديدة فوراً.", DemoVisualType.MULTILINGUAL)
                ),
                onAction = onNavigateToProfile
            )
        )
    }

    val categories = remember {
        listOf("الكل", "التعليم والمحتوى", "إدارة الحصص", "التنظيم والذكاء", "التقارير والشهادات", "الأمان والملف")
    }

    val filteredFeatures = remember(selectedFilterCategory, featureList) {
        if (selectedFilterCategory == "الكل") featureList
        else featureList.filter { it.category == selectedFilterCategory }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "مميزات التطبيق والدروس الإرشادية 🌟",
                subtitle = "شرح توضيحي ومتحرك لكيفية استخدام كل ميزة",
                onNavigateBack = onNavigateBack,
                onNavigateHome = onNavigateHome,
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                isGeneratingPdf = true
                                try {
                                    val pdfFile = withContext(Dispatchers.IO) {
                                        PdfReportExporter.generateAppFeaturePosterPdf(context, teacher)
                                    }
                                    PdfReportExporter.sharePdf(context, pdfFile, "دليل ومميزات تطبيق المعلم المساعد")
                                    Toast.makeText(context, "تم إنشاء ومشاركة دليل المميزات بنجاح 📄✨", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "حدث خطأ أثناء تصدير PDF: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isGeneratingPdf = false
                                }
                            }
                        },
                        modifier = Modifier.testTag("export_poster_pdf_btn")
                    ) {
                        if (isGeneratingPdf) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = "تصدير المميزات PDF", tint = Color.White)
                        }
                    }

                    IconButton(
                        onClick = {
                            val teacherName = teacher?.name?.ifEmpty { "أستاذ المادة" } ?: "أستاذ المادة"
                            val subject = teacher?.subject?.ifEmpty { "جميع المراحل" } ?: "المادة التخصصية"
                            val shareText = """
                                🌟 *دليل مميزات المنظومة التعليمية الرقمية لإدارة الدروس والطلاب* 🌟
                                👨‍🏫 إشراف: *$teacherName* - أستاذ ($subject)
                                
                                📱 *أهم مميزات النظام التعليمي الذكي:*
                                1️⃣ *الكتب والمذكرات والسبورة الهندسية:* رفع الكتب والشرح بالمسطرة والبرجل والمنقلة والأشكال الهندسية.
                                2️⃣ *الرسائل التلقائية الذكية:* تغيير نص رسالة واتساب حسب نتيجة الطالب (تهنئة للتفوق، أو تنبيه للمتابعة).
                                3️⃣ *نظام الباركود وQR:* مسح فوري بالهاتف وطباعة كروت ID لكل طالب.
                                4️⃣ *الحضور والواجبات:* رصد الحضور بنقرة زر + إنذار غياب جماعي فوري بالواتساب.
                                5️⃣ *المجموعات وتعدد الأترام:* إدارة القاعات والسناتر وترحيل الدرجات للأترام الجديدة.
                                6️⃣ *التقارير وشهادات التقدير:* كشوف درجات رسمية وشهادات مذهبة.
                                7️⃣ *النسخ الاحتياطي وتعدد اللغات:* خيارات تشغيل/إلغاء النسخ التلقائي ودعم اللغات.
                                
                                ✨ تجربة تعليمية متكاملة واحترافية ✨
                            """.trimIndent()

                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "مشاركة مميزات التطبيق"))
                        },
                        modifier = Modifier.testTag("share_poster_text_btn")
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "مشاركة", tint = Color.White)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Hero Header Banner
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = NavyPrimary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("poster_hero_banner")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = AmberGold.copy(alpha = 0.2f),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = AmberGold, modifier = Modifier.size(30.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "دليل مميزات التطبيق والشرح المتحرك",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "انقر على أي ميزة لمشاهدة شرح متحرك خطوة بخطوة لكيفية عملها واستخدامها في تدريسك اليومي",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 2. Category Filter Chips
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        val isSelected = selectedFilterCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilterCategory = cat },
                            label = { Text(cat, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NavyPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // 3. Feature Cards List with Demo launcher
            items(filteredFeatures, key = { it.id }) { feat ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .clickable { activeDemoFeature = feat }
                        .testTag("feature_card_${feat.id}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                                    shape = RoundedCornerShape(12.dp),
                                    color = feat.containerColor,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = feat.icon,
                                            contentDescription = null,
                                            tint = feat.primaryColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = feat.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2
                                    )
                                    Text(
                                        text = feat.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = feat.primaryColor.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = feat.badge,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = feat.primaryColor,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Highlights Bullet Points
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            feat.highlights.forEach { highlight ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = feat.primaryColor,
                                        modifier = Modifier.size(15.dp).padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = highlight,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Actions Row: 1. Play Demo Animation, 2. Open Feature
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { activeDemoFeature = feat },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Icon(Icons.Filled.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = feat.primaryColor)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("شرح متحرك 🎬", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = feat.primaryColor)
                            }

                            Button(
                                onClick = feat.onAction,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = feat.primaryColor),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Text(feat.actionText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // ----------------------------------------------------
    // ANIMATED FEATURE DEMO DIALOG
    // ----------------------------------------------------
    activeDemoFeature?.let { feat ->
        var currentStepIndex by remember { mutableStateOf(0) }
        val steps = feat.demoSteps
        val currentStep = steps.getOrNull(currentStepIndex) ?: steps.first()

        AlertDialog(
            onDismissRequest = { activeDemoFeature = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = feat.containerColor,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(feat.icon, contentDescription = null, tint = feat.primaryColor, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(feat.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                        Text("خطوة ${currentStepIndex + 1} من ${steps.size}", style = MaterialTheme.typography.labelSmall, color = feat.primaryColor)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Animated Step Visual Simulation Canvas
                    AnimatedFeatureVisualSimulation(visualType = currentStep.visualType, primaryColor = feat.primaryColor)

                    // Step Title & Description
                    Text(
                        text = currentStep.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = currentStep.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    // Step Dots Indicator
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        steps.indices.forEach { index ->
                            Box(
                                modifier = Modifier
                                    .size(if (index == currentStepIndex) 10.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(if (index == currentStepIndex) feat.primaryColor else Color.Gray.copy(alpha = 0.3f))
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (currentStepIndex < steps.size - 1) {
                            currentStepIndex++
                        } else {
                            activeDemoFeature = null
                            feat.onAction()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = feat.primaryColor)
                ) {
                    Text(if (currentStepIndex < steps.size - 1) "الخطوة التالية ⬅️" else "جرب الميزة الآن ✨")
                }
            },
            dismissButton = {
                if (currentStepIndex > 0) {
                    TextButton(onClick = { currentStepIndex-- }) {
                        Text("الخطوة السابقة")
                    }
                } else {
                    TextButton(onClick = { activeDemoFeature = null }) {
                        Text("إغلاق")
                    }
                }
            }
        )
    }
}

// ----------------------------------------------------
// ANIMATED CANVAS SIMULATION FOR FEATURES
// ----------------------------------------------------

@Composable
fun AnimatedFeatureVisualSimulation(
    visualType: DemoVisualType,
    primaryColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "demo_anim")
    val animatedProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (visualType) {
                DemoVisualType.STUDY_FILES_GEOMETRIC -> {
                    // Animated Ruler and Compass drawing a circle & triangle
                    Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val r = 40f

                        // Draw animated circle arc
                        drawArc(
                            color = primaryColor,
                            startAngle = 0f,
                            sweepAngle = animatedProgress * 360f,
                            useCenter = false,
                            style = Stroke(width = 3.dp.toPx()),
                            size = Size(r * 2, r * 2),
                            topLeft = Offset(cx - r, cy - r)
                        )

                        // Draw straight ruler line
                        val lineEnd = Offset(cx - 80f + (animatedProgress * 160f), cy + 45f)
                        drawLine(
                            color = Color(0xFFDC2626),
                            start = Offset(cx - 80f, cy + 45f),
                            end = lineEnd,
                            strokeWidth = 3.dp.toPx()
                        )
                    }
                }

                DemoVisualType.WHATSAPP_AUTO_MESSAGE -> {
                    // Animated WhatsApp message card preview
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFDCF8C6),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("💬 رسالة ولي الأمر التلقائية:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF075E54))
                                Text(
                                    if (animatedProgress < 0.5f)
                                        "🌟 نبارك للطالب (أحمد) حصوله على (10/10) في امتحان الرياضيات. متمنيين دوام التميز والتفوق! 👏"
                                    else
                                        "⚠️ نفيدكم بحصول الطالب (محمد) على (3/10) في امتحان الرياضيات. يرجى المتابعة والاهتمام 📚",
                                    fontSize = 11.sp,
                                    color = Color(0xFF0F172A),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                DemoVisualType.BARCODE_SCAN -> {
                    // Animated Barcode Scanning Laser
                    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        val w = size.width
                        val h = size.height
                        val laserY = h * animatedProgress

                        // Draw QR box
                        drawRoundRect(
                            color = primaryColor.copy(alpha = 0.2f),
                            size = Size(w * 0.7f, h),
                            topLeft = Offset(w * 0.15f, 0f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f)
                        )

                        // Laser sweep line
                        drawLine(
                            color = Color(0xFFDC2626),
                            start = Offset(w * 0.1f, laserY),
                            end = Offset(w * 0.9f, laserY),
                            strokeWidth = 3.dp.toPx()
                        )
                    }
                }

                DemoVisualType.ATTENDANCE_RECORDING -> {
                    // Animated Checkmarks
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("أحمد علي" to true, "سارة محمود" to true, "يوسف حسن" to (animatedProgress > 0.5f)).forEach { (name, isChecked) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isChecked) EmeraldSuccess else Color.Gray.copy(alpha = 0.3f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(name, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                DemoVisualType.FINANCE_CALCULATOR -> {
                    // Animated Revenue Bar Chart
                    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        val barWidth = size.width / 5f
                        val h1 = size.height * 0.8f * animatedProgress
                        val h2 = size.height * 0.4f * animatedProgress
                        val h3 = size.height * 0.65f * animatedProgress

                        drawRect(EmeraldSuccess, Offset(size.width * 0.15f, size.height - h1), Size(barWidth, h1))
                        drawRect(Color(0xFFDC2626), Offset(size.width * 0.42f, size.height - h2), Size(barWidth, h2))
                        drawRect(NavyPrimary, Offset(size.width * 0.7f, size.height - h3), Size(barWidth, h3))
                    }
                }

                DemoVisualType.EXAM_HONOR_BOARD -> {
                    // Golden Honor Award Medal
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = AmberGold.copy(alpha = 0.2f),
                            modifier = Modifier.size(50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = AmberGold, modifier = Modifier.size(32.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("لوحة شرف الأوائل والمتفوقين 🏆", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberGold)
                    }
                }

                DemoVisualType.BACKUP_SYNC -> {
                    // Cloud Upload Animation
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = primaryColor.copy(alpha = 0.15f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = primaryColor, modifier = Modifier.size(28.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            if (animatedProgress < 0.5f) "جاري النسخ الاحتياطي التلقائي... ⏳" else "تم حفظ النسخة الاحتياطية بنجاح! 💾✨",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                    }
                }

                DemoVisualType.MULTILINGUAL -> {
                    // Multilingual Flag switcher
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("العربية 🇪🇬", "English 🇺🇸", "Français 🇫🇷").forEachIndexed { i, lang ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if ((animatedProgress * 3).toInt() == i) primaryColor else MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = lang,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if ((animatedProgress * 3).toInt() == i) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
