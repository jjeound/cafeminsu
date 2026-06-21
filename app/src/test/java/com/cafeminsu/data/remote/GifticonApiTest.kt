package com.cafeminsu.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class GifticonApiTest {
    @Test
    fun myGifticonDtoKeepsOpenApiFields() {
        val gifticon = MyGifticonRes(
            gifticonId = 31,
            balance = 10_000,
            expiresAt = "2026-08-31T15:00:00Z",
        )

        assertEquals(31L, gifticon.gifticonId)
        assertEquals(10_000, gifticon.balance)
        assertEquals("2026-08-31T15:00:00Z", gifticon.expiresAt)
    }

    @Test
    fun gifticonDetailDtoKeepsQrAndStatusFields() {
        val detail = GifticonDetailRes(
            gifticonId = 31,
            amount = 10_000,
            balance = 8_500,
            qrCode = "qr-value",
            status = "PARTIAL",
            expiresAt = "2026-08-31T15:00:00Z",
            message = "고마워",
        )

        assertEquals("qr-value", detail.qrCode)
        assertEquals("PARTIAL", detail.status)
        assertEquals("고마워", detail.message)
    }

    @Test
    fun gifticonUseDtoKeepsRequiredRequestAndResponseFields() {
        val request = GifticonUseReq(
            orderId = 31,
            usedAmount = 10_000,
        )
        val response = GifticonUseRes(
            balanceAfter = 0,
            status = "USED",
        )

        assertEquals(31L, request.orderId)
        assertEquals(10_000, request.usedAmount)
        assertEquals(0, response.balanceAfter)
        assertEquals("USED", response.status)
    }
}
