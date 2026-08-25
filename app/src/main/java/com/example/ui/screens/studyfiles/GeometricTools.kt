package com.example.ui.screens.studyfiles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.*

enum class ActiveTool {
    SELECT,
    HAND,
    PEN,
    HIGHLIGHTER,
    LASER,
    ERASER,
    RULER,
    COMPASS,
    PROTRACTOR,
    SHAPE_2D,
    SHAPE_3D,
    VERTEX_EDITOR,
    STICKY_NOTE
}

enum class PenStyle(val title: String, val iconText: String, val description: String) {
    NORMAL("قلم جاف ناعم", "🖊️", "خط ناعم ودقيق للشرح والكتابة"),
    CALLIGRAPHY("قلم خط عربي مشطوف", "✒️", "خط فني عريض بزاوية هندسية"),
    HIGHLIGHTER("قلم تظليل فسفوري", "🖍️", "تظليل شفاف ومشرق لتحديد النصوص"),
    DASHED("قلم خط متقطع", "╌", "خطوط هندسية مساعدة ومتقطعة"),
    NEON_GLOW("قلم ليزري مضيء", "✨", "خط نيون مشع لإبراز النقاط الهامة"),
    DOUBLE_LINE("قلم خط مزدوج", "═", "خطين متوازيين للرسم والتأطير")
}

enum class Shape2DType(val title: String, val defaultVerticesCount: Int) {
    FREE_TRIANGLE("مثلث حر بالرؤوس 📐", 3),
    EQUILATERAL_TRIANGLE("مثلث متساوي الأضلاع 🔺", 3),
    RIGHT_TRIANGLE("مثلث قائم الزاوية 📐", 3),
    QUADRILATERAL("شكل رباعي بالرؤوس ▱", 4),
    RECTANGLE("مستطيل ▭", 4),
    SQUARE("مربع ⏹️", 4),
    PARALLELOGRAM("متوازي أضلاع ▰", 4),
    RHOMBUS("معين 🔷", 4),
    TRAPEZOID("شبه منحرف ⏢", 4),
    CIRCLE("دائرة ⭕", 2),
    ELLIPSE("شكل بيضاوي ⬭", 2),
    REGULAR_PENTAGON("خماسي منتظم ⬟", 5),
    REGULAR_HEXAGON("سداسي منتظم ⬡", 6),
    LINE_SEGMENT("قطعة مستقيمة ─", 2),
    ARROW_VECTOR("سهم متجه ➔", 2)
}

enum class Shape3DType(val title: String, val iconText: String) {
    CUBE("مكعب 3D", "🧊"),
    CUBOID("متوازي مستطيلات 3D", "📦"),
    CYLINDER("أسطوانة 3D", "🥫"),
    CONE("مخروط 3D", "🍦"),
    SPHERE("كرة فراغية 3D", "🌐"),
    SQUARE_PYRAMID("هرم رباعي القواعد 3D", "⛺"),
    TRIANGULAR_PRISM("منشور ثلاثي 3D", "📐"),
    OCTAHEDRON("مجسم ثماني السطوح 3D", "💎"),
    HEXAGONAL_PRISM("منشور سداسي 3D", "⬡"),
    TORUS("حلقة دائرية 3D", "🍩")
}

enum class BoardMode(val title: String) {
    PDF("ملف PDF 📄"),
    WHITEBOARD("سبورة بيضاء ⚪"),
    BLACKBOARD("سبورة سوداء ⚫"),
    GRID_GRAPH("ورق رسم بياني 📊"),
    RULED_LINES("دفتر مسطر 📝")
}

fun PenStyle.getLocalizedTitle(): String = when (com.example.util.LocaleManager.currentLanguage.value) {
    com.example.util.AppLanguage.ARABIC -> this.title
    com.example.util.AppLanguage.FRENCH -> when (this) {
        PenStyle.NORMAL -> "Stylo Fluide 🖊️"
        PenStyle.CALLIGRAPHY -> "Plume Calligraphie ✒️"
        PenStyle.HIGHLIGHTER -> "Surligneur 🖍️"
        PenStyle.DASHED -> "Ligne Pointillée ╌"
        PenStyle.NEON_GLOW -> "Lueur Néon ✨"
        PenStyle.DOUBLE_LINE -> "Double Ligne ═"
    }
    com.example.util.AppLanguage.ENGLISH -> when (this) {
        PenStyle.NORMAL -> "Smooth Pen 🖊️"
        PenStyle.CALLIGRAPHY -> "Calligraphy Pen ✒️"
        PenStyle.HIGHLIGHTER -> "Highlighter 🖍️"
        PenStyle.DASHED -> "Dashed Line ╌"
        PenStyle.NEON_GLOW -> "Neon Glow ✨"
        PenStyle.DOUBLE_LINE -> "Double Line ═"
    }
}

fun PenStyle.getLocalizedDescription(): String = when (com.example.util.LocaleManager.currentLanguage.value) {
    com.example.util.AppLanguage.ARABIC -> this.description
    com.example.util.AppLanguage.FRENCH -> when (this) {
        PenStyle.NORMAL -> "Trait fluide et précis pour une écriture nette"
        PenStyle.CALLIGRAPHY -> "Pointe biseautée pour calligraphie et schémas"
        PenStyle.HIGHLIGHTER -> "Surlignage fluorescent translucide"
        PenStyle.DASHED -> "Lignes directrices en pointillés pour analyse géométrique"
        PenStyle.NEON_GLOW -> "Lueur laser brillante pour mise en évidence"
        PenStyle.DOUBLE_LINE -> "Lignes parallèles pour encadrement et schémas"
    }
    com.example.util.AppLanguage.ENGLISH -> when (this) {
        PenStyle.NORMAL -> "Smooth precision line for clear writing"
        PenStyle.CALLIGRAPHY -> "Chiseled tip for calligraphy & drafting"
        PenStyle.HIGHLIGHTER -> "Translucent fluorescent text highlighter"
        PenStyle.DASHED -> "Dashed guidelines for geometric analysis"
        PenStyle.NEON_GLOW -> "Luminous laser glow for emphasis"
        PenStyle.DOUBLE_LINE -> "Parallel lines for framing & schematics"
    }
}

fun Shape2DType.getLocalizedTitle(): String = when (com.example.util.LocaleManager.currentLanguage.value) {
    com.example.util.AppLanguage.ARABIC -> this.title
    com.example.util.AppLanguage.FRENCH -> when (this) {
        Shape2DType.FREE_TRIANGLE -> "Triangle Libre 📐"
        Shape2DType.EQUILATERAL_TRIANGLE -> "Triangle Équilatéral 🔺"
        Shape2DType.RIGHT_TRIANGLE -> "Triangle Rectangle 📐"
        Shape2DType.QUADRILATERAL -> "Quadrilatère ▱"
        Shape2DType.RECTANGLE -> "Rectangle ▭"
        Shape2DType.SQUARE -> "Carré ⏹️"
        Shape2DType.PARALLELOGRAM -> "Parallélogramme ▰"
        Shape2DType.RHOMBUS -> "Losange 🔷"
        Shape2DType.TRAPEZOID -> "Trapèze ⏢"
        Shape2DType.CIRCLE -> "Cercle ⭕"
        Shape2DType.ELLIPSE -> "Ellipse ⬭"
        Shape2DType.REGULAR_PENTAGON -> "Pentagone Régulier ⬟"
        Shape2DType.REGULAR_HEXAGON -> "Hexagone Régulier ⬡"
        Shape2DType.LINE_SEGMENT -> "Segment de Droite ─"
        Shape2DType.ARROW_VECTOR -> "Vecteur / Flèche ➔"
    }
    com.example.util.AppLanguage.ENGLISH -> when (this) {
        Shape2DType.FREE_TRIANGLE -> "Free Vertex Triangle 📐"
        Shape2DType.EQUILATERAL_TRIANGLE -> "Equilateral Triangle 🔺"
        Shape2DType.RIGHT_TRIANGLE -> "Right Triangle 📐"
        Shape2DType.QUADRILATERAL -> "Quadrilateral ▱"
        Shape2DType.RECTANGLE -> "Rectangle ▭"
        Shape2DType.SQUARE -> "Square ⏹️"
        Shape2DType.PARALLELOGRAM -> "Parallelogram ▰"
        Shape2DType.RHOMBUS -> "Rhombus 🔷"
        Shape2DType.TRAPEZOID -> "Trapezoid ⏢"
        Shape2DType.CIRCLE -> "Circle ⭕"
        Shape2DType.ELLIPSE -> "Ellipse ⬭"
        Shape2DType.REGULAR_PENTAGON -> "Regular Pentagon ⬟"
        Shape2DType.REGULAR_HEXAGON -> "Regular Hexagon ⬡"
        Shape2DType.LINE_SEGMENT -> "Line Segment ─"
        Shape2DType.ARROW_VECTOR -> "Vector Arrow ➔"
    }
}

fun Shape3DType.getLocalizedTitle(): String = when (com.example.util.LocaleManager.currentLanguage.value) {
    com.example.util.AppLanguage.ARABIC -> this.title
    com.example.util.AppLanguage.FRENCH -> when (this) {
        Shape3DType.CUBE -> "Cube 3D 🧊"
        Shape3DType.CUBOID -> "Pavé Droit 3D 📦"
        Shape3DType.CYLINDER -> "Cylindre 3D 🥫"
        Shape3DType.CONE -> "Cône 3D 🍦"
        Shape3DType.SPHERE -> "Sphère 3D 🌐"
        Shape3DType.SQUARE_PYRAMID -> "Pyramide à Base Carrée ⛺"
        Shape3DType.TRIANGULAR_PRISM -> "Prisme Triangulaire 📐"
        Shape3DType.OCTAHEDRON -> "Octaèdre 3D 💎"
        Shape3DType.HEXAGONAL_PRISM -> "Prisme Hexagonal ⬡"
        Shape3DType.TORUS -> "Tore 3D 🍩"
    }
    com.example.util.AppLanguage.ENGLISH -> when (this) {
        Shape3DType.CUBE -> "3D Cube 🧊"
        Shape3DType.CUBOID -> "3D Cuboid 📦"
        Shape3DType.CYLINDER -> "3D Cylinder 🥫"
        Shape3DType.CONE -> "3D Cone 🍦"
        Shape3DType.SPHERE -> "3D Sphere 🌐"
        Shape3DType.SQUARE_PYRAMID -> "3D Square Pyramid ⛺"
        Shape3DType.TRIANGULAR_PRISM -> "3D Triangular Prism 📐"
        Shape3DType.OCTAHEDRON -> "3D Octahedron 💎"
        Shape3DType.HEXAGONAL_PRISM -> "3D Hexagonal Prism ⬡"
        Shape3DType.TORUS -> "3D Torus 🍩"
    }
}

fun BoardMode.getLocalizedTitle(): String = when (com.example.util.LocaleManager.currentLanguage.value) {
    com.example.util.AppLanguage.ARABIC -> this.title
    com.example.util.AppLanguage.FRENCH -> when (this) {
        BoardMode.PDF -> "Document PDF 📄"
        BoardMode.WHITEBOARD -> "Tableau Blanc ⚪"
        BoardMode.BLACKBOARD -> "Tableau Noir ⚫"
        BoardMode.GRID_GRAPH -> "Papier Millimétré 📊"
        BoardMode.RULED_LINES -> "Cahier Ligné 📝"
    }
    com.example.util.AppLanguage.ENGLISH -> when (this) {
        BoardMode.PDF -> "PDF Document 📄"
        BoardMode.WHITEBOARD -> "Whiteboard ⚪"
        BoardMode.BLACKBOARD -> "Blackboard ⚫"
        BoardMode.GRID_GRAPH -> "Grid Graph 📊"
        BoardMode.RULED_LINES -> "Ruled Notebook 📝"
    }
}

fun ActiveTool.getLocalizedTitle(): String = when (com.example.util.LocaleManager.currentLanguage.value) {
    com.example.util.AppLanguage.ARABIC -> when (this) {
        ActiveTool.SELECT -> "تحديد وتحريك"
        ActiveTool.HAND -> "تحريك وتكبير"
        ActiveTool.PEN -> "قلم"
        ActiveTool.HIGHLIGHTER -> "تظليل"
        ActiveTool.LASER -> "ليزر"
        ActiveTool.ERASER -> "ممحاة"
        ActiveTool.RULER -> "مسطرة"
        ActiveTool.COMPASS -> "برجل"
        ActiveTool.PROTRACTOR -> "منقلة"
        ActiveTool.SHAPE_2D -> "أشكال 2D"
        ActiveTool.SHAPE_3D -> "مجسمات 3D"
        ActiveTool.VERTEX_EDITOR -> "تعديل الرؤوس"
        ActiveTool.STICKY_NOTE -> "ملاحظة"
    }
    com.example.util.AppLanguage.FRENCH -> when (this) {
        ActiveTool.SELECT -> "Sélectionner & Déplacer"
        ActiveTool.HAND -> "Déplacer & Zoomer"
        ActiveTool.PEN -> "Stylo"
        ActiveTool.HIGHLIGHTER -> "Surligneur"
        ActiveTool.LASER -> "Laser"
        ActiveTool.ERASER -> "Gomme"
        ActiveTool.RULER -> "Règle"
        ActiveTool.COMPASS -> "Compas"
        ActiveTool.PROTRACTOR -> "Rapporteur"
        ActiveTool.SHAPE_2D -> "Formes 2D"
        ActiveTool.SHAPE_3D -> "Solides 3D"
        ActiveTool.VERTEX_EDITOR -> "Modifier Sommets"
        ActiveTool.STICKY_NOTE -> "Note Adhésive"
    }
    com.example.util.AppLanguage.ENGLISH -> when (this) {
        ActiveTool.SELECT -> "Select & Move"
        ActiveTool.HAND -> "Move & Zoom"
        ActiveTool.PEN -> "Pen"
        ActiveTool.HIGHLIGHTER -> "Highlighter"
        ActiveTool.LASER -> "Laser"
        ActiveTool.ERASER -> "Eraser"
        ActiveTool.RULER -> "Ruler"
        ActiveTool.COMPASS -> "Compass"
        ActiveTool.PROTRACTOR -> "Protractor"
        ActiveTool.SHAPE_2D -> "2D Shapes"
        ActiveTool.SHAPE_3D -> "3D Shapes"
        ActiveTool.VERTEX_EDITOR -> "Edit Vertices"
        ActiveTool.STICKY_NOTE -> "Sticky Note"
    }
}

data class VertexShapeState(
    val type: Shape2DType,
    val vertices: List<Offset>,
    val color: Color = Color(0xFF2563EB),
    val strokeWidth: Float = 4f,
    val isFilled: Boolean = false,
    val showMeasurements: Boolean = true
)

data class DrawStroke(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float,
    val penStyle: PenStyle = PenStyle.NORMAL,
    val isFilled: Boolean = false,
    val shapePath: Path? = null,
    val shape3DType: Shape3DType? = null,
    val shape3DCenter: Offset? = null,
    val shape3DSize: Float = 140f,
    val shape2DVertices: List<Offset>? = null,
    val shape2DType: Shape2DType? = null
)

object GeometricRenderer {

    /**
     * Renders an interactive 2D shape with connected vertices
     */
    fun drawVertexShape(
        drawScope: DrawScope,
        shapeState: VertexShapeState
    ) {
        val vertices = shapeState.vertices
        if (vertices.size < 2) return

        val color = shapeState.color
        val strokeWidth = shapeState.strokeWidth
        val isFilled = shapeState.isFilled

        when (shapeState.type) {
            Shape2DType.CIRCLE -> {
                val center = vertices[0]
                val edge = vertices[1]
                val radius = (edge - center).getDistance()
                if (isFilled) {
                    drawScope.drawCircle(color = color.copy(alpha = 0.2f), radius = radius, center = center, style = Fill)
                }
                drawScope.drawCircle(color = color, radius = radius, center = center, style = Stroke(width = strokeWidth))
                // Radius indicator
                if (shapeState.showMeasurements) {
                    drawScope.drawLine(color.copy(alpha = 0.6f), center, edge, strokeWidth = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
                }
                return
            }
            Shape2DType.ELLIPSE -> {
                val center = vertices[0]
                val edge = vertices[1]
                val rx = abs(edge.x - center.x).coerceAtLeast(20f)
                val ry = abs(edge.y - center.y).coerceAtLeast(20f)
                if (isFilled) {
                    drawScope.drawOval(color = color.copy(alpha = 0.2f), topLeft = Offset(center.x - rx, center.y - ry), size = Size(rx * 2, ry * 2), style = Fill)
                }
                drawScope.drawOval(color = color, topLeft = Offset(center.x - rx, center.y - ry), size = Size(rx * 2, ry * 2), style = Stroke(width = strokeWidth))
                return
            }
            Shape2DType.LINE_SEGMENT -> {
                drawScope.drawLine(color, vertices[0], vertices[1], strokeWidth = strokeWidth)
                return
            }
            Shape2DType.ARROW_VECTOR -> {
                val start = vertices[0]
                val end = vertices[1]
                drawScope.drawLine(color, start, end, strokeWidth = strokeWidth)
                // Draw Arrow Head
                val angle = atan2(end.y - start.y, end.x - start.x)
                val arrowHeadLen = (strokeWidth * 4.5f).coerceAtLeast(20f)
                val arrowAngle = Math.toRadians(30.0)

                val x1 = end.x - arrowHeadLen * cos(angle - arrowAngle).toFloat()
                val y1 = end.y - arrowHeadLen * sin(angle - arrowAngle).toFloat()
                val x2 = end.x - arrowHeadLen * cos(angle + arrowAngle).toFloat()
                val y2 = end.y - arrowHeadLen * sin(angle + arrowAngle).toFloat()

                val arrowPath = Path().apply {
                    moveTo(end.x, end.y)
                    lineTo(x1, y1)
                    lineTo(x2, y2)
                    close()
                }
                drawScope.drawPath(arrowPath, color, style = Fill)
                return
            }
            else -> {
                // Polygon / Triangle / Quadrilateral
                val path = Path().apply {
                    moveTo(vertices[0].x, vertices[0].y)
                    for (i in 1 until vertices.size) {
                        lineTo(vertices[i].x, vertices[i].y)
                    }
                    close()
                }

                if (isFilled) {
                    drawScope.drawPath(path, color.copy(alpha = 0.2f), style = Fill)
                }
                drawScope.drawPath(path, color, style = Stroke(width = strokeWidth))
            }
        }
    }

    /**
     * Renders Realistic 3D Shapes with Isometric Lighting and Hidden Dashed Lines
     */
    fun drawRealistic3DShape(
        drawScope: DrawScope,
        shape: Shape3DType,
        center: Offset,
        size: Float,
        baseColor: Color,
        strokeWidth: Float
    ) {
        val half = size / 2f
        val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

        when (shape) {
            Shape3DType.CUBE -> {
                val s = size * 0.7f
                val d = s * 0.45f // isometric depth
                val dx = d * 0.866f
                val dy = d * 0.5f

                val x = center.x - s / 2f
                val y = center.y - s / 2f + dy / 2f

                // Front face: (x, y+dy) -> (x+s, y+dy) -> (x+s, y+s+dy) -> (x, y+s+dy)
                val f1 = Offset(x, y + dy)
                val f2 = Offset(x + s, y + dy)
                val f3 = Offset(x + s, y + s + dy)
                val f4 = Offset(x, y + s + dy)

                // Top face back vertices:
                val t1 = Offset(x + dx, y)
                val t2 = Offset(x + s + dx, y)

                // Right face back vertex:
                val r3 = Offset(x + s + dx, y + s)

                // Hidden inner vertex:
                val h1 = Offset(x + dx, y + s)

                // Shaded Top Face
                val topPath = Path().apply {
                    moveTo(f1.x, f1.y); lineTo(t1.x, t1.y); lineTo(t2.x, t2.y); lineTo(f2.x, f2.y); close()
                }
                drawScope.drawPath(topPath, baseColor.copy(alpha = 0.35f), style = Fill)
                drawScope.drawPath(topPath, baseColor, style = Stroke(strokeWidth))

                // Shaded Right Face
                val rightPath = Path().apply {
                    moveTo(f2.x, f2.y); lineTo(t2.x, t2.y); lineTo(r3.x, r3.y); lineTo(f3.x, f3.y); close()
                }
                drawScope.drawPath(rightPath, baseColor.copy(alpha = 0.20f), style = Fill)
                drawScope.drawPath(rightPath, baseColor, style = Stroke(strokeWidth))

                // Shaded Front Face
                val frontPath = Path().apply {
                    moveTo(f1.x, f1.y); lineTo(f2.x, f2.y); lineTo(f3.x, f3.y); lineTo(f4.x, f4.y); close()
                }
                drawScope.drawPath(frontPath, baseColor.copy(alpha = 0.10f), style = Fill)
                drawScope.drawPath(frontPath, baseColor, style = Stroke(strokeWidth))

                // Hidden dashed back lines
                drawScope.drawLine(baseColor.copy(alpha = 0.5f), f4, h1, strokeWidth, pathEffect = dashedEffect)
                drawScope.drawLine(baseColor.copy(alpha = 0.5f), t1, h1, strokeWidth, pathEffect = dashedEffect)
                drawScope.drawLine(baseColor.copy(alpha = 0.5f), r3, h1, strokeWidth, pathEffect = dashedEffect)
            }

            Shape3DType.CUBOID -> {
                val w = size * 1.0f
                val h = size * 0.6f
                val d = size * 0.35f
                val dx = d * 0.866f
                val dy = d * 0.5f

                val x = center.x - w / 2f
                val y = center.y - h / 2f + dy / 2f

                val f1 = Offset(x, y + dy)
                val f2 = Offset(x + w, y + dy)
                val f3 = Offset(x + w, y + h + dy)
                val f4 = Offset(x, y + h + dy)

                val t1 = Offset(x + dx, y)
                val t2 = Offset(x + w + dx, y)
                val r3 = Offset(x + w + dx, y + h)
                val h1 = Offset(x + dx, y + h)

                // Top Face
                val topPath = Path().apply {
                    moveTo(f1.x, f1.y); lineTo(t1.x, t1.y); lineTo(t2.x, t2.y); lineTo(f2.x, f2.y); close()
                }
                drawScope.drawPath(topPath, baseColor.copy(alpha = 0.35f), style = Fill)
                drawScope.drawPath(topPath, baseColor, style = Stroke(strokeWidth))

                // Right Face
                val rightPath = Path().apply {
                    moveTo(f2.x, f2.y); lineTo(t2.x, t2.y); lineTo(r3.x, r3.y); lineTo(f3.x, f3.y); close()
                }
                drawScope.drawPath(rightPath, baseColor.copy(alpha = 0.20f), style = Fill)
                drawScope.drawPath(rightPath, baseColor, style = Stroke(strokeWidth))

                // Front Face
                val frontPath = Path().apply {
                    moveTo(f1.x, f1.y); lineTo(f2.x, f2.y); lineTo(f3.x, f3.y); lineTo(f4.x, f4.y); close()
                }
                drawScope.drawPath(frontPath, baseColor.copy(alpha = 0.10f), style = Fill)
                drawScope.drawPath(frontPath, baseColor, style = Stroke(strokeWidth))

                // Dashed back
                drawScope.drawLine(baseColor.copy(alpha = 0.5f), f4, h1, strokeWidth, pathEffect = dashedEffect)
                drawScope.drawLine(baseColor.copy(alpha = 0.5f), t1, h1, strokeWidth, pathEffect = dashedEffect)
                drawScope.drawLine(baseColor.copy(alpha = 0.5f), r3, h1, strokeWidth, pathEffect = dashedEffect)
            }

            Shape3DType.CYLINDER -> {
                val w = size * 0.85f
                val h = size * 1.1f
                val rx = w / 2f
                val ry = rx * 0.35f

                val topY = center.y - h / 2f
                val botY = center.y + h / 2f - ry * 2

                // Top Base Ellipse
                drawScope.drawOval(baseColor.copy(alpha = 0.25f), topLeft = Offset(center.x - rx, topY), size = Size(w, ry * 2), style = Fill)
                drawScope.drawOval(baseColor, topLeft = Offset(center.x - rx, topY), size = Size(w, ry * 2), style = Stroke(strokeWidth))

                // Lateral Shaded Body
                val bodyPath = Path().apply {
                    moveTo(center.x - rx, topY + ry)
                    lineTo(center.x - rx, botY + ry)
                    arcTo(Rect(center.x - rx, botY, center.x + rx, botY + ry * 2), 180f, -180f, false)
                    lineTo(center.x + rx, topY + ry)
                    arcTo(Rect(center.x - rx, topY, center.x + rx, topY + ry * 2), 0f, 180f, false)
                    close()
                }
                drawScope.drawPath(bodyPath, baseColor.copy(alpha = 0.15f), style = Fill)

                // Visible Front Half of Bottom Ellipse
                drawScope.drawArc(baseColor, 0f, 180f, false, topLeft = Offset(center.x - rx, botY), size = Size(w, ry * 2), style = Stroke(strokeWidth))
                // Hidden Dashed Back Half of Bottom Ellipse
                drawScope.drawArc(baseColor.copy(alpha = 0.5f), 180f, 180f, false, topLeft = Offset(center.x - rx, botY), size = Size(w, ry * 2), style = Stroke(strokeWidth, pathEffect = dashedEffect))

                // Side tangent lines
                drawScope.drawLine(baseColor, Offset(center.x - rx, topY + ry), Offset(center.x - rx, botY + ry), strokeWidth)
                drawScope.drawLine(baseColor, Offset(center.x + rx, topY + ry), Offset(center.x + rx, botY + ry), strokeWidth)

                // Height Axis (Dashed)
                drawScope.drawLine(baseColor.copy(alpha = 0.6f), Offset(center.x, topY + ry), Offset(center.x, botY + ry), strokeWidth = 1.5f, pathEffect = dashedEffect)
            }

            Shape3DType.CONE -> {
                val w = size * 0.9f
                val h = size * 1.2f
                val rx = w / 2f
                val ry = rx * 0.35f
                val apex = Offset(center.x, center.y - h / 2f)
                val botY = center.y + h / 2f - ry * 2

                // Base Fill & Arc
                drawScope.drawArc(baseColor.copy(alpha = 0.15f), 0f, 180f, true, topLeft = Offset(center.x - rx, botY), size = Size(w, ry * 2), style = Fill)
                drawScope.drawArc(baseColor, 0f, 180f, false, topLeft = Offset(center.x - rx, botY), size = Size(w, ry * 2), style = Stroke(strokeWidth))
                drawScope.drawArc(baseColor.copy(alpha = 0.5f), 180f, 180f, false, topLeft = Offset(center.x - rx, botY), size = Size(w, ry * 2), style = Stroke(strokeWidth, pathEffect = dashedEffect))

                // Lateral Cone Body
                val conePath = Path().apply {
                    moveTo(apex.x, apex.y)
                    lineTo(center.x - rx, botY + ry)
                    arcTo(Rect(center.x - rx, botY, center.x + rx, botY + ry * 2), 180f, -180f, false)
                    close()
                }
                drawScope.drawPath(conePath, baseColor.copy(alpha = 0.12f), style = Fill)

                // Outer Rays
                drawScope.drawLine(baseColor, apex, Offset(center.x - rx, botY + ry), strokeWidth)
                drawScope.drawLine(baseColor, apex, Offset(center.x + rx, botY + ry), strokeWidth)

                // Altitude & Radius (Dashed)
                val centerBase = Offset(center.x, botY + ry)
                drawScope.drawLine(baseColor.copy(alpha = 0.7f), apex, centerBase, strokeWidth = 1.5f, pathEffect = dashedEffect)
                drawScope.drawLine(baseColor.copy(alpha = 0.7f), centerBase, Offset(center.x + rx, botY + ry), strokeWidth = 1.5f, pathEffect = dashedEffect)
            }

            Shape3DType.SPHERE -> {
                val radius = half * 0.95f
                drawScope.drawCircle(baseColor.copy(alpha = 0.12f), radius = radius, center = center, style = Fill)
                drawScope.drawCircle(baseColor, radius = radius, center = center, style = Stroke(strokeWidth))

                // Equator Ellipse
                drawScope.drawArc(baseColor, 0f, 180f, false, topLeft = Offset(center.x - radius, center.y - radius * 0.3f), size = Size(radius * 2, radius * 0.6f), style = Stroke(strokeWidth))
                drawScope.drawArc(baseColor.copy(alpha = 0.5f), 180f, 180f, false, topLeft = Offset(center.x - radius, center.y - radius * 0.3f), size = Size(radius * 2, radius * 0.6f), style = Stroke(strokeWidth, pathEffect = dashedEffect))

                // Meridian Ellipse
                drawScope.drawArc(baseColor, 90f, 180f, false, topLeft = Offset(center.x - radius * 0.3f, center.y - radius), size = Size(radius * 0.6f, radius * 2), style = Stroke(strokeWidth))
                drawScope.drawArc(baseColor.copy(alpha = 0.5f), 270f, 180f, false, topLeft = Offset(center.x - radius * 0.3f, center.y - radius), size = Size(radius * 0.6f, radius * 2), style = Stroke(strokeWidth, pathEffect = dashedEffect))

                // Center Point & Radius
                drawScope.drawCircle(baseColor, radius = 3f, center = center, style = Fill)
                drawScope.drawLine(baseColor.copy(alpha = 0.7f), center, Offset(center.x + radius, center.y), strokeWidth = 1.5f, pathEffect = dashedEffect)
            }

            Shape3DType.SQUARE_PYRAMID -> {
                val baseW = size * 0.9f
                val baseH = size * 0.4f
                val height = size * 1.0f
                val top = Offset(center.x, center.y - height / 2f)

                val b1 = Offset(center.x - baseW / 2f, center.y + height / 2f)
                val b2 = Offset(center.x + baseW / 2f, center.y + height / 2f)
                val b3 = Offset(center.x + baseW * 0.25f, center.y + height / 2f - baseH)
                val b4 = Offset(center.x - baseW * 0.25f, center.y + height / 2f - baseH)

                // Shaded Faces
                val frontFace = Path().apply { moveTo(top.x, top.y); lineTo(b1.x, b1.y); lineTo(b2.x, b2.y); close() }
                drawScope.drawPath(frontFace, baseColor.copy(alpha = 0.25f), style = Fill)

                val rightFace = Path().apply { moveTo(top.x, top.y); lineTo(b2.x, b2.y); lineTo(b3.x, b3.y); close() }
                drawScope.drawPath(rightFace, baseColor.copy(alpha = 0.15f), style = Fill)

                // Visible Outer Edges
                drawScope.drawLine(baseColor, top, b1, strokeWidth)
                drawScope.drawLine(baseColor, top, b2, strokeWidth)
                drawScope.drawLine(baseColor, top, b3, strokeWidth)
                drawScope.drawLine(baseColor, b1, b2, strokeWidth)
                drawScope.drawLine(baseColor, b2, b3, strokeWidth)

                // Hidden Dashed Edges
                drawScope.drawLine(baseColor.copy(alpha = 0.5f), top, b4, strokeWidth, pathEffect = dashedEffect)
                drawScope.drawLine(baseColor.copy(alpha = 0.5f), b1, b4, strokeWidth, pathEffect = dashedEffect)
                drawScope.drawLine(baseColor.copy(alpha = 0.5f), b3, b4, strokeWidth, pathEffect = dashedEffect)
            }

            Shape3DType.TRIANGULAR_PRISM -> {
                val w = size * 0.85f
                val h = size * 0.85f
                val d = size * 0.45f

                val p1 = Offset(center.x - w / 2f, center.y + h / 2f)
                val p2 = Offset(center.x, center.y - h / 2f)
                val p3 = Offset(center.x + w / 2f - d * 0.7f, center.y + h / 2f)

                val p1Back = Offset(p1.x + d, p1.y - d * 0.5f)
                val p2Back = Offset(p2.x + d, p2.y - d * 0.5f)
                val p3Back = Offset(p3.x + d, p3.y - d * 0.5f)

                // Shaded Faces
                val topSlope = Path().apply {
                    moveTo(p2.x, p2.y); lineTo(p2Back.x, p2Back.y); lineTo(p3Back.x, p3Back.y); lineTo(p3.x, p3.y); close()
                }
                drawScope.drawPath(topSlope, baseColor.copy(alpha = 0.25f), style = Fill)
                drawScope.drawPath(topSlope, baseColor, style = Stroke(strokeWidth))

                val frontTri = Path().apply {
                    moveTo(p1.x, p1.y); lineTo(p2.x, p2.y); lineTo(p3.x, p3.y); close()
                }
                drawScope.drawPath(frontTri, baseColor.copy(alpha = 0.15f), style = Fill)
                drawScope.drawPath(frontTri, baseColor, style = Stroke(strokeWidth))

                // Outer connectors
                drawScope.drawLine(baseColor, p1, p3, strokeWidth)
                drawScope.drawLine(baseColor, p2Back, p3Back, strokeWidth)
                drawScope.drawLine(baseColor, p3, p3Back, strokeWidth)

                // Dashed inner lines
                drawScope.drawLine(baseColor.copy(alpha = 0.5f), p1, p1Back, strokeWidth, pathEffect = dashedEffect)
                drawScope.drawLine(baseColor.copy(alpha = 0.5f), p2Back, p1Back, strokeWidth, pathEffect = dashedEffect)
                drawScope.drawLine(baseColor.copy(alpha = 0.5f), p3Back, p1Back, strokeWidth, pathEffect = dashedEffect)
            }

            Shape3DType.OCTAHEDRON -> {
                val top = Offset(center.x, center.y - half * 1.1f)
                val bottom = Offset(center.x, center.y + half * 1.1f)
                val left = Offset(center.x - half * 0.95f, center.y)
                val right = Offset(center.x + half * 0.95f, center.y)
                val front = Offset(center.x, center.y + half * 0.45f)
                val back = Offset(center.x, center.y - half * 0.45f)

                // Top Front Faces
                val topFrontLeft = Path().apply { moveTo(top.x, top.y); lineTo(left.x, left.y); lineTo(front.x, front.y); close() }
                drawScope.drawPath(topFrontLeft, baseColor.copy(alpha = 0.28f), style = Fill)
                val topFrontRight = Path().apply { moveTo(top.x, top.y); lineTo(right.x, right.y); lineTo(front.x, front.y); close() }
                drawScope.drawPath(topFrontRight, baseColor.copy(alpha = 0.18f), style = Fill)

                // Bottom Front Faces
                val botFrontLeft = Path().apply { moveTo(bottom.x, bottom.y); lineTo(left.x, left.y); lineTo(front.x, front.y); close() }
                drawScope.drawPath(botFrontLeft, baseColor.copy(alpha = 0.22f), style = Fill)
                val botFrontRight = Path().apply { moveTo(bottom.x, bottom.y); lineTo(right.x, right.y); lineTo(front.x, front.y); close() }
                drawScope.drawPath(botFrontRight, baseColor.copy(alpha = 0.12f), style = Fill)

                // Visible Outer & Front Edges
                drawScope.drawLine(baseColor, top, left, strokeWidth)
                drawScope.drawLine(baseColor, top, right, strokeWidth)
                drawScope.drawLine(baseColor, top, front, strokeWidth)
                drawScope.drawLine(baseColor, bottom, left, strokeWidth)
                drawScope.drawLine(baseColor, bottom, right, strokeWidth)
                drawScope.drawLine(baseColor, bottom, front, strokeWidth)
                drawScope.drawLine(baseColor, left, front, strokeWidth)
                drawScope.drawLine(baseColor, right, front, strokeWidth)

                // Hidden Dashed Back Edges
                drawScope.drawLine(baseColor.copy(alpha = 0.5f), top, back, strokeWidth, pathEffect = dashedEffect)
                drawScope.drawLine(baseColor.copy(alpha = 0.5f), bottom, back, strokeWidth, pathEffect = dashedEffect)
                drawScope.drawLine(baseColor.copy(alpha = 0.5f), left, back, strokeWidth, pathEffect = dashedEffect)
                drawScope.drawLine(baseColor.copy(alpha = 0.5f), right, back, strokeWidth, pathEffect = dashedEffect)
            }

            Shape3DType.HEXAGONAL_PRISM -> {
                val h = size * 0.9f
                val r = size * 0.48f
                val topCenter = Offset(center.x, center.y - h / 2.2f)
                val botCenter = Offset(center.x, center.y + h / 2.2f)

                val topPts = (0 until 6).map { i ->
                    val angle = Math.toRadians(i * 60.0 - 30.0)
                    Offset(topCenter.x + (r * cos(angle)).toFloat(), topCenter.y + (r * 0.55f * sin(angle)).toFloat())
                }
                val botPts = (0 until 6).map { i ->
                    val angle = Math.toRadians(i * 60.0 - 30.0)
                    Offset(botCenter.x + (r * cos(angle)).toFloat(), botCenter.y + (r * 0.55f * sin(angle)).toFloat())
                }

                // Shaded Top Hexagon
                val topHex = Path().apply {
                    moveTo(topPts[0].x, topPts[0].y)
                    for (i in 1 until 6) lineTo(topPts[i].x, topPts[i].y)
                    close()
                }
                drawScope.drawPath(topHex, baseColor.copy(alpha = 0.35f), style = Fill)
                drawScope.drawPath(topHex, baseColor, style = Stroke(strokeWidth))

                // Front Vertical Faces
                for (i in 0..2) {
                    val next = (i + 1) % 6
                    val face = Path().apply {
                        moveTo(topPts[i].x, topPts[i].y)
                        lineTo(topPts[next].x, topPts[next].y)
                        lineTo(botPts[next].x, botPts[next].y)
                        lineTo(botPts[i].x, botPts[i].y)
                        close()
                    }
                    drawScope.drawPath(face, baseColor.copy(alpha = 0.15f + i * 0.06f), style = Fill)
                    drawScope.drawPath(face, baseColor, style = Stroke(strokeWidth))
                }

                // Visible Bottom Half
                for (i in 0..2) {
                    val next = (i + 1) % 6
                    drawScope.drawLine(baseColor, botPts[i], botPts[next], strokeWidth)
                }

                // Hidden Dashed Back Edges
                for (i in 3..5) {
                    val next = (i + 1) % 6
                    drawScope.drawLine(baseColor.copy(alpha = 0.5f), botPts[i], botPts[next], strokeWidth, pathEffect = dashedEffect)
                    drawScope.drawLine(baseColor.copy(alpha = 0.5f), topPts[i], botPts[i], strokeWidth, pathEffect = dashedEffect)
                }
            }

            Shape3DType.TORUS -> {
                val rOuter = half * 0.9f
                val rInner = half * 0.45f
                drawScope.drawCircle(baseColor.copy(alpha = 0.15f), radius = rOuter, center = center, style = Fill)
                drawScope.drawCircle(baseColor, radius = rOuter, center = center, style = Stroke(strokeWidth))
                drawScope.drawCircle(Color.Transparent, radius = rInner, center = center, style = Fill)
                drawScope.drawCircle(baseColor, radius = rInner, center = center, style = Stroke(strokeWidth))
                // 3D Inner curve lines
                drawScope.drawArc(baseColor.copy(alpha = 0.6f), 0f, 180f, false, topLeft = Offset(center.x - rOuter, center.y - rOuter * 0.25f), size = Size(rOuter * 2, rOuter * 0.5f), style = Stroke(width = 1.5f, pathEffect = dashedEffect))
            }
        }
    }
}
