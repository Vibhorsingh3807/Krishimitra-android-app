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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.krishimitra.app.data.location.DeviceLocationProvider
import com.krishimitra.app.data.remote.ApiClient
import com.krishimitra.app.domain.language.AppLanguage
import com.krishimitra.app.domain.language.LanguageManager
import com.krishimitra.app.domain.model.WeatherDayForecast
import com.krishimitra.app.domain.model.WeatherInfo
import com.krishimitra.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun WeatherScreen(apiClient: ApiClient) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentLanguage by LanguageManager.getInstance(context).currentLanguage.collectAsState()
    val isHindi = currentLanguage == AppLanguage.HINDI

    val locationProvider = remember { DeviceLocationProvider(context) }

    var isLoading by remember { mutableStateOf(false) }
    var locationStatusText by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf("gps") }

    val presetDistricts = listOf(
        "gps" to if (isHindi) "📍 वर्तमान स्थान (GPS)" else "📍 Current GPS",
        "delhi" to if (isHindi) "दिल्ली (Delhi)" else "Delhi",
        "lucknow" to if (isHindi) "लखनऊ (Lucknow)" else "Lucknow",
        "patna" to if (isHindi) "पटना (Patna)" else "Patna",
        "bhopal" to if (isHindi) "भोपाल (Bhopal)" else "Bhopal",
        "jaipur" to if (isHindi) "जयपुर (Jaipur)" else "Jaipur",
        "ludhiana" to if (isHindi) "लुधियाना (Ludhiana)" else "Ludhiana",
        "karnal" to if (isHindi) "करनाल (Karnal)" else "Karnal"
    )

    // Weather State with Cached Fallback
    var weatherData by remember { mutableStateOf(loadCachedWeather(context) ?: getDefaultWeather(isHindi)) }

    fun refreshWeather(presetKey: String = selectedPreset) {
        isLoading = true
        selectedPreset = presetKey
        coroutineScope.launch {
            try {
                if (presetKey == "gps") {
                    val loc = locationProvider.getLastKnownLocation()
                    val lat = loc?.latitude ?: 28.6139
                    val lon = loc?.longitude ?: 77.2090
                    val resolvedName = if (loc != null) locationProvider.getResolvedLocationName(lat, lon) else (if (isHindi) "दिल्ली / उत्तर भारत" else "Delhi / North India")
                    locationStatusText = resolvedName

                    val fetched = apiClient.fetchWeather(lat, lon, resolvedName) ?: apiClient.fetchOpenMeteoWeather(lat, lon, resolvedName)
                    if (fetched != null) {
                        weatherData = fetched
                        saveWeatherToPrefs(context, fetched)
                    }
                } else {
                    val (lat, lon, name) = getPresetCoordinates(presetKey, isHindi)
                    locationStatusText = name
                    val fetched = apiClient.fetchWeather(lat, lon, name) ?: apiClient.fetchOpenMeteoWeather(lat, lon, name)
                    if (fetched != null) {
                        weatherData = fetched
                        saveWeatherToPrefs(context, fetched)
                    }
                }
            } catch (e: Exception) {
                // Offline fallback
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshWeather("gps")
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Location Selector Chips
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHindi) "कृषि मौसम स्थान (Select Location)" else "Select Farm Location",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = GreenPrimary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        IconButton(
                            onClick = { refreshWeather(selectedPreset) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = GreenPrimary, modifier = Modifier.size(18.dp))
                        }
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

        // 1. HERO WEATHER CARD (Dynamic Gradient with Weather Metrics)
        item {
            val isRaining = weatherData.rainfallProb >= 50
            val gradientColors = if (isRaining) {
                listOf(Color(0xFF1E3C72), Color(0xFF2A5298))
            } else {
                listOf(Color(0xFF0277BD), Color(0xFF0091EA))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(gradientColors))
                        .padding(20.dp)
                ) {
                    Column {
                        // Location and Update State
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFFB3E5FC),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = weatherData.location,
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (weatherData.isOffline) (if (isHindi) "ऑफ़लाइन" else "Offline") else (if (isHindi) "लाइव" else "Live"),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Big Temperature & Weather Icon
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${weatherData.temperature.toInt()}°C",
                                    color = Color.White,
                                    fontSize = 46.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-1).sp
                                )
                                Text(
                                    text = if (isHindi) weatherData.conditionHi else weatherData.condition,
                                    color = Color(0xFFE1F5FE),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                val weatherIcon = when {
                                    weatherData.rainfallProb >= 50 -> Icons.Default.Thunderstorm
                                    weatherData.humidity >= 75 -> Icons.Default.Cloud
                                    else -> Icons.Default.WbSunny
                                }
                                Icon(
                                    imageVector = weatherIcon,
                                    contentDescription = null,
                                    tint = if (weatherData.rainfallProb >= 50) Color(0xFF81D4FA) else Color(0xFFFFD54F),
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.25f), thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(14.dp))

                        // High-Contrast Farm Metrics Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            WeatherStatItem(
                                icon = Icons.Default.WaterDrop,
                                label = if (isHindi) "नमी (Humidity)" else "Humidity",
                                value = "${weatherData.humidity}%"
                            )
                            WeatherStatItem(
                                icon = Icons.Default.Air,
                                label = if (isHindi) "हवा (Wind)" else "Wind",
                                value = "${weatherData.windSpeed} km/h"
                            )
                            WeatherStatItem(
                                icon = Icons.Default.Umbrella,
                                label = if (isHindi) "बारिश (Rain)" else "Rain Prob",
                                value = "${weatherData.rainfallProb}%"
                            )
                        }
                    }
                }
            }
        }

        // 2. AGRICULTURAL ACTION CARDS (Spray Window & Irrigation Advisory)
        item {
            val isHighWind = weatherData.windSpeed > 18f
            val isHighRain = weatherData.rainfallProb >= 40
            val isHighHeat = weatherData.temperature >= 36f

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Spraying Window Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isHighWind || isHighRain) Color(0xFFFFF3E0) else Color(0xFFE8F5E9)),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isHighWind || isHighRain) Color(0xFFFFCC80) else Color(0xFFA5D6A7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = if (isHighWind || isHighRain) "⚠️" else "✅", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isHindi) "कीटनाशक व खाद स्प्रे विंडो" else "Agrochemical Spray Window",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = when {
                                    isHighRain -> if (isHindi) "बारिश की संभावना: स्प्रे धुलने का खतरा, आज छिड़काव न करें।" else "Rain likely: High washout danger, avoid spraying."
                                    isHighWind -> if (isHindi) "हवा की गति तेज (${weatherData.windSpeed} km/h): दवा बहाव का खतरा, स्प्रे रोकें।" else "Windy conditions (${weatherData.windSpeed} km/h): Spray drift risk."
                                    else -> if (isHindi) "हवा शांत व मौसम साफ: आज दोपहर/शाम को सुरक्षित स्प्रे कर सकते हैं।" else "Calm wind & clear sky: Favorable window for spraying."
                                },
                                fontSize = 11.5.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Irrigation & Soil Moisture Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE1F5FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "💧", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isHindi) "सिंचाई एवं मृदा नमी सलाह" else "Irrigation & Soil Moisture Index",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = when {
                                    isHighRain -> if (isHindi) "खेत में पर्याप्त नमी / बारिश संभावित: ट्यूबवेल सिंचाई रोकें व जल निकासी सुनिश्चित रखें।" else "Rain expected: Hold tubewell irrigation and ensure drainage."
                                    isHighHeat -> if (isHindi) "तेज गर्मी: नमी संरक्षण हेतु सुबह या शाम को ही हल्की सिंचाई करें।" else "High heat: Irrigate lightly during early morning or evening."
                                    else -> if (isHindi) "नियमित चक्र अनुसार फसलों में आवश्यकतानुसार हल्की सिंचाई करें।" else "Proceed with scheduled light irrigation according to crop stage."
                                },
                                fontSize = 11.5.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // 3. DETAILED ICAR AGRICULTURAL OUTLOOK
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Agriculture,
                            contentDescription = null,
                            tint = GreenPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isHindi) "कृषि विशेषज्ञ मौसम सलाह (ICAR Advisory)" else "Agronomic Weather Advisory (ICAR)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = GreenPrimary,
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

                    // Action tags
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (weatherData.rainfallProb >= 40) {
                            AgriActionBadge(text = if (isHindi) "🌧️ सिंचाई स्थगित" else "🌧️ Hold Irrigation", bg = Color(0xFFFFEBEE), txt = AlertRed)
                        } else {
                            AgriActionBadge(text = if (isHindi) "💧 नियमित सिंचाई" else "💧 Normal Irrigation", bg = Color(0xFFE8F5E9), txt = GreenDark)
                        }

                        if (weatherData.humidity >= 70) {
                            AgriActionBadge(text = if (isHindi) "🦠 फफूंद रोग सतर्कता" else "🦠 Fungal Disease Risk", bg = Color(0xFFFFF8E1), txt = AmberSecondary)
                        }

                        if (weatherData.temperature >= 35f) {
                            AgriActionBadge(text = if (isHindi) "☀️ तेज धूप तनाव" else "☀️ Heat Stress", bg = Color(0xFFFFF3E0), txt = Color(0xFFD84315))
                        }
                    }
                }
            }
        }

        // 4. 5-DAY DETAILED AGRICULTURAL FORECAST
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isHindi) "5-दिवसीय विस्तृत कृषि पूर्वानुमान" else "5-Day Agricultural Forecast",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 16.sp
                    )
                )

                Text(
                    text = if (isHindi) "दैनिक तापमान व वर्षा" else "Temp & Rain Outlook",
                    fontSize = 11.5.sp,
                    color = TextSecondary
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
                    Column(modifier = Modifier.weight(1.3f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = day.date,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFE8F5E9)
                            ) {
                                Text(
                                    text = if (isHindi) day.conditionHi else day.conditionHi,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isHindi) day.advisoryHi else day.advisoryHi,
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.5.sp),
                            maxLines = 2
                        )
                    }

                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(0.7f)) {
                        Text(
                            text = "${day.maxTemp.toInt()}° / ${day.minTemp.toInt()}°C",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 15.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (day.rainProb >= 40) Color(0xFFFFEBEE) else Color(0xFFE1F5FE)
                        ) {
                            Text(
                                text = "🌧️ ${day.rainProb}%",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (day.rainProb >= 40) AlertRed else Color(0xFF0288D1),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
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
fun AgriActionBadge(text: String, bg: Color, txt: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg
    ) {
        Text(
            text = text,
            color = txt,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun getPresetCoordinates(key: String, isHindi: Boolean): Triple<Double, Double, String> {
    return when (key) {
        "lucknow" -> Triple(26.8467, 80.9462, if (isHindi) "लखनऊ, उत्तर प्रदेश" else "Lucknow, Uttar Pradesh")
        "patna" -> Triple(25.5941, 85.1376, if (isHindi) "पटना, बिहार" else "Patna, Bihar")
        "bhopal" -> Triple(23.2599, 77.4126, if (isHindi) "भोपाल, मध्य प्रदेश" else "Bhopal, Madhya Pradesh")
        "jaipur" -> Triple(26.9124, 75.7873, if (isHindi) "जयपुर, राजस्थान" else "Jaipur, Rajasthan")
        "ludhiana" -> Triple(30.9010, 75.8573, if (isHindi) "लुधियाना, पंजाब" else "Ludhiana, Punjab")
        "karnal" -> Triple(29.6857, 76.9905, if (isHindi) "करनाल, हरियाणा" else "Karnal, Haryana")
        else -> Triple(28.6139, 77.2090, if (isHindi) "दिल्ली / उत्तर भारत" else "Delhi / North India")
    }
}

private fun getDefaultWeather(isHindi: Boolean): WeatherInfo {
    return WeatherInfo(
        location = if (isHindi) "दिल्ली / उत्तर भारत" else "Delhi / North India",
        temperature = 31.0f,
        humidity = 64,
        windSpeed = 11.5f,
        rainfallProb = 15,
        condition = "Clear",
        conditionHi = "साफ धूप",
        advisoryEn = "Favorable agricultural weather. Ideal time for field weeding and scheduled irrigation.",
        advisoryHi = "मौसम कृषि कार्यों के अनुकूल है। खेत की तैयारी, निराई-गुड़ाई और सिंचाई हेतु उपयुक्त समय है।",
        forecast = listOf(
            WeatherDayForecast(if (isHindi) "आज" else "Today", 32f, 23f, 15, "साफ धूप", "सामान्य कृषि कार्य जारी रखें।"),
            WeatherDayForecast(if (isHindi) "कल" else "Tomorrow", 31f, 22f, 20, "साफ धूप", "शाम को हल्की सिंचाई करें।"),
            WeatherDayForecast(if (isHindi) "परसों" else "Day 3", 29f, 21f, 45, "हल्के बादल", "दवा छिड़काव से पहले मौसम देखें।"),
            WeatherDayForecast(if (isHindi) "चौथा दिन" else "Day 4", 28f, 20f, 60, "वर्षा संभावना", "सिंचाई व कीटनाशक छिड़काव स्थगित रखें।"),
            WeatherDayForecast(if (isHindi) "पांचवां दिन" else "Day 5", 30f, 21f, 25, "सामान्य", "जलभराव न होने दें।")
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
fun WeatherStatItem(icon: ImageVector, label: String, value: String) {
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
