package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.VenueEntity
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NavyPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VenueDropdownSelector(
    selectedVenueName: String,
    onVenueSelected: (String) -> Unit,
    venues: List<VenueEntity>,
    onAddNewVenue: (VenueEntity, (VenueEntity) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "مكان الحصة / السنتر / القاعة"
) {
    var expanded by remember { mutableStateOf(false) }
    var isAddingNewVenue by remember { mutableStateOf(false) }

    // Form fields for new venue
    var newName by remember { mutableStateOf("") }
    var newAddress by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var newManager by remember { mutableStateOf("") }
    var newRentType by remember { mutableStateOf("percentage") } // percentage, per_hour, per_student, fixed_monthly
    var newRentValue by remember { mutableStateOf("") }
    var newNotes by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }

    val defaultSuggestions = listOf(
        "سنتر التفوق - قاعة 1",
        "سنتر الأوائل - القاعة الكبرى",
        "قاعة المحاضرات الرئيسية",
        "مقر المجموعة الخاص",
        "أونلاين (Zoom / Google Meet)",
        "المنزل"
    )

    val allVenueNames = remember(venues) {
        val dbNames = venues.map { it.name }.filter { it.isNotBlank() }
        (dbNames + defaultSuggestions).distinct()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (!isAddingNewVenue) {
                TextButton(
                    onClick = {
                        isAddingNewVenue = true
                        newName = if (selectedVenueName.isNotBlank() && selectedVenueName !in defaultSuggestions) selectedVenueName else ""
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("open_add_venue_form_btn")
                ) {
                    Icon(Icons.Filled.AddLocationAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = NavyPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ مكان / سنتر جديد", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = NavyPrimary)
                }
            }
        }

        // Dropdown Menu Selector
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedVenueName,
                onValueChange = { onVenueSelected(it) },
                placeholder = { Text("اختر من السناتر أو اكتب اسماً مخصصاً...") },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                singleLine = true,
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .testTag("venue_selector_input")
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                // Quick Action: Add New Venue
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Filled.AddLocationAlt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "+ إضافة سنتر / مكان جديد بقائمة الأماكن",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "إدخال العنوان، الهاتف، ونظام الإيجار والنسبة",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    onClick = {
                        expanded = false
                        isAddingNewVenue = true
                    },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                        .testTag("dropdown_item_add_new_venue")
                )

                Divider()

                // List existing registered venues from DB
                if (venues.isNotEmpty()) {
                    venues.forEach { venue ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = venue.name,
                                        fontWeight = if (selectedVenueName == venue.name) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedVenueName == venue.name) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (venue.address.isNotBlank() || venue.phone.isNotBlank()) {
                                        Text(
                                            text = listOf(venue.address, venue.phone).filter { it.isNotBlank() }.joinToString(" • "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.LocationCity,
                                    contentDescription = null,
                                    tint = if (selectedVenueName == venue.name) MaterialTheme.colorScheme.primary else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                onVenueSelected(venue.name)
                                expanded = false
                            }
                        )
                    }
                    Divider()
                }

                // Default suggestions
                defaultSuggestions.forEach { suggestion ->
                    if (venues.none { it.name == suggestion }) {
                        DropdownMenuItem(
                            text = { Text(suggestion) },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Place,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                onVenueSelected(suggestion)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        // ========================================================
        // INLINE ADD NEW VENUE FORM (يفتح تكست بوكس لإضافة بيانات المكان)
        // ========================================================
        AnimatedVisibility(
            visible = isAddingNewVenue,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.AddBusiness,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "إضافة بيانات مكان / سنتر جديد",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = { isAddingNewVenue = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "إغلاق", modifier = Modifier.size(18.dp))
                        }
                    }

                    Text(
                        "سيتم حفظ المقر تلقائياً في قاعدة البيانات وتحديده لهذه المجموعة / الحصة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = newName,
                        onValueChange = {
                            newName = it
                            nameError = it.isBlank()
                        },
                        label = { Text("اسم السنتر / القاعة *") },
                        placeholder = { Text("مثال: سنتر الأوائل - قاعة 2") },
                        isError = nameError,
                        supportingText = { if (nameError) Text("اسم السنتر مطلوب") },
                        leadingIcon = { Icon(Icons.Filled.LocationCity, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_venue_name_input")
                    )

                    OutlinedTextField(
                        value = newAddress,
                        onValueChange = { newAddress = it },
                        label = { Text("العنوان / المنطقة") },
                        placeholder = { Text("مثال: شارع الجمهورية - أمام المدرسة الثانوية") },
                        leadingIcon = { Icon(Icons.Filled.Place, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_venue_address_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newPhone,
                            onValueChange = { newPhone = it },
                            label = { Text("هاتف السنتر") },
                            placeholder = { Text("010...") },
                            leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("new_venue_phone_input")
                        )

                        OutlinedTextField(
                            value = newManager,
                            onValueChange = { newManager = it },
                            label = { Text("المسؤول / المشرف") },
                            placeholder = { Text("أ. حسام") },
                            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("new_venue_manager_input")
                        )
                    }

                    // Rent type selection
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("نظام الإيجار / الحساب مع السنتر:", style = MaterialTheme.typography.bodySmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "percentage" to "نسبة مئوية %",
                                "per_student" to "لكل طالب",
                                "fixed_monthly" to "شهري ثابت"
                            ).forEach { (type, label) ->
                                FilterChip(
                                    selected = newRentType == type,
                                    onClick = { newRentType = type },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newRentValue,
                        onValueChange = { newRentValue = it },
                        label = {
                            Text(
                                when (newRentType) {
                                    "percentage" -> "النسبة المئوية (مثال: 20%)"
                                    "per_student" -> "المبلغ لكل طالب (ج.م)"
                                    else -> "المبلغ الشهري الثابت (ج.م)"
                                }
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_venue_rent_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (newName.isBlank()) {
                                    nameError = true
                                    return@Button
                                }
                                val rentDouble = newRentValue.toDoubleOrNull() ?: 0.0
                                val venueEntity = VenueEntity(
                                    name = newName.trim(),
                                    address = newAddress.trim(),
                                    phone = newPhone.trim(),
                                    managerName = newManager.trim(),
                                    rentType = newRentType,
                                    rentValue = rentDouble,
                                    notes = newNotes.trim()
                                )
                                onAddNewVenue(venueEntity) { savedVenue ->
                                    onVenueSelected(savedVenue.name)
                                    isAddingNewVenue = false
                                    // Reset inputs
                                    newName = ""
                                    newAddress = ""
                                    newPhone = ""
                                    newManager = ""
                                    newRentValue = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.5f).testTag("save_new_venue_btn")
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حفظ واختيار المكان", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { isAddingNewVenue = false },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("cancel_new_venue_btn")
                        ) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("رجوع / إلغاء")
                        }
                    }
                }
            }
        }
    }
}
