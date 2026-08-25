package com.example.ui.screens.attendance

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.local.entity.StudentEntity
import com.example.util.QrBarcodeUtils
import java.util.concurrent.Executors

@Composable
fun QuickQrScannerDialog(
    groupStudents: List<StudentEntity>,
    allStudents: List<StudentEntity> = emptyList(),
    onStudentScanned: (StudentEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedTab by remember { mutableStateOf(0) } // 0 = Live Camera, 1 = Manual/Search
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    var scannedInput by remember { mutableStateOf("") }
    var lastScannedStudent by remember { mutableStateOf<StudentEntity?>(null) }
    var lastScannedCode by remember { mutableStateOf("") }
    var lastScannedTime by remember { mutableStateOf(0L) }
    var scannedSuccessCount by remember { mutableStateOf(0) }
    var scanErrorMessage by remember { mutableStateOf<String?>(null) }
    var isFlashlightOn by remember { mutableStateOf(false) }
    var cameraControlRef by remember { mutableStateOf<CameraControl?>(null) }

    fun playFeedback() {
        try {
            val toneG = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            toneG.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        } catch (_: Exception) {}

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(100)
                }
            }
        } catch (_: Exception) {}
    }

    fun processCode(code: String): Boolean {
        val raw = code.trim()
        if (raw.isEmpty()) return false

        val now = System.currentTimeMillis()
        if (raw == lastScannedCode && (now - lastScannedTime) < 2000) {
            // Avoid duplicate scan within 2 seconds
            return true
        }

        // Clean query terms
        val trimmed = raw
        val cleanDigits = raw.filter { it.isDigit() }
        val pool = if (groupStudents.isNotEmpty()) groupStudents else allStudents
        val fullPool = (groupStudents + allStudents).distinctBy { it.id }

        // Find candidate in pool
        var matchedStudent = pool.find { s ->
            s.barcodeCode.equals(trimmed, ignoreCase = true) ||
            (cleanDigits.isNotEmpty() && s.barcodeCode.filter { it.isDigit() } == cleanDigits) ||
            s.id.toString() == trimmed ||
            (cleanDigits.isNotEmpty() && s.id.toString() == cleanDigits) ||
            "STD-${s.id}".equals(trimmed, ignoreCase = true) ||
            "STD-${1000 + s.id}".equals(trimmed, ignoreCase = true) ||
            (s.phone.isNotBlank() && (s.phone.replace(" ", "") == trimmed.replace(" ", "") || (cleanDigits.length >= 8 && s.phone.contains(cleanDigits)))) ||
            (s.parentPhone.isNotBlank() && (s.parentPhone.replace(" ", "") == trimmed.replace(" ", "") || (cleanDigits.length >= 8 && s.parentPhone.contains(cleanDigits)))) ||
            (trimmed.length >= 3 && s.name.contains(trimmed, ignoreCase = true))
        }

        // If not in current group pool, search all students
        if (matchedStudent == null && fullPool.isNotEmpty()) {
            matchedStudent = fullPool.find { s ->
                s.barcodeCode.equals(trimmed, ignoreCase = true) ||
                (cleanDigits.isNotEmpty() && s.barcodeCode.filter { it.isDigit() } == cleanDigits) ||
                s.id.toString() == trimmed ||
                (cleanDigits.isNotEmpty() && s.id.toString() == cleanDigits) ||
                "STD-${s.id}".equals(trimmed, ignoreCase = true) ||
                "STD-${1000 + s.id}".equals(trimmed, ignoreCase = true) ||
                (s.phone.isNotBlank() && s.phone.replace(" ", "") == trimmed.replace(" ", "")) ||
                (s.parentPhone.isNotBlank() && s.parentPhone.replace(" ", "") == trimmed.replace(" ", "")) ||
                (trimmed.length >= 3 && s.name.contains(trimmed, ignoreCase = true))
            }
        }

        if (matchedStudent != null) {
            playFeedback()
            lastScannedCode = raw
            lastScannedTime = now
            lastScannedStudent = matchedStudent
            scannedSuccessCount++
            scanErrorMessage = null
            onStudentScanned(matchedStudent)
            scannedInput = ""
            return true
        } else {
            scanErrorMessage = "لم يتم العثور على طالب بالكود أو الرقم ($trimmed)"
            return false
        }
    }

    // Image Gallery Picker for QR Scanning from photo
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        val decodedText = QrBarcodeUtils.decodeBitmap(bitmap)
                        if (!decodedText.isNullOrBlank()) {
                            processCode(decodedText)
                        } else {
                            scanErrorMessage = "لم يتم التعرف على كود QR واضح في الصورة"
                        }
                    }
                }
            } catch (e: Exception) {
                scanErrorMessage = "تعذر قراءة الصورة: ${e.message}"
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .testTag("quick_qr_scanner_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.QrCodeScanner,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "تسجيل الحضور بالباركود / QR",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                if (scannedSuccessCount > 0) "تم تسجيل حضور $scannedSuccessCount طالب بنجاح ✅" else "المسح الفوري المباشر",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (scannedSuccessCount > 0) Color(0xFF047857) else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mode Tabs (Camera vs Manual Search)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("كاميرا المسح المباشر", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("البحث وكتابة الكود", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Body content based on tab
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (selectedTab == 0) {
                        // Live Camera Scanner
                        if (!hasCameraPermission) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Filled.CameraAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "يلزم تفعيل إذن الكاميرا لمسح الـ QR والباركود",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "يمكنك مسح كروت الطلاب وكارنيهات الحضور تلقائياً فور توجيه الكاميرا للكود.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("السماح باستخدام الكاميرا")
                                }
                            }
                        } else {
                            // Camera Preview View
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black)
                            ) {
                                var lastFrameTime by remember { mutableStateOf(0L) }

                                AndroidView(
                                    factory = { ctx ->
                                        val previewView = PreviewView(ctx).apply {
                                            layoutParams = ViewGroup.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                        }

                                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                        val cameraExecutor = Executors.newSingleThreadExecutor()

                                        cameraProviderFuture.addListener({
                                            val cameraProvider = cameraProviderFuture.get()

                                            val preview = Preview.Builder().build().also {
                                                it.setSurfaceProvider(previewView.surfaceProvider)
                                            }

                                            val imageAnalysis = ImageAnalysis.Builder()
                                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                                .build()

                                            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                                val currentTime = System.currentTimeMillis()
                                                // Throttle decode to every 250ms for high performance
                                                if (currentTime - lastFrameTime > 250) {
                                                    lastFrameTime = currentTime
                                                    val decoded = QrBarcodeUtils.decodeCameraFrame(imageProxy)
                                                    if (!decoded.isNullOrBlank()) {
                                                        // Post to main thread
                                                        previewView.post {
                                                            processCode(decoded)
                                                        }
                                                    }
                                                }
                                                imageProxy.close()
                                            }

                                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                            try {
                                                cameraProvider.unbindAll()
                                                val cam = cameraProvider.bindToLifecycle(
                                                    lifecycleOwner,
                                                    cameraSelector,
                                                    preview,
                                                    imageAnalysis
                                                )
                                                cameraControlRef = cam.cameraControl
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }, ContextCompat.getMainExecutor(ctx))

                                        previewView
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Scanner Frame Overlay
                                Box(
                                    modifier = Modifier
                                        .size(240.dp)
                                        .align(Alignment.Center)
                                        .border(2.5.dp, Color(0xFF38BDF8), RoundedCornerShape(16.dp))
                                )

                                // Overlay Controls (Flashlight & Gallery)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.TopCenter)
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    FilledTonalIconButton(
                                        onClick = {
                                            isFlashlightOn = !isFlashlightOn
                                            cameraControlRef?.enableTorch(isFlashlightOn)
                                        },
                                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                                            containerColor = if (isFlashlightOn) Color(0xFFFBBF24) else Color.Black.copy(alpha = 0.5f),
                                            contentColor = if (isFlashlightOn) Color.Black else Color.White
                                        )
                                    ) {
                                        Icon(
                                            if (isFlashlightOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                                            contentDescription = "الفلاش"
                                        )
                                    }

                                    FilledTonalIconButton(
                                        onClick = { galleryLauncher.launch("image/*") },
                                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                                            containerColor = Color.Black.copy(alpha = 0.5f),
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(Icons.Filled.PhotoLibrary, contentDescription = "مسح من صورة بالمعرض")
                                    }
                                }

                                Text(
                                    "وجّه الكاميرا نحو كود الطالب أو الباركود",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 12.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    } else {
                        // Manual / Quick Search Tab
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = scannedInput,
                                onValueChange = {
                                    scannedInput = it
                                    if (it.endsWith("\n")) {
                                        processCode(it.trim())
                                    }
                                },
                                label = { Text("ابحث بالاسم، الكود، أو رقم الهاتف") },
                                placeholder = { Text("مثال: STD-1001 أو 01206150946") },
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (scannedInput.isNotEmpty()) {
                                        IconButton(onClick = { processCode(scannedInput) }) {
                                            Icon(Icons.Filled.CheckCircle, contentDescription = "تأكيد", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("scanner_search_input")
                            )

                            // Quick Filtered Student List for 1-Tap Attendance
                            val filteredStudents = remember(groupStudents, scannedInput) {
                                if (scannedInput.isBlank()) groupStudents
                                else groupStudents.filter {
                                    it.name.contains(scannedInput.trim(), ignoreCase = true) ||
                                    it.phone.contains(scannedInput.trim()) ||
                                    it.barcodeCode.contains(scannedInput.trim(), ignoreCase = true) ||
                                    "STD-${it.id}".contains(scannedInput.trim(), ignoreCase = true)
                                }
                            }

                            Text(
                                "اضغط على أي طالب لتسجيل حضوره مباشرة:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(filteredStudents, key = { it.id }) { student ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                processCode(student.barcodeCode.ifEmpty { "STD-${student.id}" })
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            student.name.take(1),
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(student.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                                    Text(
                                                        student.barcodeCode.ifEmpty { "STD-${student.id}" } + if (student.phone.isNotEmpty()) " • ${student.phone}" else "",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Button(
                                                onClick = { processCode(student.barcodeCode.ifEmpty { "STD-${student.id}" }) },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("تسجيل", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Success / Error status banners
                AnimatedVisibility(visible = lastScannedStudent != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "تم تسجيل الحضور بنجاح! ✅",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF047857)
                                )
                                Text(
                                    "الطالب: ${lastScannedStudent?.name} (${lastScannedStudent?.barcodeCode?.ifEmpty { "STD-${lastScannedStudent?.id}" }})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF065F46)
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = scanErrorMessage != null) {
                    Text(
                        text = scanErrorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Done Button
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("close_scanner_btn")
                ) {
                    Icon(Icons.Filled.Done, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تم والانتهاء (إغلاق الماسح)")
                }
            }
        }
    }
}
