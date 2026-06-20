package com.cafeminsu.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentApiTest {
    @Test
    fun prepareRequestKeepsOpenApiFields() {
        val request = PaymentPrepareReq(
            orderId = 77,
            useGifticonId = null,
            gifticonAmount = null,
            cardAmount = 10_000,
        )

        assertEquals(77L, request.orderId)
        assertEquals(null, request.useGifticonId)
        assertEquals(null, request.gifticonAmount)
        assertEquals(10_000, request.cardAmount)
    }

    @Test
    fun verifyRequestKeepsOpenApiFields() {
        val request = PaymentVerifyReq(
            impUid = "imp_123",
            merchantUid = "merchant-123",
        )

        assertEquals("imp_123", request.impUid)
        assertEquals("merchant-123", request.merchantUid)
    }

    @Test
    fun paymentResponseDtosKeepOpenApiFields() {
        val prepare = PaymentPrepareRes(
            merchantUid = "merchant-123",
            amount = 10_000,
        )
        val verify = PaymentVerifyRes(
            paymentId = 31,
            status = "PAID",
        )
        val detail = PaymentDetailRes(
            paymentId = 31,
            orderId = 77,
            method = "CARD",
            amount = 10_000,
            status = "PAID",
            paidAt = "2026-06-20T01:15:30Z",
        )

        assertEquals("merchant-123", prepare.merchantUid)
        assertEquals(31L, verify.paymentId)
        assertEquals("PAID", detail.status)
        assertEquals("2026-06-20T01:15:30Z", detail.paidAt)
    }
}
