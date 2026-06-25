package com.ssafy.cafeminsu.core.data.repository.store

import com.ssafy.cafeminsu.core.model.store.NearbyStoreSummary
import com.ssafy.cafeminsu.core.model.store.StoreDetail
import com.ssafy.cafeminsu.core.model.store.StoreSummary
import kotlinx.coroutines.flow.Flow

interface StoreRepository {
    fun getStores(
        query: String = "",
        page: Int = 0,
        size: Int = 20,
    ): Flow<List<StoreSummary>>

    fun getStore(storeId: Long): Flow<StoreDetail?>

    fun getNearbyStores(
        latitude: Double,
        longitude: Double,
        radius: Double = 1_000.0,
    ): Flow<List<NearbyStoreSummary>>

    suspend fun syncStores(
        query: String = "",
        page: Int = 0,
        size: Int = 20,
    )

    suspend fun syncStore(storeId: Long)
}
