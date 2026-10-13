package com.example.voice

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.data.model.VoiceSessionState
import com.example.data.model.VoiceSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class VoiceAssistantManager(
    private val context: Context,
    private val scope: CoroutineScope
) : RecognitionListener, TextToSpeech.OnInitListener {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    private val _sessionState = MutableStateFlow(VoiceSessionState.IDLE)
    val sessionState: StateFlow<VoiceSessionState> = _sessionState.asStateFlow()

    private val _currentTranscript = MutableStateFlow("")
    val currentTranscript: StateFlow<String> = _currentTranscript.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _actionProgressText = MutableStateFlow<String?>(null)
    val actionProgressText: StateFlow<String?> = _actionProgressText.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _formattedDuration = MutableStateFlow("00:00")
    val formattedDuration: StateFlow<String> = _formattedDuration.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    var onSpeechRecognized: ((String) -> Unit)? = null
    var onSessionEnded: (() -> Unit)? = null

    private var sessionStartTime: Long = 0L
    private var pausedDuration: Long = 0L
    private var pauseStartTime: Long = 0L
    private var timerJob: Job? = null

    private var currentVoiceSettings = VoiceSettings()
    private var isContinuousListening = false

    init {
        initializeTts()
    }

    private fun initializeTts() {
        try {
            textToSpeech = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            _errorMessage.value = "TTS init failed: ${e.message}"
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            textToSpeech?.let { tts ->
                applyTtsSettings(tts)
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _sessionState.value = VoiceSessionState.SPEAKING
                    }

                    override fun onDone(utteranceId: String?) {
                        scope.launch(Dispatchers.Main) {
                            if (_sessionState.value == VoiceSessionState.SPEAKING) {
                                if (isContinuousListening && currentVoiceSettings.pushToTalkMode.not()) {
                                    startListeningInternal()
                                } else {
                                    _sessionState.value = VoiceSessionState.IDLE
                                }
                            }
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        scope.launch(Dispatchers.Main) {
                            _sessionState.value = VoiceSessionState.IDLE
                        }
                    }
                })
            }
        } else {
            _errorMessage.value = "Device Text-to-Speech engine is not available"
        }
    }

    fun updateSettings(settings: VoiceSettings) {
        currentVoiceSettings = settings
        if (isTtsInitialized) {
            textToSpeech?.let { applyTtsSettings(it) }
        }
    }

    private fun applyTtsSettings(tts: TextToSpeech) {
        val targetLocale = if (currentVoiceSettings.responseLanguage.startsWith("bn")) {
            Locale("bn", "BD")
        } else {
            Locale.forLanguageTag(currentVoiceSettings.responseLanguage.ifBlank { "en-US" })
        }
        val langResult = tts.setLanguage(targetLocale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.language = Locale.US
        }
        tts.setSpeechRate(currentVoiceSettings.speakingRate)
        tts.setPitch(currentVoiceSettings.pitch)
    }

    fun setSessionState(state: VoiceSessionState, progressText: String? = null) {
        _sessionState.value = state
        _actionProgressText.value = progressText
    }

    fun startVoiceSession(continuousMode: Boolean = true) {
        if (_sessionState.value == VoiceSessionState.LISTENING || _sessionState.value == VoiceSessionState.PROCESSING) {
            return
        }

        isContinuousListening = continuousMode
        _errorMessage.value = null
        _currentTranscript.value = ""
        _partialTranscript.value = ""
        _actionProgressText.value = null

        startSessionTimer()
        startListeningInternal()
    }

    private fun startListeningInternal() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _sessionState.value = VoiceSessionState.ERROR
            _errorMessage.value = "Speech recognition service is not available on this device"
            return
        }

        scope.launch(Dispatchers.Main) {
            try {
                releaseRecognizer()
                _sessionState.value = VoiceSessionState.INITIALIZING

                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(this@VoiceAssistantManager)
                }

                val targetRecognitionLang = if (currentVoiceSettings.recognitionLanguage.startsWith("bn")) {
                    "bn-BD"
                } else {
                    currentVoiceSettings.recognitionLanguage.ifBlank { "en-US" }
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, targetRecognitionLang)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, targetRecognitionLang)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }

                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                _sessionState.value = VoiceSessionState.ERROR
                _errorMessage.value = "Failed to start microphone: ${e.message}"
            }
        }
    }

    fun pauseSession() {
        if (_sessionState.value != VoiceSessionState.IDLE && _sessionState.value != VoiceSessionState.PAUSED) {
            pauseSessionTimer()
            stopListeningInternal()
            stopSpeaking()
            _sessionState.value = VoiceSessionState.PAUSED
        }
    }

    fun resumeSession() {
        if (_sessionState.value == VoiceSessionState.PAUSED) {
            resumeSessionTimer()
            startListeningInternal()
        }
    }

    fun interruptSpeech() {
        stopSpeaking()
        if (isContinuousListening) {
            startListeningInternal()
        } else {
            _sessionState.value = VoiceSessionState.IDLE
        }
    }

    fun stopSpeaking() {
        try {
            textToSpeech?.stop()
        } catch (_: Exception) {}
        if (_sessionState.value == VoiceSessionState.SPEAKING) {
            _sessionState.value = VoiceSessionState.IDLE
        }
    }

    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        if (!isTtsInitialized || text.isBlank() || !currentVoiceSettings.speechOutputEnabled) {
            _sessionState.value = VoiceSessionState.IDLE
            onComplete?.invoke()
            return
        }

        stopListeningInternal()
        _sessionState.value = VoiceSessionState.SPEAKING

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "bushra_speech_${System.currentTimeMillis()}")
        }
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "bushra_speech_${System.currentTimeMillis()}")
    }

    fun endVoiceSession() {
        stopSessionTimer()
        stopListeningInternal()
        stopSpeaking()
        _sessionState.value = VoiceSessionState.IDLE
        _audioAmplitude.value = 0f
        _actionProgressText.value = null
        isContinuousListening = false
        onSessionEnded?.invoke()
    }

    private fun stopListeningInternal() {
        try {
            speechRecognizer?.stopListening()
            releaseRecognizer()
        } catch (_: Exception) {}
        _audioAmplitude.value = 0f
    }

    private fun releaseRecognizer() {
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }

    // --- Monotonic Session Timer ---
    private fun startSessionTimer() {
        sessionStartTime = SystemClock.elapsedRealtime()
        pausedDuration = 0L
        _elapsedSeconds.value = 0L
        _formattedDuration.value = "00:00"

        timerJob?.cancel()
        timerJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                if (_sessionState.value != VoiceSessionState.PAUSED && _sessionState.value != VoiceSessionState.IDLE) {
                    val now = SystemClock.elapsedRealtime()
                    val elapsedMillis = now - sessionStartTime - pausedDuration
                    val totalSecs = (elapsedMillis / 1000).coerceAtLeast(0)
                    _elapsedSeconds.value = totalSecs

                    val mins = totalSecs / 60
                    val secs = totalSecs % 60
                    _formattedDuration.value = String.format(Locale.US, "%02d:%02d", mins, secs)
                }
                delay(1000)
            }
        }
    }

    private fun pauseSessionTimer() {
        pauseStartTime = SystemClock.elapsedRealtime()
    }

    private fun resumeSessionTimer() {
        if (pauseStartTime > 0L) {
            pausedDuration += (SystemClock.elapsedRealtime() - pauseStartTime)
            pauseStartTime = 0L
        }
    }

    private fun stopSessionTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    // --- SpeechRecognizer RecognitionListener callbacks ---
    override fun onReadyForSpeech(params: Bundle?) {
        _sessionState.value = VoiceSessionState.LISTENING
    }

    override fun onBeginningOfSpeech() {
        _sessionState.value = VoiceSessionState.LISTENING
    }

    override fun onRmsChanged(rmsdB: Float) {
        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1.0f)
        _audioAmplitude.value = normalized
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _audioAmplitude.value = 0f
        _sessionState.value = VoiceSessionState.PROCESSING
    }

    override fun onError(error: Int) {
        _audioAmplitude.value = 0f
        val msg = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Speech recognition client error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
            SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service busy"
            SpeechRecognizer.ERROR_SERVER -> "Recognition server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
            else -> "Speech recognition error ($error)"
        }

        if (isContinuousListening && (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) {
            scope.launch(Dispatchers.Main) {
                delay(400)
                if (_sessionState.value != VoiceSessionState.IDLE && _sessionState.value != VoiceSessionState.PAUSED) {
                    startListeningInternal()
                }
            }
        } else {
            _sessionState.value = VoiceSessionState.IDLE
            if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                _errorMessage.value = msg
            }
        }
    }

    override fun onResults(results: Bundle?) {
        _audioAmplitude.value = 0f
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull()?.trim() ?: ""
        if (text.isNotBlank()) {
            _currentTranscript.value = text
            _partialTranscript.value = ""
            _sessionState.value = VoiceSessionState.PROCESSING
            onSpeechRecognized?.invoke(text)
        } else {
            if (isContinuousListening) {
                startListeningInternal()
            } else {
                _sessionState.value = VoiceSessionState.IDLE
            }
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val partial = matches?.firstOrNull()?.trim() ?: ""
        if (partial.isNotBlank()) {
            _partialTranscript.value = partial
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun destroy() {
        stopSessionTimer()
        stopListeningInternal()
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (_: Exception) {}
        textToSpeech = null
        isTtsInitialized = false
    }
}
