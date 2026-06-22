package com.cafeminsu.core.data.repository.store

import com.cafeminsu.core.model.store.Store
import kotlinx.coroutines.flow.Flow

interface StoreRepository {
    fun observeNearbyStores(query: String? = null): Flow<List<Store>>

    fun getStore(storeId: String): Flow<Store>

    fun selectStore(storeId: String): Flow<Unit>

    fun observeSelectedStore(): Flow<Store?>
}
