package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*

data class StudentWithDetails(
    val student: StudentEntity,
    val group: GroupEntity?,
    val totalPaid: Double,
    val totalRequired: Double,
    val remainingBalance: Double,
    val attendanceRate: Int,
    val averageScore: Double,
    val lastExamScore: String,
    val lastPaymentDate: String
)

data class GroupWithStudentCount(
    val group: GroupEntity,
    val studentCount: Int = 0
)

data class SessionWithGroup(
    val session: SessionEntity,
    val groupName: String,
    val location: String
)

data class GroupWithDetails(
    val group: GroupEntity,
    val studentCount: Int,
    val totalRevenue: Double,
    val nextSession: String
)

data class DashboardMetrics(
    val teacher: TeacherEntity?,
    val nextSession: SessionEntity?,
    val nextSessionGroup: GroupEntity?,
    val studentCount: Int,
    val groupCount: Int,
    val todaySessionsCount: Int,
    val todayAbsentsCount: Int,
    val totalPayments: Double,
    val totalExpenses: Double,
    val netIncome: Double,
    val upcomingExams: List<ExamWithGroup>,
    val alerts: List<String>
)

data class ExamWithGroup(
    val exam: ExamEntity,
    val groupName: String,
    val studentCount: Int = 0,
    val averageScore: Double = 0.0,
    val highestScore: Double = 0.0,
    val lowestScore: Double = 0.0,
    val passedCount: Int = 0,
    val needsImprovementCount: Int = 0
)

data class StudentGradeItem(
    val student: StudentEntity,
    val grade: ExamGradeEntity?,
    val score: Double,
    val maxScore: Double,
    val percentage: Double,
    val gradeTitle: String // ممتاز، جيد جداً، جيد، مقبول، يحتاج تحسين
)

data class LeaderboardItem(
    val student: StudentEntity,
    val groupName: String,
    val rank: Int,
    val averageScore: Double,
    val attendanceRate: Int,
    val totalExamsCount: Int,
    val overallScore: Double // composite performance score 0-100
)

class TeacherPlannerRepository(private val db: AppDatabase) {
    val teacher: Flow<TeacherEntity?> = db.teacherDao().getTeacher()
    val teacherProfile: Flow<TeacherEntity?> get() = teacher
    val allGroups: Flow<List<GroupEntity>> = db.groupDao().getAllGroups()
    val allStudents: Flow<List<StudentEntity>> = db.studentDao().getAllStudents()
    val allSessions: Flow<List<SessionEntity>> = db.sessionDao().getAllSessions()
    val allAttendance: Flow<List<AttendanceEntity>> = db.attendanceDao().getAllAttendance()
    val allExams: Flow<List<ExamEntity>> = db.examDao().getAllExams()
    val allGrades: Flow<List<ExamGradeEntity>> = db.examDao().getAllGrades()
    val allPayments: Flow<List<PaymentEntity>> = db.financeDao().getAllPayments()
    val allExpenses: Flow<List<ExpenseEntity>> = db.financeDao().getAllExpenses()
    val allNotes: Flow<List<NoteEntity>> = db.noteDao().getAllNotes()
    val allVenues: Flow<List<VenueEntity>> = db.venueDao().getAllVenues()
    val allQuestions: Flow<List<QuestionEntity>> = db.questionDao().getAllQuestions()
    val allCurriculum: Flow<List<CurriculumEntity>> = db.curriculumDao().getAllCurriculum()
    val allMaterialDeliveries: Flow<List<MaterialDeliveryEntity>> = db.materialDeliveryDao().getAllDeliveries()
    val allHomework: Flow<List<HomeworkSubmissionEntity>> = db.homeworkDao().getAllHomework()
    val allVoiceNotes: Flow<List<VoiceNoteEntity>> = db.voiceNoteDao().getAllVoiceNotes()
    val allLessonPlans: Flow<List<LessonPlanEntity>> = db.lessonPlanDao().getAllLessonPlans()
    val certificateSettings: Flow<CertificateSettingEntity?> = db.settingsDao().getCertificateSettings()
    val reportSettings: Flow<ReportSettingEntity?> = db.settingsDao().getReportSettings()

    val groupsWithStudentCount: Flow<List<GroupWithStudentCount>> = kotlinx.coroutines.flow.combine(allGroups, allStudents) { groups, students ->
        val studentCountMap = students.groupBy { it.groupId }.mapValues { it.value.size }
        groups.map { group ->
            GroupWithStudentCount(
                group = group,
                studentCount = studentCountMap[group.id] ?: 0
            )
        }
    }

    // Teacher
    suspend fun updateTeacher(teacher: TeacherEntity) = db.teacherDao().insertOrUpdate(teacher)
    suspend fun getTeacherSync(): TeacherEntity? = db.teacherDao().getTeacherSync()

    // Groups
    suspend fun insertGroup(group: GroupEntity): Long = db.groupDao().insert(group)
    suspend fun updateGroup(group: GroupEntity) = db.groupDao().update(group)
    suspend fun deleteGroup(group: GroupEntity) {
        db.groupDao().delete(group)
        com.example.util.UndoManager.recordAction(
            title = "حذف المجموعة: ${group.name}",
            description = "استعادة المجموعة وجميع بياناتها"
        ) {
            db.groupDao().insert(group)
        }
    }
    fun getGroupById(id: Long): Flow<GroupEntity?> = db.groupDao().getGroupById(id)
    suspend fun getGroupByIdSync(id: Long): GroupEntity? = db.groupDao().getGroupByIdSync(id)
    suspend fun transitionGroupToNewTerm(groupId: Long, newTermName: String) {
        val group = db.groupDao().getGroupByIdSync(groupId) ?: return
        val updated = group.copy(currentTerm = newTermName)
        db.groupDao().update(updated)
    }

    // Students
    suspend fun insertStudent(student: StudentEntity): Long = db.studentDao().insert(student)
    suspend fun insertAllStudents(students: List<StudentEntity>) = db.studentDao().insertAll(students)
    suspend fun updateStudent(student: StudentEntity) = db.studentDao().update(student)
    suspend fun deleteStudent(student: StudentEntity) {
        db.studentDao().delete(student)
        com.example.util.UndoManager.recordAction(
            title = "حذف الطالب: ${student.name}",
            description = "استعادة الطالب وبياناته"
        ) {
            db.studentDao().insert(student)
        }
    }
    fun getStudentById(id: Long): Flow<StudentEntity?> = db.studentDao().getStudentById(id)
    suspend fun getStudentByIdSync(id: Long): StudentEntity? = db.studentDao().getStudentByIdSync(id)
    fun getStudentsByGroup(groupId: Long): Flow<List<StudentEntity>> = db.studentDao().getStudentsByGroup(groupId)

    // Venues (أماكن وقاعات الدروس والسناتر)
    suspend fun insertVenue(venue: VenueEntity): Long = db.venueDao().insert(venue)
    suspend fun updateVenue(venue: VenueEntity) = db.venueDao().update(venue)
    suspend fun deleteVenue(venue: VenueEntity) {
        db.venueDao().delete(venue)
        com.example.util.UndoManager.recordAction(
            title = "حذف القاعة: ${venue.name}",
            description = "استعادة القاعة"
        ) {
            db.venueDao().insert(venue)
        }
    }
    fun getVenueById(id: Long): Flow<VenueEntity?> = db.venueDao().getVenueById(id)
    suspend fun getVenueByIdSync(id: Long): VenueEntity? = db.venueDao().getVenueByIdSync(id)

    // Sessions
    suspend fun insertSession(session: SessionEntity): Long = db.sessionDao().insert(session)
    suspend fun updateSession(session: SessionEntity) = db.sessionDao().update(session)
    suspend fun deleteSession(session: SessionEntity) {
        db.sessionDao().delete(session)
        com.example.util.UndoManager.recordAction(
            title = "حذف الحصة (${session.day} ${session.time})",
            description = "استعادة الحصة"
        ) {
            db.sessionDao().insert(session)
        }
    }
    fun getSessionsByGroup(groupId: Long): Flow<List<SessionEntity>> = db.sessionDao().getSessionsByGroup(groupId)

    // Attendance
    suspend fun insertAttendance(attendance: AttendanceEntity): Long = db.attendanceDao().insert(attendance)
    suspend fun insertAllAttendance(list: List<AttendanceEntity>) = db.attendanceDao().insertAll(list)
    suspend fun deleteAttendance(attendance: AttendanceEntity) {
        db.attendanceDao().delete(attendance)
        com.example.util.UndoManager.recordAction(
            title = "حذف سجل حضور",
            description = "استعادة الحضور"
        ) {
            db.attendanceDao().insert(attendance)
        }
    }
    fun getAttendanceByGroup(groupId: Long): Flow<List<AttendanceEntity>> =
        db.attendanceDao().getAttendanceByGroup(groupId)
    fun getAttendanceByGroupAndDate(groupId: Long, date: String): Flow<List<AttendanceEntity>> =
        db.attendanceDao().getAttendanceByGroupAndDate(groupId, date)
    fun getAttendanceByStudent(studentId: Long): Flow<List<AttendanceEntity>> =
        db.attendanceDao().getAttendanceByStudent(studentId)

    // Exams & Grades
    suspend fun insertExam(exam: ExamEntity): Long = db.examDao().insert(exam)
    suspend fun updateExam(exam: ExamEntity) = db.examDao().update(exam)
    suspend fun deleteExam(exam: ExamEntity) {
        val grades = db.examDao().getGradesByExam(exam.id)
        db.examDao().deleteGradesByExam(exam.id)
        db.examDao().delete(exam)
        com.example.util.UndoManager.recordAction(
            title = "حذف الامتحان: ${exam.title}",
            description = "استعادة الامتحان والدرجات"
        ) {
            db.examDao().insert(exam)
        }
    }
    fun getExamById(id: Long): Flow<ExamEntity?> = db.examDao().getExamById(id)
    fun getExamsByGroup(groupId: Long): Flow<List<ExamEntity>> = db.examDao().getExamsByGroup(groupId)
    fun getGradesByExam(examId: Long): Flow<List<ExamGradeEntity>> = db.examDao().getGradesByExam(examId)
    suspend fun insertGrade(grade: ExamGradeEntity) = db.examDao().insertGrade(grade)
    suspend fun insertAllGrades(grades: List<ExamGradeEntity>) = db.examDao().insertAllGrades(grades)

    // Finance
    suspend fun insertPayment(payment: PaymentEntity): Long = db.financeDao().insertPayment(payment)
    suspend fun updatePayment(payment: PaymentEntity) = db.financeDao().updatePayment(payment)
    suspend fun deletePayment(payment: PaymentEntity) {
        db.financeDao().deletePayment(payment)
        com.example.util.UndoManager.recordAction(
            title = "حذف دفعة مالية بقيمة ${payment.amount} ج.م",
            description = "استعادة الدفعة"
        ) {
            db.financeDao().insertPayment(payment)
        }
    }
    fun getPaymentsByStudent(studentId: Long): Flow<List<PaymentEntity>> = db.financeDao().getPaymentsByStudent(studentId)

    suspend fun insertExpense(expense: ExpenseEntity): Long = db.financeDao().insertExpense(expense)
    suspend fun updateExpense(expense: ExpenseEntity) = db.financeDao().updateExpense(expense)
    suspend fun deleteExpense(expense: ExpenseEntity) {
        db.financeDao().deleteExpense(expense)
        com.example.util.UndoManager.recordAction(
            title = "حذف مصروف: ${expense.title} (${expense.amount} ج.م)",
            description = "استعادة المصروف"
        ) {
            db.financeDao().insertExpense(expense)
        }
    }

    // Notes
    suspend fun insertNote(note: NoteEntity): Long = db.noteDao().insert(note)
    suspend fun updateNote(note: NoteEntity) = db.noteDao().update(note)
    suspend fun deleteNote(note: NoteEntity) {
        db.noteDao().delete(note)
        com.example.util.UndoManager.recordAction(
            title = "حذف ملاحظة: ${note.title}",
            description = "استعادة الملاحظة"
        ) {
            db.noteDao().insert(note)
        }
    }

    // Settings
    suspend fun updateCertificateSettings(settings: CertificateSettingEntity) =
        db.settingsDao().insertOrUpdateCertificateSettings(settings)
    suspend fun updateReportSettings(settings: ReportSettingEntity) =
        db.settingsDao().insertOrUpdateReportSettings(settings)

    // Question Bank
    suspend fun insertQuestion(question: QuestionEntity): Long = db.questionDao().insert(question)
    suspend fun updateQuestion(question: QuestionEntity) = db.questionDao().update(question)
    suspend fun deleteQuestion(question: QuestionEntity) = db.questionDao().delete(question)
    fun getQuestionsBySubject(subject: String): Flow<List<QuestionEntity>> = db.questionDao().getQuestionsBySubject(subject)
    fun getQuestionsByGrade(grade: String): Flow<List<QuestionEntity>> = db.questionDao().getQuestionsByGrade(grade)

    // Curriculum & Progress
    suspend fun insertCurriculum(item: CurriculumEntity): Long = db.curriculumDao().insert(item)
    suspend fun updateCurriculum(item: CurriculumEntity) = db.curriculumDao().update(item)
    suspend fun deleteCurriculum(item: CurriculumEntity) = db.curriculumDao().delete(item)
    fun getCurriculumByGrade(grade: String): Flow<List<CurriculumEntity>> = db.curriculumDao().getCurriculumByGrade(grade)
    fun getCurriculumByGroup(groupId: Long): Flow<List<CurriculumEntity>> = db.curriculumDao().getCurriculumByGroup(groupId)

    // Materials / Books Delivery
    suspend fun insertMaterialDelivery(delivery: MaterialDeliveryEntity): Long = db.materialDeliveryDao().insert(delivery)
    suspend fun updateMaterialDelivery(delivery: MaterialDeliveryEntity) = db.materialDeliveryDao().update(delivery)
    suspend fun deleteMaterialDelivery(delivery: MaterialDeliveryEntity) = db.materialDeliveryDao().delete(delivery)
    fun getDeliveriesByStudent(studentId: Long): Flow<List<MaterialDeliveryEntity>> = db.materialDeliveryDao().getDeliveriesByStudent(studentId)

    // Homework & Assignment Submissions
    suspend fun insertHomework(homework: HomeworkSubmissionEntity): Long = db.homeworkDao().insert(homework)
    suspend fun updateHomework(homework: HomeworkSubmissionEntity) = db.homeworkDao().update(homework)
    suspend fun deleteHomework(homework: HomeworkSubmissionEntity) = db.homeworkDao().delete(homework)
    suspend fun deleteHomeworkById(id: Long) = db.homeworkDao().deleteById(id)
    fun getHomeworkByStudent(studentId: Long): Flow<List<HomeworkSubmissionEntity>> = db.homeworkDao().getHomeworkByStudent(studentId)
    fun getHomeworkByGroup(groupId: Long): Flow<List<HomeworkSubmissionEntity>> = db.homeworkDao().getHomeworkByGroup(groupId)

    // Voice Notes & Audio Explanations
    suspend fun insertVoiceNote(note: VoiceNoteEntity): Long = db.voiceNoteDao().insert(note)
    suspend fun updateVoiceNote(note: VoiceNoteEntity) = db.voiceNoteDao().update(note)
    suspend fun deleteVoiceNote(note: VoiceNoteEntity) = db.voiceNoteDao().delete(note)
    suspend fun deleteVoiceNoteById(id: Long) = db.voiceNoteDao().deleteById(id)
    fun getVoiceNotesByGroup(groupId: Long): Flow<List<VoiceNoteEntity>> = db.voiceNoteDao().getVoiceNotesByGroup(groupId)
    fun getVoiceNotesByStudent(studentId: Long): Flow<List<VoiceNoteEntity>> = db.voiceNoteDao().getVoiceNotesByStudent(studentId)

    // Smart Lesson Plans & Mind Maps (التحضير الذكي للدروس وتنظيم الأفكار)
    suspend fun insertLessonPlan(plan: LessonPlanEntity): Long = db.lessonPlanDao().insert(plan)
    suspend fun updateLessonPlan(plan: LessonPlanEntity) = db.lessonPlanDao().update(plan)
    suspend fun saveLessonPlan(plan: LessonPlanEntity): Long {
        return if (plan.id == 0L) {
            db.lessonPlanDao().insert(plan)
        } else {
            db.lessonPlanDao().update(plan)
            plan.id
        }
    }
    suspend fun deleteLessonPlan(plan: LessonPlanEntity) = db.lessonPlanDao().delete(plan)
    suspend fun deleteLessonPlanById(id: Long) = db.lessonPlanDao().deleteById(id)
    fun getLessonPlanById(id: Long): Flow<LessonPlanEntity?> = db.lessonPlanDao().getLessonPlanById(id)
    suspend fun getLessonPlanByIdSync(id: Long): LessonPlanEntity? = db.lessonPlanDao().getLessonPlanByIdSync(id)
    fun getLessonPlansByGroup(groupId: Long): Flow<List<LessonPlanEntity>> = db.lessonPlanDao().getLessonPlansByGroup(groupId)
    fun getFavoriteLessonPlans(): Flow<List<LessonPlanEntity>> = db.lessonPlanDao().getFavoriteLessonPlans()

    // Student Transfer
    suspend fun transferStudent(studentId: Long, newGroupId: Long) {
        val student = db.studentDao().getStudentByIdSync(studentId) ?: return
        db.studentDao().update(student.copy(groupId = newGroupId))
    }

    // Leaderboard
    suspend fun getLeaderboard(targetGroupId: Long = 0): List<LeaderboardItem> {
        val students = if (targetGroupId > 0) {
            db.studentDao().getStudentsByGroupSync(targetGroupId)
        } else {
            db.studentDao().getAllStudentsList()
        }
        val groupsMap = db.groupDao().getAllGroupsList().associateBy { it.id }

        val items = students.map { student ->
            val totalAtt = db.attendanceDao().getTotalAttendanceCountByStudent(student.id)
            val presentAtt = db.attendanceDao().getPresentCountByStudent(student.id)
            val attendanceRate = if (totalAtt > 0) ((presentAtt.toDouble() / totalAtt) * 100).toInt() else 100

            val grades = db.examDao().getGradesByStudentSync(student.id)
            val avgScore = if (grades.isNotEmpty()) grades.map { it.score }.average() else 0.0

            // Composite overall score: 70% exam average + 30% attendance
            val overall = (avgScore * 0.7) + (attendanceRate * 0.3)

            LeaderboardItem(
                student = student,
                groupName = groupsMap[student.groupId]?.name ?: "بدون مجموعة",
                rank = 1,
                averageScore = avgScore,
                attendanceRate = attendanceRate,
                totalExamsCount = grades.size,
                overallScore = overall
            )
        }

        // Sort descending and assign rank
        return items.sortedByDescending { it.overallScore }.mapIndexed { index, item ->
            item.copy(rank = index + 1)
        }
    }

    // Comprehensive Student Details
    suspend fun getStudentDetails(studentId: Long): StudentWithDetails? {
        val student = db.studentDao().getStudentByIdSync(studentId) ?: return null
        val group = if (student.groupId > 0) db.groupDao().getGroupByIdSync(student.groupId) else null
        val payments = db.financeDao().getPaymentsByStudentSync(studentId)
        val totalPaid = payments.sumOf { it.amount }

        val monthlyPrice = group?.monthlyPrice ?: 0.0
        val totalRequired = if (student.isExempt) 0.0 else monthlyPrice * (1.0 - student.discountPercent / 100.0)
        val remainingBalance = (totalRequired - totalPaid).coerceAtLeast(0.0)

        val totalAtt = db.attendanceDao().getTotalAttendanceCountByStudent(studentId)
        val presentAtt = db.attendanceDao().getPresentCountByStudent(studentId)
        val attendanceRate = if (totalAtt > 0) ((presentAtt.toDouble() / totalAtt) * 100).toInt() else 100

        val grades = db.examDao().getGradesByStudentSync(studentId)
        val averageScore = if (grades.isNotEmpty()) grades.map { it.score }.average() else 0.0
        val lastGrade = grades.lastOrNull()
        val lastExamScore = if (lastGrade != null) "${lastGrade.score}" else "لا يوجد"
        val lastPaymentDate = payments.firstOrNull()?.date ?: "لا يوجد"

        return StudentWithDetails(
            student = student,
            group = group,
            totalPaid = totalPaid,
            totalRequired = totalRequired,
            remainingBalance = remainingBalance,
            attendanceRate = attendanceRate,
            averageScore = averageScore,
            lastExamScore = lastExamScore,
            lastPaymentDate = lastPaymentDate
        )
    }

    // Exam analytics
    suspend fun getExamAnalytics(examId: Long): ExamWithGroup? {
        val exam = db.examDao().getExamByIdSync(examId) ?: return null
        val group = db.groupDao().getGroupByIdSync(exam.groupId)
        val grades = db.examDao().getGradesByExamSync(examId)
        val scores = grades.map { it.score }

        val avg = if (scores.isNotEmpty()) scores.average() else 0.0
        val max = if (scores.isNotEmpty()) scores.maxOrNull() ?: 0.0 else 0.0
        val min = if (scores.isNotEmpty()) scores.minOrNull() ?: 0.0 else 0.0
        val passed = grades.count { it.score >= exam.passScore }
        val needsImprovement = grades.count { it.score < exam.passScore }

        return ExamWithGroup(
            exam = exam,
            groupName = group?.name ?: "مجموعة غير محددة",
            studentCount = grades.size,
            averageScore = avg,
            highestScore = max,
            lowestScore = min,
            passedCount = passed,
            needsImprovementCount = needsImprovement
        )
    }

    // Conflict detection for sessions
    suspend fun checkSessionConflict(
        day: String,
        time: String,
        durationMinutes: Int,
        excludeSessionId: Long = 0
    ): String? {
        val sessions = db.sessionDao().getAllSessionsList()
        val parseTime = { t: String ->
            val parts = t.split(":")
            if (parts.size == 2) (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0) else 0
        }
        val newStart = parseTime(time)
        val newEnd = newStart + durationMinutes

        for (s in sessions) {
            if (s.id == excludeSessionId) continue
            if (s.day.trim() == day.trim()) {
                val existingStart = parseTime(s.time)
                val existingEnd = existingStart + s.durationMinutes
                if (newStart < existingEnd && newEnd > existingStart) {
                    val group = db.groupDao().getGroupByIdSync(s.groupId)
                    return "يوجد تعارض مع حصة لمجموعة (${group?.name ?: "مجهولة"}) في نفس التوقيت (${s.time})"
                }
            }
        }
        return null
    }

    // Pre-populate sample data
    suspend fun populateSampleData() {
        // Teacher
        val teacher = TeacherEntity(
            id = 1,
            name = "أحمد محمود الإسكندراني",
            subject = "الرياضيات والفيزياء",
            phone = "01012345678",
            whatsapp = "201012345678",
            centerName = "سنتر الأوائل التعليمي",
            address = "شارع الجمهورية - الدور الثالث"
        )
        db.teacherDao().insertOrUpdate(teacher)

        // Groups
        val g1 = GroupEntity(
            name = "الصف الثالث الثانوي - علمي رياضة",
            groupNumber = "G-301",
            grade = "الثالث الثانوي",
            pricingType = "monthly",
            monthlyPrice = 450.0,
            sessionDays = "السبت, الثلاثاء",
            sessionTime = "16:00",
            durationMinutes = 120,
            location = "القاعة الكبرى - سنتر الأوائل"
        )
        val g2 = GroupEntity(
            name = "الصف الثاني الثانوي - لغات",
            groupNumber = "G-202",
            grade = "الثاني الثانوي",
            pricingType = "monthly",
            monthlyPrice = 380.0,
            sessionDays = "الأحد, الأربعاء",
            sessionTime = "18:00",
            durationMinutes = 90,
            location = "قاعة (أ) - سنتر الأوائل"
        )
        val g3 = GroupEntity(
            name = "الصف الأول الثانوي - عام",
            groupNumber = "G-101",
            grade = "الأول الثانوي",
            pricingType = "per_session",
            monthlyPrice = 60.0,
            sessionDays = "الإثنين, الخميس",
            sessionTime = "15:00",
            durationMinutes = 90,
            location = "قاعة (ب)"
        )
        val g1Id = db.groupDao().insert(g1)
        val g2Id = db.groupDao().insert(g2)
        val g3Id = db.groupDao().insert(g3)

        // Students
        val s1 = StudentEntity(name = "عمر خالد الشريف", groupId = g1Id, grade = "الثالث الثانوي", phone = "01123456789", parentPhone = "01098765432", address = "المعادي، القاهرة", status = "active")
        val s2 = StudentEntity(name = "سارة إبراهيم حسن", groupId = g1Id, grade = "الثالث الثانوي", phone = "01234567890", parentPhone = "01187654321", address = "مدينة نصر، القاهرة", status = "active")
        val s3 = StudentEntity(name = "محمود عادل فهمي", groupId = g1Id, grade = "الثالث الثانوي", phone = "01056789012", parentPhone = "01276543210", address = "التجمع الخامس", status = "active")
        val s4 = StudentEntity(name = "مريم يوسف الشافعي", groupId = g2Id, grade = "الثاني الثانوي", phone = "01189012345", parentPhone = "01065432109", address = "مصر الجديدة", status = "active")
        val s5 = StudentEntity(name = "كريم مصطفى عبد الله", groupId = g2Id, grade = "الثاني الثانوي", phone = "01290123456", parentPhone = "01154321098", address = "شبرا، القاهرة", status = "active")
        val s6 = StudentEntity(name = "نور الدين سامي", groupId = g3Id, grade = "الأول الثانوي", phone = "01011223344", parentPhone = "01243210987", address = "الدقي، الجيزة", status = "active")
        val s7 = StudentEntity(name = "جنى طارق المهدي", groupId = g3Id, grade = "الأول الثانوي", phone = "01122334455", parentPhone = "01032109876", address = "المهندسين، الجيزة", status = "active", isExempt = true)

        val s1Id = db.studentDao().insert(s1)
        val s2Id = db.studentDao().insert(s2)
        val s3Id = db.studentDao().insert(s3)
        val s4Id = db.studentDao().insert(s4)
        val s5Id = db.studentDao().insert(s5)
        val s6Id = db.studentDao().insert(s6)
        val s7Id = db.studentDao().insert(s7)

        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Sessions
        val sess1 = SessionEntity(groupId = g1Id, day = "السبت", time = "16:00", date = todayDate, durationMinutes = 120, location = "القاعة الكبرى", completed = false)
        val sess2 = SessionEntity(groupId = g2Id, day = "الأحد", time = "18:00", date = todayDate, durationMinutes = 90, location = "قاعة (أ)", completed = true)
        val sess3 = SessionEntity(groupId = g3Id, day = "الإثنين", time = "15:00", date = todayDate, durationMinutes = 90, location = "قاعة (ب)", completed = false)
        val sess1Id = db.sessionDao().insert(sess1)
        val sess2Id = db.sessionDao().insert(sess2)
        val sess3Id = db.sessionDao().insert(sess3)

        // Attendance
        db.attendanceDao().insert(AttendanceEntity(studentId = s1Id, sessionId = sess1Id, groupId = g1Id, date = todayDate, status = "present"))
        db.attendanceDao().insert(AttendanceEntity(studentId = s2Id, sessionId = sess1Id, groupId = g1Id, date = todayDate, status = "present"))
        db.attendanceDao().insert(AttendanceEntity(studentId = s3Id, sessionId = sess1Id, groupId = g1Id, date = todayDate, status = "absent", note = "عذر مرضي"))
        db.attendanceDao().insert(AttendanceEntity(studentId = s4Id, sessionId = sess2Id, groupId = g2Id, date = todayDate, status = "present"))
        db.attendanceDao().insert(AttendanceEntity(studentId = s5Id, sessionId = sess2Id, groupId = g2Id, date = todayDate, status = "late", note = "تأخر 15 دقيقة"))

        // Exams & Grades
        val exam1 = ExamEntity(groupId = g1Id, title = "امتحان التفاضل والتكامل الشامل الأول", maxScore = 50.0, passScore = 25.0, date = todayDate)
        val exam2 = ExamEntity(groupId = g2Id, title = "اختبار الفيزياء - قوانين الحركة", maxScore = 30.0, passScore = 15.0, date = todayDate)
        val exam1Id = db.examDao().insert(exam1)
        val exam2Id = db.examDao().insert(exam2)

        db.examDao().insertGrade(ExamGradeEntity(examId = exam1Id, studentId = s1Id, score = 48.5, note = "ممتاز ومتميز"))
        db.examDao().insertGrade(ExamGradeEntity(examId = exam1Id, studentId = s2Id, score = 45.0, note = "أداء رائع"))
        db.examDao().insertGrade(ExamGradeEntity(examId = exam1Id, studentId = s3Id, score = 38.0, note = "جيد جداً"))
        db.examDao().insertGrade(ExamGradeEntity(examId = exam2Id, studentId = s4Id, score = 29.0, note = "الأولى على المجموعة"))
        db.examDao().insertGrade(ExamGradeEntity(examId = exam2Id, studentId = s5Id, score = 22.0, note = "يحتاج تركيز على المسائل"))

        // Payments
        db.financeDao().insertPayment(PaymentEntity(studentId = s1Id, groupId = g1Id, amount = 450.0, date = todayDate, type = "monthly", monthName = "الشهر الحالي", note = "دفعة كاملة كاش"))
        db.financeDao().insertPayment(PaymentEntity(studentId = s2Id, groupId = g1Id, amount = 450.0, date = todayDate, type = "monthly", monthName = "الشهر الحالي", note = "تحويل فودافون كاش"))
        db.financeDao().insertPayment(PaymentEntity(studentId = s4Id, groupId = g2Id, amount = 380.0, date = todayDate, type = "monthly", monthName = "الشهر الحالي", note = "دفعة كاملة"))
        db.financeDao().insertPayment(PaymentEntity(studentId = s6Id, groupId = g3Id, amount = 60.0, date = todayDate, type = "session", note = "حصة أولى"))

        // Expenses
        db.financeDao().insertExpense(ExpenseEntity(title = "إيجار قاعة السنتر للشهر", amount = 1200.0, date = todayDate, category = "سنتر", note = "إيجار شهر كامل"))
        db.financeDao().insertExpense(ExpenseEntity(title = "طباعة ملازم المراجعة النهائية", amount = 480.0, date = todayDate, category = "مطبوعات", note = "100 نسخة"))
        db.financeDao().insertExpense(ExpenseEntity(title = "أقلام سبورة وأدوات توضيحية", amount = 95.0, date = todayDate, category = "أدوات", note = "فايبر كاستل"))

        // Notes
        db.noteDao().insert(NoteEntity(title = "تجهيز امتحان نصف الفصل", content = "مراجعة أسئلة بنك المعرفة وإضافة مسائل التفكير العليا", targetType = "general", isPinned = true, date = todayDate))
        db.noteDao().insert(NoteEntity(title = "متابعة الطالب محمود عادل", content = "تم الاتصال بولي الأمر لتنسيق حصة تعويضية للغياب", targetType = "student", targetId = s3Id, date = todayDate))
    }

    // Study Files
    fun getAllStudyFiles(): Flow<List<StudyFileEntity>> = db.studyFileDao().getAllStudyFiles()
    fun getStudyFilesByGrade(grade: String): Flow<List<StudyFileEntity>> = db.studyFileDao().getStudyFilesByGrade(grade)
    fun getStudyFilesByCategory(category: String): Flow<List<StudyFileEntity>> = db.studyFileDao().getStudyFilesByCategory(category)
    fun getStudyFileById(id: Long): Flow<StudyFileEntity?> = db.studyFileDao().getStudyFileById(id)
    suspend fun getStudyFileByIdSync(id: Long): StudyFileEntity? = db.studyFileDao().getStudyFileByIdSync(id)
    fun searchStudyFiles(query: String): Flow<List<StudyFileEntity>> = db.studyFileDao().searchStudyFiles(query)
    suspend fun insertStudyFile(file: StudyFileEntity): Long = db.studyFileDao().insert(file)
    suspend fun updateStudyFile(file: StudyFileEntity) = db.studyFileDao().update(file)
    suspend fun deleteStudyFile(file: StudyFileEntity) = db.studyFileDao().delete(file)
    suspend fun deleteStudyFileById(id: Long) = db.studyFileDao().deleteById(id)

    // Clear all data
    suspend fun clearAllData() {
        val groups = db.groupDao().getAllGroupsList()
        for (g in groups) db.groupDao().delete(g)
        val students = db.studentDao().getAllStudentsList()
        for (s in students) db.studentDao().delete(s)
        val sessions = db.sessionDao().getAllSessionsList()
        for (sess in sessions) db.sessionDao().delete(sess)
        val attendance = db.attendanceDao().getAllAttendanceList()
        for (a in attendance) db.attendanceDao().delete(a)
        val exams = db.examDao().getAllExamsList()
        for (e in exams) db.examDao().delete(e)
        val payments = db.financeDao().getAllPaymentsList()
        for (p in payments) db.financeDao().deletePayment(p)
        val expenses = db.financeDao().getAllExpensesList()
        for (exp in expenses) db.financeDao().deleteExpense(exp)
        val notes = db.noteDao().getAllNotesList()
        for (n in notes) db.noteDao().delete(n)
    }
}
