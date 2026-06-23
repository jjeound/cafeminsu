package com.cafeminsu.core.network.client

import com.cafeminsu.core.network.model.response.reward.StampDetailResponse
import com.cafeminsu.core.network.model.response.reward.StampSummaryResponse
import com.cafeminsu.core.network.service.StampService
import com.skydoves.sandwich.ApiResponse
import javax.inject.Inject

class StampClient @Inject constructor(private val stampService: StampService) {
    suspend fun getMyStamps(): ApiResponse<List<StampSummaryResponse>> = stampService.getMyStamps()
    suspend fun getStoreStamp(storeId: Long): ApiResponse<StampDetailResponse> = stampService.getStoreStamp(storeId)
}
