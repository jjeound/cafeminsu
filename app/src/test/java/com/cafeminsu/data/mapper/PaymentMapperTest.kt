package com.cafeminsu.data.mapper

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.remote.PaymentDetailRes
import com.cafeminsu.data.remote.PaymentPrepareRes
import com.cafeminsu.data.remote.PaymentVerifyRes
import com.cafeminsu.domain.model.PaymentStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentMapperTest {
    @Test
    fun prepareResponseFallsBackToAmountAsCardAmountForCardFlow() {
        val result = PaymentPrepareRes(
            merchantUid = "merchant-123",
            amount = 10_000,
        ).toPreparedPayment(orderId = "77")

        assertTrue(result is AppResult.Success)
        val payment = (result as AppResult.Success).data
        assertEquals("77", payment.orderId)
        assertEquals("merchant-123", payment.merchantUid)
        assertEquals(10_000, payment.cardAmount)
        // status 미지정/READY 는 카드 흐름(Pending)으로 진행한다.
        assertEquals(PaymentStatus.Pending, payment.status)
        assertEquals(null, payment.paymentId)
    }

    @Test
    fun prepareResponsePaidMapsToApprovedWithPaymentId() {
        val result = PaymentPrepareRes(
            merchantUid = "merchant-123",
            amount = 0,
            cardAmount = 0,
            gifticonAmount = 10_000,
            status = "PAID",
            paymentId = 9,
        ).toPreparedPayment(orderId = "77")

        assertTrue(result is AppResult.Success)
        val payment = (result as AppResult.Success).data
        assertEquals(0, payment.cardAmount)
        assertEquals(PaymentStatus.Approved, payment.status)
        assertEquals("9", payment.paymentId)
    }

    @Test
    fun prepareResponseUsesServerCardAmountForSplit() {
        val result = PaymentPrepareRes(
            merchantUid = "merchant-123",
            amount = 10_000,
            cardAmount = 7_000,
            gifticonAmount = 3_000,
            status = "READY",
        ).toPreparedPayment(orderId = "77")

        assertTrue(result is AppResult.Success)
        val payment = (result as AppResult.Success).data
        assertEquals(7_000, payment.cardAmount)
        assertEquals(PaymentStatus.Pending, payment.status)
        assertEquals(null, payment.paymentId)
    }

    @Test
    fun prepareResponsePaidWithoutPaymentIdMapsToUnknownError() {
        val result = PaymentPrepareRes(
            merchantUid = "merchant-123",
            amount = 0,
            cardAmount = 0,
            status = "PAID",
            paymentId = null,
        ).toPreparedPayment(orderId = "77")

        assertEquals(AppResult.Failure(DomainError.Unknown), result)
    }

    @Test
    fun verifyPaidMapsToApprovedPaymentResult() {
        val result = PaymentVerifyRes(
            paymentId = 31,
            status = "PAID",
        ).toPaymentResult(orderId = "77")

        assertTrue(result is AppResult.Success)
        val payment = (result as AppResult.Success).data
        assertEquals("77", payment.orderId)
        assertEquals("31", payment.paymentId)
        assertEquals(PaymentStatus.Approved, payment.status)
        assertEquals(null, payment.approvedAtMillis)
    }

    @Test
    fun verifyReadyMapsToPendingNotApproved() {
        val result = PaymentVerifyRes(
            paymentId = 31,
            status = "READY",
        ).toPaymentResult(orderId = "77")

        assertTrue(result is AppResult.Success)
        assertEquals(PaymentStatus.Pending, (result as AppResult.Success).data.status)
    }

    @Test
    fun detailPaidMapsPaidAtToApprovedMillis() {
        val result = PaymentDetailRes(
            paymentId = 31,
            orderId = 77,
            method = "CARD",
            amount = 10_000,
            status = "PAID",
            paidAt = "2026-06-20T01:15:30Z",
        ).toPaymentResult()

        assertTrue(result is AppResult.Success)
        val payment = (result as AppResult.Success).data
        assertEquals("77", payment.orderId)
        assertEquals("31", payment.paymentId)
        assertEquals(PaymentStatus.Approved, payment.status)
        assertEquals(1_781_918_130_000, payment.approvedAtMillis)
    }

    @Test
    fun missingPaymentIdMapsToUnknownError() {
        val result = PaymentVerifyRes(
            paymentId = null,
            status = "PAID",
        ).toPaymentResult(orderId = "77")

        assertEquals(AppResult.Failure(DomainError.Unknown), result)
    }
}
