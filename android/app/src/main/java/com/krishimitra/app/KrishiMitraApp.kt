package com.krishimitra.app

import android.app.Application
import com.krishimitra.app.data.local.DatabaseHelper
import com.krishimitra.app.data.remote.ApiClient
import com.krishimitra.app.domain.ai.HybridAIRouter
import com.krishimitra.app.ml.LocalNLPEngine
import com.krishimitra.app.ml.OnnxDiseaseClassifier
import com.krishimitra.app.voice.VoiceManager

class KrishiMitraApp : Application() {

    lateinit var dbHelper: DatabaseHelper
        private set
    lateinit var apiClient: ApiClient
        private set
    lateinit var localNlpEngine: LocalNLPEngine
        private set
    lateinit var ragEngine: com.krishimitra.app.domain.rag.LocalRAGEngine
        private set
    lateinit var languageModel: com.krishimitra.app.ml.OnnxLanguageModel
        private set
    lateinit var aiRouter: HybridAIRouter
        private set
    lateinit var diseaseClassifier: OnnxDiseaseClassifier
        private set
    lateinit var voiceManager: VoiceManager
        private set
    lateinit var languageManager: com.krishimitra.app.domain.language.LanguageManager
        private set

    override fun onCreate() {
        super.onCreate()
        languageManager = com.krishimitra.app.domain.language.LanguageManager.getInstance(this)
        dbHelper = DatabaseHelper.getInstance(this)
        apiClient = ApiClient(this)
        localNlpEngine = LocalNLPEngine(this)
        ragEngine = com.krishimitra.app.domain.rag.LocalRAGEngine(this, dbHelper)
        languageModel = com.krishimitra.app.ml.OnnxLanguageModel(this)
        aiRouter = HybridAIRouter(ragEngine, languageModel, localNlpEngine, apiClient)
        diseaseClassifier = OnnxDiseaseClassifier(this)
        voiceManager = VoiceManager(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        diseaseClassifier.close()
        languageModel.close()
        voiceManager.destroy()
    }
}
