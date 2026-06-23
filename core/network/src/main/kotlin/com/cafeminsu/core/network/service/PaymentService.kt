package com.cafeminsu.core.network.service

import com.cafeminsu.core.network.model.request.payment.PaymentPrepareRequest
import com.cafeminsu.core.network.model.request.payment.PaymentVerifyRequest
import com.cafeminsu.core.network.model.response.payment.PaymentDetailResponse
import com.cafeminsu.core.network.model.response.payment.PaymentPrepareResponse
import com.cafeminsu.core.network.model.response.payment.PaymentVerifyResponse
import com.skydoves.sandwich.ApiResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PaymentService {
    @POST("api/payments/prepare")
    suspend fun prepare(@Body request: PaymentPrepareRequest): ApiResponse<PaymentPrepareResponse>

    @POST("api/payments/verify")
    suspend fun verify(@Body request: PaymentVerifyRequest): ApiResponse<PaymentVerifyResponse>

    @GET("api/payments/{paymentId}")
    suspend fun getPayment(@Path("paymentId") paymentId: Long): ApiResponse<PaymentDetailResponse>
}
