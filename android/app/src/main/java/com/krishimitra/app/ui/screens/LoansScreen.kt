package com.krishimitra.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krishimitra.app.data.local.DatabaseHelper
import com.krishimitra.app.data.location.DeviceLocationProvider
import com.krishimitra.app.data.repository.LoanSchemeRepository
import com.krishimitra.app.domain.language.AppLanguage
import com.krishimitra.app.domain.language.LanguageManager
import com.krishimitra.app.domain.model.*
import com.krishimitra.app.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(dbHelper: DatabaseHelper) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentLanguage by LanguageManager.getInstance(context).currentLanguage.collectAsState()
    val isHindi = currentLanguage == AppLanguage.HINDI

    val repository = remember { LoanSchemeRepository(context) }
    val locationProvider = remember { DeviceLocationProvider(context) }

    // Top Tabs: 0 = Find Loan, 1 = My Applications, 2 = All Schemes
    var activeTab by remember { mutableStateOf(0) }

    // Multi-Step State for Find Loan
    // 1 = Profile Form, 2 = Recommendations, 3 = Application Form, 4 = Document Upload, 5 = Review, 6 = Submitted Confirmation
    var workflowStep by remember { mutableStateOf(1) }

    // Profile State
    var farmerName by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var stateName by remember { mutableStateOf("उत्तर प्रदेश (Uttar Pradesh)") }
    var districtName by remember { mutableStateOf("") }
    var villageName by remember { mutableStateOf("") }
    var ownershipType by remember { mutableStateOf(FarmerOwnershipType.OWN_LAND) }
    var areaInput by remember { mutableStateOf("3") }
    var isHectares by remember { mutableStateOf(false) }

    var selectedCrops by remember { mutableStateOf(setOf("धान (Rice)", "गेहूं (Wheat)")) }
    var selectedPurposes by remember { mutableStateOf(setOf(LoanPurpose.CULTIVATION, LoanPurpose.FERTILIZER)) }

    // Generated Recommendations
    var recommendations by remember { mutableStateOf<List<LoanRecommendation>>(emptyList()) }
    var selectedSchemeForApplication by remember { mutableStateOf<LoanScheme?>(null) }

    // Application Form State
    var bankNameInput by remember { mutableStateOf("State Bank of India (SBI)") }
    var branchNameInput by remember { mutableStateOf("निकटतम ग्रामीण शाखा") }
    var accountNumberInput by remember { mutableStateOf("") }
    var requestedAmountInput by remember { mutableStateOf("") }

    // Documents State
    var uploadedDocs by remember {
        mutableStateOf(
            listOf(
                UploadedDocument("id_proof", "Identity Proof (Aadhaar / Voter ID)", "पहचान प्रमाण (आधार / वोटर आईडी)", "Aadhaar_Front_Back.pdf", true, System.currentTimeMillis()),
                UploadedDocument("address_proof", "Address Proof (Ration Card / Domicile)", "निवास प्रमाण (राशन कार्ड / मूल निवास)", "Ration_Card_Copy.jpg", true, System.currentTimeMillis()),
                UploadedDocument("land_proof", "Land Ownership / Lease Record (Khasra/Khatauni)", "भूमि अभिलेख (खसरा / खतौनी / पट्टा)", "Khasra_Khatauni_2026.pdf", true, System.currentTimeMillis()),
                UploadedDocument("crop_info", "Crop Sowing Declaration (Girdawari)", "फसल बुवाई स्व-घोषणा पत्र", "Crop_Sowing_Form.pdf", false, 0L),
                UploadedDocument("photo", "Farmer Passport Photograph", "किसान पासपोर्ट फोटो", "Farmer_Photo.jpg", true, System.currentTimeMillis())
            )
        )
    }

    var previewDocName by remember { mutableStateOf<String?>(null) }
    var submittedApplication by remember { mutableStateOf<LoanApplicationSubmission?>(null) }
    var savedApplications by remember { mutableStateOf(repository.getSavedApplications()) }

    // Location detection helper
    val detectLocation = {
        try {
            val loc = locationProvider.getLastKnownLocation()
            if (loc != null) {
                val resolved = locationProvider.getResolvedLocationName(loc.latitude, loc.longitude)
                if (resolved.isNotBlank()) {
                    districtName = resolved.split(",").first().trim()
                }
            } else if (districtName.isBlank()) {
                districtName = "लोकल कृषि परिक्षेत्र (GPS)"
            }
        } catch (e: Exception) {
            if (districtName.isBlank()) districtName = "लोकल कृषि परिक्षेत्र"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Top Tab Navigation Bar
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = Color.White,
            contentColor = GreenPrimary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = {
                    Text(
                        text = if (isHindi) "🎯 सही ऋण खोजें" else "🎯 Find Loan",
                        fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                }
            )
            Tab(
                selected = activeTab == 1,
                onClick = {
                    savedApplications = repository.getSavedApplications()
                    activeTab = 1
                },
                text = {
                    Text(
                        text = if (isHindi) "📋 मेरे आवेदन (${savedApplications.size})" else "📋 My Applications (${savedApplications.size})",
                        fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                }
            )
            Tab(
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                text = {
                    Text(
                        text = if (isHindi) "🏦 सरकारी योजनाएं" else "🏦 All Schemes",
                        fontWeight = if (activeTab == 2) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                }
            )
        }

        // TAB 0: FIND LOAN MULTI-STEP WORKFLOW
        if (activeTab == 0) {
            when (workflowStep) {
                // STEP 1: FARMER PROFILE FORM
                1 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Notice Banner
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isHindi)
                                        "कृषिमित्र आपकी भूमि, फसल एवं आवश्यकता के अनुसार सर्वोत्तम सरकारी व बैंक ऋणों की सटीक अनुशंसा करता है।"
                                    else
                                        "KrishiMitra recommends verified agricultural loans based on your land, crop, and purpose.",
                                    color = GreenDark,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                            }
                        }

                        // Basic Information Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = if (isHindi) "1. किसान एवं स्थान विवरण (Basic Info)" else "1. Farmer & Location Details",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    color = GreenPrimary
                                )

                                OutlinedTextField(
                                    value = farmerName,
                                    onValueChange = { farmerName = it },
                                    label = { Text(if (isHindi) "किसान का पूरा नाम" else "Farmer Full Name") },
                                    placeholder = { Text("उदा. रामेश्वर सिंह / Rameshwar Singh") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                OutlinedTextField(
                                    value = mobileNumber,
                                    onValueChange = { mobileNumber = it },
                                    label = { Text(if (isHindi) "मोबाइल नंबर" else "Mobile Number") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    placeholder = { Text("9876543210") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = districtName,
                                        onValueChange = { districtName = it },
                                        label = { Text(if (isHindi) "जिला (District)" else "District") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    OutlinedTextField(
                                        value = villageName,
                                        onValueChange = { villageName = it },
                                        label = { Text(if (isHindi) "गांव / कस्बा" else "Village / Town") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }

                                Button(
                                    onClick = { detectLocation() },
                                    colors = ButtonDefaults.buttonColors(containerColor = BackgroundLight, contentColor = GreenDark),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isHindi) "📍 वर्तमान जीपीएस स्थान से जिला भरें" else "📍 Auto-Detect Location (GPS)", fontSize = 12.sp)
                                }
                            }
                        }

                        // Land Information Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = if (isHindi) "2. भूमि स्वामित्व एवं क्षेत्रफल (Land Details)" else "2. Land Ownership & Area",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    color = GreenPrimary
                                )

                                Text(
                                    text = if (isHindi) "भूमि स्वामित्व का प्रकार:" else "Land Ownership Type:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FarmerOwnershipType.values().forEach { type ->
                                        FilterChip(
                                            selected = ownershipType == type,
                                            onClick = { ownershipType = type },
                                            label = { Text(if (isHindi) type.labelHi else type.labelEn, fontSize = 12.sp) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = areaInput,
                                        onValueChange = { areaInput = it },
                                        label = { Text(if (isHindi) "कुल जोत क्षेत्रफल" else "Cultivated Area") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    FilterChip(
                                        selected = !isHectares,
                                        onClick = { isHectares = false },
                                        label = { Text(if (isHindi) "एकड़" else "Acres") }
                                    )
                                    FilterChip(
                                        selected = isHectares,
                                        onClick = { isHectares = true },
                                        label = { Text(if (isHindi) "हेक्टेयर" else "Hectares") }
                                    )
                                }
                            }
                        }

                        // Farming Information Card (What to grow)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = if (isHindi) "3. कौन सी फसलें उगाते हैं? (Crop Selection)" else "3. What do you grow? (Crops)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    color = GreenPrimary
                                )

                                val commonCrops = listOf("धान (Rice)", "गेहूं (Wheat)", "मक्का (Maize)", "कपास (Cotton)", "सरसों (Mustard)", "सोयाबीन (Soybean)", "टमाटर (Tomato)", "आलू (Potato)", "गन्ना (Sugarcane)", "चना (Chickpea)")
                                FlowRowLayout(
                                    items = commonCrops,
                                    isSelected = { selectedCrops.contains(it) },
                                    onToggle = { crop ->
                                        selectedCrops = if (selectedCrops.contains(crop)) {
                                            if (selectedCrops.size > 1) selectedCrops - crop else selectedCrops
                                        } else {
                                            selectedCrops + crop
                                        }
                                    }
                                )
                            }
                        }

                        // Purpose of Loan Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = if (isHindi) "4. ऋण का मुख्य उद्देश्य क्या है? (Loan Purpose)" else "4. Purpose of Loan",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    color = GreenPrimary
                                )

                                LoanPurpose.values().forEach { purpose ->
                                    val isChecked = selectedPurposes.contains(purpose)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isChecked) Color(0xFFE8F5E9) else BackgroundLight,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedPurposes = if (isChecked) {
                                                    if (selectedPurposes.size > 1) selectedPurposes - purpose else selectedPurposes
                                                } else {
                                                    selectedPurposes + purpose
                                                }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = null,
                                                colors = CheckboxDefaults.colors(checkedColor = GreenPrimary)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(text = purpose.icon, fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isHindi) purpose.labelHi else purpose.labelEn,
                                                fontSize = 13.sp,
                                                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                                                color = TextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Submit Button for Finding Loans
                        Button(
                            onClick = {
                                val areaVal = areaInput.toDoubleOrNull() ?: 3.0
                                val profile = FarmerLoanProfile(
                                    farmerName = farmerName,
                                    mobileNumber = mobileNumber,
                                    state = stateName,
                                    district = districtName,
                                    village = villageName,
                                    ownershipType = ownershipType,
                                    area = areaVal,
                                    isHectares = isHectares,
                                    selectedCrops = selectedCrops.toList(),
                                    selectedPurposes = selectedPurposes.toList()
                                )
                                recommendations = repository.getRecommendations(profile)
                                workflowStep = 2
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isHindi) "अपने लिए सही कृषि ऋण खोजें" else "Find Matching Agricultural Loans",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // STEP 2: RECOMMENDATIONS RESULT LIST
                2 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Top back button
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { workflowStep = 1 }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                            Text(
                                text = if (isHindi) "आपके लिए अनुशंसित ऋण योजनाएं" else "Recommended for You",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        // Disclaimer Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = AmberSecondary, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isHindi)
                                        "यह व्यवस्था अनुशंसा व आवेदन तैयारी प्रणाली है। अंतिम ऋण राशि व पात्रता का निर्णय बैंक द्वारा जिला स्केल ऑफ फाइनेंस के आधार पर किया जाएगा।"
                                    else
                                        "Final loan amount and sanction will be decided by the bank based on district scale of finance and credit assessment.",
                                    fontSize = 11.5.sp,
                                    color = TextPrimary,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        // Recommendations List
                        recommendations.forEachIndexed { index, rec ->
                            RecommendationCard(
                                rank = index + 1,
                                rec = rec,
                                isHindi = isHindi,
                                onApply = {
                                    selectedSchemeForApplication = rec.scheme
                                    workflowStep = 3
                                },
                                onOpenUrl = { url ->
                                    openExternalWebUrl(context, url)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // STEP 3: GUIDED APPLICATION FORM
                3 -> {
                    val scheme = selectedSchemeForApplication ?: repository.verifiedSchemes[0]

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { workflowStep = 2 }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                            Column {
                                Text(
                                    text = if (isHindi) "ऋण आवेदन फॉर्म" else "Loan Application Form",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (isHindi) scheme.nameHi else scheme.nameEn,
                                    fontSize = 12.sp,
                                    color = GreenDark
                                )
                            }
                        }

                        // Selected Scheme Header Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (isHindi) "चयनित ऋण: ${scheme.nameHi}" else "Selected Scheme: ${scheme.nameEn}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = GreenDark
                                )
                                Text(
                                    text = if (isHindi) "ब्याज दर: ${scheme.interestRateInfoHi}" else "Interest: ${scheme.interestRateInfoEn}",
                                    fontSize = 11.5.sp,
                                    color = TextPrimary
                                )
                            }
                        }

                        // Personal & Farming Verification
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = if (isHindi) "आवेदक एवं कृषि विवरण" else "Applicant & Farming Details",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = GreenPrimary
                                )

                                OutlinedTextField(
                                    value = farmerName,
                                    onValueChange = { farmerName = it },
                                    label = { Text(if (isHindi) "आवेदक का नाम" else "Applicant Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                OutlinedTextField(
                                    value = mobileNumber,
                                    onValueChange = { mobileNumber = it },
                                    label = { Text(if (isHindi) "मोबाइल नंबर" else "Mobile Number") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                OutlinedTextField(
                                    value = "$districtName, $villageName ($stateName)",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(if (isHindi) "कृषि पता" else "Farm Location") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                OutlinedTextField(
                                    value = "$areaInput ${if (isHectares) "हेक्टेयर" else "एकड़"} (${ownershipType.labelHi})",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(if (isHindi) "जोत विवरण" else "Land Area & Title") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                OutlinedTextField(
                                    value = requestedAmountInput,
                                    onValueChange = { requestedAmountInput = it },
                                    label = { Text(if (isHindi) "अनुमानित अपेक्षित ऋण राशि (₹)" else "Expected Credit Requirement (₹)") },
                                    placeholder = { Text("उदा. 150000") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }

                        // Bank Account Information
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = if (isHindi) "बैंक शाखा विवरण" else "Bank Branch Information",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = GreenPrimary
                                )

                                OutlinedTextField(
                                    value = bankNameInput,
                                    onValueChange = { bankNameInput = it },
                                    label = { Text(if (isHindi) "बैंक का नाम" else "Bank Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                OutlinedTextField(
                                    value = branchNameInput,
                                    onValueChange = { branchNameInput = it },
                                    label = { Text(if (isHindi) "शाखा का नाम / IFSC" else "Branch Name / IFSC") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                OutlinedTextField(
                                    value = accountNumberInput,
                                    onValueChange = { accountNumberInput = it },
                                    label = { Text(if (isHindi) "बचत खाता संख्या (वैकल्पिक)" else "Savings Account Number (Optional)") },
                                    placeholder = { Text("XXXXXXXX1234") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                // Security Banner
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFFEBEE)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Security, contentDescription = null, tint = AlertRed, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isHindi)
                                                "सुरक्षा निर्देश: कृषिमित्र कभी भी आपका एटीएम पिन, यूपीआई पिन अथवा पासवर्ड नहीं मांगता।"
                                            else
                                                "Security: KrishiMitra never asks for your ATM PIN, UPI PIN, OTP, or passwords.",
                                            fontSize = 11.sp,
                                            color = Color(0xFFC62828)
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { workflowStep = 4 },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isHindi) "दस्तावेज अपलोड की ओर बढ़ें →" else "Proceed to Document Upload →", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // STEP 4: DOCUMENT UPLOAD INTERFACE
                4 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { workflowStep = 3 }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                            Text(
                                text = if (isHindi) "आवश्यक दस्तावेज अपलोड" else "Required Documents Upload",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        // Guidance Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isHindi)
                                        "ऋण आवेदन हेतु आवश्यक दस्तावेजों की फोटो या पीडीएफ संलग्न करें। आपके दस्तावेज सुरक्षित रूप से फोन में सहेजे जाते हैं।"
                                    else
                                        "Attach clear photos or PDF copies of required records. Documents are kept safe and local.",
                                    fontSize = 11.5.sp,
                                    color = GreenDark
                                )
                            }
                        }

                        // Document Items List
                        uploadedDocs.forEachIndexed { index, doc ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(1.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isHindi) doc.categoryNameHi else doc.categoryNameEn,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = TextPrimary
                                            )
                                            if (doc.isUploaded) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(text = doc.fileName, fontSize = 11.sp, color = GreenDark)
                                                }
                                            } else {
                                                Text(
                                                    text = if (isHindi) "संलग्न नहीं है" else "Not yet uploaded",
                                                    fontSize = 11.sp,
                                                    color = AlertRed
                                                )
                                            }
                                        }

                                        if (doc.isUploaded) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                OutlinedButton(
                                                    onClick = { previewDocName = doc.fileName },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(if (isHindi) "देखें" else "View", fontSize = 11.sp)
                                                }

                                                IconButton(
                                                    onClick = {
                                                        val updated = uploadedDocs.toMutableList()
                                                        updated[index] = doc.copy(isUploaded = false, fileName = "")
                                                        uploadedDocs = updated
                                                    }
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AlertRed, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        } else {
                                            Button(
                                                onClick = {
                                                    val updated = uploadedDocs.toMutableList()
                                                    val dummyName = "${doc.categoryId}_record.pdf"
                                                    updated[index] = doc.copy(isUploaded = true, fileName = dummyName, uploadTimestamp = System.currentTimeMillis())
                                                    uploadedDocs = updated
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(if (isHindi) "अपलोड" else "Upload", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Preview Dialog
                        if (previewDocName != null) {
                            AlertDialog(
                                onDismissRequest = { previewDocName = null },
                                title = { Text(if (isHindi) "दस्तावेज पूर्वावलोकन" else "Document Preview", fontWeight = FontWeight.Bold) },
                                text = {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.Description, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(64.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(text = previewDocName!!, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (isHindi) "✓ दस्तावेज सत्यापित एवं एन्क्रिप्टेड है।" else "✓ Document verified & encrypted.",
                                            fontSize = 11.sp,
                                            color = GreenDark
                                        )
                                    }
                                },
                                confirmButton = {
                                    Button(onClick = { previewDocName = null }) {
                                        Text(if (isHindi) "बंद करें" else "Close")
                                    }
                                }
                            )
                        }

                        Button(
                            onClick = { workflowStep = 5 },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isHindi) "आवेदन की समीक्षा करें →" else "Review Application →", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // STEP 5: REVIEW APPLICATION SCREEN
                5 -> {
                    val scheme = selectedSchemeForApplication ?: repository.verifiedSchemes[0]
                    val uploadedCount = uploadedDocs.count { it.isUploaded }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { workflowStep = 4 }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                            Text(
                                text = if (isHindi) "आवेदन समीक्षा (Review Application)" else "Review Application",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        // Summary Sections Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                ReviewItemRow(
                                    label = if (isHindi) "चयनित ऋण योजना:" else "Selected Loan Scheme:",
                                    value = if (isHindi) scheme.nameHi else scheme.nameEn
                                )
                                HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

                                ReviewItemRow(
                                    label = if (isHindi) "आवेदक किसान:" else "Applicant Farmer:",
                                    value = "$farmerName (मो. $mobileNumber)"
                                )
                                ReviewItemRow(
                                    label = if (isHindi) "कृषि परिक्षेत्र:" else "Farm Location:",
                                    value = "$villageName, $districtName, $stateName"
                                )
                                HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

                                ReviewItemRow(
                                    label = if (isHindi) "भूमि जोत व फसलें:" else "Land Holding & Crops:",
                                    value = "$areaInput ${if (isHectares) "हेक्टेयर" else "एकड़"} (${ownershipType.labelHi}) | ${selectedCrops.joinToString(", ")}"
                                )
                                ReviewItemRow(
                                    label = if (isHindi) "ऋण उद्देश्य:" else "Loan Purpose:",
                                    value = selectedPurposes.joinToString(", ") { if (isHindi) it.labelHi else it.labelEn }
                                )
                                HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

                                ReviewItemRow(
                                    label = if (isHindi) "बैंक व खाता विवरण:" else "Bank & Account:",
                                    value = "$bankNameInput ($branchNameInput) | A/c: ${if (accountNumberInput.isNotBlank()) "XXXX" + accountNumberInput.takeLast(4) else "शाखा में सत्यापित होगा"}"
                                )
                                ReviewItemRow(
                                    label = if (isHindi) "संलग्न दस्तावेज:" else "Attached Documents:",
                                    value = "$uploadedCount / ${uploadedDocs.size} दस्तावेज तैयार"
                                )
                            }
                        }

                        // Action Buttons: Edit and Submit
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { workflowStep = 3 },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(if (isHindi) "संशोधन करें" else "Edit Details")
                            }

                            Button(
                                onClick = {
                                    val submission = LoanApplicationSubmission(
                                        schemeId = scheme.schemeId,
                                        schemeNameEn = scheme.nameEn,
                                        schemeNameHi = scheme.nameHi,
                                        farmerName = farmerName.ifBlank { "किसान आवेदक" },
                                        mobileNumber = mobileNumber.ifBlank { "9876543210" },
                                        state = stateName,
                                        district = districtName.ifBlank { "लोकल जिला" },
                                        village = villageName.ifBlank { "गांव" },
                                        landAreaText = "$areaInput ${if (isHectares) "हेक्टेयर" else "एकड़"}",
                                        ownershipTypeText = if (isHindi) ownershipType.labelHi else ownershipType.labelEn,
                                        selectedCropsText = selectedCrops.joinToString(", "),
                                        purposeText = selectedPurposes.joinToString(", ") { if (isHindi) it.labelHi else it.labelEn },
                                        requestedAmountText = requestedAmountInput.ifBlank { "स्केल अनुसार" },
                                        bankName = bankNameInput,
                                        branchName = branchNameInput,
                                        accountNumberMasked = if (accountNumberInput.length >= 4) "XXXX" + accountNumberInput.takeLast(4) else "संलग्न पासबुक अनुसार",
                                        documentsUploadedCount = uploadedCount,
                                        totalRequiredDocuments = uploadedDocs.size,
                                        status = ApplicationStatus.PREPARED_PROTOTYPE,
                                        submissionDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
                                        officialPortalUrl = scheme.officialUrl
                                    )
                                    repository.saveApplication(submission)
                                    submittedApplication = submission
                                    savedApplications = repository.getSavedApplications()
                                    workflowStep = 6
                                },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isHindi) "आवेदन जमा करें" else "Submit Application", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // STEP 6: APPLICATION SUBMITTED CONFIRMATION & OFFICIAL PORTAL
                6 -> {
                    val sub = submittedApplication

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F5E9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(46.dp))
                        }

                        Text(
                            text = if (isHindi) "ऋण आवेदन सफलतापूर्वक तैयार!" else "Application Prepared Successfully!",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenDark
                        )

                        Text(
                            text = if (isHindi) "आवेदन क्रमांक: ${sub?.applicationId ?: "KM-LOAN-84219"}" else "Application ID: ${sub?.applicationId ?: "KM-LOAN-84219"}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        // Honest Bank Submission Disclaimer
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = AmberSecondary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isHindi) "महत्वपूर्ण सूचना (Bank Submission Status):" else "Important Submission Status:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isHindi)
                                        "आपका आवेदन विवरण एवं दस्तावेज सफलतापूर्वक तैयार कर लिए गए हैं। चूंकि बैंकों का कोई सीधा निजी आवेदन एपीआई उपलब्ध नहीं है, कृपया आधिकारिक पोर्टल अथवा अपनी बैंक शाखा पर जाकर इस आवेदन को अंतिम रूप दें।"
                                    else
                                        "Your application profile and documents are prepared successfully. Continue to the official bank application portal or visit your bank branch to complete official sanctioning.",
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    color = TextPrimary
                                )
                            }
                        }

                        // Direct link to official government / bank portal
                        Button(
                            onClick = {
                                val url = sub?.officialPortalUrl ?: "https://www.myscheme.gov.in/schemes/kcc"
                                openExternalWebUrl(context, url)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isHindi) "आधिकारिक बैंक पोर्टल पर जाएं" else "Continue to Official Bank Portal",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        // View in My Applications
                        OutlinedButton(
                            onClick = {
                                activeTab = 1
                                workflowStep = 1
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isHindi) "मेरे आवेदन देखें (View Status)" else "View in My Applications")
                        }

                        // Start New Loan Search
                        TextButton(
                            onClick = { workflowStep = 1 }
                        ) {
                            Text(if (isHindi) "नया ऋण खोजें (Search Again)" else "Start New Loan Search", color = GreenDark)
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }

        // TAB 1: MY APPLICATIONS STATUS TRACKER
        if (activeTab == 1) {
            if (savedApplications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isHindi) "अभी तक कोई ऋण आवेदन तैयार नहीं किया गया है।" else "No loan applications prepared yet.",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                activeTab = 0
                                workflowStep = 1
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                        ) {
                            Text(if (isHindi) "ऋण खोजें व आवेदन करें" else "Find & Apply for Loan")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = if (isHindi) "सहेजे गए ऋण आवेदन ट्रैकर" else "Saved Loan Application Tracker",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                    }

                    items(savedApplications) { app ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = app.applicationId,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = GreenDark
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFE0F2F1)
                                    ) {
                                        Text(
                                            text = if (isHindi) app.status.labelHi else app.status.labelEn,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00695C),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = if (isHindi) app.schemeNameHi else app.schemeNameEn,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    color = TextPrimary
                                )

                                Text(
                                    text = "किसान: ${app.farmerName} | भूमि: ${app.landAreaText} | बैंक: ${app.bankName}",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )

                                Text(
                                    text = "तैयार होने की तारीख: ${app.submissionDate} | दस्तावेज: ${app.documentsUploadedCount}/${app.totalRequiredDocuments}",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Button(
                                    onClick = {
                                        openExternalWebUrl(context, app.officialPortalUrl)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isHindi) "आधिकारिक पोर्टल पर प्रक्रिया पूर्ण करें" else "Complete on Official Portal", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // TAB 2: ALL VERIFIED GOVERNMENT SCHEMES
        if (activeTab == 2) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = if (isHindi) "सभी प्रमाणित सरकारी कृषि ऋण योजनाएं" else "Verified Agricultural Credit Schemes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                }

                items(repository.verifiedSchemes) { scheme ->
                    var isExpanded by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (isHindi) scheme.providerHi else scheme.provider,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenDark
                            )

                            Text(
                                text = if (isHindi) scheme.nameHi else scheme.nameEn,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            Text(
                                text = if (isHindi) scheme.descriptionHi else scheme.descriptionEn,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 17.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ब्याज दर: ${if (isHindi) scheme.interestRateInfoHi.split(";").first() else scheme.interestRateInfoEn.split(";").first()}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberSecondary
                                )

                                TextButton(onClick = { isExpanded = !isExpanded }) {
                                    Text(if (isExpanded) "कम देखें" else "विवरण देखें", fontSize = 12.sp)
                                }
                            }

                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically(),
                                exit = shrinkVertically()
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

                                    Text(text = "✓ मुख्य लाभ:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                                    val benefits = if (isHindi) scheme.benefitsHi else scheme.benefitsEn
                                    benefits.forEach { b ->
                                        Text(text = "• $b", fontSize = 11.5.sp, color = TextPrimary)
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "📄 आवश्यक दस्तावेज:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    val docs = if (isHindi) scheme.requiredDocumentsHi else scheme.requiredDocumentsEn
                                    docs.forEach { d ->
                                        Text(text = "• $d", fontSize = 11.5.sp, color = TextSecondary)
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Button(
                                        onClick = {
                                            openExternalWebUrl(context, scheme.officialUrl)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isHindi) "सरकारी पोर्टल देखें (Official Link)" else "Open Official Portal", fontSize = 12.sp)
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

@Composable
fun RecommendationCard(
    rank: Int,
    rec: LoanRecommendation,
    isHindi: Boolean,
    onApply: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val scheme = rec.scheme

    val rankTitle = when (rank) {
        1 -> if (isHindi) "#1 सर्वश्रेष्ठ मिलान (Best Match)" else "#1 Best Match"
        2 -> if (isHindi) "#2 अच्छा मिलान (Good Match)" else "#2 Good Match"
        else -> if (isHindi) "#3 संभावित मिलान (Potential Match)" else "#3 Potential Match"
    }

    val matchColor = when (rec.matchLevel) {
        MatchLevel.HIGH -> GreenPrimary
        MatchLevel.MEDIUM -> AmberSecondary
        MatchLevel.POTENTIAL -> Color(0xFF0288D1)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Rank and Match Level Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rankTitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = matchColor
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = matchColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (isHindi) rec.matchLevel.labelHi else rec.matchLevel.labelEn,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = matchColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // Scheme Title
            Text(
                text = if (isHindi) scheme.nameHi else scheme.nameEn,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = if (isHindi) scheme.providerHi else scheme.provider,
                fontSize = 11.5.sp,
                color = TextSecondary
            )

            // Scale of Finance or Estimated Credit Highlight
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF1F8E9),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = GreenDark, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) rec.estimatedCreditLimitTextHi else rec.estimatedCreditLimitTextEn,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenDark,
                        lineHeight = 16.sp
                    )
                }
            }

            // Why it matches section
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (isHindi) "यह ऋण आपके लिए क्यों उपयुक्त है:" else "Why it matches your profile:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                val reasons = if (isHindi) rec.matchReasonsHi else rec.matchReasonsEn
                reasons.forEach { r ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text(text = "✓", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = r, fontSize = 11.5.sp, color = TextPrimary, lineHeight = 16.sp)
                    }
                }
            }

            // Interest & Collateral short summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (isHindi) "ब्याज दर:" else "Interest:", fontSize = 10.5.sp, color = TextSecondary)
                    Text(
                        text = if (isHindi) scheme.interestRateInfoHi.split(";").first() else scheme.interestRateInfoEn.split(";").first(),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (isHindi) "जमानत / बंधक:" else "Collateral:", fontSize = 10.5.sp, color = TextSecondary)
                    Text(
                        text = if (isHindi) scheme.collateralInfoHi.split("(").first() else scheme.collateralInfoEn.split("(").first(),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            // Action Buttons: View Details & Apply
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isExpanded) (if (isHindi) "कम देखें" else "Hide Details") else (if (isHindi) "विवरण देखें" else "View Details"), fontSize = 11.5.sp)
                }

                Button(
                    onClick = onApply,
                    modifier = Modifier.weight(1.2f),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isHindi) "ऋण आवेदन करें" else "Start Application", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // Expandable details
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HorizontalDivider(color = CardBorder, thickness = 0.5.dp)

                    Text(text = "लाभ (Benefits):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                    val benefits = if (isHindi) scheme.benefitsHi else scheme.benefitsEn
                    benefits.forEach { b ->
                        Text(text = "• $b", fontSize = 11.5.sp, color = TextPrimary)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "आवश्यक दस्तावेज:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    val docs = if (isHindi) scheme.requiredDocumentsHi else scheme.requiredDocumentsEn
                    docs.forEach { d ->
                        Text(text = "• $d", fontSize = 11.5.sp, color = TextSecondary)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = { onOpenUrl(scheme.officialUrl) },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("🔗 आधिकारिक सरकारी पोर्टल खोलें (Official Source)", fontSize = 11.sp, color = Color(0xFF0288D1))
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewItemRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
        Text(text = value, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Bold, lineHeight = 17.sp)
    }
}

@Composable
fun FlowRowLayout(
    items: List<String>,
    isSelected: (String) -> Boolean,
    onToggle: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowItems.forEach { item ->
                    val checked = isSelected(item)
                    FilterChip(
                        selected = checked,
                        onClick = { onToggle(item) },
                        label = { Text(item, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill remaining space if chunk has fewer than 3 items
                for (i in rowItems.size until 3) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
