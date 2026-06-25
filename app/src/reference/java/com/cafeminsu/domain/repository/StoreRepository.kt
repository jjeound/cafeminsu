package com.cafeminsu.domain.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.Store
import kotlinx.coroutines.flow.Flow

interface StoreRepository {
    fun observeNearbyStores(query: String? = null): Flow<AppResult<List<Store>>>
    suspend fun getStore(storeId: String): AppResult<Store>
    suspend fun selectStore(storeId: String): AppResult<Unit>
    fun observeSelectedStore(): Flow<Store?>
}
