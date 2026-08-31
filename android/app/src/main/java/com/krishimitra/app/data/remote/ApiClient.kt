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

    suspend fun queryCloudAI(query: String, crop: String? = null, district: String? = null): JsonObject? = withContext(Dispatchers.IO) {
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
            Log.w(TAG, "Weather fetch network failed: ${e.message}")
        }
        return@withContext null
    }
}
