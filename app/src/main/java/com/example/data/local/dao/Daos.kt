package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TeacherDao {
    @Query("SELECT * FROM teachers WHERE id = 1")
    fun getTeacher(): Flow<TeacherEntity?>

    @Query("SELECT * FROM teachers WHERE id = 1")
    suspend fun getTeacherSync(): TeacherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(teacher: TeacherEntity)
}

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY id DESC")
    fun getAllGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups ORDER BY id DESC")
    suspend fun getAllGroupsList(): List<GroupEntity>

    @Query("SELECT * FROM groups WHERE id = :id")
    fun getGroupById(id: Long): Flow<GroupEntity?>

    @Query("SELECT * FROM groups WHERE id = :id")
    suspend fun getGroupByIdSync(id: Long): GroupEntity?

    @Query("SELECT COUNT(*) FROM groups")
    fun getGroupCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: GroupEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(groups: List<GroupEntity>)

    @Update
    suspend fun update(group: GroupEntity)

    @Delete
    suspend fun delete(group: GroupEntity)

    @Query("DELETE FROM groups WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM groups WHERE name LIKE '%' || :query || '%' OR grade LIKE '%' || :query || '%'")
    suspend fun searchGroups(query: String): List<GroupEntity>
}

@Dao
interface StudentDao {
    @Query("SELECT * FROM students ORDER BY name ASC")
    fun getAllStudents(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students ORDER BY name ASC")
    suspend fun getAllStudentsList(): List<StudentEntity>

    @Query("SELECT * FROM students WHERE groupId = :groupId ORDER BY name ASC")
    fun getStudentsByGroup(groupId: Long): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE groupId = :groupId ORDER BY name ASC")
    suspend fun getStudentsByGroupSync(groupId: Long): List<StudentEntity>

    @Query("SELECT * FROM students WHERE id = :id")
    fun getStudentById(id: Long): Flow<StudentEntity?>

    @Query("SELECT * FROM students WHERE id = :id")
    suspend fun getStudentByIdSync(id: Long): StudentEntity?

    @Query("SELECT COUNT(*) FROM students")
    fun getStudentCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM students WHERE groupId = :groupId")
    fun getStudentCountByGroup(groupId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(student: StudentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(students: List<StudentEntity>)

    @Update
    suspend fun update(student: StudentEntity)

    @Delete
    suspend fun delete(student: StudentEntity)

    @Query("DELETE FROM students WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM students WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR parentPhone LIKE '%' || :query || '%'")
    suspend fun searchStudents(query: String): List<StudentEntity>
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY id DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY id DESC")
    suspend fun getAllSessionsList(): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE groupId = :groupId ORDER BY id DESC")
    fun getSessionsByGroup(groupId: Long): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): SessionEntity?

    @Query("SELECT * FROM sessions WHERE date = :date OR day = :day")
    suspend fun getSessionsByDateOrDay(date: String, day: String): List<SessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<SessionEntity>)

    @Update
    suspend fun update(session: SessionEntity)

    @Delete
    suspend fun delete(session: SessionEntity)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance ORDER BY date DESC, id DESC")
    fun getAllAttendance(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance ORDER BY date DESC, id DESC")
    suspend fun getAllAttendanceList(): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE studentId = :studentId ORDER BY date DESC")
    fun getAttendanceByStudent(studentId: Long): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE studentId = :studentId ORDER BY date DESC")
    suspend fun getAttendanceByStudentSync(studentId: Long): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE groupId = :groupId ORDER BY date DESC")
    fun getAttendanceByGroup(groupId: Long): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE groupId = :groupId ORDER BY date DESC")
    suspend fun getAttendanceByGroupSync(groupId: Long): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE groupId = :groupId AND date = :date")
    fun getAttendanceByGroupAndDate(groupId: Long, date: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE groupId = :groupId AND date = :date")
    suspend fun getAttendanceByGroupAndDateSync(groupId: Long, date: String): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE date = :date AND status = 'absent'")
    fun getAbsentsByDate(date: String): Flow<List<AttendanceEntity>>

    @Query("SELECT COUNT(*) FROM attendance WHERE studentId = :studentId AND status = 'present'")
    suspend fun getPresentCountByStudent(studentId: Long): Int

    @Query("SELECT COUNT(*) FROM attendance WHERE studentId = :studentId")
    suspend fun getTotalAttendanceCountByStudent(studentId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attendance: AttendanceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<AttendanceEntity>)

    @Update
    suspend fun update(attendance: AttendanceEntity)

    @Delete
    suspend fun delete(attendance: AttendanceEntity)

    @Query("DELETE FROM attendance WHERE studentId = :studentId AND date = :date")
    suspend fun deleteByStudentAndDate(studentId: Long, date: String)
}

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams ORDER BY date DESC, id DESC")
    fun getAllExams(): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams ORDER BY date DESC, id DESC")
    suspend fun getAllExamsList(): List<ExamEntity>

    @Query("SELECT * FROM exams WHERE groupId = :groupId ORDER BY date DESC")
    fun getExamsByGroup(groupId: Long): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE id = :id")
    fun getExamById(id: Long): Flow<ExamEntity?>

    @Query("SELECT * FROM exams WHERE id = :id")
    suspend fun getExamByIdSync(id: Long): ExamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exam: ExamEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exams: List<ExamEntity>)

    @Update
    suspend fun update(exam: ExamEntity)

    @Delete
    suspend fun delete(exam: ExamEntity)

    @Query("DELETE FROM exams WHERE id = :id")
    suspend fun deleteById(id: Long)

    // Grades
    @Query("SELECT * FROM exam_grades WHERE examId = :examId")
    fun getGradesByExam(examId: Long): Flow<List<ExamGradeEntity>>

    @Query("SELECT * FROM exam_grades WHERE examId = :examId")
    suspend fun getGradesByExamSync(examId: Long): List<ExamGradeEntity>

    @Query("SELECT * FROM exam_grades WHERE studentId = :studentId")
    fun getGradesByStudent(studentId: Long): Flow<List<ExamGradeEntity>>

    @Query("SELECT * FROM exam_grades WHERE studentId = :studentId")
    suspend fun getGradesByStudentSync(studentId: Long): List<ExamGradeEntity>

    @Query("SELECT * FROM exam_grades")
    fun getAllGrades(): Flow<List<ExamGradeEntity>>

    @Query("SELECT * FROM exam_grades")
    suspend fun getAllGradesList(): List<ExamGradeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrade(grade: ExamGradeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllGrades(grades: List<ExamGradeEntity>)

    @Update
    suspend fun updateGrade(grade: ExamGradeEntity)

    @Delete
    suspend fun deleteGrade(grade: ExamGradeEntity)

    @Query("DELETE FROM exam_grades WHERE examId = :examId")
    suspend fun deleteGradesByExam(examId: Long)
}

@Dao
interface FinanceDao {
    // Payments
    @Query("SELECT * FROM payments ORDER BY date DESC, id DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments ORDER BY date DESC, id DESC")
    suspend fun getAllPaymentsList(): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE studentId = :studentId ORDER BY date DESC")
    fun getPaymentsByStudent(studentId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE studentId = :studentId ORDER BY date DESC")
    suspend fun getPaymentsByStudentSync(studentId: Long): List<PaymentEntity>

    @Query("SELECT SUM(amount) FROM payments")
    fun getTotalPayments(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPayments(payments: List<PaymentEntity>)

    @Update
    suspend fun updatePayment(payment: PaymentEntity)

    @Delete
    suspend fun deletePayment(payment: PaymentEntity)

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun deletePaymentById(id: Long)

    // Expenses
    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC")
    suspend fun getAllExpensesList(): List<ExpenseEntity>

    @Query("SELECT SUM(amount) FROM expenses")
    fun getTotalExpenses(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllExpenses(expenses: List<ExpenseEntity>)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, id DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY isPinned DESC, id DESC")
    suspend fun getAllNotesList(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<NoteEntity>)

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface VenueDao {
    @Query("SELECT * FROM venues ORDER BY id DESC")
    fun getAllVenues(): Flow<List<VenueEntity>>

    @Query("SELECT * FROM venues ORDER BY id DESC")
    suspend fun getAllVenuesList(): List<VenueEntity>

    @Query("SELECT * FROM venues WHERE id = :id")
    fun getVenueById(id: Long): Flow<VenueEntity?>

    @Query("SELECT * FROM venues WHERE id = :id")
    suspend fun getVenueByIdSync(id: Long): VenueEntity?

    @Query("SELECT COUNT(*) FROM venues")
    fun getVenueCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(venue: VenueEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(venues: List<VenueEntity>)

    @Update
    suspend fun update(venue: VenueEntity)

    @Delete
    suspend fun delete(venue: VenueEntity)

    @Query("DELETE FROM venues WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM venues WHERE name LIKE '%' || :query || '%' OR address LIKE '%' || :query || '%'")
    suspend fun searchVenues(query: String): List<VenueEntity>
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM certificate_settings WHERE id = 1")
    fun getCertificateSettings(): Flow<CertificateSettingEntity?>

    @Query("SELECT * FROM certificate_settings WHERE id = 1")
    suspend fun getCertificateSettingsSync(): CertificateSettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCertificateSettings(settings: CertificateSettingEntity)

    @Query("SELECT * FROM report_settings WHERE id = 1")
    fun getReportSettings(): Flow<ReportSettingEntity?>

    @Query("SELECT * FROM report_settings WHERE id = 1")
    suspend fun getReportSettingsSync(): ReportSettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateReportSettings(settings: ReportSettingEntity)

    @Query("SELECT value FROM app_settings WHERE `key` = :key")
    suspend fun getAppSetting(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setAppSetting(setting: AppSettingEntity)

    @Query("SELECT * FROM app_settings")
    suspend fun getAllAppSettings(): List<AppSettingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAppSettings(settings: List<AppSettingEntity>)
}

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions ORDER BY id DESC")
    fun getAllQuestions(): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE subject = :subject OR :subject = '' ORDER BY id DESC")
    fun getQuestionsBySubject(subject: String): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE grade = :grade OR :grade = '' ORDER BY id DESC")
    fun getQuestionsByGrade(grade: String): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun getQuestionById(id: Long): QuestionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(question: QuestionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<QuestionEntity>)

    @Update
    suspend fun update(question: QuestionEntity)

    @Delete
    suspend fun delete(question: QuestionEntity)

    @Query("DELETE FROM questions WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface CurriculumDao {
    @Query("SELECT * FROM curriculum ORDER BY orderIndex ASC, id ASC")
    fun getAllCurriculum(): Flow<List<CurriculumEntity>>

    @Query("SELECT * FROM curriculum WHERE grade = :grade OR :grade = '' ORDER BY orderIndex ASC, id ASC")
    fun getCurriculumByGrade(grade: String): Flow<List<CurriculumEntity>>

    @Query("SELECT * FROM curriculum WHERE groupId = :groupId OR groupId = 0 ORDER BY orderIndex ASC, id ASC")
    fun getCurriculumByGroup(groupId: Long): Flow<List<CurriculumEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CurriculumEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CurriculumEntity>)

    @Update
    suspend fun update(item: CurriculumEntity)

    @Delete
    suspend fun delete(item: CurriculumEntity)

    @Query("DELETE FROM curriculum WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface MaterialDeliveryDao {
    @Query("SELECT * FROM material_deliveries ORDER BY id DESC")
    fun getAllDeliveries(): Flow<List<MaterialDeliveryEntity>>

    @Query("SELECT * FROM material_deliveries WHERE studentId = :studentId ORDER BY id DESC")
    fun getDeliveriesByStudent(studentId: Long): Flow<List<MaterialDeliveryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(delivery: MaterialDeliveryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(deliveries: List<MaterialDeliveryEntity>)

    @Update
    suspend fun update(delivery: MaterialDeliveryEntity)

    @Delete
    suspend fun delete(delivery: MaterialDeliveryEntity)

    @Query("DELETE FROM material_deliveries WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface HomeworkDao {
    @Query("SELECT * FROM homework_submissions ORDER BY id DESC")
    fun getAllHomework(): Flow<List<HomeworkSubmissionEntity>>

    @Query("SELECT * FROM homework_submissions WHERE studentId = :studentId ORDER BY id DESC")
    fun getHomeworkByStudent(studentId: Long): Flow<List<HomeworkSubmissionEntity>>

    @Query("SELECT * FROM homework_submissions WHERE groupId = :groupId ORDER BY id DESC")
    fun getHomeworkByGroup(groupId: Long): Flow<List<HomeworkSubmissionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(homework: HomeworkSubmissionEntity): Long

    @Update
    suspend fun update(homework: HomeworkSubmissionEntity)

    @Delete
    suspend fun delete(homework: HomeworkSubmissionEntity)

    @Query("DELETE FROM homework_submissions WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface VoiceNoteDao {
    @Query("SELECT * FROM voice_notes ORDER BY id DESC")
    fun getAllVoiceNotes(): Flow<List<VoiceNoteEntity>>

    @Query("SELECT * FROM voice_notes WHERE groupId = :groupId OR groupId = 0 ORDER BY id DESC")
    fun getVoiceNotesByGroup(groupId: Long): Flow<List<VoiceNoteEntity>>

    @Query("SELECT * FROM voice_notes WHERE studentId = :studentId OR studentId = 0 ORDER BY id DESC")
    fun getVoiceNotesByStudent(studentId: Long): Flow<List<VoiceNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: VoiceNoteEntity): Long

    @Update
    suspend fun update(note: VoiceNoteEntity)

    @Delete
    suspend fun delete(note: VoiceNoteEntity)

    @Query("DELETE FROM voice_notes WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface LessonPlanDao {
    @Query("SELECT * FROM lesson_plans ORDER BY id DESC")
    fun getAllLessonPlans(): Flow<List<LessonPlanEntity>>

    @Query("SELECT * FROM lesson_plans WHERE id = :id")
    fun getLessonPlanById(id: Long): Flow<LessonPlanEntity?>

    @Query("SELECT * FROM lesson_plans WHERE id = :id")
    suspend fun getLessonPlanByIdSync(id: Long): LessonPlanEntity?

    @Query("SELECT * FROM lesson_plans WHERE groupId = :groupId OR groupId = 0 ORDER BY id DESC")
    fun getLessonPlansByGroup(groupId: Long): Flow<List<LessonPlanEntity>>

    @Query("SELECT * FROM lesson_plans WHERE isFavorite = 1 ORDER BY id DESC")
    fun getFavoriteLessonPlans(): Flow<List<LessonPlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: LessonPlanEntity): Long

    @Update
    suspend fun update(plan: LessonPlanEntity)

    @Delete
    suspend fun delete(plan: LessonPlanEntity)

    @Query("DELETE FROM lesson_plans WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface StudyFileDao {
    @Query("SELECT * FROM study_files ORDER BY id DESC")
    fun getAllStudyFiles(): Flow<List<StudyFileEntity>>

    @Query("SELECT * FROM study_files WHERE grade = :grade ORDER BY id DESC")
    fun getStudyFilesByGrade(grade: String): Flow<List<StudyFileEntity>>

    @Query("SELECT * FROM study_files WHERE category = :category ORDER BY id DESC")
    fun getStudyFilesByCategory(category: String): Flow<List<StudyFileEntity>>

    @Query("SELECT * FROM study_files WHERE id = :id")
    fun getStudyFileById(id: Long): Flow<StudyFileEntity?>

    @Query("SELECT * FROM study_files WHERE id = :id")
    suspend fun getStudyFileByIdSync(id: Long): StudyFileEntity?

    @Query("SELECT * FROM study_files WHERE title LIKE '%' || :query || '%' OR grade LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%'")
    fun searchStudyFiles(query: String): Flow<List<StudyFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: StudyFileEntity): Long

    @Update
    suspend fun update(file: StudyFileEntity)

    @Delete
    suspend fun delete(file: StudyFileEntity)

    @Query("DELETE FROM study_files WHERE id = :id")
    suspend fun deleteById(id: Long)
}



