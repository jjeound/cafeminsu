package com.cafeminsu.ui.feature.gifticon

import com.cafeminsu.domain.model.Gifticon
import com.cafeminsu.domain.model.GifticonStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GifticonUiStateTest {
    @Test
    fun availableGifticonCanBeUsed() {
        val state = GifticonDetailUiState.Content(
            gifticon = sampleUiStateGifticon(status = GifticonStatus.Available),
        )

        assertTrue(state.canUse)
    }

    @Test
    fun usedAndExpiredGifticonsCannotBeUsed() {
        val used = GifticonDetailUiState.Content(
            gifticon = sampleUiStateGifticon(status = GifticonStatus.Used),
        )
        val expired = GifticonDetailUiState.Content(
            gifticon = sampleUiStateGifticon(status = GifticonStatus.Expired),
        )

        assertFalse(used.canUse)
        assertFalse(expired.canUse)
    }

    private fun sampleUiStateGifticon(status: GifticonStatus): Gifticon =
        Gifticon(
            id = "gifticon-1",
            title = "아메리카노 교환권",
            barcodeValue = "CAFE-MINSU-GIFT-0001",
            qrValue = "CAFE-MINSU-QR-0001",
            expiresAtMillis = 1_830_297_600_000L,
            status = status,
        )
}
