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
            recipient = "",
            message = "오늘 하루 수고 많았어",
            selectedFriendName = "친구",
        )

        assertEquals("10,000", content.selectedAmountLabel)
        assertEquals("₩ 10,000", content.previewAmountLabel)
        assertEquals("구매하고 선물 보내기 · 10,000원", content.primaryButtonText)
        assertEquals("친구", content.friendPickLabel)
        assertTrue(content.canSend)
    }

    @Test
    fun kakaoTalkWithoutSelectedFriendCannotSend() {
        val content = GiftUiState.Content(
            selectedAmountOption = GiftAmountOption.TenThousand,
            selectedChannel = GiftChannel.KakaoTalk,
            recipient = "",
            message = "",
        )

        assertFalse(content.canSend)
        assertFalse(content.hasSelectedFriend)
        assertEquals("카카오톡 친구 선택", content.friendPickLabel)
    }

    @Test
    fun smsWithoutRecipientCannotSend() {
        val content = GiftUiState.Content(
            selectedAmountOption = GiftAmountOption.TenThousand,
            selectedChannel = GiftChannel.Sms,
            recipient = "",
            message = "",
        )

        assertFalse(content.canSend)
    }

    @Test
    fun smsWithRecipientCanSend() {
        val content = GiftUiState.Content(
            selectedAmountOption = GiftAmountOption.TenThousand,
            selectedChannel = GiftChannel.Sms,
            recipient = "010-1234-5678",
            message = "",
        )

        assertTrue(content.canSend)
    }
}
