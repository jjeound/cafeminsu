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
    fun prepareResponseRequiresMerchantUidAndAmount() {
        val result = PaymentPrepareRes(
            merchantUid = "merchant-123",
            amount = 10_000,
        ).toPreparedPayment(orderId = "77")

        assertTrue(result is AppResult.Success)
        val payment = (result as AppResult.Success).data
        assertEquals("77", payment.orderId)
        assertEquals("merchant-123", payment.merchantUid)
        assertEquals(10_000, payment.amount)
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
