package com.krishimitra.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krishimitra.app.data.location.DeviceLocationProvider
import com.krishimitra.app.data.repository.FieldDigitalTwinRepository
import com.krishimitra.app.domain.language.AppLanguage
import com.krishimitra.app.domain.language.LanguageManager
import com.krishimitra.app.domain.model.FarmerField
import com.krishimitra.app.domain.model.GeoPoint
import com.krishimitra.app.domain.twin.FieldZoningEngine
import com.krishimitra.app.ui.components.FieldCanvasMap
import com.krishimitra.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFieldScreen(
    onFieldCreated: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentLanguage by LanguageManager.getInstance(context).currentLanguage.collectAsState()
    val isHindi = currentLanguage == AppLanguage.HINDI

    val repository = remember { FieldDigitalTwinRepository(context) }
    val locationProvider = remember { DeviceLocationProvider(context) }

    var fieldName by remember { mutableStateOf(if (isHindi) "मेरा गेहूं का खेत" else "My Wheat Field") }
    var selectedCropId by remember { mutableStateOf("wheat") }
    var selectedCropName by remember { mutableStateOf("Wheat") }
    var selectedCropNameHi by remember { mutableStateOf("गेहूं (Wheat)") }
    var variety by remember { mutableStateOf("HD-2967 (पूसा)") }
    var sowingDate by remember { mutableStateOf("15 Nov 2025") }

    var isCropDropdownExpanded by remember { mutableStateOf(false) }
    val cropOptions = listOf(
        Triple("wheat", "Wheat", "गेहूं (Wheat)"),
        Triple("rice", "Rice / Paddy", "धान / चावल (Paddy)"),
        Triple("mustard", "Mustard", "सरसों (Mustard)"),
        Triple("sugarcane", "Sugarcane", "गन्ना (Sugarcane)"),
        Triple("cotton", "Cotton", "कपास (Cotton)"),
        Triple("potato", "Potato", "आलू (Potato)"),
        Triple("tomato", "Tomato", "टमाटर (Tomato)"),
        Triple("chickpea", "Chickpea / Chana", "चना (Chickpea)"),
        Triple("maize", "Maize / Corn", "मक्का (Maize)")
    )

    // Interactive Boundary Points
    var boundaryPoints by remember {
        mutableStateOf(
            listOf(
                GeoPoint(28.7040, 77.1000),
                GeoPoint(28.7055, 77.1045),
                GeoPoint(28.7015, 77.1060),
                GeoPoint(28.7000, 77.1015)
            )
        )
    }

    val calculatedArea = remember(boundaryPoints) {
        FieldZoningEngine.calculatePolygonAreaAcres(boundaryPoints)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isHindi) "+ नया खेत जोड़ें (Add Field)" else "+ Add New Field",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Instruction Card
            Surface(
                color = Color(0xFFE8F5E9),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.TouchApp, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isHindi)
                            "नीचे दिए गए मानचित्र पर 3 या अधिक बिंदु टैप करके अपने खेत की मेड़ बनाएं।"
                        else
                            "Tap 3 or more points on the map below to define your field boundaries.",
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                }
            }

            // Interactive Map Canvas for drawing boundaries
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                FieldCanvasMap(
                    isEditMode = true,
                    boundaryPoints = boundaryPoints,
                    onBoundaryPointsChanged = { updated ->
                        boundaryPoints = updated
                    }
                )
            }

            // Map Drawing Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isHindi) "कुल बिंदु: ${boundaryPoints.size} | क्षेत्रफल: $calculatedArea एकड़" else "Points: ${boundaryPoints.size} | Area: $calculatedArea acres",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = GreenDark
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (boundaryPoints.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                if (boundaryPoints.isNotEmpty()) {
                                    boundaryPoints = boundaryPoints.dropLast(1)
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(if (isHindi) "पूर्ववत (Undo)" else "Undo", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = { boundaryPoints = emptyList() },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(if (isHindi) "साफ़ करें" else "Clear", fontSize = 11.sp, color = AlertRed)
                        }
                    }
                }
            }

            // Form Inputs Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.5.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Field Name
                    OutlinedTextField(
                        value = fieldName,
                        onValueChange = { fieldName = it },
                        label = { Text(if (isHindi) "खेत का नाम (Field Name)*" else "Field Name*") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Crop Selection Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = if (isHindi) selectedCropNameHi else selectedCropName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(if (isHindi) "मुख्य फसल (Primary Crop)*" else "Primary Crop*") },
                            trailingIcon = {
                                IconButton(onClick = { isCropDropdownExpanded = !isCropDropdownExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isCropDropdownExpanded = true }
                        )

                        DropdownMenu(
                            expanded = isCropDropdownExpanded,
                            onDismissRequest = { isCropDropdownExpanded = false }
                        ) {
                            cropOptions.forEach { (id, nameEn, nameHi) ->
                                DropdownMenuItem(
                                    text = { Text(if (isHindi) nameHi else nameEn) },
                                    onClick = {
                                        selectedCropId = id
                                        selectedCropName = nameEn
                                        selectedCropNameHi = nameHi
                                        isCropDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Variety & Sowing Date
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = variety,
                            onValueChange = { variety = it },
                            label = { Text(if (isHindi) "किस्म (Variety)" else "Variety") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = sowingDate,
                            onValueChange = { sowingDate = it },
                            label = { Text(if (isHindi) "बुवाई तिथि (Sowing)" else "Sowing Date") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }

            // Save Field & Generate Twin Button
            Button(
                onClick = {
                    if (fieldName.isBlank()) {
                        Toast.makeText(context, if (isHindi) "कृपया खेत का नाम दर्ज करें" else "Please enter field name", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (boundaryPoints.size < 3) {
                        Toast.makeText(context, if (isHindi) "कृपया खेत की मेड़ हेतु कम से कम 3 बिंदु बनाएं" else "Please plot at least 3 points for boundary", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val centerLat = boundaryPoints.map { it.latitude }.average()
                    val centerLon = boundaryPoints.map { it.longitude }.average()

                    val newField = FarmerField(
                        id = "FIELD-${System.currentTimeMillis() % 10000}",
                        name = fieldName.trim(),
                        cropId = selectedCropId,
                        cropName = selectedCropName,
                        cropNameHi = selectedCropNameHi,
                        variety = variety.trim().ifBlank { null },
                        areaAcres = calculatedArea,
                        sowingDate = sowingDate.trim(),
                        boundary = boundaryPoints,
                        centerLat = centerLat,
                        centerLon = centerLon
                    )

                    repository.saveField(newField)
                    Toast.makeText(
                        context,
                        if (isHindi) "✓ खेत सहेजा गया! 9 प्रबंधन ज़ोन तैयार किए गए।" else "✓ Field saved! 9 management zones generated.",
                        Toast.LENGTH_SHORT
                    ).show()
                    onFieldCreated()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isHindi) "खेत सहेजें व डिजिटल ट्विन बनाएं (Generate Twin)" else "Save Field & Generate 9 Zones",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp
                )
            }
        }
    }
}
