package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.PaymentRequest
import com.cafeminsu.domain.model.PaymentResult
import com.cafeminsu.domain.model.PaymentStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockPaymentRepositoryTest {
    @Test
    fun payApprovesValidToken() = runBlocking {
        val repository = MockPaymentRepository()

        val result = repository.pay(paymentRequest()).successData()

        assertEquals(PaymentStatus.Approved, result.status)
        assertNotNull(result.approvedAtMillis)
    }

    @Test
    fun payFailsForMockFailureToken() = runBlocking {
        val repository = MockPaymentRepository()

        val result = repository.pay(
            paymentRequest(paymentMethodToken = "tok_mock_fail"),
        ).successData()

        assertEquals(PaymentStatus.Failed, result.status)
        assertEquals(null, result.approvedAtMillis)
    }

    @Test
    fun repeatedPayWithSameIdempotencyKeyReturnsSameResult() = runBlocking {
        val repository = MockPaymentRepository()
        val request = paymentRequest()

        val first = repository.pay(request).successData()
        val second = repository.pay(request).successData()
        val status = repository.getPaymentStatus(request.orderId, request.idempotencyKey).successData()

        assertEquals(first, second)
        assertEquals(first, status)
    }

    private fun paymentRequest(
        paymentMethodToken: String = "pg-token-1",
    ): PaymentRequest =
        PaymentRequest(
            orderId = "order-1",
            amount = 12_000,
            paymentMethodToken = paymentMethodToken,
            idempotencyKey = "idem-1",
        )

    @Suppress("UNCHECKED_CAST")
    private fun AppResult<PaymentResult>.successData(): PaymentResult {
        assertTrue(this is AppResult.Success<*>)
        return (this as AppResult.Success<PaymentResult>).data
    }
}
