package com.krishimitra.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.krishimitra.app.domain.model.MarketPrice
import com.krishimitra.app.ui.theme.*

@Composable
fun MandiScreen(
    dbHelper: DatabaseHelper,
    onAskAI: ((String) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("all") }

    val allPrices = remember { dbHelper.getAllMarketPrices() }

    val filterChips = listOf(
        "all" to "सभी (All)",
        "rice" to "धान (Rice)",
        "wheat" to "गेहूं (Wheat)",
        "tomato" to "टमाटर (Tomato)",
        "cotton" to "कपास (Cotton)",
        "potato" to "आलू (Potato)",
        "coconut" to "नारियल (Coconut)",
        "onion" to "प्याज (Onion)"
    )

    val filteredPrices = remember(searchQuery, selectedFilter, allPrices) {
        allPrices.filter { item ->
            val matchesFilter = when (selectedFilter) {
                "all" -> true
                else -> item.cropId == selectedFilter || item.commodity.contains(selectedFilter, ignoreCase = true)
            }
            val matchesSearch = searchQuery.isBlank() ||
                item.commodity.contains(searchQuery, ignoreCase = true) ||
                item.market.contains(searchQuery, ignoreCase = true) ||
                item.district.contains(searchQuery, ignoreCase = true) ||
                item.state.contains(searchQuery, ignoreCase = true)

            matchesFilter && matchesSearch
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
                .padding(bottom = 10.dp),
            placeholder = { Text("मंडी या फसल खोजें (जैसे: Palakkad, Wheat, Tomato)…", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GreenPrimary,
                unfocusedBorderColor = CardBorder,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )

        // Filter chips row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            items(filterChips) { (key, label) ->
                FilterChip(
                    selected = selectedFilter == key,
                    onClick = { selectedFilter = key },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GreenPrimary,
                        selectedLabelColor = Color.White,
                        containerColor = Color.White,
                        labelColor = TextPrimary
                    )
                )
            }
        }

        // Summary Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "कुल दर्ज मंडियां: ${filteredPrices.size} भाव रिकॉर्ड",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "प्रामाणिक स्रोत: Agmarknet, DAFW KCC व राज्य कृषि विपणन बोर्ड",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Price List
        if (filteredPrices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "कोई मंडी भाव नहीं मिला",
                    color = TextSecondary,
                    fontSize = 15.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredPrices) { item ->
                    MandiPriceCard(price = item, onAskClick = onAskAI)
                }
            }
        }
    }
}

@Composable
fun MandiPriceCard(
    price: MarketPrice,
    onAskClick: ((String) -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Commodity Name & Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = price.commodity,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )
                    )
                    if (!price.variety.isNullOrBlank()) {
                        Text(
                            text = "किस्म (Variety): ${price.variety}",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                }

                // Modal Price Box
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${price.modalPrice.toInt()}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF2E7D32)
                        )
                    )
                    Text(
                        text = price.unit,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Location Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFFE65100),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${price.market} (${price.district}, ${price.state})",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Range & Date Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "दायरा: ₹${price.minPrice.toInt()} - ₹${price.maxPrice.toInt()}",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
                Text(
                    text = "दिनांक: ${price.priceDate}",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = CardBorder, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(6.dp))

            // Source Attribution & Ask Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "स्रोत: ${price.source}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF757575),
                        fontSize = 10.sp
                    ),
                    modifier = Modifier.weight(1f)
                )

                if (onAskClick != null) {
                    TextButton(
                        onClick = { onAskClick("${price.commodity} का मंडी भाव क्या है?") },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = GreenPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("पूछें", fontSize = 11.sp, color = GreenPrimary)
                    }
                }
            }
        }
    }
}
