package com.cafeminsu.core.data.repository.store

import com.cafeminsu.core.model.store.NearbyStoreSummary
import com.cafeminsu.core.model.store.StoreDetail
import com.cafeminsu.core.model.store.StoreSummary
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
}
