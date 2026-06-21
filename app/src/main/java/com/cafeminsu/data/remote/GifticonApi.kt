package com.cafeminsu.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface GifticonApi {
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
}

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
