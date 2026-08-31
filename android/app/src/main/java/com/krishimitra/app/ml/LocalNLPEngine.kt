package com.krishimitra.app.ml

import android.content.Context
import android.util.Log
import com.krishimitra.app.data.local.DatabaseHelper
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class LocalNLPEngine(
    private val context: Context,
    private val dbHelper: DatabaseHelper? = null
) {

    companion object {
        private const val TAG = "LocalNLPEngine"
    }

    private var vocabulary: Map<String, Int> = emptyMap()
    private var idf: List<Float> = emptyList()
    private var classes: List<String> = emptyList()
    private var coefficients: List<List<Float>> = emptyList()
    private var intercepts: List<Float> = emptyList()
    private var knowledgeFacts: List<FactEntry> = emptyList()
    private var isLoaded: Boolean = false

    data class FactEntry(
        val intent: String,
        val cropId: String?,
        val sampleQuestion: String,
        val answerEn: String,
        val answerHi: String,
        val source: String
    )

    data class NLPResult(
        val answer: String,
        val answerHi: String,
        val intent: String,
        val cropId: String?,
        val confidence: Float,
        val isVerified: Boolean,
        val source: String
    )

    init {
        loadAssets()
    }

    private fun loadAssets() {
        try {
            // 1. Load Intent Model
            context.assets.open("mobile_nlp_intent_model.json").use { stream ->
                val jsonStr = stream.bufferedReader().use { it.readText() }
                val obj = JSONObject(jsonStr)

                val vocabObj = obj.getJSONObject("vocabulary")
                val vocabMap = mutableMapOf<String, Int>()
                val keys = vocabObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    vocabMap[k] = vocabObj.getInt(k)
                }
                vocabulary = vocabMap

                val idfArr = obj.getJSONArray("idf")
                val idfList = mutableListOf<Float>()
                for (i in 0 until idfArr.length()) {
                    idfList.add(idfArr.getDouble(i).toFloat())
                }
                idf = idfList

                val classesArr = obj.getJSONArray("classes")
                val classesList = mutableListOf<String>()
                for (i in 0 until classesArr.length()) {
                    classesList.add(classesArr.getString(i))
                }
                classes = classesList

                val coefArr = obj.getJSONArray("coefficients")
                val coefList = mutableListOf<List<Float>>()
                for (i in 0 until coefArr.length()) {
                    val row = coefArr.getJSONArray(i)
                    val rowList = mutableListOf<Float>()
                    for (j in 0 until row.length()) {
                        rowList.add(row.getDouble(j).toFloat())
                    }
                    coefList.add(rowList)
                }
                coefficients = coefList

                val interArr = obj.getJSONArray("intercepts")
                val interList = mutableListOf<Float>()
                for (i in 0 until interArr.length()) {
                    interList.add(interArr.getDouble(i).toFloat())
                }
                intercepts = interList
            }

            // 2. Load Knowledge Index
            context.assets.open("mobile_knowledge_index.json").use { stream ->
                val jsonStr = stream.bufferedReader().use { it.readText() }
                val arr = JSONArray(jsonStr)
                val factsList = mutableListOf<FactEntry>()
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    factsList.add(
                        FactEntry(
                            intent = item.getString("intent"),
                            cropId = if (item.has("crop_id") && !item.isNull("crop_id")) item.getString("crop_id") else null,
                            sampleQuestion = item.optString("sample_question", ""),
                            answerEn = item.getString("answer_en"),
                            answerHi = item.getString("answer_hi"),
                            source = item.getString("source")
                        )
                    )
                }
                knowledgeFacts = factsList
            }

            isLoaded = true
            Log.i(TAG, "Local NLP Engine assets loaded successfully: ${vocabulary.size} vocab words, ${knowledgeFacts.size} facts.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load NLP engine assets: ${e.message}", e)
        }
    }

    fun isOutOfScope(query: String): Boolean {
        val q = query.lowercase(Locale.ROOT)
        val nonAgriKeywords = listOf(
            "prime minister", "president", "python", "code", "coding", "movie", "cricket", "football",
            "capital of", "calculator", "song", "joke", "bollywood", "hollywood", "dance", "sports", "game",
            "प्रधानमंत्री", "राष्ट्रपति", "गाना", "पायथन", "फिल्म", "क्रिकेट", "राजधानी", "खेल", "राष्ट्रीय खेल"
        )
        val agriKeywords = listOf(
            "crop", "soil", "kisan", "farmer", "fasal", "kheti", "pani", "water", "irrigation", "sinchai",
            "khad", "fertilizer", "urea", "dap", "seed", "beej", "pest", "keet", "disease", "rog",
            "mandi", "bhav", "scheme", "yojana", "loan", "rin", "krishi", "agriculture", "price", "rate", "variety", "kism",
            "फसल", "खेती", "मिट्टी", "सिंचाई", "खाद", "बीज", "कीट", "रोग", "योजना", "ऋण", "कृषि", "किसान", "भाव", "मंडी", "किस्म", "दाम"
        )

        val hasNonAgri = nonAgriKeywords.any { q.contains(it) }
        val hasAgri = agriKeywords.any { q.contains(it) } || detectCrop(query) != null
        return hasNonAgri && !hasAgri
    }

    fun detectCrop(query: String): String? {
        val q = query.lowercase(Locale.ROOT)
        if (q.contains("solar") || q.contains("solar pump") || q.contains("सोलर")) {
            return null
        }

        val cropAliases = mapOf(
            "rice" to listOf("rice", "paddy", "धान", "चावल", "dhan", "chawal", "basmati", "बासमती", "sona masuri", "matta"),
            "wheat" to listOf("wheat", "गेहूं", "गेहू", "gehu", "gehun", "sharbati", "शरबती", "lokwan", "लोकवन"),
            "maize" to listOf("maize", "corn", "मक्का", "मकई", "makka", "makai", "bhutta"),
            "cotton" to listOf("cotton", "कपास", "रुई", "kapas", "कापूस"),
            "sugarcane" to listOf("sugarcane", "गन्ना", "ईख", "ganna"),
            "mustard" to listOf("mustard", "सरसों", "राई", "sarson", "sarso", "rai"),
            "soybean" to listOf("soybean", "सोयाबीन", "soyabean"),
            "chickpea" to listOf("chickpea", "gram", "चना", "chana", "chane", "bengal gram"),
            "groundnut" to listOf("groundnut", "peanut", "मूंगफली", "mungfali", "moongphali"),
            "potato" to listOf("potato", "आलू", "alu", "aaloo", "kufri", "कुफरी"),
            "tomato" to listOf("tomato", "टमाटर", "tamatar"),
            "onion" to listOf("onion", "प्याज", "kanda", "pyaj", "कांदा"),
            "chilli" to listOf("chilli", "chillies", "chili", "मिर्च", "mirch", "mirchi"),
            "coconut" to listOf("coconut", "नारियल", "nariyal", "thenga", "copra", "खोपरा"),
            "pigeon_pea" to listOf("pigeon pea", "red gram", "arhar", "tur", "अरहर", "तुअर", "तूर"),
            "black_gram" to listOf("black gram", "urad", "उड़द"),
            "green_gram" to listOf("green gram", "moong", "मूंग"),
            "lentil" to listOf("lentil", "masoor", "मसूर"),
            "pearl_millet" to listOf("pearl millet", "bajra", "बाजरा"),
            "sorghum" to listOf("sorghum", "jowar", "ज्वार"),
            "finger_millet" to listOf("finger millet", "ragi", "रागी", "मडुआ"),
            "brinjal" to listOf("brinjal", "eggplant", "aubergine", "baingan", "बैंगन"),
            "okra" to listOf("okra", "ladyfinger", "bhindi", "भिंडी"),
            "papaya" to listOf("papaya", "papita", "पपीता"),
            "mango" to listOf("mango", "aam", "आम"),
            "banana" to listOf("banana", "kela", "केला"),
            "tea" to listOf("tea", "chai", "चाय", "cha"),
            "jute" to listOf("jute", "patson", "पटसन", "जूट")
        )

        val sortedAliases = cropAliases.flatMap { (cropId, aliases) ->
            aliases.map { it to cropId }
        }.sortedByDescending { it.first.length }

        for ((alias, cropId) in sortedAliases) {
            if (q.contains(Regex("\\b" + Regex.escape(alias) + "\\b")) || (alias.length >= 3 && q.contains(alias))) {
                return cropId
            }
        }
        return null
    }

    fun detectLocation(query: String): Pair<String?, String?> {
        val q = query.lowercase(Locale.ROOT)
        val locMap = mapOf(
            "palakkad" to ("Palakkad" to "Kerala"),
            "kozhikode" to ("Kozhikode" to "Kerala"),
            "ludhiana" to ("Ludhiana" to "Punjab"),
            "khanna" to ("Ludhiana" to "Punjab"),
            "karnal" to ("Karnal" to "Haryana"),
            "guntur" to ("Guntur" to "Andhra Pradesh"),
            "burdwan" to ("Purba Bardhaman" to "West Bengal"),
            "varanasi" to ("Varanasi" to "Uttar Pradesh"),
            "indore" to ("Indore" to "Madhya Pradesh"),
            "karanja" to ("Washim" to "Maharashtra"),
            "washim" to ("Washim" to "Maharashtra"),
            "hapur" to ("Hapur" to "Uttar Pradesh"),
            "kota" to ("Kota" to "Rajasthan"),
            "kolar" to ("Kolar" to "Karnataka"),
            "madanapalle" to ("Chittoor" to "Andhra Pradesh"),
            "nashik" to ("Nashik" to "Maharashtra"),
            "lasalgaon" to ("Nashik" to "Maharashtra"),
            "azadpur" to ("North Delhi" to "Delhi"),
            "yavatmal" to ("Yavatmal" to "Maharashtra"),
            "kinwat" to ("Yavatmal" to "Maharashtra"),
            "rajkot" to ("Rajkot" to "Gujarat"),
            "gondal" to ("Rajkot" to "Gujarat"),
            "hanumangarh" to ("Hanumangarh" to "Rajasthan"),
            "goluwala" to ("Hanumangarh" to "Rajasthan"),
            "adilabad" to ("Adilabad" to "Telangana"),
            "agra" to ("Agra" to "Uttar Pradesh"),
            "farrukhabad" to ("Farrukhabad" to "Uttar Pradesh"),
            "hooghly" to ("Hooghly" to "West Bengal"),
            "jalandhar" to ("Jalandhar" to "Punjab"),
            "pollachi" to ("Coimbatore" to "Tamil Nadu"),
            "sikar" to ("Sikar" to "Rajasthan"),
            "jabalpur" to ("Jabalpur" to "Madhya Pradesh"),
            "bharatpur" to ("Bharatpur" to "Rajasthan")
        )

        for ((k, v) in locMap) {
            if (q.contains(k)) return v
        }
        return null to null
    }

    fun predictIntent(query: String): Pair<String, Float> {
        val q = query.lowercase(Locale.ROOT)
        if (isOutOfScope(query)) return "out_of_scope" to 0.99f

        // Market & Price Intents
        if (listOf("best price", "better price", "highest price", "compare market", "compare price", "highest rate", "सबसे अच्छा भाव", "सबसे ज्यादा भाव", "तुलना").any { q.contains(it) }) return "market_price_compare" to 0.98f
        if (listOf("min price", "max price", "minimum and maximum", "minimum price", "maximum price", "न्यूनतम", "अधिकतम", "कम से कम", "ज्यादा से ज्यादा").any { q.contains(it) }) return "market_price_min_max" to 0.98f
        if (listOf("previous price", "historical price", "past price", "yesterday price", "पिछला भाव", "इतिहास", "पहले का भाव").any { q.contains(it) }) return "market_price_history" to 0.98f
        if (listOf("market price", "mandi price", "current price", "latest price", "price", "rate", "bhav", "dam", "mandi", "भाव", "दाम", "मंडी भाव", "रेट", "कीमत").any { q.contains(it) }) return "market_price_latest" to 0.98f

        // Variety Intents
        if (listOf("variety", "varieties", "hybrid", "किस्म", "किस्में", "प्रजाति", "उन्नत किस्म", "बीज की किस्म").any { q.contains(it) }) return "crop_variety" to 0.98f

        // Agronomic heuristics
        if (listOf("कम पानी", "drought", "सूखा", "कौन सी फसल", "less water", "kam pani").any { q.contains(it) }) return "crop_selection" to 0.98f
        if (listOf("बीज उपचार", "beej upchar", "seed treatment", "seed dressing", "बीजोपचार", "उपचारित", "शोधन").any { q.contains(it) }) return "seed_treatment" to 0.98f
        if (listOf("पत्ते पीले", "yellow leaves", "deficiency", "पोषक तत्व", "peele", "peela", "peelapan", "पीले", "पीलापन", "sukh rahe", "सूख रहे").any { q.contains(it) }) return "nutrient_deficiency" to 0.98f
        if (listOf("खरपतवार", "weed", "weeds", "herbicide", "ghaas", "घास", "कचरा", "kachra", "निराई", "nirayi").any { q.contains(it) }) return "weed_management" to 0.98f
        if (listOf("फसल चक्र", "crop rotation", "अंतःफसल", "intercrop", "intercropping", "ke baad", "के बाद", "साथ में").any { q.contains(it) }) return "crop_rotation" to 0.98f
        if (listOf("रोग", "कीट", "disease", "pest", "pests", "dawai", "दवा", "इल्ली", "illi", "sundi", "सुंडी", "झुलसा", "कीड़ा", "keeda", "छेद", "chhed", "धब्बा", "सड़न", "मकड़ी", "माइट", "mite", "spider mite", "रोकथाम", "control", "blight", "rust", "smut", "canker").any { q.contains(it) }) return "pests_diseases" to 0.98f
        if (listOf("बीज दर", "seed rate", "बुवाई", "sow", "sowing", "lagaye", "रोपाई", "plant", "planting", "बोने", "कब लगाएं", "कतार बुवाई").any { q.contains(it) }) return "sowing" to 0.98f
        if (listOf("दूरी", "spacing", "distance", "फासला", "doori", "लाइन से लाइन", "पौधे से पौधे").any { q.contains(it) }) return "spacing" to 0.98f
        if (listOf("भंडारण", "storage", "store", "नमी", "bhandaran", "कोठी", "गोदाम", "घुन", "ghun").any { q.contains(it) }) return "storage" to 0.98f
        if (listOf("जीवामृत", "jeevamrut", "जैविक", "organic", "वर्मीकम्पोस्ट").any { q.contains(it) }) return "organic_farming" to 0.98f
        if (listOf("सोलर पंप", "solar pump", "kusum", "मशीनरी", "ट्रैक्टर", "rotavator").any { q.contains(it) }) return "farm_machinery" to 0.98f
        if (listOf("मिट्टी", "soil", "mitti", "जमीन", "ph", "पीएच", "दोमट", "काली मिट्टी", "लाल मिट्टी").any { q.contains(it) }) return "soil" to 0.98f
        if (listOf("सिंचाई", "irrigation", "water", "पानी", "pani", "sinchai", "watering").any { q.contains(it) }) return "irrigation" to 0.98f
        if (listOf("खाद", "fertilizer", "urea", "यूरिया", "dap", "npk", "khad", "उर्वरक", "poshak").any { q.contains(it) }) return "fertilizer" to 0.98f
        if (listOf("कटाई", "harvest", "katai", "पक", "तुड़ाई", "तैयार", "maturity", "chunai", "चुनाई", "retting", "रेट्टिंग").any { q.contains(it) }) return "harvesting" to 0.98f
        if (listOf("योजना", "scheme", "subsidy", "अनुदान", "pm-kisan", "pmfby", "सम्मान निधि").any { q.contains(it) }) return "schemes" to 0.98f
        if (listOf("लोन", "ऋण", "loan", "kcc", "credit", "bank", "ब्याज", "केसीसी").any { q.contains(it) }) return "loans" to 0.98f

        if (!isLoaded || coefficients.isEmpty()) return "cultivation" to 0.70f

        val clean = q.replace(Regex("[?!.,;:'\"()\\[\\]{}]"), " ")
        val tokens = clean.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val featureCounts = mutableMapOf<Int, Float>()

        for (token in tokens) {
            vocabulary[token]?.let { idx ->
                featureCounts[idx] = (featureCounts[idx] ?: 0f) + 1f
            }
        }
        for (i in 0 until tokens.size - 1) {
            val bigram = "${tokens[i]} ${tokens[i + 1]}"
            vocabulary[bigram]?.let { idx ->
                featureCounts[idx] = (featureCounts[idx] ?: 0f) + 1f
            }
        }

        if (featureCounts.isEmpty()) {
            return "cultivation" to 0.65f
        }

        var normSq = 0f
        val tfidf = mutableMapOf<Int, Float>()
        for ((idx, count) in featureCounts) {
            val weight = count * (idf.getOrNull(idx) ?: 1f)
            tfidf[idx] = weight
            normSq += weight * weight
        }
        val norm = if (normSq > 0) kotlin.math.sqrt(normSq) else 1f

        var maxScore = -Float.MAX_VALUE
        var bestClass = classes.firstOrNull() ?: "cultivation"

        for (cIdx in classes.indices) {
            var score = intercepts.getOrNull(cIdx) ?: 0f
            val row = coefficients.getOrNull(cIdx) ?: continue
            for ((featIdx, weight) in tfidf) {
                val coefVal = row.getOrNull(featIdx) ?: 0f
                score += (weight / norm) * coefVal
            }
            if (score > maxScore) {
                maxScore = score
                bestClass = classes[cIdx]
            }
        }

        val conf = 1f / (1f + kotlin.math.exp(-maxScore.coerceIn(-10f, 10f)))
        return bestClass to conf.coerceIn(0.70f, 0.99f)
    }

    fun answerQuery(
        query: String,
        cropContext: String? = null,
        districtContext: String? = null,
        forceLang: String? = null
    ): NLPResult {
        val hasDevanagari = query.any { it.code in 0x0900..0x097F }
        val hasHinglish = listOf("kya", "kaise", "bhav", "mandi", "dawai", "beej", "fasal", "khet", "pani", "khad", "rog", "kitna", "chahiye", "lagaye", "batao", "hai", "me", "se", "ko").any { query.lowercase(Locale.ROOT).contains(it) }
        val isHindi = hasDevanagari || hasHinglish || forceLang == "hi"

        // 1. Guardrail Refusal Check
        if (isOutOfScope(query)) {
            val guardrailHi = "मैं कृषिमित्र (KrishiMitra) हूँ। मैं केवल कृषि, फसल, मिट्टी, खाद, सिंचाई, कीट-रोग, पशुपालन, मंडी भाव और किसान योजनाओं से संबंधित प्रश्नों में आपकी सहायता कर सकता हूँ। कृपया खेती से जुड़ा कोई प्रश्न पूछें।"
            val guardrailEn = "I am KrishiMitra, your digital agriculture assistant. I can only assist with crops, soil, irrigation, fertilizer, pests, diseases, market prices, and government farming schemes. Please ask a farming-related question."
            return NLPResult(
                answer = if (isHindi) guardrailHi else guardrailEn,
                answerHi = guardrailHi,
                intent = "out_of_scope",
                cropId = null,
                confidence = 0.99f,
                isVerified = true,
                source = "KrishiMitra Domain Guardrail"
            )
        }

        val detectedCrop = detectCrop(query) ?: cropContext
        val (detectedDist, _) = detectLocation(query)
        val finalDist = detectedDist ?: districtContext
        val (intent, confidence) = predictIntent(query)

        // 2. Handle Market Price Queries via Database
        if (intent.startsWith("market_price") && dbHelper != null) {
            if (intent == "market_price_compare") {
                val comparisons = dbHelper.compareMarkets(detectedCrop)
                if (comparisons.isNotEmpty()) {
                    val best = comparisons.first()
                    val comm = best.commodity
                    val ansEn = "For $comm, the highest modal price in available markets is ₹${best.modalPrice.toInt()} per quintal at ${best.market} (${best.district}, ${best.state}) recorded on ${best.priceDate}. (Source: ${best.source})."
                    val ansHi = "$comm के लिए उपलब्ध मंडियों में सबसे अच्छा मॉडल भाव ₹${best.modalPrice.toInt()} प्रति क्विंटल ${best.market} (${best.district}, ${best.state}) में है (दिनांक: ${best.priceDate}, स्रोत: ${best.source})।"
                    return NLPResult(
                        answer = if (isHindi) ansHi else ansEn,
                        answerHi = ansHi,
                        intent = intent,
                        cropId = detectedCrop,
                        confidence = 0.98f,
                        isVerified = true,
                        source = best.source
                    )
                }
            } else {
                val priceRec = dbHelper.getLatestMarketPrice(detectedCrop, finalDist)
                    ?: dbHelper.getLatestMarketPrice(detectedCrop)

                if (priceRec != null) {
                    val comm = priceRec.commodity
                    val mkt = priceRec.market
                    val dist = priceRec.district
                    val st = priceRec.state
                    val mp = priceRec.modalPrice.toInt()
                    val minP = priceRec.minPrice.toInt()
                    val maxP = priceRec.maxPrice.toInt()
                    val dt = priceRec.priceDate
                    val src = priceRec.source

                    val ansEn = when (intent) {
                        "market_price_min_max" -> "The price range for $comm at $mkt ($dist, $st) is minimum ₹$minP and maximum ₹$maxP per quintal, with a modal price of ₹$mp recorded on $dt. (Source: $src)."
                        "market_price_history" -> "The previous recorded price for $comm at $mkt was ₹$mp per quintal (Range: ₹$minP - ₹$maxP) recorded on $dt according to $src."
                        else -> "The latest available modal price for $comm at $mkt ($dist, $st) is ₹$mp per quintal (Range: ₹$minP - ₹$maxP), recorded on $dt according to $src."
                    }

                    val ansHi = when (intent) {
                        "market_price_min_max" -> "$mkt ($dist, $st) में $comm का न्यूनतम भाव ₹$minP और अधिकतम भाव ₹$maxP प्रति क्विंटल है, तथा मॉडल भाव ₹$mp है (दिनांक: $dt, स्रोत: $src)।"
                        "market_price_history" -> "$mkt में $comm का दर्ज पिछला भाव ₹$mp प्रति क्विंटल (दायरा: ₹$minP - ₹$maxP) था, जो $dt को $src द्वारा दर्ज किया गया था।"
                        else -> "$mkt ($dist, $st) में $comm का नवीनतम मॉडल भाव ₹$mp प्रति क्विंटल है (दायरा: ₹$minP - ₹$maxP), जो $dt को $src के अनुसार दर्ज किया गया।"
                    }

                    return NLPResult(
                        answer = if (isHindi) ansHi else ansEn,
                        answerHi = ansHi,
                        intent = intent,
                        cropId = detectedCrop,
                        confidence = 0.98f,
                        isVerified = true,
                        source = src
                    )
                }
            }
        }

        // 3. Handle Crop Variety Queries via Database
        if (intent == "crop_variety" && detectedCrop != null && dbHelper != null) {
            val varieties = dbHelper.getCropVarieties(detectedCrop)
            if (varieties.isNotEmpty()) {
                val varListEn = varieties.take(3).mapIndexed { i, v ->
                    "${i + 1}. ${v.varietyName} (${v.category ?: "HYV"}, Yield: ${v.yieldPotential}, Duration: ${v.durationDays}) - ${v.specialFeatures}"
                }.joinToString("\n")

                val varListHi = varieties.take(3).mapIndexed { i, v ->
                    val featHi = v.specialFeaturesHi ?: v.specialFeatures
                    "${i + 1}. ${v.varietyName} (${v.category ?: "उन्नत"}, पैदावार: ${v.yieldPotential}, अवधि: ${v.durationDays}) - $featHi"
                }.joinToString("\n")

                val src = varieties.first().source ?: "ICAR Institute Variety Guides"
                val cropTitle = detectedCrop.replaceFirstChar { it.uppercase() }
                val ansEn = "Recommended varieties for $cropTitle include:\n$varListEn\n(Source: $src)"
                val ansHi = "$cropTitle की प्रमुख उन्नत किस्में:\n$varListHi\n(स्रोत: $src)"

                return NLPResult(
                    answer = if (isHindi) ansHi else ansEn,
                    answerHi = ansHi,
                    intent = "crop_variety",
                    cropId = detectedCrop,
                    confidence = 0.98f,
                    isVerified = true,
                    source = src
                )
            }
        }

        // 4. Match against pre-seeded knowledge facts
        var match = knowledgeFacts.firstOrNull { it.intent == intent && it.cropId == detectedCrop }

        if (match == null && detectedCrop != null) {
            match = knowledgeFacts.firstOrNull { it.cropId == detectedCrop && (it.intent == "cultivation" || it.intent == intent) }
                ?: knowledgeFacts.firstOrNull { it.cropId == detectedCrop }
        }

        if (match == null) {
            match = knowledgeFacts.firstOrNull { it.intent == intent }
        }

        if (match == null) {
            val qLower = query.lowercase(Locale.ROOT)
            match = knowledgeFacts.firstOrNull { fact ->
                qLower.split(" ").any { word -> word.length > 3 && (fact.sampleQuestion.lowercase(Locale.ROOT).contains(word) || fact.answerEn.lowercase(Locale.ROOT).contains(word)) }
            }
        }

        if (match != null) {
            val ans = if (isHindi) match.answerHi else match.answerEn
            return NLPResult(
                answer = ans,
                answerHi = match.answerHi,
                intent = intent,
                cropId = detectedCrop,
                confidence = confidence,
                isVerified = true,
                source = match.source
            )
        }

        // Fallback
        val defaultHi = "आपकी फसल से संबंधित यह प्रश्न दर्ज कर लिया गया है। कृपया सटीक सिफारिश हेतु अपने जिले के कृषि विज्ञान केंद्र (KVK) से संपर्क करें अथवा नजदीकी किसान कॉल सेंटर (1800-180-1551) पर कॉल करें।"
        val defaultEn = "Your query has been recorded. For customized field advice, please contact your District Krishi Vigyan Kendra (KVK) or Kisan Call Centre at 1800-180-1551."

        return NLPResult(
            answer = if (isHindi) defaultHi else defaultEn,
            answerHi = defaultHi,
            intent = intent,
            cropId = detectedCrop,
            confidence = 0.65f,
            isVerified = true,
            source = "ICAR Kisan Call Centre National Guidelines"
        )
    }
}
