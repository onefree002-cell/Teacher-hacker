package com.example.util

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

val LocalAppLanguage = compositionLocalOf { AppLanguage.ARABIC }

enum class AppLanguage(val code: String, val displayName: String, val flag: String, val isRtl: Boolean) {
    ARABIC("ar", "العربية", "🇪🇬", true),
    ENGLISH("en", "English", "🇬🇧", false),
    FRENCH("fr", "Français", "🇫🇷", false)
}

object LocaleManager {
    private const val PREFS_NAME = "hacker_locale_prefs"
    private const val KEY_SELECTED_LANGUAGE = "key_selected_language"

    private var prefs: SharedPreferences? = null
    private val _currentLanguage = MutableStateFlow(AppLanguage.ARABIC)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedCode = prefs?.getString(KEY_SELECTED_LANGUAGE, AppLanguage.ARABIC.code) ?: AppLanguage.ARABIC.code
            val lang = AppLanguage.entries.firstOrNull { it.code == savedCode } ?: AppLanguage.ARABIC
            _currentLanguage.value = lang
        }
    }

    fun setLanguage(language: AppLanguage) {
        prefs?.edit()?.putString(KEY_SELECTED_LANGUAGE, language.code)?.apply()
        _currentLanguage.value = language
    }

    fun getLayoutDirection(): LayoutDirection {
        return if (_currentLanguage.value.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    }
}

object L {
    val current: AppLanguage
        @Composable
        get() {
            val lang by LocaleManager.currentLanguage.collectAsState()
            return lang
        }

    fun isArabic(): Boolean = LocaleManager.currentLanguage.value == AppLanguage.ARABIC

    // App Name & Branding
    fun appName(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "هاكر التدريس"
        AppLanguage.ENGLISH -> "The Hacker - Teacher Planner"
        AppLanguage.FRENCH -> "The Hacker - Planificateur Enseignant"
    }

    fun appSubtitle(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "المنظومة الرقمية الشاملة للمعلم الذكي"
        AppLanguage.ENGLISH -> "Smart Digital Teaching Suite"
        AppLanguage.FRENCH -> "Suite Numérique Pédagogique Intelligente"
    }

    // Navigation & Screens
    fun dashboard(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "الرئيسية"
        AppLanguage.ENGLISH -> "Dashboard"
        AppLanguage.FRENCH -> "Accueil"
    }

    fun schedule(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "الجدول"
        AppLanguage.ENGLISH -> "Schedule"
        AppLanguage.FRENCH -> "Planning"
    }

    fun attendance(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "الحضور والغياب"
        AppLanguage.ENGLISH -> "Attendance"
        AppLanguage.FRENCH -> "Présences"
    }

    fun students(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "الطلاب"
        AppLanguage.ENGLISH -> "Students"
        AppLanguage.FRENCH -> "Élèves"
    }

    fun groups(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "المجموعات"
        AppLanguage.ENGLISH -> "Groups"
        AppLanguage.FRENCH -> "Groupes"
    }

    fun exams(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "الامتحانات"
        AppLanguage.ENGLISH -> "Exams"
        AppLanguage.FRENCH -> "Examens"
    }

    fun finance(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "المالية"
        AppLanguage.ENGLISH -> "Finance"
        AppLanguage.FRENCH -> "Finances"
    }

    fun smartPrep(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "التحضير الذكي"
        AppLanguage.ENGLISH -> "Smart Prep"
        AppLanguage.FRENCH -> "Prépa Intelligente"
    }

    fun questionBank(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "بنك الأسئلة"
        AppLanguage.ENGLISH -> "Question Bank"
        AppLanguage.FRENCH -> "Banque Questions"
    }

    fun certificates(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "الشهادات"
        AppLanguage.ENGLISH -> "Certificates"
        AppLanguage.FRENCH -> "Certificats"
    }

    fun reports(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "التقارير"
        AppLanguage.ENGLISH -> "Reports"
        AppLanguage.FRENCH -> "Rapports"
    }

    fun venues(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "السناتر والقاعات"
        AppLanguage.ENGLISH -> "Venues & Centers"
        AppLanguage.FRENCH -> "Salles & Centres"
    }

    fun curriculum(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "المنهج الدراسي"
        AppLanguage.ENGLISH -> "Curriculum"
        AppLanguage.FRENCH -> "Programme"
    }

    fun teacherTools(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "أدوات المعلم"
        AppLanguage.ENGLISH -> "Teacher Tools"
        AppLanguage.FRENCH -> "Outils Enseignant"
    }

    fun poster(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "المميزات"
        AppLanguage.ENGLISH -> "Features"
        AppLanguage.FRENCH -> "Fonctionnalités"
    }

    fun studyFiles(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "كتب ومذكرات"
        AppLanguage.ENGLISH -> "Study Files & Books"
        AppLanguage.FRENCH -> "Manuels & Fichiers"
    }

    fun profile(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "الملف الشخصي"
        AppLanguage.ENGLISH -> "Profile"
        AppLanguage.FRENCH -> "Profil"
    }

    fun backup(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "النسخ وتليجرام"
        AppLanguage.ENGLISH -> "Backup & Sync"
        AppLanguage.FRENCH -> "Sauvegarde & Synchro"
    }

    fun settings(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "الإعدادات"
        AppLanguage.ENGLISH -> "Settings"
        AppLanguage.FRENCH -> "Paramètres"
    }

    fun search(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "البحث الشامل"
        AppLanguage.ENGLISH -> "Search"
        AppLanguage.FRENCH -> "Recherche"
    }

    fun undo(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "تراجع"
        AppLanguage.ENGLISH -> "Undo"
        AppLanguage.FRENCH -> "Annuler"
    }

    fun changeLanguage(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "تغيير لغة التطبيق"
        AppLanguage.ENGLISH -> "Change Language"
        AppLanguage.FRENCH -> "Changer la Langue"
    }

    fun autoBackup(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "النسخ الاحتياطي التلقائي"
        AppLanguage.ENGLISH -> "Auto Backup"
        AppLanguage.FRENCH -> "Sauvegarde Automatique"
    }

    fun disableAutoBackup(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "إلغاء النسخ الاحتياطي التلقائي"
        AppLanguage.ENGLISH -> "Disable Auto Backup"
        AppLanguage.FRENCH -> "Désactiver la Sauvegarde Auto"
    }

    fun save(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "حفظ"
        AppLanguage.ENGLISH -> "Save"
        AppLanguage.FRENCH -> "Enregistrer"
    }

    fun cancel(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "إلغاء"
        AppLanguage.ENGLISH -> "Cancel"
        AppLanguage.FRENCH -> "Annuler"
    }

    fun delete(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "حذف"
        AppLanguage.ENGLISH -> "Delete"
        AppLanguage.FRENCH -> "Supprimer"
    }

    fun edit(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "تعديل"
        AppLanguage.ENGLISH -> "Edit"
        AppLanguage.FRENCH -> "Modifier"
    }

    fun share(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "مشاركة"
        AppLanguage.ENGLISH -> "Share"
        AppLanguage.FRENCH -> "Partager"
    }

    fun print(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "طباعة"
        AppLanguage.ENGLISH -> "Print"
        AppLanguage.FRENCH -> "Imprimer"
    }

    fun exportPdf(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "تصدير PDF"
        AppLanguage.ENGLISH -> "Export PDF"
        AppLanguage.FRENCH -> "Exporter PDF"
    }

    fun totalStudents(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "إجمالي الطلاب"
        AppLanguage.ENGLISH -> "Total Students"
        AppLanguage.FRENCH -> "Total Élèves"
    }

    fun activeGroups(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "المجموعات النشطة"
        AppLanguage.ENGLISH -> "Active Groups"
        AppLanguage.FRENCH -> "Groupes Actifs"
    }

    fun todaysClasses(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "حصص اليوم"
        AppLanguage.ENGLISH -> "Today's Classes"
        AppLanguage.FRENCH -> "Cours d'Aujourd'hui"
    }

    fun monthlyIncome(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "دخل الشهر"
        AppLanguage.ENGLISH -> "Monthly Income"
        AppLanguage.FRENCH -> "Revenu Mensuel"
    }

    fun smartTranslator(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "مترجم المعلم الفوري"
        AppLanguage.ENGLISH -> "Smart Teacher Translator"
        AppLanguage.FRENCH -> "Traducteur Pédagogique Intelligent"
    }

    fun homeworkPdfScanner(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "تصوير واجب الطالب (PDF)"
        AppLanguage.ENGLISH -> "Homework PDF Scanner"
        AppLanguage.FRENCH -> "Scanner Devoirs (PDF)"
    }

    fun quickActions(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "الإجراءات السريعة"
        AppLanguage.ENGLISH -> "Quick Actions"
        AppLanguage.FRENCH -> "Actions Rapides"
    }

    fun nextSession(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "الحصة القادمة"
        AppLanguage.ENGLISH -> "Next Class"
        AppLanguage.FRENCH -> "Prochain Cours"
    }

    fun addStudent(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "إضافة طالب"
        AppLanguage.ENGLISH -> "Add Student"
        AppLanguage.FRENCH -> "Ajouter Élève"
    }

    fun addGroup(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "إضافة مجموعة"
        AppLanguage.ENGLISH -> "Add Group"
        AppLanguage.FRENCH -> "Ajouter Groupe"
    }

    fun recordAttendance(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "تسجيل حضور"
        AppLanguage.ENGLISH -> "Take Attendance"
        AppLanguage.FRENCH -> "Faire l'Appel"
    }

    fun addExam(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "إضافة امتحان"
        AppLanguage.ENGLISH -> "Add Exam"
        AppLanguage.FRENCH -> "Ajouter Examen"
    }

    // Classroom & Whiteboard Tools
    fun tools(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "الأدوات"
        AppLanguage.ENGLISH -> "Tools"
        AppLanguage.FRENCH -> "Outils"
    }

    fun ruler(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "المسطرة"
        AppLanguage.ENGLISH -> "Ruler"
        AppLanguage.FRENCH -> "Règle"
    }

    fun rulerDesc(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "رسم وقياس مستقيمات دقيقة"
        AppLanguage.ENGLISH -> "Draw and measure precise straight lines"
        AppLanguage.FRENCH -> "Tracer et mesurer des lignes droites précises"
    }

    fun compass(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "البرجل الهندسي"
        AppLanguage.ENGLISH -> "Compass"
        AppLanguage.FRENCH -> "Compas"
    }

    fun compassDesc(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "رسم دوائر وأقواس دقيقة"
        AppLanguage.ENGLISH -> "Draw circles and precise arcs"
        AppLanguage.FRENCH -> "Tracer des cercles et des arcs précis"
    }

    fun protractor(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "المنقلة"
        AppLanguage.ENGLISH -> "Protractor"
        AppLanguage.FRENCH -> "Rapporteur"
    }

    fun protractorDesc(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "قياس وتحديد الزوايا 360°"
        AppLanguage.ENGLISH -> "Measure and set 360° angles"
        AppLanguage.FRENCH -> "Mesurer et définir les angles à 360°"
    }

    fun selectTool(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "أداة التحديد والتحريك"
        AppLanguage.ENGLISH -> "Select & Move Tool"
        AppLanguage.FRENCH -> "Outil de Sélection"
    }

    fun selectToolDesc(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "تحديد الأشكال والنصوص وتحريكها"
        AppLanguage.ENGLISH -> "Select and move shapes & texts"
        AppLanguage.FRENCH -> "Sélectionner et déplacer formes et textes"
    }

    fun shapes2D(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "أشكال ثنائية 2D"
        AppLanguage.ENGLISH -> "2D Shapes"
        AppLanguage.FRENCH -> "Formes 2D"
    }

    fun shapes2DDesc(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "مثلث، مستطيل، دائرة، مضلع"
        AppLanguage.ENGLISH -> "Triangle, rectangle, circle, polygon"
        AppLanguage.FRENCH -> "Triangle, rectangle, cercle, polygone"
    }

    fun shapes3D(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "مجسمات ثلاثية 3D"
        AppLanguage.ENGLISH -> "3D Solids"
        AppLanguage.FRENCH -> "Solides 3D"
    }

    fun shapes3DDesc(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "مكعب، أسطوانة، مخروط، هرم"
        AppLanguage.ENGLISH -> "Cube, cylinder, cone, pyramid"
        AppLanguage.FRENCH -> "Cube, cylindre, cône, pyramide"
    }

    fun handPan(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "تحريك وتكبير"
        AppLanguage.ENGLISH -> "Pan & Zoom"
        AppLanguage.FRENCH -> "Déplacer & Zoomer"
    }

    fun smartPen(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "قلم ذكي"
        AppLanguage.ENGLISH -> "Smart Pen"
        AppLanguage.FRENCH -> "Stylet Intelligent"
    }

    fun highlighter(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "تظليل"
        AppLanguage.ENGLISH -> "Highlighter"
        AppLanguage.FRENCH -> "Surligneur"
    }

    fun laserPointer(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "مؤشر ليزر"
        AppLanguage.ENGLISH -> "Laser Pointer"
        AppLanguage.FRENCH -> "Pointeur Laser"
    }

    fun eraser(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "ممحاة"
        AppLanguage.ENGLISH -> "Eraser"
        AppLanguage.FRENCH -> "Gomme"
    }

    fun voiceStudio(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "تسجيل الصوت"
        AppLanguage.ENGLISH -> "Voice Studio"
        AppLanguage.FRENCH -> "Studio Vocal"
    }

    fun homeworkScanner(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "تصوير الواجب"
        AppLanguage.ENGLISH -> "Homework Scanner"
        AppLanguage.FRENCH -> "Scanner de Devoir"
    }

    fun luckyPicker(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "القرعة الذكية"
        AppLanguage.ENGLISH -> "Random Student Picker"
        AppLanguage.FRENCH -> "Tirage au Sort"
    }

    fun classTimer(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "مؤقت الحصة"
        AppLanguage.ENGLISH -> "Class Timer"
        AppLanguage.FRENCH -> "Minuteur de Classe"
    }

    fun gradeCalculator(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "حاسبة الدرجات"
        AppLanguage.ENGLISH -> "Grade Calculator"
        AppLanguage.FRENCH -> "Calculateur de Notes"
    }

    fun bookletTracker(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "سجل المذكرات"
        AppLanguage.ENGLISH -> "Booklet Tracker"
        AppLanguage.FRENCH -> "Gestionnaire de Fascicules"
    }

    fun portfolioCards(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "بورتفوليو المعلم"
        AppLanguage.ENGLISH -> "Teacher Portfolio"
        AppLanguage.FRENCH -> "Portfolio Enseignant"
    }

    fun printHub(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "مركز الطباعة"
        AppLanguage.ENGLISH -> "Print Hub"
        AppLanguage.FRENCH -> "Centre d'Impression"
    }

    fun templates(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "رسائل جاهزة"
        AppLanguage.ENGLISH -> "Ready Messages"
        AppLanguage.FRENCH -> "Modèles de Messages"
    }

    fun darkMode(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "الوضع الليلي (Dark Mode)"
        AppLanguage.ENGLISH -> "Dark Mode"
        AppLanguage.FRENCH -> "Mode Sombre"
    }

    fun lightMode(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "الوضع النهاري"
        AppLanguage.ENGLISH -> "Light Mode"
        AppLanguage.FRENCH -> "Mode Clair"
    }

    fun extractPageAsImage(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "استخراج الصفحة كصورة"
        AppLanguage.ENGLISH -> "Export Page as Image"
        AppLanguage.FRENCH -> "Exporter Page en Image"
    }

    fun convertPdfToImages(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "تحويل الملف كامل لصور"
        AppLanguage.ENGLISH -> "Convert Full PDF to Images"
        AppLanguage.FRENCH -> "Convertir PDF en Images"
    }

    fun imageQuality(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "تحديد جودة الصور"
        AppLanguage.ENGLISH -> "Image Quality"
        AppLanguage.FRENCH -> "Qualité d'Image"
    }

    fun highQuality(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "جودة فائقة (300 DPI)"
        AppLanguage.ENGLISH -> "High Quality (300 DPI)"
        AppLanguage.FRENCH -> "Haute Qualité (300 DPI)"
    }

    fun mediumQuality(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "جودة متوازنة (150 DPI)"
        AppLanguage.ENGLISH -> "Medium Quality (150 DPI)"
        AppLanguage.FRENCH -> "Qualité Moyenne (150 DPI)"
    }

    fun standardQuality(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "جودة خفيفة (96 DPI)"
        AppLanguage.ENGLISH -> "Standard Quality (96 DPI)"
        AppLanguage.FRENCH -> "Qualité Standard (96 DPI)"
    }

    fun shareImages(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "مشاركة الصور"
        AppLanguage.ENGLISH -> "Share Images"
        AppLanguage.FRENCH -> "Partager les Images"
    }

    fun saveToGallery(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "حفظ في الاستوديو"
        AppLanguage.ENGLISH -> "Save to Gallery"
        AppLanguage.FRENCH -> "Enregistrer dans Galerie"
    }

    fun selectGradeStage(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "تحديد الصف الدراسي"
        AppLanguage.ENGLISH -> "Select Grade / Stage"
        AppLanguage.FRENCH -> "Choisir la Classe"
    }

    // Additional Tool & PDF Actions
    fun pen(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "قلم"
        AppLanguage.ENGLISH -> "Pen"
        AppLanguage.FRENCH -> "Stylo"
    }

    fun calligraphyPen(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "خط عربي"
        AppLanguage.ENGLISH -> "Calligraphy Pen"
        AppLanguage.FRENCH -> "Plume Calligraphie"
    }

    fun neonPen(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "نيون مضيء"
        AppLanguage.ENGLISH -> "Neon Glow"
        AppLanguage.FRENCH -> "Néon Brillant"
    }

    fun dashedPen(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "قلم متقطع"
        AppLanguage.ENGLISH -> "Dashed Line"
        AppLanguage.FRENCH -> "Ligne Pointillée"
    }

    fun stickyNote(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "ملاحظة لاصقة"
        AppLanguage.ENGLISH -> "Sticky Note"
        AppLanguage.FRENCH -> "Note Adhésive"
    }

    fun textTool(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "نص"
        AppLanguage.ENGLISH -> "Text"
        AppLanguage.FRENCH -> "Texte"
    }

    fun boardMode(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "نوع السبورة"
        AppLanguage.ENGLISH -> "Board Mode"
        AppLanguage.FRENCH -> "Type de Tableau"
    }

    fun saveCopy(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "حفظ نسخة"
        AppLanguage.ENGLISH -> "Save Copy"
        AppLanguage.FRENCH -> "Sauvegarder Copie"
    }

    fun fullscreen(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "ملء الشاشة"
        AppLanguage.ENGLISH -> "Fullscreen"
        AppLanguage.FRENCH -> "Plein Écran"
    }

    fun exitFullscreen(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "إلغاء ملء الشاشة"
        AppLanguage.ENGLISH -> "Exit Fullscreen"
        AppLanguage.FRENCH -> "Quitter Plein Écran"
    }

    fun clearAll(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "مسح الكل"
        AppLanguage.ENGLISH -> "Clear All"
        AppLanguage.FRENCH -> "Tout Effacer"
    }

    fun redo(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "إعادة"
        AppLanguage.ENGLISH -> "Redo"
        AppLanguage.FRENCH -> "Rétablir"
    }

    fun homework(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "واجب الحصة"
        AppLanguage.ENGLISH -> "Class Homework"
        AppLanguage.FRENCH -> "Devoir du Cours"
    }

    fun page(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "صفحة"
        AppLanguage.ENGLISH -> "Page"
        AppLanguage.FRENCH -> "Page"
    }

    fun zoomIn(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "تكبير"
        AppLanguage.ENGLISH -> "Zoom In"
        AppLanguage.FRENCH -> "Zoom Avant"
    }

    fun zoomOut(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "تصغير"
        AppLanguage.ENGLISH -> "Zoom Out"
        AppLanguage.FRENCH -> "Zoom Arrière"
    }

    fun fitScreen(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "احتواء الصفحة"
        AppLanguage.ENGLISH -> "Fit Screen"
        AppLanguage.FRENCH -> "Ajuster à l'Écran"
    }

    fun previousPage(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "الصفحة السابقة"
        AppLanguage.ENGLISH -> "Previous Page"
        AppLanguage.FRENCH -> "Page Précédente"
    }

    fun nextPage(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "الصفحة التالية"
        AppLanguage.ENGLISH -> "Next Page"
        AppLanguage.FRENCH -> "Page Suivante"
    }

    fun firstPage(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "الصفحة الأولى"
        AppLanguage.ENGLISH -> "First Page"
        AppLanguage.FRENCH -> "Première Page"
    }

    fun lastPage(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "الصفحة الأخيرة"
        AppLanguage.ENGLISH -> "Last Page"
        AppLanguage.FRENCH -> "Dernière Page"
    }

    fun pageThumbnails(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "معاينة الصفحات"
        AppLanguage.ENGLISH -> "Page Thumbnails"
        AppLanguage.FRENCH -> "Aperçu des Pages"
    }

    fun exportAndShare(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "استخراج صور ومشاركة"
        AppLanguage.ENGLISH -> "Export Images & Share"
        AppLanguage.FRENCH -> "Exporter Images & Partager"
    }

    fun format(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "صيغة الصورة"
        AppLanguage.ENGLISH -> "Image Format"
        AppLanguage.FRENCH -> "Format d'Image"
    }

    fun pageCount(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "عدد الصفحات"
        AppLanguage.ENGLISH -> "Page Count"
        AppLanguage.FRENCH -> "Nombre de Pages"
    }

    fun processing(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "جاري المعالجة..."
        AppLanguage.ENGLISH -> "Processing..."
        AppLanguage.FRENCH -> "Traitement en cours..."
    }

    fun completedSuccessfully(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "تمت العملية بنجاح"
        AppLanguage.ENGLISH -> "Completed successfully"
        AppLanguage.FRENCH -> "Terminé avec succès"
    }

    // Scientific Calculator & Advanced Geometric Helpers
    fun scientificCalculator(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "حاسبة كاسيو العلمية (fx-ES)"
        AppLanguage.ENGLISH -> "Casio Scientific Calculator (fx-ES)"
        AppLanguage.FRENCH -> "Calculatrice Scientifique Casio (fx-ES)"
    }

    fun scientificCalculatorDesc(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "حاسبة fx-991ES PLUS بدقة علمية متقدمة"
        AppLanguage.ENGLISH -> "Casio fx-991ES PLUS Advanced Engine"
        AppLanguage.FRENCH -> "Moteur scientifique avancé fx-991ES"
    }

    fun pinShape(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "تثبيت الشكل"
        AppLanguage.ENGLISH -> "Pin Shape"
        AppLanguage.FRENCH -> "Épingler Forme"
    }

    fun moveVertices(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "سحب رؤوس الشكل"
        AppLanguage.ENGLISH -> "Drag Vertices"
        AppLanguage.FRENCH -> "Déplacer Sommets"
    }

    fun freeText(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "نص حر"
        AppLanguage.ENGLISH -> "Free Text"
        AppLanguage.FRENCH -> "Texte Libre"
    }

    fun moveHandle(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "تحريك"
        AppLanguage.ENGLISH -> "Move"
        AppLanguage.FRENCH -> "Déplacer"
    }

    fun drawMode(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "وضع الرسم"
        AppLanguage.ENGLISH -> "Draw Mode"
        AppLanguage.FRENCH -> "Mode Dessin"
    }

    fun moveAndZoom(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "تحريك وتكبير"
        AppLanguage.ENGLISH -> "Move & Zoom"
        AppLanguage.FRENCH -> "Déplacer & Zoomer"
    }

    fun addMovableText(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "إضافة نص حر قابل للتحريك 🔤"
        AppLanguage.ENGLISH -> "Add Movable Text 🔤"
        AppLanguage.FRENCH -> "Ajouter Texte Mobile 🔤"
    }

    fun typeTextHere(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "اكتب النص أو القانون أو العنوان هنا..."
        AppLanguage.ENGLISH -> "Type text, formula or title here..."
        AppLanguage.FRENCH -> "Tapez le texte, la formule ou le titre ici..."
    }

    fun textColor(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "لون النص:"
        AppLanguage.ENGLISH -> "Text Color:"
        AppLanguage.FRENCH -> "Couleur du Texte:"
    }

    fun textBg(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "خلفية النص:"
        AppLanguage.ENGLISH -> "Text Background:"
        AppLanguage.FRENCH -> "Arrière-plan:"
    }

    fun textSize(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "الحجم:"
        AppLanguage.ENGLISH -> "Size:"
        AppLanguage.FRENCH -> "Taille:"
    }

    fun addText(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "إضافة النص"
        AppLanguage.ENGLISH -> "Add Text"
        AppLanguage.FRENCH -> "Ajouter Texte"
    }

    fun shapes2DTitle(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "🔺 الأشكال المستوية (2D) والتحكم في الرؤوس"
        AppLanguage.ENGLISH -> "🔺 2D Shapes & Vertex Manipulation"
        AppLanguage.FRENCH -> "🔺 Formes 2D & Manipulation des Sommets"
    }

    fun fillShapeBg(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "تعبئة خلفية الشكل بلون خفيف"
        AppLanguage.ENGLISH -> "Fill shape background"
        AppLanguage.FRENCH -> "Remplir l'arrière-plan de la forme"
    }

    fun selectShapePrompt(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "اختر الشكل الهندسي:"
        AppLanguage.ENGLISH -> "Select Geometric Shape:"
        AppLanguage.FRENCH -> "Sélectionner la Forme Géométrique:"
    }

    fun shapes3DTitle(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "🧊 المجسمات الهندسية الفراغية (3D)"
        AppLanguage.ENGLISH -> "🧊 3D Solid Geometry"
        AppLanguage.FRENCH -> "🧊 Géométrie Spatiale 3D"
    }

    fun shapes3DPrompt(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "اختر المجسم الهندسي الفراغي لإدراجه فوراً على السبورة مع إمكانية التحريك والتكبير والتصغير:"
        AppLanguage.ENGLISH -> "Select 3D solid to place immediately with move & resize controls:"
        AppLanguage.FRENCH -> "Sélectionnez le solide 3D à insérer avec contrôles de déplacement et zoom:"
    }

    fun insertOnBoard(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "إدراج الناتج على السبورة 📋"
        AppLanguage.ENGLISH -> "Insert Result on Whiteboard 📋"
        AppLanguage.FRENCH -> "Insérer sur le Tableau 📋"
    }

    fun drawFullCircle(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "رسم دائرة كاملة ⭕"
        AppLanguage.ENGLISH -> "Draw Circle ⭕"
        AppLanguage.FRENCH -> "Tracer Cercle ⭕"
    }

    fun drawHalfCircle(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "نصف دائرة ⌒"
        AppLanguage.ENGLISH -> "Half Circle ⌒"
        AppLanguage.FRENCH -> "Demi-Cercle ⌒"
    }

    fun drawQuarterArc(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "قوس ربع دائرة ◜"
        AppLanguage.ENGLISH -> "Quarter Arc ◜"
        AppLanguage.FRENCH -> "Arc Quart ◜"
    }

    fun radiusLabel(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "نصف القطر:"
        AppLanguage.ENGLISH -> "Radius:"
        AppLanguage.FRENCH -> "Rayon:"
    }

    fun drawAngle(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "رسم الزاوية 📐"
        AppLanguage.ENGLISH -> "Draw Angle 📐"
        AppLanguage.FRENCH -> "Tracer l'Angle 📐"
    }

    fun drawTriangle(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "رسم المثلث 📐"
        AppLanguage.ENGLISH -> "Draw Triangle 📐"
        AppLanguage.FRENCH -> "Tracer le Triangle 📐"
    }

    fun rotate(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "تدوير"
        AppLanguage.ENGLISH -> "Rotate"
        AppLanguage.FRENCH -> "Pivoter"
    }

    fun rotateLeft(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "تدوير يسار"
        AppLanguage.ENGLISH -> "Rotate Left"
        AppLanguage.FRENCH -> "Pivoter à Gauche"
    }

    fun rotateRight(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "تدوير يمين"
        AppLanguage.ENGLISH -> "Rotate Right"
        AppLanguage.FRENCH -> "Pivoter à Droite"
    }

    fun close(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "إغلاق"
        AppLanguage.ENGLISH -> "Close"
        AppLanguage.FRENCH -> "Fermer"
    }

    fun duplicate(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "تكرار"
        AppLanguage.ENGLISH -> "Duplicate"
        AppLanguage.FRENCH -> "Dupliquer"
    }

    fun editVertices(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "تعديل الرؤوس"
        AppLanguage.ENGLISH -> "Edit Vertices"
        AppLanguage.FRENCH -> "Modifier Sommets"
    }

    // ==========================================
    // Days of the Week Localization (أيام الأسبوع)
    // ==========================================
    fun allDays(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "الكل"
        AppLanguage.ENGLISH -> "All Days"
        AppLanguage.FRENCH -> "Tous les jours"
    }

    fun saturday(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "السبت"
        AppLanguage.ENGLISH -> "Saturday"
        AppLanguage.FRENCH -> "Samedi"
    }

    fun sunday(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "الأحد"
        AppLanguage.ENGLISH -> "Sunday"
        AppLanguage.FRENCH -> "Dimanche"
    }

    fun monday(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "الإثنين"
        AppLanguage.ENGLISH -> "Monday"
        AppLanguage.FRENCH -> "Lundi"
    }

    fun tuesday(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "الثلاثاء"
        AppLanguage.ENGLISH -> "Tuesday"
        AppLanguage.FRENCH -> "Mardi"
    }

    fun wednesday(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "الأربعاء"
        AppLanguage.ENGLISH -> "Wednesday"
        AppLanguage.FRENCH -> "Mercredi"
    }

    fun thursday(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "الخميس"
        AppLanguage.ENGLISH -> "Thursday"
        AppLanguage.FRENCH -> "Jeudi"
    }

    fun friday(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "الجمعة"
        AppLanguage.ENGLISH -> "Friday"
        AppLanguage.FRENCH -> "Vendredi"
    }

    fun localizedDay(day: String): String {
        return when (day.trim().lowercase()) {
            "all", "الكل", "tous" -> allDays()
            "السبت", "saturday", "samedi", "sat" -> saturday()
            "الأحد", "الاحد", "sunday", "dimanche", "sun" -> sunday()
            "الإثنين", "الاثنين", "monday", "lundi", "mon" -> monday()
            "الثلاثاء", "tuesday", "mardi", "tue" -> tuesday()
            "الأربعاء", "الاربعاء", "wednesday", "mercredi", "wed" -> wednesday()
            "الخميس", "thursday", "jeudi", "thu" -> thursday()
            "الجمعة", "friday", "vendredi", "fri" -> friday()
            else -> day
        }
    }

    // ==========================================
    // Educational Stages Localization (المراحل والصفوف)
    // ==========================================
    fun primaryStage(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "المرحلة الابتدائية (1 - 6)"
        AppLanguage.ENGLISH -> "Primary Stage (Grades 1 - 6)"
        AppLanguage.FRENCH -> "Cycle Primaire (1ère - 6ème)"
    }

    fun preparatoryStage(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "المرحلة الإعدادية (1 - 3)"
        AppLanguage.ENGLISH -> "Preparatory Stage (Grades 7 - 9)"
        AppLanguage.FRENCH -> "Cycle Collège (7ème - 9ème)"
    }

    fun secondaryStage(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "المرحلة الثانوية (1 - 3)"
        AppLanguage.ENGLISH -> "Secondary Stage (Grades 10 - 12)"
        AppLanguage.FRENCH -> "Cycle Lycée (10ème - 12ème)"
    }

    fun otherStage(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "أخرى / عام"
        AppLanguage.ENGLISH -> "Other / General"
        AppLanguage.FRENCH -> "Autre / Général"
    }

    fun localizedGrade(grade: String): String {
        val g = grade.trim()
        val lang = LocaleManager.currentLanguage.value
        if (lang == AppLanguage.ARABIC) return g

        return when {
            g.contains("الأول الابتدائي") || g.contains("الاول الابتدائي") -> if (lang == AppLanguage.ENGLISH) "1st Primary Grade" else "1ère Année Primaire"
            g.contains("الثاني الابتدائي") -> if (lang == AppLanguage.ENGLISH) "2nd Primary Grade" else "2ème Année Primaire"
            g.contains("الثالث الابتدائي") -> if (lang == AppLanguage.ENGLISH) "3rd Primary Grade" else "3ème Année Primaire"
            g.contains("الرابع الابتدائي") -> if (lang == AppLanguage.ENGLISH) "4th Primary Grade" else "4ème Année Primaire"
            g.contains("الخامس الابتدائي") -> if (lang == AppLanguage.ENGLISH) "5th Primary Grade" else "5ème Année Primaire"
            g.contains("السادس الابتدائي") -> if (lang == AppLanguage.ENGLISH) "6th Primary Grade" else "6ème Année Primaire"
            g.contains("الأول الإعدادي") || g.contains("الاول الاعدادي") -> if (lang == AppLanguage.ENGLISH) "1st Prep (Grade 7)" else "1ère Année Collège"
            g.contains("الثاني الإعدادي") || g.contains("الثاني الاعدادي") -> if (lang == AppLanguage.ENGLISH) "2nd Prep (Grade 8)" else "2ème Année Collège"
            g.contains("الثالث الإعدادي") || g.contains("الثالث الاعدادي") -> if (lang == AppLanguage.ENGLISH) "3rd Prep (Grade 9)" else "3ème Année Collège"
            g.contains("الأول الثانوي") || g.contains("الاول الثانوي") -> if (lang == AppLanguage.ENGLISH) "1st Secondary (Grade 10)" else "1ère Année Lycée"
            g.contains("الثاني الثانوي") -> if (lang == AppLanguage.ENGLISH) "2nd Secondary (Grade 11)" else "2ème Année Lycée"
            g.contains("الثالث الثانوي") -> if (lang == AppLanguage.ENGLISH) "3rd Secondary (Grade 12)" else "3ème Année Lycée (Bac)"
            g.contains("رياض أطفال") -> if (lang == AppLanguage.ENGLISH) "Kindergarten (KG)" else "Maternelle (KG)"
            g.contains("تأسيس") -> if (lang == AppLanguage.ENGLISH) "Foundation & Tutoring" else "Fondation & Soutien"
            g.contains("تعليم حر") -> if (lang == AppLanguage.ENGLISH) "Courses / Free Study" else "Cours Libres / Formations"
            g.contains("جامعي") -> if (lang == AppLanguage.ENGLISH) "University / Diploma" else "Universitaire / Diplôme"
            g.contains("الكل") -> if (lang == AppLanguage.ENGLISH) "All Grades" else "Toutes les Classes"
            else -> g
        }
    }

    // ==========================================
    // Guided App Tour Strings (الجولة التعريفية)
    // ==========================================
    fun appGuidedTour(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "جولة تعريفية بالتطبيق 📖"
        AppLanguage.ENGLISH -> "App Guided Tour 📖"
        AppLanguage.FRENCH -> "Visite Guidée de l'Application 📖"
    }

    fun startTour(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "بدء الجولة التعريفية"
        AppLanguage.ENGLISH -> "Start Guided Tour"
        AppLanguage.FRENCH -> "Démarrer la Visite"
    }

    fun skipTour(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "تخطي الجولة ✕"
        AppLanguage.ENGLISH -> "Skip Tour ✕"
        AppLanguage.FRENCH -> "Passer ✕"
    }

    fun next(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "التالي >"
        AppLanguage.ENGLISH -> "Next >"
        AppLanguage.FRENCH -> "Suivant >"
    }

    fun previous(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "< السابق"
        AppLanguage.ENGLISH -> "< Previous"
        AppLanguage.FRENCH -> "< Précédent"
    }

    fun finishTour(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "إنهاء وبدء استخدام التطبيق 🚀"
        AppLanguage.ENGLISH -> "Finish & Start Using App 🚀"
        AppLanguage.FRENCH -> "Terminer & Démarrer 🚀"
    }

    fun stepOf(current: Int, total: Int): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "الخطوة $current من $total"
        AppLanguage.ENGLISH -> "Step $current of $total"
        AppLanguage.FRENCH -> "Étape $current sur $total"
    }

    // ==========================================
    // Attendance Split Tabs (صفحة الحضور المقسمة)
    // ==========================================
    fun tabAttendance(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "📋 تسجيل الحضور والغياب"
        AppLanguage.ENGLISH -> "📋 Attendance"
        AppLanguage.FRENCH -> "📋 Présences"
    }

    fun tabHomeworkCapture(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "📸 تصوير وتوثيق الواجب"
        AppLanguage.ENGLISH -> "📸 Homework Scanner"
        AppLanguage.FRENCH -> "📸 Scanner Devoirs"
    }

    fun tabExtraTools(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "⚙️ أدوات إضافية"
        AppLanguage.ENGLISH -> "⚙️ Extra Tools"
        AppLanguage.FRENCH -> "⚙️ Outils Supplémentaires"
    }

    fun takeCameraPhoto(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "التقاط صورة للواجب بالكاميرا 📷"
        AppLanguage.ENGLISH -> "Take Photo with Camera 📷"
        AppLanguage.FRENCH -> "Prendre Photo avec Caméra 📷"
    }

    fun pickFromGallery(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "اختيار صور من المعرض 🖼️"
        AppLanguage.ENGLISH -> "Pick from Gallery 🖼️"
        AppLanguage.FRENCH -> "Choisir depuis Galerie 🖼️"
    }

    fun generateHomeworkPdf(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "حفظ وتوليد ملف PDF الواجب 📄"
        AppLanguage.ENGLISH -> "Save & Generate Homework PDF 📄"
        AppLanguage.FRENCH -> "Générer PDF du Devoir 📄"
    }

    fun shareOnWhatsApp(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "مشاركة التقرير عبر واتساب 💬"
        AppLanguage.ENGLISH -> "Share Report via WhatsApp 💬"
        AppLanguage.FRENCH -> "Partager sur WhatsApp 💬"
    }

    fun seniorFriendlyHint(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "💡 نصيحة: صُمم هذا النظام بأزرار كبيرة وواضحة لتسهيل الاستخدام لكافة المعلمين."
        AppLanguage.ENGLISH -> "💡 Tip: Designed with large high-contrast buttons for effortless usage."
        AppLanguage.FRENCH -> "💡 Astuce: Conçu avec de grands boutons pour une utilisation sans effort."
    }

    fun casioCalculator(): String = when (LocaleManager.currentLanguage.value) {
        AppLanguage.ARABIC -> "الآلة الحاسبة العلمية Casio"
        AppLanguage.ENGLISH -> "Casio FX Scientific Calculator"
        AppLanguage.FRENCH -> "Calculatrice Scientifique Casio"
    }
}
