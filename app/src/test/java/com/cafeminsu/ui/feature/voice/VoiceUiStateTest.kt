package com.cafeminsu.ui.feature.voice

import com.cafeminsu.domain.voice.ParsedOrderItem
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceUiStateTest {
    @Test
    fun listeningStateUsesPartialTextAsTranscript() {
        val state = VoiceUiState.Listening(partialText = "아메리카노")

        assertEquals("아메리카노", state.transcript)
    }

    @Test
    fun interpretingStateExposesTranscript() {
        val state = VoiceUiState.Interpreting(transcript = "아메리카노 두 잔")

        assertEquals("아메리카노 두 잔", state.transcript)
    }

    @Test
    fun parsedStateKeepsFinalTranscriptAndUnmatchedItems() {
        val state = VoiceUiState.Parsed(
            transcript = "아메리카노 두 잔 피자 하나",
            items = listOf(
                ParsedOrderItem(
                    menuItemId = "americano",
                    name = "아메리카노",
                    quantity = 2,
                    selectedOptions = emptyList(),
                ),
            ),
            unmatched = listOf("피자 하나"),
        )

        assertEquals("아메리카노 두 잔 피자 하나", state.transcript)
        assertEquals(listOf("피자 하나"), state.unmatched)
        assertEquals(2, state.items.single().quantity)
    }
}
