package com.krishimitra.app.domain.model

/**
 * Field Digital Twin Domain Models
 * Represents the geospatial boundary, zoning breakdown, multi-temporal observations,
 * farmer management actions, and rule-based agronomic recommendations.
 */

data class GeoPoint(
    val latitude: Double,
    val longitude: Double
)

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH
}

enum class CropHealthStatus {
    EXCELLENT,
    GOOD,
    MODERATE,
    POOR
}

data class FarmerField(
    val id: String,
    val name: String,
    val cropId: String,
    val cropName: String,
    val cropNameHi: String,
    val variety: String? = null,
    val areaAcres: Double,
    val sowingDate: String,
    val boundary: List<GeoPoint>,
    val centerLat: Double,
    val centerLon: Double,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class FieldZone(
    val id: String,                    // e.g. "Z-01", "Z-07"
    val fieldId: String,
    val zoneNumber: Int,
    val zoneLabel: String,             // e.g. "Zone 1", "Zone 7"
    val zoneLabelHi: String,           // e.g. "ज़ोन 1", "ज़ोन 7"
    val boundary: List<GeoPoint>,
    val centerLat: Double,
    val centerLon: Double,
    val areaAcres: Double,
    val crop: String,
    val cropHi: String
)

data class ZoneObservation(
    val id: String,
    val zoneId: String,
    val timestamp: Long,
    val dateString: String,            // e.g. "01 Sep", "10 Sep", "आज"
    val ndvi: Float,                   // 0.0 to 1.0 Normalized Difference Vegetation Index
    val moisture: Int,                 // 0% to 100% volumetric/relative soil moisture
    val temperature: Float,            // °C
    val rainfallMm: Float,             // mm
    val humidity: Int,                 // %
    val cropHealth: CropHealthStatus,
    val riskLevel: RiskLevel,
    val riskType: String,              // "WATER_STRESS", "FUNGAL_RISK", "HEAT_STRESS", "NONE"
    val riskTypeHi: String,
    val source: String,                // "Demonstration Data", "Satellite", "Weather API", "Camera AI", "Farmer Input"
    val sourceHi: String
)

data class IrrigationEvent(
    val id: String,
    val zoneId: String,
    val timestamp: Long,
    val dateString: String,
    val amountMm: Float? = null,
    val durationMinutes: Int? = null,
    val notes: String? = null,
    val source: String = "Farmer Action"
)

data class ZoneRecommendation(
    val id: String,
    val zoneId: String,
    val timestamp: Long,
    val title: String,
    val titleHi: String,
    val recommendation: String,
    val recommendationHi: String,
    val priority: RiskLevel,
    val reason: String,
    val reasonHi: String,
    val actionRequired: String? = null, // "IRRIGATE", "INSPECT", "SPRAY", "DRAIN"
    val isResolved: Boolean = false
)

data class FieldTwinSummary(
    val field: FarmerField,
    val zones: List<FieldZone>,
    val latestObservations: Map<String, ZoneObservation>, // zoneId -> observation
    val recommendations: List<ZoneRecommendation>,
    val avgMoisture: Int,
    val avgNdvi: Float,
    val healthyZonesCount: Int,
    val attentionZonesCount: Int,
    val highRiskZonesCount: Int,
    val lastActionText: String?,
    val lastActionTextHi: String?
)
