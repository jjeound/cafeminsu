package com.ssafy.cafeminsu.core.network.service

import com.ssafy.cafeminsu.core.network.model.response.store.NearbyStoreResponse
import com.ssafy.cafeminsu.core.network.model.response.store.StoreDetailResponse
import com.ssafy.cafeminsu.core.network.model.response.store.StoreSearchResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface StoreService {
    @GET("api/stores")
    suspend fun searchStores(
        @Query("keyword") keyword: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): StoreSearchResponse

    @GET("api/stores/{storeId}")
    suspend fun getStore(@Path("storeId") storeId: Long): StoreDetailResponse

    @GET("api/stores/nearby")
    suspend fun getNearbyStores(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radius") radius: Double = 1_000.0,
    ): List<NearbyStoreResponse>
}
