package com.cafeminsu.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface GifticonApi {
    @POST("api/gifticons")
    suspend fun purchaseGifticon(
        @Body request: GifticonPurchaseReq,
    ): BaseResponse<GifticonPurchaseRes>

    @GET("api/gifticons/my")
    suspend fun getMyGifticons(): BaseResponse<List<MyGifticonRes>>

    @GET("api/gifticons/{gifticonId}")
    suspend fun getGifticon(
        @Path("gifticonId") gifticonId: Long,
    ): BaseResponse<GifticonDetailRes>

    @POST("api/gifticons/{gifticonId}/use")
    suspend fun useGifticon(
        @Path("gifticonId") gifticonId: Long,
        @Body request: GifticonUseReq,
    ): BaseResponse<GifticonUseRes>

    @POST("api/gifticons/{gifticonId}/share")
    suspend fun shareGifticon(
        @Path("gifticonId") gifticonId: Long,
    ): BaseResponse<GifticonShareRes>
}

@JsonClass(generateAdapter = true)
data class GifticonPurchaseReq(
    val amount: Int,
    val receiverId: Long? = null,
    val receiverPhone: String? = null,
    val message: String? = null,
)

@JsonClass(generateAdapter = true)
data class GifticonPurchaseRes(
    val gifticonId: Long?,
    val qrCode: String?,
    val merchantUid: String?,
)

@JsonClass(generateAdapter = true)
data class MyGifticonRes(
    val gifticonId: Long?,
    val balance: Int?,
    val expiresAt: String?,
)

@JsonClass(generateAdapter = true)
data class GifticonDetailRes(
    val gifticonId: Long?,
    val amount: Int?,
    val balance: Int?,
    val qrCode: String?,
    val status: String?,
    val expiresAt: String?,
    val message: String?,
)

@JsonClass(generateAdapter = true)
data class GifticonUseReq(
    val orderId: Long,
    val usedAmount: Int,
)

@JsonClass(generateAdapter = true)
data class GifticonUseRes(
    val balanceAfter: Int?,
    val status: String?,
)

@JsonClass(generateAdapter = true)
data class GifticonShareRes(
    val shareLink: String?,
    val deepLink: String?,
)
