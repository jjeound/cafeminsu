package com.cafeminsu.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RewardTest {
    @Test
    fun exposesRewardAndGifticonDomainModels() {
        val event = StampEvent(
            id = "stamp-1",
            orderId = "order-1",
            count = 1,
            createdAtMillis = 1_725_000_000_000L,
        )
        val stampCard = StampCard(
            userId = "user-1",
            currentCount = 4,
            goalCount = 10,
            history = listOf(event),
        )
        val gifticon = Gifticon(
            id = "gifticon-1",
            title = "아메리카노",
            barcodeValue = "barcode",
            qrValue = "qr",
            expiresAtMillis = 1_730_000_000_000L,
            status = GifticonStatus.Available,
        )

        assertEquals(4, stampCard.currentCount)
        assertEquals(10, stampCard.goalCount)
        assertEquals("order-1", stampCard.history.single().orderId)
        assertEquals("barcode", gifticon.barcodeValue)
        assertEquals(GifticonStatus.Available, gifticon.status)
    }

    @Test
    fun exposesAllGifticonStatuses() {
        assertEquals(3, GifticonStatus.entries.size)
    }
}
