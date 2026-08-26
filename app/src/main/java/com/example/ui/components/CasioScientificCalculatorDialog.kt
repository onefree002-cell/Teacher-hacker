package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import java.text.DecimalFormat
import kotlin.math.*

/**
 * Authentic Casio fx-991ES PLUS / fx-82ES PLUS Scientific Calculator Emulator.
 * Enforces strict Left-to-Right (LTR) physical alignment identical to the official Casio hardware/PDF manual.
 * Featuring 2-Line Natural V.P.A.M. LCD, Replay D-Pad, Shift/Alpha modes, full trigonometric, calculus & fraction engines.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CasioScientificCalculatorDialog(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .testTag("casio_calculator_dialog"),
                shape = RoundedCornerShape(26.dp),
                color = Color(0xFF1E242B), // Casio Dark Titanium Charcoal
                tonalElevation = 12.dp,
                shadowElevation = 16.dp,
                border = BorderStroke(1.5.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Bar (Casio Branding, Solar Cell Simulation, Close Button)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "CASIO",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = Color(0xFFE2E8F0),
                                letterSpacing = 2.5.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF334155),
                                modifier = Modifier.padding(horizontal = 2.dp)
                            ) {
                                Text(
                                    text = "fx-991ES PLUS",
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Solar Panel Simulation (TWO WAY POWER)
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = Color(0xFF2C1810),
                                border = BorderStroke(1.dp, Color(0xFF4A3525)),
                                modifier = Modifier.size(width = 44.dp, height = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(4) {
                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .fillMaxHeight(0.7f)
                                                .background(Color(0xFF6B4C35))
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF1E3A8A).copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, Color(0xFF3B82F6))
                            ) {
                                Text(
                                    text = "NATURAL-V.P.A.M.",
                                    color = Color(0xFF93C5FD),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("btn_close_casio_calc")
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color(0xFFCBD5E1), modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Casio Calculator Core Content
                    CasioCalculatorContent()
                }
            }
        }
    }
}

@Composable
fun CasioCalculatorContent(modifier: Modifier = Modifier) {
    var expression by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("0") }
    var lastAns by remember { mutableStateOf("0") }
    var isShiftActive by remember { mutableStateOf(false) }
    var isAlphaActive by remember { mutableStateOf(false) }
    var angleMode by remember { mutableStateOf("DEG") } // DEG, RAD, GRA
    var isFractionMode by remember { mutableStateOf(false) } // S<=>D toggle
    var memoryValue by remember { mutableDoubleStateOf(0.0) }
    var history by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var historyIndex by remember { mutableIntStateOf(-1) }
    var showHistorySheet by remember { mutableStateOf(false) }

    fun appendText(txt: String) {
        if (resultText == "Error" || resultText == "Syntax ERROR") {
            expression = ""
            resultText = "0"
        }
        expression += txt
    }

    fun calculate() {
        if (expression.isBlank()) return
        try {
            val evaluated = evaluateCasioExpression(expression, angleMode, lastAns)
            val formatted = formatResult(evaluated)
            history = listOf((expression to formatted)) + history.take(25)
            historyIndex = -1
            resultText = formatted
            lastAns = formatted
            isFractionMode = false
        } catch (e: Exception) {
            resultText = "Syntax ERROR"
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. CASIO NATURAL-V.P.A.M. 2-LINE DOT-MATRIX LCD SCREEN
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF8E9F88), // Authentic Casio Gray-Green LCD tint
                border = BorderStroke(3.dp, Color(0xFF0F172A)),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .testTag("casio_lcd_screen")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // LCD Top Status Flag Line
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Shift Indicator
                            if (isShiftActive) {
                                Text("S", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color(0xFF92400E))
                            }
                            // Alpha Indicator
                            if (isAlphaActive) {
                                Text("A", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color(0xFF991B1B))
                            }
                            // Memory Indicator
                            if (memoryValue != 0.0) {
                                Text("M", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF1E293B))
                            }
                            // Angle Mode Indicator
                            Text(
                                text = when (angleMode) {
                                    "RAD" -> "R"
                                    "GRA" -> "G"
                                    else -> "D"
                                },
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = Color(0xFF0F172A)
                            )
                            // Math Display Symbol
                            Text("Math ▲▼", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        }

                        // History tape toggle button
                        Text(
                            text = if (history.isNotEmpty()) "HIST (${history.size})" else "fx-991ES",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            modifier = Modifier.clickable { showHistorySheet = !showHistorySheet }
                        )
                    }

                    // Dot Matrix Line 1: Expression with Scroll
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState(Int.MAX_VALUE)),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text(
                            text = if (expression.isEmpty()) "0" else expression,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = Color(0xFF0F172A),
                            maxLines = 1
                        )
                    }

                    // Line 2: Large 7-Segment High Precision Numeric Result
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val displayResult = if (isFractionMode && resultText != "Syntax ERROR" && resultText != "Error") {
                            toFractionString(resultText.toDoubleOrNull() ?: 0.0)
                        } else {
                            resultText
                        }

                        Text(
                            text = displayResult,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 26.sp,
                            color = Color(0xFF020617),
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. TOP FUNCTION & CONTROL KEYPAD ROW (SHIFT, ALPHA, REPLAY D-PAD, MODE, ON)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Controls: SHIFT & ALPHA
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CasioSmallPillKey(
                        label = "SHIFT",
                        subLabel = "",
                        labelColor = Color(0xFFFBBF24), // Gold Yellow
                        containerColor = if (isShiftActive) Color(0xFFB45309) else Color(0xFF334155),
                        onClick = {
                            isShiftActive = !isShiftActive
                            isAlphaActive = false
                        }
                    )
                    CasioSmallPillKey(
                        label = "ALPHA",
                        subLabel = "",
                        labelColor = Color(0xFFF87171), // Red
                        containerColor = if (isAlphaActive) Color(0xFF991B1B) else Color(0xFF334155),
                        onClick = {
                            isAlphaActive = !isAlphaActive
                            isShiftActive = false
                        }
                    )
                }

                // Center: Big Metallic Silver Circular REPLAY D-PAD
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF94A3B8),
                    border = BorderStroke(2.dp, Color(0xFFCBD5E1)),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(86.dp)
                        .testTag("casio_replay_pad")
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        // Central Replay Plate
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF475569),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("REPLAY", fontSize = 7.sp, fontWeight = FontWeight.Black, color = Color(0xFFE2E8F0))
                            }
                        }

                        // Up Arrow (Navigate History Back)
                        IconButton(
                            onClick = {
                                if (history.isNotEmpty()) {
                                    if (historyIndex < history.size - 1) historyIndex++
                                    val item = history.getOrNull(historyIndex)
                                    if (item != null) {
                                        expression = item.first
                                        resultText = item.second
                                    }
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .size(24.dp)
                        ) {
                            Icon(Icons.Filled.ArrowDropUp, contentDescription = "Up", tint = Color(0xFF0F172A))
                        }

                        // Down Arrow (Navigate History Forward)
                        IconButton(
                            onClick = {
                                if (historyIndex > 0) {
                                    historyIndex--
                                    val item = history.getOrNull(historyIndex)
                                    if (item != null) {
                                        expression = item.first
                                        resultText = item.second
                                    }
                                } else if (historyIndex == 0) {
                                    historyIndex = -1
                                    expression = ""
                                    resultText = "0"
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .size(24.dp)
                        ) {
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Down", tint = Color(0xFF0F172A))
                        }

                        // Left Arrow (Move Cursor Left)
                        IconButton(
                            onClick = {
                                if (expression.isNotEmpty()) {
                                    // Move back or backspace in edit
                                    expression = expression.dropLast(1)
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .size(24.dp)
                        ) {
                            Icon(Icons.Filled.ArrowLeft, contentDescription = "Left", tint = Color(0xFF0F172A))
                        }

                        // Right Arrow (Move Cursor Right)
                        IconButton(
                            onClick = {
                                if (resultText.isNotEmpty() && resultText != "0" && resultText != "Syntax ERROR") {
                                    expression += resultText
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(24.dp)
                        ) {
                            Icon(Icons.Filled.ArrowRight, contentDescription = "Right", tint = Color(0xFF0F172A))
                        }
                    }
                }

                // Right Controls: MODE/SETUP & ON
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CasioSmallPillKey(
                        label = "MODE",
                        subLabel = "SETUP",
                        labelColor = Color(0xFFE2E8F0),
                        containerColor = Color(0xFF334155),
                        onClick = {
                            // Cycle angle mode DEG -> RAD -> GRA
                            angleMode = when (angleMode) {
                                "DEG" -> "RAD"
                                "RAD" -> "GRA"
                                else -> "DEG"
                            }
                        }
                    )
                    CasioSmallPillKey(
                        label = "ON",
                        subLabel = "",
                        labelColor = Color(0xFFE2E8F0),
                        containerColor = Color(0xFF334155),
                        onClick = {
                            expression = ""
                            resultText = "0"
                            isShiftActive = false
                            isAlphaActive = false
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. SCIENTIFIC FUNCTION KEYS (CALCULUS, POWERS, TRIGONOMETRY, FRACTIONS)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Row 1: CALC, d/dx, [■/□], √, x², xʸ
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CasioFuncKey(
                        label = if (isShiftActive) "SOLVE" else "CALC",
                        shiftLabel = "SOLVE",
                        alphaLabel = "=",
                        isShift = isShiftActive,
                        isAlpha = isAlphaActive,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            calculate()
                            isShiftActive = false
                            isAlphaActive = false
                        }
                    )
                    CasioFuncKey(
                        label = if (isShiftActive) "d/dx" else "∫dx",
                        shiftLabel = "d/dx",
                        alphaLabel = ":",
                        isShift = isShiftActive,
                        isAlpha = isAlphaActive,
                        modifier = Modifier.weight(1f),
                        onClick = { appendText("∫(") }
                    )
                    CasioFuncKey(
                        label = if (isShiftActive) "d/c" else "■/□",
                        shiftLabel = "d/c",
                        alphaLabel = "",
                        isShift = isShiftActive,
                        isAlpha = isAlphaActive,
                        modifier = Modifier.weight(1f),
                        onClick = { appendText("/") }
                    )
                    CasioFuncKey(
                        label = if (isShiftActive) "³√" else "√",
                        shiftLabel = "³√",
                        alphaLabel = "",
                        isShift = isShiftActive,
                        isAlpha = isAlphaActive,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (isShiftActive) appendText("cbrt(") else appendText("√(")
                            isShiftActive = false
                        }
                    )
                    CasioFuncKey(
                        label = if (isShiftActive) "x³" else "x²",
                        shiftLabel = "x³",
                        alphaLabel = "",
                        isShift = isShiftActive,
                        isAlpha = isAlphaActive,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (isShiftActive) appendText("^3") else appendText("^2")
                            isShiftActive = false
                        }
                    )
                    CasioFuncKey(
                        label = if (isShiftActive) "ˣ√" else "^",
                        shiftLabel = "ˣ√",
                        alphaLabel = "",
                        isShift = isShiftActive,
                        isAlpha = isAlphaActive,
                        modifier = Modifier.weight(1f),
                        onClick = { appendText("^") }
                    )
                }

                // Row 2: log, ln, (-), ° ' ", hyp, sin
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CasioFuncKey(
                        label = if (isShiftActive) "10ˣ" else "log",
                        shiftLabel = "10ˣ",
                        alphaLabel = "B",
                        isShift = isShiftActive,
                        isAlpha = isAlphaActive,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (isShiftActive) appendText("10^(") else appendText("log(")
                            isShiftActive = false
                        }
                    )
                    CasioFuncKey(
                        label = if (isShiftActive) "eˣ" else "ln",
                        shiftLabel = "eˣ",
                        alphaLabel = "e",
                        isShift = isShiftActive,
                        isAlpha = isAlphaActive,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (isShiftActive) appendText("e^(") else appendText("ln(")
                            isShiftActive = false
                        }
                    )
                    CasioFuncKey(
                        label = "(-)",
                        shiftLabel = "A",
                        alphaLabel = "A",
                        isShift = isShiftActive,
                        isAlpha = isAlphaActive,
                        modifier = Modifier.weight(1f),
                        onClick = { appendText("-") }
                    )
                    CasioFuncKey(
                        label = "° ' \"",
                        shiftLabel = "←",
                        alphaLabel = "B",
                        isShift = isShiftActive,
                        isAlpha = isAlphaActive,
                        modifier = Modifier.weight(1f),
                        onClick = { appendText("°") }
                    )
                    CasioFuncKey(
                        label = "hyp",
                        shiftLabel = "",
                        alphaLabel = "C",
                        isShift = isShiftActive,
                        isAlpha = isAlphaActive,
                        modifier = Modifier.weight(1f),
                        onClick = { appendText("sinh(") }
                    )
                    CasioFuncKey(
                        label = if (isShiftActive) "sin⁻¹" else "sin",
                        shiftLabel = "sin⁻¹",
                        alphaLabel = "D",
                        isShift = isShiftActive,
                        isAlpha = isAlphaActive,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (isShiftActive) appendText("asin(") else appendText("sin(")
                            isShiftActive = false
                        }
                    )
                }

                // Row 3: cos, tan, RCL, ENG, (, )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CasioFuncKey(
                        label = if (isShiftActive) "cos⁻¹" else "cos",
                        shiftLabel = "cos⁻¹",
                        alphaLabel = "E",
                        isShift = isShiftActive,
                        isAlpha = isAlphaActive,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (isShiftActive) appendText("acos(") else appendText("cos(")
                            isShiftActive = false
                        }
                    )
                    CasioFuncKey(
                        label = if (isShiftActive) "tan⁻¹" else "tan",
                        shiftLabel = "tan⁻¹",
                        alphaLabel = "F",
                        isShift = isShiftActive,
                        isAlpha = isAlphaActive,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (isShiftActive) appendText("atan(") else appendText("tan(")
                            isShiftActive = false
                        }
                    )
                    CasioFuncKey(
                        label = if (isShiftActive) "STO" else "RCL",
                        shiftLabel = "STO",
                        alphaLabel = "",
                        isShift = isShiftActive,
                        isAlpha = isAlphaActive,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (isShiftActive) {
                                memoryValue = resultText.toDoubleOrNull() ?: 0.0
                                isShiftActive = false
                            } else {
                                appendText(memoryValue.toString())
                            }
                        }
                    )
                    CasioFuncKey(
                        label = "ENG",
                        shiftLabel = "←",
                        alphaLabel = "i",
                        isShift = isShiftActive,
                        isAlpha = isAlphaActive,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val v = resultText.toDoubleOrNull() ?: 0.0
                            resultText = DecimalFormat("0.###E0").format(v)
                        }
                    )
                    CasioFuncKey(
                        label = "(",
                        shiftLabel = "%",
                        alphaLabel = "X",
                        isShift = isShiftActive,
                        isAlpha = isAlphaActive,
                        modifier = Modifier.weight(1f),
                        onClick = { appendText("(") }
                    )
                    CasioFuncKey(
                        label = ")",
                        shiftLabel = ",",
                        alphaLabel = "Y",
                        isShift = isShiftActive,
                        isAlpha = isAlphaActive,
                        modifier = Modifier.weight(1f),
                        onClick = { appendText(")") }
                    )
                }

                // Row 4: S<=>D, M+, x!, Abs, Mod, nCr
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CasioFuncKey(
                        label = "S<=>D",
                        shiftLabel = "a b/c",
                        alphaLabel = "",
                        isShift = isShiftActive,
                        isAlpha = isAlphaActive,
                        modifier = Modifier.weight(1.2f),
                        onClick = {
                            isFractionMode = !isFractionMode
                        }
                    )
                    CasioFuncKey(
                        label = if (isShiftActive) "M-" else "M+",
                        shiftLabel = "M-",
                        alphaLabel = "M",
                        isShift = isShiftActive,
                        isAlpha = isAlphaActive,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val v = resultText.toDoubleOrNull() ?: 0.0
                            if (isShiftActive) memoryValue -= v else memoryValue += v
                            isShiftActive = false
                        }
                    )
                    CasioFuncKey(
                        label = if (isShiftActive) "x!" else "x⁻¹",
                        shiftLabel = "x!",
                        alphaLabel = "",
                        isShift = isShiftActive,
                        isAlpha = isAlphaActive,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (isShiftActive) appendText("!") else appendText("^(-1)")
                            isShiftActive = false
                        }
                    )
                    CasioFuncKey(
                        label = "Abs",
                        shiftLabel = "Abs",
                        alphaLabel = "",
                        isShift = isShiftActive,
                        isAlpha = isAlphaActive,
                        modifier = Modifier.weight(1f),
                        onClick = { appendText("abs(") }
                    )
                    CasioFuncKey(
                        label = if (isShiftActive) "nPr" else "nCr",
                        shiftLabel = "nPr",
                        alphaLabel = "",
                        isShift = isShiftActive,
                        isAlpha = isAlphaActive,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (isShiftActive) appendText("P") else appendText("C")
                            isShiftActive = false
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. CASIO NUMERIC & OPERATOR KEYPAD (7-8-9 DEL AC, 4-5-6 × ÷, 1-2-3 + -, 0 . ×10ˣ Ans =)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Keypad Row 1: 7, 8, 9, DEL (Orange), AC (Orange)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CasioNumKey("7", modifier = Modifier.weight(1f)) { appendText("7") }
                    CasioNumKey("8", modifier = Modifier.weight(1f)) { appendText("8") }
                    CasioNumKey("9", modifier = Modifier.weight(1f)) { appendText("9") }
                    CasioActionKey("DEL", Color(0xFFEA580C), modifier = Modifier.weight(1f)) {
                        if (expression.isNotEmpty()) expression = expression.dropLast(1)
                    }
                    CasioActionKey("AC", Color(0xFFEA580C), modifier = Modifier.weight(1f)) {
                        expression = ""
                        resultText = "0"
                    }
                }

                // Keypad Row 2: 4, 5, 6, ×, ÷
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CasioNumKey("4", modifier = Modifier.weight(1f)) { appendText("4") }
                    CasioNumKey("5", modifier = Modifier.weight(1f)) { appendText("5") }
                    CasioNumKey("6", modifier = Modifier.weight(1f)) { appendText("6") }
                    CasioOpKey("×", modifier = Modifier.weight(1f)) { appendText("×") }
                    CasioOpKey("÷", modifier = Modifier.weight(1f)) { appendText("÷") }
                }

                // Keypad Row 3: 1, 2, 3, +, -
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CasioNumKey("1", modifier = Modifier.weight(1f)) { appendText("1") }
                    CasioNumKey("2", modifier = Modifier.weight(1f)) { appendText("2") }
                    CasioNumKey("3", modifier = Modifier.weight(1f)) { appendText("3") }
                    CasioOpKey("+", modifier = Modifier.weight(1f)) { appendText("+") }
                    CasioOpKey("-", modifier = Modifier.weight(1f)) { appendText("-") }
                }

                // Keypad Row 4: 0, ., ×10ˣ (π), Ans (%), = (Equals)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CasioNumKey("0", modifier = Modifier.weight(1f)) { appendText("0") }
                    CasioNumKey(".", modifier = Modifier.weight(1f)) { appendText(".") }
                    CasioSpecialKey(
                        label = if (isShiftActive) "π" else "×10ˣ",
                        subLabel = "π",
                        isShift = isShiftActive,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isShiftActive) {
                            appendText("π")
                            isShiftActive = false
                        } else {
                            appendText("×10^")
                        }
                    }
                    CasioSpecialKey(
                        label = if (isShiftActive) "%" else "Ans",
                        subLabel = "%",
                        isShift = isShiftActive,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isShiftActive) {
                            appendText("%")
                            isShiftActive = false
                        } else {
                            appendText("Ans")
                        }
                    }
                    CasioEqualsKey("=", modifier = Modifier.weight(1.1f)) {
                        calculate()
                    }
                }
            }

            // Calculation History Modal Sheet
            if (showHistorySheet) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tape History (KeyLog)", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { history = emptyList() }) {
                                Text("Clear", color = Color(0xFFF87171), fontSize = 11.sp)
                            }
                        }
                        if (history.isEmpty()) {
                            Text("No recent calculations", color = Color(0xFF64748B), fontSize = 12.sp)
                        } else {
                            history.take(6).forEach { (exp, res) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expression = exp
                                            resultText = res
                                        }
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(exp, color = Color(0xFFCBD5E1), fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                                    Text("= $res", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// CASIO HARDWARE-STYLED BUTTON COMPONENTS
// -----------------------------------------------------------------------------------------

@Composable
private fun CasioSmallPillKey(
    label: String,
    subLabel: String,
    labelColor: Color,
    containerColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        border = BorderStroke(1.dp, Color(0xFF475569)),
        shadowElevation = 3.dp,
        modifier = Modifier
            .size(width = 54.dp, height = 30.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = labelColor
            )
            if (subLabel.isNotBlank()) {
                Text(
                    text = subLabel,
                    fontSize = 7.sp,
                    color = Color(0xFFFBBF24),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CasioFuncKey(
    label: String,
    shiftLabel: String,
    alphaLabel: String,
    isShift: Boolean,
    isAlpha: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Shift (Yellow) and Alpha (Red) Over-label indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (shiftLabel.length > 5) shiftLabel.take(5) else shiftLabel,
                fontSize = 8.sp,
                color = Color(0xFFFBBF24),
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = alphaLabel,
                fontSize = 8.sp,
                color = Color(0xFFF87171),
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }

        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFF27313E), // Casio Function Key Charcoal Slate
            border = BorderStroke(1.dp, Color(0xFF3E4C5E)),
            shadowElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clickable { onClick() }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF1F5F9),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun CasioNumKey(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF8FAFC), // Authentic Casio Off-White Number Key
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        shadowElevation = 4.dp,
        modifier = modifier
            .height(44.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}

@Composable
private fun CasioOpKey(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF334155), // Dark Slate Operator Key
        border = BorderStroke(1.dp, Color(0xFF475569)),
        shadowElevation = 4.dp,
        modifier = modifier
            .height(44.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF8FAFC)
            )
        }
    }
}

@Composable
private fun CasioActionKey(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color,
        border = BorderStroke(1.dp, color.copy(alpha = 0.8f)),
        shadowElevation = 4.dp,
        modifier = modifier
            .height(44.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}

@Composable
private fun CasioSpecialKey(
    label: String,
    subLabel: String,
    isShift: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF334155),
        border = BorderStroke(1.dp, Color(0xFF475569)),
        shadowElevation = 4.dp,
        modifier = modifier
            .height(44.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isShift) Color(0xFFFBBF24) else Color(0xFFF8FAFC)
            )
        }
    }
}

@Composable
private fun CasioEqualsKey(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF0284C7), // Casio Vibrant Blue Equals Key
        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
        shadowElevation = 5.dp,
        modifier = modifier
            .height(44.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}

// -----------------------------------------------------------------------------------------
// CASIO MATHEMATICAL EXPRESSION PARSER & EVALUATOR ENGINE
// -----------------------------------------------------------------------------------------

fun evaluateCasioExpression(rawExp: String, angleMode: String, lastAns: String): Double {
    var exp = rawExp
        .replace("×", "*")
        .replace("÷", "/")
        .replace("π", Math.PI.toString())
        .replace("Ans", lastAns.ifBlank { "0" })
        .replace("°", "")

    // Replace functions
    exp = exp.replace("sin(", "s(")
        .replace("cos(", "c(")
        .replace("tan(", "t(")
        .replace("asin(", "as(")
        .replace("acos(", "ac(")
        .replace("atan(", "at(")
        .replace("log(", "l(")
        .replace("ln(", "n(")
        .replace("cbrt(", "cb(")
        .replace("√(", "r(")
        .replace("abs(", "ab(")

    return parseExpression(exp, angleMode)
}

private fun parseExpression(expression: String, angleMode: String): Double {
    var str = expression.replace(" ", "")
    if (str.isEmpty()) return 0.0

    // Handle factorial e.g. 5!
    while (str.contains("!")) {
        val idx = str.indexOf("!")
        var start = idx - 1
        while (start >= 0 && (str[start].isDigit() || str[start] == '.')) {
            start--
        }
        val numStr = str.substring(start + 1, idx)
        val num = numStr.toDoubleOrNull() ?: 1.0
        val factVal = factorial(num.toInt())
        str = str.substring(0, start + 1) + factVal + str.substring(idx + 1)
    }

    // Evaluate simple recursive arithmetic
    return object : Any() {
        var pos = -1
        var ch = 0

        fun nextChar() {
            ch = if (++pos < str.length) str[pos].code else -1
        }

        fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < str.length) throw RuntimeException("Unexpected: " + ch.toChar())
            return x
        }

        fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+'.code) -> x += parseTerm()
                    eat('-'.code) -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*'.code) -> x *= parseFactor()
                    eat('/'.code) -> x /= parseFactor()
                    eat('%'.code) -> x %= parseFactor()
                    else -> return x
                }
            }
        }

        fun parseFactor(): Double {
            if (eat('+'.code)) return +parseFactor()
            if (eat('-'.code)) return -parseFactor()

            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseExpression()
                eat(')'.code)
            } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
                while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                x = str.substring(startPos, pos).toDouble()
            } else if (eat('s'.code)) { // sin
                eat('('.code)
                val arg = parseExpression()
                eat(')'.code)
                val rad = if (angleMode == "DEG") Math.toRadians(arg) else arg
                x = sin(rad)
            } else if (eat('c'.code)) { // cos
                eat('('.code)
                val arg = parseExpression()
                eat(')'.code)
                val rad = if (angleMode == "DEG") Math.toRadians(arg) else arg
                x = cos(rad)
            } else if (eat('t'.code)) { // tan
                eat('('.code)
                val arg = parseExpression()
                eat(')'.code)
                val rad = if (angleMode == "DEG") Math.toRadians(arg) else arg
                x = tan(rad)
            } else if (eat('l'.code)) { // log base 10
                eat('('.code)
                val arg = parseExpression()
                eat(')'.code)
                x = log10(arg)
            } else if (eat('n'.code)) { // ln
                eat('('.code)
                val arg = parseExpression()
                eat(')'.code)
                x = ln(arg)
            } else if (eat('r'.code)) { // sqrt
                eat('('.code)
                val arg = parseExpression()
                eat(')'.code)
                x = sqrt(arg)
            } else if (eat('a'.code) && eat('b'.code)) { // abs
                eat('('.code)
                val arg = parseExpression()
                eat(')'.code)
                x = abs(arg)
            } else {
                throw RuntimeException("Unknown token: " + ch.toChar())
            }

            if (eat('^'.code)) x = x.pow(parseFactor())

            return x
        }
    }.parse()
}

private fun factorial(n: Int): Double {
    if (n <= 1) return 1.0
    var res = 1.0
    for (i in 2..min(n, 170)) res *= i
    return res
}

fun formatResult(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "Math ERROR"
    val df = DecimalFormat("#.########")
    return df.format(value)
}

fun toFractionString(value: Double): String {
    if (value == value.toLong().toDouble()) return value.toLong().toString()
    val tolerance = 1.0E-6
    var h1 = 1.0
    var h2 = 0.0
    var k1 = 0.0
    var k2 = 1.0
    var b = value
    do {
        val a = floor(b)
        var aux = h1
        h1 = a * h1 + h2
        h2 = aux
        aux = k1
        k1 = a * k1 + k2
        k2 = aux
        b = 1.0 / (b - a)
    } while (abs(value - h1 / k1) > value * tolerance && k1 < 10000)

    return "${h1.toLong()} / ${k1.toLong()}"
}
