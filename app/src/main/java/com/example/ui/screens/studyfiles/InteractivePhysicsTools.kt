package com.example.ui.screens.studyfiles

import java.util.Locale
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NavyPrimary
import com.example.util.L
import kotlin.math.*

// ============================================================================
// 1. ULTRA-SMOOTH REALISTIC RULER (المسطرة المدرسية الشفافة الدقيقة)
// ============================================================================
@Composable
fun RealisticRuler(
    offset: Offset,
    angle: Float,
    lengthCm: Float = 15f,
    onOffsetChange: (Offset) -> Unit,
    onAngleChange: (Float) -> Unit,
    onDrawLine: (Offset, Offset) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOffset by rememberUpdatedState(offset)
    val currentAngle by rememberUpdatedState(angle)
    var selectedLengthCm by remember { mutableStateOf(lengthCm) }
    var drawEndCm by remember { mutableStateOf(10f) }
    var liveEdgeDrawingActive by remember { mutableStateOf(false) }
    var liveEdgeCm by remember { mutableStateOf(0f) }

    val pxPerCm = 36f
    val rulerWidthDp = (selectedLengthCm * 24f) + 120f
    var isDrawingMode by remember { mutableStateOf(false) }

    // Direct, ultra-fluid container positioning with 1:1 natural movement
    Box(
        modifier = modifier
            .absoluteOffset {
                IntOffset(
                    currentOffset.x.roundToInt().coerceIn(-200, 3000),
                    currentOffset.y.roundToInt().coerceIn(-100, 4000)
                )
            }
            .rotate(currentAngle)
    ) {
        // Classic School Yellow-Tinted Crystal Acrylic Ruler Design
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xEEFEF08A), // Classic translucent yellow stationery acrylic
            border = BorderStroke(1.5.dp, if (isDrawingMode) Color(0xFF2563EB) else Color(0xFFCA8A04)),
            shadowElevation = 14.dp,
            modifier = Modifier
                .width(rulerWidthDp.dp)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Header & Physical Drag Bar (Smooth 1:1 Screen Translation)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFEF9C3), Color(0xFFFEF08A), Color(0xFFFEF9C3))
                            )
                        )
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onOffsetChange(currentOffset + dragAmount)
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.DragIndicator,
                            contentDescription = if (L.isArabic()) "سحب المسطرة" else "Drag Ruler",
                            tint = Color(0xFF854D0E),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (L.isArabic()) "📐 مسطرة ${selectedLengthCm.toInt()} سم" else "📐 Ruler ${selectedLengthCm.toInt()} cm",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF713F12),
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFCA8A04).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFFCA8A04).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "${currentAngle.toInt()}°",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF854D0E),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Mode Switch & Rotation Actions
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Drawing Mode vs Move Mode Toggle
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDrawingMode) Color(0xFF2563EB) else Color.White,
                            border = BorderStroke(1.dp, if (isDrawingMode) Color(0xFF2563EB) else Color(0xFFCA8A04)),
                            modifier = Modifier.clickable { isDrawingMode = !isDrawingMode }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (isDrawingMode) Icons.Filled.Edit else Icons.Filled.OpenWith,
                                    contentDescription = null,
                                    tint = if (isDrawingMode) Color.White else Color(0xFF713F12),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isDrawingMode) (if (L.isArabic()) "وضع الرسم" else "Draw Mode") else (if (L.isArabic()) "تحريك" else "Move"),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDrawingMode) Color.White else Color(0xFF713F12)
                                )
                            }
                        }

                        // Rotate Left & Right
                        IconButton(
                            onClick = { onAngleChange((currentAngle - 15f + 360) % 360) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Filled.RotateLeft, contentDescription = if (L.isArabic()) "تدوير يسار" else "Rotate Left", tint = Color(0xFF854D0E), modifier = Modifier.size(15.dp))
                        }
                        IconButton(
                            onClick = { onAngleChange((currentAngle + 15f) % 360) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Filled.RotateRight, contentDescription = if (L.isArabic()) "تدوير يمين" else "Rotate Right", tint = Color(0xFF854D0E), modifier = Modifier.size(15.dp))
                        }

                        // Close
                        IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = if (L.isArabic()) "إغلاق" else "Close", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Photorealistic Millimeter & Centimeter Scale Graduation Edge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0x66FFFFFF),
                    border = BorderStroke(1.dp, if (isDrawingMode) Color(0xFF2563EB) else Color(0x99CA8A04)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .pointerInput(selectedLengthCm, currentAngle, currentOffset, isDrawingMode) {
                            if (isDrawingMode) {
                                detectDragGestures(
                                    onDragStart = { localOffset ->
                                        liveEdgeDrawingActive = true
                                        val totalMm = (selectedLengthCm * 10).toInt()
                                        val mmStep = (size.width - 28f) / totalMm
                                        val clickedMm = ((localOffset.x - 14f) / mmStep).coerceIn(0f, totalMm.toFloat())
                                        liveEdgeCm = (clickedMm / 10f)
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val totalMm = (selectedLengthCm * 10).toInt()
                                        val mmStep = (size.width - 28f) / totalMm
                                        val currentMm = ((change.position.x - 14f) / mmStep).coerceIn(0f, totalMm.toFloat())
                                        liveEdgeCm = (currentMm / 10f)
                                        drawEndCm = liveEdgeCm
                                    },
                                    onDragEnd = {
                                        liveEdgeDrawingActive = false
                                        if (liveEdgeCm > 0.5f) {
                                            val rad = Math.toRadians(currentAngle.toDouble())
                                            val startX = currentOffset.x + 20f * cos(rad).toFloat()
                                            val startY = currentOffset.y + 20f * sin(rad).toFloat()
                                            val lineLengthPx = liveEdgeCm * pxPerCm
                                            val endX = startX + lineLengthPx * cos(rad).toFloat()
                                            val endY = startY + lineLengthPx * sin(rad).toFloat()
                                            onDrawLine(Offset(startX, startY), Offset(endX, endY))
                                        }
                                    }
                                )
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val totalMm = (selectedLengthCm * 10).toInt()
                        val mmStep = (size.width - 28f) / totalMm
                        val nativeCanvas = drawContext.canvas.nativeCanvas

                        // Beveled Top Edge Highlight
                        drawLine(
                            color = Color.White,
                            start = Offset(0f, 1f),
                            end = Offset(size.width, 1f),
                            strokeWidth = 2.5f
                        )

                        // Central Clear Slot Aesthetic
                        drawRoundRect(
                            color = Color(0x33CA8A04),
                            topLeft = Offset(24f, size.height - 12f),
                            size = Size(size.width - 48f, 6f),
                            cornerRadius = CornerRadius(3f, 3f)
                        )

                        // If user is dragging along edge, show glowing live guide line
                        if (liveEdgeDrawingActive && liveEdgeCm > 0f) {
                            val activeEndX = 14f + (liveEdgeCm * 10f) * mmStep
                            drawLine(
                                color = Color(0xFF2563EB),
                                start = Offset(14f, 2f),
                                end = Offset(activeEndX, 2f),
                                strokeWidth = 5f
                            )
                        }

                        val textPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.rgb(15, 23, 42)
                            textSize = 21f
                            isFakeBoldText = true
                            textAlign = android.graphics.Paint.Align.CENTER
                        }

                        val redTextPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.rgb(220, 38, 38)
                            textSize = 21f
                            isFakeBoldText = true
                            textAlign = android.graphics.Paint.Align.CENTER
                        }

                        for (mm in 0..totalMm) {
                            val x = 14f + mm * mmStep
                            val isCm = mm % 10 == 0
                            val isHalfCm = mm % 5 == 0 && !isCm
                            val tickH = if (isCm) 30f else if (isHalfCm) 20f else 12f
                            val tickColor = if (isCm) Color(0xFF0F172A) else if (isHalfCm) Color(0xFFB45309) else Color(0xFF78350F)
                            val stroke = if (isCm) 2.5f else if (isHalfCm) 1.5f else 1f

                            drawLine(
                                color = tickColor,
                                start = Offset(x, 0f),
                                end = Offset(x, tickH),
                                strokeWidth = stroke
                            )

                            if (isCm) {
                                val cmVal = mm / 10
                                val p = if (cmVal == 0 || cmVal == selectedLengthCm.toInt()) redTextPaint else textPaint
                                nativeCanvas.drawText("$cmVal", x, 46f, p)
                            }
                        }
                    }
                }

                // Ruler Action Bar (Precision Line Slider + One-Tap Straight Line Button + Length Presets)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Length selector
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(10f, 15f, 20f, 30f).forEach { l ->
                            FilterChip(
                                selected = selectedLengthCm == l,
                                onClick = {
                                    selectedLengthCm = l
                                    drawEndCm = min(drawEndCm, l)
                                },
                                label = { Text(if (L.isArabic()) "${l.toInt()} سم" else "${l.toInt()} cm", fontSize = 9.sp) },
                                modifier = Modifier.height(26.dp)
                            )
                        }
                    }

                    // Draw Line Controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = EmeraldSuccess.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = if (L.isArabic()) "${String.format(Locale.ENGLISH, "%.1f", drawEndCm)} سم" else "${String.format(Locale.ENGLISH, "%.1f", drawEndCm)} cm",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Button(
                            onClick = {
                                val rad = Math.toRadians(currentAngle.toDouble())
                                val startX = currentOffset.x + 20f * cos(rad).toFloat()
                                val startY = currentOffset.y + 20f * sin(rad).toFloat()
                                val lineLengthPx = drawEndCm * pxPerCm
                                val endX = startX + lineLengthPx * cos(rad).toFloat()
                                val endY = startY + lineLengthPx * sin(rad).toFloat()
                                onDrawLine(Offset(startX, startY), Offset(endX, endY))
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (L.isArabic()) "رسم خط مستقيم ✏️" else "Draw Line ✏️", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 2. REALISTIC INTERACTIVE PROTRACTOR (المنقلة الهندسية الشفافة المدرسية)
// ============================================================================
@Composable
fun RealisticProtractor(
    center: Offset,
    baseAngle: Float = 0f,
    targetAngle: Float = 60f,
    onCenterChange: (Offset) -> Unit,
    onBaseAngleChange: (Float) -> Unit,
    onTargetAngleChange: (Float) -> Unit,
    onDrawAngle: (Offset, Float, Float) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentCenter by rememberUpdatedState(center)
    val currentBaseAngle by rememberUpdatedState(baseAngle)
    val currentTargetAngle by rememberUpdatedState(targetAngle)

    val protractorWidth = 330.dp

    Box(
        modifier = modifier
            .absoluteOffset {
                IntOffset(
                    (currentCenter.x - 165f).roundToInt().coerceIn(-100, 3000),
                    (currentCenter.y - 115f).roundToInt().coerceIn(-100, 4000)
                )
            }
            .rotate(currentBaseAngle)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xF8F0F9FF), // Translucent crystal acrylic
            border = BorderStroke(1.5.dp, Color(0xFF0284C7)),
            shadowElevation = 12.dp,
            modifier = Modifier
                .width(protractorWidth)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Direct Smooth Drag Header with 1:1 movement
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFE0F2FE), Color(0xFFBAE6FD), Color(0xFFE0F2FE))
                            )
                        )
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onCenterChange(currentCenter + dragAmount)
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.DragIndicator, contentDescription = null, tint = Color(0xFF0369A1), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (L.isArabic()) "🧭 المنقلة: ${currentTargetAngle.toInt()}°" else "🧭 Protractor: ${currentTargetAngle.toInt()}°",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF0369A1),
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val angleType = if (L.isArabic()) {
                            when {
                                currentTargetAngle == 90f -> "قائمة 📐"
                                currentTargetAngle < 90f -> "حادة 🔻"
                                currentTargetAngle == 180f -> "مستقيمة ─"
                                else -> "منفرجة ◺"
                            }
                        } else {
                            when {
                                currentTargetAngle == 90f -> "Right 📐"
                                currentTargetAngle < 90f -> "Acute 🔻"
                                currentTargetAngle == 180f -> "Straight ─"
                                else -> "Obtuse ◺"
                            }
                        }
                        Text(
                            text = "($angleType)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF0284C7),
                            fontSize = 9.sp
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onBaseAngleChange((currentBaseAngle - 15f + 360) % 360) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.RotateLeft, contentDescription = if (L.isArabic()) "تدوير لليسار" else "Rotate Left", tint = Color(0xFF0369A1), modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { onBaseAngleChange((currentBaseAngle + 15f) % 360) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.RotateRight, contentDescription = if (L.isArabic()) "تدوير لليمين" else "Rotate Right", tint = Color(0xFF0369A1), modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { onCenterChange(Offset(280f, 420f)) },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(Icons.Filled.FilterCenterFocus, contentDescription = if (L.isArabic()) "إعادة للوسط" else "Center", tint = Color(0xFF0369A1), modifier = Modifier.size(14.dp))
                        }
                        IconButton(onClick = onClose, modifier = Modifier.size(22.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = if (L.isArabic()) "إغلاق" else "Close", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // Semicircle Authentic Graduation Canvas with Dual Scale (0-180 & 180-0)
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .pointerInput(Unit) {
                            detectTapGestures { tapOffset ->
                                val c = Offset(size.width / 2f, size.height - 12f)
                                val dx = tapOffset.x - c.x
                                val dy = -(tapOffset.y - c.y)
                                if (dy >= -10f) {
                                    var deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                    if (deg < 0) deg += 360f
                                    val protractorAngle = (180f - deg).coerceIn(0f, 180f)
                                    onTargetAngleChange(protractorAngle.roundToInt().toFloat())
                                }
                            }
                        }
                ) {
                    val c = Offset(size.width / 2f, size.height - 12f)
                    val r = size.width / 2f - 16f

                    // Translucent body
                    drawArc(
                        color = Color(0x220284C7),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(c.x - r, c.y - r),
                        size = Size(r * 2, r * 2)
                    )

                    // Outer border
                    drawArc(
                        color = Color(0xFF0284C7),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(c.x - r, c.y - r),
                        size = Size(r * 2, r * 2),
                        style = Stroke(width = 2.5f)
                    )

                    // Baseline
                    drawLine(
                        color = Color(0xFF0284C7),
                        start = Offset(c.x - r, c.y),
                        end = Offset(c.x + r, c.y),
                        strokeWidth = 2f
                    )

                    // Center point crosshair
                    drawCircle(color = Color(0xFFDC2626), radius = 4f, center = c)
                    drawLine(color = Color(0xFFDC2626), start = Offset(c.x - 8f, c.y), end = Offset(c.x + 8f, c.y), strokeWidth = 1.5f)
                    drawLine(color = Color(0xFFDC2626), start = Offset(c.x, c.y - 8f), end = Offset(c.x, c.y + 8f), strokeWidth = 1.5f)

                    val targetRad = Math.toRadians((180 - currentTargetAngle).toDouble())
                    val targetX = c.x + (r * cos(targetRad)).toFloat()
                    val targetY = c.y - (r * sin(targetRad)).toFloat()

                    // Active Angle Ray Indicator
                    drawLine(
                        color = Color(0xFFDC2626),
                        start = c,
                        end = Offset(targetX, targetY),
                        strokeWidth = 3f
                    )

                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.rgb(3, 105, 161)
                        textSize = 18f
                        isFakeBoldText = true
                        textAlign = android.graphics.Paint.Align.CENTER
                    }

                    val nativeCanvas = drawContext.canvas.nativeCanvas

                    for (deg in 0..180 step 5) {
                        val rad = Math.toRadians((180 - deg).toDouble())
                        val cosA = cos(rad).toFloat()
                        val sinA = sin(rad).toFloat()

                        val isMajor = deg % 30 == 0
                        val isMid = deg % 10 == 0 && !isMajor
                        val isRight = deg == 90
                        val tickLen = if (isRight) 26f else if (isMajor) 20f else if (isMid) 14f else 8f
                        val tickColor = if (isRight) Color(0xFFDC2626) else if (isMajor) Color(0xFF0369A1) else Color(0xFF64748B)

                        drawLine(
                            color = tickColor,
                            start = Offset(c.x + r * cosA, c.y - r * sinA),
                            end = Offset(c.x + (r - tickLen) * cosA, c.y - (r - tickLen) * sinA),
                            strokeWidth = if (isMajor || isRight) 2f else 1f
                        )

                        if (deg % 30 == 0) {
                            val textR = r - 32f
                            val tx = c.x + textR * cosA
                            val ty = c.y - textR * sinA + 6f
                            nativeCanvas.drawText("$deg", tx, ty, textPaint)
                        }
                    }
                }

                // Preset Angles & Action Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(30f, 45f, 60f, 90f, 120f, 180f).forEach { ang ->
                            FilterChip(
                                selected = currentTargetAngle.toInt() == ang.toInt(),
                                onClick = { onTargetAngleChange(ang                    Button(
                        onClick = { onDrawAngle(currentCenter, currentBaseAngle, currentTargetAngle) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(L.drawAngle(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ============================================================================
// 3. PHOTOREALISTIC NATURAL SCHOOL COMPASS (البرجل المدرسي المعدني الأصلي)
// ============================================================================
@Composable
fun CompassToolView(
    center: Offset,
    radiusPx: Float,
    onCenterChange: (Offset) -> Unit,
    onRadiusChange: (Float) -> Unit,
    onDrawCircleOrArc: (Offset, Float, Float) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentCenter by rememberUpdatedState(center)
    val currentRadiusPx by rememberUpdatedState(radiusPx)
    val cm = currentRadiusPx / 36f

    Box(
        modifier = modifier
            .absoluteOffset {
                IntOffset(
                    (currentCenter.x - currentRadiusPx - 40f).roundToInt().coerceIn(-100, 3000),
                    (currentCenter.y - currentRadiusPx - 70f).roundToInt().coerceIn(-100, 4000)
                )
            }
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFDFCFCFD),
            border = BorderStroke(1.5.dp, Color(0xFF059669)),
            shadowElevation = 14.dp,
            modifier = Modifier.wrapContentSize()
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Grip (Metallic Drafting Compass)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFD1FAE5), Color(0xFFA7F3D0), Color(0xFFD1FAE5))
                            )
                        )
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onCenterChange(currentCenter + dragAmount)
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.DragIndicator, contentDescription = null, tint = Color(0xFF047857), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (com.example.util.LocaleManager.currentLanguage.value) {
                                com.example.util.AppLanguage.ARABIC -> "🧭 البرجل الهندسي: نق = ${String.format(Locale.ENGLISH, "%.1f", cm)} سم"
                                com.example.util.AppLanguage.FRENCH -> "🧭 Compas d'École: R = ${String.format(Locale.ENGLISH, "%.1f", cm)} cm"
                                com.example.util.AppLanguage.ENGLISH -> "🧭 School Compass: R = ${String.format(Locale.ENGLISH, "%.1f", cm)} cm"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF065F46),
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(22.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = L.close(), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }

                // Photorealistic Canvas Compass: Left Leg is the Moving Pencil, Right Leg is the Needle
                Canvas(
                    modifier = Modifier
                        .size(width = 280.dp, height = 150.dp)
                        .pointerInput(currentRadiusPx) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                // Dragging on left expands/contracts the radius naturally
                                val newRadius = (currentRadiusPx - dragAmount.x).coerceIn(40f, 320f)
                                onRadiusChange(newRadius)
                            }
                        }
                ) {
                    val canvasCenter = Offset(size.width / 2f, 15f)
                    // Left leg: Moving Pencil (matches radius controller on the left)
                    val pencilTip = Offset(canvasCenter.x - currentRadiusPx * 0.45f, size.height - 20f)
                    // Right leg: Steel Needle pivot point
                    val needleTip = Offset(canvasCenter.x + currentRadiusPx * 0.45f, size.height - 20f)

                    // 1. Top Pivot Grip Handle (Knurled Chrome/Brass)
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFF64748B), Color(0xFFE2E8F0), Color(0xFF94A3B8), Color(0xFF475569))
                        ),
                        topLeft = Offset(canvasCenter.x - 7f, 0f),
                        size = Size(14f, 22f),
                        cornerRadius = CornerRadius(3f, 3f)
                    )

                    // Top Central Hinge Ball
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color(0xFFF1F5F9), Color(0xFF64748B), Color(0xFF1E293B)),
                            center = canvasCenter
                        ),
                        radius = 12f,
                        center = canvasCenter
                    )

                    // 2. Central Threaded Spindle / Adjustment Wheel
                    val midY = (canvasCenter.y + pencilTip.y) * 0.42f
                    val leftMid = Offset(canvasCenter.x - (currentRadiusPx * 0.22f), midY)
                    val rightMid = Offset(canvasCenter.x + (currentRadiusPx * 0.22f), midY)

                    // Threaded screw rod
                    drawLine(
                        color = Color(0xFF94A3B8),
                        start = leftMid,
                        end = rightMid,
                        strokeWidth = 3.5f
                    )
                    // Brass thumbwheel
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFFFBBF24), Color(0xFFB45309), Color(0xFFF59E0B))
                        ),
                        topLeft = Offset(canvasCenter.x - 9f, midY - 7f),
                        size = Size(18f, 14f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )

                    // 3. LEFT LEG: Metal Holder with Classic Wooden Pencil (Moving Part on the Left)
                    val leftLegPath = Path().apply {
                        moveTo(canvasCenter.x - 4f, canvasCenter.y)
                        lineTo(pencilTip.x - 7f, pencilTip.y - 45f)
                        lineTo(pencilTip.x + 7f, pencilTip.y - 45f)
                        lineTo(canvasCenter.x - 1f, canvasCenter.y)
                        close()
                    }
                    drawPath(
                        path = leftLegPath,
                        brush = Brush.linearGradient(
                            listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8), Color(0xFF475569))
                        )
                    )

                    // Pencil Clamp Ring on Left Leg
                    drawRoundRect(
                        color = Color(0xFF334155),
                        topLeft = Offset(pencilTip.x - 8f, pencilTip.y - 46f),
                        size = Size(16f, 12f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )

                    // Yellow Wooden Pencil Body
                    val pencilWoodPath = Path().apply {
                        moveTo(pencilTip.x - 6f, pencilTip.y - 40f)
                        lineTo(pencilTip.x + 6f, pencilTip.y - 40f)
                        lineTo(pencilTip.x + 6f, pencilTip.y - 14f)
                        lineTo(pencilTip.x - 6f, pencilTip.y - 14f)
                        close()
                    }
                    drawPath(
                        path = pencilWoodPath,
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFFEAB308), Color(0xFFFDE047), Color(0xFFCA8A04))
                        )
                    )

                    // Sharpened Wooden Tip
                    val pencilTipWoodPath = Path().apply {
                        moveTo(pencilTip.x - 6f, pencilTip.y - 14f)
                        lineTo(pencilTip.x + 6f, pencilTip.y - 14f)
                        lineTo(pencilTip.x, pencilTip.y - 4f)
                        close()
                    }
                    drawPath(path = pencilTipWoodPath, color = Color(0xFFFED7AA))

                    // Graphite Lead Point
                    val graphiteTipPath = Path().apply {
                        moveTo(pencilTip.x - 2f, pencilTip.y - 4f)
                        lineTo(pencilTip.x + 2f, pencilTip.y - 4f)
                        lineTo(pencilTip.x, pencilTip.y)
                        close()
                    }
                    drawPath(path = graphiteTipPath, color = Color(0xFF1E293B))

                    // 4. RIGHT LEG: Solid Steel Needle Leg (Pivot Anchor on the Right)
                    val rightLegPath = Path().apply {
                        moveTo(canvasCenter.x + 1f, canvasCenter.y)
                        lineTo(needleTip.x - 3f, needleTip.y - 18f)
                        lineTo(needleTip.x, needleTip.y)
                        lineTo(needleTip.x + 3f, needleTip.y - 18f)
                        lineTo(canvasCenter.x + 4f, canvasCenter.y)
                        close()
                    }
                    drawPath(
                        path = rightLegPath,
                        brush = Brush.linearGradient(
                            listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8), Color(0xFF64748B))
                        )
                    )
                    // Sharp Steel Pin Tip
                    drawLine(
                        color = Color(0xFF0F172A),
                        start = Offset(needleTip.x, needleTip.y - 14f),
                        end = needleTip,
                        strokeWidth = 2.5f,
                        cap = StrokeCap.Round
                    )

                    // Center Pivot Crosshair indicator on needle
                    drawCircle(color = Color(0xFFDC2626), radius = 3.5f, center = needleTip)
                    // Radius Guide Arc
                    drawArc(
                        color = Color(0x66059669),
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(needleTip.x - 10f, needleTip.y - 10f),
                        size = Size(20f, 20f),
                        style = Stroke(1.5f)
                    )
                }

                // Interactive Radius Slider (Adjusts length from the left smoothly)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(L.radiusLabel(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                    Slider(
                        value = currentRadiusPx,
                        onValueChange = onRadiusChange,
                        valueRange = 50f..300f,
                        modifier = Modifier.weight(1f).height(24.dp)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFD1FAE5),
                        border = BorderStroke(1.dp, Color(0xFF059669))
                    ) {
                        Text(
                            text = "${String.format(Locale.ENGLISH, "%.1f", cm)} cm",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF065F46),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Draw Actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onDrawCircleOrArc(currentCenter, currentRadiusPx, 360f) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(L.drawFullCircle(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { onDrawCircleOrArc(currentCenter, currentRadiusPx, 180f) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(L.drawHalfCircle(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { onDrawCircleOrArc(currentCenter, currentRadiusPx, 90f) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(L.drawQuarterArc(), fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

// ============================================================================
// 4. REALISTIC SET SQUARE (المثلث القائم)
// ============================================================================
@Composable
fun RealisticSetSquare(
    offset: Offset,
    angle: Float = 0f,
    onOffsetChange: (Offset) -> Unit,
    onAngleChange: (Float) -> Unit,
    onDrawTriangle: (Offset, Float, Float) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOffset by rememberUpdatedState(offset)
    val currentAngle by rememberUpdatedState(angle)
    val triangleSize = 180f

    Box(
        modifier = modifier
            .absoluteOffset {
                IntOffset(
                    currentOffset.x.roundToInt().coerceIn(-100, 3000),
                    currentOffset.y.roundToInt().coerceIn(-100, 4000)
                )
            }
            .rotate(currentAngle)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xF7FFFBEB),
            border = BorderStroke(1.5.dp, Color(0xFFD97706)),
            shadowElevation = 10.dp,
            modifier = Modifier.wrapContentSize()
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFEF3C7))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onOffsetChange(currentOffset + dragAmount)
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.DragIndicator, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("📐 30°-60°-90°", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = L.close(), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(onClick = { onAngleChange((currentAngle + 45f) % 360) }, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Filled.RotateRight, contentDescription = L.rotate(), tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                    }

                    Button(
                        onClick = { onDrawTriangle(currentOffset, currentAngle, triangleSize) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(L.drawTriangle(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ============================================================================
// 5. EXACT PIXEL-PERFECT INTERACTIVE 2D VERTEX HANDLES & SHAPE TRANSLATOR
// ============================================================================
@Composable
fun InteractiveVertexHandles(
    shapeState: VertexShapeState,
    panOffset: Offset = Offset.Zero,
    zoomScale: Float = 1f,
    onVertexMoved: (Int, Offset) -> Unit,
    onMoveWholeShape: ((Offset) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val handleSizeDp = 42.dp
    val handleRadiusPx = with(density) { (handleSizeDp / 2).toPx() }

    val currentShapeState by rememberUpdatedState(shapeState)
    val currentOnVertexMoved by rememberUpdatedState(onVertexMoved)
    val currentOnMoveWholeShape by rememberUpdatedState(onMoveWholeShape)

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Center Whole-Shape Move Controller
        if (currentShapeState.vertices.isNotEmpty() && currentOnMoveWholeShape != null) {
            val centroidWorld = Offset(
                currentShapeState.vertices.map { it.x }.average().toFloat(),
                currentShapeState.vertices.map { it.y }.average().toFloat()
            )
            val centerScreen = panOffset + centroidWorld * zoomScale
            val centerHandleSizeDp = 48.dp
            val centerRadiusPx = with(density) { (centerHandleSizeDp / 2).toPx() }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 8.dp,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .absoluteOffset {
                        IntOffset(
                            (centerScreen.x - centerRadiusPx).roundToInt(),
                            (centerScreen.y - centerRadiusPx).roundToInt()
                        )
                    }
                    .wrapContentSize()
                    .pointerInput(panOffset, zoomScale) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val worldDelta = dragAmount / zoomScale
                            currentOnMoveWholeShape?.invoke(worldDelta)
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Filled.PanToolAlt,
                        contentDescription = L.moveHandle(),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = L.moveHandle(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // 2. Individual Vertex Transform Handles with Continuous Smooth Accumulator
        currentShapeState.vertices.forEachIndexed { index, vertex ->
            val label = ('A' + index).toString()
            val screenPos = panOffset + vertex * zoomScale

            Box(
                modifier = Modifier
                    .absoluteOffset {
                        IntOffset(
                            (screenPos.x - handleRadiusPx).roundToInt(),
                            (screenPos.y - handleRadiusPx).roundToInt()
                        )
                    }
                    .size(handleSizeDp)
                    .pointerInput(index, panOffset, zoomScale) {
                        var runningVertex = Offset.Zero
                        detectDragGestures(
                            onDragStart = {
                                runningVertex = currentShapeState.vertices.getOrElse(index) { Offset.Zero }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val worldDelta = dragAmount / zoomScale
                                runningVertex += worldDelta
                                currentOnVertexMoved(index, runningVertex)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Outer Pulse Ring
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(currentShapeState.color.copy(alpha = 0.28f))
                )
                // Solid Inner Controller with Crosshair Center
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(currentShapeState.color)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
})
                        .background(shapeState.color)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
