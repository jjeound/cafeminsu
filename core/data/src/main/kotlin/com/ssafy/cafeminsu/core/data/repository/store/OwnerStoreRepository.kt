package com.ssafy.cafeminsu.core.data.repository.store

import com.ssafy.cafeminsu.core.model.store.OwnerStoreSummary
import com.ssafy.cafeminsu.core.network.model.request.store.StoreCreateRequest
import com.ssafy.cafeminsu.core.network.model.request.store.StoreUpdateRequest
import kotlinx.coroutines.flow.Flow

interface OwnerStoreRepository {
    fun getMyStores(): Flow<List<OwnerStoreSummary>>

    fun createStore(request: StoreCreateRequest): Flow<Long>

    fun updateStore(storeId: Long, request: StoreUpdateRequest): Flow<Unit>

    fun deleteStore(storeId: Long): Flow<Unit>
}
