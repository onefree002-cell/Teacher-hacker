package com.example.ui.screens.studyfiles

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.util.L
import java.util.Locale
import kotlin.math.*

/**
 * Photorealistic Casio fx-991ES PLUS / fx-82ES PLUS Scientific Calculator
 * with Natural Textbook Display and Mathematical Parsing Engine
 */

class ScientificEngine {
    var isRadianMode: Boolean = false
    var lastAnswer: Double = 0.0
    val history = mutableListOf<Pair<String, String>>()
    var historyIndex = -1

    fun evaluate(expression: String): String {
        if (expression.isBlank()) return "0"
        try {
            var formatted = expression
                .replace("×", "*")
                .replace("÷", "/")
                .replace("−", "-")
                .replace("π", "${Math.PI}")
                .replace("e", "${Math.E}")
                .replace("Ans", "$lastAnswer")

            val result = parseExpression(formatted)
            lastAnswer = result
            history.add(expression to formatResult(result))
            historyIndex = history.size
            return formatResult(result)
        } catch (e: Exception) {
            return "Syntax ERROR"
        }
    }

    private fun formatResult(value: Double): String {
        if (value.isNaN()) return "Math ERROR"
        if (value.isInfinite()) return "Math ERROR (∞)"
        if (abs(value - round(value)) < 1e-11) {
            return round(value).toLong().toString()
        }
        val rounded = String.format(Locale.US, "%.10f", value).trimEnd('0').trimEnd('.')
        return if (rounded.length > 13) {
            String.format(Locale.US, "%.6e", value)
        } else {
            rounded
        }
    }

    private fun parseExpression(expr: String): Double {
        val tokens = tokenize(expr)
        val parser = ExpressionParser(tokens, isRadianMode)
        return parser.parseExpr()
    }

    private class ExpressionParser(
        private val tokens: List<String>,
        private val isRadianMode: Boolean
    ) {
        private var pos = 0

        private fun peek(): String = if (pos < tokens.size) tokens[pos] else ""
        private fun get(): String = if (pos < tokens.size) tokens[pos++] else ""

        fun parseExpr(): Double {
            var value = parseTerm()
            while (peek() == "+" || peek() == "-") {
                val op = get()
                val next = parseTerm()
                value = when (op) {
                    "+" -> value + next
                    "-" -> value - next
                    else -> value
                }
            }
            return value
        }

        private fun parseTerm(): Double {
            var value = parsePower()
            while (peek() == "*" || peek() == "/" || peek() == "%") {
                val op = get()
                val next = parsePower()
                value = when (op) {
                    "*" -> value * next
                    "/" -> if (next == 0.0) Double.NaN else value / next
                    "%" -> value % next
                    else -> value
                }
            }
            return value
        }

        private fun parsePower(): Double {
            var value = parseFactor()
            while (peek() == "^" || peek() == "²" || peek() == "³") {
                val op = get()
                when (op) {
                    "²" -> value = value.pow(2.0)
                    "³" -> value = value.pow(3.0)
                    "^" -> {
                        val exponent = parseFactor()
                        value = value.pow(exponent)
                    }
                }
            }
            return value
        }

        private fun parseFactor(): Double {
            val token = get()
            if (token == "-") {
                return -parseFactor()
            }
            if (token == "+") {
                return parseFactor()
            }
            if (token == "(") {
                val value = parseExpr()
                if (peek() == ")") {
                    get()
                }
                return value
            }
            if (token.matches(Regex("^[0-9]+(\\.[0-9]+)?$"))) {
                var num = token.toDouble()
                if (peek() == "!") {
                    get()
                    num = factorial(num.toInt()).toDouble()
                }
                return num
            }
            if (token in listOf("sin", "cos", "tan", "asin", "acos", "atan", "sinh", "cosh", "tanh", "ln", "log", "sqrt", "cbrt", "abs")) {
                val arg = if (peek() == "(") {
                    get() // consume (
                    val v = parseExpr()
                    if (peek() == ")") get()
                    v
                } else {
                    parseFactor()
                }

                return when (token) {
                    "sin" -> {
                        val angle = if (!isRadianMode) Math.toRadians(arg) else arg
                        if (abs(sin(angle)) < 1e-12) 0.0 else sin(angle)
                    }
                    "cos" -> {
                        val angle = if (!isRadianMode) Math.toRadians(arg) else arg
                        if (abs(cos(angle)) < 1e-12) 0.0 else cos(angle)
                    }
                    "tan" -> {
                        val angle = if (!isRadianMode) Math.toRadians(arg) else arg
                        val cosVal = cos(angle)
                        if (abs(cosVal) < 1e-12) Double.NaN else tan(angle)
                    }
                    "asin" -> {
                        val v = asin(arg)
                        if (!isRadianMode) Math.toDegrees(v) else v
                    }
                    "acos" -> {
                        val v = acos(arg)
                        if (!isRadianMode) Math.toDegrees(v) else v
                    }
                    "atan" -> {
                        val v = atan(arg)
                        if (!isRadianMode) Math.toDegrees(v) else v
                    }
                    "sinh" -> sinh(arg)
                    "cosh" -> cosh(arg)
                    "tanh" -> tanh(arg)
                    "ln" -> if (arg <= 0) Double.NaN else ln(arg)
                    "log" -> if (arg <= 0) Double.NaN else log10(arg)
                    "sqrt" -> if (arg < 0) Double.NaN else sqrt(arg)
                    "cbrt" -> cbrt(arg)
                    "abs" -> abs(arg)
                    else -> arg
                }
            }
            return 0.0
        }

        private fun factorial(n: Int): Long {
            if (n < 0 || n > 20) return 0
            var f = 1L
            for (i in 2..n) f *= i
            return f
        }
    }

    private fun tokenize(expr: String): List<String> {
        val result = mutableListOf<String>()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            if (c.isWhitespace()) {
                i++
                continue
            }
            if (c in "+-*/^%()!²³") {
                result.add(c.toString())
                i++
                continue
            }
            if (c.isDigit() || c == '.') {
                val sb = StringBuilder()
                while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                    sb.append(expr[i])
                    i++
                }
                result.add(sb.toString())
                continue
            }
            if (c.isLetter()) {
                val sb = StringBuilder()
                while (i < expr.length && expr[i].isLetter()) {
                    sb.append(expr[i])
                    i++
                }
                result.add(sb.toString())
                continue
            }
            i++
        }
        return result
    }
}

@Composable
fun CasioScientificCalculatorDialog(
    onDismiss: () -> Unit,
    onInsertResultToBoard: ((String) -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CasioCalculatorBody(
            onDismiss = onDismiss,
            onInsertResultToBoard = onInsertResultToBoard
        )
    }
}

@Composable
fun CasioCalculatorOverlay(
    offset: Offset,
    onOffsetChange: (Offset) -> Unit,
    onClose: () -> Unit,
    onInsertResultToBoard: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .absoluteOffset {
                IntOffset(
                    offset.x.roundToInt().coerceIn(-100, 3000),
                    offset.y.roundToInt().coerceIn(-100, 4000)
                )
            }
    ) {
        CasioCalculatorBody(
            onDismiss = onClose,
            onInsertResultToBoard = onInsertResultToBoard,
            onDragHeader = { delta -> onOffsetChange(offset + delta) },
            isOverlay = true
        )
    }
}

@Composable
fun CasioCalculatorBody(
    onDismiss: () -> Unit,
    onInsertResultToBoard: ((String) -> Unit)? = null,
    onDragHeader: ((Offset) -> Unit)? = null,
    isOverlay: Boolean = false
) {
    val engine = remember { ScientificEngine() }
    var expression by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("0") }
    var isShiftActive by remember { mutableStateOf(false) }
    var isAlphaActive by remember { mutableStateOf(false) }
    var isRadianMode by remember { mutableStateOf(false) }
    var showFractionResult by remember { mutableStateOf(false) }

    fun append(text: String) {
        expression += text
        isShiftActive = false
        isAlphaActive = false
    }

    fun calculate() {
        if (expression.isNotBlank()) {
            engine.isRadianMode = isRadianMode
            resultText = engine.evaluate(expression)
        }
    }

    fun clearAll() {
        expression = ""
        resultText = "0"
        isShiftActive = false
        isAlphaActive = false
    }

    fun backspace() {
        if (expression.isNotEmpty()) {
            val functions = listOf("sin(", "cos(", "tan(", "asin(", "acos(", "atan(", "sinh(", "cosh(", "tanh(", "ln(", "log(", "sqrt(", "cbrt(", "abs(")
            val matched = functions.firstOrNull { expression.endsWith(it) }
            if (matched != null) {
                expression = expression.dropLast(matched.length)
            } else {
                expression = expression.dropLast(1)
            }
        }
    }

    // Outer Casio Real Physical Curved Casing (Silver-Slate Gradient Body)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Card(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 22.dp, bottomEnd = 22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E242C) // Real fx-991ES dark slate lower casing
        ),
        border = BorderStroke(2.dp, Color(0xFF475569)),
        elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
        modifier = Modifier
            .width(360.dp)
            .wrapContentHeight()
            .padding(if (isOverlay) 0.dp else 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Upper Silver Bezel Header Section
            Surface(
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
                color = Color(0xFFE2E8F0), // Metallic Silver upper plate
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Header Bar with Drag, Brand Logo & Solar Cell
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (onDragHeader != null) {
                                    Modifier.pointerInput(Unit) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            onDragHeader(dragAmount)
                                        }
                                    }
                                } else Modifier
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (onDragHeader != null) {
                                Icon(Icons.Filled.DragIndicator, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Column {
                                Text(
                                    text = "CASIO",
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    letterSpacing = 2.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "fx-991ES PLUS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0369A1)
                                )
                            }
                        }

                        // Authentic Solar Glass Cell & Close Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Solar Panel with 4 cell partitions
                            Row(
                                modifier = Modifier
                                    .width(64.dp)
                                    .height(18.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFF2E2412))
                                    .border(1.dp, Color(0xFF854D0E), RoundedCornerShape(3.dp))
                                    .padding(1.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                repeat(4) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(13.dp)
                                            .background(Color(0xFF45361A))
                                    )
                                }
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "إغلاق", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Text(
                        text = "NATURAL-V.P.A.M.  TWO WAY POWER",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                    )

                    // Authentic Natural Textbook LCD Display
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFB8C7B0), // Authentic Casio dot-matrix LCD greenish tone
                        border = BorderStroke(2.dp, Color(0xFF334155)),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // LCD Top Status Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    if (isShiftActive) {
                                        Text("S", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFFD97706))
                                    }
                                    if (isAlphaActive) {
                                        Text("A", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFFDC2626))
                                    }
                                    Text("M", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B).copy(alpha = 0.35f))
                                    Text(
                                        text = if (isRadianMode) "R" else "D",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text("Math ▲▼", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                                }

                                Surface(
                                    shape = RoundedCornerShape(3.dp),
                                    color = Color(0x22000000),
                                    modifier = Modifier.clickable { isRadianMode = !isRadianMode }
                                ) {
                                    Text(
                                        text = if (isRadianMode) "RAD [deg]" else "DEG [rad]",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0F172A),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            // Math Input Expression Line
                            Text(
                                text = if (expression.isEmpty()) "0" else expression,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                textAlign = TextAlign.Start,
                                maxLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Math Output Result Line
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = resultText,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF020617)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Upper Function Control Section with Authentic REPLAY Rocker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Shift & Alpha Keys
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    CasioSmallKey(
                        label = "SHIFT",
                        subLabel = "",
                        isActive = isShiftActive,
                        accentColor = Color(0xFFD97706),
                        onClick = { isShiftActive = !isShiftActive; isAlphaActive = false }
                    )
                    CasioSmallKey(
                        label = "ALPHA",
                        subLabel = "",
                        isActive = isAlphaActive,
                        accentColor = Color(0xFFDC2626),
                        onClick = { isAlphaActive = !isAlphaActive; isShiftActive = false }
                    )
                }

                // Center REPLAY Silver Rocker
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF94A3B8),
                    border = BorderStroke(2.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        IconButton(
                            onClick = {
                                if (engine.history.isNotEmpty()) {
                                    engine.historyIndex = (engine.historyIndex - 1 + engine.history.size) % engine.history.size
                                    expression = engine.history[engine.historyIndex].first
                                    resultText = engine.history[engine.historyIndex].second
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .size(20.dp)
                        ) {
                            Icon(Icons.Filled.ArrowDropUp, contentDescription = "UP", tint = Color(0xFF0F172A))
                        }
                        IconButton(
                            onClick = {
                                if (engine.history.isNotEmpty()) {
                                    engine.historyIndex = (engine.historyIndex + 1) % engine.history.size
                                    expression = engine.history[engine.historyIndex].first
                                    resultText = engine.history[engine.historyIndex].second
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .size(20.dp)
                        ) {
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = "DOWN", tint = Color(0xFF0F172A))
                        }
                        IconButton(
                            onClick = { if (expression.isNotEmpty()) expression = expression.dropLast(1) },
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .size(20.dp)
                        ) {
                            Icon(Icons.Filled.ArrowLeft, contentDescription = "LEFT", tint = Color(0xFF0F172A))
                        }
                        IconButton(
                            onClick = { /* right */ },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(20.dp)
                        ) {
                            Icon(Icons.Filled.ArrowRight, contentDescription = "RIGHT", tint = Color(0xFF0F172A))
                        }
                        Text(
                            text = "REPLAY",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                // Right Mode/Setup & ON Keys
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    CasioSmallKey(
                        label = "MODE",
                        subLabel = "SETUP",
                        onClick = { isRadianMode = !isRadianMode }
                    )
                    CasioSmallKey(
                        label = "ON",
                        subLabel = "",
                        accentColor = Color(0xFF16A34A),
                        onClick = { clearAll() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scientific Function Matrix (4 rows x 6 keys)
            val sciKeys = listOf(
                listOf(
                    ScientificKey("x!", "x⁻¹") { append("!") },
                    ScientificKey("³√", "√") { if (isShiftActive) append("cbrt(") else append("sqrt(") },
                    ScientificKey("x³", "x²") { if (isShiftActive) append("³") else append("²") },
                    ScientificKey("ʸ√x", "xʸ") { append("^") },
                    ScientificKey("10ˣ", "log") { if (isShiftActive) append("10^") else append("log(") },
                    ScientificKey("eˣ", "ln") { if (isShiftActive) append("e^") else append("ln(") }
                ),
                listOf(
                    ScientificKey("abs", "(-)") { append("-") },
                    ScientificKey("° ' \"", "° ' \"") { append("°") },
                    ScientificKey("hyp", "hyp") { append("sinh(") },
                    ScientificKey("sin⁻¹", "sin") { if (isShiftActive) append("asin(") else append("sin(") },
                    ScientificKey("cos⁻¹", "cos") { if (isShiftActive) append("acos(") else append("cos(") },
                    ScientificKey("tan⁻¹", "tan") { if (isShiftActive) append("atan(") else append("tan(") }
                ),
                listOf(
                    ScientificKey("STO", "RCL") { append("Ans") },
                    ScientificKey("←", "ENG") { append("×10^") },
                    ScientificKey("d/c", "(") { append("(") },
                    ScientificKey("%", ")") { append(")") },
                    ScientificKey("a b/c", "S<=>D") {
                        if (resultText.toDoubleOrNull() != null) {
                            val v = resultText.toDouble()
                            if (!showFractionResult) {
                                val frac = toFraction(v)
                                resultText = frac
                                showFractionResult = true
                            } else {
                                resultText = String.format(Locale.US, "%.6f", v).trimEnd('0').trimEnd('.')
                                showFractionResult = false
                            }
                        }
                    },
                    ScientificKey("M-", "M+") { engine.lastAnswer = resultText.toDoubleOrNull() ?: 0.0 }
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                sciKeys.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        row.forEach { k ->
                            CasioScientificKeyButton(
                                key = k,
                                isShift = isShiftActive,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Numeric & Arithmetic Keypad (Authentic fx-991ES Colors)
            val keypad = listOf(
                listOf(
                    PadButton("7", Color(0xFFF1F5F9), Color(0xFF0F172A)) { append("7") },
                    PadButton("8", Color(0xFFF1F5F9), Color(0xFF0F172A)) { append("8") },
                    PadButton("9", Color(0xFFF1F5F9), Color(0xFF0F172A)) { append("9") },
                    PadButton("DEL", Color(0xFFEA580C), Color.White) { backspace() }, // Real Casio Orange DEL
                    PadButton("AC", Color(0xFFDC2626), Color.White) { clearAll() }    // Real Casio Red AC
                ),
                listOf(
                    PadButton("4", Color(0xFFF1F5F9), Color(0xFF0F172A)) { append("4") },
                    PadButton("5", Color(0xFFF1F5F9), Color(0xFF0F172A)) { append("5") },
                    PadButton("6", Color(0xFFF1F5F9), Color(0xFF0F172A)) { append("6") },
                    PadButton("×", Color(0xFF334155), Color.White) { append("×") },
                    PadButton("÷", Color(0xFF334155), Color.White) { append("÷") }
                ),
                listOf(
                    PadButton("1", Color(0xFFF1F5F9), Color(0xFF0F172A)) { append("1") },
                    PadButton("2", Color(0xFFF1F5F9), Color(0xFF0F172A)) { append("2") },
                    PadButton("3", Color(0xFFF1F5F9), Color(0xFF0F172A)) { append("3") },
                    PadButton("+", Color(0xFF334155), Color.White) { append("+") },
                    PadButton("−", Color(0xFF334155), Color.White) { append("−") }
                ),
                listOf(
                    PadButton("0", Color(0xFFF1F5F9), Color(0xFF0F172A)) { append("0") },
                    PadButton(".", Color(0xFFF1F5F9), Color(0xFF0F172A)) { append(".") },
                    PadButton("×10ˣ", Color(0xFF334155), Color.White) { append("*10^") },
                    PadButton("Ans", Color(0xFF0284C7), Color.White) { append("Ans") },
                    PadButton("=", Color(0xFF1D4ED8), Color.White) { calculate() }
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                keypad.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        row.forEach { btn ->
                            Button(
                                onClick = btn.action,
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = btn.bgColor),
                                contentPadding = PaddingValues(0.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                            ) {
                                Text(
                                    text = btn.text,
                                    color = btn.textColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Action Bar: Insert to Board
            if (onInsertResultToBoard != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        val textToInsert = if (resultText != "0" && resultText != "Syntax ERROR") {
                            "$expression = $resultText"
                        } else {
                            expression.ifBlank { resultText }
                        }
                        onInsertResultToBoard(textToInsert)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                    Icon(Icons.Filled.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (L.isArabic()) "إدراج الناتج على السبورة 📋" else "Insert on Board 📋",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
}

data class ScientificKey(
    val shiftLabel: String,
    val primaryLabel: String,
    val onClick: () -> Unit
)

data class PadButton(
    val text: String,
    val bgColor: Color,
    val textColor: Color,
    val action: () -> Unit
)

@Composable
fun CasioSmallKey(
    label: String,
    subLabel: String,
    isActive: Boolean = false,
    accentColor: Color = Color(0xFF94A3B8),
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(44.dp)
    ) {
        if (subLabel.isNotBlank()) {
            Text(subLabel, fontSize = 7.sp, color = Color(0xFFCBD5E1))
        }
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (isActive) accentColor else Color(0xFF334155),
            border = BorderStroke(1.dp, if (isActive) Color.White else Color(0xFF64748B)),
            modifier = Modifier
                .width(42.dp)
                .height(22.dp)
                .clickable { onClick() }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = label,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) Color.White else accentColor
                )
            }
        }
    }
}

@Composable
fun CasioScientificKeyButton(
    key: ScientificKey,
    isShift: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = key.shiftLabel,
            fontSize = 7.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFF59E0B),
            maxLines = 1
        )
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color(0xFF1E293B),
            border = BorderStroke(1.dp, Color(0xFF475569)),
            shadowElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clickable { key.onClick() }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = key.primaryLabel,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

fun toFraction(x: Double): String {
    val eps = 1.0E-6
    var h1 = 1.0; var h2 = 0.0
    var k1 = 0.0; var k2 = 1.0
    var b = x
    do {
        val a = floor(b)
        var aux = h1
        h1 = a * h1 + h2
        h2 = aux
        aux = k1
        k1 = a * k1 + k2
        k2 = aux
        b = 1.0 / (b - a)
    } while (abs(x - h1 / k1) > x * eps && k1 < 1000)

    val num = h1.toLong()
    val den = k1.toLong()
    return if (den == 1L) "$num" else "$num/$den"
}
