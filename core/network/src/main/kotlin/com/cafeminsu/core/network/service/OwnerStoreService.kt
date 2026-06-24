package com.cafeminsu.core.network.service

import com.cafeminsu.core.network.model.request.store.StoreCreateRequest
import com.cafeminsu.core.network.model.request.store.StoreUpdateRequest
import com.cafeminsu.core.network.model.response.store.OwnerStoreResponse
import com.cafeminsu.core.network.model.response.store.StoreCreateResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.POST

interface OwnerStoreService {
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
