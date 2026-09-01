package com.krishimitra.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krishimitra.app.data.local.DatabaseHelper
import com.krishimitra.app.domain.language.AppLanguage
import com.krishimitra.app.domain.language.LanguageManager
import com.krishimitra.app.domain.model.Scheme
import com.krishimitra.app.ui.theme.*

fun openExternalWebUrl(context: Context, rawUrl: String?) {
    if (rawUrl.isNullOrBlank()) {
        Toast.makeText(context, "आधिकारिक लिंक उपलब्ध नहीं है", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val formattedUrl = if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
            "https://$rawUrl"
        } else {
            rawUrl
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e("SchemesScreen", "Failed to launch web browser for $rawUrl: ${e.message}")
        Toast.makeText(context, "वेबसाइट खोलने में असमर्थ। कृपया ब्राउज़र जांचें।", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemesScreen(dbHelper: DatabaseHelper) {
    val context = LocalContext.current
    val currentLanguage by LanguageManager.getInstance(context).currentLanguage.collectAsState()
    val isHindi = currentLanguage == AppLanguage.HINDI

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val categories = remember(isHindi) {
        if (isHindi) {
            listOf("सभी", "सब्सिडी", "बीज", "सिंचाई", "ऋण", "बीमा", "पेंशन")
        } else {
            listOf("All", "Subsidy", "Seeds", "Irrigation", "Credit", "Insurance", "Pension")
        }
    }

    val allSchemes = remember {
        try {
            val list = dbHelper.getAllSchemes()
            if (list.isNotEmpty()) list else getVerifiedGovernmentSchemes()
        } catch (e: Throwable) {
            getVerifiedGovernmentSchemes()
        }
    }

    val filteredSchemes = remember(searchQuery, selectedCategory, allSchemes) {
        allSchemes.filter { scheme ->
            val matchesQuery = searchQuery.isBlank() ||
                    scheme.nameHi.contains(searchQuery, ignoreCase = true) ||
                    scheme.nameEn.contains(searchQuery, ignoreCase = true) ||
                    (scheme.categoryHi?.contains(searchQuery, ignoreCase = true) == true) ||
                    (scheme.benefitsHi?.contains(searchQuery, ignoreCase = true) == true)

            val matchesCategory = if (selectedCategory == null || selectedCategory == "सभी" || selectedCategory == "All") {
                true
            } else {
                val catEn = scheme.category?.lowercase() ?: ""
                val catHi = scheme.categoryHi?.lowercase() ?: ""
                val benefits = (scheme.benefitsHi ?: "") + " " + (scheme.benefitsEn ?: "")
                val sel = selectedCategory!!.lowercase()

                when {
                    sel.contains("subsid") || sel.contains("सब्सिडी") -> catEn.contains("subsid") || catHi.contains("सब्सिडी") || benefits.contains("सब्सिडी")
                    sel.contains("seed") || sel.contains("बीज") -> catEn.contains("seed") || catHi.contains("बीज") || benefits.contains("बीज")
                    sel.contains("irrigat") || sel.contains("सिंचाई") -> catEn.contains("irrigat") || catHi.contains("सिंचाई") || benefits.contains("सिंचाई")
                    sel.contains("credit") || sel.contains("loan") || sel.contains("ऋण") -> catEn.contains("credit") || catHi.contains("ऋण") || catEn.contains("loan")
                    sel.contains("insuran") || sel.contains("बीमा") -> catEn.contains("insuran") || catHi.contains("बीमा")
                    sel.contains("pension") || sel.contains("पेंशन") -> catEn.contains("pension") || catHi.contains("पेंशन")
                    else -> true
                }
            }

            matchesQuery && matchesCategory
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(16.dp)
    ) {
        // Header Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE7F6))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF6A1B9A).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = Color(0xFF6A1B9A),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isHindi) "सरकारी योजनाएं व सब्सिडी पोर्टल" else "Government Schemes & Subsidies",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isHindi)
                            "आवेदन करने हेतु नीचे दिए बटन से सीधे आधिकारिक सरकारी पोर्टल पर जाएं"
                        else
                            "Apply directly on verified official government portals via link",
                        fontSize = 11.5.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            placeholder = {
                Text(
                    text = if (isHindi) "योजना या सब्सिडी खोजें (PM-Kisan, फसल बीमा, सिंचाई)…" else "Search scheme (PM-Kisan, Insurance, Solar Pump)…",
                    fontSize = 13.sp
                )
            },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GreenPrimary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GreenPrimary,
                unfocusedBorderColor = CardBorder,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )

        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            items(categories) { cat ->
                val isSelected = (selectedCategory == null && (cat == "सभी" || cat == "All")) || selectedCategory == cat
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedCategory = if (cat == "सभी" || cat == "All") null else cat
                    },
                    label = { Text(cat, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    shape = RoundedCornerShape(18.dp)
                )
            }
        }

        // Results count
        Text(
            text = if (isHindi) "उपलब्ध योजनाएं: ${filteredSchemes.size}" else "Available Schemes: ${filteredSchemes.size}",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredSchemes) { scheme ->
                SchemeCard(
                    scheme = scheme,
                    isHindi = isHindi,
                    onOpenPortal = {
                        openExternalWebUrl(context, scheme.officialUrl)
                    }
                )
            }
        }
    }
}

@Composable
fun SchemeCard(
    scheme: Scheme,
    isHindi: Boolean,
    onOpenPortal: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Category & Ministry
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isHindi) (scheme.categoryHi ?: scheme.category ?: "सरकारी योजना") else (scheme.category ?: "Govt Scheme"),
                        color = GreenPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = scheme.ministry ?: scheme.source ?: "भारत सरकार",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scheme Name
            Text(
                text = if (isHindi) scheme.nameHi else scheme.nameEn,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Key Benefits
            Text(
                text = if (isHindi) (scheme.benefitsHi ?: scheme.benefitsEn ?: "") else (scheme.benefitsEn ?: scheme.benefitsHi ?: ""),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.5.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                ),
                maxLines = if (isExpanded) Int.MAX_VALUE else 3
            )

            // Eligibility text
            val eligibilityText = if (isHindi) (scheme.eligibilityHi ?: scheme.eligibilityEn) else (scheme.eligibilityEn ?: scheme.eligibilityHi)
            if (!eligibilityText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = BackgroundLight
                ) {
                    Text(
                        text = "${if (isHindi) "पात्रता: " else "Eligibility: "}$eligibilityText",
                        fontSize = 11.sp,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = CardBorder, thickness = 0.6.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: Details and Apply Online
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isExpanded) (if (isHindi) "कम देखें ▲" else "Less ▲") else (if (isHindi) "विस्तार से देखें ▼" else "More Details ▼"),
                        fontSize = 12.sp,
                        color = GreenPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onOpenPortal,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHindi) "आवेदन करें / वेबसाइट" else "Apply / Official Portal",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Expandable details (How to apply & source)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    val processText = if (isHindi) scheme.applicationProcessHi ?: scheme.applicationProcessEn else scheme.applicationProcessEn ?: scheme.applicationProcessHi
                    if (!processText.isNullOrBlank()) {
                        Text(
                            text = if (isHindi) "आवेदन प्रक्रिया:" else "Application Process:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = processText,
                            fontSize = 11.5.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (!scheme.officialUrl.isNullOrBlank()) {
                        Text(
                            text = "🔗 आधिकारिक यूआरएल: ${scheme.officialUrl}",
                            fontSize = 11.sp,
                            color = Color(0xFF0288D1),
                            modifier = Modifier.clickable { onOpenPortal() }
                        )
                    }
                }
            }
        }
    }
}

private fun getVerifiedGovernmentSchemes(): List<Scheme> {
    return listOf(
        Scheme(
            id = "pm_kisan",
            nameEn = "PM-KISAN (Pradhan Mantri Kisan Samman Nidhi)",
            nameHi = "प्रधानमंत्री किसान सम्मान निधि (PM-KISAN)",
            category = "Direct Income Support",
            categoryHi = "प्रत्यक्ष आय सहायता",
            ministry = "कृषि एवं किसान कल्याण मंत्रालय",
            benefitsEn = "₹6,000 per year provided in three equal installments of ₹2,000 directly transferred into the bank accounts of all landholding farmer families.",
            benefitsHi = "सभी भूमिधारक किसान परिवारों को ₹6,000 प्रति वर्ष 3 समान किस्तों (₹2,000) में सीधे बैंक खाते में DBT द्वारा हस्तांतरित।",
            eligibilityEn = "All landholding farmer families with cultivable landholding in their name.",
            eligibilityHi = "सभी किसान जिनके नाम पर कृषि योग्य भूमि का आधिकारिक रिकॉर्ड दर्ज है।",
            applicationProcessEn = "Apply online via PM-KISAN portal (pmkisan.gov.in) with Aadhaar and Khatauni, or visit nearest CSC.",
            applicationProcessHi = "पीएम-किसान पोर्टल (pmkisan.gov.in) पर आधार व खतौनी से ऑनलाइन पंजीकरण करें अथवा जनसेवा केंद्र पर जाएं।",
            officialUrl = "https://pmkisan.gov.in",
            source = "pmkisan.gov.in",
            lastVerified = "2026-08-20"
        ),
        Scheme(
            id = "pmfby",
            nameEn = "PMFBY (Pradhan Mantri Fasal Bima Yojana)",
            nameHi = "प्रधानमंत्री फसल बीमा योजना (PMFBY)",
            category = "Crop Insurance",
            categoryHi = "फसल बीमा",
            ministry = "कृषि एवं किसान कल्याण मंत्रालय",
            benefitsEn = "Comprehensive crop insurance covering non-preventable natural risks from pre-sowing to post-harvest. Low premium: 2% Kharif, 1.5% Rabi, 5% Commercial crops.",
            benefitsHi = "बुवाई से लेकर कटाई तक प्राकृतिक आपदा, सूखा, बाढ़, ओलावृष्टि व कीट रोग से फसल क्षति पर संपूर्ण बीमा सुरक्षा। प्रीमियम मात्र 2% खरीफ, 1.5% रबी।",
            eligibilityEn = "All farmers including sharecroppers and tenant farmers growing notified crops in notified areas.",
            eligibilityHi = "अधिसूचित क्षेत्रों में अधिसूचित फसलें उगाने वाले सभी किसान, जिनमें बटाईदार व काश्तकार भी शामिल हैं।",
            applicationProcessEn = "Enroll through bank branch, national crop insurance portal (pmfby.gov.in), or nearest CSC before cutoff date.",
            applicationProcessHi = "संबंधित बैंक शाखा, राष्ट्रीय फसल बीमा पोर्टल (pmfby.gov.in) या सीएससी केंद्र से निर्धारित तिथि से पूर्व आवेदन करें।",
            officialUrl = "https://pmfby.gov.in",
            source = "pmfby.gov.in",
            lastVerified = "2026-08-20"
        ),
        Scheme(
            id = "kcc_scheme",
            nameEn = "Kisan Credit Card (KCC) Scheme",
            nameHi = "किसान क्रेडिट कार्ड (KCC) योजना",
            category = "Agricultural Credit",
            categoryHi = "ऋण एवं कार्यशील पूंजी",
            ministry = "कृषि मंत्रालय एवं नाबार्ड",
            benefitsEn = "Timely credit for agricultural inputs and cultivation expenses at subsidized 4% p.a. interest rate with prompt repayment. Collateral-free up to ₹1.6 Lakhs.",
            benefitsHi = "खाद, बीज व खेती के खर्च हेतु मात्र 4% रियायती ब्याज दर पर ऋण। ₹1.60 लाख तक बिना किसी जमीन बंधक के ऋण उपलब्ध।",
            eligibilityEn = "Owner-cultivators, tenant farmers, oral lessees, and sharecroppers.",
            eligibilityHi = "मालिक किसान, किरायेदार किसान, बटाईदार व स्वयं सहायता समूह।",
            applicationProcessEn = "Apply via JanSamarth portal or submit common application form at local bank branch.",
            applicationProcessHi = "जनसमर्थ पोर्टल अथवा निकटतम बैंक शाखा में आवेदन पत्र प्रस्तुत करें।",
            officialUrl = "https://www.myscheme.gov.in/schemes/kcc",
            source = "myscheme.gov.in",
            lastVerified = "2026-08-20"
        ),
        Scheme(
            id = "soil_health_card",
            nameEn = "Soil Health Card (SHC) Scheme",
            nameHi = "मृदा स्वास्थ्य कार्ड (Soil Health Card) योजना",
            category = "Soil Management",
            categoryHi = "मिट्टी परीक्षण व पोषण",
            ministry = "कृषि एवं किसान कल्याण मंत्रालय",
            benefitsEn = "Free soil testing and customized advisory on 12 essential soil parameters and fertilizer dosages to reduce input cost and improve yield.",
            benefitsHi = "खेत की मिट्टी की निःशुल्क जांच और 12 पोषक तत्वों की स्थिति व उपयुक्त खाद की वैज्ञानिक सिफारिश, जिससे 20-25% खाद खर्च बचता है।",
            eligibilityEn = "All farmers across India with agricultural land.",
            eligibilityHi = "देश के समस्त किसान।",
            applicationProcessEn = "Soil samples collected by local agriculture department or submit sample at nearest Krishi Vigyan Kendra (KVK).",
            applicationProcessHi = "स्थानीय कृषि विभाग अथवा निकटतम कृषि विज्ञान केंद्र (KVK) में मिट्टी का नमूना जमा करें।",
            officialUrl = "https://soilhealth.dac.gov.in",
            source = "soilhealth.dac.gov.in",
            lastVerified = "2026-08-15"
        ),
        Scheme(
            id = "pmksy",
            nameEn = "PMKSY (Pradhan Mantri Krishi Sinchayee Yojana - Per Drop More Crop)",
            nameHi = "प्रधानमंत्री कृषि सिंचाई योजना (PMKSY - प्रति बूंद अधिक फसल)",
            category = "Micro Irrigation",
            categoryHi = "सूक्ष्म सिंचाई व ड्रिप",
            ministry = "जल शक्ति एवं कृषि मंत्रालय",
            benefitsEn = "Up to 55% subsidy for small/marginal farmers (45% for others) on installation of Drip & Sprinkler micro-irrigation systems.",
            benefitsHi = "ड्रिप व स्प्रिंकलर (फव्वारा) सूक्ष्म सिंचाई प्रणाली लगाने पर छोटे व सीमांत किसानों को 55% तक और अन्य को 45% तक भारी सरकारी सब्सिडी।",
            eligibilityEn = "Farmers with assured water source and agricultural landholding.",
            eligibilityHi = "जल स्रोत उपलब्ध रखने वाले सभी कृषि भूमिधारक किसान।",
            applicationProcessEn = "Register through state horticulture / agriculture department portal.",
            applicationProcessHi = "राज्य उद्यान अथवा कृषि विभाग के ऑनलाइन पोर्टल पर पंजीकरण करें।",
            officialUrl = "https://pmksy.gov.in",
            source = "pmksy.gov.in",
            lastVerified = "2026-08-18"
        ),
        Scheme(
            id = "smam_machinery",
            nameEn = "SMAM (Sub-Mission on Agricultural Mechanization)",
            nameHi = "कृषि यंत्रीकरण उप-मिशन (SMAM)",
            category = "Farm Machinery",
            categoryHi = "कृषि यंत्र व ट्रैक्टर सब्सिडी",
            ministry = "कृषि एवं किसान कल्याण मंत्रालय",
            benefitsEn = "40% to 50% subsidy for individual farmers on purchase of tractors, rotavators, tillers; up to 80% subsidy for Custom Hiring Centres (CHCs).",
            benefitsHi = "ट्रैक्टर, रोटावेटर व थ्रेशर खरीद पर किसानों को 40% से 50% सब्सिडी; कस्टम हायरिंग सेंटर (CHC) स्थापना हेतु 80% तक अनुदान।",
            eligibilityEn = "Individual farmers, FPOs, cooperatives, and rural youth entrepreneurs.",
            eligibilityHi = "व्यक्तिगत किसान, एफपीओ, सहकारी समितियां एवं ग्रामीण उद्यमी।",
            applicationProcessEn = "Apply online on the centralized agrimachinery.nic.in portal.",
            applicationProcessHi = "केंद्रीय agrimachinery.nic.in पोर्टल पर ऑनलाइन टोकन प्राप्त कर आवेदन करें।",
            officialUrl = "https://agrimachinery.nic.in",
            source = "agrimachinery.nic.in",
            lastVerified = "2026-08-20"
        ),
        Scheme(
            id = "pkvy_organic",
            nameEn = "PKVY (Paramparagat Krishi Vikas Yojana - Organic Farming)",
            nameHi = "परंपरागत कृषि विकास योजना (PKVY - जैविक खेती)",
            category = "Organic Farming",
            categoryHi = "जैविक खेती प्रोत्साहन",
            ministry = "कृषि एवं किसान कल्याण मंत्रालय",
            benefitsEn = "Financial assistance of ₹50,000 per hectare for cluster-based organic farming, certification, bio-fertilizers, packaging, and marketing.",
            benefitsHi = "जैविक खेती क्लस्टर हेतु ₹50,000 प्रति हेक्टेयर वित्तीय सहायता, निःशुल्क जैविक प्रमाणीकरण व विपणन सहायता।",
            eligibilityEn = "Farmers willing to form clusters of 20 or more hectares for organic cultivation.",
            eligibilityHi = "20 हेक्टेयर या उससे अधिक क्लस्टर बनाने वाले किसान।",
            applicationProcessEn = "Contact District Agriculture Office or apply through PGS-India organic portal.",
            applicationProcessHi = "जिला कृषि अधिकारी से संपर्क करें अथवा pgsindia-dacfw.nic.in पर पंजीकरण करें।",
            officialUrl = "https://pgsindia-dacfw.nic.in",
            source = "pgsindia-dacfw.nic.in",
            lastVerified = "2026-08-12"
        ),
        Scheme(
            id = "pm_kmy",
            nameEn = "PM-KMY (Pradhan Mantri Kisan Maan-Dhan Yojana)",
            nameHi = "प्रधानमंत्री किसान मान-धन योजना (PM-KMY पेंशन)",
            category = "Social Security",
            categoryHi = "किसान सामाजिक सुरक्षा व पेंशन",
            ministry = "कृषि एवं किसान कल्याण मंत्रालय (LIC द्वारा प्रबंधित)",
            benefitsEn = "Assured monthly pension of ₹3,000 to small and marginal farmers on attaining the age of 60 years.",
            benefitsHi = "60 वर्ष की आयु पूर्ण होने पर छोटे व सीमांत किसानों को ₹3,000 प्रतिमाह की सुनिश्चित वृद्धावस्था पेंशन।",
            eligibilityEn = "Small and marginal farmers aged 18 to 40 years with cultivable land up to 2 hectares.",
            eligibilityHi = "18 से 40 वर्ष आयु वर्ग के छोटे व सीमांत किसान जिनके पास 2 हेक्टेयर तक भूमि है।",
            applicationProcessEn = "Enroll through Common Service Centres (CSC) or online at maandhan.in with Aadhaar and bank passbook.",
            applicationProcessHi = "आधार कार्ड व बैंक पासबुक लेकर निकटतम सीएससी केंद्र पर जाएं अथवा maandhan.in पर पंजीकरण करें।",
            officialUrl = "https://maandhan.in",
            source = "maandhan.in",
            lastVerified = "2026-08-20"
        )
    )
}
