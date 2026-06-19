package com.cafeminsu.domain.voice

import kotlinx.coroutines.flow.Flow

interface VoiceRecognizer {
    val events: Flow<VoiceRecognitionEvent>

    fun start()

    fun stop()

    fun destroy()
}

sealed interface VoiceRecognitionEvent {
    data class Partial(val text: String) : VoiceRecognitionEvent
    data class Final(val text: String) : VoiceRecognitionEvent
    data class Error(val reason: VoiceRecognitionError) : VoiceRecognitionEvent
    data object EndOfSpeech : VoiceRecognitionEvent
}

sealed interface VoiceRecognitionError {
    data object RecognizerUnavailable : VoiceRecognitionError
    data object PermissionDenied : VoiceRecognitionError
    data object Audio : VoiceRecognitionError
    data object Network : VoiceRecognitionError
    data object NetworkTimeout : VoiceRecognitionError
    data object NoMatch : VoiceRecognitionError
    data object Busy : VoiceRecognitionError
    data object Client : VoiceRecognitionError
    data object Server : VoiceRecognitionError
    data object SpeechTimeout : VoiceRecognitionError
    data object TooManyRequests : VoiceRecognitionError
    data class Unknown(val code: Int) : VoiceRecognitionError
}
