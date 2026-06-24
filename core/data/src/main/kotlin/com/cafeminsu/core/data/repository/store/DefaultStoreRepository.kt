package com.cafeminsu.core.data.repository.store

import com.cafeminsu.core.common.network.CafeMinsuDispatcher
import com.cafeminsu.core.common.network.Dispatcher
import com.cafeminsu.core.data.model.asExternalModel
import com.cafeminsu.core.model.store.NearbyStoreSummary
import com.cafeminsu.core.model.store.StoreDetail
import com.cafeminsu.core.model.store.StoreSummary
import com.cafeminsu.core.network.client.StoreClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class DefaultStoreRepository @Inject constructor(
    private val client: StoreClient,
    @Dispatcher(CafeMinsuDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : StoreRepository {
    override fun getStores(
        query: String,
        page: Int,
        size: Int,
    ): Flow<List<StoreSummary>> = flow {
        val keyword = query.trim().ifBlank { null }
        emit(client.searchStores(keyword, page, size).stores.map { it.asExternalModel() })
    }.flowOn(ioDispatcher)

    override fun getStore(storeId: Long): Flow<StoreDetail> = flow {
        emit(client.getStore(storeId).asExternalModel())
    }.flowOn(ioDispatcher)

    override fun getNearbyStores(
        latitude: Double,
        longitude: Double,
        radius: Double,
    ): Flow<List<NearbyStoreSummary>> = flow {
        emit(client.getNearbyStores(latitude, longitude, radius).map { it.asExternalModel() })
    }.flowOn(ioDispatcher)
}
