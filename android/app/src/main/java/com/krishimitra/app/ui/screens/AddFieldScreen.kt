package com.krishimitra.app.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.krishimitra.app.ui.components.GoogleMapLikeView
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

    // Map Facility View Mode: Satellite (Google-like) vs Canvas
    var useSatelliteMap by remember { mutableStateOf(true) }

    // User GPS location & resolved address
    var userLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var userLocationName by remember { mutableStateOf<String?>(null) }
    var isLocating by remember { mutableStateOf(false) }

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

    // Function to acquire GPS
    val acquireLocation = {
        isLocating = true
        val loc = locationProvider.getLastKnownLocation()
        if (loc != null) {
            val pt = GeoPoint(loc.latitude, loc.longitude)
            userLocation = pt
            userLocationName = locationProvider.getResolvedLocationName(loc.latitude, loc.longitude)
            isLocating = false
            Toast.makeText(
                context,
                if (isHindi) "✓ वर्तमान स्थान मिला: ${userLocationName ?: ""}" else "✓ Location found: ${userLocationName ?: ""}",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            isLocating = false
            Toast.makeText(
                context,
                if (isHindi) "GPS स्थान प्राप्त नहीं हुआ। कृपया डिवाइस लोकेशन ऑन करें।" else "Could not detect GPS. Please check location settings.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Location Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            acquireLocation()
        } else {
            Toast.makeText(
                context,
                if (isHindi) "स्थान की अनुमति आवश्यक है" else "Location permission is required",
                Toast.LENGTH_SHORT
            ).show()
        }
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
                actions = {
                    // Toggle Map Engine: Satellite vs Vector Canvas
                    TextButton(onClick = { useSatelliteMap = !useSatelliteMap }) {
                        Text(
                            text = if (useSatelliteMap) "🗺️ उपग्रह" else "🎨 कैनवास",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )
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
            // Location Bar with GPS Button
            Surface(
                color = Color(0xFFE8F5E9),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = GreenPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (userLocation != null)
                                    (userLocationName ?: "${userLocation?.latitude?.toString()?.take(7)}, ${userLocation?.longitude?.toString()?.take(7)}")
                                else
                                    if (isHindi) "GPS स्थान उपलब्ध नहीं" else "GPS Location Not Set",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = if (isHindi) "मानचित्र को अपने खेत पर केंद्रित करें" else "Auto-center map on your farm",
                                fontSize = 10.5.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (locationProvider.hasLocationPermission()) {
                                acquireLocation()
                            } else {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        if (isLocating) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isHindi) "स्थान लें" else "My GPS", fontSize = 11.5.sp)
                        }
                    }
                }
            }

            // Quick One-Tap Action: Generate Field around GPS
            if (userLocation != null) {
                Surface(
                    color = Color(0xFFE3F2FD),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isHindi) "📍 वर्तमान स्थान के चारों ओर खेत बनाएं (~2 एकड़)" else "📍 Plot ~2 acre field around GPS",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0D47A1)
                        )
                        OutlinedButton(
                            onClick = {
                                val lat = userLocation!!.latitude
                                val lon = userLocation!!.longitude
                                val dLat = 0.00075
                                val dLon = 0.00075
                                boundaryPoints = listOf(
                                    GeoPoint(lat + dLat, lon - dLon),
                                    GeoPoint(lat + dLat, lon + dLon),
                                    GeoPoint(lat - dLat, lon + dLon),
                                    GeoPoint(lat - dLat, lon - dLon)
                                )
                                Toast.makeText(
                                    context,
                                    if (isHindi) "✓ वर्तमान स्थान पर खेत की सीमा तैयार!" else "✓ Boundary created around your location!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(if (isHindi) "यहाँ बनाएं" else "Plot Here", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Map Facility Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isHindi) "खेत की सीमा (उपग्रह पर मेड़ बिंदु टैप करें):" else "Field Boundary (Tap to set vertices):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (useSatelliteMap) Color(0xFF2E7D32) else Color(0xFF546E7A)
                ) {
                    Text(
                        text = if (useSatelliteMap) "🛰️ Esri Satellite" else "🎨 Vector Canvas",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Interactive Map Facility Container (Height 290dp)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                if (useSatelliteMap) {
                    // Google-like Satellite Map Facility
                    GoogleMapLikeView(
                        modifier = Modifier.fillMaxSize(),
                        initialCenterLat = userLocation?.latitude ?: 28.7040,
                        initialCenterLon = userLocation?.longitude ?: 77.1025,
                        initialZoom = 16,
                        isEditMode = true,
                        boundaryPoints = boundaryPoints,
                        userLocation = userLocation,
                        onBoundaryPointsChanged = { updated ->
                            boundaryPoints = updated
                        }
                    )
                } else {
                    // Lightweight Vector Canvas Map
                    FieldCanvasMap(
                        isEditMode = true,
                        boundaryPoints = boundaryPoints,
                        onBoundaryPointsChanged = { updated ->
                            boundaryPoints = updated
                        }
                    )
                }
            }

            // Map Controls & Real-Time Acreage Readout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isHindi) "बिंदु: ${boundaryPoints.size} | क्षेत्रफल: $calculatedArea एकड़" else "Points: ${boundaryPoints.size} | Area: $calculatedArea acres",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
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
                            Text(if (isHindi) "पूर्ववत" else "Undo", fontSize = 11.sp)
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
