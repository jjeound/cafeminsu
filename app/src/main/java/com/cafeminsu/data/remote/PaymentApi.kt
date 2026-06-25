package com.cafeminsu.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PaymentApi {
    @POST("api/payments/prepare")
    suspend fun prepare(
        @Body request: PaymentPrepareReq,
    ): PaymentPrepareRes

    @POST("api/payments/verify")
    suspend fun verify(
        @Body request: PaymentVerifyReq,
    ): PaymentVerifyRes

    @GET("api/payments/{paymentId}")
    suspend fun getPayment(
        @Path("paymentId") paymentId: Long,
    ): PaymentDetailRes
}

@JsonClass(generateAdapter = true)
data class PaymentPrepareReq(
    val orderId: Long,
    // 사용할 기프티콘 ID만 전달한다. 카드/기프티콘 금액 분할은 서버가 권위 있게 계산한다.
    val useGifticonId: Long? = null,
)

@JsonClass(generateAdapter = true)
data class PaymentPrepareRes(
    val merchantUid: String?,
    val amount: Int?,
    // 서버가 계산한 분할 금액과 확정 상태. 전액 기프티콘이면 status=PAID, cardAmount=0, paymentId 포함.
    val cardAmount: Int? = null,
    val gifticonAmount: Int? = null,
    val status: String? = null,
    val paymentId: Long? = null,
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
