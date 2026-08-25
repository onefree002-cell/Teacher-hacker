package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "teachers")
data class TeacherEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "عبده أيمن",
    val title: String = "أستاذ المادة والمشرف الأكاديمي",
    val subject: String = "جميع المواد",
    val phone: String = "01206150946",
    val whatsapp: String = "01206150946",
    val centerName: String = "سنتر التفوق والتميز",
    val address: String = "شارع الجمهورية - الدور الثالث",
    val logoUri: String? = null,
    val showLogoInPrintouts: Boolean = true,
    val bio: String = "معلم خبير ومتخصص في تبسيط الشرح وتأسيس الطلاب بأحدث الأساليب التربوية المبتكرة.",
    val experienceYears: String = "خبرة 10 سنوات",
    val degrees: String = "بكالوريوس ودبلوم مهني في طرق التدريس الحديثة",
    val telegram: String = "",
    val facebookPage: String = "",
    val youtubeChannel: String = "",
    val stagesTaught: String = "المرحلة الثانوية والإعدادية",
    val teachingFeatures: String = "متابعة دورية أسبوعية • بنك أسئلة شامل • تقارير فورية لولي الأمر • تدريب مكثف على الامتحانات",
    val notes: String = ""
)

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val groupNumber: String = "",
    val stage: String = "ثانوي", // "ابتدائي", "إعدادي", "ثانوي", "أخرى"
    val grade: String = "الصف الأول الثانوي",
    val termsCount: Int = 2, // 1 (ترم واحد), 2 (ترمان), 3 (ثلاثة أترام)
    val currentTerm: String = "الترم الأول", // مسمى الترم الحالي (e.g. "الترم الأول 2025/2026")
    val pricingType: String = "monthly", // "monthly" or "per_session"
    val monthlyPrice: Double = 0.0,
    val sessionDays: String = "", // e.g. "السبت, الثلاثاء"
    val sessionTime: String = "16:00",
    val durationMinutes: Int = 90,
    val location: String = "",
    val whatsappGroupLink: String = "", // رابط جروب واتساب للمجموعة
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val groupId: Long = 0,
    val grade: String = "",
    val phone: String = "",
    val parentPhone: String = "",
    val address: String = "",
    val status: String = "active", // "active", "inactive", "suspended"
    val notes: String = "",
    val isExempt: Boolean = false,
    val discountPercent: Double = 0.0,
    val barcodeCode: String = "", // unique student code (e.g. STD-1001)
    val tags: String = "", // comma-separated tags e.g. "متميز, يحتاج متابعة"
    val photoUri: String? = null, // Student profile picture URI / local file path
    val audioNoteUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val day: String = "", // e.g. "السبت"
    val time: String = "16:00",
    val date: String = "", // YYYY-MM-DD
    val durationMinutes: Int = 90,
    val location: String = "",
    val completed: Boolean = false,
    val term: String = "الترم الأول",
    val homeworkTitle: String = "", // واجب الحصة
    val homeworkPages: String = "", // الصفحات أو أرقام المسائل
    val homeworkNotes: String = "", // تعليمات خاصة بالواجب
    val homeworkDeadline: String = "", // موعد تسليم الواجب
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "attendance")
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val sessionId: Long = 0,
    val groupId: Long = 0,
    val date: String, // YYYY-MM-DD
    val status: String = "present", // "present" (حاضر), "absent" (غائب), "late" (متأخر), "excused" (بعذر)
    val homeworkStatus: String = "none", // "none", "completed" (كامل), "partial" (ناقص), "not_done" (لم يحل), "exempt" (معفى)
    val term: String = "الترم الأول",
    val isMakeUp: Boolean = false, // حضور تعويضي
    val originalGroupId: Long = 0,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val title: String,
    val maxScore: Double = 100.0,
    val passScore: Double = 50.0,
    val date: String, // YYYY-MM-DD
    val term: String = "الترم الأول",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "exam_grades")
data class ExamGradeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val examId: Long,
    val studentId: Long,
    val score: Double = 0.0,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val groupId: Long = 0,
    val amount: Double,
    val date: String, // YYYY-MM-DD
    val type: String = "monthly", // "monthly", "session", "book", "other"
    val monthName: String = "", // e.g. "أكتوبر 2024"
    val term: String = "الترم الأول",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val date: String, // YYYY-MM-DD
    val category: String = "عام", // "سنتر", "مطبوعات", "أدوات", "أخرى"
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val targetType: String = "general", // "general", "student", "group"
    val targetId: Long = 0,
    val date: String = "",
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "venues")
data class VenueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, // e.g. "سنتر الأوائل - قاعة 1"
    val address: String = "",
    val phone: String = "",
    val managerName: String = "",
    val rentType: String = "percentage", // "percentage", "per_hour", "per_student", "fixed_monthly"
    val rentValue: Double = 0.0,
    val notes: String = "",
    val colorHex: String = "#1E3A8A",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val mapLocationName: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "certificate_settings")
data class CertificateSettingEntity(
    @PrimaryKey val id: Int = 1,
    val title: String = "شهادة تفوق وتقدير",
    val schoolName: String = "أكاديمية التفوق التعليمية",
    val subtitle: String = "تقديراً للجهد المتميز والأداء الرائع في الفصل الدراسي",
    val bodyTemplate: String = "يسرنا منح هذه الشهادة للجهود المبذولة والدرجات المتميزة",
    val signatureName: String = "أستاذ المادة",
    val primaryColorHex: String = "#1E3A8A",
    val secondaryColorHex: String = "#D97706",
    val accentColorHex: String = "#10B981",
    val titleColorHex: String = "#1E3A8A",
    val studentNameColorHex: String = "#B45309",
    val bodyTextColorHex: String = "#1F2937",
    val subtitleColorHex: String = "#4B5563",
    val signatureColorHex: String = "#1E3A8A",
    val schoolNameColorHex: String = "#374151",
    val templateId: String = "classic_gold", // classic_gold, modern_navy, emerald_luxury, imperial_burgundy, dark_onyx_gold
    val logoUri: String? = null,
    val removeLogoBackground: Boolean = true,
    val presetLogo: String = "crown", // "crown", "trophy", "medal", "quill", "book", "custom"
    val showSeal: Boolean = true,
    val sealText: String = "اعتماد الأستاذ"
)

@Entity(tableName = "report_settings")
data class ReportSettingEntity(
    @PrimaryKey val id: Int = 1,
    val showStudentName: Boolean = true,
    val showGroup: Boolean = true,
    val showGrade: Boolean = true,
    val showPhone: Boolean = true,
    val showParentPhone: Boolean = true,
    val showAddress: Boolean = true,
    val showStatus: Boolean = true,
    val showPrice: Boolean = true,
    val showPayments: Boolean = true,
    val showAttendance: Boolean = true,
    val showExams: Boolean = true,
    val showNotes: Boolean = true,
    val headerTitle: String = "تقرير متابعة الطالب",
    val centerName: String = ""
)

@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String = "",
    val grade: String = "",
    val unitLesson: String = "",
    val questionText: String = "",
    val questionType: String = "mcq", // "mcq" (اختيار من متعدد), "true_false" (صح/خطأ), "essay" (مقالي)
    val optionA: String = "",
    val optionB: String = "",
    val optionC: String = "",
    val optionD: String = "",
    val correctAnswer: String = "",
    val difficulty: String = "متوسط", // "سهل", "متوسط", "صعب", "متميزين"
    val marks: Double = 5.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "curriculum")
data class CurriculumEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val grade: String = "",
    val subject: String = "",
    val unitTitle: String = "",
    val lessonTitle: String = "",
    val orderIndex: Int = 0,
    val isCompleted: Boolean = false,
    val completionDate: String = "",
    val notes: String = "",
    val groupId: Long = 0, // 0 for all groups of grade
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "material_deliveries")
data class MaterialDeliveryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val materialName: String, // e.g. "مذكرة الفصل الأول"
    val price: Double = 0.0,
    val isDelivered: Boolean = true,
    val isPaid: Boolean = false,
    val deliveryDate: String = "", // YYYY-MM-DD
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "homework_submissions")
data class HomeworkSubmissionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val groupId: Long = 0,
    val title: String, // e.g. "واجب الدرس الأول - الفيزياء"
    val assignedDate: String = "", // YYYY-MM-DD
    val photoUri: String = "", // Camera or Gallery Image path/Uri
    val audioFeedbackUri: String = "", // Teacher recorded voice note path
    val score: Double = 10.0,
    val maxScore: Double = 10.0,
    val rating: String = "ممتاز", // "ممتاز", "جيد جداً", "جيد", "مقبول", "يحتاج إعادة"
    val feedbackNote: String = "",
    val status: String = "corrected", // "corrected", "pending", "needs_resubmission"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "voice_notes")
data class VoiceNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String, // e.g. "شرح مسألة حسابية معقدة"
    val groupId: Long = 0,
    val studentId: Long = 0,
    val filePath: String, // Storage file path
    val durationSeconds: Int = 0,
    val category: String = "شرح درس", // "شرح درس", "توجيه لأولياء الأمور", "ملاحظة واجب", "تسجيل عام"
    val date: String = "", // YYYY-MM-DD
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "lesson_plans")
data class LessonPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String, // عنوان الدرس أو الموضوع
    val subject: String = "", // المادة الدراسية
    val grade: String = "", // الصف الدراسي
    val unitName: String = "", // الوحدة / الفصل
    val groupId: Long = 0, // ربط بمجموعة محددة أو 0 للكل
    val lessonDate: String = "", // YYYY-MM-DD
    val targetDate: String = "", // تاريخ تنفيذ الدرس YYYY-MM-DD
    val durationMinutes: Int = 90, // زمن الحصة بالدقائق
    val templateType: String = "standard", // standard, interactive, revision, lab
    val objectives: String = "", // الأهداف التعليمية (معرفية، مهارية، وجدانية)
    val mindMapPoints: String = "", // الخريطة الذهنية والأفكار الأساسية والمحاور
    val keyPoints: String = "", // عناصر ومحاور الشرح الأساسية
    val warmUpIntro: String = "", // التهيئة والتمهيد للدرس وسؤال التحدي
    val warmupHook: String = "", // سؤال التحدي والتمهيد المشوق
    val teachingStrategies: String = "", // استراتيجيات التدريس والوسائل التعليمية
    val teachingAids: String = "", // الوسائل التعليمية والتكنولوجيا المستخدمة
    val activities: String = "", // الأنشطة الطلابية والتطبيق العملي
    val boardPlan: String = "", // خطة تقسيم السبورة (يمين، وسط، يسار)
    val timeBreakdown: String = "", // التوزيع الزمني للحصة
    val assessmentQuestions: String = "", // أسئلة التقويم وقياس الفهم
    val homeworkAssignment: String = "", // الواجب المنزلي والمسائل
    val homework: String = "", // الواجب والتمارين المنزلية
    val commonMistakesAndTips: String = "", // تنبيهات المعلم والأخطاء الشائعة للطلاب
    val voiceNoteUri: String? = null, // تسجيل صوتي لأفكار المعلم
    val isCompleted: Boolean = false, // هل تم شرح الدرس وتنفيذه
    val isFavorite: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_files")
data class StudyFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String, // e.g. "كتاب الرياضيات - الترم الأول"
    val grade: String = "الصف الأول الثانوي", // الصف الدراسي (e.g. "الصف الثالث الإعدادي")
    val category: String = "كتاب الوزارة", // "كتاب الوزارة", "مذكرة الشرح", "ملخص ومراجعة", "امتحانات سابقة", "أوراق عمل ومتابعة", "أخرى"
    val subject: String = "", // المادة
    val localFilePath: String, // المسار المحلي الداخلي للملف المحفوظ
    val originalFileName: String = "", // اسم الملف الأصلي
    val fileExtension: String = "pdf", // pdf, docx, pptx, png, jpg, etc.
    val fileSizeBytes: Long = 0, // الحجم بالبايت
    val notes: String = "", // ملاحظات المعلم
    val isFavorite: Boolean = false, // مفضلة
    val dateAdded: String = "", // YYYY-MM-DD
    val createdAt: Long = System.currentTimeMillis()
)


