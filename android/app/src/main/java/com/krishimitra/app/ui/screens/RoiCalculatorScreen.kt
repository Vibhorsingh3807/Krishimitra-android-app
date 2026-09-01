package com.krishimitra.app.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.krishimitra.app.data.local.DatabaseHelper
import com.krishimitra.app.domain.language.AppLanguage
import com.krishimitra.app.domain.language.LanguageManager
import com.krishimitra.app.domain.model.Crop
import com.krishimitra.app.ui.theme.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SavedRoiEstimate(
    val id: String = java.util.UUID.randomUUID().toString(),
    val cropName: String,
    val landAreaText: String,
    val totalInvestment: Double,
    val totalRevenue: Double,
    val netProfit: Double,
    val roiPercent: Double,
    val dateStr: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoiCalculatorScreen(
    dbHelper: DatabaseHelper
) {
    val context = LocalContext.current
    val currentLanguage by LanguageManager.getInstance(context).currentLanguage.collectAsState()
    val isHindi = currentLanguage == AppLanguage.HINDI

    val df = remember { DecimalFormat("#,##,##0.#") }
    val dfPercent = remember { DecimalFormat("#0.0") }

    // 1. Crop List
    val dbCrops = remember {
        try {
            dbHelper.getAllCrops()
        } catch (e: Throwable) {
            emptyList<Crop>()
        }
    }
    val cropNames = remember(dbCrops, isHindi) {
        if (dbCrops.isNotEmpty()) {
            dbCrops.map { if (isHindi) it.nameHi else it.nameEn }
        } else {
            if (isHindi) {
                listOf("गेहूं", "धान (चावल)", "कपास", "मक्का", "सरसों", "सोयाबीन", "चना", "टमाटर", "आलू", "प्याज")
            } else {
                listOf("Wheat", "Rice (Paddy)", "Cotton", "Maize", "Mustard", "Soybean", "Chickpea", "Tomato", "Potato", "Onion")
            }
        }
    }

    var selectedCrop by remember(isHindi) {
        mutableStateOf(cropNames.firstOrNull() ?: (if (isHindi) "गेहूं" else "Wheat"))
    }
    var isCropDropdownExpanded by remember { mutableStateOf(false) }

    // 2. Land Details
    var areaInput by remember { mutableStateOf("5") }
    var isHectares by remember { mutableStateOf(false) } // false = Acres, true = Hectares
    var isRentedLand by remember { mutableStateOf(false) }
    var landRentInput by remember { mutableStateOf("4000") } // Rent or opportunity cost per acre/hectare

    // 3. Operating Costs Inputs
    var seedCostInput by remember { mutableStateOf("5000") }
    var fertilizerCostInput by remember { mutableStateOf("8000") }
    var protectionCostInput by remember { mutableStateOf("3500") }

    // Labour
    var numLabourersInput by remember { mutableStateOf("4") }
    var labourDaysInput by remember { mutableStateOf("5") }
    var dailyWageInput by remember { mutableStateOf("450") }

    var machineryCostInput by remember { mutableStateOf("6000") }
    var irrigationCostInput by remember { mutableStateOf("3000") }
    var transportCostInput by remember { mutableStateOf("2500") }
    var miscCostInput by remember { mutableStateOf("2000") }

    // 4. Yield & Price
    var expectedYieldInput by remember { mutableStateOf("75") } // e.g. 75 Quintals
    val yieldUnit = if (isHindi) "कुंतल" else "Quintal"
    var expectedPriceInput by remember { mutableStateOf("2275") } // e.g. ₹2275 / Quintal

    // What-if Sensitivity Factors
    var priceSensitivityPercent by remember { mutableStateOf(0f) } // -20%, -10%, 0%, +10%, +20%
    var yieldSensitivityPercent by remember { mutableStateOf(0f) }

    // Expandable Sections State
    var isAdvancedExpensesOpen by remember { mutableStateOf(true) }
    var isSensitivityOpen by remember { mutableStateOf(false) }
    var isHistoryOpen by remember { mutableStateOf(false) }

    // History Saved State
    var savedEstimates by remember { mutableStateOf(loadSavedEstimates(context)) }
    var showSaveToast by remember { mutableStateOf(false) }

    // --- COMPUTATION ENGINE ---
    val landArea = areaInput.toDoubleOrNull() ?: 0.0
    val areaInAcres = if (isHectares) landArea * 2.47105 else landArea
    val landRentRate = landRentInput.toDoubleOrNull() ?: 0.0
    val landTotalCost = landArea * landRentRate

    val seedCost = seedCostInput.toDoubleOrNull() ?: 0.0
    val fertilizerCost = fertilizerCostInput.toDoubleOrNull() ?: 0.0
    val protectionCost = protectionCostInput.toDoubleOrNull() ?: 0.0

    val numLabourers = numLabourersInput.toDoubleOrNull() ?: 0.0
    val labourDays = labourDaysInput.toDoubleOrNull() ?: 0.0
    val dailyWage = dailyWageInput.toDoubleOrNull() ?: 0.0
    val labourCost = numLabourers * labourDays * dailyWage

    val machineryCost = machineryCostInput.toDoubleOrNull() ?: 0.0
    val irrigationCost = irrigationCostInput.toDoubleOrNull() ?: 0.0
    val transportCost = transportCostInput.toDoubleOrNull() ?: 0.0
    val miscCost = miscCostInput.toDoubleOrNull() ?: 0.0

    val totalVariableCost = seedCost + fertilizerCost + protectionCost + labourCost + machineryCost + irrigationCost + transportCost + miscCost
    val totalInvestment = totalVariableCost + landTotalCost

    // Base Yield & Price
    val baseYield = expectedYieldInput.toDoubleOrNull() ?: 0.0
    val basePrice = expectedPriceInput.toDoubleOrNull() ?: 0.0

    // Adjusted Yield & Price for What-If
    val effectiveYield = baseYield * (1.0 + yieldSensitivityPercent / 100.0)
    val effectivePrice = basePrice * (1.0 + priceSensitivityPercent / 100.0)

    val totalRevenue = effectiveYield * effectivePrice
    val netProfit = totalRevenue - totalInvestment
    val roiPercent = if (totalInvestment > 0) ((totalRevenue - totalInvestment) / totalInvestment) * 100.0 else 0.0

    val breakEvenPrice = if (effectiveYield > 0) totalInvestment / effectiveYield else 0.0
    val breakEvenYield = if (effectivePrice > 0) totalInvestment / effectivePrice else 0.0

    val investmentPerAcre = if (areaInAcres > 0) totalInvestment / areaInAcres else 0.0
    val profitPerAcre = if (areaInAcres > 0) netProfit / areaInAcres else 0.0

    val isProfitable = netProfit >= 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFBE9E7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = null,
                        tint = Color(0xFFD84315),
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = if (isHindi) "फसल निवेश व लाभ कैलकुलेटर" else "Crop Investment & ROI Calculator",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isHindi)
                            "बीज, खाद, मजदूर व सिंचाई से कुल मुनाफा एवं ब्रेक-इवन निकालें"
                        else
                            "Calculate total costs, profit/loss & break-even selling price",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Section 1: Crop & Land Setup
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Grass, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "1. फसल एवं भूमि विवरण" else "1. Crop & Land Setup",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = GreenPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Crop Selector
                ExposedDropdownMenuBox(
                    expanded = isCropDropdownExpanded,
                    onExpandedChange = { isCropDropdownExpanded = !isCropDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCrop,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (isHindi) "फसल चुनें" else "Select Crop") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCropDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = isCropDropdownExpanded,
                        onDismissRequest = { isCropDropdownExpanded = false }
                    ) {
                        cropNames.forEach { name ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    selectedCrop = name
                                    isCropDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Area Input & Unit Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = areaInput,
                        onValueChange = { areaInput = it },
                        label = { Text(if (isHindi) "कुल क्षेत्रफल" else "Total Area") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
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

                Spacer(modifier = Modifier.height(12.dp))

                // Land Ownership Options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !isRentedLand,
                        onClick = { isRentedLand = false },
                        label = { Text(if (isHindi) "खुद की जमीन (Owned)" else "Owned Land") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isRentedLand,
                        onClick = { isRentedLand = true },
                        label = { Text(if (isHindi) "किराया / बटाई (Leased)" else "Leased / Rented") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = landRentInput,
                    onValueChange = { landRentInput = it },
                    label = {
                        Text(
                            if (isHindi)
                                (if (isRentedLand) "किराया प्रति ${if (isHectares) "हेक्टेयर" else "एकड़"} (₹)" else "भूमि अवसर लागत / अनुमानित किराया (₹)")
                            else
                                (if (isRentedLand) "Rent per ${if (isHectares) "Hectare" else "Acre"} (₹)" else "Land Opportunity Cost / Imputed Rent (₹)")
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Section 2: Cultivation Costs (Inputs)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isAdvancedExpensesOpen = !isAdvancedExpensesOpen },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Payments, contentDescription = null, tint = AmberSecondary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "2. खेती की परिचालन लागत" else "2. Cultivation & Operating Costs",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                    }
                    Icon(
                        imageVector = if (isAdvancedExpensesOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }

                AnimatedVisibility(
                    visible = isAdvancedExpensesOpen,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Seed & Fertilizer
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = seedCostInput,
                                onValueChange = { seedCostInput = it },
                                label = { Text(if (isHindi) "बीज खर्च (Seeds ₹)" else "Seeds Cost (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = fertilizerCostInput,
                                onValueChange = { fertilizerCostInput = it },
                                label = { Text(if (isHindi) "खाद व उर्वरक (₹)" else "Fertilizers (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Crop Protection & Irrigation
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = protectionCostInput,
                                onValueChange = { protectionCostInput = it },
                                label = { Text(if (isHindi) "कीटनाशक व दवा (₹)" else "Crop Protection (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = irrigationCostInput,
                                onValueChange = { irrigationCostInput = it },
                                label = { Text(if (isHindi) "सिंचाई व बिजली (₹)" else "Irrigation Cost (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Labour Costs Section
                        Text(
                            text = if (isHindi) "मजदूरी व्यय (Labour Costs):" else "Labour Costs Breakdown:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = numLabourersInput,
                                onValueChange = { numLabourersInput = it },
                                label = { Text(if (isHindi) "मजदूर (No.)" else "Labourers") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = labourDaysInput,
                                onValueChange = { labourDaysInput = it },
                                label = { Text(if (isHindi) "दिन (Days)" else "Work Days") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = dailyWageInput,
                                onValueChange = { dailyWageInput = it },
                                label = { Text(if (isHindi) "दहाड़ी (₹/Day)" else "Daily Wage") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Machinery, Transport & Misc
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = machineryCostInput,
                                onValueChange = { machineryCostInput = it },
                                label = { Text(if (isHindi) "ट्रैक्टर व जुताई (₹)" else "Machinery/Diesel (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = transportCostInput,
                                onValueChange = { transportCostInput = it },
                                label = { Text(if (isHindi) "कटाई व ढुलाई (₹)" else "Harvest & Freight (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        OutlinedTextField(
                            value = miscCostInput,
                            onValueChange = { miscCostInput = it },
                            label = { Text(if (isHindi) "अन्य अनावृत खर्च (Misc Expenses ₹)" else "Miscellaneous Expenses (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        // Section 3: Expected Production & Price
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF0288D1), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "3. अनुमानित पैदावार व बिक्री मूल्य" else "3. Expected Yield & Selling Price",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF0288D1)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = expectedYieldInput,
                        onValueChange = { expectedYieldInput = it },
                        label = { Text(if (isHindi) "कुल उपज ($yieldUnit)" else "Total Yield ($yieldUnit)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = expectedPriceInput,
                        onValueChange = { expectedPriceInput = it },
                        label = { Text(if (isHindi) "अनुमानित भाव (₹/$yieldUnit)" else "Market Price (₹/$yieldUnit)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Section 4: RESULTS SUMMARY CARD (Main ROI & Profit Calculation Output)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = if (isProfitable) Color(0xFFF1F8E9) else Color(0xFFFFEBEE)),
            elevation = CardDefaults.cardElevation(3.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHindi) "अनुमानित वित्तीय परिणाम" else "Financial ROI Summary",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (isProfitable) GreenDark else AlertRed
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isProfitable) GreenPrimary else AlertRed
                    ) {
                        Text(
                            text = "ROI: ${dfPercent.format(roiPercent)}%",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.Black.copy(alpha = 0.1f), thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Primary Numbers Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(if (isHindi) "कुल निवेश लागत" else "Total Investment", fontSize = 12.sp, color = TextSecondary)
                        Text("₹${df.format(totalInvestment)}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Column {
                        Text(if (isHindi) "अनुमानित आय" else "Expected Revenue", fontSize = 12.sp, color = TextSecondary)
                        Text("₹${df.format(totalRevenue)}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0288D1))
                    }
                    Column {
                        Text(
                            if (isProfitable)
                                (if (isHindi) "शुद्ध मुनाफा" else "Net Profit")
                            else
                                (if (isHindi) "शुद्ध घाटा" else "Net Loss"),
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            "₹${df.format(netProfit)}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isProfitable) GreenPrimary else AlertRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Secondary Per-Area Numbers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.7f))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (isHindi) "प्रति एकड़ लागत" else "Cost per Acre", fontSize = 11.sp, color = TextSecondary)
                        Text("₹${df.format(investmentPerAcre)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (isHindi) "प्रति एकड़ मुनाफा" else "Profit per Acre", fontSize = 11.sp, color = TextSecondary)
                        Text("₹${df.format(profitPerAcre)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isProfitable) GreenDark else AlertRed)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Break-Even Analysis Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(if (isHindi) "ब्रेक-इवन मूल्य" else "Break-even Price", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            Text("₹${df.format(breakEvenPrice)} / $yieldUnit", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AmberSecondary)
                            Text(if (isHindi) "लागत निकालने हेतु न्यूनतम भाव" else "Min price to cover cost", fontSize = 9.5.sp, color = TextSecondary)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(if (isHindi) "ब्रेक-इवन उपज" else "Break-even Yield", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            Text("${dfPercent.format(breakEvenYield)} $yieldUnit", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6A1B9A))
                            Text(if (isHindi) "लागत वसूल करने हेतु न्यूनतम उपज" else "Min yield to cover cost", fontSize = 9.5.sp, color = TextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Farmer Friendly Interpretation
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isProfitable) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isProfitable) GreenPrimary else AlertRed,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isHindi) {
                                if (isProfitable)
                                    "आपकी अनुमानित आय (₹${df.format(totalRevenue)}) खेती की कुल लागत (₹${df.format(totalInvestment)}) से अधिक है। ₹${df.format(breakEvenPrice)} प्रति $yieldUnit से ऊपर भाव मिलने पर आपको सीधा सकारात्मक मुनाफा प्राप्त होगा।"
                                else
                                    "आपकी अनुमानित लागत (₹${df.format(totalInvestment)}) संभावित आय से अधिक है। कृपया बीज, खाद या जुताई खर्च कम करने अथवा बेहतर मंडी भाव प्राप्त करने का प्रयास करें।"
                            } else {
                                if (isProfitable)
                                    "Your projected revenue (₹${df.format(totalRevenue)}) exceeds total costs (₹${df.format(totalInvestment)}). Any selling price above ₹${df.format(breakEvenPrice)}/$yieldUnit delivers positive net returns."
                                else
                                    "Your total expenses (₹${df.format(totalInvestment)}) currently exceed projected income. Consider optimizing fertilizer, tillage, or targeting higher mandi prices."
                            },
                            fontSize = 11.5.sp,
                            color = TextPrimary,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons: Save Estimate
                Button(
                    onClick = {
                        val newEst = SavedRoiEstimate(
                            cropName = selectedCrop,
                            landAreaText = "$areaInput ${if (isHectares) "Hectares" else "Acres"}",
                            totalInvestment = totalInvestment,
                            totalRevenue = totalRevenue,
                            netProfit = netProfit,
                            roiPercent = roiPercent,
                            dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                        )
                        val updated = listOf(newEst) + savedEstimates
                        savedEstimates = updated
                        saveEstimatesToPrefs(context, updated)
                        showSaveToast = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "यह गणना सहेजें (Save Estimate)" else "Save This Estimate",
                        fontWeight = FontWeight.Bold
                    )
                }

                if (showSaveToast) {
                    Text(
                        text = if (isHindi) "✓ गणना सफलतापूर्वक सहेजी गई!" else "✓ Estimate saved successfully!",
                        color = GreenDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 4.dp)
                    )
                }
            }
        }

        // Section 5: WHAT-IF SENSITIVITY ANALYSIS
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isSensitivityOpen = !isSensitivityOpen },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Analytics, contentDescription = null, tint = Color(0xFF6A1B9A), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "4. 'यदि बाजार बदला तो?' (What-if Scenario)" else "4. Price & Yield Sensitivity (What-If)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF6A1B9A)
                        )
                    }
                    Icon(
                        imageVector = if (isSensitivityOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }

                AnimatedVisibility(
                    visible = isSensitivityOpen,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Text(
                            text = if (isHindi) "मंडी भाव में परिवर्तन (Selling Price Variance):" else "Market Price Fluctuation:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf(-20f, -10f, 0f, 10f, 20f).forEach { pct ->
                                FilterChip(
                                    selected = priceSensitivityPercent == pct,
                                    onClick = { priceSensitivityPercent = pct },
                                    label = { Text("${if (pct > 0) "+" else ""}${pct.toInt()}%") }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (isHindi) "उपज में बदलाव (Yield Variance):" else "Yield Fluctuation:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf(-20f, -10f, 0f, 10f, 20f).forEach { pct ->
                                FilterChip(
                                    selected = yieldSensitivityPercent == pct,
                                    onClick = { yieldSensitivityPercent = pct },
                                    label = { Text("${if (pct > 0) "+" else ""}${pct.toInt()}%") }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = CardBorder, thickness = 0.6.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(if (isHindi) "समायोजित आय" else "Adjusted Revenue", fontSize = 11.sp, color = TextSecondary)
                                Text("₹${df.format(totalRevenue)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0288D1))
                            }
                            Column {
                                Text(if (isHindi) "समायोजित मुनाफा" else "Adjusted Profit", fontSize = 11.sp, color = TextSecondary)
                                Text("₹${df.format(netProfit)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isProfitable) GreenPrimary else AlertRed)
                            }
                            Column {
                                Text(if (isHindi) "समायोजित ROI" else "Adjusted ROI", fontSize = 11.sp, color = TextSecondary)
                                Text("${dfPercent.format(roiPercent)}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }
                    }
                }
            }
        }

        // Section 6: SAVED CALCULATIONS HISTORY
        if (savedEstimates.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isHistoryOpen = !isHistoryOpen },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.History, contentDescription = null, tint = GreenDark, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${if (isHindi) "पिछली सहेजी गई गणनाएं" else "Previous Saved Estimates"} (${savedEstimates.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                        }
                        Icon(
                            imageVector = if (isHistoryOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }

                    AnimatedVisibility(
                        visible = isHistoryOpen,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            savedEstimates.take(5).forEach { est ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = BackgroundLight),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(text = "${est.cropName} (${est.landAreaText})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(
                                                text = "${if (isHindi) "निवेश: " else "Cost: "}₹${df.format(est.totalInvestment)} | ${est.dateStr}",
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "₹${df.format(est.netProfit)}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (est.netProfit >= 0) GreenPrimary else AlertRed
                                            )
                                            Text(text = "ROI: ${dfPercent.format(est.roiPercent)}%", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
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
}

private fun loadSavedEstimates(context: Context): List<SavedRoiEstimate> {
    return try {
        val prefs = context.getSharedPreferences("krishi_roi_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("saved_estimates", null) ?: return emptyList()
        val type = object : TypeToken<List<SavedRoiEstimate>>() {}.type
        Gson().fromJson(json, type) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}

private fun saveEstimatesToPrefs(context: Context, list: List<SavedRoiEstimate>) {
    try {
        val prefs = context.getSharedPreferences("krishi_roi_prefs", Context.MODE_PRIVATE)
        val json = Gson().toJson(list)
        prefs.edit().putString("saved_estimates", json).apply()
    } catch (e: Exception) {
        Log.e("RoiCalculator", "Error saving estimates: ${e.message}")
    }
}
