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

    private val _voiceMode = MutableStateFlow(VoiceMode.HINDI)
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

    private fun initSpeechRecognizer() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
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
                            val errorMsg = when (error) {
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                                SpeechRecognizer.ERROR_NETWORK -> "Network or offline speech pack error"
                                SpeechRecognizer.ERROR_NO_MATCH -> "No clear speech detected. Please speak closer to mic."
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Microphone busy"
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                                else -> "Voice recognition error ($error)"
                            }
                            Log.w(TAG, "Speech error: $errorMsg ($error)")
                            _lastError.value = errorMsg
                            _voiceState.value = VoiceState.ERROR
                        }

                        override fun onResults(results: Bundle?) {
                            _isListening.value = false
                            _voiceState.value = VoiceState.IDLE
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                val recognized = matches[0]
                                _transcription.value = recognized
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
                    })
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing SpeechRecognizer: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true

            val hindiLocale = Locale("hi", "IN")
            val avail = tts?.isLanguageAvailable(hindiLocale)
            isHindiTtsAvailable = (avail != TextToSpeech.LANG_NOT_SUPPORTED && avail != TextToSpeech.LANG_MISSING_DATA)

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

    fun startListening(onResult: (String) -> Unit) {
        stopSpeaking()
        this.onSpeechResultCallback = onResult
        _transcription.value = null
        _lastError.value = null
        _voiceState.value = VoiceState.RECORDING

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)

            when (_voiceMode.value) {
                VoiceMode.HINDI -> {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                }
                VoiceMode.ENGLISH -> {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")
                }
                VoiceMode.AUTO -> {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, arrayListOf("hi-IN", "en-IN"))
                }
            }
        }

        try {
            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening: ${e.message}")
            _isListening.value = false
            _voiceState.value = VoiceState.ERROR
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

        val targetLocale = if (shouldUseHindi && isHindiTtsAvailable) {
            Locale("hi", "IN")
        } else {
            Locale("en", "IN")
        }

        try {
            tts?.language = targetLocale
            tts?.setSpeechRate(0.92f)
            _voiceState.value = VoiceState.SPEAKING
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "krishi_tts_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Log.e(TAG, "Error speaking text: ${e.message}")
        }
    }

    fun stopSpeaking() {
        if (isTtsReady) {
            try {
                tts?.stop()
            } catch (ignored: Exception) {}
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
