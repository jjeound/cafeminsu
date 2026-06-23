package com.cafeminsu.core.data.repository.store

import com.cafeminsu.core.model.store.StoreDetail
import com.cafeminsu.core.model.store.StoreSummary
import kotlinx.coroutines.flow.Flow

interface StoreRepository {
    fun observeStores(query: String, page: Int): Flow<List<StoreSummary>>

    fun getStore(storeId: Long): Flow<StoreDetail>

    fun refreshStores(query: String, page: Int): Flow<Unit>
}
