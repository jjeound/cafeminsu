package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.PaymentRequest
import com.cafeminsu.domain.model.PaymentResult
import com.cafeminsu.domain.model.PaymentStatus
import com.cafeminsu.domain.repository.PaymentRepository

class MockPaymentRepository(
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) : PaymentRepository {
    private val paymentsByIdempotencyKey = mutableMapOf<String, PaymentResult>()

    override suspend fun pay(request: PaymentRequest): AppResult<PaymentResult> {
        val validationError = validate(request)
        if (validationError != null) {
            return AppResult.Failure(validationError)
        }

        paymentsByIdempotencyKey[request.idempotencyKey]?.let { previous ->
            return if (previous.orderId == request.orderId) {
                AppResult.Success(previous)
            } else {
                AppResult.Failure(DomainError.Payment("idempotency-key-conflict"))
            }
        }

        val payment = PaymentResult(
            orderId = request.orderId,
            paymentId = "payment-${paymentsByIdempotencyKey.size + 1}",
            status = PaymentStatus.Approved,
            approvedAtMillis = nowMillis(),
        )
        paymentsByIdempotencyKey[request.idempotencyKey] = payment
        return AppResult.Success(payment)
    }

    override suspend fun getPaymentStatus(
        orderId: String,
        idempotencyKey: String,
    ): AppResult<PaymentResult> {
        val payment = paymentsByIdempotencyKey[idempotencyKey]
        return if (payment != null && payment.orderId == orderId) {
            AppResult.Success(payment)
        } else {
            AppResult.Failure(DomainError.NotFound)
        }
    }

    private fun validate(request: PaymentRequest): DomainError? =
        when {
            request.orderId.isBlank() -> DomainError.Validation("orderId")
            request.amount <= 0 -> DomainError.Validation("amount")
            request.paymentMethodToken.isBlank() -> DomainError.Payment("invalid-payment-token")
            request.idempotencyKey.isBlank() -> DomainError.Validation("idempotencyKey")
            else -> null
        }
}
