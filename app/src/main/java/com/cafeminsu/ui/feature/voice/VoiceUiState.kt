package com.cafeminsu.ui.feature.voice

import com.cafeminsu.domain.voice.ParsedOrderItem

sealed interface VoiceUiState {
    val transcript: String

    data object PermissionRequired : VoiceUiState {
        override val transcript: String = ""
    }

    data object Idle : VoiceUiState {
        override val transcript: String = ""
    }

    data class Listening(
        val partialText: String,
    ) : VoiceUiState {
        override val transcript: String = partialText
    }

    data class Parsed(
        override val transcript: String,
        val items: List<ParsedOrderItem>,
        val unmatched: List<String>,
        val estimatedTotalAmount: Int = 0,
        val confidencePercent: Int = 0,
    ) : VoiceUiState

    data class AddedToCart(
        override val transcript: String,
        val items: List<ParsedOrderItem>,
    ) : VoiceUiState

    data class Error(
        val message: String,
        override val transcript: String = "",
    ) : VoiceUiState
}

sealed interface VoiceEvent {
    data object NavigateToCart : VoiceEvent
}
