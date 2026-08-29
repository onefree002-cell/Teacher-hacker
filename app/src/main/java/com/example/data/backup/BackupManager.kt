package com.example.data.backup

import android.content.Context
import android.net.Uri
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

data class BackupMetadata(
    val appVersion: String,
    val backupDate: String,
    val studentsCount: Int,
    val groupsCount: Int,
    val sessionsCount: Int,
    val attendanceCount: Int,
    val examsCount: Int,
    val gradesCount: Int,
    val paymentsCount: Int,
    val expensesCount: Int,
    val notesCount: Int,
    val isValid: Boolean,
    val errorMessage: String? = null
)

class BackupManager(private val db: AppDatabase) {

    suspend fun createBackupJson(): String {
        val root = JSONObject()
        root.put("app", "TeacherPlannerPro")
        root.put("version", "2.0")
        root.put("backupDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

        // Teacher
        val teacher = db.teacherDao().getTeacherSync()
        if (teacher != null) {
            val tObj = JSONObject()
            tObj.put("id", teacher.id)
            tObj.put("name", teacher.name)
            tObj.put("subject", teacher.subject)
            tObj.put("phone", teacher.phone)
            tObj.put("whatsapp", teacher.whatsapp)
            tObj.put("centerName", teacher.centerName)
            tObj.put("address", teacher.address)
            tObj.put("notes", teacher.notes)
            root.put("teacher", tObj)
        }

        // Groups
        val groupsArray = JSONArray()
        for (g in db.groupDao().getAllGroupsList()) {
            val obj = JSONObject()
            obj.put("id", g.id)
            obj.put("name", g.name)
            obj.put("groupNumber", g.groupNumber)
            obj.put("grade", g.grade)
            obj.put("pricingType", g.pricingType)
            obj.put("monthlyPrice", g.monthlyPrice)
            obj.put("sessionDays", g.sessionDays)
            obj.put("sessionTime", g.sessionTime)
            obj.put("durationMinutes", g.durationMinutes)
            obj.put("location", g.location)
            obj.put("notes", g.notes)
            groupsArray.put(obj)
        }
        root.put("groups", groupsArray)

        // Students
        val studentsArray = JSONArray()
        for (s in db.studentDao().getAllStudentsList()) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("name", s.name)
            obj.put("groupId", s.groupId)
            obj.put("grade", s.grade)
            obj.put("phone", s.phone)
            obj.put("parentPhone", s.parentPhone)
            obj.put("address", s.address)
            obj.put("status", s.status)
            obj.put("notes", s.notes)
            obj.put("isExempt", s.isExempt)
            obj.put("discountPercent", s.discountPercent)
            obj.put("barcodeCode", s.barcodeCode)
            obj.put("tags", s.tags)
            obj.put("gender", s.gender)
            obj.put("photoUri", s.photoUri ?: "")
            obj.put("audioNoteUri", s.audioNoteUri ?: "")
            obj.put("createdAt", s.createdAt)
            studentsArray.put(obj)
        }
        root.put("students", studentsArray)

        // Sessions
        val sessionsArray = JSONArray()
        for (sess in db.sessionDao().getAllSessionsList()) {
            val obj = JSONObject()
            obj.put("id", sess.id)
            obj.put("groupId", sess.groupId)
            obj.put("day", sess.day)
            obj.put("time", sess.time)
            obj.put("date", sess.date)
            obj.put("durationMinutes", sess.durationMinutes)
            obj.put("location", sess.location)
            obj.put("completed", sess.completed)
            obj.put("note", sess.note)
            sessionsArray.put(obj)
        }
        root.put("sessions", sessionsArray)

        // Attendance
        val attendanceArray = JSONArray()
        for (att in db.attendanceDao().getAllAttendanceList()) {
            val obj = JSONObject()
            obj.put("id", att.id)
            obj.put("studentId", att.studentId)
            obj.put("sessionId", att.sessionId)
            obj.put("groupId", att.groupId)
            obj.put("date", att.date)
            obj.put("status", att.status)
            obj.put("note", att.note)
            attendanceArray.put(obj)
        }
        root.put("attendance", attendanceArray)

        // Exams
        val examsArray = JSONArray()
        for (e in db.examDao().getAllExamsList()) {
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("groupId", e.groupId)
            obj.put("title", e.title)
            obj.put("maxScore", e.maxScore)
            obj.put("passScore", e.passScore)
            obj.put("date", e.date)
            obj.put("note", e.note)
            examsArray.put(obj)
        }
        root.put("exams", examsArray)

        // Grades
        val gradesArray = JSONArray()
        for (gr in db.examDao().getAllGradesList()) {
            val obj = JSONObject()
            obj.put("id", gr.id)
            obj.put("examId", gr.examId)
            obj.put("studentId", gr.studentId)
            obj.put("score", gr.score)
            obj.put("note", gr.note)
            gradesArray.put(obj)
        }
        root.put("exam_grades", gradesArray)

        // Payments
        val paymentsArray = JSONArray()
        for (p in db.financeDao().getAllPaymentsList()) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("studentId", p.studentId)
            obj.put("groupId", p.groupId)
            obj.put("amount", p.amount)
            obj.put("date", p.date)
            obj.put("type", p.type)
            obj.put("monthName", p.monthName)
            obj.put("note", p.note)
            paymentsArray.put(obj)
        }
        root.put("payments", paymentsArray)

        // Expenses
        val expensesArray = JSONArray()
        for (exp in db.financeDao().getAllExpensesList()) {
            val obj = JSONObject()
            obj.put("id", exp.id)
            obj.put("title", exp.title)
            obj.put("amount", exp.amount)
            obj.put("date", exp.date)
            obj.put("category", exp.category)
            obj.put("note", exp.note)
            expensesArray.put(obj)
        }
        root.put("expenses", expensesArray)

        // Notes
        val notesArray = JSONArray()
        for (n in db.noteDao().getAllNotesList()) {
            val obj = JSONObject()
            obj.put("id", n.id)
            obj.put("title", n.title)
            obj.put("content", n.content)
            obj.put("targetType", n.targetType)
            obj.put("targetId", n.targetId)
            obj.put("date", n.date)
            obj.put("isPinned", n.isPinned)
            notesArray.put(obj)
        }
        root.put("notes", notesArray)

        return root.toString(2)
    }

    fun inspectBackup(jsonString: String): BackupMetadata {
        return try {
            val root = JSONObject(jsonString)
            val version = root.optString("version", "1.0")
            val date = root.optString("backupDate", "غير معروف")
            val studentsCount = root.optJSONArray("students")?.length() ?: 0
            val groupsCount = root.optJSONArray("groups")?.length() ?: 0
            val sessionsCount = root.optJSONArray("sessions")?.length() ?: 0
            val attendanceCount = root.optJSONArray("attendance")?.length() ?: 0
            val examsCount = root.optJSONArray("exams")?.length() ?: 0
            val gradesCount = root.optJSONArray("exam_grades")?.length() ?: 0
            val paymentsCount = root.optJSONArray("payments")?.length() ?: 0
            val expensesCount = root.optJSONArray("expenses")?.length() ?: 0
            val notesCount = root.optJSONArray("notes")?.length() ?: 0

            BackupMetadata(
                appVersion = version,
                backupDate = date,
                studentsCount = studentsCount,
                groupsCount = groupsCount,
                sessionsCount = sessionsCount,
                attendanceCount = attendanceCount,
                examsCount = examsCount,
                gradesCount = gradesCount,
                paymentsCount = paymentsCount,
                expensesCount = expensesCount,
                notesCount = notesCount,
                isValid = true
            )
        } catch (e: Exception) {
            BackupMetadata(
                appVersion = "غير صالح",
                backupDate = "غير صالح",
                studentsCount = 0,
                groupsCount = 0,
                sessionsCount = 0,
                attendanceCount = 0,
                examsCount = 0,
                gradesCount = 0,
                paymentsCount = 0,
                expensesCount = 0,
                notesCount = 0,
                isValid = false,
                errorMessage = "الملف غير صالح أو تالف: ${e.localizedMessage}"
            )
        }
    }

    suspend fun restoreBackup(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)

            // Teacher
            val teacherObj = root.optJSONObject("teacher")
            if (teacherObj != null) {
                db.teacherDao().insertOrUpdate(
                    TeacherEntity(
                        id = 1,
                        name = teacherObj.optString("name", "المدرس"),
                        subject = teacherObj.optString("subject", "جميع المواد"),
                        phone = teacherObj.optString("phone", ""),
                        whatsapp = teacherObj.optString("whatsapp", ""),
                        centerName = teacherObj.optString("centerName", ""),
                        address = teacherObj.optString("address", ""),
                        notes = teacherObj.optString("notes", "")
                    )
                )
            }

            // Groups
            val groupsArr = root.optJSONArray("groups")
            if (groupsArr != null) {
                val list = mutableListOf<GroupEntity>()
                for (i in 0 until groupsArr.length()) {
                    val obj = groupsArr.getJSONObject(i)
                    list.add(
                        GroupEntity(
                            id = obj.optLong("id", 0),
                            name = obj.getString("name"),
                            groupNumber = obj.optString("groupNumber", ""),
                            grade = obj.optString("grade", ""),
                            pricingType = obj.optString("pricingType", "monthly"),
                            monthlyPrice = obj.optDouble("monthlyPrice", 0.0),
                            sessionDays = obj.optString("sessionDays", ""),
                            sessionTime = obj.optString("sessionTime", "16:00"),
                            durationMinutes = obj.optInt("durationMinutes", 90),
                            location = obj.optString("location", ""),
                            notes = obj.optString("notes", "")
                        )
                    )
                }
                db.groupDao().insertAll(list)
            }

            // Students
            val studentsArr = root.optJSONArray("students")
            if (studentsArr != null) {
                val list = mutableListOf<StudentEntity>()
                for (i in 0 until studentsArr.length()) {
                    val obj = studentsArr.getJSONObject(i)
                    list.add(
                        StudentEntity(
                            id = obj.optLong("id", 0),
                            name = obj.getString("name"),
                            groupId = obj.optLong("groupId", 0),
                            grade = obj.optString("grade", ""),
                            phone = obj.optString("phone", ""),
                            parentPhone = obj.optString("parentPhone", ""),
                            address = obj.optString("address", ""),
                            status = obj.optString("status", "active"),
                            notes = obj.optString("notes", ""),
                            isExempt = obj.optBoolean("isExempt", false),
                            discountPercent = obj.optDouble("discountPercent", 0.0),
                            barcodeCode = obj.optString("barcodeCode", ""),
                            tags = obj.optString("tags", ""),
                            gender = obj.optString("gender", "boy"),
                            photoUri = obj.optString("photoUri", "").ifEmpty { null },
                            audioNoteUri = obj.optString("audioNoteUri", "").ifEmpty { null },
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                db.studentDao().insertAll(list)
            }

            // Sessions
            val sessArr = root.optJSONArray("sessions")
            if (sessArr != null) {
                val list = mutableListOf<SessionEntity>()
                for (i in 0 until sessArr.length()) {
                    val obj = sessArr.getJSONObject(i)
                    list.add(
                        SessionEntity(
                            id = obj.optLong("id", 0),
                            groupId = obj.optLong("groupId", 0),
                            day = obj.optString("day", ""),
                            time = obj.optString("time", "16:00"),
                            date = obj.optString("date", ""),
                            durationMinutes = obj.optInt("durationMinutes", 90),
                            location = obj.optString("location", ""),
                            completed = obj.optBoolean("completed", false),
                            note = obj.optString("note", "")
                        )
                    )
                }
                db.sessionDao().insertAll(list)
            }

            // Attendance
            val attArr = root.optJSONArray("attendance")
            if (attArr != null) {
                val list = mutableListOf<AttendanceEntity>()
                for (i in 0 until attArr.length()) {
                    val obj = attArr.getJSONObject(i)
                    list.add(
                        AttendanceEntity(
                            id = obj.optLong("id", 0),
                            studentId = obj.optLong("studentId", 0),
                            sessionId = obj.optLong("sessionId", 0),
                            groupId = obj.optLong("groupId", 0),
                            date = obj.getString("date"),
                            status = obj.optString("status", "present"),
                            note = obj.optString("note", "")
                        )
                    )
                }
                db.attendanceDao().insertAll(list)
            }

            // Exams
            val examsArr = root.optJSONArray("exams")
            if (examsArr != null) {
                val list = mutableListOf<ExamEntity>()
                for (i in 0 until examsArr.length()) {
                    val obj = examsArr.getJSONObject(i)
                    list.add(
                        ExamEntity(
                            id = obj.optLong("id", 0),
                            groupId = obj.optLong("groupId", 0),
                            title = obj.getString("title"),
                            maxScore = obj.optDouble("maxScore", 100.0),
                            passScore = obj.optDouble("passScore", 50.0),
                            date = obj.getString("date"),
                            note = obj.optString("note", "")
                        )
                    )
                }
                db.examDao().insertAll(list)
            }

            // Grades
            val gradesArr = root.optJSONArray("exam_grades")
            if (gradesArr != null) {
                val list = mutableListOf<ExamGradeEntity>()
                for (i in 0 until gradesArr.length()) {
                    val obj = gradesArr.getJSONObject(i)
                    list.add(
                        ExamGradeEntity(
                            id = obj.optLong("id", 0),
                            examId = obj.optLong("examId", 0),
                            studentId = obj.optLong("studentId", 0),
                            score = obj.optDouble("score", 0.0),
                            note = obj.optString("note", "")
                        )
                    )
                }
                db.examDao().insertAllGrades(list)
            }

            // Payments
            val paymentsArr = root.optJSONArray("payments")
            if (paymentsArr != null) {
                val list = mutableListOf<PaymentEntity>()
                for (i in 0 until paymentsArr.length()) {
                    val obj = paymentsArr.getJSONObject(i)
                    list.add(
                        PaymentEntity(
                            id = obj.optLong("id", 0),
                            studentId = obj.optLong("studentId", 0),
                            groupId = obj.optLong("groupId", 0),
                            amount = obj.getDouble("amount"),
                            date = obj.getString("date"),
                            type = obj.optString("type", "monthly"),
                            monthName = obj.optString("monthName", ""),
                            note = obj.optString("note", "")
                        )
                    )
                }
                db.financeDao().insertAllPayments(list)
            }

            // Expenses
            val expensesArr = root.optJSONArray("expenses")
            if (expensesArr != null) {
                val list = mutableListOf<ExpenseEntity>()
                for (i in 0 until expensesArr.length()) {
                    val obj = expensesArr.getJSONObject(i)
                    list.add(
                        ExpenseEntity(
                            id = obj.optLong("id", 0),
                            title = obj.getString("title"),
                            amount = obj.getDouble("amount"),
                            date = obj.getString("date"),
                            category = obj.optString("category", "عام"),
                            note = obj.optString("note", "")
                        )
                    )
                }
                db.financeDao().insertAllExpenses(list)
            }

            // Notes
            val notesArr = root.optJSONArray("notes")
            if (notesArr != null) {
                val list = mutableListOf<NoteEntity>()
                for (i in 0 until notesArr.length()) {
                    val obj = notesArr.getJSONObject(i)
                    list.add(
                        NoteEntity(
                            id = obj.optLong("id", 0),
                            title = obj.getString("title"),
                            content = obj.optString("content", ""),
                            targetType = obj.optString("targetType", "general"),
                            targetId = obj.optLong("targetId", 0),
                            date = obj.optString("date", ""),
                            isPinned = obj.optBoolean("isPinned", false)
                        )
                    )
                }
                db.noteDao().insertAll(list)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
