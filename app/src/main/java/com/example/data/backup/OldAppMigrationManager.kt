package com.example.data.backup

import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import org.json.JSONArray
import org.json.JSONObject

data class MigrationPreview(
    val isValidOldFormat: Boolean,
    val sourceDatabaseName: String,
    val studentsCount: Int,
    val groupsCount: Int,
    val sessionsCount: Int,
    val attendanceCount: Int,
    val examsCount: Int,
    val gradesCount: Int,
    val paymentsCount: Int,
    val expensesCount: Int,
    val notesCount: Int,
    val parsedData: OldAppData? = null,
    val errorMessage: String? = null
)

data class MigrationResult(
    val success: Boolean,
    val importedStudents: Int,
    val importedGroups: Int,
    val importedSessions: Int,
    val importedAttendance: Int,
    val importedExams: Int,
    val importedGrades: Int,
    val importedPayments: Int,
    val importedExpenses: Int,
    val importedNotes: Int,
    val errorMessage: String? = null
)

data class OldAppData(
    val teacher: TeacherEntity?,
    val groups: List<GroupEntity>,
    val students: List<StudentEntity>,
    val sessions: List<SessionEntity>,
    val attendance: List<AttendanceEntity>,
    val exams: List<ExamEntity>,
    val grades: List<ExamGradeEntity>,
    val payments: List<PaymentEntity>,
    val expenses: List<ExpenseEntity>,
    val notes: List<NoteEntity>
)

class OldAppMigrationManager(private val db: AppDatabase) {

    fun parseAndPreviewOldBackup(jsonString: String): MigrationPreview {
        return try {
            val root = JSONObject(jsonString)

            // Detect if JSON is wrapped in key "teacher_planner_v18" or direct
            val dataObj = if (root.has("teacher_planner_v18")) {
                val raw = root.get("teacher_planner_v18")
                if (raw is String) JSONObject(raw) else raw as JSONObject
            } else if (root.has("data") && root.getJSONObject("data").has("teacher_planner_v18")) {
                root.getJSONObject("data").getJSONObject("teacher_planner_v18")
            } else {
                root
            }

            // Teacher
            var teacher: TeacherEntity? = null
            val tObj = dataObj.optJSONObject("teacher") ?: dataObj.optJSONObject("profile") ?: dataObj.optJSONObject("teacher_info")
            if (tObj != null) {
                teacher = TeacherEntity(
                    id = 1,
                    name = tObj.optString("name", tObj.optString("teacherName", "المدرس")),
                    subject = tObj.optString("subject", tObj.optString("teacherSubject", "جميع المواد")),
                    phone = tObj.optString("phone", tObj.optString("teacherPhone", "")),
                    whatsapp = tObj.optString("whatsapp", tObj.optString("teacherWhatsapp", "")),
                    centerName = tObj.optString("centerName", tObj.optString("schoolName", "")),
                    address = tObj.optString("address", ""),
                    notes = tObj.optString("notes", "")
                )
            }

            // Groups
            val groupsList = mutableListOf<GroupEntity>()
            val groupsArr = dataObj.optJSONArray("groups") ?: dataObj.optJSONArray("groupList")
            if (groupsArr != null) {
                for (i in 0 until groupsArr.length()) {
                    val g = groupsArr.getJSONObject(i)
                    groupsList.add(
                        GroupEntity(
                            id = g.optLong("id", 0),
                            name = g.optString("name", g.optString("groupName", "مجموعة")),
                            groupNumber = g.optString("groupNumber", g.optString("code", "")),
                            grade = g.optString("grade", g.optString("stage", "")),
                            pricingType = g.optString("pricingType", g.optString("priceType", "monthly")),
                            monthlyPrice = g.optDouble("monthlyPrice", g.optDouble("price", 0.0)),
                            sessionDays = g.optString("sessionDays", g.optString("days", "")),
                            sessionTime = g.optString("sessionTime", g.optString("time", "16:00")),
                            durationMinutes = g.optInt("durationMinutes", g.optInt("duration", 90)),
                            location = g.optString("location", g.optString("place", "")),
                            notes = g.optString("notes", "")
                        )
                    )
                }
            }

            // Students
            val studentsList = mutableListOf<StudentEntity>()
            val studentsArr = dataObj.optJSONArray("students") ?: dataObj.optJSONArray("studentList")
            if (studentsArr != null) {
                for (i in 0 until studentsArr.length()) {
                    val s = studentsArr.getJSONObject(i)
                    studentsList.add(
                        StudentEntity(
                            id = s.optLong("id", 0),
                            name = s.optString("name", s.optString("studentName", "طالب")),
                            groupId = s.optLong("groupId", s.optLong("group_id", 0)),
                            grade = s.optString("grade", s.optString("stage", "")),
                            phone = s.optString("phone", s.optString("studentPhone", "")),
                            parentPhone = s.optString("parentPhone", s.optString("guardianPhone", "")),
                            address = s.optString("address", ""),
                            status = s.optString("status", "active"),
                            notes = s.optString("notes", ""),
                            isExempt = s.optBoolean("isExempt", s.optBoolean("exempt", false)),
                            discountPercent = s.optDouble("discountPercent", s.optDouble("discount", 0.0))
                        )
                    )
                }
            }

            // Sessions
            val sessionsList = mutableListOf<SessionEntity>()
            val sessArr = dataObj.optJSONArray("sessions") ?: dataObj.optJSONArray("classes")
            if (sessArr != null) {
                for (i in 0 until sessArr.length()) {
                    val sess = sessArr.getJSONObject(i)
                    sessionsList.add(
                        SessionEntity(
                            id = sess.optLong("id", 0),
                            groupId = sess.optLong("groupId", sess.optLong("group_id", 0)),
                            day = sess.optString("day", ""),
                            time = sess.optString("time", "16:00"),
                            date = sess.optString("date", ""),
                            durationMinutes = sess.optInt("durationMinutes", 90),
                            location = sess.optString("location", ""),
                            completed = sess.optBoolean("completed", false),
                            note = sess.optString("note", "")
                        )
                    )
                }
            }

            // Attendance
            val attendanceList = mutableListOf<AttendanceEntity>()
            val attArr = dataObj.optJSONArray("attendance") ?: dataObj.optJSONArray("attendanceList")
            if (attArr != null) {
                for (i in 0 until attArr.length()) {
                    val att = attArr.getJSONObject(i)
                    attendanceList.add(
                        AttendanceEntity(
                            id = att.optLong("id", 0),
                            studentId = att.optLong("studentId", att.optLong("student_id", 0)),
                            sessionId = att.optLong("sessionId", att.optLong("session_id", 0)),
                            groupId = att.optLong("groupId", att.optLong("group_id", 0)),
                            date = att.optString("date", ""),
                            status = att.optString("status", "present"),
                            note = att.optString("note", "")
                        )
                    )
                }
            }

            // Exams
            val examsList = mutableListOf<ExamEntity>()
            val examsArr = dataObj.optJSONArray("exams") ?: dataObj.optJSONArray("examList")
            if (examsArr != null) {
                for (i in 0 until examsArr.length()) {
                    val ex = examsArr.getJSONObject(i)
                    examsList.add(
                        ExamEntity(
                            id = ex.optLong("id", 0),
                            groupId = ex.optLong("groupId", ex.optLong("group_id", 0)),
                            title = ex.optString("title", ex.optString("examName", "امتحان")),
                            maxScore = ex.optDouble("maxScore", ex.optDouble("totalScore", 100.0)),
                            passScore = ex.optDouble("passScore", 50.0),
                            date = ex.optString("date", ""),
                            note = ex.optString("note", "")
                        )
                    )
                }
            }

            // Grades
            val gradesList = mutableListOf<ExamGradeEntity>()
            val gradesArr = dataObj.optJSONArray("exam_grades") ?: dataObj.optJSONArray("grades") ?: dataObj.optJSONArray("scores")
            if (gradesArr != null) {
                for (i in 0 until gradesArr.length()) {
                    val gr = gradesArr.getJSONObject(i)
                    gradesList.add(
                        ExamGradeEntity(
                            id = gr.optLong("id", 0),
                            examId = gr.optLong("examId", gr.optLong("exam_id", 0)),
                            studentId = gr.optLong("studentId", gr.optLong("student_id", 0)),
                            score = gr.optDouble("score", gr.optDouble("grade", 0.0)),
                            note = gr.optString("note", "")
                        )
                    )
                }
            }

            // Payments
            val paymentsList = mutableListOf<PaymentEntity>()
            val payArr = dataObj.optJSONArray("payments") ?: dataObj.optJSONArray("paymentList")
            if (payArr != null) {
                for (i in 0 until payArr.length()) {
                    val p = payArr.getJSONObject(i)
                    paymentsList.add(
                        PaymentEntity(
                            id = p.optLong("id", 0),
                            studentId = p.optLong("studentId", p.optLong("student_id", 0)),
                            groupId = p.optLong("groupId", p.optLong("group_id", 0)),
                            amount = p.optDouble("amount", 0.0),
                            date = p.optString("date", ""),
                            type = p.optString("type", "monthly"),
                            monthName = p.optString("monthName", ""),
                            note = p.optString("note", "")
                        )
                    )
                }
            }

            // Expenses
            val expensesList = mutableListOf<ExpenseEntity>()
            val expArr = dataObj.optJSONArray("expenses") ?: dataObj.optJSONArray("expenseList")
            if (expArr != null) {
                for (i in 0 until expArr.length()) {
                    val exp = expArr.getJSONObject(i)
                    expensesList.add(
                        ExpenseEntity(
                            id = exp.optLong("id", 0),
                            title = exp.optString("title", exp.optString("expenseName", "مصروف")),
                            amount = exp.optDouble("amount", 0.0),
                            date = exp.optString("date", ""),
                            category = exp.optString("category", "عام"),
                            note = exp.optString("note", "")
                        )
                    )
                }
            }

            // Notes
            val notesList = mutableListOf<NoteEntity>()
            val notesArr = dataObj.optJSONArray("notes") ?: dataObj.optJSONArray("noteList")
            if (notesArr != null) {
                for (i in 0 until notesArr.length()) {
                    val n = notesArr.getJSONObject(i)
                    notesList.add(
                        NoteEntity(
                            id = n.optLong("id", 0),
                            title = n.optString("title", "ملاحظة"),
                            content = n.optString("content", n.optString("text", "")),
                            targetType = n.optString("targetType", "general"),
                            targetId = n.optLong("targetId", 0),
                            date = n.optString("date", ""),
                            isPinned = n.optBoolean("isPinned", false)
                        )
                    )
                }
            }

            val parsedData = OldAppData(
                teacher = teacher,
                groups = groupsList,
                students = studentsList,
                sessions = sessionsList,
                attendance = attendanceList,
                exams = examsList,
                grades = gradesList,
                payments = paymentsList,
                expenses = expensesList,
                notes = notesList
            )

            MigrationPreview(
                isValidOldFormat = true,
                sourceDatabaseName = "teacher_planner_v18 (LocalStorage HTML)",
                studentsCount = studentsList.size,
                groupsCount = groupsList.size,
                sessionsCount = sessionsList.size,
                attendanceCount = attendanceList.size,
                examsCount = examsList.size,
                gradesCount = gradesList.size,
                paymentsCount = paymentsList.size,
                expensesCount = expensesList.size,
                notesCount = notesList.size,
                parsedData = parsedData
            )
        } catch (e: Exception) {
            MigrationPreview(
                isValidOldFormat = false,
                sourceDatabaseName = "غير معروف",
                studentsCount = 0,
                groupsCount = 0,
                sessionsCount = 0,
                attendanceCount = 0,
                examsCount = 0,
                gradesCount = 0,
                paymentsCount = 0,
                expensesCount = 0,
                notesCount = 0,
                errorMessage = "فشل في قراءة ملف النسخة القديمة: ${e.localizedMessage}"
            )
        }
    }

    suspend fun executeMigration(parsedData: OldAppData): MigrationResult {
        return try {
            if (parsedData.teacher != null) {
                db.teacherDao().insertOrUpdate(parsedData.teacher)
            }

            if (parsedData.groups.isNotEmpty()) {
                db.groupDao().insertAll(parsedData.groups)
            }

            if (parsedData.students.isNotEmpty()) {
                db.studentDao().insertAll(parsedData.students)
            }

            if (parsedData.sessions.isNotEmpty()) {
                db.sessionDao().insertAll(parsedData.sessions)
            }

            if (parsedData.attendance.isNotEmpty()) {
                db.attendanceDao().insertAll(parsedData.attendance)
            }

            if (parsedData.exams.isNotEmpty()) {
                db.examDao().insertAll(parsedData.exams)
            }

            if (parsedData.grades.isNotEmpty()) {
                db.examDao().insertAllGrades(parsedData.grades)
            }

            if (parsedData.payments.isNotEmpty()) {
                db.financeDao().insertAllPayments(parsedData.payments)
            }

            if (parsedData.expenses.isNotEmpty()) {
                db.financeDao().insertAllExpenses(parsedData.expenses)
            }

            if (parsedData.notes.isNotEmpty()) {
                db.noteDao().insertAll(parsedData.notes)
            }

            MigrationResult(
                success = true,
                importedStudents = parsedData.students.size,
                importedGroups = parsedData.groups.size,
                importedSessions = parsedData.sessions.size,
                importedAttendance = parsedData.attendance.size,
                importedExams = parsedData.exams.size,
                importedGrades = parsedData.grades.size,
                importedPayments = parsedData.payments.size,
                importedExpenses = parsedData.expenses.size,
                importedNotes = parsedData.notes.size
            )
        } catch (e: Exception) {
            MigrationResult(
                success = false,
                importedStudents = 0,
                importedGroups = 0,
                importedSessions = 0,
                importedAttendance = 0,
                importedExams = 0,
                importedGrades = 0,
                importedPayments = 0,
                importedExpenses = 0,
                importedNotes = 0,
                errorMessage = "خطأ أثناء تنفيذ الترحيل: ${e.localizedMessage}"
            )
        }
    }
}
