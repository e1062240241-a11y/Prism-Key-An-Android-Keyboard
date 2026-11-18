package com.example.engine

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed interface VoiceTypingState {
    data object Idle : VoiceTypingState
    data class Listening(
        val rmsDb: Float = 0f,
        val partialTranscript: String = "",
        val isOnDeviceEngine: Boolean = true,
    ) : VoiceTypingState
    data class Error(
        val message: String,
        val needsMicPermission: Boolean = false,
    ) : VoiceTypingState
}

class LocalVoiceTypingEngine(
    private val context: Context,
    private val onCommitFinalText: (String) -> Unit,
) {
    private val _state = MutableStateFlow<VoiceTypingState>(VoiceTypingState.Idle)
    val state: StateFlow<VoiceTypingState> = _state.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var usingOnDevice = false

    fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun startListening(languageTag: String = Locale.getDefault().toLanguageTag()) {
        if (!hasAudioPermission()) {
            _state.value = VoiceTypingState.Error(
                message = "Tap Allow Mic so you can type with your voice.",
                needsMicPermission = true,
            )
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.value = VoiceTypingState.Error(
                message = "Voice typing isn't available on this phone.",
            )
            return
        }

        stopListeningInternal()

        val onDeviceAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

        usingOnDevice = onDeviceAvailable
        val recognizer = if (onDeviceAvailable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            SpeechRecognizer.createSpeechRecognizer(context)
        }

        recognizer.setRecognitionListener(
            object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _state.value = VoiceTypingState.Listening(
                        rmsDb = 0f,
                        partialTranscript = "",
                        isOnDeviceEngine = usingOnDevice,
                    )
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {
                    val curr = _state.value
                    if (curr is VoiceTypingState.Listening) {
                        _state.value = curr.copy(rmsDb = rmsdB.coerceIn(0f, 12f))
                    }
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Couldn't hear the microphone — please try again"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone access is needed for voice typing"
                        SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that — tap the mic to try again"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No voice heard — tap the mic to try again"
                        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
                        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE, -> "Offline voice pack isn't saved for this language yet"
                        else -> "Voice typing paused — tap the mic to try again"
                    }
                    _state.value = VoiceTypingState.Error(
                        message = msg,
                        needsMicPermission = error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS,
                    )
                    destroyRecognizer()
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val best = matches?.firstOrNull()?.trim().orEmpty()
                    if (best.isNotEmpty()) {
                        onCommitFinalText("$best ")
                    }
                    _state.value = VoiceTypingState.Idle
                    destroyRecognizer()
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partial = matches?.firstOrNull()?.trim().orEmpty()
                    val curr = _state.value
                    if (curr is VoiceTypingState.Listening) {
                        _state.value = curr.copy(partialTranscript = partial)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            },
        )

        speechRecognizer = recognizer

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        _state.value = VoiceTypingState.Listening(
            rmsDb = 1f,
            partialTranscript = "",
            isOnDeviceEngine = usingOnDevice,
        )
        recognizer.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _state.value = VoiceTypingState.Idle
        destroyRecognizer()
    }

    fun release() {
        stopListeningInternal()
        _state.value = VoiceTypingState.Idle
    }

    private fun stopListeningInternal() {
        runCatching { speechRecognizer?.cancel() }
        destroyRecognizer()
    }

    private fun destroyRecognizer() {
        runCatching { speechRecognizer?.destroy() }
        speechRecognizer = null
    }
}
