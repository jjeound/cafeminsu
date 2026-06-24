package com.cafeminsu.core.data.repository.store

import com.cafeminsu.core.model.store.NearbyStoreSummary
import com.cafeminsu.core.model.store.OwnerStoreSummary
import com.cafeminsu.core.model.store.StoreDetail
import com.cafeminsu.core.model.store.StoreSummary
import com.cafeminsu.core.network.model.request.store.StoreCreateRequest
import com.cafeminsu.core.network.model.request.store.StoreUpdateRequest
import kotlinx.coroutines.flow.Flow

interface StoreRepository {
    fun getStores(
        query: String = "",
        page: Int = 0,
        size: Int = 20,
    ): Flow<List<StoreSummary>>

    fun getStore(storeId: Long): Flow<StoreDetail>

    fun getNearbyStores(
        latitude: Double,
        longitude: Double,
        radius: Double = 1_000.0,
    ): Flow<List<NearbyStoreSummary>>

    fun getMyStores(): Flow<List<OwnerStoreSummary>>

    fun createStore(request: StoreCreateRequest): Flow<Long>

    fun updateStore(storeId: Long, request: StoreUpdateRequest): Flow<Unit>

    fun deleteStore(storeId: Long): Flow<Unit>
}
