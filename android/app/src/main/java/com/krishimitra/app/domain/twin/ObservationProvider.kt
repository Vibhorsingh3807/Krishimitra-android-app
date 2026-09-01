package com.krishimitra.app.domain.twin

import com.krishimitra.app.domain.model.*

/**
 * ObservationProvider
 *
 * Decouples observation ingestion from the Digital Twin core.
 * Real production implementations can plug in Sentinel-2/Copernicus satellite feeds,
 * IoT soil sensor arrays, or localized weather sensors.
 */
interface ObservationProvider {
    fun getDataSourceName(isHindi: Boolean): String
    fun getLatestObservations(fieldId: String, zones: List<FieldZone>): Map<String, ZoneObservation>
    fun getHistoricalObservations(zoneId: String): List<ZoneObservation>
}

/**
 * DemoObservationProvider
 *
 * Provides realistic, deterministic multi-temporal agronomic observations.
 * Explicitly designed for the Smart India Hackathon (SIH) demonstration flow:
 * - Zone 7 suffers from acute Water Stress (Moisture 18% ↓, NDVI 0.49 ↓, Temp 36°C)
 * - Zone 5 has moderate stress
 * - Zones 1, 2, 4, 6 are healthy
 * - Fully deterministic (identical results on every launch)
 */
class DemoObservationProvider : ObservationProvider {

    override fun getDataSourceName(isHindi: Boolean): String {
        return if (isHindi) "प्रदर्शन डेटा (Demonstration Data)" else "Demonstration Data"
    }

    override fun getLatestObservations(fieldId: String, zones: List<FieldZone>): Map<String, ZoneObservation> {
        val map = mutableMapOf<String, ZoneObservation>()
        val now = System.currentTimeMillis()

        for (z in zones) {
            val obs = when (z.id) {
                "Z-01" -> ZoneObservation(
                    id = "OBS-01",
                    zoneId = z.id,
                    timestamp = now,
                    dateString = "आज (Today)",
                    ndvi = 0.72f,
                    moisture = 31,
                    temperature = 31.5f,
                    rainfallMm = 14f,
                    humidity = 65,
                    cropHealth = CropHealthStatus.EXCELLENT,
                    riskLevel = RiskLevel.LOW,
                    riskType = "NONE",
                    riskTypeHi = "सामान्य (कोई जोखिम नहीं)",
                    source = "Demonstration Data",
                    sourceHi = "प्रदर्शन डेटा"
                )
                "Z-02" -> ZoneObservation(
                    id = "OBS-02",
                    zoneId = z.id,
                    timestamp = now,
                    dateString = "आज (Today)",
                    ndvi = 0.68f,
                    moisture = 28,
                    temperature = 32.0f,
                    rainfallMm = 12f,
                    humidity = 64,
                    cropHealth = CropHealthStatus.GOOD,
                    riskLevel = RiskLevel.LOW,
                    riskType = "NONE",
                    riskTypeHi = "सामान्य (कोई जोखिम नहीं)",
                    source = "Demonstration Data",
                    sourceHi = "प्रदर्शन डेटा"
                )
                "Z-03" -> ZoneObservation(
                    id = "OBS-03",
                    zoneId = z.id,
                    timestamp = now,
                    dateString = "आज (Today)",
                    ndvi = 0.59f,
                    moisture = 23,
                    temperature = 33.0f,
                    rainfallMm = 8f,
                    humidity = 58,
                    cropHealth = CropHealthStatus.MODERATE,
                    riskLevel = RiskLevel.LOW,
                    riskType = "NONE",
                    riskTypeHi = "सामान्य (हल्की निगरानी)",
                    source = "Demonstration Data",
                    sourceHi = "प्रदर्शन डेटा"
                )
                "Z-04" -> ZoneObservation(
                    id = "OBS-04",
                    zoneId = z.id,
                    timestamp = now,
                    dateString = "आज (Today)",
                    ndvi = 0.71f,
                    moisture = 30,
                    temperature = 31.8f,
                    rainfallMm = 15f,
                    humidity = 66,
                    cropHealth = CropHealthStatus.EXCELLENT,
                    riskLevel = RiskLevel.LOW,
                    riskType = "NONE",
                    riskTypeHi = "सामान्य (कोई जोखिम नहीं)",
                    source = "Demonstration Data",
                    sourceHi = "प्रदर्शन डेटा"
                )
                "Z-05" -> ZoneObservation(
                    id = "OBS-05",
                    zoneId = z.id,
                    timestamp = now,
                    dateString = "आज (Today)",
                    ndvi = 0.54f,
                    moisture = 21,
                    temperature = 34.2f,
                    rainfallMm = 5f,
                    humidity = 52,
                    cropHealth = CropHealthStatus.MODERATE,
                    riskLevel = RiskLevel.MEDIUM,
                    riskType = "WATER_STRESS",
                    riskTypeHi = "मध्यम पानी का तनाव",
                    source = "Demonstration Data",
                    sourceHi = "प्रदर्शन डेटा"
                )
                "Z-06" -> ZoneObservation(
                    id = "OBS-06",
                    zoneId = z.id,
                    timestamp = now,
                    dateString = "आज (Today)",
                    ndvi = 0.67f,
                    moisture = 27,
                    temperature = 32.5f,
                    rainfallMm = 10f,
                    humidity = 62,
                    cropHealth = CropHealthStatus.GOOD,
                    riskLevel = RiskLevel.LOW,
                    riskType = "NONE",
                    riskTypeHi = "सामान्य (कोई जोखिम नहीं)",
                    source = "Demonstration Data",
                    sourceHi = "प्रदर्शन डेटा"
                )
                "Z-07" -> ZoneObservation(
                    id = "OBS-07",
                    zoneId = z.id,
                    timestamp = now,
                    dateString = "आज (Today)",
                    ndvi = 0.49f,
                    moisture = 18,
                    temperature = 36.0f,
                    rainfallMm = 2f,
                    humidity = 42,
                    cropHealth = CropHealthStatus.POOR,
                    riskLevel = RiskLevel.HIGH,
                    riskType = "WATER_STRESS",
                    riskTypeHi = "गंभीर पानी की कमी (Water Stress)",
                    source = "Demonstration Data",
                    sourceHi = "प्रदर्शन डेटा"
                )
                "Z-08" -> ZoneObservation(
                    id = "OBS-08",
                    zoneId = z.id,
                    timestamp = now,
                    dateString = "आज (Today)",
                    ndvi = 0.52f,
                    moisture = 20,
                    temperature = 34.5f,
                    rainfallMm = 4f,
                    humidity = 50,
                    cropHealth = CropHealthStatus.MODERATE,
                    riskLevel = RiskLevel.MEDIUM,
                    riskType = "WATER_STRESS",
                    riskTypeHi = "मध्यम तनाव",
                    source = "Demonstration Data",
                    sourceHi = "प्रदर्शन डेटा"
                )
                else -> ZoneObservation(
                    id = "OBS-09",
                    zoneId = z.id,
                    timestamp = now,
                    dateString = "आज (Today)",
                    ndvi = 0.58f,
                    moisture = 24,
                    temperature = 33.2f,
                    rainfallMm = 8f,
                    humidity = 56,
                    cropHealth = CropHealthStatus.MODERATE,
                    riskLevel = RiskLevel.LOW,
                    riskType = "NONE",
                    riskTypeHi = "सामान्य",
                    source = "Demonstration Data",
                    sourceHi = "प्रदर्शन डेटा"
                )
            }
            map[z.id] = obs
        }

        return map
    }

    override fun getHistoricalObservations(zoneId: String): List<ZoneObservation> {
        val now = System.currentTimeMillis()
        val dayMs = 86400000L

        return when (zoneId) {
            "Z-07" -> listOf(
                ZoneObservation("H-01", zoneId, now - 15 * dayMs, "01 Aug", 0.68f, 32, 29.5f, 25f, 72, CropHealthStatus.GOOD, RiskLevel.LOW, "NONE", "सामान्य", "Demonstration Data", "प्रदर्शन डेटा"),
                ZoneObservation("H-02", zoneId, now - 10 * dayMs, "10 Aug", 0.62f, 28, 31.0f, 18f, 65, CropHealthStatus.GOOD, RiskLevel.LOW, "NONE", "सामान्य", "Demonstration Data", "प्रदर्शन डेटा"),
                ZoneObservation("H-03", zoneId, now - 5 * dayMs, "20 Aug", 0.55f, 22, 33.8f, 5f, 54, CropHealthStatus.MODERATE, RiskLevel.MEDIUM, "WATER_STRESS", "नमी में गिरावट", "Demonstration Data", "प्रदर्शन डेटा"),
                ZoneObservation("H-04", zoneId, now, "आज (Today)", 0.49f, 18, 36.0f, 2f, 42, CropHealthStatus.POOR, RiskLevel.HIGH, "WATER_STRESS", "गंभीर पानी की कमी", "Demonstration Data", "प्रदर्शन डेटा")
            )
            "Z-05" -> listOf(
                ZoneObservation("H-05", zoneId, now - 15 * dayMs, "01 Aug", 0.65f, 30, 30.0f, 22f, 70, CropHealthStatus.GOOD, RiskLevel.LOW, "NONE", "सामान्य", "Demonstration Data", "प्रदर्शन डेटा"),
                ZoneObservation("H-06", zoneId, now - 10 * dayMs, "10 Aug", 0.60f, 26, 32.0f, 12f, 60, CropHealthStatus.MODERATE, RiskLevel.LOW, "NONE", "सामान्य", "Demonstration Data", "प्रदर्शन डेटा"),
                ZoneObservation("H-07", zoneId, now, "आज (Today)", 0.54f, 21, 34.2f, 5f, 52, CropHealthStatus.MODERATE, RiskLevel.MEDIUM, "WATER_STRESS", "मध्यम तनाव", "Demonstration Data", "प्रदर्शन डेटा")
            )
            else -> listOf(
                ZoneObservation("H-08", zoneId, now - 15 * dayMs, "01 Aug", 0.70f, 34, 30.0f, 20f, 70, CropHealthStatus.EXCELLENT, RiskLevel.LOW, "NONE", "सामान्य", "Demonstration Data", "प्रदर्शन डेटा"),
                ZoneObservation("H-09", zoneId, now - 10 * dayMs, "10 Aug", 0.71f, 32, 31.0f, 15f, 68, CropHealthStatus.EXCELLENT, RiskLevel.LOW, "NONE", "सामान्य", "Demonstration Data", "प्रदर्शन डेटा"),
                ZoneObservation("H-10", zoneId, now, "आज (Today)", 0.72f, 31, 31.5f, 14f, 65, CropHealthStatus.EXCELLENT, RiskLevel.LOW, "NONE", "सामान्य", "Demonstration Data", "प्रदर्शन डेटा")
            )
        }
    }
}

/**
 * SatelliteObservationProvider (Production Ready Stub)
 *
 * Structured to integrate Sentinel-2 L2A BOA Reflectance API via Copernicus Data Space Ecosystem.
 * Formula: NDVI = (B8 - B4) / (B8 + B4)
 */
class SatelliteObservationProvider : ObservationProvider {
    override fun getDataSourceName(isHindi: Boolean): String = if (isHindi) "सेंटिनल-2 उपग्रह (Sentinel-2 L2A)" else "Sentinel-2 Satellite (Copernicus)"
    override fun getLatestObservations(fieldId: String, zones: List<FieldZone>): Map<String, ZoneObservation> = emptyMap()
    override fun getHistoricalObservations(zoneId: String): List<ZoneObservation> = emptyList()
}

/**
 * IoTObservationProvider (Production Ready Stub)
 *
 * Structured for LoRaWAN / 4G cellular IoT soil moisture probes & telemetry units.
 */
class IoTObservationProvider : ObservationProvider {
    override fun getDataSourceName(isHindi: Boolean): String = if (isHindi) "आईओटी मृदा सेंसर (In-Situ IoT)" else "In-Situ IoT Soil Probes"
    override fun getLatestObservations(fieldId: String, zones: List<FieldZone>): Map<String, ZoneObservation> = emptyMap()
    override fun getHistoricalObservations(zoneId: String): List<ZoneObservation> = emptyList()
}
