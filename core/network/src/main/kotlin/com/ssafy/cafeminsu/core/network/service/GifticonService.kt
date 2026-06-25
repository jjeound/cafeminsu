package com.ssafy.cafeminsu.core.network.service

import com.ssafy.cafeminsu.core.network.model.request.reward.GifticonPurchaseRequest
import com.ssafy.cafeminsu.core.network.model.request.reward.GifticonUseRequest
import com.ssafy.cafeminsu.core.network.model.response.reward.GifticonPurchaseResponse
import com.ssafy.cafeminsu.core.network.model.response.reward.GifticonResponse
import com.ssafy.cafeminsu.core.network.model.response.reward.GifticonShareResponse
import com.ssafy.cafeminsu.core.network.model.response.reward.GifticonUseResponse
import com.skydoves.sandwich.ApiResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface GifticonService {
    @POST("api/gifticons") suspend fun purchase(@Body request: GifticonPurchaseRequest): ApiResponse<GifticonPurchaseResponse>
    @GET("api/gifticons/my") suspend fun getMyGifticons(): ApiResponse<List<GifticonResponse>>
    @GET("api/gifticons/{gifticonId}") suspend fun getGifticon(@Path("gifticonId") gifticonId: Long): ApiResponse<GifticonResponse>
    @POST("api/gifticons/{gifticonId}/use") suspend fun useGifticon(@Path("gifticonId") gifticonId: Long, @Body request: GifticonUseRequest): ApiResponse<GifticonUseResponse>
    @POST("api/gifticons/{gifticonId}/share") suspend fun shareGifticon(@Path("gifticonId") gifticonId: Long): ApiResponse<GifticonShareResponse>
}
