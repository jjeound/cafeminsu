package com.cafeminsu.core.network.client

import com.cafeminsu.core.network.model.response.store.NearbyStoreResponse
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
}
