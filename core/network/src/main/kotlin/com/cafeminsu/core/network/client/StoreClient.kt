package com.cafeminsu.core.network.client

import com.cafeminsu.core.network.model.response.store.StoreDetailResponse
import com.cafeminsu.core.network.model.response.store.StoreSearchResponse
import com.cafeminsu.core.network.service.StoreService
import com.skydoves.sandwich.ApiResponse
import javax.inject.Inject

class StoreClient @Inject constructor(private val storeService: StoreService) {
    suspend fun searchStores(keyword: String? = null, page: Int = 0, size: Int = 20): ApiResponse<StoreSearchResponse> = storeService.searchStores(keyword, page, size)
    suspend fun getStore(storeId: Long): ApiResponse<StoreDetailResponse> = storeService.getStore(storeId)
}
