package com.ssafy.cafeminsu.core.data.repository.store

import com.ssafy.cafeminsu.core.common.network.CafeMinsuDispatcher
import com.ssafy.cafeminsu.core.common.network.Dispatcher
import com.ssafy.cafeminsu.core.data.model.asEntity
import com.ssafy.cafeminsu.core.data.model.asExternalModel
import com.ssafy.cafeminsu.core.database.dao.StoreDao
import com.ssafy.cafeminsu.core.model.store.NearbyStoreSummary
import com.ssafy.cafeminsu.core.model.store.StoreDetail
import com.ssafy.cafeminsu.core.model.store.StoreSummary
import com.ssafy.cafeminsu.core.network.client.StoreClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DefaultStoreRepository @Inject constructor(
    private val client: StoreClient,
    private val storeDao: StoreDao,
    @Dispatcher(CafeMinsuDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : StoreRepository {
    override fun getStores(
        query: String,
        page: Int,
        size: Int,
    ): Flow<List<StoreSummary>> {
        val normalizedQuery = query.normalizedSearchQuery()

        return storeDao.getStoreEntities(
            query = normalizedQuery,
            page = page,
        ).map { storeEntities ->
            storeEntities.map { it.asExternalModel() }
        }.flowOn(ioDispatcher)
    }

    override fun getStore(storeId: Long): Flow<StoreDetail?> =
        storeDao.getStoreDetailEntity(storeId)
            .map { it?.asExternalModel() }
            .flowOn(ioDispatcher)

    override fun getNearbyStores(
        latitude: Double,
        longitude: Double,
        radius: Double,
    ): Flow<List<NearbyStoreSummary>> = flow {
        emit(client.getNearbyStores(latitude, longitude, radius).map { it.asExternalModel() })
    }.flowOn(ioDispatcher)

    override suspend fun syncStores(
        query: String,
        page: Int,
        size: Int,
    ) {
        withContext(ioDispatcher) {
            val normalizedQuery = query.normalizedSearchQuery()
            val keyword = normalizedQuery.ifBlank { null }
            val response = client.searchStores(keyword, page, size)

            storeDao.replaceStoreSearchResults(
                query = normalizedQuery,
                page = page,
                storeEntities = response.stores.map { it.asEntity() },
                storeSearchEntities = response.stores.mapIndexed { index, store ->
                    store.asEntity(
                        query = normalizedQuery,
                        page = page,
                        position = index,
                    )
                },
            )
        }
    }

    override suspend fun syncStore(storeId: Long) {
        withContext(ioDispatcher) {
            val response = client.getStore(storeId)
            storeDao.upsertStoreDetailEntity(response.asEntity())
        }
    }

    private fun String.normalizedSearchQuery(): String = trim()
}
