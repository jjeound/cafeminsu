package com.cafeminsu.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PaymentApi {
    @POST("api/payments/prepare")
    suspend fun prepare(
        @Query("userId") userId: Long,
        @Body request: PaymentPrepareReq,
    ): BaseResponse<PaymentPrepareRes>

    @POST("api/payments/verify")
    suspend fun verify(
        @Query("userId") userId: Long,
        @Body request: PaymentVerifyReq,
    ): BaseResponse<PaymentVerifyRes>

    @GET("api/payments/{paymentId}")
    suspend fun getPayment(
        @Path("paymentId") paymentId: Long,
        @Query("userId") userId: Long,
    ): BaseResponse<PaymentDetailRes>
}

@JsonClass(generateAdapter = true)
data class PaymentPrepareReq(
    val orderId: Long,
    val useGifticonId: Long? = null,
    val gifticonAmount: Int? = null,
    val cardAmount: Int? = null,
)

@JsonClass(generateAdapter = true)
data class PaymentPrepareRes(
    val merchantUid: String?,
    val amount: Int?,
)

@JsonClass(generateAdapter = true)
data class PaymentVerifyReq(
    val impUid: String,
    val merchantUid: String,
)

@JsonClass(generateAdapter = true)
data class PaymentVerifyRes(
    val paymentId: Long?,
    val status: String?,
)

@JsonClass(generateAdapter = true)
data class PaymentDetailRes(
    val paymentId: Long?,
    val orderId: Long?,
    val method: String?,
    val amount: Int?,
    val status: String?,
    val paidAt: String?,
)
