package com.example.data.export

import android.content.Context
import com.example.data.local.AppDatabase
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

class ExcelExporter(private val db: AppDatabase) {

    suspend fun generateComprehensiveCsv(context: Context): File {
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val file = File(exportDir, "TeacherPlannerPro_Data_$dateStr.csv")

        val students = db.studentDao().getAllStudentsList()
        val groups = db.groupDao().getAllGroupsList()
        val sessions = db.sessionDao().getAllSessionsList()
        val attendance = db.attendanceDao().getAllAttendanceList()
        val exams = db.examDao().getAllExamsList()
        val grades = db.examDao().getAllGradesList()
        val payments = db.financeDao().getAllPaymentsList()
        val expenses = db.financeDao().getAllExpensesList()

        val groupMap = groups.associateBy { it.id }
        val studentMap = students.associateBy { it.id }
        val examMap = exams.associateBy { it.id }

        FileOutputStream(file).use { fos ->
            // Write UTF-8 BOM so Microsoft Excel recognizes Arabic correctly
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            val writer = OutputStreamWriter(fos, Charsets.UTF_8)

            writer.append("=== تقرير شامل - هاكر التدريس (The Hacker) ===\n")
            writer.append("تاريخ التصدير:,").append(SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())).append("\n\n")

            // 1. STUDENTS
            writer.append("--- جدول الطلاب (Students) ---\n")
            writer.append("م,الاسم,المجموعة,الصف,الهاتف,هاتف ولي الأمر,العنوان,الحالة,معفي من المصاريف\n")
            students.forEachIndexed { idx, s ->
                val gName = groupMap[s.groupId]?.name ?: "غير محدد"
                val exempt = if (s.isExempt) "نعم" else "لا"
                val statusAr = when (s.status) {
                    "active" -> "نشط"
                    "inactive" -> "غير نشط"
                    "suspended" -> "موقوف"
                    else -> s.status
                }
                writer.append("${idx + 1},\"${escape(s.name)}\",\"${escape(gName)}\",\"${escape(s.grade)}\",\"${s.phone}\",\"${s.parentPhone}\",\"${escape(s.address)}\",\"$statusAr\",\"$exempt\"\n")
            }
            writer.append("\n")

            // 2. GROUPS
            writer.append("--- جدول المجموعات (Groups) ---\n")
            writer.append("م,اسم المجموعة,كود المجموعة,الصف الدراسي,نوع التسعير,السعر,أيام الحصص,التوقيت,المدة (دقيقة),المكان\n")
            groups.forEachIndexed { idx, g ->
                val pricing = if (g.pricingType == "monthly") "شهري" else "بالحصة"
                writer.append("${idx + 1},\"${escape(g.name)}\",\"${g.groupNumber}\",\"${escape(g.grade)}\",\"$pricing\",${g.monthlyPrice},\"${escape(g.sessionDays)}\",\"${g.sessionTime}\",${g.durationMinutes},\"${escape(g.location)}\"\n")
            }
            writer.append("\n")

            // 3. SESSIONS
            writer.append("--- جدول الحصص (Sessions) ---\n")
            writer.append("م,المجموعة,اليوم,التاريخ,الوقت,المدة,المكان,الحالة,ملاحظات\n")
            sessions.forEachIndexed { idx, sess ->
                val gName = groupMap[sess.groupId]?.name ?: "غير محدد"
                val state = if (sess.completed) "مكتملة" else "قادمة"
                writer.append("${idx + 1},\"${escape(gName)}\",\"${sess.day}\",\"${sess.date}\",\"${sess.time}\",${sess.durationMinutes},\"${escape(sess.location)}\",\"$state\",\"${escape(sess.note)}\"\n")
            }
            writer.append("\n")

            // 4. ATTENDANCE
            writer.append("--- سجل الحضور والغياب (Attendance) ---\n")
            writer.append("م,اسم الطالب,المجموعة,التاريخ,الحالة,ملاحظات\n")
            attendance.forEachIndexed { idx, att ->
                val sName = studentMap[att.studentId]?.name ?: "طالب #${att.studentId}"
                val gName = groupMap[att.groupId]?.name ?: "غير محدد"
                val statusAr = when (att.status) {
                    "present" -> "حاضر"
                    "absent" -> "غائب"
                    "late" -> "متأخر"
                    "excused" -> "بعذر"
                    else -> att.status
                }
                writer.append("${idx + 1},\"${escape(sName)}\",\"${escape(gName)}\",\"${att.date}\",\"$statusAr\",\"${escape(att.note)}\"\n")
            }
            writer.append("\n")

            // 5. EXAMS & GRADES
            writer.append("--- جدول الامتحانات والدرجات (Exams & Grades) ---\n")
            writer.append("م,اسم الامتحان,المجموعة,التاريخ,الدرجة العظمى,اسم الطالب,درجة الطالب,النسبة المئوية,التقدير\n")
            grades.forEachIndexed { idx, gr ->
                val ex = examMap[gr.examId]
                val sName = studentMap[gr.studentId]?.name ?: "طالب #${gr.studentId}"
                val gName = if (ex != null) groupMap[ex.groupId]?.name ?: "غير محدد" else "غير محدد"
                val maxScore = ex?.maxScore ?: 100.0
                val pct = if (maxScore > 0) (gr.score / maxScore) * 100 else 0.0
                val gradeTitle = when {
                    pct >= 85 -> "ممتاز"
                    pct >= 75 -> "جيد جداً"
                    pct >= 65 -> "جيد"
                    pct >= 50 -> "مقبول"
                    else -> "يحتاج تحسين"
                }
                writer.append("${idx + 1},\"${escape(ex?.title ?: "امتحان")}\",\"${escape(gName)}\",\"${ex?.date ?: ""}\",$maxScore,\"${escape(sName)}\",${gr.score},${String.format(Locale.US, "%.1f", pct)}%,\"$gradeTitle\"\n")
            }
            writer.append("\n")

            // 6. PAYMENTS
            writer.append("--- جدول المدفوعات (Payments) ---\n")
            writer.append("م,اسم الطالب,المجموعة,المبلغ,التاريخ,نوع الدفع,الشهر/الفترة,ملاحظات\n")
            payments.forEachIndexed { idx, p ->
                val sName = studentMap[p.studentId]?.name ?: "طالب #${p.studentId}"
                val gName = groupMap[p.groupId]?.name ?: "غير محدد"
                val typeAr = when (p.type) {
                    "monthly" -> "اشتراك شهري"
                    "session" -> "حصة"
                    "book" -> "مذكرات وكتب"
                    else -> "أخرى"
                }
                writer.append("${idx + 1},\"${escape(sName)}\",\"${escape(gName)}\",${p.amount},\"${p.date}\",\"$typeAr\",\"${escape(p.monthName)}\",\"${escape(p.note)}\"\n")
            }
            writer.append("\n")

            // 7. EXPENSES
            writer.append("--- جدول المصروفات (Expenses) ---\n")
            writer.append("م,بند المصروف,المبلغ,التاريخ,التصنيف,ملاحظات\n")
            expenses.forEachIndexed { idx, exp ->
                writer.append("${idx + 1},\"${escape(exp.title)}\",${exp.amount},\"${exp.date}\",\"${escape(exp.category)}\",\"${escape(exp.note)}\"\n")
            }
            writer.append("\n")

            // 8. FINANCIAL SUMMARY
            val totalIncome = payments.sumOf { it.amount }
            val totalExpense = expenses.sumOf { it.amount }
            val netProfit = totalIncome - totalExpense
            writer.append("--- الملخص المالي (Financial Summary) ---\n")
            writer.append("إجمالي الإيرادات (المدفوعات),$totalIncome ج.م\n")
            writer.append("إجمالي المصروفات,$totalExpense ج.م\n")
            writer.append("صافي الدخل (الأرباح),$netProfit ج.م\n")

            writer.flush()
        }

        return file
    }

    private fun escape(text: String): String {
        return text.replace("\"", "\"\"").replace("\n", " ")
    }
}
