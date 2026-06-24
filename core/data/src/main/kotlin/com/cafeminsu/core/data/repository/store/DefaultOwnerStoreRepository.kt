package com.cafeminsu.core.data.repository.store

import com.cafeminsu.core.common.network.CafeMinsuDispatcher
import com.cafeminsu.core.common.network.Dispatcher
import com.cafeminsu.core.data.model.asExternalModel
import com.cafeminsu.core.model.store.OwnerStoreSummary
import com.cafeminsu.core.network.client.OwnerStoreClient
import com.cafeminsu.core.network.model.request.store.StoreCreateRequest
import com.cafeminsu.core.network.model.request.store.StoreUpdateRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class DefaultOwnerStoreRepository @Inject constructor(
    private val client: OwnerStoreClient,
    @Dispatcher(CafeMinsuDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : OwnerStoreRepository {
    override fun getMyStores(): Flow<List<OwnerStoreSummary>> = flow {
        emit(client.getMyStores().map { it.asExternalModel() })
    }.flowOn(ioDispatcher)

    override fun createStore(request: StoreCreateRequest): Flow<Long> = flow {
        emit(client.createStore(request).storeId)
    }.flowOn(ioDispatcher)

    override fun updateStore(storeId: Long, request: StoreUpdateRequest): Flow<Unit> = flow {
        emit(client.updateStore(storeId, request))
    }.flowOn(ioDispatcher)

    override fun deleteStore(storeId: Long): Flow<Unit> = flow {
        emit(client.deleteStore(storeId))
    }.flowOn(ioDispatcher)
}
