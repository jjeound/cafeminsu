package com.cafeminsu.core.network.service

import com.cafeminsu.core.network.model.response.reward.StampDetailResponse
import com.cafeminsu.core.network.model.response.reward.StampSummaryResponse
import com.skydoves.sandwich.ApiResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface StampService {
    @GET("api/stamps") suspend fun getMyStamps(): ApiResponse<List<StampSummaryResponse>>
    @GET("api/stamps/{storeId}") suspend fun getStoreStamp(@Path("storeId") storeId: Long): ApiResponse<StampDetailResponse>
}
