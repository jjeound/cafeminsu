package com.ssafy.cafeminsu.core.network.client

import com.ssafy.cafeminsu.core.network.model.request.reward.GifticonPurchaseRequest
import com.ssafy.cafeminsu.core.network.model.request.reward.GifticonUseRequest
import com.ssafy.cafeminsu.core.network.model.response.reward.GifticonPurchaseResponse
import com.ssafy.cafeminsu.core.network.model.response.reward.GifticonResponse
import com.ssafy.cafeminsu.core.network.model.response.reward.GifticonShareResponse
import com.ssafy.cafeminsu.core.network.model.response.reward.GifticonUseResponse
import com.ssafy.cafeminsu.core.network.service.GifticonService
import com.skydoves.sandwich.ApiResponse
import javax.inject.Inject

class GifticonClient @Inject constructor(private val gifticonService: GifticonService) {
    suspend fun purchase(request: GifticonPurchaseRequest): ApiResponse<GifticonPurchaseResponse> = gifticonService.purchase(request)
    suspend fun getMyGifticons(): ApiResponse<List<GifticonResponse>> = gifticonService.getMyGifticons()
    suspend fun getGifticon(gifticonId: Long): ApiResponse<GifticonResponse> = gifticonService.getGifticon(gifticonId)
    suspend fun useGifticon(gifticonId: Long, request: GifticonUseRequest): ApiResponse<GifticonUseResponse> = gifticonService.useGifticon(gifticonId, request)
    suspend fun shareGifticon(gifticonId: Long): ApiResponse<GifticonShareResponse> = gifticonService.shareGifticon(gifticonId)
}
