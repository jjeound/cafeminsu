package com.cafeminsu.domain.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.PaymentRequest
import com.cafeminsu.domain.model.PaymentResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentRepositoryTest {
    @Test
    fun exposesPaymentRepositoryContract() = runBlocking {
        val repository = object : PaymentRepository {
            override suspend fun pay(request: PaymentRequest): AppResult<PaymentResult> =
                AppResult.Failure(com.cafeminsu.core.DomainError.Unknown)

            override suspend fun getPaymentStatus(
                orderId: String,
                idempotencyKey: String,
            ): AppResult<PaymentResult> = AppResult.Failure(com.cafeminsu.core.DomainError.Unknown)
        }

        assertTrue(repository.getPaymentStatus("order-1", "idem-1") is AppResult.Failure)
    }
}
