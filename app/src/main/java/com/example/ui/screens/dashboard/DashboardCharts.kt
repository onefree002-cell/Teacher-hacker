package com.example.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

enum class ChartTab {
    SUMMARY,
    ATTENDANCE,
    EXAM_PERFORMANCE,
    STUDENT_DISTRIBUTION
}

data class WeeklyStudentData(
    val dayLabel: String,
    val studentCount: Int,
    val sessionCount: Int
)

data class AttendanceSummaryData(
    val totalRecords: Int,
    val presentCount: Int,
    val absentCount: Int,
    val lateCount: Int,
    val attendanceRate: Float, // 0.0 - 100.0
    val dailyRates: List<Pair<String, Float>> // Day to %
)

data class ExamPerformanceData(
    val totalExams: Int,
    val totalGradesRecorded: Int,
    val averageScorePercent: Float, // 0.0 - 100.0
    val passRatePercent: Float, // 0.0 - 100.0
    val excellentCount: Int, // >= 85%
    val veryGoodCount: Int,  // 75 - 84%
    val goodCount: Int,      // 60 - 74%
    val needsHelpCount: Int  // < 60%
)

@Composable
fun DashboardChartsCard(
    studentData: List<WeeklyStudentData>,
    attendanceData: AttendanceSummaryData,
    examData: ExamPerformanceData,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(ChartTab.SUMMARY) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_charts_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with Title & Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(NavyPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BarChart,
                            contentDescription = null,
                            tint = NavyPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "لوحة التحليلات والرسوم البيانية 📊",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "متابعة أسبوعية للأداء، الحضور والطلاب",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Tab Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ChartChip(
                    title = "ملخص",
                    icon = Icons.Filled.Insights,
                    isSelected = selectedTab == ChartTab.SUMMARY,
                    onClick = { selectedTab = ChartTab.SUMMARY },
                    modifier = Modifier.weight(1f)
                )
                ChartChip(
                    title = "الحضور",
                    icon = Icons.Filled.FactCheck,
                    isSelected = selectedTab == ChartTab.ATTENDANCE,
                    onClick = { selectedTab = ChartTab.ATTENDANCE },
                    modifier = Modifier.weight(1f)
                )
                ChartChip(
                    title = "الامتحانات",
                    icon = Icons.Filled.Grade,
                    isSelected = selectedTab == ChartTab.EXAM_PERFORMANCE,
                    onClick = { selectedTab = ChartTab.EXAM_PERFORMANCE },
                    modifier = Modifier.weight(1f)
                )
                ChartChip(
                    title = "الطلاب",
                    icon = Icons.Filled.People,
                    isSelected = selectedTab == ChartTab.STUDENT_DISTRIBUTION,
                    onClick = { selectedTab = ChartTab.STUDENT_DISTRIBUTION },
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Chart Content based on tab
            Crossfade(targetState = selectedTab, label = "chart_crossfade") { tab ->
                when (tab) {
                    ChartTab.SUMMARY -> SummaryChartSection(attendanceData, examData, studentData)
                    ChartTab.ATTENDANCE -> AttendanceChartSection(attendanceData)
                    ChartTab.EXAM_PERFORMANCE -> ExamPerformanceChartSection(examData)
                    ChartTab.STUDENT_DISTRIBUTION -> StudentDistributionChartSection(studentData)
                }
            }
        }
    }
}

@Composable
private fun ChartChip(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// 1. Overall Summary View (Combined Metrics + Mini Bars)
@Composable
private fun SummaryChartSection(
    attendance: AttendanceSummaryData,
    exam: ExamPerformanceData,
    students: List<WeeklyStudentData>
) {
    val totalStudents = students.sumOf { it.studentCount }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Quick 3 Key Rates
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MiniRateBadge(
                title = "نسبة الحضور",
                percent = attendance.attendanceRate,
                color = EmeraldSuccess,
                modifier = Modifier.weight(1f)
            )
            MiniRateBadge(
                title = "معدل النجاح",
                percent = exam.passRatePercent,
                color = NavyPrimary,
                modifier = Modifier.weight(1f)
            )
            MiniRateBadge(
                title = "متوسط الدرجات",
                percent = exam.averageScorePercent,
                color = AmberGoldDark,
                modifier = Modifier.weight(1f)
            )
        }

        // Weekly Distribution Visual Bar Chart
        Text(
            text = "توزيع الطلاب الأسبوعي حسب الأيام:",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        WeeklyBarChart(
            data = students,
            barColor = NavyPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        )
    }
}

// 2. Attendance Rates & Breakdown Chart
@Composable
private fun AttendanceChartSection(attendance: AttendanceSummaryData) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Progress Gauge
            Box(
                modifier = Modifier.size(110.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularAttendanceGauge(
                    rate = attendance.attendanceRate,
                    modifier = Modifier.fillMaxSize()
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "%.1f%%".format(attendance.attendanceRate),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "حضور عام",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Legend & Counts
            Column(
                modifier = Modifier.weight(1f).padding(start = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AttendanceLegendRow(
                    label = "حاضر",
                    count = attendance.presentCount,
                    color = EmeraldSuccess,
                    percent = if (attendance.totalRecords > 0) (attendance.presentCount * 100f / attendance.totalRecords) else 0f
                )
                AttendanceLegendRow(
                    label = "غائب",
                    count = attendance.absentCount,
                    color = CrimsonError,
                    percent = if (attendance.totalRecords > 0) (attendance.absentCount * 100f / attendance.totalRecords) else 0f
                )
                AttendanceLegendRow(
                    label = "متأخر",
                    count = attendance.lateCount,
                    color = AmberGold,
                    percent = if (attendance.totalRecords > 0) (attendance.lateCount * 100f / attendance.totalRecords) else 0f
                )
            }
        }

        // Daily Attendance Mini Bars
        if (attendance.dailyRates.isNotEmpty()) {
            Text(
                text = "نسبة الحضور اليومية خلال الأسبوع:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            DailyAttendanceTrendChart(
                dailyRates = attendance.dailyRates,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            )
        }
    }
}

// 3. Exam Performance Chart Section
@Composable
private fun ExamPerformanceChartSection(exam: ExamPerformanceData) {
    val total = if (exam.totalGradesRecorded > 0) exam.totalGradesRecorded.toFloat() else 1f

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MiniRateBadge(
                title = "متوسط الدرجات",
                percent = exam.averageScorePercent,
                color = IndigoExam,
                modifier = Modifier.weight(1f)
            )
            MiniRateBadge(
                title = "نسبة النجاح",
                percent = exam.passRatePercent,
                color = EmeraldSuccess,
                modifier = Modifier.weight(1f)
            )
            MiniRateBadge(
                title = "إجمالي الاختبارات",
                percent = exam.totalExams.toFloat(),
                suffix = " اختبار",
                color = AmberGoldDark,
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = "توزيع مستويات الطلاب في الامتحانات:",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PerformanceLevelBar(
                title = "ممتاز (85% فأكثر)",
                count = exam.excellentCount,
                percent = (exam.excellentCount * 100f / total),
                color = EmeraldSuccess
            )
            PerformanceLevelBar(
                title = "جيد جداً (75% - 84%)",
                count = exam.veryGoodCount,
                percent = (exam.veryGoodCount * 100f / total),
                color = NavyPrimary
            )
            PerformanceLevelBar(
                title = "جيد (60% - 74%)",
                count = exam.goodCount,
                percent = (exam.goodCount * 100f / total),
                color = AmberGold
            )
            PerformanceLevelBar(
                title = "يحتاج متابعة (أقل من 60%)",
                count = exam.needsHelpCount,
                percent = (exam.needsHelpCount * 100f / total),
                color = CrimsonError
            )
        }
    }
}

// 4. Student Distribution Chart Section
@Composable
private fun StudentDistributionChartSection(studentData: List<WeeklyStudentData>) {
    val totalStudents = studentData.sumOf { it.studentCount }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "إجمالي حضور الطلاب المسجلين أسبوعياً: $totalStudents",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        WeeklyBarChart(
            data = studentData,
            barColor = Color(0xFF8B5CF6),
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        )
    }
}

// --- Canvas Chart Drawing Components ---

@Composable
private fun WeeklyBarChart(
    data: List<WeeklyStudentData>,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    val maxCount = (data.maxOfOrNull { it.studentCount } ?: 1).coerceAtLeast(1)

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val width = size.width
            val height = size.height
            val barCount = data.size
            if (barCount == 0) return@Canvas

            val barSpacing = width / (barCount * 1.6f)
            val barWidth = barSpacing * 0.8f
            val totalSpan = barCount * barSpacing
            val startX = (width - totalSpan) / 2f + (barSpacing - barWidth) / 2f

            // Baseline
            drawLine(
                color = Color.LightGray.copy(alpha = 0.4f),
                start = Offset(0f, height),
                end = Offset(width, height),
                strokeWidth = 2f
            )

            data.forEachIndexed { index, item ->
                val barHeight = (item.studentCount.toFloat() / maxCount.toFloat()) * (height - 20f)
                val x = startX + index * barSpacing
                val y = height - barHeight

                // Draw Bar
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(barColor, barColor.copy(alpha = 0.6f)),
                        startY = y,
                        endY = height
                    ),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight.coerceAtLeast(4f)),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }
        }

        // Labels underneath bars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            data.forEach { item ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = item.dayLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${item.studentCount}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = barColor
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyAttendanceTrendChart(
    dailyRates: List<Pair<String, Float>>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val width = size.width
            val height = size.height
            if (dailyRates.isEmpty()) return@Canvas

            val stepX = width / (dailyRates.size - 1).coerceAtLeast(1)
            val points = dailyRates.mapIndexed { index, pair ->
                val x = index * stepX
                val y = height - (pair.second / 100f) * (height - 15f)
                Offset(x, y.coerceIn(5f, height - 5f))
            }

            // Draw Area
            val areaPath = Path().apply {
                moveTo(points.first().x, height)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, height)
                close()
            }
            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(EmeraldSuccess.copy(alpha = 0.35f), EmeraldSuccess.copy(alpha = 0.05f))
                )
            )

            // Draw Line
            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }
            drawPath(
                path = linePath,
                color = EmeraldSuccess,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw Points
            points.forEach { pt ->
                drawCircle(
                    color = Color.White,
                    radius = 5.dp.toPx(),
                    center = pt
                )
                drawCircle(
                    color = EmeraldSuccess,
                    radius = 3.5.dp.toPx(),
                    center = pt
                )
            }
        }

        // Daily Labels
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dailyRates.forEach { (day, rate) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "%.0f%%".format(rate),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = EmeraldSuccess
                    )
                }
            }
        }
    }
}

@Composable
private fun CircularAttendanceGauge(
    rate: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val stroke = 10.dp.toPx()
        val diameter = size.minDimension - stroke
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)

        // Background Track
        drawArc(
            color = Color.LightGray.copy(alpha = 0.25f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )

        // Filled Gauge
        val sweep = (rate.coerceIn(0f, 100f) / 100f) * 360f
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(EmeraldSuccess, TealAccent, EmeraldSuccess)
            ),
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun MiniRateBadge(
    title: String,
    percent: Float,
    suffix: String = "%",
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (suffix == "%") "%.1f%s".format(percent, suffix) else "%.0f%s".format(percent, suffix),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AttendanceLegendRow(
    label: String,
    count: Int,
    color: Color,
    percent: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = "$count (%.0f%%)".format(percent),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

@Composable
private fun PerformanceLevelBar(
    title: String,
    count: Int,
    percent: Float,
    color: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$count طالب (%.0f%%)".format(percent),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { (percent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
    }
}
