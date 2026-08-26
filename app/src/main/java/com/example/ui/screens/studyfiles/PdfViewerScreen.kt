package com.example.ui.screens.studyfiles

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppDatabase
import com.example.data.local.entity.StudyFileEntity
import com.example.data.repository.TeacherPlannerRepository
import com.example.ui.theme.*
import com.example.util.PdfImageFormat
import com.example.util.PdfImageQuality
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.util.WatermarkConfig
import com.example.util.WatermarkPosition
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.util.PdfImageExtractorHelper
import com.example.util.StudyFileManager
import com.example.util.MovableShapeItem
import com.example.util.MovableTextItem
import com.example.util.StudyFileAnnotationStore
import com.example.util.L
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.*

/**
 * Data class for Sticky Notes placed on the whiteboard/PDF
 */
data class StickyNoteItem(
    val id: String = UUID.randomUUID().toString(),
    var offset: Offset,
    var text: String,
    var color: Color = Color(0xFFFEF08A), // Light pastel yellow
    var fontSize: Float = 12f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    filePath: String,
    title: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember {
        TeacherPlannerRepository(AppDatabase.getInstance(context.applicationContext))
    }

    // Board & PDF State
    var boardMode by remember {
        mutableStateOf(if (filePath.isNotBlank() && filePath.endsWith(".pdf", ignoreCase = true)) BoardMode.PDF else BoardMode.WHITEBOARD)
    }
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var fileDescriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var totalPages by remember { mutableStateOf(1) }
    var currentPageIndex by remember { mutableStateOf(0) }
    var currentPageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isLoadingPdf by remember { mutableStateOf(false) }
    var isSavingChanges by remember { mutableStateOf(false) }

    // Image Extraction & Conversion Dialog States
    var showExportPageImageDialog by remember { mutableStateOf(false) }
    var showConvertFullPdfDialog by remember { mutableStateOf(false) }
    var showQuickPageThumbnailSheet by remember { mutableStateOf(false) }
    var isExportingImage by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableStateOf(0f) }
    var exportStatusText by remember { mutableStateOf("") }
    var extractedImageFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var extractedZipFile by remember { mutableStateOf<File?>(null) }

    // myViewBoard Canvas Zoom & Pan State
    var zoomScale by remember { mutableStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Active Drawing Settings
    var activeTool by remember { mutableStateOf(ActiveTool.PEN) }
    var activePenStyle by remember { mutableStateOf(PenStyle.NORMAL) }
    var selectedColor by remember { mutableStateOf(Color(0xFF1E3A8A)) } // Navy Blue
    var strokeWidth by remember { mutableStateOf(6f) }
    var strokeOpacity by remember { mutableStateOf(1.0f) }
    var isFilledShape by remember { mutableStateOf(false) }

    // UI Modes & Toggles
    var isFullscreen by remember { mutableStateOf(false) }
    var showQuickPenPopover by remember { mutableStateOf(false) }
    var showQuickHighlighterPopover by remember { mutableStateOf(false) }
    var showToolsDropdown by remember { mutableStateOf(false) }
    var showStrokeWidthDialog by remember { mutableStateOf(false) }
    var showGeometricToolsSheet by remember { mutableStateOf(false) }
    var show2DShapesSheet by remember { mutableStateOf(false) }
    var show3DShapesSheet by remember { mutableStateOf(false) }
    var showBoardModeSheet by remember { mutableStateOf(false) }
    var showHomeworkDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showAddTextDialog by remember { mutableStateOf(false) }
    var showJumpToPageDialog by remember { mutableStateOf(false) }

    // 2D & 3D Selected Types
    var selected2DShape by remember { mutableStateOf(Shape2DType.FREE_TRIANGLE) }
    var selected3DShape by remember { mutableStateOf(Shape3DType.CUBE) }
    var shape3DSize by remember { mutableStateOf(160f) }

    // Strokes, Laser & Redo Stack
    val strokes = remember { mutableStateListOf<DrawStroke>() }
    val redoStrokes = remember { mutableStateListOf<DrawStroke>() }
    var currentPathPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    val laserPoints = remember { mutableStateListOf<Pair<Offset, Long>>() }
    val stickyNotes = remember { mutableStateListOf<StickyNoteItem>() }
    val movableShapes = remember { mutableStateListOf<MovableShapeItem>() }
    val movableTexts = remember { mutableStateListOf<MovableTextItem>() }
    var selectedMovableShapeId by remember { mutableStateOf<String?>(null) }
    var selectedMovableTextId by remember { mutableStateOf<String?>(null) }

    // Realistic Tool Overlays States
    var isRulerVisible by remember { mutableStateOf(false) }
    var rulerOffset by remember { mutableStateOf(Offset(140f, 320f)) }
    var rulerAngle by remember { mutableStateOf(0f) }
    var rulerLengthCm by remember { mutableStateOf(15f) }

    var isProtractorVisible by remember { mutableStateOf(false) }
    var protractorCenter by remember { mutableStateOf(Offset(260f, 420f)) }
    var protractorBaseAngle by remember { mutableStateOf(0f) }
    var protractorTargetAngle by remember { mutableStateOf(60f) }

    var isCompassVisible by remember { mutableStateOf(false) }
    var compassCenter by remember { mutableStateOf(Offset(260f, 460f)) }
    var compassRadiusPx by remember { mutableStateOf(140f) }

    var isSetSquareVisible by remember { mutableStateOf(false) }
    var setSquareOffset by remember { mutableStateOf(Offset(220f, 380f)) }
    var setSquareAngle by remember { mutableStateOf(0f) }

    // Casio fx-991ES PLUS Scientific Calculator
    var isScientificCalculatorVisible by remember { mutableStateOf(false) }
    var calculatorOffset by remember { mutableStateOf(Offset(60f, 180f)) }

    // 2D Vertex Manipulation Active State
    var isVertexEditorActive by remember { mutableStateOf(false) }
    var activeVertexShape by remember {
        mutableStateOf(
            VertexShapeState(
                type = Shape2DType.FREE_TRIANGLE,
                vertices = listOf(
                    Offset(150f, 450f),
                    Offset(350f, 450f),
                    Offset(250f, 250f)
                ),
                color = Color(0xFF2563EB),
                strokeWidth = 4f
            )
        )
    }

    // Laser pointer decay loop (auto vanishes after 1.2s)
    LaunchedEffect(activeTool) {
        while (isActive) {
            val now = System.currentTimeMillis()
            if (laserPoints.isNotEmpty()) {
                laserPoints.removeAll { now - it.second > 1200L }
            }
            delay(40)
        }
    }

    // Helper to init default vertices for selected 2D shape
    fun createDefaultVerticesForShape(shape: Shape2DType, center: Offset = Offset(300f, 450f)): List<Offset> {
        return when (shape) {
            Shape2DType.FREE_TRIANGLE, Shape2DType.EQUILATERAL_TRIANGLE -> listOf(
                Offset(center.x - 100f, center.y + 80f),
                Offset(center.x + 100f, center.y + 80f),
                Offset(center.x, center.y - 90f)
            )
            Shape2DType.RIGHT_TRIANGLE -> listOf(
                Offset(center.x - 90f, center.y - 80f),
                Offset(center.x - 90f, center.y + 80f),
                Offset(center.x + 90f, center.y + 80f)
            )
            Shape2DType.QUADRILATERAL, Shape2DType.PARALLELOGRAM -> listOf(
                Offset(center.x - 100f, center.y - 60f),
                Offset(center.x + 80f, center.y - 60f),
                Offset(center.x + 110f, center.y + 70f),
                Offset(center.x - 70f, center.y + 70f)
            )
            Shape2DType.RECTANGLE -> listOf(
                Offset(center.x - 110f, center.y - 60f),
                Offset(center.x + 110f, center.y - 60f),
                Offset(center.x + 110f, center.y + 60f),
                Offset(center.x - 110f, center.y + 60f)
            )
            Shape2DType.SQUARE -> listOf(
                Offset(center.x - 75f, center.y - 75f),
                Offset(center.x + 75f, center.y - 75f),
                Offset(center.x + 75f, center.y + 75f),
                Offset(center.x - 75f, center.y + 75f)
            )
            Shape2DType.RHOMBUS -> listOf(
                Offset(center.x, center.y - 90f),
                Offset(center.x + 90f, center.y),
                Offset(center.x, center.y + 90f),
                Offset(center.x - 90f, center.y)
            )
            Shape2DType.TRAPEZOID -> listOf(
                Offset(center.x - 60f, center.y - 60f),
                Offset(center.x + 60f, center.y - 60f),
                Offset(center.x + 110f, center.y + 60f),
                Offset(center.x - 110f, center.y + 60f)
            )
            Shape2DType.REGULAR_PENTAGON -> {
                val list = mutableListOf<Offset>()
                for (i in 0 until 5) {
                    val a = Math.toRadians(i * 72.0 - 90.0)
                    list.add(Offset(center.x + (85f * cos(a)).toFloat(), center.y + (85f * sin(a)).toFloat()))
                }
                list
            }
            Shape2DType.REGULAR_HEXAGON -> {
                val list = mutableListOf<Offset>()
                for (i in 0 until 6) {
                    val a = Math.toRadians(i * 60.0)
                    list.add(Offset(center.x + (85f * cos(a)).toFloat(), center.y + (85f * sin(a)).toFloat()))
                }
                list
            }
            Shape2DType.CIRCLE, Shape2DType.ELLIPSE -> listOf(
                center,
                Offset(center.x + 90f, center.y)
            )
            Shape2DType.LINE_SEGMENT, Shape2DType.ARROW_VECTOR -> listOf(
                Offset(center.x - 100f, center.y),
                Offset(center.x + 100f, center.y)
            )
        }
    }

    // Load PDF Page
    fun loadPdfPage(index: Int) {
        val renderer = pdfRenderer ?: return
        if (index < 0 || index >= renderer.pageCount) return
        scope.launch(Dispatchers.IO) {
            try {
                val page = renderer.openPage(index)
                val width = page.width * 2
                val height = page.height * 2
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                withContext(Dispatchers.Main) {
                    currentPageBitmap = bitmap.asImageBitmap()
                    currentPageIndex = index
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Load Annotations whenever filePath or currentPageIndex changes
    LaunchedEffect(filePath, currentPageIndex) {
        val data = StudyFileAnnotationStore.loadPageAnnotations(context, filePath, currentPageIndex)
        strokes.clear()
        strokes.addAll(data.strokes)
        redoStrokes.clear()
        stickyNotes.clear()
        stickyNotes.addAll(data.stickyNotes)
        movableShapes.clear()
        movableShapes.addAll(data.movableShapes)
        movableTexts.clear()
        movableTexts.addAll(data.movableTexts)
    }

    // Auto-save Annotations whenever content changes (Strokes, Notes, Movable Shapes & Texts)
    LaunchedEffect(
        strokes.size,
        stickyNotes.size,
        movableShapes.size,
        movableTexts.size,
        currentPageIndex
    ) {
        delay(350)
        StudyFileAnnotationStore.savePageAnnotations(
            context = context,
            filePath = filePath,
            pageIndex = currentPageIndex,
            strokes = strokes.toList(),
            stickyNotes = stickyNotes.toList(),
            movableShapes = movableShapes.toList(),
            movableTexts = movableTexts.toList()
        )
    }

    // Initialize PDF
    LaunchedEffect(filePath) {
        if (filePath.isNotBlank()) {
            val file = File(filePath)
            if (file.exists() && file.length() > 0 && filePath.endsWith(".pdf", ignoreCase = true)) {
                isLoadingPdf = true
                try {
                    val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    fileDescriptor = pfd
                    val renderer = PdfRenderer(pfd)
                    pdfRenderer = renderer
                    totalPages = renderer.pageCount
                    loadPdfPage(0)
                    boardMode = BoardMode.PDF
                } catch (e: Exception) {
                    boardMode = BoardMode.WHITEBOARD
                } finally {
                    isLoadingPdf = false
                }
            } else {
                boardMode = BoardMode.WHITEBOARD
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                pdfRenderer?.close()
                fileDescriptor?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Save Annotations Function into single persistent file without duplicates
    fun saveAnnotationsToFile() {
        scope.launch(Dispatchers.IO) {
            isSavingChanges = true
            try {
                // 1. Immediately persist full JSON sidecar
                StudyFileAnnotationStore.savePageAnnotations(
                    context = context,
                    filePath = filePath,
                    pageIndex = currentPageIndex,
                    strokes = strokes.toList(),
                    stickyNotes = stickyNotes.toList(),
                    movableShapes = movableShapes.toList(),
                    movableTexts = movableTexts.toList()
                )

                val doc = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(1080, 1920, 1).create()
                val page = doc.startPage(pageInfo)
                val canvas = page.canvas

                // Background
                val bgPaint = android.graphics.Paint().apply {
                    color = when (boardMode) {
                        BoardMode.WHITEBOARD -> android.graphics.Color.WHITE
                        BoardMode.BLACKBOARD -> android.graphics.Color.rgb(30, 41, 59)
                        BoardMode.GRID_GRAPH -> android.graphics.Color.rgb(248, 250, 252)
                        BoardMode.RULED_LINES -> android.graphics.Color.rgb(254, 252, 232)
                        BoardMode.PDF -> android.graphics.Color.rgb(241, 245, 249)
                    }
                }
                canvas.drawRect(0f, 0f, 1080f, 1920f, bgPaint)

                // If PDF page bitmap exists
                currentPageBitmap?.let { bmp ->
                    val androidBmp = bmp.asAndroidBitmap()
                    val destRect = android.graphics.RectF(0f, 120f, 1080f, 1850f)
                    canvas.drawBitmap(androidBmp, null, destRect, null)
                }

                // Render all strokes onto PDF canvas
                strokes.forEach { stroke ->
                    val p = android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(
                            (stroke.color.alpha * 255).toInt(),
                            (stroke.color.red * 255).toInt(),
                            (stroke.color.green * 255).toInt(),
                            (stroke.color.blue * 255).toInt()
                        )
                        strokeWidth = stroke.strokeWidth * 1.5f
                        style = android.graphics.Paint.Style.STROKE
                        strokeCap = android.graphics.Paint.Cap.ROUND
                        strokeJoin = android.graphics.Paint.Join.ROUND
                        isAntiAlias = true
                    }
                    if (stroke.points.size >= 2) {
                        val path = android.graphics.Path()
                        path.moveTo(stroke.points.first().x, stroke.points.first().y)
                        for (i in 1 until stroke.points.size) {
                            path.lineTo(stroke.points[i].x, stroke.points[i].y)
                        }
                        canvas.drawPath(path, p)
                    }
                }

                // Render Movable Texts onto PDF canvas
                val textPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                }
                movableTexts.forEach { t ->
                    textPaint.color = android.graphics.Color.argb(
                        (t.color.alpha * 255).toInt(),
                        (t.color.red * 255).toInt(),
                        (t.color.green * 255).toInt(),
                        (t.color.blue * 255).toInt()
                    )
                    textPaint.textSize = t.fontSize * 2f
                    textPaint.isFakeBoldText = t.isBold
                    canvas.drawText(t.text, t.offset.x * 2f, t.offset.y * 2f, textPaint)
                }

                // Title header
                val headerPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.rgb(30, 58, 138)
                    textSize = 28f
                    isFakeBoldText = true
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
                canvas.drawText("الشرح والحل الهندسي: $title", 1020f, 70f, headerPaint)

                doc.finishPage(page)

                // Save in dedicated safe app folder as a single master copy
                val savedFile = StudyFileManager.saveSingleMasterCopy(
                    context = context,
                    pdfDocument = doc,
                    baseTitle = title.ifBlank { "مذكرة_الشرح" }
                )
                doc.close()

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        if (L.isArabic())
                            "تم حفظ التعديلات والرسومات تلقائياً بنجاح في نسخة واحدة 💾"
                        else
                            "Saved annotations and drawings successfully in single master copy 💾",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "حدث خطأ أثناء الحفظ: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isSavingChanges = false
            }
        }
    }

    // Quick Pen Presets Colors (Studio Grade Palette)
    val quickPenColors = listOf(
        Color(0xFF0F172A), // Charcoal Black
        Color(0xFF1E3A8A), // Oxford Navy
        Color(0xFF2563EB), // Electric Royal Blue
        Color(0xFF0284C7), // Sky Blue
        Color(0xFFDC2626), // Vivid Crimson Red
        Color(0xFFE11D48), // Rose Red
        Color(0xFF16A34A), // Emerald Green
        Color(0xFF059669), // Jade Green
        Color(0xFFD97706), // Amber Gold
        Color(0xFF9333EA), // Royal Violet
        Color(0xFFDB2777), // Neon Magenta
        Color(0xFFFFFFFF)  // Chalk White
    )

    // Quick Highlighter Presets
    val quickHighlighters = listOf(
        Color(0xFFFACC15), // Neon Yellow
        Color(0xFF4ADE80), // Neon Green
        Color(0xFF38BDF8), // Neon Cyan
        Color(0xFFF472B6), // Neon Pink
        Color(0xFFFB923C), // Neon Orange
        Color(0xFFA78BFA)  // Soft Lavender
    )

    var showTopOverflowMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (!isFullscreen) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = title.ifBlank { "السبورة الهندسية وعارض المذكرات" },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                            Text(
                                text = if (boardMode == BoardMode.PDF) "صفحة ${currentPageIndex + 1} من $totalPages • مجلد: ${StudyFileManager.APP_MAIN_FOLDER}" else boardMode.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                    },
                    actions = {
                        // Undo
                        IconButton(
                            onClick = {
                                if (strokes.isNotEmpty()) {
                                    val last = strokes.removeAt(strokes.lastIndex)
                                    redoStrokes.add(last)
                                }
                            },
                            enabled = strokes.isNotEmpty()
                        ) {
                            Icon(Icons.Filled.Undo, contentDescription = com.example.util.L.undo())
                        }

                        // Redo
                        IconButton(
                            onClick = {
                                if (redoStrokes.isNotEmpty()) {
                                    val last = redoStrokes.removeAt(redoStrokes.lastIndex)
                                    strokes.add(last)
                                }
                            },
                            enabled = redoStrokes.isNotEmpty()
                        ) {
                            Icon(Icons.Filled.Redo, contentDescription = com.example.util.L.redo())
                        }

                        // Save Copy Button
                        Button(
                            onClick = { saveAnnotationsToFile() },
                            enabled = !isSavingChanges,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            if (isSavingChanges) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(com.example.util.L.saveCopy(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // More Actions Overflow Menu
                        Box {
                            IconButton(onClick = { showTopOverflowMenu = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "المزيد من الخيارات")
                            }

                            DropdownMenu(
                                expanded = showTopOverflowMenu,
                                onDismissRequest = { showTopOverflowMenu = false }
                            ) {
                                // Background / Board Mode Selector
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.DashboardCustomize, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(com.example.util.L.boardMode(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    onClick = {
                                        showTopOverflowMenu = false
                                        showBoardModeSheet = true
                                    }
                                    )

                                // Add as Homework
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.AssignmentTurnedIn, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(com.example.util.L.homework(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    onClick = {
                                        showTopOverflowMenu = false
                                        showHomeworkDialog = true
                                    }
                                )

                                // Extract / Convert as Image
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Image, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(com.example.util.L.extractPageAsImage() + " 📸", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    onClick = {
                                        showTopOverflowMenu = false
                                        showExportPageImageDialog = true
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Collections, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(com.example.util.L.convertPdfToImages() + " 📑", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    onClick = {
                                        showTopOverflowMenu = false
                                        showConvertFullPdfDialog = true
                                    }
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                // Clear All Drawings
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(com.example.util.L.clearAll(), fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                        }
                                    },
                                    onClick = {
                                        showTopOverflowMenu = false
                                        strokes.clear()
                                        redoStrokes.clear()
                                        laserPoints.clear()
                                    }
                                )

                                // Fullscreen Toggle
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Fullscreen, contentDescription = null, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(com.example.util.L.fullscreen(), fontSize = 12.sp)
                                        }
                                    },
                                    onClick = {
                                        showTopOverflowMenu = false
                                        isFullscreen = true
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(if (isFullscreen) PaddingValues(0.dp) else paddingValues)
                .background(
                    when (boardMode) {
                        BoardMode.PDF -> Color(0xFFE2E8F0)
                        BoardMode.WHITEBOARD -> Color.White
                        BoardMode.BLACKBOARD -> Color(0xFF1E293B)
                        BoardMode.GRID_GRAPH -> Color(0xFFF8FAFC)
                        BoardMode.RULED_LINES -> Color(0xFFFEFCE8)
                    }
                )
        ) {
            // Fullscreen Exit Floating Button
            if (isFullscreen) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    IconButton(
                        onClick = { isFullscreen = false },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(Icons.Filled.FullscreenExit, contentDescription = "إلغاء ملء الشاشة", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Layer 1 & 2: Interactive Zoomable & Drawable Canvas
            val canvasModifier = if (activeTool == ActiveTool.HAND) {
                Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            val oldScale = zoomScale
                            val newScale = (zoomScale * zoom).coerceIn(0.5f, 6.0f)
                            // Keep focal centroid stationary during zoom & pan
                            panOffset = centroid - (centroid - panOffset) * (newScale / oldScale) + pan
                            zoomScale = newScale
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { tapOffset ->
                                if (zoomScale > 1.1f) {
                                    zoomScale = 1.0f
                                    panOffset = Offset.Zero
                                } else {
                                    val targetScale = 2.2f
                                    panOffset = tapOffset - (tapOffset - panOffset) * (targetScale / zoomScale)
                                    zoomScale = targetScale
                                }
                            }
                        )
                    }
            } else if (activeTool == ActiveTool.SELECT) {
                Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .pointerInput(movableShapes.size, movableTexts.size, zoomScale, panOffset) {
                        detectTapGestures(
                            onTap = { screenTap ->
                                val worldTap = (screenTap - panOffset) / zoomScale
                                val tappedShape = movableShapes.lastOrNull { s ->
                                    val dist = (s.center - worldTap).getDistance()
                                    dist <= (s.size * 0.8f) || (s.vertices?.any { (it - worldTap).getDistance() < 50f } == true)
                                }
                                if (tappedShape != null) {
                                    selectedMovableShapeId = tappedShape.id
                                    selectedMovableTextId = null
                                } else {
                                    val tappedText = movableTexts.lastOrNull { t ->
                                        (t.offset - worldTap).getDistance() <= 120f
                                    }
                                    if (tappedText != null) {
                                        selectedMovableTextId = tappedText.id
                                        selectedMovableShapeId = null
                                    } else {
                                        selectedMovableShapeId = null
                                        selectedMovableTextId = null
                                    }
                                }
                            }
                        )
                    }
            } else if (activeTool == ActiveTool.SHAPE_3D) {
                Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .pointerInput(selected3DShape, selectedColor, strokeWidth, shape3DSize, zoomScale, panOffset) {
                        detectTapGestures(
                            onTap = { tapOffset ->
                                val worldOffset = (tapOffset - panOffset) / zoomScale
                                movableShapes.add(
                                    MovableShapeItem(
                                        type3D = selected3DShape,
                                        center = worldOffset,
                                        size = shape3DSize,
                                        color = selectedColor,
                                        strokeWidth = strokeWidth
                                    )
                                )
                                Toast.makeText(
                                    context,
                                    if (L.isArabic()) "تم إضافة مجسم ثلاثي الأبعاد! يمكنك سحبه وتعديله 🧊" else "3D Shape added! Drag & resize freely 🧊",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
            } else {
                Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .pointerInput(activeTool, selectedColor, strokeWidth, activePenStyle, strokeOpacity, zoomScale, panOffset) {
                        detectDragGestures(
                            onDragStart = { screenOffset ->
                                val worldOffset = (screenOffset - panOffset) / zoomScale
                                when (activeTool) {
                                    ActiveTool.PEN, ActiveTool.HIGHLIGHTER -> {
                                        currentPathPoints = listOf(worldOffset)
                                    }
                                    ActiveTool.LASER -> {
                                        laserPoints.add(worldOffset to System.currentTimeMillis())
                                    }
                                    ActiveTool.ERASER -> {
                                        strokes.removeAll { stroke ->
                                            stroke.points.any { p -> (p - worldOffset).getDistance() < (strokeWidth * 3f).coerceAtLeast(35f) }
                                        }
                                    }
                                    else -> {}
                                }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val worldOffset = (change.position - panOffset) / zoomScale
                                when (activeTool) {
                                    ActiveTool.PEN, ActiveTool.HIGHLIGHTER -> {
                                        currentPathPoints = currentPathPoints + worldOffset
                                    }
                                    ActiveTool.LASER -> {
                                        laserPoints.add(worldOffset to System.currentTimeMillis())
                                    }
                                    ActiveTool.ERASER -> {
                                        strokes.removeAll { stroke ->
                                            stroke.points.any { p -> (p - worldOffset).getDistance() < (strokeWidth * 3f).coerceAtLeast(35f) }
                                        }
                                    }
                                    else -> {}
                                }
                            },
                            onDragEnd = {
                                if (currentPathPoints.isNotEmpty()) {
                                    strokes.add(
                                        DrawStroke(
                                            points = currentPathPoints,
                                            color = selectedColor.copy(alpha = strokeOpacity),
                                            strokeWidth = strokeWidth,
                                            penStyle = activePenStyle
                                        )
                                    )
                                    currentPathPoints = emptyList()
                                    redoStrokes.clear()
                                }
                            }
                        )
                    }
            }

            Canvas(modifier = canvasModifier) {
                // Apply Zoom & Pan Matrix Transformation to all drawings & layers
                withTransform({
                    translate(panOffset.x, panOffset.y)
                    scale(zoomScale, zoomScale, pivot = Offset.Zero)
                }) {
                    // Background Grids & Lines
                    when (boardMode) {
                        BoardMode.GRID_GRAPH -> {
                            val step = 40f
                            val gridPaint = Color(0xFFCBD5E1)
                            var x = -1000f
                            while (x < size.width + 1000f) {
                                drawLine(gridPaint, Offset(x, -1000f), Offset(x, size.height + 1000f), strokeWidth = 1f)
                                x += step
                            }
                            var y = -1000f
                            while (y < size.height + 1000f) {
                                drawLine(gridPaint, Offset(-1000f, y), Offset(size.width + 1000f, y), strokeWidth = 1f)
                                y += step
                            }
                        }
                        BoardMode.RULED_LINES -> {
                            val step = 55f
                            val linePaint = Color(0xFF93C5FD)
                            var y = -1000f
                            while (y < size.height + 1000f) {
                                drawLine(linePaint, Offset(-1000f, y), Offset(size.width + 1000f, y), strokeWidth = 1.5f)
                                y += step
                            }
                        }
                        BoardMode.PDF -> {
                            currentPageBitmap?.let { bmp ->
                                val scale = min(size.width / bmp.width, size.height / bmp.height)
                                val destW = bmp.width * scale
                                val destH = bmp.height * scale
                                val left = (size.width - destW) / 2f
                                val top = (size.height - destH) / 2f

                                // Shadow behind PDF page
                                drawRect(
                                    color = Color(0x33000000),
                                    topLeft = Offset(left + 6f, top + 6f),
                                    size = Size(destW, destH)
                                )
                                drawImage(
                                    bmp,
                                    dstOffset = IntOffset(left.toInt(), top.toInt()),
                                    dstSize = androidx.compose.ui.unit.IntSize(destW.toInt(), destH.toInt())
                                )
                            }
                        }
                        else -> {}
                    }

                    // 1. Saved Strokes
                    strokes.forEach { stroke ->
                        if (stroke.shape3DType != null && stroke.shape3DCenter != null) {
                            GeometricRenderer.drawRealistic3DShape(
                                this,
                                stroke.shape3DType,
                                stroke.shape3DCenter,
                                stroke.shape3DSize,
                                stroke.color,
                                stroke.strokeWidth
                            )
                        } else if (stroke.shape2DVertices != null && stroke.shape2DType != null) {
                            GeometricRenderer.drawVertexShape(
                                this,
                                VertexShapeState(
                                    type = stroke.shape2DType,
                                    vertices = stroke.shape2DVertices,
                                    color = stroke.color,
                                    strokeWidth = stroke.strokeWidth,
                                    isFilled = stroke.isFilled
                                )
                            )
                        } else if (stroke.points.size > 1) {
                            renderStyledPath(
                                drawScope = this,
                                points = stroke.points,
                                color = stroke.color,
                                strokeWidth = stroke.strokeWidth,
                                penStyle = stroke.penStyle
                            )
                        }
                    }

                    // 1.5 Render Movable 2D & 3D Shapes
                    movableShapes.forEach { shape ->
                        if (shape.type3D != null) {
                            GeometricRenderer.drawRealistic3DShape(
                                this,
                                shape.type3D,
                                shape.center,
                                shape.size,
                                shape.color,
                                shape.strokeWidth
                            )
                        } else if (shape.type2D != null && shape.vertices != null) {
                            GeometricRenderer.drawVertexShape(
                                this,
                                VertexShapeState(
                                    type = shape.type2D,
                                    vertices = shape.vertices,
                                    color = shape.color,
                                    strokeWidth = shape.strokeWidth,
                                    isFilled = shape.isFilled
                                )
                            )
                        }
                    }

                    // 2. Current In-Progress Stroke
                    if (currentPathPoints.size > 1) {
                        renderStyledPath(
                            drawScope = this,
                            points = currentPathPoints,
                            color = selectedColor.copy(alpha = strokeOpacity),
                            strokeWidth = strokeWidth,
                            penStyle = activePenStyle
                        )
                    }

                    // 3. Active 2D Vertex Shape Preview
                    if (isVertexEditorActive) {
                        GeometricRenderer.drawVertexShape(this, activeVertexShape)
                    }

                    // 4. Laser Pointer Trail (myViewBoard glowing trail)
                    if (laserPoints.isNotEmpty()) {
                        val now = System.currentTimeMillis()
                        laserPoints.forEachIndexed { _, (pt, time) ->
                            val age = (now - time).toFloat()
                            val alpha = (1.0f - (age / 1200f)).coerceIn(0.0f, 1.0f)
                            if (alpha > 0f) {
                                drawCircle(
                                    color = Color(0xFFEF4444).copy(alpha = alpha * 0.7f),
                                    radius = 16f * alpha,
                                    center = pt
                                )
                                drawCircle(
                                    color = Color.White.copy(alpha = alpha),
                                    radius = 6f * alpha,
                                    center = pt
                                )
                            }
                        }
                    }
                }
            }

            // Layer 3: Draggable Sticky Notes (With smooth state updates)
            stickyNotes.forEachIndexed { index, note ->
                val currentNoteOffset by rememberUpdatedState(note.offset)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = note.color,
                    shadowElevation = 6.dp,
                    border = BorderStroke(1.dp, Color(0x33000000)),
                    modifier = Modifier
                        .absoluteOffset { IntOffset(currentNoteOffset.x.toInt(), currentNoteOffset.y.toInt()) }
                        .widthIn(min = 140.dp, max = 220.dp)
                        .pointerInput(note.id) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                if (index in stickyNotes.indices) {
                                    stickyNotes[index] = stickyNotes[index].copy(offset = currentNoteOffset + dragAmount)
                                }
                            }
                        }
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.DragHandle, contentDescription = null, tint = Color(0x66000000), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("ملاحظة 📌", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF422006))
                            }
                            IconButton(
                                onClick = { stickyNotes.removeAt(index) },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "حذف الملاحظة", tint = Color.Red, modifier = Modifier.size(12.dp))
                            }
                        }
                        Text(
                            text = note.text,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = note.fontSize.sp, color = Color(0xFF1E293B)),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Layer 3.2: Movable Shapes Interactive Controls & Drag
            movableShapes.forEachIndexed { index, shape ->
                val isSelected = selectedMovableShapeId == shape.id
                val shapeScreenCenter = panOffset + shape.center * zoomScale
                val shapeScreenSize = (shape.size * zoomScale).coerceAtLeast(60f)
                val density = LocalDensity.current

                Box(
                    modifier = Modifier
                        .absoluteOffset {
                            IntOffset(
                                (shapeScreenCenter.x - shapeScreenSize / 2f).toInt(),
                                (shapeScreenCenter.y - shapeScreenSize / 2f).toInt()
                            )
                        }
                        .size(with(density) { shapeScreenSize.toDp() })
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .pointerInput(shape.id, zoomScale, panOffset) {
                            detectTapGestures {
                                selectedMovableShapeId = if (isSelected) null else shape.id
                                selectedMovableTextId = null
                            }
                        }
                        .pointerInput(shape.id, zoomScale, panOffset) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                if (index in movableShapes.indices) {
                                    val deltaWorld = dragAmount / zoomScale
                                    val current = movableShapes[index]
                                    val newCenter = current.center + deltaWorld
                                    val newVertices = current.vertices?.map { it + deltaWorld }
                                    movableShapes[index] = current.copy(
                                        center = newCenter,
                                        vertices = newVertices
                                    )
                                    selectedMovableShapeId = shape.id
                                }
                            }
                        }
                ) {
                    if (isSelected) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 8.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(y = (-40).dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                // If 2D shape with vertices, allow returning to Vertex Editor
                                if (shape.type2D != null && shape.vertices != null) {
                                    IconButton(
                                        onClick = {
                                            activeVertexShape = VertexShapeState(
                                                type = shape.type2D,
                                                vertices = shape.vertices,
                                                color = shape.color,
                                                strokeWidth = shape.strokeWidth,
                                                isFilled = shape.isFilled
                                            )
                                            isVertexEditorActive = true
                                            activeTool = ActiveTool.VERTEX_EDITOR
                                            movableShapes.removeAt(index)
                                            selectedMovableShapeId = null
                                        },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = "تعديل الرؤوس", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        if (index in movableShapes.indices) {
                                            val cur = movableShapes[index]
                                            val newSize = (cur.size + 30f).coerceAtMost(600f)
                                            val scaleRatio = newSize / cur.size
                                            val newVertices = cur.vertices?.map { v ->
                                                cur.center + (v - cur.center) * scaleRatio
                                            }
                                            movableShapes[index] = cur.copy(size = newSize, vertices = newVertices)
                                        }
                                    },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(Icons.Filled.ZoomIn, contentDescription = "تكبير", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = {
                                        if (index in movableShapes.indices) {
                                            val cur = movableShapes[index]
                                            val newSize = (cur.size - 30f).coerceAtLeast(60f)
                                            val scaleRatio = newSize / cur.size
                                            val newVertices = cur.vertices?.map { v ->
                                                cur.center + (v - cur.center) * scaleRatio
                                            }
                                            movableShapes[index] = cur.copy(size = newSize, vertices = newVertices)
                                        }
                                    },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(Icons.Filled.ZoomOut, contentDescription = "تصغير", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                                // Duplicate Shape
                                IconButton(
                                    onClick = {
                                        if (index in movableShapes.indices) {
                                            val cur = movableShapes[index]
                                            val offsetDelta = Offset(40f, 40f)
                                            val dup = cur.copy(
                                                id = UUID.randomUUID().toString(),
                                                center = cur.center + offsetDelta,
                                                vertices = cur.vertices?.map { it + offsetDelta }
                                            )
                                            movableShapes.add(dup)
                                            selectedMovableShapeId = dup.id
                                        }
                                    },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "تكرار", tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = {
                                        if (index in movableShapes.indices) {
                                            movableShapes.removeAt(index)
                                            selectedMovableShapeId = null
                                        }
                                    },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Layer 3.5: Movable and Customizable Text Items
            movableTexts.forEachIndexed { index, item ->
                val isSelected = selectedMovableTextId == item.id
                val textScreenOffset = panOffset + item.offset * zoomScale

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = item.bgColor,
                    shadowElevation = if (isSelected) 6.dp else 0.dp,
                    border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier
                        .absoluteOffset { IntOffset(textScreenOffset.x.toInt(), textScreenOffset.y.toInt()) }
                        .pointerInput(item.id, zoomScale, panOffset) {
                            detectTapGestures {
                                selectedMovableTextId = if (isSelected) null else item.id
                                selectedMovableShapeId = null
                            }
                        }
                        .pointerInput(item.id, zoomScale, panOffset) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                if (index in movableTexts.indices) {
                                    val deltaWorld = dragAmount / zoomScale
                                    movableTexts[index] = movableTexts[index].copy(
                                        offset = movableTexts[index].offset + deltaWorld
                                    )
                                    selectedMovableTextId = item.id
                                }
                            }
                        }
                ) {
                    Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                        if (isSelected) {
                            Row(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        if (index in movableTexts.indices) {
                                            val cur = movableTexts[index]
                                            movableTexts[index] = cur.copy(fontSize = (cur.fontSize + 2f).coerceAtMost(60f))
                                        }
                                    },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "تكبير الخط", modifier = Modifier.size(14.dp))
                                }
                                IconButton(
                                    onClick = {
                                        if (index in movableTexts.indices) {
                                            val cur = movableTexts[index]
                                            movableTexts[index] = cur.copy(fontSize = (cur.fontSize - 2f).coerceAtLeast(10f))
                                        }
                                    },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Filled.Remove, contentDescription = "تصغير الخط", modifier = Modifier.size(14.dp))
                                }
                                IconButton(
                                    onClick = {
                                        if (index in movableTexts.indices) {
                                            val cur = movableTexts[index]
                                            movableTexts[index] = cur.copy(isBold = !cur.isBold)
                                        }
                                    },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Filled.FormatBold, contentDescription = "غامق", tint = if (item.isBold) MaterialTheme.colorScheme.primary else Color.Gray, modifier = Modifier.size(14.dp))
                                }
                                IconButton(
                                    onClick = {
                                        if (index in movableTexts.indices) {
                                            movableTexts.removeAt(index)
                                            selectedMovableTextId = null
                                        }
                                    },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        Text(
                            text = item.text,
                            fontSize = (item.fontSize * zoomScale).sp,
                            fontWeight = if (item.isBold) FontWeight.Bold else FontWeight.Normal,
                            color = item.color
                        )
                    }
                }
            }

            // Layer 4: Interactive 2D Vertex Controller
            if (isVertexEditorActive) {
                InteractiveVertexHandles(
                    shapeState = activeVertexShape,
                    panOffset = panOffset,
                    zoomScale = zoomScale,
                    onVertexMoved = { idx, newOffset ->
                        val updated = activeVertexShape.vertices.toMutableList()
                        if (idx in updated.indices) {
                            updated[idx] = newOffset
                            activeVertexShape = activeVertexShape.copy(vertices = updated)
                        }
                    },
                    onMoveWholeShape = { deltaWorld ->
                        activeVertexShape = activeVertexShape.copy(
                            vertices = activeVertexShape.vertices.map { it + deltaWorld }
                        )
                    }
                )

                // Vertex Shape Action Floating Bar
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = if (isFullscreen) 60.dp else 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${L.moveVertices()}: ${activeVertexShape.type.getLocalizedTitle()}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        Button(
                            onClick = {
                                val centroid = Offset(
                                    activeVertexShape.vertices.map { it.x }.average().toFloat(),
                                    activeVertexShape.vertices.map { it.y }.average().toFloat()
                                )
                                movableShapes.add(
                                    MovableShapeItem(
                                        type2D = activeVertexShape.type,
                                        center = centroid,
                                        vertices = activeVertexShape.vertices,
                                        color = activeVertexShape.color,
                                        strokeWidth = activeVertexShape.strokeWidth,
                                        isFilled = activeVertexShape.isFilled
                                    )
                                )
                                isVertexEditorActive = false
                                activeTool = ActiveTool.PEN
                                Toast.makeText(
                                    context,
                                    if (L.isArabic()) "تم تثبيت الشكل الهندسي! يمكنك سحبه وتعديله بحرية 📐" else "Shape pinned! Drag & modify freely 📐",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Filled.PushPin, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(L.pinShape(), fontSize = 10.sp)
                        }

                        IconButton(
                            onClick = { isVertexEditorActive = false },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "إغلاق", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Layer 5: Realistic Physical Geometric Overlays & Scientific Calculator
            if (isScientificCalculatorVisible) {
                CasioCalculatorOverlay(
                    offset = calculatorOffset,
                    onOffsetChange = { calculatorOffset = it },
                    onClose = { isScientificCalculatorVisible = false },
                    onInsertResultToBoard = { resultText ->
                        val insertPos = (-panOffset + Offset(200f, 300f)) / zoomScale
                        movableTexts.add(
                            MovableTextItem(
                                text = resultText,
                                offset = insertPos,
                                fontSize = 20f,
                                isBold = true,
                                color = selectedColor
                            )
                        )
                        Toast.makeText(
                            context,
                            if (L.isArabic()) "تم إدراج ناتج الحاسبة على السبورة بنجاح 📋" else "Calculator result inserted on board 📋",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
            if (isRulerVisible) {
                RealisticRuler(
                    offset = rulerOffset,
                    angle = rulerAngle,
                    lengthCm = rulerLengthCm,
                    onOffsetChange = { rulerOffset = it },
                    onAngleChange = { rulerAngle = it },
                    onDrawLine = { start, end ->
                        strokes.add(
                            DrawStroke(
                                points = listOf(start, end),
                                color = selectedColor,
                                strokeWidth = strokeWidth,
                                penStyle = activePenStyle
                            )
                        )
                    },
                    onClose = { isRulerVisible = false }
                )
            }

            if (isProtractorVisible) {
                RealisticProtractor(
                    center = protractorCenter,
                    baseAngle = protractorBaseAngle,
                    targetAngle = protractorTargetAngle,
                    onCenterChange = { protractorCenter = it },
                    onBaseAngleChange = { protractorBaseAngle = it },
                    onTargetAngleChange = { protractorTargetAngle = it },
                    onDrawAngle = { center, baseAngle, angleDeg ->
                        val baseRad = Math.toRadians((180 - baseAngle).toDouble())
                        val targetRad = Math.toRadians((180 - (baseAngle + angleDeg)).toDouble())
                        val r = 160f
                        val ray1 = Offset(center.x + (r * cos(baseRad)).toFloat(), center.y - (r * sin(baseRad)).toFloat())
                        val ray2 = Offset(center.x + (r * cos(targetRad)).toFloat(), center.y - (r * sin(targetRad)).toFloat())

                        strokes.add(DrawStroke(points = listOf(center, ray1), color = selectedColor, strokeWidth = strokeWidth))
                        strokes.add(DrawStroke(points = listOf(center, ray2), color = selectedColor, strokeWidth = strokeWidth))
                    },
                    onClose = { isProtractorVisible = false }
                )
            }

            if (isCompassVisible) {
                RealisticCompass(
                    center = compassCenter,
                    radiusPx = compassRadiusPx,
                    onCenterChange = { compassCenter = it },
                    onRadiusChange = { compassRadiusPx = it },
                    onDrawCircleOrArc = { center, r, sweep ->
                        val pts = mutableListOf<Offset>()
                        val step = if (sweep >= 360f) 5 else 2
                        for (deg in 0..sweep.toInt() step step) {
                            val rad = Math.toRadians(deg.toDouble())
                            pts.add(Offset(center.x + (r * cos(rad)).toFloat(), center.y + (r * sin(rad)).toFloat()))
                        }
                        strokes.add(DrawStroke(points = pts, color = selectedColor, strokeWidth = strokeWidth))
                    },
                    onClose = { isCompassVisible = false }
                )
            }

            if (isSetSquareVisible) {
                RealisticSetSquare(
                    offset = setSquareOffset,
                    angle = setSquareAngle,
                    onOffsetChange = { setSquareOffset = it },
                    onAngleChange = { setSquareAngle = it },
                    onDrawTriangle = { offset, angle, sizeDp ->
                        val p1 = offset
                        val rad = Math.toRadians(angle.toDouble())
                        val p2 = Offset(p1.x + sizeDp * cos(rad).toFloat(), p1.y + sizeDp * sin(rad).toFloat())
                        val radPerp = Math.toRadians((angle + 90).toDouble())
                        val p3 = Offset(p1.x + sizeDp * cos(radPerp).toFloat(), p1.y + sizeDp * sin(radPerp).toFloat())
                        strokes.add(DrawStroke(points = listOf(p1, p2, p3, p1), color = selectedColor, strokeWidth = strokeWidth))
                    },
                    onClose = { isSetSquareVisible = false }
                )
            }

            // Layer 6: Floating PDF Page Navigator & Smooth Zoom Controls
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (isFullscreen) 12.dp else 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Page Navigation Bar
                if (boardMode == BoardMode.PDF && totalPages > 1) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        shadowElevation = 8.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // First Page
                            IconButton(
                                onClick = { loadPdfPage(0) },
                                enabled = currentPageIndex > 0,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Filled.FirstPage, contentDescription = "الصفحة الأولى", modifier = Modifier.size(18.dp))
                            }

                            // Prev Page
                            IconButton(
                                onClick = { if (currentPageIndex > 0) loadPdfPage(currentPageIndex - 1) },
                                enabled = currentPageIndex > 0,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Filled.ChevronRight, contentDescription = "السابقة", modifier = Modifier.size(20.dp))
                            }

                            // Clickable Page Number to Jump
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier.clickable { showQuickPageThumbnailSheet = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.GridView, contentDescription = com.example.util.L.pageThumbnails(), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${com.example.util.L.page()} ${currentPageIndex + 1} / $totalPages",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Next Page
                            IconButton(
                                onClick = { if (currentPageIndex < totalPages - 1) loadPdfPage(currentPageIndex + 1) },
                                enabled = currentPageIndex < totalPages - 1,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Filled.ChevronLeft, contentDescription = com.example.util.L.nextPage(), modifier = Modifier.size(20.dp))
                            }

                            // Last Page
                            IconButton(
                                onClick = { loadPdfPage(totalPages - 1) },
                                enabled = currentPageIndex < totalPages - 1,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Filled.LastPage, contentDescription = com.example.util.L.lastPage(), modifier = Modifier.size(18.dp))
                            }

                            VerticalDivider(modifier = Modifier.height(18.dp).padding(horizontal = 2.dp))

                            // Fast Page Share / Export Button
                            IconButton(
                                onClick = { showExportPageImageDialog = true },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Filled.CameraAlt, contentDescription = com.example.util.L.extractPageAsImage(), tint = EmeraldSuccess, modifier = Modifier.size(17.dp))
                            }
                        }
                    }
                }

                // Dedicated Zoom & View Controls Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    shadowElevation = 6.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Zoom Out
                        IconButton(
                            onClick = { zoomScale = (zoomScale - 0.25f).coerceAtLeast(0.5f) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Filled.ZoomOut, contentDescription = "تصغير", modifier = Modifier.size(16.dp))
                        }

                        // Zoom Badge / Reset
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (zoomScale != 1.0f) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier.clickable {
                                zoomScale = 1.0f
                                panOffset = Offset.Zero
                            }
                        ) {
                            Text(
                                text = "${(zoomScale * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (zoomScale != 1.0f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // Zoom In
                        IconButton(
                            onClick = { zoomScale = (zoomScale + 0.25f).coerceAtMost(4.0f) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Filled.ZoomIn, contentDescription = "تكبير", modifier = Modifier.size(16.dp))
                        }

                        // Reset / Fit Page
                        IconButton(
                            onClick = {
                                zoomScale = 1.0f
                                panOffset = Offset.Zero
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Filled.FitScreen, contentDescription = "احتواء الصفحة", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // ========================================================================
            // Layer 7: myViewBoard Sleek Floating Smart Tool Bar
            // ========================================================================
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp, start = 12.dp, end = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Quick Pen Popover Palette (1-Tap Fast Selection)
                AnimatedVisibility(
                    visible = showQuickPenPopover,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 }
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 10.dp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Colors in 2 neat rows
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                quickPenColors.chunked(6).forEach { rowColors ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        rowColors.forEach { color ->
                                            val isSelected = selectedColor == color && activePenStyle != PenStyle.HIGHLIGHTER
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                                    .border(
                                                        width = if (isSelected) 3.dp else 1.dp,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                                        shape = CircleShape
                                                    )
                                                    .clickable {
                                                        selectedColor = color
                                                        activePenStyle = PenStyle.NORMAL
                                                        strokeOpacity = 1.0f
                                                        showQuickPenPopover = false
                                                    }
                                            )
                                        }
                                    }
                                }
                            }

                            // Thickness Presets
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(
                                    3f to "رفيع (3px)",
                                    6f to "عادي (6px)",
                                    12f to "متوسط (12px)",
                                    24f to "عريض (24px)"
                                ).forEach { (w, lbl) ->
                                    val isSelected = strokeWidth == w
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            strokeWidth = w
                                            showQuickPenPopover = false
                                        },
                                        label = { Text(lbl, fontSize = 10.sp) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }

                            // Pen Style Switchers
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(
                                    PenStyle.NORMAL to "قلم ناعم 🖊️",
                                    PenStyle.CALLIGRAPHY to "خط عربي ✒️",
                                    PenStyle.NEON_GLOW to "نيون مضيء ✨",
                                    PenStyle.DASHED to "متقطع ╌"
                                ).forEach { (st, lbl) ->
                                    val isSelected = activePenStyle == st
                                    SuggestionChip(
                                        onClick = {
                                            activePenStyle = st
                                            strokeOpacity = 1.0f
                                            showQuickPenPopover = false
                                        },
                                        label = { Text(lbl, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick Highlighter Popover
                AnimatedVisibility(
                    visible = showQuickHighlighterPopover,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 }
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 10.dp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("تظليل فسفوري: ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            quickHighlighters.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (selectedColor == color && activePenStyle == PenStyle.HIGHLIGHTER) 3.dp else 1.dp,
                                            color = if (selectedColor == color && activePenStyle == PenStyle.HIGHLIGHTER) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            selectedColor = color
                                            activePenStyle = PenStyle.HIGHLIGHTER
                                            strokeOpacity = 0.45f
                                            strokeWidth = 24f
                                            activeTool = ActiveTool.HIGHLIGHTER
                                            showQuickHighlighterPopover = false
                                        }
                                )
                            }
                        }
                    }
                }

                // The Floating Main Toolbar Pill
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    shadowElevation = 12.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 0. Selection & Move Tool (🎯 Select & Move Shapes & Annotations)
                        MyViewBoardToolButton(
                            icon = Icons.Filled.NearMe,
                            label = L.selectTool(),
                            isActive = activeTool == ActiveTool.SELECT,
                            activeColor = Color(0xFF0284C7),
                            onClick = {
                                activeTool = ActiveTool.SELECT
                                showQuickPenPopover = false
                                showQuickHighlighterPopover = false
                            }
                        )

                        // 1. Hand / Pan Tool (🖐️ Move & Zoom without drawing)
                        MyViewBoardToolButton(
                            icon = Icons.Filled.PanTool,
                            label = "تحريك وتكبير",
                            isActive = activeTool == ActiveTool.HAND,
                            activeColor = NavyPrimary,
                            onClick = {
                                activeTool = ActiveTool.HAND
                                showQuickPenPopover = false
                                showQuickHighlighterPopover = false
                            }
                        )

                        // 2. Smart Pen (✏️)
                        MyViewBoardToolButton(
                            icon = Icons.Filled.Edit,
                            label = "قلم",
                            isActive = activeTool == ActiveTool.PEN,
                            activeColor = if (activeTool == ActiveTool.PEN) selectedColor else NavyPrimary,
                            onClick = {
                                if (activeTool == ActiveTool.PEN) {
                                    showQuickPenPopover = !showQuickPenPopover
                                } else {
                                    activeTool = ActiveTool.PEN
                                    activePenStyle = PenStyle.NORMAL
                                    strokeOpacity = 1.0f
                                    showQuickPenPopover = false
                                }
                                showQuickHighlighterPopover = false
                            }
                        )

                        // 3. Highlighter (🖍️)
                        MyViewBoardToolButton(
                            icon = Icons.Filled.Brush,
                            label = "تظليل",
                            isActive = activeTool == ActiveTool.HIGHLIGHTER || (activeTool == ActiveTool.PEN && activePenStyle == PenStyle.HIGHLIGHTER),
                            activeColor = Color(0xFFEAB308),
                            onClick = {
                                if (activeTool == ActiveTool.HIGHLIGHTER) {
                                    showQuickHighlighterPopover = !showQuickHighlighterPopover
                                } else {
                                    activeTool = ActiveTool.HIGHLIGHTER
                                    activePenStyle = PenStyle.HIGHLIGHTER
                                    strokeOpacity = 0.45f
                                    strokeWidth = 24f
                                    showQuickHighlighterPopover = false
                                }
                                showQuickPenPopover = false
                            }
                        )

                        // 4. Stroke & Font Size Control (🔤)
                        MyViewBoardToolButton(
                            icon = Icons.Filled.FormatSize,
                            label = "حجم الخط",
                            isActive = showStrokeWidthDialog,
                            activeColor = Color(0xFF7C3AED),
                            onClick = {
                                showStrokeWidthDialog = true
                                showQuickPenPopover = false
                                showQuickHighlighterPopover = false
                            }
                        )

                        // 5. Laser Pointer (🪄 Glowing vanishing trail)
                        MyViewBoardToolButton(
                            icon = Icons.Filled.AutoAwesome,
                            label = "ليزر",
                            isActive = activeTool == ActiveTool.LASER,
                            activeColor = Color(0xFFEF4444),
                            onClick = {
                                activeTool = ActiveTool.LASER
                                showQuickPenPopover = false
                                showQuickHighlighterPopover = false
                                Toast.makeText(context, "مؤشر ليزر تفاعلي للشرح يختفي تلقائياً 🪄", Toast.LENGTH_SHORT).show()
                            }
                        )

                        // 6. Eraser (🧹)
                        MyViewBoardToolButton(
                            icon = Icons.Filled.AutoFixNormal,
                            label = "ممحاة",
                            isActive = activeTool == ActiveTool.ERASER,
                            activeColor = Color(0xFFEF4444),
                            onClick = {
                                activeTool = ActiveTool.ERASER
                                showQuickPenPopover = false
                                showQuickHighlighterPopover = false
                            }
                        )

                        VerticalDivider(
                            modifier = Modifier
                                .height(24.dp)
                                .padding(horizontal = 2.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        // 7. Unified Tools Dropdown Menu (📐 المسطرة، البرجل، المنقلة، الأشكال الهندسية)
                        Box {
                            MyViewBoardToolButton(
                                icon = Icons.Filled.SquareFoot,
                                label = com.example.util.L.tools(),
                                isActive = isRulerVisible || isProtractorVisible || isCompassVisible || isSetSquareVisible || activeTool == ActiveTool.SHAPE_2D || activeTool == ActiveTool.SHAPE_3D || isVertexEditorActive,
                                activeColor = Color(0xFF2563EB),
                                onClick = {
                                    showToolsDropdown = true
                                    showQuickPenPopover = false
                                    showQuickHighlighterPopover = false
                                }
                            )

                            DropdownMenu(
                                expanded = showToolsDropdown,
                                onDismissRequest = { showToolsDropdown = false },
                                modifier = Modifier
                                    .width(230.dp)
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Filled.Straighten, contentDescription = null, tint = if (isRulerVisible) Color(0xFF2563EB) else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                                            Column {
                                                Text(com.example.util.L.ruler(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(com.example.util.L.rulerDesc(), fontSize = 10.sp, color = Color.Gray)
                                            }
                                        }
                                    },
                                    trailingIcon = {
                                        if (isRulerVisible) {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                                        }
                                    },
                                    onClick = {
                                        val newState = !isRulerVisible
                                        isRulerVisible = newState
                                        if (newState) {
                                            isProtractorVisible = false
                                            isCompassVisible = false
                                            isSetSquareVisible = false
                                            isVertexEditorActive = false
                                            activeTool = ActiveTool.RULER
                                            Toast.makeText(context, "📐 ${com.example.util.L.ruler()}", Toast.LENGTH_SHORT).show()
                                        }
                                        showToolsDropdown = false
                                    }
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Filled.ChangeHistory, contentDescription = null, tint = if (isCompassVisible) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                                            Column {
                                                Text(com.example.util.L.compass(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(com.example.util.L.compassDesc(), fontSize = 10.sp, color = Color.Gray)
                                            }
                                        }
                                    },
                                    trailingIcon = {
                                        if (isCompassVisible) {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                                        }
                                    },
                                    onClick = {
                                        val newState = !isCompassVisible
                                        isCompassVisible = newState
                                        if (newState) {
                                            isRulerVisible = false
                                            isProtractorVisible = false
                                            isSetSquareVisible = false
                                            isVertexEditorActive = false
                                            activeTool = ActiveTool.COMPASS
                                            Toast.makeText(context, "🧭 ${com.example.util.L.compass()}", Toast.LENGTH_SHORT).show()
                                        }
                                        showToolsDropdown = false
                                    }
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Filled.Explore, contentDescription = null, tint = if (isProtractorVisible) Color(0xFF0284C7) else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                                            Column {
                                                Text(com.example.util.L.protractor(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(com.example.util.L.protractorDesc(), fontSize = 10.sp, color = Color.Gray)
                                            }
                                        }
                                    },
                                    trailingIcon = {
                                        if (isProtractorVisible) {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                                        }
                                    },
                                    onClick = {
                                        val newState = !isProtractorVisible
                                        isProtractorVisible = newState
                                        if (newState) {
                                            isRulerVisible = false
                                            isCompassVisible = false
                                            isSetSquareVisible = false
                                            isVertexEditorActive = false
                                            activeTool = ActiveTool.PROTRACTOR
                                            Toast.makeText(context, "📐 ${com.example.util.L.protractor()}", Toast.LENGTH_SHORT).show()
                                        }
                                        showToolsDropdown = false
                                    }
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Filled.Category, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(20.dp))
                                            Column {
                                                Text(com.example.util.L.shapes2D(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(com.example.util.L.shapes2DDesc(), fontSize = 10.sp, color = Color.Gray)
                                            }
                                        }
                                    },
                                    onClick = {
                                        isRulerVisible = false
                                        isCompassVisible = false
                                        isProtractorVisible = false
                                        isSetSquareVisible = false
                                        show2DShapesSheet = true
                                        showToolsDropdown = false
                                    }
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Filled.ViewInAr, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                                            Column {
                                                Text(com.example.util.L.shapes3D(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(com.example.util.L.shapes3DDesc(), fontSize = 10.sp, color = Color.Gray)
                                            }
                                        }
                                    },
                                    onClick = {
                                        isRulerVisible = false
                                        isCompassVisible = false
                                        isProtractorVisible = false
                                        isSetSquareVisible = false
                                        show3DShapesSheet = true
                                        showToolsDropdown = false
                                    }
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Filled.Calculate, contentDescription = null, tint = if (isScientificCalculatorVisible) Color(0xFF0284C7) else Color(0xFF0F766E), modifier = Modifier.size(20.dp))
                                            Column {
                                                Text(com.example.util.L.scientificCalculator(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(com.example.util.L.scientificCalculatorDesc(), fontSize = 10.sp, color = Color.Gray)
                                            }
                                        }
                                    },
                                    trailingIcon = {
                                        if (isScientificCalculatorVisible) {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                                        }
                                    },
                                    onClick = {
                                        isScientificCalculatorVisible = !isScientificCalculatorVisible
                                        showToolsDropdown = false
                                    }
                                )
                            }
                        }

                        // 8. Sticky Note (📝)
                        MyViewBoardToolButton(
                            icon = Icons.Filled.NoteAdd,
                            label = if (L.isArabic()) "ملاحظة" else "Note",
                            isActive = false,
                            activeColor = Color(0xFF0D9488),
                            onClick = {
                                showAddNoteDialog = true
                                showQuickPenPopover = false
                                showQuickHighlighterPopover = false
                            }
                        )

                        // 9. Movable Text (🔤 نص قابل للتحريك)
                        MyViewBoardToolButton(
                            icon = Icons.Filled.TextFields,
                            label = if (L.isArabic()) "نص حر" else "Text",
                            isActive = false,
                            activeColor = Color(0xFF8B5CF6),
                            onClick = {
                                showAddTextDialog = true
                                showQuickPenPopover = false
                                showQuickHighlighterPopover = false
                            }
                        )
                    }
                }
            }

            // Layer 8: Loading State
            if (isLoadingPdf) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("جاري تحميل صفحات الكتاب والمذكرة...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    // ========================================================================
    // DIALOGS & BOTTOM SHEETS
    // ========================================================================

    // Stroke Width & Font Size Controller Dialog
    if (showStrokeWidthDialog) {
        AlertDialog(
            onDismissRequest = { showStrokeWidthDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.FormatSize, contentDescription = null, tint = NavyPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("التحكم في حجم الخط والسمك 🔤", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Live Stroke Width Preview Box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawLine(
                                    color = selectedColor.copy(alpha = strokeOpacity),
                                    start = Offset(40f, size.height / 2),
                                    end = Offset(size.width - 40f, size.height / 2),
                                    strokeWidth = strokeWidth,
                                    cap = StrokeCap.Round
                                )
                            }
                            Text(
                                text = "سمك الخط الحالي: ${strokeWidth.toInt()} px",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 4.dp)
                            )
                        }
                    }

                    // Stroke Width Slider
                    Column {
                        Text("ضبط السمك بدقة:", style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = strokeWidth,
                            onValueChange = { strokeWidth = it },
                            valueRange = 1f..50f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Quick Preset Buttons
                    Text("أحجام سريعة جاهزة:", style = MaterialTheme.typography.labelSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val presets = listOf(
                            2f to "دقيق (2px)",
                            4f to "عادي (4px)",
                            8f to "متوسط (8px)",
                            16f to "عريض (16px)",
                            28f to "تظليل (28px)",
                            40f to "كبير جداً (40px)"
                        )
                        items(presets) { (w, lbl) ->
                            FilterChip(
                                selected = strokeWidth.toInt() == w.toInt(),
                                onClick = { strokeWidth = w },
                                label = { Text(lbl, fontSize = 10.sp) }
                            )
                        }
                    }

                    // Opacity Slider
                    Column {
                        Text("شفافية الخط: ${(strokeOpacity * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = strokeOpacity,
                            onValueChange = { strokeOpacity = it },
                            valueRange = 0.15f..1.0f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showStrokeWidthDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) {
                    Text("تم")
                }
            }
        )
    }

    // Jump To Page Dialog (انتقال سريع لصفحة محددة)
    if (showJumpToPageDialog && totalPages > 1) {
        var targetPageText by remember { mutableStateOf("${currentPageIndex + 1}") }
        var targetPageSlider by remember { mutableStateOf((currentPageIndex + 1).toFloat()) }

        AlertDialog(
            onDismissRequest = { showJumpToPageDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.MenuBook, contentDescription = null, tint = NavyPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("الانتقال إلى صفحة 📖", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("إجمالي صفحات الكتاب: $totalPages صفحة", style = MaterialTheme.typography.bodyMedium)

                    OutlinedTextField(
                        value = targetPageText,
                        onValueChange = { input ->
                            val clean = input.filter { it.isDigit() }
                            targetPageText = clean
                            clean.toIntOrNull()?.let { num ->
                                if (num in 1..totalPages) {
                                    targetPageSlider = num.toFloat()
                                }
                            }
                        },
                        label = { Text("رقم الصفحة (1 - $totalPages)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Slider(
                        value = targetPageSlider,
                        onValueChange = {
                            targetPageSlider = it
                            targetPageText = "${it.toInt()}"
                        },
                        valueRange = 1f..totalPages.toFloat(),
                        steps = (totalPages - 2).coerceAtLeast(0),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick Jump Buttons (+5, -5, +10, -10)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(-10, -5, +5, +10).forEach { delta ->
                            val newTarget = (currentPageIndex + 1 + delta).coerceIn(1, totalPages)
                            FilledTonalButton(
                                onClick = {
                                    targetPageSlider = newTarget.toFloat()
                                    targetPageText = "$newTarget"
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(28.dp)
                            ) {
                                Text(if (delta > 0) "+$delta" else "$delta", fontSize = 10.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = (targetPageText.toIntOrNull() ?: 1).coerceIn(1, totalPages)
                        loadPdfPage(p - 1)
                        showJumpToPageDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                ) {
                    Text("انتقال")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpToPageDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Add Sticky Note Dialog
    if (showAddNoteDialog) {
        var noteText by remember { mutableStateOf("") }
        var noteColor by remember { mutableStateOf(Color(0xFFFEF08A)) }
        var noteFontSize by remember { mutableStateOf(12f) }

        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = { Text("إضافة ملاحظة لاصقة على الصفحة 📝") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        placeholder = { Text("اكتب الملاحظة أو التوجيه هنا...") },
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("اللون:", fontSize = 11.sp)
                        listOf(
                            Color(0xFFFEF08A) to "أصفر",
                            Color(0xFFBBF7D0) to "أخضر",
                            Color(0xFFBAE6FD) to "أزرق",
                            Color(0xFFFBCFE8) to "وردي"
                        ).forEach { (c, _) ->
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(if (noteColor == c) 2.dp else 0.dp, Color.Black, CircleShape)
                                    .clickable { noteColor = c }
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("حجم الخط:", fontSize = 11.sp)
                        listOf(10f to "صغير", 12f to "متوسط", 15f to "كبير").forEach { (sz, label) ->
                            FilterChip(
                                selected = noteFontSize == sz,
                                onClick = { noteFontSize = sz },
                                label = { Text(label, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (noteText.isNotBlank()) {
                            stickyNotes.add(
                                StickyNoteItem(
                                    offset = Offset(180f, 260f),
                                    text = noteText,
                                    color = noteColor,
                                    fontSize = noteFontSize
                                )
                            )
                            showAddNoteDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                ) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Add Movable Text Dialog
    if (showAddTextDialog) {
        var freeTextValue by remember { mutableStateOf("") }
        var freeTextColor by remember { mutableStateOf(Color(0xFF1E293B)) }
        var freeTextBgColor by remember { mutableStateOf(Color.Transparent) }
        var freeTextSize by remember { mutableStateOf(18f) }
        var isFreeBold by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddTextDialog = false },
            title = { Text(if (L.isArabic()) "إضافة نص حر قابل للتحريك 🔤" else "Add Movable Text 🔤") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = freeTextValue,
                        onValueChange = { freeTextValue = it },
                        placeholder = { Text(if (L.isArabic()) "اكتب النص أو القانون أو العنوان هنا..." else "Type text here...") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (L.isArabic()) "لون النص:" else "Color:", fontSize = 11.sp)
                        listOf(
                            Color(0xFF1E293B) to "أسود",
                            Color(0xFF1E3A8A) to "أزرق",
                            Color(0xFFDC2626) to "أحمر",
                            Color(0xFF059669) to "أخضر",
                            Color(0xFF7C3AED) to "بنفسجي"
                        ).forEach { (c, _) ->
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(if (freeTextColor == c) 2.dp else 0.dp, Color.Black, CircleShape)
                                    .clickable { freeTextColor = c }
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (L.isArabic()) "خلفية النص:" else "Background:", fontSize = 11.sp)
                        listOf(
                            Color.Transparent to "شفافة",
                            Color(0xFFFFFFFF) to "أبيض",
                            Color(0xFFFEF08A) to "أصفر",
                            Color(0xFFBAE6FD) to "أزرق"
                        ).forEach { (bg, lbl) ->
                            FilterChip(
                                selected = freeTextBgColor == bg,
                                onClick = { freeTextBgColor = bg },
                                label = { Text(lbl, fontSize = 10.sp) }
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (L.isArabic()) "الحجم:" else "Size:", fontSize = 11.sp)
                            listOf(14f to "صغير", 18f to "متوسط", 26f to "كبير").forEach { (sz, label) ->
                                FilterChip(
                                    selected = freeTextSize == sz,
                                    onClick = { freeTextSize = sz },
                                    label = { Text(label, fontSize = 10.sp) }
                                )
                            }
                        }

                        IconButton(
                            onClick = { isFreeBold = !isFreeBold },
                            modifier = Modifier
                                .size(34.dp)
                                .background(if (isFreeBold) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, CircleShape)
                        ) {
                            Icon(Icons.Filled.FormatBold, contentDescription = "غامق", tint = if (isFreeBold) MaterialTheme.colorScheme.primary else Color.Gray)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (freeTextValue.isNotBlank()) {
                            val worldCenter = (Offset(200f, 300f) - panOffset) / zoomScale
                            movableTexts.add(
                                MovableTextItem(
                                    offset = worldCenter,
                                    text = freeTextValue,
                                    fontSize = freeTextSize,
                                    color = freeTextColor,
                                    bgColor = freeTextBgColor,
                                    isBold = isFreeBold
                                )
                            )
                            showAddTextDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (L.isArabic()) "إضافة النص" else "Add Text")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTextDialog = false }) {
                    Text(if (L.isArabic()) "إلغاء" else "Cancel")
                }
            }
        )
    }

    // Geometric Tools Bottom Sheet (Ruler, Compass, Protractor)
    if (showGeometricToolsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showGeometricToolsSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "📐 الأدوات الهندسية التفاعلية",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                GeometricToolToggleCard(
                    title = "المسطرة الهندسية (Ruler)",
                    subtitle = "مسطرة حقيقية بالسنتيمتر والميلليمتر مع تحكم بالزاوية ورسم مستقيمات دقيقة",
                    icon = Icons.Filled.Straighten,
                    isEnabled = isRulerVisible,
                    onToggle = {
                        isRulerVisible = !isRulerVisible
                        if (isRulerVisible) activeTool = ActiveTool.RULER
                    }
                )

                GeometricToolToggleCard(
                    title = "المنقلة الهندسية (Protractor)",
                    subtitle = "منقلة شفافة لقياس الزوايا بدقة من 0° إلى 180° ورسم الأشعة",
                    icon = Icons.Filled.Explore,
                    isEnabled = isProtractorVisible,
                    onToggle = {
                        isProtractorVisible = !isProtractorVisible
                        if (isProtractorVisible) activeTool = ActiveTool.PROTRACTOR
                    }
                )

                GeometricToolToggleCard(
                    title = "الفرجار / البرجل الهندسي (Compass)",
                    subtitle = "برجل بإبرة وقلم لتحديد نصف القطر ورسم الدوائر والأقواس الهندسية",
                    icon = Icons.Filled.ChangeHistory,
                    isEnabled = isCompassVisible,
                    onToggle = {
                        isCompassVisible = !isCompassVisible
                        if (isCompassVisible) activeTool = ActiveTool.COMPASS
                    }
                )

                GeometricToolToggleCard(
                    title = "المثلث القائم 45° (Set Square)",
                    subtitle = "مثلث رسم هندسي قائم الزاوية شفاف مع قياسات بالملليمتر",
                    icon = Icons.Filled.SquareFoot,
                    isEnabled = isSetSquareVisible,
                    onToggle = {
                        isSetSquareVisible = !isSetSquareVisible
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // 2D Shapes & Vertex Manipulation Bottom Sheet
    if (show2DShapesSheet) {
        ModalBottomSheet(
            onDismissRequest = { show2DShapesSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (L.isArabic()) "🔺 الأشكال المستوية (2D) والتحكم في الرؤوس" else "🔺 2D Shapes & Vertex Manipulation",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (L.isArabic()) "تعبئة خلفية الشكل بلون خفيف" else "Fill shape background", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isFilledShape, onCheckedChange = { isFilledShape = it })
                }

                HorizontalDivider()

                Text(if (L.isArabic()) "اختر الشكل الهندسي:" else "Select Geometric Shape:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(Shape2DType.values()) { shape ->
                        FilterChip(
                            selected = selected2DShape == shape,
                            onClick = {
                                selected2DShape = shape
                                val centerWorld = (-panOffset + Offset(400f, 650f)) / zoomScale
                                val vertices = createDefaultVerticesForShape(shape, center = centerWorld)
                                val new2DShape = MovableShapeItem(
                                    type2D = shape,
                                    center = centerWorld,
                                    vertices = vertices,
                                    color = selectedColor,
                                    strokeWidth = strokeWidth,
                                    isFilled = isFilledShape,
                                    size = 180f
                                )
                                movableShapes.add(new2DShape)
                                selectedMovableShapeId = new2DShape.id
                                activeTool = ActiveTool.SELECT
                                show2DShapesSheet = false
                                Toast.makeText(
                                    context,
                                    if (L.isArabic()) "تم إدراج الشكل وتحديده للتحريك الفوري 📐 (اسحبه في أي مكان أو عدل رؤوسه)" else "Placed shape with immediate move controls 📐",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            label = { Text(shape.getLocalizedTitle(), fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (L.isArabic())
                                "عند اختيار أي شكل ستظهر دوائر الرؤوس (أ، ب، جـ..) على السبورة ويمكنك سحب أي رأس لتغيير قياسات الزوايا والأضلاع بحرية ثم الضغط على 'تثبيت'."
                            else
                                "Selecting any shape displays vertex handles directly on the vertices. Drag handles to morph angles & sides freely, then tap 'Pin Shape'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // 3D Shapes Bottom Sheet
    if (show3DShapesSheet) {
        ModalBottomSheet(
            onDismissRequest = { show3DShapesSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (L.isArabic()) "🧊 المجسمات الهندسية الفراغية (3D)" else "🧊 3D Solid Geometry",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Text(
                    if (L.isArabic()) "اختر المجسم الهندسي الفراغي لإدراجه فوراً على السبورة مع إمكانية التحريك والتكبير والتصغير:" else "Select 3D solid to place immediately with move & resize options:",
                    style = MaterialTheme.typography.bodySmall
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(Shape3DType.values()) { shape ->
                        FilterChip(
                            selected = selected3DShape == shape,
                            onClick = {
                                selected3DShape = shape
                                val centerWorld = (-panOffset + Offset(400f, 650f)) / zoomScale
                                val new3DShape = MovableShapeItem(
                                    type3D = shape,
                                    center = centerWorld,
                                    size = shape3DSize,
                                    color = selectedColor,
                                    strokeWidth = strokeWidth
                                )
                                movableShapes.add(new3DShape)
                                selectedMovableShapeId = new3DShape.id
                                activeTool = ActiveTool.SELECT
                                show3DShapesSheet = false
                                Toast.makeText(context, if (L.isArabic()) "تم إدراج مجسم ${shape.getLocalizedTitle()} وتحديده للتحريك الفوري 🧊" else "Placed ${shape.getLocalizedTitle()} with immediate move controls 🧊", Toast.LENGTH_SHORT).show()
                            },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(shape.iconText)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(shape.getLocalizedTitle(), fontSize = 11.sp)
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("حجم المجسم: ${shape3DSize.toInt()} dp", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = shape3DSize,
                        onValueChange = { shape3DSize = it },
                        valueRange = 80f..300f,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Board Mode Bottom Sheet
    if (showBoardModeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBoardModeSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "🎨 نوع السبورة والخلفية",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                BoardMode.values().forEach { mode ->
                    val isSelected = boardMode == mode
                    Card(
                        onClick = {
                            boardMode = mode
                            showBoardModeSheet = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = isSelected, onClick = null)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(mode.title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Add As Homework Dialog
    if (showHomeworkDialog) {
        AddAsHomeworkDialog(
            repository = repository,
            currentFileTitle = title,
            currentPageIndex = currentPageIndex,
            onDismiss = { showHomeworkDialog = false },
            onHomeworkSaved = { message: String ->
                showHomeworkDialog = false
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        )
    }

    // ========================================================================
    // NEW: Export Current Page as Image Dialog (Ultra HD & Watermark System)
    // ========================================================================
    if (showExportPageImageDialog) {
        var selectedQuality by remember { mutableStateOf(PdfImageQuality.HIGH) }
        var selectedFormat by remember { mutableStateOf(PdfImageFormat.PNG) }
        var includeAnnotations by remember { mutableStateOf(true) }
        var enableWatermark by remember { mutableStateOf(false) }
        var watermarkText by remember { mutableStateOf("هاكر التدريس") }
        var watermarkSubtext by remember { mutableStateOf("حقوق المحتوى محفوظة") }
        var watermarkPosition by remember { mutableStateOf(WatermarkPosition.BOTTOM_RIGHT) }
        var watermarkOpacity by remember { mutableStateOf(0.40f) }

        AlertDialog(
            onDismissRequest = { if (!isExportingImage) showExportPageImageDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = EmeraldSuccess)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("استخراج الصفحة الحالية كصورة 📸", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("الصفحة الحالية: ${currentPageIndex + 1} من إجمالي $totalPages صفحة", style = MaterialTheme.typography.bodyMedium)

                    // 1. Quality Selection
                    Text("1. تحديد جودة واستبانة الصورة (DPI):", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            Triple(PdfImageQuality.ULTRA_HD, "💎 فائقة الدقة 4K (600 DPI)", "أعلى جودة للطباعة الاحترافية واللوحات الذكية"),
                            Triple(PdfImageQuality.HIGH, "🌟 ممتازة جداً (300 DPI)", "وضوح عالي جداً مع تباين ممتاز للنصوص"),
                            Triple(PdfImageQuality.MEDIUM, "⚡ متوازنة (150 DPI)", "حجم مناسب جداً للمشاركة عبر واتساب وتيليجرام"),
                            Triple(PdfImageQuality.STANDARD, "📦 خفيفة وسريعة (96 DPI)", "حجم ملف خفيف جداً للمعاينة السريعة")
                        ).forEach { (qual, lbl, desc) ->
                            val isSelected = selectedQuality == qual
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedQuality = qual }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = isSelected, onClick = { selectedQuality = qual })
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(lbl, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp)
                                        Text(desc, fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }

                    // 2. Format Selection
                    Text("2. صيغة الصورة:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            PdfImageFormat.PNG to "PNG (دقة قصوى)",
                            PdfImageFormat.JPEG to "JPG (حجم مضغوط)"
                        ).forEach { (fmt, label) ->
                            FilterChip(
                                selected = selectedFormat == fmt,
                                onClick = { selectedFormat = fmt },
                                label = { Text(label, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // 3. Fixed Watermark / Logo Card
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (enableWatermark) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, if (enableWatermark) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("وضع لوجو / علامة مائية ثابتة 🛡️", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Switch(checked = enableWatermark, onCheckedChange = { enableWatermark = it })
                            }

                            if (enableWatermark) {
                                OutlinedTextField(
                                    value = watermarkText,
                                    onValueChange = { watermarkText = it },
                                    label = { Text("اسم المعلم أو الشعار (اللوجو)", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = watermarkSubtext,
                                    onValueChange = { watermarkSubtext = it },
                                    label = { Text("النص الفرعي / الحقوق", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Text("موضع العلامة المائية:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf(
                                        WatermarkPosition.BOTTOM_RIGHT to "أسفل يمين",
                                        WatermarkPosition.BOTTOM_LEFT to "أسفل يسار",
                                        WatermarkPosition.CENTER to "وسط الصفحة",
                                        WatermarkPosition.DIAGONAL to "مائل قطري"
                                    ).forEach { (pos, lbl) ->
                                        FilterChip(
                                            selected = watermarkPosition == pos,
                                            onClick = { watermarkPosition = pos },
                                            label = { Text(lbl, fontSize = 9.sp) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("شفافية العلامة: ${(watermarkOpacity * 100).toInt()}%", fontSize = 10.sp, modifier = Modifier.width(110.dp))
                                    Slider(
                                        value = watermarkOpacity,
                                        onValueChange = { watermarkOpacity = it },
                                        valueRange = 0.15f..0.85f,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    if (isExportingImage) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text(exportStatusText.ifEmpty { "جاري استخراج وتجهيز الصورة..." }, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isExportingImage = true
                            exportStatusText = "جاري استخراج الصفحة بجودة $selectedQuality..."
                            val wmConfig = if (enableWatermark) {
                                WatermarkConfig(
                                    isEnabled = true,
                                    text = watermarkText,
                                    subText = watermarkSubtext,
                                    position = watermarkPosition,
                                    opacity = watermarkOpacity,
                                    includePageNumber = true
                                )
                            } else null

                            val file = try {
                                PdfImageExtractorHelper.exportCurrentPageAsImage(
                                    context = context,
                                    pdfFile = File(filePath),
                                    pageIndex = currentPageIndex,
                                    totalPages = totalPages,
                                    docTitle = title,
                                    quality = selectedQuality,
                                    format = selectedFormat,
                                    overlayBitmap = null,
                                    watermarkConfig = wmConfig
                                )
                            } catch (e: Exception) {
                                null
                            }
                            isExportingImage = false
                            if (file != null) {
                                showExportPageImageDialog = false
                                Toast.makeText(context, "تم حفظ الصورة بنجاح في مجلد: هاكر_التدريس 📸", Toast.LENGTH_SHORT).show()
                                PdfImageExtractorHelper.shareSingleImage(
                                    context = context,
                                    imageFile = file,
                                    title = "مشاركة صفحة ${currentPageIndex + 1} من $title"
                                )
                            } else {
                                Toast.makeText(context, "تعذر استخراج الصفحة كصورة", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !isExportingImage,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("استخراج ومشاركة الآن 📤")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExportPageImageDialog = false },
                    enabled = !isExportingImage
                ) {
                    Text("إلغاء")
                }
            }
        )
    }

    // ========================================================================
    // NEW: Convert Full PDF to Images Dialog (with Watermark & Batch Export)
    // ========================================================================
    if (showConvertFullPdfDialog) {
        var selectedQuality by remember { mutableStateOf(PdfImageQuality.MEDIUM) }
        var isConverting by remember { mutableStateOf(false) }
        var conversionProgress by remember { mutableStateOf(0f) }
        var resultFiles by remember { mutableStateOf<List<File>>(emptyList()) }
        var resultZip by remember { mutableStateOf<File?>(null) }
        var enableBatchWatermark by remember { mutableStateOf(false) }
        var watermarkText by remember { mutableStateOf("هاكر التدريس") }
        var watermarkSubtext by remember { mutableStateOf("حقوق المحتوى محفوظة") }
        var watermarkPosition by remember { mutableStateOf(WatermarkPosition.BOTTOM_RIGHT) }

        AlertDialog(
            onDismissRequest = { if (!isConverting) showConvertFullPdfDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Collections, contentDescription = null, tint = NavyPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تحويل كامل الملف إلى صور 📑", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "سيتم تحويل جميع صفحات الملف ($totalPages صفحة) إلى صور فردية عالية الدقة مع إمكانية تصديرها كألبوم أو ملف مضغوط ZIP وحفظها في مجلد التطبيق.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    // Quality Selection
                    if (resultFiles.isEmpty() && !isConverting) {
                        Text("اختر جودة واستبانة استخراج الصور:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(
                                Triple(PdfImageQuality.ULTRA_HD, "💎 فائقة 4K (600 DPI)", "أعلى جودة ممكنة"),
                                Triple(PdfImageQuality.HIGH, "🌟 عالية (300 DPI)", "ممتازة للمطبوعات"),
                                Triple(PdfImageQuality.MEDIUM, "⚡ متوازنة (150 DPI)", "سريعة وتناسب المشاركة"),
                                Triple(PdfImageQuality.STANDARD, "📦 قياسية (96 DPI)", "حجم ملف مضغوط")
                            ).forEach { (q, lbl, desc) ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selectedQuality == q) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = if (selectedQuality == q) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedQuality = q }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(selected = selectedQuality == q, onClick = { selectedQuality = q })
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Column {
                                            Text(lbl, fontSize = 11.sp, fontWeight = if (selectedQuality == q) FontWeight.Bold else FontWeight.Normal)
                                            Text(desc, fontSize = 9.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }

                        // Fixed Watermark Toggle for Batch
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (enableBatchWatermark) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, if (enableBatchWatermark) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("تطبيق علامة مائية ثابتة على كل الصفحات 🛡️", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Switch(checked = enableBatchWatermark, onCheckedChange = { enableBatchWatermark = it })
                                }
                                if (enableBatchWatermark) {
                                    OutlinedTextField(
                                        value = watermarkText,
                                        onValueChange = { watermarkText = it },
                                        label = { Text("شعار المعلم", fontSize = 10.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    if (isConverting) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LinearProgressIndicator(
                                progress = { conversionProgress },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "جاري تحويل الصفحات: ${(conversionProgress * 100).toInt()}%...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (resultFiles.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تم تحويل ${resultFiles.size} صفحة بنجاح! 🎉", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = EmeraldSuccess)
                                }
                                Text("تم حفظ جميع الصور في مجلد تطبيق: ${StudyFileManager.APP_MAIN_FOLDER}", fontSize = 11.sp, color = Color.DarkGray)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (resultFiles.isEmpty()) {
                    Button(
                        onClick = {
                            scope.launch {
                                isConverting = true
                                conversionProgress = 0f
                                val wm = if (enableBatchWatermark) {
                                    WatermarkConfig(
                                        isEnabled = true,
                                        text = watermarkText,
                                        subText = watermarkSubtext,
                                        position = watermarkPosition,
                                        includePageNumber = true
                                    )
                                } else null

                                val files = try {
                                    PdfImageExtractorHelper.convertFullPdfToImages(
                                        context = context,
                                        pdfFile = File(filePath),
                                        docTitle = title,
                                        quality = selectedQuality,
                                        format = PdfImageFormat.JPEG,
                                        watermarkConfig = wm
                                    ) { current, total ->
                                        conversionProgress = current.toFloat() / total.toFloat()
                                    }
                                } catch (e: Exception) {
                                    emptyList()
                                }
                                isConverting = false
                                resultFiles = files
                                if (files.isNotEmpty()) {
                                    val zip = try {
                                        PdfImageExtractorHelper.createZipArchive(
                                            context = context,
                                            files = files,
                                            zipBaseName = File(filePath).nameWithoutExtension
                                        )
                                    } catch (e: Exception) {
                                        null
                                    }
                                    resultZip = zip
                                } else {
                                    Toast.makeText(context, "حدث خطأ أثناء تحويل الصفحات", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isConverting,
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                    ) {
                        if (isConverting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("جاري التحويل...")
                        } else {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("بدء التحويل 🚀")
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Share Multiple
                        Button(
                            onClick = {
                                PdfImageExtractorHelper.shareMultipleImages(context, resultFiles, "مشاركة ألبوم صور $title")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مشاركة الصور 📤", fontSize = 11.sp)
                        }

                        // Share ZIP
                        if (resultZip != null) {
                            OutlinedButton(
                                onClick = {
                                    PdfImageExtractorHelper.shareZipFile(context, resultZip!!, "مشاركة أرشيف صور $title")
                                }
                            ) {
                                Icon(Icons.Filled.FolderZip, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("مشاركة ZIP 🗜️", fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConvertFullPdfDialog = false },
                    enabled = !isConverting
                ) {
                    Text(if (resultFiles.isNotEmpty()) "إغلاق" else "إلغاء")
                }
            }
        )
    }

    // ========================================================================
    // NEW: Quick Page Thumbnail Grid Bottom Sheet
    // ========================================================================
    if (showQuickPageThumbnailSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQuickPageThumbnailSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📑 معاينة صفحات الملف ($totalPages صفحة)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = {
                        showQuickPageThumbnailSheet = false
                        showJumpToPageDialog = true
                    }) {
                        Icon(Icons.Filled.Edit, contentDescription = "كتابة رقم الصفحة", tint = NavyPrimary)
                    }
                }

                // Page slider quick scrubber
                var sliderPage by remember { mutableStateOf((currentPageIndex + 1).toFloat()) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("1", fontSize = 11.sp, color = Color.Gray)
                    Slider(
                        value = sliderPage,
                        onValueChange = {
                            sliderPage = it
                            loadPdfPage((it.toInt() - 1).coerceIn(0, totalPages - 1))
                        },
                        valueRange = 1f..totalPages.toFloat(),
                        steps = (totalPages - 2).coerceAtLeast(0),
                        modifier = Modifier.weight(1f)
                    )
                    Text("$totalPages", fontSize = 11.sp, color = Color.Gray)
                }

                // Grid of pages
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(minSize = 75.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp)
                ) {
                    items(totalPages) { pageIdx ->
                        val isCurrent = pageIdx == currentPageIndex
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isCurrent) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(0.5.dp, Color.LightGray),
                            modifier = Modifier
                                .height(90.dp)
                                .clickable {
                                    loadPdfPage(pageIdx)
                                    showQuickPageThumbnailSheet = false
                                }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Gray,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "صفحة ${pageIdx + 1}",
                                    fontSize = 11.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ----------------------------------------------------------------------------
// myViewBoard Smart Tool Button
// ----------------------------------------------------------------------------

@Composable
fun MyViewBoardToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isActive) activeColor.copy(alpha = 0.18f) else Color.Transparent,
        border = if (isActive) BorderStroke(1.5.dp, activeColor) else null,
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun GeometricToolToggleCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    Card(
        onClick = onToggle,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isEnabled) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (isEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Switch(checked = isEnabled, onCheckedChange = { onToggle() })
        }
    }
}

/**
 * Draws lines with chosen pen style
 */
fun renderStyledPath(
    drawScope: DrawScope,
    points: List<Offset>,
    color: Color,
    strokeWidth: Float,
    penStyle: PenStyle
) {
    if (points.size < 2) return

    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) {
            lineTo(points[i].x, points[i].y)
        }
    }

    when (penStyle) {
        PenStyle.NORMAL -> {
            drawScope.drawPath(
                path = path,
                color = color,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
        PenStyle.HIGHLIGHTER -> {
            drawScope.drawPath(
                path = path,
                color = color.copy(alpha = 0.38f),
                style = Stroke(width = strokeWidth * 2.8f, cap = StrokeCap.Square, join = StrokeJoin.Miter)
            )
        }
        PenStyle.DASHED -> {
            drawScope.drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 12f))
                )
            )
        }
        PenStyle.NEON_GLOW -> {
            // Outer glow
            drawScope.drawPath(
                path = path,
                color = color.copy(alpha = 0.35f),
                style = Stroke(width = strokeWidth * 3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            // Bright inner core
            drawScope.drawPath(
                path = path,
                color = Color.White,
                style = Stroke(width = strokeWidth * 0.7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
        PenStyle.CALLIGRAPHY -> {
            for (i in 0 until points.size - 1) {
                val p1 = points[i]
                val p2 = points[i + 1]
                val ribbonOffset = Offset(strokeWidth * 0.7f, strokeWidth * 0.7f)
                val poly = Path().apply {
                    moveTo(p1.x, p1.y)
                    lineTo(p2.x, p2.y)
                    lineTo(p2.x + ribbonOffset.x, p2.y + ribbonOffset.y)
                    lineTo(p1.x + ribbonOffset.x, p1.y + ribbonOffset.y)
                    close()
                }
                drawScope.drawPath(poly, color, style = Fill)
            }
        }
        PenStyle.DOUBLE_LINE -> {
            val offsetDist = strokeWidth * 1.5f
            drawScope.drawPath(path, color, style = Stroke(width = strokeWidth * 0.6f))
            val path2 = Path().apply {
                moveTo(points.first().x + offsetDist, points.first().y + offsetDist)
                for (i in 1 until points.size) {
                    lineTo(points[i].x + offsetDist, points[i].y + offsetDist)
                }
            }
            drawScope.drawPath(path2, color, style = Stroke(width = strokeWidth * 0.6f))
        }
    }
}
