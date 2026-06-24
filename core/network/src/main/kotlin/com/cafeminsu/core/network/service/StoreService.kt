package com.cafeminsu.core.network.service

import com.cafeminsu.core.network.model.request.store.StoreCreateRequest
import com.cafeminsu.core.network.model.request.store.StoreUpdateRequest
import com.cafeminsu.core.network.model.response.store.NearbyStoreResponse
import com.cafeminsu.core.network.model.response.store.OwnerStoreResponse
import com.cafeminsu.core.network.model.response.store.StoreCreateResponse
import com.cafeminsu.core.network.model.response.store.StoreDetailResponse
import com.cafeminsu.core.network.model.response.store.StoreSearchResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.POST
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

    @GET("api/stores/my")
    suspend fun getMyStores(): List<OwnerStoreResponse>

    @POST("api/stores")
    suspend fun createStore(@Body request: StoreCreateRequest): StoreCreateResponse

    @PATCH("api/stores/{storeId}")
    suspend fun updateStore(
        @Path("storeId") storeId: Long,
        @Body request: StoreUpdateRequest,
    )

    @DELETE("api/stores/{storeId}")
    suspend fun deleteStore(@Path("storeId") storeId: Long)
}
