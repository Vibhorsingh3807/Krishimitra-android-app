package com.krishimitra.app.domain.ai

import com.krishimitra.app.data.remote.ApiClient
import com.krishimitra.app.domain.model.ChatMessage
import com.krishimitra.app.domain.rag.LocalRAGEngine
import com.krishimitra.app.ml.LocalNLPEngine
import com.krishimitra.app.ml.OnnxLanguageModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface AIProvider {
    suspend fun answer(query: String, crop: String? = null, district: String? = null, forceLang: String? = null): ChatMessage
}

class LocalAIProvider(
    private val ragEngine: LocalRAGEngine,
    private val languageModel: OnnxLanguageModel,
    private val localEngine: LocalNLPEngine
) : AIProvider {
    override suspend fun answer(query: String, crop: String?, district: String?, forceLang: String?): ChatMessage = withContext(Dispatchers.Default) {
        val isHindi = forceLang == "hi" || (forceLang == null && query.any { it.code in 0x0900..0x097F })

        val (predictedIntent, _) = localEngine.predictIntent(query)
        val isMarketQuery = predictedIntent.startsWith("market_price")

        // 1. Retrieve knowledge via Local BM25 RAG Engine (unless market query meant for live DB)
        val ragResult = ragEngine.retrieve(query, forceLang)

        // 2. If RAG found a match or farmer experience, synthesize via OnnxLanguageModel
        if (!isMarketQuery && (ragResult.bestRecord != null || ragResult.farmerExperiences.isNotEmpty())) {
            val generatedText = languageModel.generateAnswer(ragResult)
            return@withContext ChatMessage(
                text = generatedText,
                isUser = false,
                source = ragResult.sourceBadge,
                isVerified = ragResult.isVerified,
                intent = ragResult.bestRecord?.topic ?: "agriculture"
            )
        }

        // 3. Fall back to Local NLP Intent & Knowledge Base / Live DB
        val nlpResult = localEngine.answerQuery(query, cropContext = crop, districtContext = district, forceLang = if (isHindi) "hi" else "en")
        val ans = if (isHindi) nlpResult.answerHi else nlpResult.answer

        return@withContext ChatMessage(
            text = ans,
            isUser = false,
            source = nlpResult.source,
            isVerified = nlpResult.isVerified,
            intent = nlpResult.intent
        )
    }
}

class RemoteAIProvider(private val apiClient: ApiClient) : AIProvider {
    override suspend fun answer(query: String, crop: String?, district: String?, forceLang: String?): ChatMessage = withContext(Dispatchers.IO) {
        val cloudResponse = apiClient.queryCloudAI(query, crop, district)
        if (cloudResponse != null) {
            val ans = cloudResponse.get("answer").asString
            val source = cloudResponse.get("source")?.asString ?: "Cloud AI (ICAR Grounded)"
            val verified = cloudResponse.get("is_verified_fact")?.asBoolean ?: false
            val intent = cloudResponse.get("detected_intent")?.asString

            return@withContext ChatMessage(
                text = ans,
                isUser = false,
                source = source,
                isVerified = verified,
                intent = intent
            )
        }
        throw IllegalStateException("Cloud AI request failed or offline")
    }
}

class HybridAIRouter(
    private val ragEngine: LocalRAGEngine,
    private val languageModel: OnnxLanguageModel,
    private val localEngine: LocalNLPEngine,
    private val apiClient: ApiClient
) {
    private val localProvider = LocalAIProvider(ragEngine, languageModel, localEngine)
    private val remoteProvider = RemoteAIProvider(apiClient)

    suspend fun routeQuery(query: String, crop: String? = null, district: String? = null, forceLang: String? = null): ChatMessage = withContext(Dispatchers.Default) {
        // Evaluate local provider first (RAG + LLM + NLP/Market DB fallback)
        val localMsg = localProvider.answer(query, crop, district, forceLang)

        // If local answer is verified OR device has no network, return immediately
        if (localMsg.isVerified || !apiClient.isNetworkAvailable()) {
            return@withContext localMsg
        }

        // Low confidence AND internet available -> Attempt remote AI fallback
        try {
            return@withContext remoteProvider.answer(query, crop, district, forceLang)
        } catch (e: Exception) {
            // Graceful fallback to local response
            return@withContext localMsg
        }
    }
}


