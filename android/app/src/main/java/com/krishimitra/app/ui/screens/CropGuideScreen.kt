package com.krishimitra.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krishimitra.app.data.local.DatabaseHelper
import com.krishimitra.app.domain.model.Crop
import com.krishimitra.app.ui.theme.*

@Composable
fun CropGuideScreen(dbHelper: DatabaseHelper) {
    var searchQuery by remember { mutableStateOf("") }
    val allCrops = remember { dbHelper.getAllCrops() }

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
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            placeholder = { Text("फसल खोजें (धान, गेहूं, मक्का, आदि)…", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GreenPrimary,
                unfocusedBorderColor = CardBorder,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredCrops) { crop ->
                CropExpandableCard(crop = crop)
            }
        }
    }
}

@Composable
fun CropExpandableCard(crop: Crop) {
    var isExpanded by remember { mutableStateOf(false) }
    val isHindi = androidx.compose.ui.platform.LocalConfiguration.current.locales[0].language == "hi"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isHindi) crop.nameHi else crop.nameEn,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )
                    )
                    Text(
                        text = if (isHindi) "${crop.nameEn} (${crop.scientificName ?: ""})" else "${crop.nameHi} (${crop.scientificName ?: ""})",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GreenPrimaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = if (isHindi) (crop.categoryHi ?: "फसल") else (crop.category ?: "Crop"),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = GreenDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Facts Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF9FBF8))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = if (isHindi) "बुवाई मौसम:" else "Sowing Season:", fontSize = 11.sp, color = TextSecondary)
                    Text(text = if (isHindi) (crop.sowingSeasonHi ?: "") else (crop.sowingSeason ?: ""), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text(text = if (isHindi) "मिट्टी पीएच:" else "Soil pH:", fontSize = 11.sp, color = TextSecondary)
                    Text(text = crop.soilPh ?: "6.0 - 7.5", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text(text = if (isHindi) "अनुकूल तापमान:" else "Optimum Temp:", fontSize = 11.sp, color = TextSecondary)
                    Text(text = crop.temperature ?: "", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Expanded Details
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Divider(color = Color(0xFFEEEEEE), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    CropDetailSection(
                        title = if (isHindi) "उपयुक्त मिट्टी:" else "Suitable Soil:",
                        content = if (isHindi) crop.soilHi else crop.soil
                    )
                    CropDetailSection(
                        title = if (isHindi) "सिंचाई प्रबंधन:" else "Irrigation Management:",
                        content = if (isHindi) crop.irrigationHi else crop.irrigation
                    )
                    CropDetailSection(
                        title = if (isHindi) "खाद एवं उर्वरक (NPK):" else "Fertilizer Schedule (NPK):",
                        content = if (isHindi) crop.fertilizerHi else crop.fertilizer
                    )
                    CropDetailSection(
                        title = if (isHindi) "कटाई व अवधि:" else "Harvesting & Duration:",
                        content = if (isHindi) crop.harvestingHi else crop.harvesting
                    )
                    CropDetailSection(
                        title = if (isHindi) "प्रमुख कीट:" else "Major Pests:",
                        content = if (isHindi) crop.pestsHi else crop.pests
                    )
                    CropDetailSection(
                        title = if (isHindi) "प्रमुख रोग:" else "Common Diseases:",
                        content = if (isHindi) crop.diseasesHi else crop.diseases
                    )
                    CropDetailSection(
                        title = if (isHindi) "उन्नत तकनीक व सुझाव:" else "Cultivation Best Practices:",
                        content = if (isHindi) crop.cultivationTipsHi else crop.cultivationTips
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isHindi) "स्रोत: ${crop.source ?: "भाकृअनुप (ICAR)"}" else "Source: ${crop.source ?: "ICAR"}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            // Expand Hint
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) {
                        if (isHindi) "कम विवरण देखें" else "Show Less"
                    } else {
                        if (isHindi) "विस्तृत विवरण देखें" else "Show Detailed Guide"
                    },
                    fontSize = 11.sp,
                    color = GreenPrimary,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun CropDetailSection(title: String, content: String?) {
    if (content.isNullOrBlank()) return
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = GreenPrimary
            )
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextPrimary,
                lineHeight = 18.sp
            )
        )
    }
}
