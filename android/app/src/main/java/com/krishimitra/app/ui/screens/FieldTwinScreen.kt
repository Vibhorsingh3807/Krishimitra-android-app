package com.krishimitra.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krishimitra.app.data.repository.FieldDigitalTwinRepository
import com.krishimitra.app.domain.language.AppLanguage
import com.krishimitra.app.domain.language.LanguageManager
import com.krishimitra.app.domain.model.*
import com.krishimitra.app.domain.twin.FieldTwinRuleEngine
import com.krishimitra.app.ui.components.FieldCanvasMap
import com.krishimitra.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldTwinScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddField: () -> Unit,
    onOpenAssistantForZone: (String) -> Unit
) {
    val context = LocalContext.current
    val currentLanguage by LanguageManager.getInstance(context).currentLanguage.collectAsState()
    val isHindi = currentLanguage == AppLanguage.HINDI

    val repository = remember { FieldDigitalTwinRepository(context) }
    var fields by remember { mutableStateOf(repository.getFields()) }
    var selectedField by remember { mutableStateOf(fields.firstOrNull()) }

    var selectedZone by remember { mutableStateOf<FieldZone?>(null) }
    var showIrrigationDialog by remember { mutableStateOf(false) }
    var irrigationAmount by remember { mutableStateOf("35") }
    var irrigationNotes by remember { mutableStateOf("") }

    // Summary data for selected field
    var summary by remember(selectedField) {
        mutableStateOf(selectedField?.let { repository.getFieldTwinSummary(it.id) })
    }

    fun refreshTwin() {
        fields = repository.getFields()
        selectedField = fields.firstOrNull { it.id == selectedField?.id } ?: fields.firstOrNull()
        summary = selectedField?.let { repository.getFieldTwinSummary(it.id) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // TOP APP BAR
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        if (selectedZone != null) {
                            selectedZone = null
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = if (selectedZone != null) {
                                if (isHindi) "${selectedZone?.zoneLabelHi} - विस्तृत विवरण" else "${selectedZone?.zoneLabel} Details"
                            } else {
                                if (isHindi) "🌾 मेरा डिजिटल खेत (Field Twin)" else "🌾 My Field Digital Twin"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isHindi) "डेटा स्रोत: प्रदर्शन डेटा (Demonstration Mode)" else "Source: Demonstration Data (SIH Prototype)",
                            fontSize = 10.5.sp,
                            color = GreenDark,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (selectedZone == null) {
                    Button(
                        onClick = onNavigateToAddField,
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isHindi) "+ नया खेत" else "+ Add Field", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (fields.isEmpty() || selectedField == null) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Landscape, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isHindi) "कोई खेत नहीं जोड़ा गया है" else "No Field Added Yet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isHindi) "अपने खेत की मेड़ बनाकर उसका डिजिटल ट्विन तैयार करें।" else "Define your field boundary to create your living digital twin.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onNavigateToAddField,
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isHindi) "+ पहला खेत जोड़ें" else "+ Add Your First Field", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Main Twin View or Zone Detail View
            if (selectedZone == null) {
                // DASHBOARD VIEW
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Field Selector Chips (if multiple fields)
                    if (fields.size > 1) {
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(fields) { f ->
                                    val isSel = f.id == selectedField?.id
                                    FilterChip(
                                        selected = isSel,
                                        onClick = {
                                            selectedField = f
                                            refreshTwin()
                                        },
                                        label = { Text(f.name, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Field Header Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = selectedField!!.name,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 18.sp,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "${if (isHindi) selectedField!!.cropNameHi else selectedField!!.cropName} • ${selectedField!!.variety ?: "पूसा प्रमाणित"} • ${selectedField!!.areaAcres} एकड़",
                                            fontSize = 12.5.sp,
                                            color = TextSecondary
                                        )
                                    }

                                    Surface(
                                        color = Color(0xFFE8F5E9),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "9 ज़ोन सक्रिय (Active)",
                                            color = GreenDark,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                summary?.lastActionTextHi?.let { lastAct ->
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        color = Color(0xFFE1F5FE),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = "⚡ ", fontSize = 12.sp)
                                            Text(
                                                text = if (isHindi) lastAct else summary?.lastActionText ?: lastAct,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF0277BD)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // FIELD MAP (Centerpiece Canvas)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            elevation = CardDefaults.cardElevation(3.dp)
                        ) {
                            FieldCanvasMap(
                                isEditMode = false,
                                field = selectedField,
                                zones = summary?.zones ?: emptyList(),
                                observations = summary?.latestObservations ?: emptyMap(),
                                selectedZoneId = selectedZone?.id,
                                onZoneSelected = { tappedZone ->
                                    selectedZone = tappedZone
                                }
                            )
                        }
                    }

                    // FIELD SUMMARY COMPACT METRICS
                    item {
                        summary?.let { s ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(1.5.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    SummaryMetricItem(icon = "🌱", label = if (isHindi) "फसल" else "Crop", value = if (isHindi) s.field.cropNameHi.split(" ").first() else s.field.cropName)
                                    SummaryMetricItem(icon = "💧", label = if (isHindi) "औसत नमी" else "Avg Moisture", value = "${s.avgMoisture}%")
                                    SummaryMetricItem(icon = "🌿", label = if (isHindi) "औसत NDVI" else "Avg NDVI", value = String.format("%.2f", s.avgNdvi))
                                    SummaryMetricItem(icon = "⚠️", label = if (isHindi) "ध्यान दें" else "Attention", value = "${s.attentionZonesCount} ज़ोन", isWarning = s.attentionZonesCount > 0)
                                }
                            }
                        }
                    }

                    // PROMINENT CRITICAL ALERT (ZONE 7 WATER STRESS ALERT)
                    item {
                        val criticalRec = summary?.recommendations?.firstOrNull { it.priority == RiskLevel.HIGH }
                        if (criticalRec != null) {
                            val zoneForRec = summary?.zones?.firstOrNull { it.id == criticalRec.zoneId }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "⚠️", fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isHindi) "${criticalRec.titleHi}" else criticalRec.title,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = AlertRed
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (isHindi) criticalRec.recommendationHi else criticalRec.recommendation,
                                        fontSize = 12.sp,
                                        color = TextPrimary,
                                        lineHeight = 17.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Button(
                                            onClick = {
                                                selectedZone = zoneForRec
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (isHindi) "ज़ोन 7 जांचें व सिंचाई करें →" else "Inspect Zone 7 →",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ZONE LIST PREVIEW
                    item {
                        Text(
                            text = if (isHindi) "खेत के सभी 9 प्रबंधन ज़ोन (Management Zones)" else "All 9 Field Management Zones",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                    }

                    summary?.zones?.let { zoneList ->
                        items(zoneList) { z ->
                            val obs = summary?.latestObservations?.get(z.id)
                            ZoneListItem(
                                zone = z,
                                observation = obs,
                                isHindi = isHindi,
                                onClick = {
                                    selectedZone = z
                                }
                            )
                        }
                    }
                }
            } else {
                // ZONE DETAIL VIEW
                val curZone = selectedZone!!
                val obs = summary?.latestObservations?.get(curZone.id)
                val history = remember(curZone.id) { repository.getHistoricalObservations(curZone.id) }
                val rec = remember(curZone.id, obs) {
                    obs?.let { FieldTwinRuleEngine.evaluateZone(curZone, it, history) }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Zone Header
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = if (isHindi) "${curZone.zoneLabelHi} (${curZone.id})" else "${curZone.zoneLabel} (${curZone.id})",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 20.sp,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "${if (isHindi) curZone.cropHi else curZone.crop} • ${curZone.areaAcres} एकड़ क्षेत्रफल",
                                            fontSize = 12.5.sp,
                                            color = TextSecondary
                                        )
                                    }

                                    Surface(
                                        color = when (obs?.riskLevel) {
                                            RiskLevel.HIGH -> Color(0xFFFFEBEE)
                                            RiskLevel.MEDIUM -> Color(0xFFFFF8E1)
                                            else -> Color(0xFFE8F5E9)
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = when (obs?.riskLevel) {
                                                RiskLevel.HIGH -> if (isHindi) "🔴 पानी की कमी" else "🔴 Water Stress"
                                                RiskLevel.MEDIUM -> if (isHindi) "🟡 निगरानी योग्य" else "🟡 Monitor"
                                                else -> if (isHindi) "🟢 स्वस्थ स्थिति" else "🟢 Healthy"
                                            },
                                            color = when (obs?.riskLevel) {
                                                RiskLevel.HIGH -> AlertRed
                                                RiskLevel.MEDIUM -> Color(0xFFF57F17)
                                                else -> GreenDark
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // CURRENT STATE GAUGES
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = if (isHindi) "वर्तमान डिजिटल स्थिति (Current State)" else "Current Digital State",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    ZoneStateGauge(
                                        label = if (isHindi) "💧 मृदा नमी" else "💧 Soil Moisture",
                                        value = "${obs?.moisture ?: 25}%",
                                        trend = if ((obs?.moisture ?: 25) < 20) "↓ गंभीर कम" else "सामान्य",
                                        isWarning = (obs?.moisture ?: 25) < 20
                                    )
                                    ZoneStateGauge(
                                        label = if (isHindi) "🌿 वनस्पति (NDVI)" else "🌿 NDVI Index",
                                        value = String.format("%.2f", obs?.ndvi ?: 0.65f),
                                        trend = if ((obs?.ndvi ?: 0.6f) < 0.5f) "↓ धीमा विकास" else "उत्तम",
                                        isWarning = (obs?.ndvi ?: 0.6f) < 0.5f
                                    )
                                    ZoneStateGauge(
                                        label = if (isHindi) "🌡️ ज़ोन तापमान" else "🌡️ Temperature",
                                        value = "${obs?.temperature?.toInt() ?: 32}°C",
                                        trend = if ((obs?.temperature ?: 32f) > 34f) "↑ अधिक ताप" else "अनुकूल",
                                        isWarning = (obs?.temperature ?: 32f) > 34f
                                    )
                                }
                            }
                        }
                    }

                    // MULTI-TEMPORAL TREND SPARKLINE
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isHindi) "📊 बहु-कालिक नमी गिरावट का रुझान (Trend)" else "📊 Moisture Depletion Trend",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = TextPrimary
                                    )
                                    Text(text = "विगत 30 दिन", fontSize = 11.sp, color = TextSecondary)
                                }
                                Spacer(modifier = Modifier.height(14.dp))

                                // Sparkline Canvas
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(75.dp)
                                ) {
                                    val points = history.map { it.moisture.toFloat() }
                                    if (points.size >= 2) {
                                        val minM = 10f
                                        val maxM = 40f
                                        val w = size.width
                                        val h = size.height
                                        val step = w / (points.size - 1)

                                        val path = Path()
                                        points.forEachIndexed { i, m ->
                                            val x = i * step
                                            val y = h - ((m - minM) / (maxM - minM)) * h
                                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                        }

                                        drawPath(
                                            path = path,
                                            color = if (points.last() < 20f) Color(0xFFE53935) else GreenPrimary,
                                            style = Stroke(width = 4f, cap = StrokeCap.Round)
                                        )

                                        // Draw points
                                        points.forEachIndexed { i, m ->
                                            val x = i * step
                                            val y = h - ((m - minM) / (maxM - minM)) * h
                                            drawCircle(color = Color.White, radius = 6f, center = Offset(x, y))
                                            drawCircle(color = if (points.last() < 20f) Color(0xFFE53935) else GreenPrimary, radius = 3.5f, center = Offset(x, y))
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    history.forEach { h ->
                                        Text(text = "${h.dateString.split(" ").first()}: ${h.moisture}%", fontSize = 10.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }

                    // AGRONOMIC RECOMMENDATION
                    if (rec != null) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = if (rec.priority == RiskLevel.HIGH) Color(0xFFFFEBEE) else Color(0xFFFFF8E1)),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = if (isHindi) "💡 कृषि सिफारिश: ${rec.titleHi}" else "💡 Recommendation: ${rec.title}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (rec.priority == RiskLevel.HIGH) AlertRed else Color(0xFFF57F17)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (isHindi) rec.recommendationHi else rec.recommendation,
                                        fontSize = 12.5.sp,
                                        color = TextPrimary,
                                        lineHeight = 18.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (isHindi) "तार्किक आधार: ${rec.reasonHi}" else "Logic: ${rec.reason}",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // FARMER ACTION LOOP (CRITICAL SIH REQUIREMENT)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = if (isHindi) "🌾 किसान कार्य दर्ज करें (Farmer Action Loop)" else "🌾 Farmer Action Loop",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (isHindi)
                                        "कार्य दर्ज करने पर डिजिटल ट्विन की स्थिति तुरंत स्वतः अपडेट होगी।"
                                    else
                                        "Recording an action updates the digital twin's state dynamically.",
                                    fontSize = 11.5.sp,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { showIrrigationDialog = true },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(text = if (isHindi) "✓ सिंचाई दर्ज करें" else "✓ Mark Irrigated", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            Toast.makeText(context, if (isHindi) "निरीक्षण कार्य दर्ज किया गया।" else "Inspection logged.", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(text = if (isHindi) "✓ निरीक्षण पूर्ण" else "✓ Mark Inspected", fontSize = 12.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Ask AI Assistant Button
                                obs?.let { o ->
                                    Button(
                                        onClick = {
                                            val prompt = FieldTwinRuleEngine.buildAssistantPrompt(curZone, o, isHindi)
                                            onOpenAssistantForZone(prompt)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isHindi) "इस ज़ोन पर AI कृषि साथी से सलाह लें" else "Ask AI Assistant About This Zone",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // IRRIGATION DIALOG (Action feedback loop)
    if (showIrrigationDialog) {
        AlertDialog(
            onDismissRequest = { showIrrigationDialog = false },
            title = {
                Text(
                    text = if (isHindi) "सिंचाई कार्य दर्ज करें" else "Record Irrigation Event",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isHindi)
                            "ज़ोन ${selectedZone?.id} में दी गई पानी की मात्रा दर्ज करें:"
                        else
                            "Enter irrigation amount provided to Zone ${selectedZone?.id}:",
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = irrigationAmount,
                        onValueChange = { irrigationAmount = it },
                        label = { Text(if (isHindi) "मात्रा (मिलीमीटर / mm)" else "Amount (mm)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = irrigationNotes,
                        onValueChange = { irrigationNotes = it },
                        label = { Text(if (isHindi) "टिप्पणी (वैकल्पिक)" else "Notes (Optional)") },
                        placeholder = { Text(if (isHindi) "उदा. ड्रिप सिंचाई या स्प्रिंकलर" else "e.g. Drip or flood irrigation") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = irrigationAmount.toFloatOrNull() ?: 35f
                        selectedField?.let { f ->
                            selectedZone?.let { z ->
                                repository.recordIrrigation(f.id, z.id, amount, irrigationNotes)
                                refreshTwin()
                                Toast.makeText(
                                    context,
                                    if (isHindi) "✓ डिजिटल ट्विन अपडेट हुआ! ज़ोन ${z.id} की नमी सुधरकर 32% हुई।" else "✓ Digital Twin updated! Zone ${z.id} moisture restored to 32%.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        showIrrigationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) {
                    Text(if (isHindi) "सहेजें व ट्विन अपडेट करें" else "Save & Update Twin")
                }
            },
            dismissButton = {
                TextButton(onClick = { showIrrigationDialog = false }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }
}

@Composable
private fun SummaryMetricItem(icon: String, label: String, value: String, isWarning: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = icon, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, fontSize = 10.5.sp, color = TextSecondary)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isWarning) AlertRed else TextPrimary
        )
    }
}

@Composable
private fun ZoneListItem(
    zone: FieldZone,
    observation: ZoneObservation?,
    isHindi: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (observation?.riskLevel) {
                                RiskLevel.HIGH -> Color(0xFFFFEBEE)
                                RiskLevel.MEDIUM -> Color(0xFFFFF8E1)
                                else -> Color(0xFFE8F5E9)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = zone.id,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = when (observation?.riskLevel) {
                            RiskLevel.HIGH -> AlertRed
                            RiskLevel.MEDIUM -> Color(0xFFF57F17)
                            else -> GreenDark
                        }
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isHindi) zone.zoneLabelHi else zone.zoneLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "नमी: ${observation?.moisture ?: 25}% • NDVI: ${String.format("%.2f", observation?.ndvi ?: 0.65f)}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = when (observation?.riskLevel) {
                        RiskLevel.HIGH -> Color(0xFFFFEBEE)
                        RiskLevel.MEDIUM -> Color(0xFFFFF8E1)
                        else -> Color(0xFFE8F5E9)
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = when (observation?.riskLevel) {
                            RiskLevel.HIGH -> if (isHindi) "🔴 पानी की कमी" else "🔴 Water Stress"
                            RiskLevel.MEDIUM -> if (isHindi) "🟡 मध्यम" else "🟡 Moderate"
                            else -> if (isHindi) "🟢 उत्तम" else "🟢 Good"
                        },
                        color = when (observation?.riskLevel) {
                            RiskLevel.HIGH -> AlertRed
                            RiskLevel.MEDIUM -> Color(0xFFF57F17)
                            else -> GreenDark
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun ZoneStateGauge(label: String, value: String, trend: String, isWarning: Boolean) {
    Column(
        modifier = Modifier
            .width(95.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isWarning) Color(0xFFFFEBEE) else Color(0xFFF1F8E9))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, fontSize = 10.sp, color = TextSecondary, maxLines = 1)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isWarning) AlertRed else GreenDark
        )
        Text(
            text = trend,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isWarning) AlertRed else GreenPrimary
        )
    }
}
