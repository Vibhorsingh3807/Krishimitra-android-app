package com.krishimitra.app.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.krishimitra.app.domain.model.WeatherDayForecast
import com.krishimitra.app.domain.model.WeatherInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class ApiClient(private val context: Context) {

    companion object {
        private const val TAG = "ApiClient"
        // 10.0.2.2 maps to host machine in Android Emulator; 127.0.0.1 for local device port forwarding
        var BASE_URL = "http://10.0.2.2:8000/api/v1"
    }

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(4, TimeUnit.SECONDS)
        .build()

    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun queryGroqGrokAI(query: String, crop: String? = null, district: String? = null): JsonObject? = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) return@withContext null

        try {
            val apiKey = "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"
            val systemPrompt = "You are KrishiMitra, an intelligent AI assistant powered by Grok. You specialize in agricultural advice for farmers (crops, weather, mandi prices, schemes, diseases), but you can also answer ANY general question on any topic (science, math, general knowledge, coding, history, daily life) accurately, clearly, and concisely in Hindi or English as requested."

            val messages = com.google.gson.JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("role", "system")
                    addProperty("content", systemPrompt)
                })
                add(JsonObject().apply {
                    addProperty("role", "user")
                    addProperty("content", if (crop != null || district != null) "Crop: ${crop ?: "General"}, District: ${district ?: "General"}. Question: $query" else query)
                })
            }

            val reqObj = JsonObject().apply {
                addProperty("model", "llama-3.3-70b-versatile")
                add("messages", messages)
                addProperty("temperature", 0.5)
                addProperty("max_tokens", 800)
            }

            val body = reqObj.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://api.groq.com/openai/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            client.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) {
                    val respStr = resp.body?.string() ?: return@withContext null
                    val respJson = gson.fromJson(respStr, JsonObject::class.java)
                    val choices = respJson.getAsJsonArray("choices")
                    if (choices != null && choices.size() > 0) {
                        val content = choices.get(0).asJsonObject.getAsJsonObject("message").get("content").asString
                        
                        var tokenUsageStr: String? = null
                        if (respJson.has("usage") && !respJson.get("usage").isJsonNull) {
                            val usage = respJson.getAsJsonObject("usage")
                            val promptTok = usage.get("prompt_tokens")?.asInt ?: 0
                            val compTok = usage.get("completion_tokens")?.asInt ?: 0
                            val totalTok = usage.get("total_tokens")?.asInt ?: (promptTok + compTok)
                            tokenUsageStr = "Tokens: $totalTok (Prompt: $promptTok | Comp: $compTok)"
                        }

                        val result = JsonObject().apply {
                            addProperty("answer", content)
                            addProperty("source", "KrishiMitra Cloud AI (Grok LLM)")
                            addProperty("is_verified_fact", true)
                            addProperty("detected_intent", "cloud_grok_ai")
                            if (tokenUsageStr != null) {
                                addProperty("token_usage", tokenUsageStr)
                            }
                        }
                        return@withContext result
                    }
                } else {
                    Log.w(TAG, "Groq Grok API response error code: ${resp.code}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Groq Grok API query failed: ${e.message}")
        }
        return@withContext null
    }

    suspend fun queryGeminiVisionAI(query: String, imageBase64: String? = null, userApiKey: String? = null): JsonObject? = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) return@withContext null

        try {
            val key = if (!userApiKey.isNullOrBlank()) userApiKey else "AQ.Ab8RN6KvURTkpztv_gAxRpUNOFlg89iogX6v7AWSwHyn63OPng"
            val systemInstruction = "You are KrishiMitra Gemini 1.5 Flash Multimodal AI Assistant for Indian Farmers. Analyze crop photos (leaf diseases, soil, pests, fertilizers) and user questions, providing clear, practical farming advice in Hindi or English."

            val partsArray = com.google.gson.JsonArray()

            if (!imageBase64.isNullOrBlank()) {
                val inlineData = JsonObject().apply {
                    addProperty("mime_type", "image/jpeg")
                    addProperty("data", imageBase64)
                }
                partsArray.add(JsonObject().apply {
                    add("inline_data", inlineData)
                })
            }

            partsArray.add(JsonObject().apply {
                addProperty("text", "$systemInstruction\nFarmer Question: ${if (query.isBlank()) "Analyze this crop image and explain any disease or nutrient issue with treatment recommendations." else query}")
            })

            val contentsArray = com.google.gson.JsonArray().apply {
                add(JsonObject().apply {
                    add("parts", partsArray)
                })
            }

            val requestObj = JsonObject().apply {
                add("contents", contentsArray)
            }

            val body = requestObj.toString().toRequestBody("application/json".toMediaType())
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$key"

            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            client.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) {
                    val respStr = resp.body?.string() ?: return@withContext null
                    val respJson = gson.fromJson(respStr, JsonObject::class.java)
                    val candidates = respJson.getAsJsonArray("candidates")
                    if (candidates != null && candidates.size() > 0) {
                        val firstCand = candidates.get(0).asJsonObject
                        val contentObj = firstCand.getAsJsonObject("content")
                        val parts = contentObj?.getAsJsonArray("parts")
                        if (parts != null && parts.size() > 0) {
                            val ansText = parts.get(0).asJsonObject.get("text").asString
                            val usageObj = respJson.getAsJsonObject("usageMetadata")
                            var tokenStr: String? = null
                            if (usageObj != null) {
                                val pTok = usageObj.get("promptTokenCount")?.asInt ?: 0
                                val cTok = usageObj.get("candidatesTokenCount")?.asInt ?: 0
                                val tTok = usageObj.get("totalTokenCount")?.asInt ?: (pTok + cTok)
                                tokenStr = "Tokens: $tTok (Prompt: $pTok | Comp: $cTok)"
                            }

                            return@withContext JsonObject().apply {
                                addProperty("answer", ansText)
                                addProperty("source", "✨ Gemini 1.5 Flash Vision AI")
                                addProperty("is_verified_fact", true)
                                addProperty("detected_intent", "gemini_vision_ai")
                                if (tokenStr != null) {
                                    addProperty("token_usage", tokenStr)
                                }
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "Gemini Vision API response error code: ${resp.code}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gemini Vision API query failed: ${e.message}")
        }
        return@withContext null
    }

    suspend fun queryCloudAI(query: String, crop: String? = null, district: String? = null, imageBase64: String? = null): JsonObject? = withContext(Dispatchers.IO) {
        if (!imageBase64.isNullOrBlank()) {
            val geminiResp = queryGeminiVisionAI(query, imageBase64)
            if (geminiResp != null) return@withContext geminiResp
        }

        val grokResp = queryGroqGrokAI(query, crop, district)
        if (grokResp != null) return@withContext grokResp

        if (!isNetworkAvailable()) return@withContext null

        try {
            val json = JsonObject().apply {
                addProperty("query", query)
                addProperty("language", "auto")
                if (crop != null) addProperty("crop", crop)
                if (district != null) addProperty("district", district)
            }

            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL/ai/query")
                .post(body)
                .build()

            client.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) {
                    val respStr = resp.body?.string() ?: return@withContext null
                    return@withContext gson.fromJson(respStr, JsonObject::class.java)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cloud AI query network failed: ${e.message}")
        }
        return@withContext null
    }

    suspend fun fetchOpenMeteoWeather(lat: Double, lon: Double, locName: String? = null): WeatherInfo? = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) return@withContext null

        try {
            val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max&timezone=auto"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) {
                    val respStr = resp.body?.string() ?: return@withContext null
                    val json = gson.fromJson(respStr, JsonObject::class.java)

                    val current = json.getAsJsonObject("current")
                    val daily = json.getAsJsonObject("daily")

                    val temp = current?.get("temperature_2m")?.asFloat ?: 30.0f
                    val humidity = current?.get("relative_humidity_2m")?.asInt ?: 60
                    val windSpeed = current?.get("wind_speed_10m")?.asFloat ?: 10.0f
                    val code = current?.get("weather_code")?.asInt ?: 0

                    val (condEn, condHi) = parseWeatherCode(code)

                    val times = daily?.getAsJsonArray("time")
                    val maxTemps = daily?.getAsJsonArray("temperature_2m_max")
                    val minTemps = daily?.getAsJsonArray("temperature_2m_min")
                    val rainProbs = daily?.getAsJsonArray("precipitation_probability_max")
                    val dailyCodes = daily?.getAsJsonArray("weather_code")

                    val forecastList = mutableListOf<WeatherDayForecast>()
                    val dayLabelsHi = listOf("आज", "कल", "परसों", "चौथे दिन", "पांचवें दिन")

                    if (times != null) {
                        for (i in 0 until minOf(5, times.size())) {
                            val maxT = maxTemps?.get(i)?.asFloat ?: (temp + 2)
                            val minT = minTemps?.get(i)?.asFloat ?: (temp - 5)
                            val rProb = rainProbs?.get(i)?.asInt ?: 10
                            val dCode = dailyCodes?.get(i)?.asInt ?: 0
                            val (dCondEn, dCondHi) = parseWeatherCode(dCode)
                            val label = if (i < dayLabelsHi.size) dayLabelsHi[i] else times.get(i).asString

                            val dayAdvHi = when {
                                rProb >= 60 -> "वर्षा की प्रबल संभावना। सिंचाई व स्प्रे रोकें।"
                                rProb >= 35 -> "बादल छाए रहेंगे। वर्षा पर नजर रखकर सिंचाई करें।"
                                maxT >= 36 -> "भीषण गर्मी। फसलों में पर्याप्त नमी बनाए रखें।"
                                else -> "सामान्य कृषि कार्य जारी रखें।"
                            }

                            forecastList.add(
                                WeatherDayForecast(
                                    date = label,
                                    maxTemp = maxT,
                                    minTemp = minT,
                                    rainProb = rProb,
                                    conditionHi = dCondHi,
                                    advisoryHi = dayAdvHi
                                )
                            )
                        }
                    }

                    val todayRainProb = if (forecastList.isNotEmpty()) forecastList[0].rainProb else 15

                    val agriAdvisoryEn = when {
                        todayRainProb >= 60 -> "Rain expected today. Hold irrigation and ensure proper field drainage."
                        temp >= 35 -> "High heat stress risk. Ensure timely morning/evening irrigation."
                        humidity >= 70 -> "High humidity favor fungal diseases. Monitor leaves closely."
                        else -> "Weather favorable for routine field operations, weeding and scheduled irrigation."
                    }

                    val agriAdvisoryHi = when {
                        todayRainProb >= 60 -> "आज वर्षा की संभावना है। सिंचाई स्थगित रखें व जल निकासी सुनिश्चित करें।"
                        temp >= 35 -> "तेज गर्मी की स्थिति। फसल में पानी की कमी न होने दें, सुबह/शाम सिंचाई करें।"
                        humidity >= 70 -> "हवा में नमी अधिक है। फफूंद व कीट रोग की निगरानी करें।"
                        else -> "मौसम कृषि कार्यों के अनुकूल है। खेत की तैयारी व सिंचाई हेतु सही समय।"
                    }

                    val finalLocName = locName ?: "स्थानीय कृषि क्षेत्र"

                    return@withContext WeatherInfo(
                        location = finalLocName,
                        temperature = temp,
                        humidity = humidity,
                        windSpeed = windSpeed,
                        rainfallProb = todayRainProb,
                        condition = condEn,
                        conditionHi = condHi,
                        advisoryEn = agriAdvisoryEn,
                        advisoryHi = agriAdvisoryHi,
                        forecast = forecastList,
                        isOffline = false
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Open-Meteo direct fetch failed: ${e.message}")
        }
        return@withContext null
    }

    private fun parseWeatherCode(code: Int): Pair<String, String> {
        return when (code) {
            0 -> "Clear Sky" to "साफ धूप"
            1, 2, 3 -> "Partly Cloudy" to "हल्के बादल"
            45, 48 -> "Foggy" to "कोहरा"
            51, 53, 55 -> "Drizzle" to "हल्की बूंदाबांदी"
            61, 63, 65 -> "Rain" to "वर्षा"
            80, 81, 82 -> "Showers" to "बौछारें"
            95, 96, 99 -> "Thunderstorm" to "आंधी तूफान"
            else -> "Partly Sunny" to "सामान्य मौसम"
        }
    }

    suspend fun fetchWeather(lat: Double, lon: Double, district: String? = null): WeatherInfo? = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) return@withContext null

        try {
            val urlBuilder = StringBuilder("$BASE_URL/weather?lat=$lat&lon=$lon")
            if (!district.isNullOrBlank()) {
                urlBuilder.append("&district=$district")
            }

            val request = Request.Builder().url(urlBuilder.toString()).get().build()
            client.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) {
                    val respStr = resp.body?.string() ?: return@withContext null
                    val json = gson.fromJson(respStr, JsonObject::class.java)

                    val forecastList = mutableListOf<WeatherDayForecast>()
                    val forecastArr = json.getAsJsonArray("forecast")
                    if (forecastArr != null) {
                        for (i in 0 until forecastArr.size()) {
                            val fObj = forecastArr.get(i).asJsonObject
                            forecastList.add(
                                WeatherDayForecast(
                                    date = fObj.get("date").asString,
                                    maxTemp = fObj.get("max_temp").asFloat,
                                    minTemp = fObj.get("min_temp").asFloat,
                                    rainProb = fObj.get("precipitation_prob").asInt,
                                    conditionHi = fObj.get("condition_hi").asString,
                                    advisoryHi = fObj.get("advisory_hi").asString
                                )
                            )
                        }
                    }

                    return@withContext WeatherInfo(
                        location = json.get("location").asString,
                        temperature = json.get("current_temperature").asFloat,
                        humidity = json.get("humidity").asInt,
                        windSpeed = json.get("wind_speed").asFloat,
                        rainfallProb = if (forecastList.isNotEmpty()) forecastList[0].rainProb else 10,
                        condition = json.get("weather_condition").asString,
                        conditionHi = json.get("weather_condition_hi").asString,
                        advisoryEn = json.get("agri_advisory").asString,
                        advisoryHi = json.get("agri_advisory_hi").asString,
                        forecast = forecastList,
                        isOffline = false
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Backend Weather fetch network failed, trying direct Open-Meteo: ${e.message}")
        }
        return@withContext fetchOpenMeteoWeather(lat, lon, district)
    }
}
