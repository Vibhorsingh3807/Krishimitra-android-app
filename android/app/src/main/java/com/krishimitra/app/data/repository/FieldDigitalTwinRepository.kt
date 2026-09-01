package com.krishimitra.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.krishimitra.app.domain.model.*
import com.krishimitra.app.domain.twin.DemoObservationProvider
import com.krishimitra.app.domain.twin.FieldTwinRuleEngine
import com.krishimitra.app.domain.twin.FieldZoningEngine
import com.krishimitra.app.domain.twin.ObservationProvider
import kotlin.math.max

/**
 * FieldDigitalTwinRepository
 *
 * Coordinates local persistence, multi-temporal zone observation tracking,
 * farmer actions (irrigation/inspection), and the closed-loop twin update.
 */
class FieldDigitalTwinRepository(
    private val context: Context,
    private val observationProvider: ObservationProvider = DemoObservationProvider()
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("field_digital_twin_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    init {
        // Initialize with default SIH demo field if none exists
        if (getFields().isEmpty()) {
            initDefaultDemoField()
        }
    }

    private fun initDefaultDemoField() {
        val demoBoundary = listOf(
            GeoPoint(28.7040, 77.1000),
            GeoPoint(28.7055, 77.1045),
            GeoPoint(28.7015, 77.1060),
            GeoPoint(28.7000, 77.1015)
        )

        val defaultField = FarmerField(
            id = "FIELD-001",
            name = "My Wheat Field",
            cropId = "wheat",
            cropName = "Wheat",
            cropNameHi = "गेहूं (Wheat)",
            variety = "HD-2967 (पूसा)",
            areaAcres = 5.2,
            sowingDate = "15 Nov 2025",
            boundary = demoBoundary,
            centerLat = 28.7027,
            centerLon = 28.7030,
            createdAt = System.currentTimeMillis() - 45 * 86400000L
        )

        saveField(defaultField)
    }

    fun getFields(): List<FarmerField> {
        val json = prefs.getString("fields_list", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<FarmerField>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getField(fieldId: String): FarmerField? {
        return getFields().firstOrNull { it.id == fieldId } ?: getFields().firstOrNull()
    }

    fun saveField(field: FarmerField) {
        val current = getFields().toMutableList()
        val index = current.indexOfFirst { it.id == field.id }
        if (index >= 0) {
            current[index] = field
        } else {
            current.add(field)
        }
        prefs.edit().putString("fields_list", gson.toJson(current)).apply()

        // Generate and save zones
        val zones = FieldZoningEngine.generateZones(field)
        saveZones(field.id, zones)

        // Initialize latest observations
        val obsMap = observationProvider.getLatestObservations(field.id, zones)
        saveLatestObservations(field.id, obsMap)
    }

    fun deleteField(fieldId: String) {
        val current = getFields().filterNot { it.id == fieldId }
        prefs.edit().putString("fields_list", gson.toJson(current)).apply()
        prefs.edit().remove("zones_$fieldId").remove("obs_$fieldId").apply()
    }

    fun getZones(fieldId: String): List<FieldZone> {
        val json = prefs.getString("zones_$fieldId", null)
        if (!json.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<FieldZone>>() {}.type
                val list: List<FieldZone>? = gson.fromJson(json, type)
                if (!list.isNullOrEmpty()) return list
            } catch (e: Exception) {
            }
        }

        val field = getField(fieldId) ?: return emptyList()
        val generated = FieldZoningEngine.generateZones(field)
        saveZones(fieldId, generated)
        return generated
    }

    private fun saveZones(fieldId: String, zones: List<FieldZone>) {
        prefs.edit().putString("zones_$fieldId", gson.toJson(zones)).apply()
    }

    fun getLatestObservations(fieldId: String): Map<String, ZoneObservation> {
        val json = prefs.getString("obs_$fieldId", null)
        if (!json.isNullOrBlank()) {
            try {
                val type = object : TypeToken<Map<String, ZoneObservation>>() {}.type
                val map: Map<String, ZoneObservation>? = gson.fromJson(json, type)
                if (!map.isNullOrEmpty()) return map
            } catch (e: Exception) {
            }
        }

        val zones = getZones(fieldId)
        val fetched = observationProvider.getLatestObservations(fieldId, zones)
        saveLatestObservations(fieldId, fetched)
        return fetched
    }

    fun saveLatestObservations(fieldId: String, map: Map<String, ZoneObservation>) {
        prefs.edit().putString("obs_$fieldId", gson.toJson(map)).apply()
    }

    fun getHistoricalObservations(zoneId: String): List<ZoneObservation> {
        val key = "history_$zoneId"
        val json = prefs.getString(key, null)
        if (!json.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<ZoneObservation>>() {}.type
                val list: List<ZoneObservation>? = gson.fromJson(json, type)
                if (!list.isNullOrEmpty()) return list
            } catch (e: Exception) {
            }
        }

        val fetched = observationProvider.getHistoricalObservations(zoneId)
        prefs.edit().putString(key, gson.toJson(fetched)).apply()
        return fetched
    }

    fun getIrrigationEvents(zoneId: String): List<IrrigationEvent> {
        val json = prefs.getString("irrigation_$zoneId", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<IrrigationEvent>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Records farmer irrigation action and triggers the closed-loop Digital Twin update.
     * Updates zone state from stressed (18%) to healthy/improving (32%).
     */
    fun recordIrrigation(
        fieldId: String,
        zoneId: String,
        amountMm: Float? = 35f,
        notes: String? = null
    ): ZoneObservation {
        val now = System.currentTimeMillis()
        val event = IrrigationEvent(
            id = "IRR-${System.currentTimeMillis()}",
            zoneId = zoneId,
            timestamp = now,
            dateString = "आज (Today)",
            amountMm = amountMm,
            notes = notes ?: "कैनोपी व मृदा को 35mm सिंचाई दी गई"
        )

        // Save event in timeline
        val events = getIrrigationEvents(zoneId).toMutableList()
        events.add(0, event)
        prefs.edit().putString("irrigation_$zoneId", gson.toJson(events)).apply()

        // Closed loop update: Zone Digital Twin state dynamically improves
        val currentObservations = getLatestObservations(fieldId).toMutableMap()
        val oldObs = currentObservations[zoneId]

        val updatedObs = ZoneObservation(
            id = "OBS-UPDATED-${System.currentTimeMillis()}",
            zoneId = zoneId,
            timestamp = now,
            dateString = "आज (सिंचाई उपरांत)",
            ndvi = max(0.58f, (oldObs?.ndvi ?: 0.49f) + 0.08f),
            moisture = 32, // Soil moisture restored to optimal
            temperature = 31.0f,
            rainfallMm = oldObs?.rainfallMm ?: 2f,
            humidity = 65,
            cropHealth = CropHealthStatus.GOOD,
            riskLevel = RiskLevel.LOW,
            riskType = "NONE",
            riskTypeHi = "सामान्य (सिंचाई उपरांत सुधार)",
            source = "Farmer Action + Twin Simulation",
            sourceHi = "किसान कार्य + ट्विन सिमुलेशन"
        )

        currentObservations[zoneId] = updatedObs
        saveLatestObservations(fieldId, currentObservations)

        // Update history with new post-irrigation point
        val history = getHistoricalObservations(zoneId).toMutableList()
        history.add(updatedObs)
        prefs.edit().putString("history_$zoneId", gson.toJson(history)).apply()

        // Set persistent banner text for SIH demonstration
        prefs.edit()
            .putString("last_action_text", "Zone $zoneId: Irrigated 35mm today — soil moisture restored to 32%")
            .putString("last_action_text_hi", "$zoneId: आज 35mm सिंचाई पूर्ण — नमी सुधरकर 32% हुई")
            .apply()

        return updatedObs
    }

    /**
     * Associates a Leaf Camera Disease Scan to a specific Field Zone.
     */
    fun recordCameraObservation(
        fieldId: String,
        zoneId: String,
        diseaseName: String,
        confidence: Float
    ) {
        val currentObservations = getLatestObservations(fieldId).toMutableMap()
        val oldObs = currentObservations[zoneId]

        val isHealthy = diseaseName.contains("healthy", ignoreCase = true)
        val health = if (isHealthy) CropHealthStatus.GOOD else CropHealthStatus.POOR
        val risk = if (isHealthy) RiskLevel.LOW else RiskLevel.HIGH

        val updatedObs = ZoneObservation(
            id = "CAM-${System.currentTimeMillis()}",
            zoneId = zoneId,
            timestamp = System.currentTimeMillis(),
            dateString = "कैमरा स्कैन (Today)",
            ndvi = oldObs?.ndvi ?: 0.55f,
            moisture = oldObs?.moisture ?: 24,
            temperature = oldObs?.temperature ?: 32f,
            rainfallMm = oldObs?.rainfallMm ?: 0f,
            humidity = oldObs?.humidity ?: 60,
            cropHealth = health,
            riskLevel = risk,
            riskType = if (isHealthy) "NONE" else "LEAF_DISEASE",
            riskTypeHi = if (isHealthy) "स्वस्थ पत्ती" else "पत्ती रोग: $diseaseName (${(confidence * 100).toInt()}%)",
            source = "Camera AI Scan",
            sourceHi = "कैमरा एआई स्कैन"
        )

        currentObservations[zoneId] = updatedObs
        saveLatestObservations(fieldId, currentObservations)
    }

    fun getFieldTwinSummary(fieldId: String): FieldTwinSummary {
        val field = getField(fieldId) ?: initDefaultDemoField().let { getFields().first() }
        val zones = getZones(field.id)
        val observations = getLatestObservations(field.id)

        var totalMoisture = 0
        var totalNdvi = 0f
        var healthyCount = 0
        var attentionCount = 0
        var highRiskCount = 0

        val recommendations = mutableListOf<ZoneRecommendation>()

        for (z in zones) {
            val obs = observations[z.id]
            if (obs != null) {
                totalMoisture += obs.moisture
                totalNdvi += obs.ndvi

                when (obs.riskLevel) {
                    RiskLevel.LOW -> healthyCount++
                    RiskLevel.MEDIUM -> attentionCount++
                    RiskLevel.HIGH -> {
                        attentionCount++
                        highRiskCount++
                    }
                }

                val rec = FieldTwinRuleEngine.evaluateZone(z, obs, getHistoricalObservations(z.id))
                if (rec != null) {
                    recommendations.add(rec)
                }
            }
        }

        val count = max(1, zones.size)
        val avgMoisture = totalMoisture / count
        val avgNdvi = totalNdvi / count

        val lastActionText = prefs.getString("last_action_text", null)
        val lastActionTextHi = prefs.getString("last_action_text_hi", null)

        return FieldTwinSummary(
            field = field,
            zones = zones,
            latestObservations = observations,
            recommendations = recommendations,
            avgMoisture = avgMoisture,
            avgNdvi = avgNdvi,
            healthyZonesCount = healthyCount,
            attentionZonesCount = attentionCount,
            highRiskZonesCount = highRiskCount,
            lastActionText = lastActionText,
            lastActionTextHi = lastActionTextHi
        )
    }
}
