package com.cafeminsu.core.network.client

import com.cafeminsu.core.network.model.request.payment.PaymentPrepareRequest
import com.cafeminsu.core.network.model.request.payment.PaymentVerifyRequest
import com.cafeminsu.core.network.model.response.payment.PaymentDetailResponse
import com.cafeminsu.core.network.model.response.payment.PaymentPrepareResponse
import com.cafeminsu.core.network.model.response.payment.PaymentVerifyResponse
import com.cafeminsu.core.network.service.PaymentService
import javax.inject.Inject

class PaymentClient @Inject constructor(
    private val paymentService: PaymentService,
) {
    suspend fun prepare(request: PaymentPrepareRequest): PaymentPrepareResponse = paymentService.prepare(request)

    suspend fun verify(request: PaymentVerifyRequest): PaymentVerifyResponse = paymentService.verify(request)

    suspend fun getPayment(paymentId: Long): PaymentDetailResponse = paymentService.getPayment(paymentId)
}
