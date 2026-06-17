package com.cafeminsu.domain.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.PaymentRequest
import com.cafeminsu.domain.model.PaymentResult

interface PaymentRepository {
    suspend fun pay(request: PaymentRequest): AppResult<PaymentResult>
    suspend fun getPaymentStatus(orderId: String, idempotencyKey: String): AppResult<PaymentResult>
}
