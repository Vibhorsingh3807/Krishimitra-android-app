package com.krishimitra.app.domain.twin

import com.krishimitra.app.domain.model.*

/**
 * FieldTwinRuleEngine
 *
 * Transparent, deterministic rule-based agronomic intelligence:
 * 1. Trend Detection across time-series observations.
 * 2. Multi-parameter Environmental Risk Assessment (Moisture + NDVI + Temperature + Rain).
 * 3. Generation of structured recommendations and farmer-friendly AI explanation prompts.
 */
object FieldTwinRuleEngine {

    /**
     * Generates agronomic recommendations for a given zone based on its current observation and time series.
     */
    fun evaluateZone(
        zone: FieldZone,
        observation: ZoneObservation,
        history: List<ZoneObservation>,
        rainForecastProb: Int = 15
    ): ZoneRecommendation? {
        // Rule 1: Acute Water Stress (Low moisture, high temp, no imminent rain)
        if (observation.moisture < 20 && rainForecastProb < 35) {
            return ZoneRecommendation(
                id = "REC-${zone.id}-01",
                zoneId = zone.id,
                timestamp = System.currentTimeMillis(),
                title = "Water Stress Alert: Immediate Irrigation Needed",
                titleHi = "पानी की कमी की चेतावनी: तत्काल सिंचाई आवश्यक",
                recommendation = "Zone ${zone.zoneNumber} soil moisture has dropped to ${observation.moisture}% under ${observation.temperature.toInt()}°C heat. Rainfall is unlikely (${rainForecastProb}%). Inspect soil immediately and schedule 30-40mm irrigation to prevent crop wilting.",
                recommendationHi = "${zone.zoneLabelHi} में मिट्टी की नमी घटकर मात्र ${observation.moisture}% रह गई है तथा तापमान ${observation.temperature.toInt()}°C है। बारिश की संभावना नगण्य (${rainForecastProb}%) है। फसल को सूखने से बचाने के लिए तुरंत 30-40 मिमी सिंचाई करें।",
                priority = RiskLevel.HIGH,
                reason = "Moisture < 20%, Temperature > 34°C, Rain probability < 35%",
                reasonHi = "मृदा नमी < 20%, तापमान > 34°C, वर्षा संभावना < 35%",
                actionRequired = "IRRIGATE",
                isResolved = false
            )
        }

        // Rule 2: Low Moisture but High Rain Forecast (Delay Irrigation)
        if (observation.moisture < 23 && rainForecastProb >= 60) {
            return ZoneRecommendation(
                id = "REC-${zone.id}-02",
                zoneId = zone.id,
                timestamp = System.currentTimeMillis(),
                title = "Rain Forecast: Delay Scheduled Irrigation",
                titleHi = "बारिश का पूर्वानुमान: सिंचाई स्थगित रखें",
                recommendation = "Zone ${zone.zoneNumber} soil moisture is low (${observation.moisture}%), but weather forecast indicates high rain probability (${rainForecastProb}%). Delay tubewell irrigation by 24 hours to prevent waterlogging.",
                recommendationHi = "${zone.zoneLabelHi} में नमी कम है (${observation.moisture}%), परंतु अगले 24 घंटों में ${rainForecastProb}% वर्षा की संभावना है। जलभराव रोकने हेतु ट्यूबवेल सिंचाई फिलहाल स्थगित रखें।",
                priority = RiskLevel.MEDIUM,
                reason = "Rain probability >= 60% with moderate soil moisture deficit",
                reasonHi = "वर्षा संभावना >= 60%, अतिरिक्त पानी से बचाव आवश्यक",
                actionRequired = "HOLD_IRRIGATION",
                isResolved = false
            )
        }

        // Rule 3: Fungal Disease Risk (High Humidity + Moderate Temp)
        if (observation.humidity >= 75 && observation.temperature in 22f..30f) {
            return ZoneRecommendation(
                id = "REC-${zone.id}-03",
                zoneId = zone.id,
                timestamp = System.currentTimeMillis(),
                title = "Fungal Microclimate Risk Detected",
                titleHi = "फफूंद रोग का सूक्ष्म-जलवायु खतरा",
                recommendation = "High canopy humidity (${observation.humidity}%) and warm temperature (${observation.temperature.toInt()}°C) favor foliar fungal infections. Inspect lower leaf canopy for rust, blight, or powdery mildew.",
                recommendationHi = "हवा में उच्च नमी (${observation.humidity}%) व अनुकूल तापमान (${observation.temperature.toInt()}°C) से फफूंद जनित रोगों का खतरा बढ़ गया है। निचली पत्तियों पर रतुआ या झुलसा रोग की जांच करें।",
                priority = RiskLevel.MEDIUM,
                reason = "Relative Humidity >= 75% within favorable 22-30°C thermal band",
                reasonHi = "सापेक्ष आर्द्रता >= 75% और तापमान 22-30°C के बीच",
                actionRequired = "INSPECT",
                isResolved = false
            )
        }

        // Rule 4: Declining Vegetation Index (NDVI Decline Trend)
        if (history.size >= 3) {
            val firstNdvi = history.first().ndvi
            val lastNdvi = history.last().ndvi
            if (firstNdvi - lastNdvi > 0.15f) {
                return ZoneRecommendation(
                    id = "REC-${zone.id}-04",
                    zoneId = zone.id,
                    timestamp = System.currentTimeMillis(),
                    title = "Vegetation Health Declining Trend",
                    titleHi = "वनस्पति स्वास्थ्य में गिरावट का रुझान",
                    recommendation = "Satellite/field NDVI in Zone ${zone.zoneNumber} dropped from ${String.format("%.2f", firstNdvi)} to ${String.format("%.2f", lastNdvi)}. Scout zone for localized pest infestation or nutrient deficiency.",
                    recommendationHi = "${zone.zoneLabelHi} में वनस्पति सूचकांक (NDVI) ${String.format("%.2f", firstNdvi)} से गिरकर ${String.format("%.2f", lastNdvi)} पर आ गया है। कीट प्रकोप अथवा पोषक तत्व की कमी हेतु खेत का निरीक्षण करें।",
                    priority = RiskLevel.MEDIUM,
                    reason = "Significant NDVI drop over multi-temporal observation window",
                    reasonHi = "विगत अवलोकनों में NDVI में निरंतर गिरावट दर्ज",
                    actionRequired = "INSPECT",
                    isResolved = false
                )
            }
        }

        return null
    }

    /**
     * Formulates a structured natural language prompt for the AI Assistant,
     * grounding the LLM in verified physical measurements without hallucination.
     */
    fun buildAssistantPrompt(zone: FieldZone, obs: ZoneObservation, isHindi: Boolean): String {
        return if (isHindi) {
            """
            फसल: ${zone.cropHi}
            खेत ज़ोन: ${zone.zoneLabelHi} (${zone.areaAcres} एकड़)
            मिट्टी की नमी: ${obs.moisture}%
            वनस्पति सूचकांक (NDVI): ${String.format("%.2f", obs.ndvi)}
            तापमान: ${obs.temperature.toInt()}°C
            फसल स्वास्थ्य स्थिति: ${if (obs.cropHealth == CropHealthStatus.POOR) "खराब (तनावग्रस्त)" else "मध्यम"}
            जोखिम: ${obs.riskTypeHi}
            
            कृपया किसान के लिए सरल, व्यावहारिक भाषा में बताएं कि इस ज़ोन में तुरंत क्या कदम उठाने चाहिए?
            """.trimIndent()
        } else {
            """
            Crop: ${zone.crop}
            Field Zone: ${zone.zoneLabel} (${zone.areaAcres} acres)
            Soil Moisture: ${obs.moisture}%
            Vegetation Index (NDVI): ${String.format("%.2f", obs.ndvi)}
            Temperature: ${obs.temperature.toInt()}°C
            Crop Health: ${obs.cropHealth.name}
            Risk Factor: ${obs.riskType}
            
            Please provide simple, actionable agronomic advice for the farmer on managing this specific zone.
            """.trimIndent()
        }
    }
}
