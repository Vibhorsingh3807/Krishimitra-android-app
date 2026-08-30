package com.krishimitra.app.ml

import android.content.Context
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.krishimitra.app.domain.rag.RAGRetrievalResult
import org.json.JSONObject
import java.nio.LongBuffer
import java.util.Locale

class OnnxLanguageModel(private val context: Context) : AutoCloseable {

    companion object {
        private const val TAG = "OnnxLanguageModel"
        private const val MODEL_NAME = "krishi_mini_llm_quantized.onnx"
        private const val VOCAB_NAME = "vocab.json"
        private const val FIXED_SEQ_LEN = 32
    }

    private var env: OrtEnvironment? = null
    private var session: OrtSession? = null
    private var token2id: Map<String, Long> = emptyMap()
    private var id2token: Map<Long, String> = emptyMap()
    private var isLoaded = false

    init {
        loadModelAndVocab()
    }

    private fun loadModelAndVocab() {
        try {
            env = OrtEnvironment.getEnvironment()

            // 1. Load Vocab
            context.assets.open(VOCAB_NAME).use { stream ->
                val jsonStr = stream.bufferedReader().use { it.readText() }
                val obj = JSONObject(jsonStr)
                val t2id = mutableMapOf<String, Long>()
                val id2t = mutableMapOf<Long, String>()

                val t2idObj = obj.getJSONObject("token2id")
                val keys = t2idObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val id = t2idObj.getLong(k)
                    t2id[k] = id
                    id2t[id] = k
                }
                token2id = t2id
                id2token = id2t
            }

            // 2. Load ONNX Model
            val modelBytes = context.assets.open(MODEL_NAME).use { it.readBytes() }
            val sessionOptions = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(2)
            }
            session = env?.createSession(modelBytes, sessionOptions)
            isLoaded = true
            Log.i(TAG, "KrishiMiniLM loaded successfully with ${token2id.size} vocabulary tokens.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load KrishiMiniLM: ${e.message}", e)
        }
    }

    fun generateAnswer(ragResult: RAGRetrievalResult): String {
        val isHindi = ragResult.isHindi
        val query = ragResult.query

        // Guardrail: Refuse only if no verified fact or farmer experience was found
        if (ragResult.bestRecord == null && ragResult.farmerExperiences.isEmpty()) {
            return if (isHindi) {
                "इस सवाल का विश्वसनीय उत्तर ऑफलाइन उपलब्ध नहीं है। इंटरनेट चालू करके दोबारा पूछें अथवा किसान कॉल सेंटर (1800-180-1551) पर संपर्क करें।"
            } else {
                "I don't have reliable offline information for this question. Please connect to the internet and try again, or contact the Kisan Call Centre at 1800-180-1551."
            }
        }

        // Context-grounded synthesis
        val answerBuilder = StringBuilder()

        // 1. Primary Verified Fact
        if (ragResult.bestRecord != null) {
            val fact = if (isHindi) ragResult.bestRecord.answerHi else ragResult.bestRecord.answerEn
            answerBuilder.append(fact)
        }

        // 2. Separate Farmer Experience Memory with strict cautionary framing
        if (ragResult.farmerExperiences.isNotEmpty()) {
            if (answerBuilder.isNotEmpty()) {
                answerBuilder.append("\n\n")
            }
            if (isHindi) {
                answerBuilder.append("🌾 कुछ किसानों के अनुभव (अपुष्ट रिपोर्ट):\n")
                for (exp in ragResult.farmerExperiences) {
                    val obs = exp.observationHi ?: exp.observation
                    answerBuilder.append("• ${exp.state} के किसान: \"$obs\"\n")
                }
            } else {
                answerBuilder.append("🌾 Some Farmer Community Reports (Unverified):\n")
                for (exp in ragResult.farmerExperiences) {
                    val obs = exp.observationEn ?: exp.observation
                    answerBuilder.append("• Farmer in ${exp.state}: \"$obs\"\n")
                }
            }
        }

        // Optional neural inference pass to test on-device execution
        if (isLoaded && session != null && env != null) {
            try {
                val words = query.lowercase(Locale.ROOT).split("\\s+".toRegex()).filter { it.isNotBlank() }
                val tokens = mutableListOf<Long>()
                for (w in words) {
                    tokens.add(token2id[w] ?: token2id["<UNK>"] ?: 1L)
                }
                while (tokens.size < FIXED_SEQ_LEN) {
                    tokens.add(token2id["<PAD>"] ?: 0L)
                }
                val trimmed = tokens.take(FIXED_SEQ_LEN)

                val buffer = LongBuffer.allocate(FIXED_SEQ_LEN)
                for (t in trimmed) buffer.put(t)
                buffer.flip()

                val tensor = OnnxTensor.createTensor(env, buffer, longArrayOf(1, FIXED_SEQ_LEN.toLong()))
                tensor.use {
                    session?.run(mapOf("input_ids" to it))?.close()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Neural inference execution note: ${e.message}")
            }
        }

        return answerBuilder.toString()
    }

    override fun close() {
        try {
            session?.close()
            env?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing ONNX session: ${e.message}")
        }
    }
}
