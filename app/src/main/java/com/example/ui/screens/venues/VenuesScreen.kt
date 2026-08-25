package com.example.ui.screens.venues

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.VenueEntity
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VenuesScreen(
    viewModel: VenuesViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showAddEditDialog by remember { mutableStateOf(false) }
    var showMapPickerDialog by remember { mutableStateOf(false) }
    var selectedVenueForEdit by remember { mutableStateOf<VenueEntity?>(null) }
    var venueToDelete by remember { mutableStateOf<VenueEntity?>(null) }

    val filteredVenues = remember(state.venues, state.searchQuery) {
        if (state.searchQuery.isEmpty()) state.venues
        else state.venues.filter {
            it.venue.name.contains(state.searchQuery, ignoreCase = true) ||
                    it.venue.address.contains(state.searchQuery, ignoreCase = true) ||
                    it.venue.managerName.contains(state.searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "أماكن وقاعات الدروس (السناتر)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedVenueForEdit = null
                    showAddEditDialog = true
                },
                containerColor = NavyPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_venue_fab")
            ) {
                Icon(Icons.Filled.AddLocationAlt, contentDescription = "إضافة سنتر/مكان جديد")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("بحث عن سنتر، قاعة، أو عنوان...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color.Gray) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "مسح")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .testTag("venues_search_field")
            )

            if (filteredVenues.isEmpty()) {
                EmptyStateCard(
                    title = "لا توجد أماكن دروس مضافة",
                    subtitle = "أضف سناتر وقاعات التدريس لتنظيم المجموعات والحسابات والنسب المالية بكل دقة",
                    icon = Icons.Filled.LocationCity,
                    actionText = "إضافة سنتر الآن",
                    onActionClick = {
                        selectedVenueForEdit = null
                        showAddEditDialog = true
                    },
                    modifier = Modifier.padding(top = 40.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp, top = 6.dp)
                ) {
                    items(filteredVenues, key = { it.venue.id }) { item ->
                        VenueCard(
                            item = item,
                            onEdit = {
                                selectedVenueForEdit = item.venue
                                showAddEditDialog = true
                            },
                            onDelete = { venueToDelete = item.venue },
                            onCall = {
                                if (item.venue.phone.isNotEmpty()) {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.venue.phone}"))
                                    context.startActivity(intent)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddEditDialog) {
        AddEditVenueDialog(
            initialVenue = selectedVenueForEdit,
            onDismiss = { showAddEditDialog = false },
            onSave = { venue ->
                viewModel.addOrUpdateVenue(venue)
                showAddEditDialog = false
            }
        )
    }

    venueToDelete?.let { v ->
        AlertDialog(
            onDismissRequest = { venueToDelete = null },
            title = { Text("حذف مكان الدرس") },
            text = { Text("هل أنت متأكد من حذف ${v.name}؟ لن يتم حذف المجموعات المرتبطة به.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteVenue(v)
                        venueToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = CrimsonError)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { venueToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun VenueCard(
    item: VenueWithStats,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCall: () -> Unit
) {
    val venue = item.venue
    val context = LocalContext.current
    val rentLabel = when (venue.rentType) {
        "percentage" -> "نسبة السنتر: ${venue.rentValue.toInt()}%"
        "per_hour" -> "إيجار بالساعة: ${venue.rentValue.toInt()} ج.م"
        "per_student" -> "لكل طالب: ${venue.rentValue.toInt()} ج.م"
        "fixed_monthly" -> "إيجار شهري: ${venue.rentValue.toInt()} ج.م"
        else -> "النسبة: ${venue.rentValue}"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NavyPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = NavyPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = venue.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (venue.address.isNotEmpty()) {
                            Text(
                                text = venue.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }

                    Row {
                        if (item.venue.latitude != 0.0 && item.venue.longitude != 0.0) {
                            IconButton(onClick = {
                                val uri = Uri.parse("geo:${item.venue.latitude},${item.venue.longitude}?q=${item.venue.latitude},${item.venue.longitude}(${Uri.encode(item.venue.name)})")
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${item.venue.latitude},${item.venue.longitude}")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                                }
                            }) {
                                Icon(Icons.Filled.Directions, contentDescription = "الاتجاهات والموقع على الخريطة", tint = Color(0xFF2563EB))
                            }
                        }
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Filled.Edit, contentDescription = "تعديل", tint = NavyPrimary)
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف", tint = CrimsonError)
                        }
                    }
                }

                if (venue.latitude != 0.0 && venue.longitude != 0.0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.Place, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(14.dp))
                        Text(
                            text = "إحداثيات GPS: %.4f, %.4f".format(venue.latitude, venue.longitude),
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldSuccess
                        )
                    }
                }

            Spacer(modifier = Modifier.height(10.dp))

            // Rent Badge & Stats Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text(rentLabel, style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(16.dp), tint = AmberGoldDark) }
                )
                AssistChip(
                    onClick = {},
                    label = { Text("${item.groupCount} مجموعات", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(Icons.Filled.Class, contentDescription = null, modifier = Modifier.size(16.dp), tint = NavyPrimary) }
                )
            }

            if (venue.managerName.isNotEmpty() || venue.phone.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "المسؤول: ${venue.managerName.ifEmpty { "غير محدد" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )
                    if (venue.phone.isNotEmpty()) {
                        FilledTonalButton(
                            onClick = onCall,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(venue.phone, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditVenueDialog(
    initialVenue: VenueEntity?,
    onDismiss: () -> Unit,
    onSave: (VenueEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialVenue?.name ?: "") }
    var address by remember { mutableStateOf(initialVenue?.address ?: "") }
    var phone by remember { mutableStateOf(initialVenue?.phone ?: "") }
    var managerName by remember { mutableStateOf(initialVenue?.managerName ?: "") }
    var rentType by remember { mutableStateOf(initialVenue?.rentType ?: "percentage") }
    var rentValue by remember { mutableStateOf(if (initialVenue != null && initialVenue.rentValue > 0) initialVenue.rentValue.toString() else "") }
    var notes by remember { mutableStateOf(initialVenue?.notes ?: "") }
    var latitude by remember { mutableDoubleStateOf(initialVenue?.latitude ?: 0.0) }
    var longitude by remember { mutableDoubleStateOf(initialVenue?.longitude ?: 0.0) }
    var showMapPicker by remember { mutableStateOf(false) }

    if (showMapPicker) {
        VenueLocationPickerDialog(
            initialLat = latitude,
            initialLng = longitude,
            initialName = name,
            initialAddress = address,
            onDismiss = { showMapPicker = false },
            onLocationConfirmed = { loc ->
                latitude = loc.latitude
                longitude = loc.longitude
                if (loc.venueName.isNotBlank() && name.isBlank()) {
                    name = loc.venueName
                }
                if (loc.address.isNotBlank()) {
                    address = loc.address
                }
                showMapPicker = false
            }
        )
    }

    val rentTypes = listOf(
        Pair("percentage", "نسبة مئوية (%)"),
        Pair("per_hour", "إيجار بالساعة"),
        Pair("per_student", "مبلغ لكل طالب"),
        Pair("fixed_monthly", "إيجار شهري ثابت")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialVenue == null) "إضافة سنتر / مقر درس" else "تعديل بيانات المقر",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Map Picker Trigger Button
                OutlinedButton(
                    onClick = { showMapPicker = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (latitude != 0.0) EmeraldSuccess.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("open_map_picker_button")
                ) {
                    Icon(
                        if (latitude != 0.0) Icons.Filled.CheckCircle else Icons.Filled.Map,
                        contentDescription = null,
                        tint = if (latitude != 0.0) EmeraldSuccess else NavyPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (latitude != 0.0) "تم تحديد الموقع على الخريطة (تعديل 📍)" else "تحديد الموقع بدقة من الخريطة 🗺️",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (latitude != 0.0) EmeraldSuccess else NavyPrimary
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم السنتر أو القاعة *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("venue_name_input")
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("العنوان / المنطقة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = managerName,
                        onValueChange = { managerName = it },
                        label = { Text("اسم المسؤول") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("هاتف التواصل") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Rent Type Selection
                Text("نظام المحاسبة / الإيجار:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rentTypes.take(2).forEach { (key, label) ->
                        FilterChip(
                            selected = rentType == key,
                            onClick = { rentType = key },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rentTypes.drop(2).forEach { (key, label) ->
                        FilterChip(
                            selected = rentType == key,
                            onClick = { rentType = key },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = rentValue,
                    onValueChange = { rentValue = it },
                    label = { Text(if (rentType == "percentage") "النسبة المئوية (%)" else "القيمة المالية (ج.م)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val v = initialVenue?.copy(
                            name = name.trim(),
                            address = address.trim(),
                            phone = phone.trim(),
                            managerName = managerName.trim(),
                            rentType = rentType,
                            rentValue = rentValue.toDoubleOrNull() ?: 0.0,
                            latitude = latitude,
                            longitude = longitude,
                            notes = notes.trim()
                        ) ?: VenueEntity(
                            name = name.trim(),
                            address = address.trim(),
                            phone = phone.trim(),
                            managerName = managerName.trim(),
                            rentType = rentType,
                            rentValue = rentValue.toDoubleOrNull() ?: 0.0,
                            latitude = latitude,
                            longitude = longitude,
                            notes = notes.trim()
                        )
                        onSave(v)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
