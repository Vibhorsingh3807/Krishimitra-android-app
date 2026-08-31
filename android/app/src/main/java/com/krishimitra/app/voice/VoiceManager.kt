package com.krishimitra.app.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class VoiceMode {
    AUTO,
    HINDI,
    ENGLISH
}

enum class VoiceState {
    IDLE,
    RECORDING,
    PROCESSING,
    SPEAKING,
    ERROR
}

class VoiceManager(private val context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "VoiceManager"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var isHindiTtsAvailable = false

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _transcription = MutableStateFlow<String?>(null)
    val transcription: StateFlow<String?> = _transcription.asStateFlow()

    private val _voiceMode = MutableStateFlow(VoiceMode.HINDI) // Default to Hindi for farmer app
    val voiceMode: StateFlow<VoiceMode> = _voiceMode.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private var onSpeechResultCallback: ((String) -> Unit)? = null

    init {
        try {
            tts = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to instantiate TTS: ${e.message}")
        }
        initSpeechRecognizer()
    }

    fun initSpeechRecognizer() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createRecognitionListener())
                }
                Log.i(TAG, "SpeechRecognizer initialized successfully.")
            } else {
                Log.w(TAG, "Speech recognition is not available on this device service.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize SpeechRecognizer: ${e.message}", e)
        }
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _isListening.value = true
            _voiceState.value = VoiceState.RECORDING
            _lastError.value = null
        }

        override fun onBeginningOfSpeech() {
            _voiceState.value = VoiceState.RECORDING
        }

        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _isListening.value = false
            _voiceState.value = VoiceState.PROCESSING
        }

        override fun onError(error: Int) {
            _isListening.value = false
            try {
                speechRecognizer?.cancel()
            } catch (ignored: Exception) {}

            val errorMsg = when (error) {
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout / इंटरनेट धीमा है"
                SpeechRecognizer.ERROR_NETWORK -> "इंटरनेट कनेक्शन की जांच करें / Check internet"
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error / माइक त्रुटि"
                SpeechRecognizer.ERROR_SERVER -> "Server error / सर्वर त्रुटि"
                SpeechRecognizer.ERROR_CLIENT -> "Client error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "कोई आवाज नहीं सुनाई दी / No speech detected"
                SpeechRecognizer.ERROR_NO_MATCH -> "आवाज स्पष्ट नहीं सुनाई दी, दोबारा बोलें"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "माइक व्यस्त था, पुनः प्रयास करें"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "माइक्रोफोन अनुमति आवश्यक है"
                else -> "आवाज पहचानने में त्रुटि ($error)"
            }
            Log.w(TAG, "Speech error code $error: $errorMsg")
            _lastError.value = errorMsg
            _voiceState.value = VoiceState.ERROR

            // Self-healing: if recognizer got stuck in busy or client error, recreate it cleanly
            if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
                error == SpeechRecognizer.ERROR_CLIENT ||
                error == SpeechRecognizer.ERROR_SERVER) {
                initSpeechRecognizer()
            }
        }

        override fun onResults(results: Bundle?) {
            _isListening.value = false
            _voiceState.value = VoiceState.IDLE
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val recognized = matches[0]
                _transcription.value = recognized
                Log.i(TAG, "Speech recognition result: $recognized")
                onSpeechResultCallback?.invoke(recognized)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                _transcription.value = matches[0]
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true

            val hindiLocale = Locale("hi", "IN")
            val avail = tts?.isLanguageAvailable(hindiLocale)
            isHindiTtsAvailable = (avail != TextToSpeech.LANG_NOT_SUPPORTED && avail != TextToSpeech.LANG_MISSING_DATA)

            if (isHindiTtsAvailable) {
                Log.i(TAG, "Offline Hindi TTS voice pack is available.")
            } else {
                Log.w(TAG, "Hindi TTS pack status: $avail. Setting generic Hindi.")
            }

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                    _voiceState.value = VoiceState.SPEAKING
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    _voiceState.value = VoiceState.IDLE
                }

                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    _voiceState.value = VoiceState.IDLE
                }
            })
        }
    }

    fun setVoiceMode(mode: VoiceMode) {
        _voiceMode.value = mode
    }

    fun createSpeechIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)

            when (_voiceMode.value) {
                VoiceMode.HINDI -> {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "hi", "en-IN"))
                }
                VoiceMode.ENGLISH -> {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-IN", "en-US", "hi-IN"))
                }
                VoiceMode.AUTO -> {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "hi", "en-IN"))
                }
            }
            putExtra(RecognizerIntent.EXTRA_PROMPT, "अपनी फसल या खेती के बारे में पूछें...")
        }
    }

    fun startListening(onResult: (String) -> Unit) {
        stopSpeaking()
        this.onSpeechResultCallback = onResult
        _transcription.value = null
        _lastError.value = null

        if (speechRecognizer == null) {
            initSpeechRecognizer()
        }

        val intent = createSpeechIntent()
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.startListening(intent)
            _isListening.value = true
            _voiceState.value = VoiceState.RECORDING
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening: ${e.message}", e)
            _isListening.value = false
            _voiceState.value = VoiceState.ERROR
            _lastError.value = e.message
            initSpeechRecognizer()
            throw e
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping listening: ${e.message}")
        } finally {
            _isListening.value = false
            if (_voiceState.value == VoiceState.RECORDING) {
                _voiceState.value = VoiceState.PROCESSING
            }
        }
    }

    fun speak(text: String, forceHindi: Boolean? = null) {
        if (!isTtsReady || tts == null) return
        stopSpeaking()

        val shouldUseHindi = forceHindi ?: when (_voiceMode.value) {
            VoiceMode.HINDI -> true
            VoiceMode.ENGLISH -> false
            VoiceMode.AUTO -> LanguageDetector.isHindiResponsePreferred(text, VoiceMode.AUTO)
        }

        try {
            if (shouldUseHindi) {
                val hiLocale = Locale("hi", "IN")
                val avail = tts?.isLanguageAvailable(hiLocale)
                if (avail != TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(hiLocale)
                } else {
                    tts?.setLanguage(Locale("hi"))
                }
            } else {
                tts?.setLanguage(Locale("en", "IN"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error setting TTS locale: ${e.message}")
            try { tts?.setLanguage(Locale.getDefault()) } catch (ignored: Exception) {}
        }

        tts?.setSpeechRate(0.92f)
        _voiceState.value = VoiceState.SPEAKING
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "krishi_tts_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        if (isTtsReady) {
            try {
                tts?.stop()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping TTS: ${e.message}")
            }
            _isSpeaking.value = false
            if (_voiceState.value == VoiceState.SPEAKING) {
                _voiceState.value = VoiceState.IDLE
            }
        }
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying voice manager: ${e.message}")
        }
    }
}
