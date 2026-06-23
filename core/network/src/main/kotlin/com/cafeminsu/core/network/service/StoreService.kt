package com.cafeminsu.core.network.service

import com.cafeminsu.core.network.model.response.store.StoreDetailResponse
import com.cafeminsu.core.network.model.response.store.StoreSearchResponse
import com.skydoves.sandwich.ApiResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface StoreService {
    @GET("api/stores")
    suspend fun searchStores(@Query("keyword") keyword: String? = null, @Query("page") page: Int = 0, @Query("size") size: Int = 20): ApiResponse<StoreSearchResponse>

    @GET("api/stores/{storeId}")
    suspend fun getStore(@Path("storeId") storeId: Long): ApiResponse<StoreDetailResponse>
}
