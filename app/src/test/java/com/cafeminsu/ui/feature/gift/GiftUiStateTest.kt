package com.cafeminsu.ui.feature.gift

import com.cafeminsu.domain.model.GiftChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GiftUiStateTest {
    @Test
    fun contentFormatsAmountAndButtonState() {
        val content = GiftUiState.Content(
            selectedAmountOption = GiftAmountOption.TenThousand,
            selectedChannel = GiftChannel.KakaoTalk,
            recipient = "friend-1",
            message = "오늘 하루 수고 많았어",
        )

        assertEquals("10,000", content.selectedAmountLabel)
        assertEquals("₩ 10,000", content.previewAmountLabel)
        assertEquals("구매하고 선물 보내기 · 10,000원", content.primaryButtonText)
        assertTrue(content.canSend)
    }

    @Test
    fun blankRecipientCannotSend() {
        val content = GiftUiState.Content(
            selectedAmountOption = GiftAmountOption.TenThousand,
            selectedChannel = GiftChannel.KakaoTalk,
            recipient = "",
            message = "",
        )

        assertFalse(content.canSend)
    }
}
