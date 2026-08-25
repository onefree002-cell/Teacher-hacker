package com.example.ui.screens.venues

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.ui.theme.AmberGold
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NavyPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class SelectedMapLocation(
    val latitude: Double,
    val longitude: Double,
    val venueName: String = "",
    val address: String = "",
    val cityOrArea: String = "",
    val hasAutoData: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VenueLocationPickerDialog(
    initialLat: Double = 30.0444, // Default Cairo
    initialLng: Double = 31.2357,
    initialName: String = "",
    initialAddress: String = "",
    onDismiss: () -> Unit,
    onLocationConfirmed: (SelectedMapLocation) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentLat by remember { mutableStateOf(if (initialLat != 0.0) initialLat else 30.0444) }
    var currentLng by remember { mutableStateOf(if (initialLng != 0.0) initialLng else 31.2357) }
    var venueNameInput by remember { mutableStateOf(initialName) }
    var addressInput by remember { mutableStateOf(initialAddress) }
    var areaInput by remember { mutableStateOf("") }
    var isGeocoding by remember { mutableStateOf(false) }
    var hasAutoData by remember { mutableStateOf(false) }
    var mapZoom by remember { mutableFloatStateOf(1.0f) }
    var mapCenterOffset by remember { mutableStateOf(Offset.Zero) }

    // Preset popular study centers / landmarks
    val presetLocations = listOf(
        Triple("سنتر الأوائل التعليمي", 30.0500, 31.2400),
        Triple("أكاديمية التفوق والنخبة", 30.0620, 31.2800),
        Triple("سنتر المستقبل للدروس", 30.0150, 31.2050),
        Triple("قاعة الإبداع والتميز", 30.0800, 31.3200),
        Triple("مجمع السناتر التعليمية", 30.0330, 31.2150)
    )

    // Function to perform Reverse Geocoding
    fun reverseGeocode(lat: Double, lng: Double) {
        coroutineScope.launch(Dispatchers.IO) {
            isGeocoding = true
            try {
                if (Geocoder.isPresent()) {
                    val geocoder = Geocoder(context, Locale("ar"))
                    val addresses: List<Address>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        var result: List<Address>? = null
                        geocoder.getFromLocation(lat, lng, 1) { list -> result = list }
                        result
                    } else {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocation(lat, lng, 1)
                    }

                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        val feature = addr.featureName ?: ""
                        val thoroughfare = addr.thoroughfare ?: ""
                        val locality = addr.locality ?: addr.adminArea ?: ""
                        val full = addr.getAddressLine(0) ?: "$thoroughfare $locality"

                        withContext(Dispatchers.Main) {
                            hasAutoData = true
                            if (venueNameInput.isBlank() && feature.isNotBlank() && feature != thoroughfare) {
                                venueNameInput = "سنتر $feature"
                            }
                            addressInput = full
                            areaInput = locality
                            Toast.makeText(context, "تم جلب بيانات الموقع تلقائياً بنجاح 📍", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            hasAutoData = false
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        hasAutoData = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hasAutoData = false
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isGeocoding = false
                }
            }
        }
    }

    // Function to fetch current device GPS location
    fun fetchDeviceLocation() {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            Toast.makeText(context, "يرجى منح إذن الوصول للموقع لتحديد مكانك بدقة 📍", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val gpsLocation = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val networkLocation = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val bestLocation: Location? = gpsLocation ?: networkLocation

            if (bestLocation != null) {
                currentLat = bestLocation.latitude
                currentLng = bestLocation.longitude
                reverseGeocode(currentLat, currentLng)
                Toast.makeText(context, "تم تحديد موقعك الحالي بنجاح 🛰️", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "جاري تحديد الموقع، يرجى التأكد من تشغيل GPS والإنترنت", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر جلب الموقع: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    // Permission launcher for Location
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchDeviceLocation()
        } else {
            Toast.makeText(context, "تم رفض إذن الموقع. يمكنك اختيار المكان يدوياً من الخريطة.", Toast.LENGTH_LONG).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "تحديد موقع السنتر من الخريطة 🗺️",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "اضغط على الخريطة أو استخدم زر تحديد مكاني",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "إغلاق")
                    }
                }

                // Mandatory Internet Advisory Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF3C7),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Wifi, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(18.dp))
                        Text(
                            text = "تنويه: جلب بيانات العناوين تلقائياً والخرائط التفاعلية تتطلب اتصالاً نشطاً بالإنترنت وGPS.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF92400E)
                        )
                    }
                }

                // Interactive Map Canvas with Pin, Radar, and Gestures
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFE2E8F0))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                mapCenterOffset += dragAmount
                                // Shift lat/lng slightly proportional to drag
                                currentLat -= (dragAmount.y * 0.0001) / mapZoom
                                currentLng += (dragAmount.x * 0.0001) / mapZoom
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures { tapOffset ->
                                // Tap shifts location
                                reverseGeocode(currentLat, currentLng)
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawMapStyling(
                            centerOffset = mapCenterOffset,
                            zoom = mapZoom,
                            primaryColor = Color(0xFF3B82F6),
                            roadColor = Color(0xFFCBD5E1),
                            waterColor = Color(0xFF93C5FD)
                        )
                    }

                    // Center Marker Pin (Always centered on target)
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = "الموقع المحدد",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier
                                .size(44.dp)
                                .shadow(6.dp, CircleShape)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.75f),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "%.4f, %.4f".format(currentLat, currentLng),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Floating Map Controls: My Location GPS + Zoom + External Google Maps
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // GPS "تحديد مكاني" button
                        FilledIconButton(
                            onClick = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = NavyPrimary),
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("my_location_gps_button")
                        ) {
                            Icon(Icons.Filled.MyLocation, contentDescription = "تحديد مكاني GPS", tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        // Open in Google Maps App
                        FilledIconButton(
                            onClick = {
                                val uri = Uri.parse("geo:$currentLat,$currentLng?q=$currentLat,$currentLng(${Uri.encode(venueNameInput.ifBlank { "مقر الدرس" })})")
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$currentLat,$currentLng")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Filled.OpenInNew, contentDescription = "فتح في خرائط جوجل", modifier = Modifier.size(18.dp))
                        }

                        // Zoom In
                        FilledIconButton(
                            onClick = { mapZoom = (mapZoom * 1.25f).coerceAtMost(3.0f) },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "تكبير", modifier = Modifier.size(16.dp))
                        }

                        // Zoom Out
                        FilledIconButton(
                            onClick = { mapZoom = (mapZoom / 1.25f).coerceAtLeast(0.5f) },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = "تصغير", modifier = Modifier.size(16.dp))
                        }
                    }

                    // Loading Geocoding indicator
                    if (isGeocoding) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.7f),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                Text("جاري جلب بيانات العنوان...", color = Color.White, fontSize = 10.sp)
                            }
                        }
                    }
                }

                // Preset Popular Venues Quick Chips
                Text("أماكن ومجمعات دراسية شهيرة:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presetLocations) { (pName, pLat, pLng) ->
                        SuggestionChip(
                            onClick = {
                                currentLat = pLat
                                currentLng = pLng
                                venueNameInput = pName
                                addressInput = "القاهرة - $pName"
                                hasAutoData = true
                            },
                            label = { Text(pName, fontSize = 11.sp) },
                            icon = { Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(14.dp), tint = NavyPrimary) }
                        )
                    }
                }

                // Auto-filled or Manual Input Form
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!hasAutoData && venueNameInput.isBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "✍️ لم يتم العثور على اسم تلقائي للموقع، يمكنك إدخال اسم السنتر والعنوان يدوياً:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = venueNameInput,
                        onValueChange = { venueNameInput = it },
                        label = { Text("اسم السنتر / القاعة *") },
                        placeholder = { Text("مثال: سنتر الأوائل - قاعة 1") },
                        trailingIcon = {
                            if (hasAutoData) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = "بيانات تلقائية", tint = AmberGold)
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("map_venue_name_input")
                    )

                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        label = { Text("العنوان التفصيلي / الشارع") },
                        placeholder = { Text("مثال: شارع الجمهورية - بجوار المحطة") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("map_venue_address_input")
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء")
                    }

                    Button(
                        onClick = {
                            if (venueNameInput.isBlank()) {
                                Toast.makeText(context, "يرجى كتابة اسم السنتر أو القاعة أولاً", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            onLocationConfirmed(
                                SelectedMapLocation(
                                    latitude = currentLat,
                                    longitude = currentLng,
                                    venueName = venueNameInput,
                                    address = addressInput,
                                    cityOrArea = areaInput,
                                    hasAutoData = hasAutoData
                                )
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("confirm_map_location_button")
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تأكيد وحفظ الموقع 📍")
                    }
                }
            }
        }
    }
}

// Canvas Visual Simulation of Maps (Roads, Blocks, Parks, Water)
private fun DrawScope.drawMapStyling(
    centerOffset: Offset,
    zoom: Float,
    primaryColor: Color,
    roadColor: Color,
    waterColor: Color
) {
    val w = size.width
    val h = size.height
    val cx = (w / 2f) + centerOffset.x
    val cy = (h / 2f) + centerOffset.y

    // Background terrain
    drawRect(Color(0xFFF1F5F9))

    // Water body (river / lake curve)
    val waterPath = Path().apply {
        moveTo(0f, cy - 80f * zoom)
        cubicTo(
            cx - 100f * zoom, cy - 120f * zoom,
            cx + 100f * zoom, cy - 40f * zoom,
            w, cy - 100f * zoom
        )
        lineTo(w, cy - 40f * zoom)
        cubicTo(
            cx + 100f * zoom, cy + 20f * zoom,
            cx - 100f * zoom, cy - 60f * zoom,
            0f, cy - 20f * zoom
        )
        close()
    }
    drawPath(waterPath, waterColor)

    // City Blocks
    val blockColor = Color(0xFFE2E8F0)
    for (i in -3..3) {
        for (j in -3..3) {
            val bx = cx + (i * 90f * zoom)
            val by = cy + (j * 70f * zoom)
            drawRoundRect(
                color = blockColor,
                topLeft = Offset(bx + 6f, by + 6f),
                size = androidx.compose.ui.geometry.Size(78f * zoom, 58f * zoom),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f * zoom, 6f * zoom)
            )
        }
    }

    // Grid Roads
    val roadStroke = Stroke(width = 8f * zoom)
    for (i in -4..4) {
        val y = cy + (i * 70f * zoom)
        drawLine(roadColor, Offset(0f, y), Offset(w, y), strokeWidth = 10f * zoom)
    }
    for (j in -4..4) {
        val x = cx + (j * 90f * zoom)
        drawLine(roadColor, Offset(x, 0f), Offset(x, h), strokeWidth = 10f * zoom)
    }

    // Main Avenue
    drawLine(
        color = Color(0xFFFBBF24),
        start = Offset(0f, cy),
        end = Offset(w, cy),
        strokeWidth = 14f * zoom
    )

    // Radar ring around center
    drawCircle(
        color = primaryColor.copy(alpha = 0.15f),
        radius = 50f * zoom,
        center = Offset(w / 2f, h / 2f)
    )
    drawCircle(
        color = primaryColor.copy(alpha = 0.35f),
        radius = 25f * zoom,
        center = Offset(w / 2f, h / 2f),
        style = Stroke(width = 2f)
    )
}
