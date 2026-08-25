package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TeacherEntity::class,
        GroupEntity::class,
        StudentEntity::class,
        SessionEntity::class,
        AttendanceEntity::class,
        ExamEntity::class,
        ExamGradeEntity::class,
        PaymentEntity::class,
        ExpenseEntity::class,
        NoteEntity::class,
        VenueEntity::class,
        CertificateSettingEntity::class,
        ReportSettingEntity::class,
        AppSettingEntity::class,
        QuestionEntity::class,
        CurriculumEntity::class,
        MaterialDeliveryEntity::class,
        HomeworkSubmissionEntity::class,
        VoiceNoteEntity::class,
        LessonPlanEntity::class,
        StudyFileEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun teacherDao(): TeacherDao
    abstract fun groupDao(): GroupDao
    abstract fun studentDao(): StudentDao
    abstract fun sessionDao(): SessionDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun examDao(): ExamDao
    abstract fun financeDao(): FinanceDao
    abstract fun noteDao(): NoteDao
    abstract fun venueDao(): VenueDao
    abstract fun settingsDao(): SettingsDao
    abstract fun questionDao(): QuestionDao
    abstract fun curriculumDao(): CurriculumDao
    abstract fun materialDeliveryDao(): MaterialDeliveryDao
    abstract fun homeworkDao(): HomeworkDao
    abstract fun voiceNoteDao(): VoiceNoteDao
    abstract fun lessonPlanDao(): LessonPlanDao
    abstract fun studyFileDao(): StudyFileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "teacher_planner_pro.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Initialize default settings and sample data
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getInstance(context)
                                database.teacherDao().insertOrUpdate(
                                    TeacherEntity(
                                        id = 1,
                                        name = "عبده أيمن",
                                        subject = "جميع المواد",
                                        phone = "01206150946",
                                        whatsapp = "01206150946",
                                        centerName = "سنتر التفوق والتميز",
                                        address = "شارع الجمهورية - الدور الثالث"
                                    )
                                )
                                database.settingsDao().insertOrUpdateCertificateSettings(
                                    CertificateSettingEntity()
                                )
                                database.settingsDao().insertOrUpdateReportSettings(
                                    ReportSettingEntity()
                                )
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
