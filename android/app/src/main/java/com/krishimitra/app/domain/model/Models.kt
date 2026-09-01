package com.krishimitra.app.domain.model

data class Crop(
    val id: String,
    val nameEn: String,
    val nameHi: String,
    val scientificName: String?,
    val category: String?,
    val categoryHi: String?,
    val soil: String?,
    val soilHi: String?,
    val soilPh: String?,
    val climate: String?,
    val climateHi: String?,
    val temperature: String?,
    val sowingSeason: String?,
    val sowingSeasonHi: String?,
    val irrigation: String?,
    val irrigationHi: String?,
    val fertilizer: String?,
    val fertilizerHi: String?,
    val harvesting: String?,
    val harvestingHi: String?,
    val pests: String?,
    val pestsHi: String?,
    val diseases: String?,
    val diseasesHi: String?,
    val cultivationTips: String?,
    val cultivationTipsHi: String?,
    val source: String?,
    val sourceUrl: String?
)

data class Disease(
    val id: String,
    val crop: String,
    val cropHi: String,
    val diseaseNameEn: String,
    val diseaseNameHi: String,
    val pathogen: String?,
    val symptomsEn: String?,
    val symptomsHi: String?,
    val causesEn: String?,
    val causesHi: String?,
    val treatmentOrganicEn: String?,
    val treatmentOrganicHi: String?,
    val treatmentChemicalEn: String?,
    val treatmentChemicalHi: String?,
    val preventionEn: String?,
    val preventionHi: String?,
    val confidenceThreshold: Float = 0.70f
)

data class Scheme(
    val id: String,
    val nameEn: String,
    val nameHi: String,
    val category: String?,
    val categoryHi: String?,
    val ministry: String?,
    val benefitsEn: String?,
    val benefitsHi: String?,
    val eligibilityEn: String?,
    val eligibilityHi: String?,
    val applicationProcessEn: String?,
    val applicationProcessHi: String?,
    val officialUrl: String?,
    val source: String?,
    val lastVerified: String?
)

data class Loan(
    val id: String,
    val bankName: String,
    val bankNameHi: String,
    val loanType: String,
    val loanTypeHi: String,
    val purposeEn: String?,
    val purposeHi: String?,
    val interestRate: String?,
    val interestRateHi: String?,
    val maxLimit: String?,
    val maxLimitHi: String?,
    val eligibilityEn: String?,
    val eligibilityHi: String?,
    val documentsRequired: String?,
    val documentsRequiredHi: String?,
    val officialUrl: String?,
    val source: String?,
    val lastVerified: String?
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String? = null,
    val isVerified: Boolean = true,
    val intent: String? = null,
    val isSpeaking: Boolean = false,
    val tokenUsage: String? = null
)

data class WeatherInfo(
    val location: String,
    val temperature: Float,
    val humidity: Int,
    val windSpeed: Float,
    val rainfallProb: Int,
    val condition: String,
    val conditionHi: String,
    val advisoryEn: String,
    val advisoryHi: String,
    val forecast: List<WeatherDayForecast> = emptyList(),
    val isOffline: Boolean = false
)

data class WeatherDayForecast(
    val date: String,
    val maxTemp: Float,
    val minTemp: Float,
    val rainProb: Int,
    val conditionHi: String,
    val advisoryHi: String
)

data class DiseaseDiagnosisResult(
    val crop: String,
    val diseaseNameEn: String,
    val diseaseNameHi: String,
    val confidence: Float,
    val isUncertain: Boolean,
    val symptoms: String,
    val organicRemedy: String,
    val chemicalRemedy: String,
    val prevention: String,
    val source: String
)

data class FarmerExperience(
    val id: String = java.util.UUID.randomUUID().toString(),
    val cropId: String,
    val cropName: String,
    val state: String,
    val district: String? = null,
    val observation: String,
    val observationHi: String? = null,
    val observationEn: String? = null,
    val language: String = "hi",
    val createdAt: String = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
)

data class RAGKnowledgeRecord(
    val id: String,
    val topic: String,
    val cropId: String?,
    val questionEn: String,
    val questionHi: String,
    val answerEn: String,
    val answerHi: String,
    val source: String,
    val sourceUrl: String? = null,
    val isVerified: Boolean = true,
    val category: String = "VERIFIED_KNOWLEDGE" // "VERIFIED_KNOWLEDGE" or "FARMER_EXPERIENCE"
)

data class MarketPrice(
    val id: Int,
    val cropId: String?,
    val commodity: String,
    val variety: String?,
    val state: String,
    val district: String,
    val market: String,
    val minPrice: Float,
    val maxPrice: Float,
    val modalPrice: Float,
    val priceDate: String,
    val unit: String = "₹/Quintal",
    val source: String
)

data class CropVariety(
    val id: String,
    val cropId: String,
    val varietyName: String,
    val category: String?,
    val durationDays: String?,
    val yieldPotential: String?,
    val suitableZones: String?,
    val specialFeatures: String?,
    val specialFeaturesHi: String?,
    val source: String?
)

