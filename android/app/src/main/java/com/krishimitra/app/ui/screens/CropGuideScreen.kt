package com.krishimitra.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krishimitra.app.R
import com.krishimitra.app.data.local.DatabaseHelper
import com.krishimitra.app.domain.model.Crop
import com.krishimitra.app.ui.theme.*

@Composable
fun CropGuideScreen(dbHelper: DatabaseHelper) {
    var searchQuery by remember { mutableStateOf("") }
    val allCrops = remember {
        try {
            dbHelper.getAllCrops()
        } catch (e: Throwable) {
            emptyList<Crop>()
        }
    }

    val filteredCrops = remember(searchQuery, allCrops) {
        if (searchQuery.isBlank()) allCrops
        else allCrops.filter {
            it.nameHi.contains(searchQuery, ignoreCase = true) ||
            it.nameEn.contains(searchQuery, ignoreCase = true) ||
            (it.categoryHi?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(16.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            placeholder = { Text("फसल खोजें (गेहूं, धान, मक्का, कपास, सरसों, आदि)…", fontSize = 13.5.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GreenPrimary) },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GreenPrimary,
                unfocusedBorderColor = CardBorder,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredCrops) { crop ->
                CropGuideCard(crop = crop)
            }
        }
    }
}

@Composable
fun CropGuideCard(crop: Crop) {
    var isExpanded by remember { mutableStateOf(false) }
    val isHindi = java.util.Locale.getDefault().language == "hi"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8F5E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Grass,
                            contentDescription = null,
                            tint = GreenPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isHindi) crop.nameHi else crop.nameEn,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = if (isHindi) "${crop.nameEn} • ${crop.scientificName ?: ""}" else "${crop.nameHi} • ${crop.scientificName ?: ""}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 11.5.sp
                            )
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE0F2F1)
                ) {
                    Text(
                        text = if (isHindi) (crop.categoryHi ?: "फसल") else (crop.category ?: "Crop"),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF00695C),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // COMPACT QUICK FACTS GRID
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8FAF6))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickFactBadge(
                    icon = Icons.Default.CalendarMonth,
                    label = if (isHindi) "बुवाई समय" else "Sowing",
                    value = (if (isHindi) crop.sowingSeasonHi else crop.sowingSeason) ?: "सामयिक"
                )
                QuickFactBadge(
                    icon = Icons.Default.Thermostat,
                    label = if (isHindi) "तापमान" else "Temp",
                    value = crop.temperature ?: "20-30°C"
                )
                QuickFactBadge(
                    icon = Icons.Default.Science,
                    label = if (isHindi) "मिट्टी pH" else "Soil pH",
                    value = crop.soilPh ?: "6.0-7.5"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Expandable Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.8.dp)

                    FormattedSection(
                        icon = Icons.Default.Spa,
                        title = if (isHindi) "🌱 उपयुक्त मिट्टी व तैयारी" else "🌱 Suitable Soil & Preparation",
                        content = if (isHindi) crop.soilHi else crop.soil
                    )

                    FormattedSection(
                        icon = Icons.Default.WaterDrop,
                        title = if (isHindi) "💧 सिंचाई व जल प्रबंधन" else "💧 Irrigation Management",
                        content = if (isHindi) crop.irrigationHi else crop.irrigation
                    )

                    FormattedSection(
                        icon = Icons.Default.Science,
                        title = if (isHindi) "🧪 खाद व पोषण तालिका (NPK)" else "🧪 Fertilizer Schedule (NPK)",
                        content = if (isHindi) crop.fertilizerHi else crop.fertilizer
                    )

                    FormattedSection(
                        icon = Icons.Default.WbSunny,
                        title = if (isHindi) "🌡️ जलवायु व तापमान आवश्यक" else "🌡️ Climate & Temperature",
                        content = if (isHindi) crop.climateHi else crop.climate
                    )

                    FormattedSection(
                        icon = Icons.Default.BugReport,
                        title = if (isHindi) "🐛 प्रमुख कीट व रोकथाम" else "🐛 Major Pests & Control",
                        content = if (isHindi) crop.pestsHi else crop.pests
                    )

                    FormattedSection(
                        icon = Icons.Default.Healing,
                        title = if (isHindi) "🦠 प्रमुख रोग व उपचार" else "🦠 Common Diseases & Treatment",
                        content = if (isHindi) crop.diseasesHi else crop.diseases
                    )

                    FormattedSection(
                        icon = Icons.Default.ContentCut,
                        title = if (isHindi) "🌾 कटाई व औसतन उपज" else "🌾 Harvesting & Yield",
                        content = if (isHindi) crop.harvestingHi else crop.harvesting
                    )

                    FormattedSection(
                        icon = Icons.Default.TipsAndUpdates,
                        title = if (isHindi) "⚠️ उन्नत सुझाव व सावधानियां" else "⚠️ Best Practices & Tips",
                        content = if (isHindi) crop.cultivationTipsHi else crop.cultivationTips
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isHindi) "प्रमाणित स्रोत: ${crop.source ?: "भारतीय कृषि अनुसंधान परिषद (ICAR)"}" else "Verified Source: ${crop.source ?: "ICAR"}",
                        fontSize = 10.5.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Expand / Collapse Action Line
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) {
                        if (isHindi) "कम विवरण समेटें ▲" else "Collapse Guide ▲"
                    } else {
                        if (isHindi) "पूरा कृषि गाइड देखें ▼" else "Open Complete Cultivation Manual ▼"
                    },
                    fontSize = 12.sp,
                    color = GreenPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun QuickFactBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text(text = label, fontSize = 10.5.sp, color = TextSecondary)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
    }
}

@Composable
fun FormattedSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: String?
) {
    if (content.isNullOrBlank()) return

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = GreenDark
            )
        }
        Spacer(modifier = Modifier.height(4.dp))

        // Split text into bullet points if comma or semicolon or newline separated
        val bullets = remember(content) {
            if (content.contains("\n")) {
                content.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            } else if (content.contains(";")) {
                content.split(";").map { it.trim() }.filter { it.isNotEmpty() }
            } else {
                listOf(content.trim())
            }
        }

        bullets.forEach { point ->
            val cleanPoint = if (point.startsWith("•") || point.startsWith("-")) point else "• $point"
            Text(
                text = cleanPoint,
                fontSize = 12.sp,
                color = TextPrimary,
                lineHeight = 17.sp,
                modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
            )
        }
    }
}
