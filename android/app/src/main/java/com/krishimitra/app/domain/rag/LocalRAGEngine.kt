package com.krishimitra.app.domain.rag

import android.content.Context
import android.util.Log
import com.krishimitra.app.data.local.DatabaseHelper
import com.krishimitra.app.domain.model.FarmerExperience
import com.krishimitra.app.domain.model.RAGKnowledgeRecord
import java.util.Locale
import kotlin.math.ln

data class RAGRetrievalResult(
    val query: String,
    val isHindi: Boolean,
    val bestRecord: RAGKnowledgeRecord?,
    val topRecords: List<RAGKnowledgeRecord>,
    val farmerExperiences: List<FarmerExperience>,
    val confidence: Float,
    val contextPrompt: String,
    val sourceBadge: String,
    val isVerified: Boolean
)

class LocalRAGEngine(
    private val context: Context,
    private val dbHelper: DatabaseHelper
) {
    companion object {
        private const val TAG = "LocalRAGEngine"
        private const val BM25_K1 = 1.2f
        private const val BM25_B = 0.75f
    }

    private var indexedRecords: List<RAGKnowledgeRecord> = emptyList()
    private var docTokens: List<List<String>> = emptyList()
    private var docLengths: List<Int> = emptyList()
    private var avgDocLength: Float = 0f
    private val dfMap: MutableMap<String, Int> = mutableMapOf()
    private var totalDocs: Int = 0

    init {
        indexKnowledgeBase()
    }

    fun indexKnowledgeBase() {
        try {
            var records = dbHelper.getAllRAGKnowledge()
            if (records.isEmpty()) {
                Log.w(TAG, "RAG Knowledge base in SQLite is empty. Loading fallback from mobile_knowledge_index.json...")
                records = loadFallbackFromAssets()
            }

            indexedRecords = records
            totalDocs = records.size

            val tokenized = mutableListOf<List<String>>()
            val lengths = mutableListOf<Int>()
            var sumLength = 0

            dfMap.clear()

            for (rec in records) {
                // Build searchable text combining topic, crop, questions, answers, and keywords
                val fullText = "${rec.topic} ${rec.cropId ?: ""} ${rec.questionEn} ${rec.questionHi} ${rec.answerEn} ${rec.answerHi}"
                val tokens = tokenize(fullText)
                tokenized.add(tokens)
                lengths.add(tokens.size)
                sumLength += tokens.size

                val uniqueTokens = tokens.toSet()
                for (t in uniqueTokens) {
                    dfMap[t] = (dfMap[t] ?: 0) + 1
                }
            }

            docTokens = tokenized
            docLengths = lengths
            avgDocLength = if (totalDocs > 0) sumLength.toFloat() / totalDocs else 1f

            Log.i(TAG, "Indexed $totalDocs RAG records with ${dfMap.size} unique terms. Avg doc len: $avgDocLength")
        } catch (e: Exception) {
            Log.e(TAG, "Error indexing RAG knowledge: ${e.message}", e)
        }
    }

    private fun loadFallbackFromAssets(): List<RAGKnowledgeRecord> {
        val list = mutableListOf<RAGKnowledgeRecord>()
        try {
            context.assets.open("mobile_knowledge_index.json").use { stream ->
                val jsonStr = stream.bufferedReader().use { it.readText() }
                val arr = org.json.JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        RAGKnowledgeRecord(
                            id = "asset_$i",
                            topic = obj.optString("intent", "agriculture"),
                            cropId = if (obj.has("crop_id") && !obj.isNull("crop_id")) obj.getString("crop_id") else null,
                            questionEn = obj.optString("sample_question", ""),
                            questionHi = obj.optString("sample_question", ""),
                            answerEn = obj.optString("answer_en", ""),
                            answerHi = obj.optString("answer_hi", ""),
                            source = obj.optString("source", "ICAR Verified Agricultural Data"),
                            sourceUrl = null,
                            isVerified = true,
                            category = "VERIFIED_KNOWLEDGE"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading fallback index: ${e.message}")
        }
        return list
    }

    fun tokenize(text: String): List<String> {
        val clean = text.lowercase(Locale.ROOT)
            .replace(Regex("[?!.,;:'\"()\\[\\]{}|/\\\\@#$%^&*_+=~`-]"), " ")
        return clean.split("\\s+".toRegex()).filter { it.length >= 2 }
    }

    fun detectCropId(query: String): String? {
        val q = query.lowercase(Locale.ROOT)
        if (q.contains("solar") || q.contains("solar pump") || q.contains("सोलर")) {
            return null
        }

        val cropAliases = mapOf(
            "rice" to listOf("rice", "paddy", "धान", "चावल", "dhan", "chawal", "basmati", "बासमती", "sona masuri", "matta"),
            "wheat" to listOf("wheat", "गेहूं", "गेहू", "gehu", "gehun", "sharbati", "शरबती", "lokwan", "लोकवन", "hd 2967", "dbw 187"),
            "maize" to listOf("maize", "corn", "मक्का", "मकई", "makka", "makai", "bhutta"),
            "cotton" to listOf("cotton", "कपास", "रुई", "kapas", "कापूस", "bt cotton"),
            "sugarcane" to listOf("sugarcane", "गन्ना", "ईख", "ganna"),
            "mustard" to listOf("mustard", "सरसों", "राई", "sarson", "sarso", "rai", "toria"),
            "soybean" to listOf("soybean", "सोयाबीन", "soyabean"),
            "chickpea" to listOf("chickpea", "gram", "चना", "chana", "chane", "bengal gram"),
            "groundnut" to listOf("groundnut", "peanut", "मूंगफली", "mungfali", "moongphali"),
            "potato" to listOf("potato", "आलू", "alu", "aaloo", "kufri", "कुफरी"),
            "tomato" to listOf("tomato", "टमाटर", "tamatar"),
            "onion" to listOf("onion", "प्याज", "kanda", "pyaj", "कांदा"),
            "chilli" to listOf("chilli", "chillies", "chili", "मिर्च", "mirch", "mirchi"),
            "mango" to listOf("mango", "aam", "आम"),
            "banana" to listOf("banana", "kela", "केला"),
            "bajra" to listOf("bajra", "बाजरा", "pearl millet", "millet"),
            "jowar" to listOf("jowar", "ज्वार", "sorghum"),
            "ragi" to listOf("ragi", "रागी", "finger millet", "मडुआ"),
            "arhar" to listOf("arhar", "tur", "tuar", "अरहर", "तुअर", "तूर", "pigeon pea", "red gram"),
            "moong" to listOf("moong", "मूंग", "green gram"),
            "urad" to listOf("urad", "उड़द", "black gram", "mash"),
            "masoor" to listOf("masoor", "मसूर", "lentil"),
            "peas" to listOf("peas", "मटर", "matar"),
            "sesame" to listOf("sesame", "तिल", "til"),
            "sunflower" to listOf("sunflower", "सूरजमुखी", "surajmukhi"),
            "safflower" to listOf("safflower", "कुसुम", "kusum"),
            "brinjal" to listOf("brinjal", "eggplant", "aubergine", "baingan", "बैंगन"),
            "okra" to listOf("okra", "ladyfinger", "bhindi", "भिंडी"),
            "cabbage" to listOf("cabbage", "पत्तागोभी", "patta gobhi", "bandha gobhi"),
            "cauliflower" to listOf("cauliflower", "फूलगोभी", "phool gobhi"),
            "carrot" to listOf("carrot", "गाजर", "gajar"),
            "radish" to listOf("radish", "मूली", "mooli"),
            "green_peas" to listOf("green peas", "हरी मटर"),
            "papaya" to listOf("papaya", "पपीता", "papita"),
            "guava" to listOf("guava", "अमरूद", "amrood"),
            "citrus" to listOf("citrus", "नींबू", "nimbu", "संत्रा", "santara", "orange", "kinnow"),
            "grapes" to listOf("grapes", "अंगूर", "angoor"),
            "pomegranate" to listOf("pomegranate", "अनार", "anaar"),
            "watermelon" to listOf("watermelon", "तरबूज", "tarbooz"),
            "turmeric" to listOf("turmeric", "हल्दी", "haldi"),
            "ginger" to listOf("ginger", "अदरक", "adrak"),
            "garlic" to listOf("garlic", "लहसुन", "lahsun"),
            "coriander" to listOf("coriander", "धनिया", "dhaniya"),
            "cumin" to listOf("cumin", "जीरा", "jeera"),
            "black_pepper" to listOf("black pepper", "काली मिर्च", "kali mirch"),
            "tea" to listOf("tea", "चाय", "chai"),
            "coffee" to listOf("coffee", "कॉफी"),
            "rubber" to listOf("rubber", "रबर"),
            "jute" to listOf("jute", "जूट", "patson", "पटसन")
        )

        // Check longest alias match first
        val sortedAliases = cropAliases.flatMap { (cropId, aliases) ->
            aliases.map { it to cropId }
        }.sortedByDescending { it.first.length }

        for ((alias, cropId) in sortedAliases) {
            if (q.contains(alias)) return cropId
        }
        return null
    }

    fun retrieve(query: String, forceLang: String? = null, topK: Int = 3): RAGRetrievalResult {
        val isHindi = forceLang == "hi" || (forceLang == null && query.any { it.code in 0x0900..0x097F })
        val detectedCrop = detectCropId(query)
        val queryTokens = tokenize(query)

        if (queryTokens.isEmpty() || totalDocs == 0) {
            return RAGRetrievalResult(
                query = query,
                isHindi = isHindi,
                bestRecord = null,
                topRecords = emptyList(),
                farmerExperiences = emptyList(),
                confidence = 0f,
                contextPrompt = "",
                sourceBadge = if (isHindi) "अज्ञात प्रश्न" else "Unknown query",
                isVerified = false
            )
        }

        // BM25 Scoring
        val scores = FloatArray(totalDocs)
        for (i in 0 until totalDocs) {
            val doc = docTokens[i]
            val docLen = docLengths[i].toFloat()
            val rec = indexedRecords[i]

            // Boost score if detected crop matches document crop
            val cropBoost = if (detectedCrop != null && rec.cropId == detectedCrop) 1.8f else 1.0f

            // Intent / Topic boost based on farmer query keywords
            val qLower = query.lowercase(Locale.ROOT)
            val topicBoost = when (rec.topic) {
                "market_price" -> if (qLower.contains("bhav") || qLower.contains("mandi") || qLower.contains("भाव") || qLower.contains("मंडी") || qLower.contains("price") || qLower.contains("rate") || qLower.contains("दाम")) 2.2f else 1.0f
                "variety" -> if (qLower.contains("variety") || qLower.contains("kism") || qLower.contains("किस्म") || qLower.contains("उन्नत") || qLower.contains("seed") || qLower.contains("beej") || qLower.contains("बीज")) 2.2f else 1.0f
                "scheme" -> if (qLower.contains("scheme") || qLower.contains("yojana") || qLower.contains("योजना") || qLower.contains("subsidy") || qLower.contains("सब्सिडी") || qLower.contains("kisan") || qLower.contains("pm-kisan")) 2.0f else 1.0f
                "loan" -> if (qLower.contains("loan") || qLower.contains("rin") || qLower.contains("ऋण") || qLower.contains("लोन") || qLower.contains("kcc") || qLower.contains("credit") || qLower.contains("ब्याज")) 2.0f else 1.0f
                "disease_treatment" -> if (qLower.contains("disease") || qLower.contains("rog") || qLower.contains("रोग") || qLower.contains("ilaj") || qLower.contains("इलाज") || qLower.contains("upchar") || qLower.contains("उपचार") || qLower.contains("dawa") || qLower.contains("दवाई") || qLower.contains("blight") || qLower.contains("rust") || qLower.contains("झुलसा") || qLower.contains("रतुआ")) 2.0f else 1.0f
                "fertilizer" -> if (qLower.contains("fertilizer") || qLower.contains("khad") || qLower.contains("खाद") || qLower.contains("urea") || qLower.contains("यूरिया") || qLower.contains("dap") || qLower.contains("उर्वरक")) 2.0f else 1.0f
                "irrigation" -> if (qLower.contains("irrigation") || qLower.contains("pani") || qLower.contains("पानी") || qLower.contains("sinchai") || qLower.contains("सिंचाई")) 2.0f else 1.0f
                "sowing" -> if (qLower.contains("sowing") || qLower.contains("buwai") || qLower.contains("बुवाई") || qLower.contains("samay") || qLower.contains("season") || qLower.contains("मौसम")) 2.0f else 1.0f
                "pests" -> if (qLower.contains("pest") || qLower.contains("keet") || qLower.contains("कीट") || qLower.contains("कीड़ा") || qLower.contains("borer")) 2.0f else 1.0f
                else -> 1.0f
            }

            // Term frequency in doc
            val tfMap = mutableMapOf<String, Int>()
            for (w in doc) {
                tfMap[w] = (tfMap[w] ?: 0) + 1
            }

            var docScore = 0f
            for (qTerm in queryTokens) {
                val tf = tfMap[qTerm] ?: 0
                if (tf > 0) {
                    val df = dfMap[qTerm] ?: 1
                    val idf = ln(1f + (totalDocs - df + 0.5f) / (df + 0.5f))
                    val numerator = tf * (BM25_K1 + 1f)
                    val denominator = tf + BM25_K1 * (1f - BM25_B + BM25_B * (docLen / avgDocLength))
                    docScore += idf * (numerator / denominator)
                }
            }

            // If crop matches, give positive base score so crop facts are always considered
            val isMatchingCrop = (detectedCrop != null && rec.cropId == detectedCrop)
            if (isMatchingCrop && docScore == 0f) {
                docScore = 1.0f
            }

            scores[i] = docScore * cropBoost * topicBoost
        }

        // Rank by highest score
        val rankedIndices = scores.indices.sortedByDescending { scores[it] }
        val topMatches = mutableListOf<RAGKnowledgeRecord>()
        val maxScore = if (rankedIndices.isNotEmpty()) scores[rankedIndices[0]] else 0f

        for (i in 0 until minOf(topK, rankedIndices.size)) {
            val idx = rankedIndices[i]
            if (scores[idx] > 0.15f) {
                topMatches.add(indexedRecords[idx])
            }
        }

        val best = topMatches.firstOrNull() ?: if (maxScore > 0.10f && rankedIndices.isNotEmpty()) indexedRecords[rankedIndices[0]] else null
        // Normalized confidence
        val confidence = if (maxScore > 0) (maxScore / (maxScore + 5f)).coerceIn(0.1f, 0.99f) else 0f

        // Retrieve Farmer Experiences if crop detected
        val experiences = if (detectedCrop != null) {
            dbHelper.getFarmerExperiencesByCrop(detectedCrop)
        } else {
            emptyList()
        }

        // Build structured context prompt
        val contextBuilder = StringBuilder()
        if (best != null) {
            if (isHindi) {
                contextBuilder.append("प्रमाणित कृषि ज्ञान (${best.source}):\n")
                contextBuilder.append(best.answerHi)
            } else {
                contextBuilder.append("Verified Agricultural Knowledge (${best.source}):\n")
                contextBuilder.append(best.answerEn)
            }
        }

        if (experiences.isNotEmpty()) {
            contextBuilder.append("\n\n")
            if (isHindi) {
                contextBuilder.append("किसान अनुभव (स्थानीय रिपोर्ट - अप्रमाणित):\n")
                for (exp in experiences) {
                    val obs = exp.observationHi ?: exp.observation
                    contextBuilder.append("• ${exp.state} के किसान: \"$obs\"\n")
                }
            } else {
                contextBuilder.append("Farmer Community Reports (Unverified):\n")
                for (exp in experiences) {
                    val obs = exp.observationEn ?: exp.observation
                    contextBuilder.append("• Farmer in ${exp.state}: \"$obs\"\n")
                }
            }
        }

        val badge = when {
            best != null && experiences.isNotEmpty() -> if (isHindi) "भाकृअनुप प्रमाणित + किसान अनुभव" else "ICAR Verified + Farmer Report"
            best != null -> if (isHindi) "भाकृअनुप (ICAR) प्रमाणित" else "ICAR Verified Fact"
            experiences.isNotEmpty() -> if (isHindi) "किसान अनुभव (अपुष्ट)" else "Farmer Experience"
            else -> if (isHindi) "ऑफलाइन जानकारी सीमित" else "Limited Offline Info"
        }

        return RAGRetrievalResult(
            query = query,
            isHindi = isHindi,
            bestRecord = best,
            topRecords = topMatches,
            farmerExperiences = experiences,
            confidence = confidence,
            contextPrompt = contextBuilder.toString(),
            sourceBadge = badge,
            isVerified = best?.isVerified ?: false
        )
    }
}
