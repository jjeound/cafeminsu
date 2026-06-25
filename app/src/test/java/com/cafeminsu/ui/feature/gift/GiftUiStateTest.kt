package com.cafeminsu.ui.feature.gift

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GiftUiStateTest {
    @Test
    fun contentFormatsAmountAndButtonState() {
        val content = GiftUiState.Content(
            selectedAmountOption = GiftAmountOption.TenThousand,
            message = "오늘 하루 수고 많았어",
        )

        assertEquals("10,000", content.selectedAmountLabel)
        assertEquals("₩ 10,000", content.previewAmountLabel)
        assertEquals("구매하고 선물 보내기 · 10,000원", content.primaryButtonText)
        // 카카오톡 단일 채널: 금액만 있으면 전송 가능.
        assertTrue(content.canSend)
    }

    @Test
    fun zeroAmountCannotSend() {
        val content = GiftUiState.Content(
            selectedAmountOption = GiftAmountOption.Custom,
            message = "",
            customAmountText = "",
        )

        assertFalse(content.canSend)
    }

    @Test
    fun customAmountEnablesSend() {
        val content = GiftUiState.Content(
            selectedAmountOption = GiftAmountOption.Custom,
            message = "",
            customAmountText = "5000",
        )

        assertEquals(5_000, content.selectedAmount)
        assertTrue(content.canSend)
    }
}
