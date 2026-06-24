package com.cafeminsu.core.network.client

import com.cafeminsu.core.network.model.request.store.StoreCreateRequest
import com.cafeminsu.core.network.model.request.store.StoreUpdateRequest
import com.cafeminsu.core.network.model.response.store.NearbyStoreResponse
import com.cafeminsu.core.network.model.response.store.OwnerStoreResponse
import com.cafeminsu.core.network.model.response.store.StoreCreateResponse
import com.cafeminsu.core.network.model.response.store.StoreDetailResponse
import com.cafeminsu.core.network.model.response.store.StoreSearchResponse
import com.cafeminsu.core.network.service.StoreService
import javax.inject.Inject

class StoreClient @Inject constructor(private val storeService: StoreService) {
    suspend fun searchStores(
        keyword: String? = null,
        page: Int = 0,
        size: Int = 20,
    ): StoreSearchResponse = storeService.searchStores(keyword, page, size)

    suspend fun getStore(storeId: Long): StoreDetailResponse = storeService.getStore(storeId)

    suspend fun getNearbyStores(
        latitude: Double,
        longitude: Double,
        radius: Double = 1_000.0,
    ): List<NearbyStoreResponse> = storeService.getNearbyStores(latitude, longitude, radius)

    suspend fun getMyStores(): List<OwnerStoreResponse> = storeService.getMyStores()

    suspend fun createStore(request: StoreCreateRequest): StoreCreateResponse =
        storeService.createStore(request)

    suspend fun updateStore(storeId: Long, request: StoreUpdateRequest) {
        storeService.updateStore(storeId, request)
    }

    suspend fun deleteStore(storeId: Long) {
        storeService.deleteStore(storeId)
    }
}
