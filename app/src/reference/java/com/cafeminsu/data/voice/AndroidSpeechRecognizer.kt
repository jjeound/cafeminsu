package com.cafeminsu.data.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer as PlatformSpeechRecognizer
import androidx.core.content.ContextCompat
import com.cafeminsu.domain.voice.VoiceRecognitionError
import com.cafeminsu.domain.voice.VoiceRecognitionEvent
import com.cafeminsu.domain.voice.VoiceRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class AndroidSpeechRecognizer @Inject constructor(
    @ApplicationContext private val context: Context,
) : VoiceRecognizer {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val mutableEvents = MutableSharedFlow<VoiceRecognitionEvent>(
        extraBufferCapacity = EVENT_BUFFER_CAPACITY,
    )
    private var recognizer: PlatformSpeechRecognizer? = null

    override val events: Flow<VoiceRecognitionEvent> = mutableEvents.asSharedFlow()

    override fun start() {
        runOnMain(::startListening)
    }

    override fun stop() {
        runOnMain {
            runCatching { recognizer?.stopListening() }
                .onFailure { emitError(VoiceRecognitionError.Client) }
        }
    }

    override fun destroy() {
        runOnMain(::releaseRecognizer)
    }

    private fun startListening() {
        if (!hasRecordAudioPermission()) {
            emitError(VoiceRecognitionError.PermissionDenied)
            return
        }

        if (!PlatformSpeechRecognizer.isRecognitionAvailable(context)) {
            emitError(VoiceRecognitionError.RecognizerUnavailable)
            return
        }

        runCatching {
            releaseRecognizer()
            recognizer = PlatformSpeechRecognizer.createSpeechRecognizer(context).also { speechRecognizer ->
                speechRecognizer.setRecognitionListener(createRecognitionListener())
                speechRecognizer.startListening(createRecognizerIntent())
            }
        }.onFailure {
            releaseRecognizer()
            emitError(VoiceRecognitionError.Client)
        }
    }

    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createRecognizerIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, KOREAN_LANGUAGE_TAG)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, MAX_RESULTS)
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit

            override fun onBeginningOfSpeech() = Unit

            override fun onRmsChanged(rmsdB: Float) = Unit

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                mutableEvents.tryEmit(VoiceRecognitionEvent.EndOfSpeech)
            }

            override fun onError(error: Int) {
                emitError(error.toVoiceRecognitionError())
                releaseRecognizer()
            }

            override fun onResults(results: Bundle?) {
                val finalText = results.bestRecognitionText()
                if (finalText == null) {
                    emitError(VoiceRecognitionError.NoMatch)
                } else {
                    mutableEvents.tryEmit(VoiceRecognitionEvent.Final(finalText))
                }
                releaseRecognizer()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                partialResults.bestRecognitionText()?.let { text ->
                    mutableEvents.tryEmit(VoiceRecognitionEvent.Partial(text))
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        }
    }

    private fun Bundle?.bestRecognitionText(): String? {
        return this
            ?.getStringArrayList(PlatformSpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun Int.toVoiceRecognitionError(): VoiceRecognitionError {
        return when (this) {
            PlatformSpeechRecognizer.ERROR_AUDIO -> VoiceRecognitionError.Audio
            PlatformSpeechRecognizer.ERROR_CLIENT -> VoiceRecognitionError.Client
            PlatformSpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VoiceRecognitionError.PermissionDenied
            PlatformSpeechRecognizer.ERROR_NETWORK -> VoiceRecognitionError.Network
            PlatformSpeechRecognizer.ERROR_NETWORK_TIMEOUT -> VoiceRecognitionError.NetworkTimeout
            PlatformSpeechRecognizer.ERROR_NO_MATCH -> VoiceRecognitionError.NoMatch
            PlatformSpeechRecognizer.ERROR_RECOGNIZER_BUSY -> VoiceRecognitionError.Busy
            PlatformSpeechRecognizer.ERROR_SERVER -> VoiceRecognitionError.Server
            PlatformSpeechRecognizer.ERROR_SPEECH_TIMEOUT -> VoiceRecognitionError.SpeechTimeout
            PlatformSpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> VoiceRecognitionError.TooManyRequests
            else -> VoiceRecognitionError.Unknown(this)
        }
    }

    private fun runOnMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }

    private fun emitError(reason: VoiceRecognitionError) {
        mutableEvents.tryEmit(VoiceRecognitionEvent.Error(reason))
    }

    private fun releaseRecognizer() {
        runCatching { recognizer?.destroy() }
        recognizer = null
    }

    private companion object {
        const val KOREAN_LANGUAGE_TAG = "ko-KR"
        const val MAX_RESULTS = 1
        const val EVENT_BUFFER_CAPACITY = 16
    }
}
