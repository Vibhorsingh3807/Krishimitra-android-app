package com.krishimitra.app.ui.screens

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krishimitra.app.data.local.DatabaseHelper
import com.krishimitra.app.domain.language.AppLanguage
import com.krishimitra.app.domain.language.LanguageManager
import com.krishimitra.app.domain.model.Crop
import com.krishimitra.app.ui.theme.*

@Composable
fun rememberCropImage(context: Context, cropId: String): ImageBitmap? {
    return remember(cropId) {
        try {
            context.assets.open("crops/$cropId.webp").use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        } catch (e: Exception) {
            null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropGuideScreen(dbHelper: DatabaseHelper) {
    val context = LocalContext.current
    val currentLanguage by LanguageManager.getInstance(context).currentLanguage.collectAsState()
    val isHindi = currentLanguage == AppLanguage.HINDI

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedCropForDetail by remember { mutableStateOf<Crop?>(null) }

    val allCrops = remember {
        try {
            dbHelper.getAllCrops()
        } catch (e: Throwable) {
            emptyList<Crop>()
        }
    }

    // Categories list
    val categories = remember(isHindi) {
        if (isHindi) {
            listOf("सभी", "अनाज (Cereals)", "दालें (Pulses)", "तिलहन (Oilseeds)", "सब्जियां (Vegetables)", "फल (Fruits)", "मसाले (Spices)", "व्यावसायिक (Commercial)")
        } else {
            listOf("All", "Cereals", "Pulses", "Oilseeds", "Vegetables", "Fruits", "Spices", "Commercial")
        }
    }

    val filteredCrops = remember(searchQuery, selectedCategory, allCrops) {
        allCrops.filter { crop ->
            val matchesQuery = searchQuery.isBlank() ||
                    crop.nameHi.contains(searchQuery, ignoreCase = true) ||
                    crop.nameEn.contains(searchQuery, ignoreCase = true) ||
                    (crop.scientificName?.contains(searchQuery, ignoreCase = true) == true)

            val matchesCategory = if (selectedCategory == null || selectedCategory == "सभी" || selectedCategory == "All") {
                true
            } else {
                val catEn = crop.category?.lowercase() ?: ""
                val catHi = crop.categoryHi?.lowercase() ?: ""
                val sel = selectedCategory!!.lowercase()

                when {
                    sel.contains("cereal") || sel.contains("अनाज") -> catEn.contains("cereal") || catEn.contains("millet") || catHi.contains("अनाज")
                    sel.contains("pulse") || sel.contains("दाल") -> catEn.contains("pulse") || catHi.contains("दाल")
                    sel.contains("oilseed") || sel.contains("तिलहन") -> catEn.contains("oilseed") || catHi.contains("तिलहन")
                    sel.contains("vegetable") || sel.contains("सब्ज") -> catEn.contains("vegetable") || catHi.contains("सब्ज") || catEn.contains("tuber")
                    sel.contains("fruit") || sel.contains("फल") -> catEn.contains("fruit") || catHi.contains("फल")
                    sel.contains("spice") || sel.contains("मसाले") -> catEn.contains("spice") || catHi.contains("मसाले")
                    sel.contains("commercial") || sel.contains("व्यावसायिक") -> catEn.contains("cash") || catEn.contains("fiber") || catEn.contains("sugar") || catEn.contains("commercial")
                    else -> true
                }
            }

            matchesQuery && matchesCategory
        }
    }

    if (selectedCropForDetail != null) {
        CropDetailView(
            crop = selectedCropForDetail!!,
            isHindi = isHindi,
            onBack = { selectedCropForDetail = null }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Agriculture, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isHindi) "फसल संपूर्ण गाइड (49 फसलें)" else "ICAR Crop Guide (49 Crops)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isHindi) "बुवाई, सिंचाई, खाद व रोग रोकथाम की प्रमाणित जानकारी" else "Scientific sowing, irrigation, fertilizer & pest advisory",
                        fontSize = 11.5.sp,
                        color = TextSecondary
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                placeholder = {
                    Text(
                        text = if (isHindi) "फसल खोजें (गेहूं, धान, कपास, सरसों, आदि)…" else "Search crop (Rice, Wheat, Cotton, Tomato)…",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(20.dp)) },
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = (selectedCategory == null && (cat == "सभी" || cat == "All")) || selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedCategory = if (cat == "सभी" || cat == "All") null else cat
                        },
                        label = { Text(cat, fontSize = 11.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            // Results count
            Text(
                text = if (isHindi) "कुल उपलब्ध फसलें: ${filteredCrops.size}" else "Available crops: ${filteredCrops.size}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Compact 2-Column Grid of 49 Crops
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredCrops, key = { it.id }) { crop ->
                    CompactCropCard(
                        crop = crop,
                        isHindi = isHindi,
                        onClick = { selectedCropForDetail = crop }
                    )
                }
            }
        }
    }
}

@Composable
fun CompactCropCard(
    crop: Crop,
    isHindi: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val bitmap = rememberCropImage(context, crop.id)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            // Image Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(105.dp)
                    .background(Color(0xFFE8F5E9)),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = crop.nameEn,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Grass,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Category pill overlay
                val categoryText = if (isHindi) (crop.categoryHi ?: crop.category) else crop.category
                if (!categoryText.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = categoryText.split("/").first().trim(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Text Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    text = if (isHindi) crop.nameHi else crop.nameEn,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = if (isHindi) crop.nameEn else crop.nameHi,
                    fontSize = 11.5.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val season = if (isHindi) (crop.sowingSeasonHi ?: crop.sowingSeason) else crop.sowingSeason
                    Text(
                        text = "📅 ${season?.split(",")?.firstOrNull()?.trim() ?: "खरीफ/रबी"}",
                        fontSize = 10.sp,
                        color = GreenDark,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CropDetailView(
    crop: Crop,
    isHindi: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val bitmap = rememberCropImage(context, crop.id)

    // Expandable sections state
    var isSowingOpen by remember { mutableStateOf(true) }
    var isIrrigationOpen by remember { mutableStateOf(true) }
    var isFertilizerOpen by remember { mutableStateOf(false) }
    var isPestOpen by remember { mutableStateOf(false) }
    var isHarvestOpen by remember { mutableStateOf(false) }
    var isTipsOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Top Bar with Back Button
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = if (isHindi) crop.nameHi else crop.nameEn,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = crop.scientificName ?: (if (isHindi) crop.nameEn else crop.nameHi),
                        fontSize = 11.5.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }
        }

        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Image Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(Color(0xFFE8F5E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = crop.nameEn,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Grass,
                                contentDescription = null,
                                tint = GreenPrimary,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isHindi) "${crop.nameHi} (${crop.nameEn})" else "${crop.nameEn} (${crop.nameHi})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (!crop.scientificName.isNullOrBlank()) {
                                Text(
                                    text = "वनस्पति नाम: ${crop.scientificName}",
                                    fontSize = 11.5.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE8F5E9)
                        ) {
                            Text(
                                text = if (isHindi) (crop.categoryHi ?: crop.category ?: "फसल") else (crop.category ?: "Crop"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Quick Facts Section (5-Grid Cards)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isHindi) "महत्वपूर्ण त्वरित तथ्य (Quick Facts)" else "Key Agronomic Quick Facts",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = GreenPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuickFactRow(
                            icon = "🌱",
                            label = if (isHindi) "उपयुक्त मिट्टी" else "Suitable Soil",
                            value = "${if (isHindi) (crop.soilHi ?: crop.soil) else crop.soil}${if (!crop.soilPh.isNullOrBlank()) " (pH: ${crop.soilPh})" else ""}"
                        )
                        QuickFactRow(
                            icon = "📅",
                            label = if (isHindi) "बुवाई का समय" else "Sowing Season",
                            value = if (isHindi) (crop.sowingSeasonHi ?: crop.sowingSeason ?: "-") else (crop.sowingSeason ?: "-")
                        )
                        QuickFactRow(
                            icon = "💧",
                            label = if (isHindi) "सिंचाई जरूरत" else "Water / Irrigation",
                            value = if (isHindi) (crop.irrigationHi ?: crop.irrigation ?: "-") else (crop.irrigation ?: "-")
                        )
                        QuickFactRow(
                            icon = "🌡️",
                            label = if (isHindi) "जलवायु व तापमान" else "Climate & Temp",
                            value = "${if (isHindi) (crop.climateHi ?: crop.climate ?: "-") else (crop.climate ?: "-")}${if (!crop.temperature.isNullOrBlank()) " (${crop.temperature})" else ""}"
                        )
                        QuickFactRow(
                            icon = "⏱️",
                            label = if (isHindi) "फसल अवधि व कटाई" else "Duration & Harvest",
                            value = if (isHindi) (crop.harvestingHi ?: crop.harvesting ?: "-") else (crop.harvesting ?: "-")
                        )
                    }
                }
            }

            // Expandable Section 1: Sowing Guide
            val sowingContent = if (isHindi) (crop.sowingSeasonHi ?: crop.sowingSeason) else crop.sowingSeason
            if (!sowingContent.isNullOrBlank()) {
                ExpandableCropSection(
                    title = if (isHindi) "🌱 बुवाई व बीज प्रबंधन (Sowing Guide)" else "🌱 Sowing & Seed Preparation",
                    isOpen = isSowingOpen,
                    onToggle = { isSowingOpen = !isSowingOpen },
                    content = sowingContent
                )
            }

            // Expandable Section 2: Irrigation
            val irrigationContent = if (isHindi) (crop.irrigationHi ?: crop.irrigation) else crop.irrigation
            if (!irrigationContent.isNullOrBlank()) {
                ExpandableCropSection(
                    title = if (isHindi) "💧 सिंचाई व्यवस्था (Irrigation Schedule)" else "💧 Irrigation Management",
                    isOpen = isIrrigationOpen,
                    onToggle = { isIrrigationOpen = !isIrrigationOpen },
                    content = irrigationContent
                )
            }

            // Expandable Section 3: Fertilizer
            val fertilizerContent = if (isHindi) (crop.fertilizerHi ?: crop.fertilizer) else crop.fertilizer
            if (!fertilizerContent.isNullOrBlank()) {
                ExpandableCropSection(
                    title = if (isHindi) "🧪 खाद व पोषण प्रबंधन (Fertilizer & Nutrition)" else "🧪 Fertilizer & Nutrition Schedule",
                    isOpen = isFertilizerOpen,
                    onToggle = { isFertilizerOpen = !isFertilizerOpen },
                    content = fertilizerContent
                )
            }

            // Expandable Section 4: Diseases & Pests
            val pestContent = if (isHindi) {
                "${crop.pestsHi ?: crop.pests ?: ""}\n\n${crop.diseasesHi ?: crop.diseases ?: ""}".trim()
            } else {
                "${crop.pests ?: ""}\n\n${crop.diseases ?: ""}".trim()
            }
            if (pestContent.isNotBlank()) {
                ExpandableCropSection(
                    title = if (isHindi) "🐛 कीट व रोग नियंत्रण (Pests & Diseases)" else "🐛 Pest & Disease Protection",
                    isOpen = isPestOpen,
                    onToggle = { isPestOpen = !isPestOpen },
                    content = pestContent
                )
            }

            // Expandable Section 5: Harvest & Yield
            val harvestContent = if (isHindi) (crop.harvestingHi ?: crop.harvesting) else crop.harvesting
            if (!harvestContent.isNullOrBlank()) {
                ExpandableCropSection(
                    title = if (isHindi) "🌾 कटाई व पैदावार (Harvest & Yield)" else "🌾 Harvesting & Yield",
                    isOpen = isHarvestOpen,
                    onToggle = { isHarvestOpen = !isHarvestOpen },
                    content = harvestContent
                )
            }

            // Expandable Section 6: Cultivation Tips
            val tipsContent = if (isHindi) (crop.cultivationTipsHi ?: crop.cultivationTips) else crop.cultivationTips
            if (!tipsContent.isNullOrBlank()) {
                ExpandableCropSection(
                    title = if (isHindi) "⚠️ विशेष सावधानियां एवं वैज्ञानिक सलाह (Important Tips)" else "⚠️ Expert Tips & Precautions",
                    isOpen = isTipsOpen,
                    onToggle = { isTipsOpen = !isTipsOpen },
                    content = tipsContent
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun QuickFactRow(icon: String, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(BackgroundLight)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = icon, fontSize = 15.sp, modifier = Modifier.padding(top = 1.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            Text(text = value, fontSize = 12.5.sp, color = TextPrimary, lineHeight = 17.sp)
        }
    }
}

@Composable
fun ExpandableCropSection(
    title: String,
    isOpen: Boolean,
    onToggle: () -> Unit,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = GreenPrimary
                )
            }

            AnimatedVisibility(
                visible = isOpen,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(color = CardBorder, thickness = 0.6.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Split into clean bullet points if multiple sentences
                    val bullets = content.split("।", "\n", ".").map { it.trim() }.filter { it.length > 5 }
                    if (bullets.size > 1) {
                        bullets.forEach { point ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(text = "•", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = point,
                                    fontSize = 12.5.sp,
                                    color = TextPrimary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    } else {
                        Text(
                            text = content,
                            fontSize = 12.5.sp,
                            color = TextPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}
