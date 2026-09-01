package com.krishimitra.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.krishimitra.app.domain.language.AppLanguage
import com.krishimitra.app.domain.language.LanguageManager
import com.krishimitra.app.ui.theme.*

data class EquipmentItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val titleHi: String,
    val titleEn: String,
    val category: String, // "TRACTOR", "ROTAVATOR", "HARVESTER", "SEED_DRILL", "SPRAYER", "PUMP", "ALL"
    val ownerNameHi: String,
    val ownerNameEn: String,
    val villageHi: String,
    val villageEn: String,
    val distanceKm: Double,
    val rateAmount: Int,
    val rateUnitHi: String,
    val rateUnitEn: String,
    val includesFuelAndDriver: Boolean,
    val phone: String,
    val isAvailable: Boolean = true,
    val isUserListing: Boolean = false,
    val descriptionHi: String,
    val descriptionEn: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipmentRentalScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentLanguage by LanguageManager.getInstance(context).currentLanguage.collectAsState()
    val isHindi = currentLanguage == AppLanguage.HINDI

    var selectedTab by remember { mutableStateOf(0) } // 0: Find, 1: Rent Out, 2: Govt CHC
    var selectedCategory by remember { mutableStateOf("ALL") }

    // Booking Dialog state
    var bookingItem by remember { mutableStateOf<EquipmentItem?>(null) }
    var bookingHours by remember { mutableStateOf("4") }
    var bookingDate by remember { mutableStateOf("कल (Tomorrow)") }
    var showBookingSuccessDialog by remember { mutableStateOf(false) }

    // Initial demo machinery listings nearby
    val defaultListings = remember {
        listOf(
            EquipmentItem(
                id = "eq_1",
                titleHi = "महिंद्रा 575 DI (45 HP ट्रैक्टर)",
                titleEn = "Mahindra 575 DI (45 HP Tractor)",
                category = "TRACTOR",
                ownerNameHi = "रामेश्वर शर्मा",
                ownerNameEn = "Rameshwar Sharma",
                villageHi = "रामपुर (2.4 km दूर)",
                villageEn = "Rampur (2.4 km away)",
                distanceKm = 2.4,
                rateAmount = 850,
                rateUnitHi = "प्रति घंटा",
                rateUnitEn = "per hour",
                includesFuelAndDriver = true,
                phone = "9876543210",
                descriptionHi = "ट्रॉली, कल्टीवेटर सहित उपलब्ध। अनुभवी ड्राइवर व डीजल शामिल।",
                descriptionEn = "Available with trolley & cultivator. Experienced driver & diesel included."
            ),
            EquipmentItem(
                id = "eq_2",
                titleHi = "शक्तिमान 7-फीट रोटावेटर",
                titleEn = "Shaktiman 7-Feet Rotavator",
                category = "ROTAVATOR",
                ownerNameHi = "कुलदीप सिंह",
                ownerNameEn = "Kuldeep Singh",
                villageHi = "किशनगढ़ (3.8 km दूर)",
                villageEn = "Kishangarh (3.8 km away)",
                distanceKm = 3.8,
                rateAmount = 650,
                rateUnitHi = "प्रति घंटा",
                rateUnitEn = "per hour",
                includesFuelAndDriver = true,
                phone = "9812345678",
                descriptionHi = "धान व गेहूं की बुवाई हेतु मिट्टी को भुरभुरा बनाने में सर्वोत्तम।",
                descriptionEn = "Best for seedbed preparation & soil pulverization for wheat and paddy."
            ),
            EquipmentItem(
                id = "eq_3",
                titleHi = "क्लास क्रॉप टाइगर 30 (कंबाइन हार्वेस्टर)",
                titleEn = "Claas Crop Tiger 30 (Combine Harvester)",
                category = "HARVESTER",
                ownerNameHi = "हरप्रीत सिंह",
                ownerNameEn = "Harpreet Singh",
                villageHi = "सराय (5.2 km दूर)",
                villageEn = "Sarai (5.2 km away)",
                distanceKm = 5.2,
                rateAmount = 1800,
                rateUnitHi = "प्रति एकड़",
                rateUnitEn = "per acre",
                includesFuelAndDriver = true,
                phone = "9823456789",
                descriptionHi = "धान और गेहूं की तेज कटाई व मड़ाई, अनाज का न्यूनतम नुकसान।",
                descriptionEn = "Fast harvesting & threshing for paddy and wheat with minimal grain loss."
            ),
            EquipmentItem(
                id = "eq_4",
                titleHi = "फील्डकिंग जीरो टिल मल्टीक्रॉप सीड ड्रिल",
                titleEn = "Fieldking Zero-Till Multi-Crop Seed Drill",
                category = "SEED_DRILL",
                ownerNameHi = "दिनेश यादव",
                ownerNameEn = "Dinesh Yadav",
                villageHi = "मंगोलपुर (1.9 km दूर)",
                villageEn = "Mangolpur (1.9 km away)",
                distanceKm = 1.9,
                rateAmount = 450,
                rateUnitHi = "प्रति घंटा",
                rateUnitEn = "per hour",
                includesFuelAndDriver = false,
                phone = "9834567890",
                descriptionHi = "बिना जुताई सीधी बुवाई, पराली में भी कारगर, खाद व बीज एक साथ।",
                descriptionEn = "Direct sowing in stubble without pre-tillage, saves fuel and moisture."
            ),
            EquipmentItem(
                id = "eq_5",
                titleHi = "किसान एग्री-ड्रोन स्प्रेयर (10L टैंक)",
                titleEn = "Kisan Agri-Drone Sprayer (10L Tank)",
                category = "SPRAYER",
                ownerNameHi = "अमन वर्मा (प्रमाणित ड्रोन पायलट)",
                ownerNameEn = "Aman Verma (Certified Drone Pilot)",
                villageHi = "हरिनगर (6.1 km दूर)",
                villageEn = "Hari Nagar (6.1 km away)",
                distanceKm = 6.1,
                rateAmount = 350,
                rateUnitHi = "प्रति एकड़",
                rateUnitEn = "per acre",
                includesFuelAndDriver = true,
                phone = "9845678901",
                descriptionHi = "10 मिनट में 1 एकड़ छिड़काव। 90% पानी की बचत, कीटनाशक का समान फैलाव।",
                descriptionEn = "10-minute spray per acre. 90% water savings and zero human chemical exposure."
            ),
            EquipmentItem(
                id = "eq_6",
                titleHi = "किर्लोस्कर 7.5 HP डीजल वाटर पंप",
                titleEn = "Kirloskar 7.5 HP Diesel Water Pump",
                category = "PUMP",
                ownerNameHi = "सोहन लाल",
                ownerNameEn = "Sohan Lal",
                villageHi = "बहादुरपुर (1.2 km दूर)",
                villageEn = "Bahadurpur (1.2 km away)",
                distanceKm = 1.2,
                rateAmount = 250,
                rateUnitHi = "प्रति घंटा",
                rateUnitEn = "per hour",
                includesFuelAndDriver = false,
                phone = "9856789012",
                descriptionHi = "200 फीट डिलीवरी पाइप सहित। तत्काल सिंचाई हेतु उच्च प्रेशर डिस्चार्ज।",
                descriptionEn = "Includes 200ft delivery pipe. High discharge water pump for instant irrigation."
            )
        )
    }

    // Persisted User Listings in SharedPreferences
    val prefs = remember { context.getSharedPreferences("krishi_equipment_prefs", Context.MODE_PRIVATE) }
    var equipmentList by remember {
        mutableStateOf(loadEquipments(prefs, defaultListings))
    }

    // Form states for Tab 1 (Listing Equipment)
    var formTitle by remember { mutableStateOf("") }
    var formCategory by remember { mutableStateOf("TRACTOR") }
    var formOwnerName by remember { mutableStateOf("") }
    var formVillage by remember { mutableStateOf("") }
    var formRate by remember { mutableStateOf("") }
    var formRateUnit by remember { mutableStateOf("HOUR") } // HOUR, ACRE, DAY
    var formIncludesFuel by remember { mutableStateOf(true) }
    var formPhone by remember { mutableStateOf("") }
    var formDescription by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isHindi) "कृषि यंत्र किराया केंद्र" else "Farm Equipment Rental",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = if (isHindi) "आसपास के किसानों से मशीनें किराए पर लें या दें" else "Rent or share farm machinery with nearby farmers",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
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
                .padding(padding)
                .background(BackgroundLight)
        ) {
            // Top Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = GreenPrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = if (isHindi) "किराये पर लें" else "Find Machines",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    icon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = if (isHindi) "किराये पर दें" else "Rent Out",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    icon = { Icon(imageVector = Icons.Default.AddCircle, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            text = if (isHindi) "सरकारी CHC" else "Govt CHC",
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    icon = { Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null) }
                )
            }

            when (selectedTab) {
                // TAB 0: FIND AND RENT EQUIPMENT
                0 -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Category Filter Chips
                        val categories = listOf(
                            "ALL" to (if (isHindi) "सभी मशीनें" else "All Machines"),
                            "TRACTOR" to (if (isHindi) "ट्रैक्टर" else "Tractor"),
                            "ROTAVATOR" to (if (isHindi) "रोटावेटर" else "Rotavator"),
                            "HARVESTER" to (if (isHindi) "हार्वेस्टर" else "Harvester"),
                            "SEED_DRILL" to (if (isHindi) "सीड ड्रिल" else "Seed Drill"),
                            "SPRAYER" to (if (isHindi) "स्प्रेयर / ड्रोन" else "Sprayer / Drone"),
                            "PUMP" to (if (isHindi) "वाटर पंप" else "Water Pump")
                        )

                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(categories) { (catKey, catLabel) ->
                                FilterChip(
                                    selected = selectedCategory == catKey,
                                    onClick = { selectedCategory = catKey },
                                    label = { Text(catLabel, fontSize = 13.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GreenPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        val filteredListings = equipmentList.filter {
                            selectedCategory == "ALL" || it.category == selectedCategory
                        }

                        if (filteredListings.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isHindi) "इस श्रेणी में कोई मशीन उपलब्ध नहीं है।" else "No equipment available in this category.",
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(filteredListings) { item ->
                                    EquipmentCard(
                                        item = item,
                                        isHindi = isHindi,
                                        onCall = {
                                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                                data = Uri.parse("tel:${item.phone}")
                                            }
                                            context.startActivity(intent)
                                        },
                                        onBook = {
                                            bookingItem = item
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // TAB 1: RENT OUT YOUR MACHINE (LISTING FORM)
                1 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFE8F5E9)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AddBusiness,
                                                contentDescription = null,
                                                tint = GreenPrimary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = if (isHindi) "अपनी मशीन किराये पर लगाकर कमाएं" else "Earn by Renting Out Your Machinery",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = if (isHindi) "खाली समय में अपनी मशीन पास के किसानों को किराये पर दें" else "List your idle farm equipment for nearby farmers",
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Equipment Category Selector
                                    Text(
                                        text = if (isHindi) "मशीन का प्रकार (Equipment Type)" else "Equipment Type",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val formCategories = listOf(
                                        "TRACTOR" to (if (isHindi) "ट्रैक्टर" else "Tractor"),
                                        "ROTAVATOR" to (if (isHindi) "रोटावेटर" else "Rotavator"),
                                        "HARVESTER" to (if (isHindi) "हार्वेस्टर" else "Harvester"),
                                        "SEED_DRILL" to (if (isHindi) "सीड ड्रिल" else "Seed Drill"),
                                        "SPRAYER" to (if (isHindi) "स्प्रेयर/ड्रोन" else "Sprayer/Drone"),
                                        "PUMP" to (if (isHindi) "वाटर पंप" else "Water Pump")
                                    )
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(formCategories) { (catKey, catLabel) ->
                                            FilterChip(
                                                selected = formCategory == catKey,
                                                onClick = { formCategory = catKey },
                                                label = { Text(catLabel, fontSize = 12.sp) }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    OutlinedTextField(
                                        value = formTitle,
                                        onValueChange = { formTitle = it },
                                        label = { Text(if (isHindi) "मशीन का नाम व मॉडल (उदा. स्वराज 744 FE)" else "Machine Name & Model (e.g. Swaraj 744 FE)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Rate & Unit
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = formRate,
                                            onValueChange = { formRate = it },
                                            label = { Text(if (isHindi) "किराया दर (₹)" else "Rental Rate (₹)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                FilterChip(
                                                    selected = formRateUnit == "HOUR",
                                                    onClick = { formRateUnit = "HOUR" },
                                                    label = { Text(if (isHindi) "/घंटा" else "/hr", fontSize = 11.sp) }
                                                )
                                                FilterChip(
                                                    selected = formRateUnit == "ACRE",
                                                    onClick = { formRateUnit = "ACRE" },
                                                    label = { Text(if (isHindi) "/एकड़" else "/acre", fontSize = 11.sp) }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Fuel & Driver Switch
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isHindi) "डीजल व ऑपरेटर शामिल है?" else "Includes Fuel & Driver?",
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = if (formIncludesFuel)
                                                    (if (isHindi) "हां, किराए में ईंधन व चालक शामिल है" else "Yes, driver & fuel included in rate")
                                                else
                                                    (if (isHindi) "नहीं, केवल मशीन (डीजल किसान का होगा)" else "No, bare machine only"),
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                        }
                                        Switch(
                                            checked = formIncludesFuel,
                                            onCheckedChange = { formIncludesFuel = it }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = formOwnerName,
                                        onValueChange = { formOwnerName = it },
                                        label = { Text(if (isHindi) "आपका नाम (Your Name)" else "Your Full Name") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = formVillage,
                                            onValueChange = { formVillage = it },
                                            label = { Text(if (isHindi) "गांव / क्षेत्र (Village/Area)" else "Village / Area") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        OutlinedTextField(
                                            value = formPhone,
                                            onValueChange = { formPhone = it },
                                            label = { Text(if (isHindi) "मोबाइल नंबर" else "Mobile No.") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = formDescription,
                                        onValueChange = { formDescription = it },
                                        label = { Text(if (isHindi) "मशीन की स्थिति व विवरण (वैकल्पिक)" else "Machine details & conditions (Optional)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        minLines = 2
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Button(
                                        onClick = {
                                            if (formTitle.isBlank() || formRate.isBlank() || formOwnerName.isBlank() || formPhone.isBlank()) {
                                                Toast.makeText(
                                                    context,
                                                    if (isHindi) "कृपया मशीन का नाम, किराया, नाम और फोन नंबर दर्ज करें" else "Please fill name, rate and phone number",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                return@Button
                                            }

                                            val newItem = EquipmentItem(
                                                titleHi = formTitle,
                                                titleEn = formTitle,
                                                category = formCategory,
                                                ownerNameHi = formOwnerName,
                                                ownerNameEn = formOwnerName,
                                                villageHi = "${formVillage.ifBlank { "निकटवर्ती" }} (0.5 km दूर)",
                                                villageEn = "${formVillage.ifBlank { "Nearby" }} (0.5 km away)",
                                                distanceKm = 0.5,
                                                rateAmount = formRate.toIntOrNull() ?: 500,
                                                rateUnitHi = if (formRateUnit == "HOUR") "प्रति घंटा" else if (formRateUnit == "ACRE") "प्रति एकड़" else "प्रति दिन",
                                                rateUnitEn = if (formRateUnit == "HOUR") "per hour" else if (formRateUnit == "ACRE") "per acre" else "per day",
                                                includesFuelAndDriver = formIncludesFuel,
                                                phone = formPhone,
                                                isUserListing = true,
                                                descriptionHi = formDescription.ifBlank { "उत्कृष्ट स्थिति में उपलब्ध।" },
                                                descriptionEn = formDescription.ifBlank { "Available in excellent working condition." }
                                            )

                                            val updatedList = listOf(newItem) + equipmentList
                                            equipmentList = updatedList
                                            saveEquipments(prefs, updatedList)

                                            // Clear form
                                            formTitle = ""
                                            formRate = ""
                                            formDescription = ""

                                            Toast.makeText(
                                                context,
                                                if (isHindi) "आपकी मशीन सफलतापूर्वक लिस्ट हो गई है!" else "Your equipment has been listed successfully!",
                                                Toast.LENGTH_LONG
                                            ).show()

                                            // Switch back to listings
                                            selectedTab = 0
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isHindi) "किराये के लिए पोस्ट करें (List Machine)" else "Post Equipment for Rent",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // TAB 2: GOVT CUSTOM HIRING CENTRE (SMAM SCHEME)
                2 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFE1F5FE)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AccountBalance,
                                                contentDescription = null,
                                                tint = Color(0xFF0277BD)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = if (isHindi) "सब-मिशन ऑन एग्रीकल्चरल मैकेनाइजेशन (SMAM)" else "Sub-Mission on Agri Mechanization (SMAM)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = if (isHindi) "कृषि एवं किसान कल्याण मंत्रालय, भारत सरकार" else "Ministry of Agriculture & Farmers Welfare, Govt of India",
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = if (isHindi)
                                            "छोटे व सीमांत किसानों को आधुनिक कृषि मशीनरी सुलभ कराने हेतु केंद्र सरकार 'कस्टम हायरिंग सेंटर (CHC)' स्थापना पर भारी अनुदान देती है।"
                                        else
                                            "To make modern farm machinery accessible to small and marginal farmers, the Government provides heavy subsidies to set up Custom Hiring Centres (CHC).",
                                        fontSize = 13.5.sp,
                                        color = TextPrimary,
                                        lineHeight = 19.sp
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Key Benefits
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9))
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                text = if (isHindi) "योजना के प्रमुख लाभ एवं अनुदान दरें:" else "Key Scheme Benefits & Subsidy Rates:",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = GreenPrimary
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = if (isHindi)
                                                    "• व्यक्तिगत किसानों हेतु: ट्रैक्टर, रोटावेटर व कल्टीवेटर पर 40% से 50% तक अनुदान।\n" +
                                                            "• कस्टम हायरिंग सेंटर (CHC): एफपीओ (FPO) व सहकारी समितियों को 10 लाख रुपये तक की परियोजना पर 80% तक सब्सिडी (अधिकतम ₹8 लाख)।\n" +
                                                            "• किसान ड्रोन योजना: ड्रोन खरीद पर एफपीओ को 75% और कृषि विज्ञान केंद्र को 100% अनुदान।"
                                                else
                                                    "• For Individual Farmers: 40% to 50% subsidy on tractors, rotavators, and seed drills.\n" +
                                                            "• Custom Hiring Centre (CHC): Up to 80% subsidy (up to ₹8-10 Lakhs) for FPOs and Farmer Cooperatives.\n" +
                                                            "• Kisan Drone Scheme: 75% subsidy for FPOs and 100% grant for KVKs on agricultural drones.",
                                                fontSize = 13.sp,
                                                color = TextPrimary,
                                                lineHeight = 20.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://agrimachinery.nic.in/"))
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0277BD)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isHindi) "सरकारी पोर्टल पर आवेदन करें (agrimachinery.nic.in)" else "Apply on Official Portal (agrimachinery.nic.in)",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Booking Modal Dialog
        bookingItem?.let { item ->
            AlertDialog(
                onDismissRequest = { bookingItem = null },
                title = {
                    Text(
                        text = if (isHindi) "मशीन बुकिंग अनुरोध" else "Book Equipment",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (isHindi) item.titleHi else item.titleEn,
                            fontWeight = FontWeight.SemiBold,
                            color = GreenPrimary,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "${if (isHindi) "मालिक: " else "Owner: "}${if (isHindi) item.ownerNameHi else item.ownerNameEn} • ${if (isHindi) item.villageHi else item.villageEn}",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "${if (isHindi) "किराया: " else "Rate: "}₹${item.rateAmount} ${if (isHindi) item.rateUnitHi else item.rateUnitEn}",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        HorizontalDivider()

                        OutlinedTextField(
                            value = bookingHours,
                            onValueChange = { bookingHours = it },
                            label = { Text(if (isHindi) "अनुमानित कार्य (घंटे / एकड़)" else "Required Work (Hours / Acres)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = bookingDate,
                            onValueChange = { bookingDate = it },
                            label = { Text(if (isHindi) "कार्य का दिन (Date / Day)" else "Work Date") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        val totalEst = (bookingHours.toIntOrNull() ?: 1) * item.rateAmount
                        Text(
                            text = "${if (isHindi) "अनुमानित कुल खर्च: " else "Estimated Total: "}₹$totalEst",
                            fontWeight = FontWeight.Bold,
                            color = AmberSecondary,
                            fontSize = 14.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            bookingItem = null
                            showBookingSuccessDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                    ) {
                        Text(if (isHindi) "बुकिंग अनुरोध भेजें" else "Send Request")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { bookingItem = null }) {
                        Text(if (isHindi) "रद्द करें" else "Cancel")
                    }
                }
            )
        }

        // Booking Success Dialog
        if (showBookingSuccessDialog) {
            AlertDialog(
                onDismissRequest = { showBookingSuccessDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                },
                title = {
                    Text(
                        text = if (isHindi) "बुकिंग अनुरोध सफलतापूर्वक भेजा गया!" else "Booking Request Sent!",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Text(
                        text = if (isHindi)
                            "मशीन के मालिक को आपका अनुरोध प्राप्त हो गया है। वे कार्य की पुष्टि हेतु शीघ्र ही आपके फोन पर संपर्क करेंगे।"
                        else
                            "The equipment owner has received your request. They will call you shortly to confirm the schedule.",
                        textAlign = TextAlign.Center,
                        fontSize = 13.5.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showBookingSuccessDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                    ) {
                        Text("ठीक है (OK)")
                    }
                }
            )
        }
    }
}

@Composable
fun EquipmentCard(
    item: EquipmentItem,
    isHindi: Boolean,
    onCall: () -> Unit,
    onBook: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (item.isUserListing) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = if (isHindi) "⭐ आपकी लिस्टेड मशीन" else "⭐ Your Listing",
                                color = GreenPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = if (isHindi) item.titleHi else item.titleEn,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${if (isHindi) item.ownerNameHi else item.ownerNameEn} • ${if (isHindi) item.villageHi else item.villageEn}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${item.rateAmount}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary,
                            fontSize = 18.sp
                        )
                    )
                    Text(
                        text = if (isHindi) item.rateUnitHi else item.rateUnitEn,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges (Fuel, Availability)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (item.includesFuelAndDriver) Color(0xFFE0F2F1) else Color(0xFFFFF3E0)
                ) {
                    Text(
                        text = if (item.includesFuelAndDriver)
                            (if (isHindi) "✓ डीजल व ऑपरेटर सहित" else "✓ Driver & Fuel Included")
                        else
                            (if (isHindi) "• केवल मशीन (डीजल किसान का)" else "• Bare Machine Only"),
                        color = if (item.includesFuelAndDriver) Color(0xFF00695C) else Color(0xFFE65100),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE8F5E9)
                ) {
                    Text(
                        text = if (isHindi) "उपलब्ध (Available)" else "Available",
                        color = GreenPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isHindi) item.descriptionHi else item.descriptionEn,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons (Call & Book)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCall,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHindi) "कॉल करें" else "Call",
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = onBook,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHindi) "बुक करें" else "Book",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun loadEquipments(prefs: android.content.SharedPreferences, defaults: List<EquipmentItem>): List<EquipmentItem> {
    val json = prefs.getString("saved_equipments_list", null) ?: return defaults
    return try {
        val type = object : TypeToken<List<EquipmentItem>>() {}.type
        val saved = Gson().fromJson<List<EquipmentItem>>(json, type)
        if (saved.isNullOrEmpty()) defaults else saved
    } catch (e: Exception) {
        defaults
    }
}

private fun saveEquipments(prefs: android.content.SharedPreferences, list: List<EquipmentItem>) {
    try {
        val json = Gson().toJson(list)
        prefs.edit().putString("saved_equipments_list", json).apply()
    } catch (e: Exception) {
        // ignore
    }
}
