package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import com.example.util.DocumentScannerHelper
import com.example.util.DocumentScannerHelper.ScanFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * High-End CamScanner-Style Document Scanner for Student Homework & Class Worksheets.
 * Automatically identifies page borders, removes shadows, enhances handwriting/ink,
 * and outputs pristine document scans.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CamScannerDialog(
    initialBitmap: Bitmap? = null,
    studentName: String? = null,
    homeworkTitle: String = "واجب الحصة",
    onDismiss: () -> Unit,
    onPagesScanned: (List<Bitmap>) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Scanned pages collection
    val scannedPages = remember { mutableStateListOf<Bitmap>() }
    var currentRawBitmap by remember { mutableStateOf(initialBitmap) }
    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Step: 0 = No Image / Camera Prompt, 1 = Crop & Quad Adjustment, 2 = Filter & Enhance
    var scanStep by remember { mutableIntStateOf(if (initialBitmap != null) 1 else 0) }
    var selectedFilter by remember { mutableStateOf(ScanFilter.MAGIC_COLOR) }

    // Quad corners for perspective crop
    var corners by remember {
        mutableStateOf(
            if (initialBitmap != null) {
                DocumentScannerHelper.autoDetectDocumentCorners(initialBitmap)
            } else {
                DocumentScannerHelper.getDefaultCorners(1000f, 1400f)
            }
        )
    }

    // Camera and Gallery Launchers
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            currentRawBitmap = bmp
            corners = DocumentScannerHelper.autoDetectDocumentCorners(bmp)
            scanStep = 1
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bmp = BitmapFactory.decodeStream(stream)
                        if (bmp != null) {
                            withContext(Dispatchers.Main) {
                                currentRawBitmap = bmp
                                corners = DocumentScannerHelper.autoDetectDocumentCorners(bmp)
                                scanStep = 1
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 12.dp)
                .testTag("cam_scanner_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0284C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.DocumentScanner,
                                contentDescription = "CamScanner",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "الماسح الضوئي الذكي (CamScanner)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = EmeraldSuccess.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        "إزالة الظلال وتبييض الورقة",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF047857),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                if (!studentName.isNullOrBlank()) "تصوير واجب الطالب: $studentName" else homeworkTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_cam_scanner_btn")
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "إغلاق")
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                // Main Body Switcher
                when (scanStep) {
                    0 -> {
                        // Prompt to take photo or choose from gallery
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0284C7).copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.CameraEnhance,
                                        contentDescription = null,
                                        modifier = Modifier.size(52.dp),
                                        tint = Color(0xFF0284C7)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "مسح صفحة الواجب بدقة عالية",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "يقوم الماسح تلقائياً بقص أطراف كشكول الواجب، إزالة ظلال الإضاءة واليدين، وتوضيح خط الطالب كالمستندات المطبوعة.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(24.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = { cameraLauncher.launch(null) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(48.dp)
                                    ) {
                                        Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("التقاط بالكاميرا الآن", fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { galleryLauncher.launch("image/*") },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(48.dp)
                                    ) {
                                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("اختيار من المعرض", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // Step 1: Crop & Quad Corner Adjustment
                        val rawBmp = currentRawBitmap
                        if (rawBmp != null) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                // Tools bar for cropping
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "الخطوة 1: ضبط أطراف الصفحة وقصها",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF0284C7)
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        FilledTonalButton(
                                            onClick = {
                                                corners = DocumentScannerHelper.autoDetectDocumentCorners(rawBmp)
                                                Toast.makeText(context, "تم التحديد التلقائي للصفحة ✨", Toast.LENGTH_SHORT).show()
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("تحديد تلقائي", style = MaterialTheme.typography.labelSmall)
                                        }

                                        FilledTonalButton(
                                            onClick = {
                                                corners = DocumentScannerHelper.getDefaultCorners(rawBmp.width.toFloat(), rawBmp.height.toFloat(), 0.01f)
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Filled.CropFree, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("كامل الصورة", style = MaterialTheme.typography.labelSmall)
                                        }

                                        IconButton(
                                            onClick = {
                                                currentRawBitmap = DocumentScannerHelper.rotateBitmap(rawBmp, 90f)
                                                corners = DocumentScannerHelper.autoDetectDocumentCorners(currentRawBitmap!!)
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Filled.RotateRight, contentDescription = "تدوير 90 درجة")
                                        }
                                    }
                                }

                                // Interactive Cropper View
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF0F172A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    InteractiveQuadCropper(
                                        bitmap = rawBmp,
                                        corners = corners,
                                        onCornersChanged = { corners = it }
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Bottom Navigation Buttons for Step 1
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { scanStep = 0 },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("إعادة التصوير")
                                    }

                                    Button(
                                        onClick = {
                                            coroutineScope.launch(Dispatchers.Default) {
                                                val cropped = DocumentScannerHelper.cropPerspective(rawBmp, corners)
                                                val filtered = DocumentScannerHelper.applyFilter(cropped, selectedFilter)
                                                withContext(Dispatchers.Main) {
                                                    processedBitmap = filtered
                                                    scanStep = 2
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                                    ) {
                                        Text("قص ومعالجة الورقة", fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // Step 2: Filter Enhancement (Magic Color, B&W, Grayscale, etc.)
                        val procBmp = processedBitmap
                        if (procBmp != null) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "الخطوة 2: تحسين المستند وإزالة الظلال",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF047857)
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                processedBitmap = DocumentScannerHelper.rotateBitmap(procBmp, 90f)
                                            },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(Icons.Filled.RotateRight, contentDescription = "تدوير")
                                        }
                                    }
                                }

                                // Preview Processed Document
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1E293B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = procBmp.asImageBitmap(),
                                        contentDescription = "المستند بعد المعالجة",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(6.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Filter Selection Chips
                                Text(
                                    "فلاتر التنقية الذكية (CamScanner Magic Filters):",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ScanFilter.values().forEach { filter ->
                                        val isSelected = filter == selectedFilter
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                selectedFilter = filter
                                                val raw = currentRawBitmap
                                                if (raw != null) {
                                                    coroutineScope.launch(Dispatchers.Default) {
                                                        val cropped = DocumentScannerHelper.cropPerspective(raw, corners)
                                                        val newFiltered = DocumentScannerHelper.applyFilter(cropped, filter)
                                                        withContext(Dispatchers.Main) {
                                                            processedBitmap = newFiltered
                                                        }
                                                    }
                                                }
                                            },
                                            label = { Text(filter.displayNameAr, fontSize = 12.sp) },
                                            leadingIcon = {
                                                when (filter) {
                                                    ScanFilter.MAGIC_COLOR -> Icon(Icons.Filled.AutoFixHigh, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                                                    ScanFilter.BW_CLEAN -> Icon(Icons.Filled.Contrast, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    ScanFilter.GRAYSCALE -> Icon(Icons.Filled.FilterBAndW, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    ScanFilter.LIGHTEN -> Icon(Icons.Filled.WbSunny, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    ScanFilter.ORIGINAL -> Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                                }
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF0284C7).copy(alpha = 0.18f),
                                                selectedLabelColor = Color(0xFF0284C7)
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Scanned pages queue row (Multi-page support)
                                if (scannedPages.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "الصفحات الممسوحة (${scannedPages.size}):",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            itemsIndexed(scannedPages) { idx, pageBmp ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .border(1.dp, Color(0xFF0284C7), RoundedCornerShape(6.dp))
                                                ) {
                                                    Image(
                                                        bitmap = pageBmp.asImageBitmap(),
                                                        contentDescription = "صفحة ${idx + 1}",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                // Bottom Action Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { scanStep = 1 },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Filled.Crop, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تعديل القص", fontSize = 13.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            scannedPages.add(procBmp)
                                            scanStep = 0
                                            Toast.makeText(context, "تمت إضافة الصفحة! جاهز لتصوير صفحة أخرى 📷", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0284C7)),
                                        modifier = Modifier.weight(1.2f)
                                    ) {
                                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("+ صفحة ثانية", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            val finalPages = scannedPages.toMutableList()
                                            finalPages.add(procBmp)
                                            onPagesScanned(finalPages)
                                            onDismiss()
                                            Toast.makeText(context, "تم حفظ ${finalPages.size} صفحة بجودة CamScanner فائقة 🚀", Toast.LENGTH_LONG).show()
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                        modifier = Modifier.weight(1.5f)
                                    ) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (scannedPages.isEmpty()) "حفظ الواجب" else "حفظ الكل (${scannedPages.size + 1})", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interactive Quad Cropping Canvas with 4 corner control handles
 */
@Composable
fun InteractiveQuadCropper(
    bitmap: Bitmap,
    corners: DocumentScannerHelper.QuadCorners,
    onCornersChanged: (DocumentScannerHelper.QuadCorners) -> Unit,
    modifier: Modifier = Modifier
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    // Map bitmap coordinates to display canvas coordinates
    val scaleX = if (bitmap.width > 0 && canvasSize.width > 0) canvasSize.width / bitmap.width.toFloat() else 1f
    val scaleY = if (bitmap.height > 0 && canvasSize.height > 0) canvasSize.height / bitmap.height.toFloat() else 1f

    var activeCornerIndex by remember { mutableIntStateOf(-1) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                canvasSize = Size(coordinates.size.width.toFloat(), coordinates.size.height.toFloat())
            }
            .pointerInput(bitmap, canvasSize, corners) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val tl = Offset(corners.topLeft.x * scaleX, corners.topLeft.y * scaleY)
                        val tr = Offset(corners.topRight.x * scaleX, corners.topRight.y * scaleY)
                        val br = Offset(corners.bottomRight.x * scaleX, corners.bottomRight.y * scaleY)
                        val bl = Offset(corners.bottomLeft.x * scaleX, corners.bottomLeft.y * scaleY)

                        val touchRadius = 80f
                        activeCornerIndex = when {
                            (offset - tl).getDistance() < touchRadius -> 0
                            (offset - tr).getDistance() < touchRadius -> 1
                            (offset - br).getDistance() < touchRadius -> 2
                            (offset - bl).getDistance() < touchRadius -> 3
                            else -> -1
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (activeCornerIndex >= 0 && scaleX > 0 && scaleY > 0) {
                            val dxInBitmap = dragAmount.x / scaleX
                            val dyInBitmap = dragAmount.y / scaleY

                            val newCorners = when (activeCornerIndex) {
                                0 -> corners.copy(topLeft = PointF((corners.topLeft.x + dxInBitmap).coerceIn(0f, bitmap.width.toFloat()), (corners.topLeft.y + dyInBitmap).coerceIn(0f, bitmap.height.toFloat())))
                                1 -> corners.copy(topRight = PointF((corners.topRight.x + dxInBitmap).coerceIn(0f, bitmap.width.toFloat()), (corners.topRight.y + dyInBitmap).coerceIn(0f, bitmap.height.toFloat())))
                                2 -> corners.copy(bottomRight = PointF((corners.bottomRight.x + dxInBitmap).coerceIn(0f, bitmap.width.toFloat()), (corners.bottomRight.y + dyInBitmap).coerceIn(0f, bitmap.height.toFloat())))
                                3 -> corners.copy(bottomLeft = PointF((corners.bottomLeft.x + dxInBitmap).coerceIn(0f, bitmap.width.toFloat()), (corners.bottomLeft.y + dyInBitmap).coerceIn(0f, bitmap.height.toFloat())))
                                else -> corners
                            }
                            onCornersChanged(newCorners)
                        }
                    },
                    onDragEnd = { activeCornerIndex = -1 },
                    onDragCancel = { activeCornerIndex = -1 }
                )
            }
    ) {
        // Draw underlying bitmap
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "الورقة الأصلية",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )

        // Draw overlay polygon and corner drag handles
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (canvasSize.width > 0 && canvasSize.height > 0) {
                val tl = Offset(corners.topLeft.x * scaleX, corners.topLeft.y * scaleY)
                val tr = Offset(corners.topRight.x * scaleX, corners.topRight.y * scaleY)
                val br = Offset(corners.bottomRight.x * scaleX, corners.bottomRight.y * scaleY)
                val bl = Offset(corners.bottomLeft.x * scaleX, corners.bottomLeft.y * scaleY)

                val path = Path().apply {
                    moveTo(tl.x, tl.y)
                    lineTo(tr.x, tr.y)
                    lineTo(br.x, br.y)
                    lineTo(bl.x, bl.y)
                    close()
                }

                // Semi-transparent border fill
                drawPath(path, color = Color(0x330284C7))
                // Solid bounding outline
                drawPath(path, color = Color(0xFF00E5FF), style = Stroke(width = 3.dp.toPx()))

                // Corner circles
                val cornerRadius = 12.dp.toPx()
                val innerRadius = 5.dp.toPx()
                val cornerColor = Color(0xFF00E5FF)

                listOf(tl, tr, br, bl).forEachIndexed { idx, point ->
                    val isActive = idx == activeCornerIndex
                    drawCircle(color = Color.Black.copy(alpha = 0.4f), radius = cornerRadius + 2f, center = point)
                    drawCircle(color = if (isActive) Color(0xFFFFD600) else cornerColor, radius = if (isActive) cornerRadius * 1.3f else cornerRadius, center = point)
                    drawCircle(color = Color.White, radius = innerRadius, center = point)
                }
            }
        }
    }
}
