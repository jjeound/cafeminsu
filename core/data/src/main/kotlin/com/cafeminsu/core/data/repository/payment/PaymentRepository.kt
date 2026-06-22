package com.cafeminsu.core.data.repository.payment

import com.cafeminsu.core.model.payment.PaymentRequest
import com.cafeminsu.core.model.payment.PaymentResult
import kotlinx.coroutines.flow.Flow

interface PaymentRepository {
    fun pay(request: PaymentRequest): Flow<PaymentResult>

    fun getPaymentStatus(
        orderId: String,
        idempotencyKey: String
    ): Flow<PaymentResult>
}
