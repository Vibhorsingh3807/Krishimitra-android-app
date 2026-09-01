package com.krishimitra.app.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.krishimitra.app.R
import com.krishimitra.app.data.location.DeviceLocationProvider
import com.krishimitra.app.data.remote.ApiClient
import com.krishimitra.app.domain.model.WeatherDayForecast
import com.krishimitra.app.domain.model.WeatherInfo
import com.krishimitra.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun WeatherScreen(apiClient: ApiClient) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val locationProvider = remember { DeviceLocationProvider(context) }

    var isLoading by remember { mutableStateOf(false) }
    var locationStatusText by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf("gps") }

    val presetDistricts = listOf(
        "gps" to "📍 वर्तमान स्थान (GPS)",
        "delhi" to "दिल्ली (Delhi)",
        "lucknow" to "लखनऊ (Lucknow)",
        "patna" to "पटना (Patna)",
        "bhopal" to "भोपाल (Bhopal)",
        "jaipur" to "जयपुर (Jaipur)",
        "ludhiana" to "लुधियाना (Ludhiana)",
        "karnal" to "करनाल (Karnal)"
    )

    // Initial Weather State with Cached Fallback
    var weatherData by remember { mutableStateOf(loadCachedWeather(context) ?: getDefaultWeather()) }

    fun refreshWeather(presetKey: String = selectedPreset) {
        isLoading = true
        selectedPreset = presetKey
        coroutineScope.launch {
            try {
                if (presetKey == "gps") {
                    val loc = locationProvider.getLastKnownLocation()
                    val lat = loc?.latitude ?: 28.6139
                    val lon = loc?.longitude ?: 77.2090
                    val resolvedName = if (loc != null) locationProvider.getResolvedLocationName(lat, lon) else "दिल्ली / उत्तर भारत"
                    locationStatusText = resolvedName

                    val fetched = apiClient.fetchWeather(lat, lon, resolvedName) ?: apiClient.fetchOpenMeteoWeather(lat, lon, resolvedName)
                    if (fetched != null) {
                        weatherData = fetched
                        saveWeatherToPrefs(context, fetched)
                    }
                } else {
                    val (lat, lon, name) = getPresetCoordinates(presetKey)
                    locationStatusText = name
                    val fetched = apiClient.fetchWeather(lat, lon, name) ?: apiClient.fetchOpenMeteoWeather(lat, lon, name)
                    if (fetched != null) {
                        weatherData = fetched
                        saveWeatherToPrefs(context, fetched)
                    }
                }
            } catch (e: Exception) {
                // Offline or failure fallback
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshWeather("gps")
    }

    val isHindi = java.util.Locale.getDefault().language == "hi"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // District / Location Selector Chips
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHindi) "स्थान चुनें (Select Location)" else "Select Location",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )

                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = GreenPrimary, strokeWidth = 2.dp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presetDistricts) { (key, name) ->
                        val isSelected = selectedPreset == key
                        Surface(
                            modifier = Modifier.clickable { refreshWeather(key) },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) GreenPrimary else Color.White,
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                        ) {
                            Text(
                                text = name,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = if (isSelected) Color.White else TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.5.sp
                                )
                            )
                        }
                    }
                }
            }
        }

        // CURRENT WEATHER CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF0277BD), Color(0xFF01579B))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFFB3E5FC),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = weatherData.location,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = if (isHindi) weatherData.conditionHi else weatherData.condition,
                                    color = Color(0xFFB3E5FC),
                                    fontSize = 14.sp
                                )
                            }
                            Text(
                                text = "${weatherData.temperature.toInt()}°C",
                                color = Color.White,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0x33FFFFFF), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(14.dp))

                        // Stats Grid (Humidity, Wind, Rain)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            WeatherStatItem(
                                icon = Icons.Default.WaterDrop,
                                label = if (isHindi) "नमी (Humidity)" else "Humidity",
                                value = "${weatherData.humidity}%"
                            )
                            WeatherStatItem(
                                icon = Icons.Default.Air,
                                label = if (isHindi) "हवा की गति" else "Wind Speed",
                                value = "${weatherData.windSpeed} km/h"
                            )
                            WeatherStatItem(
                                icon = Icons.Default.Umbrella,
                                label = if (isHindi) "बारिश संभावना" else "Rain Prob",
                                value = "${weatherData.rainfallProb}%"
                            )
                        }
                    }
                }
            }
        }

        // FARMING OUTLOOK CARD (Farmer Actionable Insights)
        item {
            val isHighRain = weatherData.rainfallProb >= 50
            val isHighHeat = weatherData.temperature >= 35f
            val isHighHumidity = weatherData.humidity >= 70

            val cardBg = when {
                isHighRain || isHighHeat -> Color(0xFFFFF3E0)
                else -> Color(0xFFE8F5E9)
            }
            val accentColor = when {
                isHighRain || isHighHeat -> AlertRed
                else -> GreenPrimary
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Agriculture,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isHindi) "🌾 खेती के लिए महत्वपूर्ण सलाह (Farming Outlook)" else "🌾 Farming Outlook & Advisory",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                fontSize = 15.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isHindi) weatherData.advisoryHi else weatherData.advisoryEn,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextPrimary,
                            fontSize = 13.5.sp,
                            lineHeight = 20.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Specific Action Signals
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isHighRain) {
                            AgriActionTag(text = if (isHindi) "🌧️ सिंचाई रोकें" else "🌧️ Hold Irrigation", bgColor = Color(0xFFFFEBEE), textColor = AlertRed)
                        } else {
                            AgriActionTag(text = if (isHindi) "💧 नियमित सिंचाई करें" else "💧 Scheduled Irrigation", bgColor = Color(0xFFE8F5E9), textColor = GreenDark)
                        }

                        if (isHighHumidity) {
                            AgriActionTag(text = if (isHindi) "🦠 फफूंद रोग निगरानी" else "🦠 Monitor Fungal Risk", bgColor = Color(0xFFFFF8E1), textColor = WarningOrange)
                        }

                        if (weatherData.windSpeed > 18f) {
                            AgriActionTag(text = if (isHindi) "💨 स्प्रे न करें" else "💨 Avoid Spraying", bgColor = Color(0xFFFFF3E0), textColor = Color(0xFFE65100))
                        }
                    }
                }
            }
        }

        // 5-DAY FORECAST LIST
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isHindi) "5-दिवसीय मौसम पूर्वानुमान" else "5-Day Agricultural Forecast",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 16.sp
                    )
                )

                Text(
                    text = if (weatherData.isOffline) "ऑफ़लाइन कैश डेटा" else "लाइव डेटा",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        items(weatherData.forecast) { day ->
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text(
                            text = day.date,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        )
                        Text(
                            text = if (isHindi) day.conditionHi else day.conditionHi,
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.5.sp)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = if (isHindi) day.advisoryHi else day.advisoryHi,
                            style = MaterialTheme.typography.bodySmall.copy(color = GreenPrimary, fontSize = 11.sp),
                            maxLines = 2
                        )
                    }

                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(0.8f)) {
                        Text(
                            text = "${day.maxTemp.toInt()}° / ${day.minTemp.toInt()}°C",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 15.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (day.rainProb >= 50) Color(0xFFFFEBEE) else Color(0xFFE1F5FE)
                        ) {
                            Text(
                                text = "बारिश: ${day.rainProb}%",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (day.rainProb >= 50) AlertRed else Color(0xFF0288D1),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AgriActionTag(text: String, bgColor: Color, textColor: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun getPresetCoordinates(key: String): Triple<Double, Double, String> {
    return when (key) {
        "lucknow" -> Triple(26.8467, 80.9462, "लखनऊ, उत्तर प्रदेश")
        "patna" -> Triple(25.5941, 85.1376, "पटना, बिहार")
        "bhopal" -> Triple(23.2599, 77.4126, "भोपाल, मध्य प्रदेश")
        "jaipur" -> Triple(26.9124, 75.7873, "जयपुर, राजस्थान")
        "ludhiana" -> Triple(30.9010, 75.8573, "लुधियाना, पंजाब")
        "karnal" -> Triple(29.6857, 76.9905, "करनाल, हरियाणा")
        else -> Triple(28.6139, 77.2090, "दिल्ली / उत्तर भारत")
    }
}

private fun getDefaultWeather(): WeatherInfo {
    return WeatherInfo(
        location = "दिल्ली / उत्तर भारत",
        temperature = 31.0f,
        humidity = 64,
        windSpeed = 11.5f,
        rainfallProb = 15,
        condition = "Clear",
        conditionHi = "साफ धूप",
        advisoryEn = "Favorable agricultural weather. Ideal time for field weeding and scheduled irrigation.",
        advisoryHi = "मौसम कृषि कार्यों के अनुकूल है। खेत की तैयारी, निराई-गुड़ाई और सिंचाई हेतु उपयुक्त समय है।",
        forecast = listOf(
            WeatherDayForecast("आज", 32f, 23f, 15, "साफ धूप", "सामान्य कृषि कार्य जारी रखें।"),
            WeatherDayForecast("कल", 31f, 22f, 20, "साफ धूप", "शाम को हल्की सिंचाई करें।"),
            WeatherDayForecast("परसों", 29f, 21f, 45, "हल्के बादल", "दवा छिड़काव से पहले मौसम देखें।"),
            WeatherDayForecast("दिन 4", 28f, 20f, 60, "वर्षा संभावना", "सिंचाई व कीटनाशक छिड़काव स्थगित रखें।"),
            WeatherDayForecast("दिन 5", 30f, 21f, 25, "सामान्य", "जलभराव न होने दें।")
        ),
        isOffline = false
    )
}

private fun saveWeatherToPrefs(context: Context, weather: WeatherInfo) {
    try {
        val prefs = context.getSharedPreferences("krishi_weather_prefs", Context.MODE_PRIVATE)
        val json = Gson().toJson(weather)
        prefs.edit().putString("cached_weather", json).putLong("cached_time", System.currentTimeMillis()).apply()
    } catch (e: Exception) {
    }
}

private fun loadCachedWeather(context: Context): WeatherInfo? {
    return try {
        val prefs = context.getSharedPreferences("krishi_weather_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("cached_weather", null) ?: return null
        val obj = Gson().fromJson(json, WeatherInfo::class.java)
        obj.copy(isOffline = true)
    } catch (e: Exception) {
        null
    }
}

@Composable
fun WeatherStatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFB3E5FC),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = Color(0xFFE1F5FE), fontSize = 11.sp)
        Text(text = value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
