package com.ssafy.cafeminsu.core.network.client

import com.ssafy.cafeminsu.core.network.model.request.store.StoreCreateRequest
import com.ssafy.cafeminsu.core.network.model.request.store.StoreUpdateRequest
import com.ssafy.cafeminsu.core.network.model.response.store.OwnerStoreResponse
import com.ssafy.cafeminsu.core.network.model.response.store.StoreCreateResponse
import com.ssafy.cafeminsu.core.network.service.OwnerStoreService
import javax.inject.Inject

class OwnerStoreClient @Inject constructor(
    private val ownerStoreService: OwnerStoreService,
) {
    suspend fun getMyStores(): List<OwnerStoreResponse> = ownerStoreService.getMyStores()

    suspend fun createStore(request: StoreCreateRequest): StoreCreateResponse =
        ownerStoreService.createStore(request)

    suspend fun updateStore(storeId: Long, request: StoreUpdateRequest) {
        ownerStoreService.updateStore(storeId, request)
    }

    suspend fun deleteStore(storeId: Long) {
        ownerStoreService.deleteStore(storeId)
    }
}
