package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.mapper.toStore
import com.cafeminsu.data.mapper.toStores
import com.cafeminsu.data.remote.DefaultStorePage
import com.cafeminsu.data.remote.DefaultStorePageSize
import com.cafeminsu.data.remote.StoreApi
import com.cafeminsu.data.remote.Unauthenticated
import com.cafeminsu.data.remote.runCatchingToAppResult
import com.cafeminsu.data.remote.unwrap
import com.cafeminsu.di.IoDispatcher
import com.cafeminsu.domain.model.Store
import com.cafeminsu.domain.repository.StoreRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

@Singleton
class RealStoreRepository @Inject constructor(
    @Unauthenticated
    private val storeApi: StoreApi,
    private val selectedStoreHolder: SelectedStoreHolder,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : StoreRepository {
    override fun observeNearbyStores(query: String?): Flow<AppResult<List<Store>>> =
        flow {
            emit(fetchStores(query))
        }.flowOn(ioDispatcher)

    override suspend fun getStore(storeId: String): AppResult<Store> =
        withContext(ioDispatcher) {
            val serverId = storeId.toLongOrNull()
                ?: return@withContext AppResult.Failure(DomainError.NotFound)

            when (
                val response = runCatchingToAppResult {
                    storeApi.getStore(serverId)
                }
            ) {
                is AppResult.Success -> response.data.unwrap { it.toStore() }
                is AppResult.Failure -> response
            }
        }

    override suspend fun selectStore(storeId: String): AppResult<Unit> =
        when (val result = getStore(storeId)) {
            is AppResult.Success -> {
                selectedStoreHolder.select(result.data)
                AppResult.Success(Unit)
            }

            is AppResult.Failure -> result
        }

    override fun observeSelectedStore(): Flow<Store?> = selectedStoreHolder.observe()

    private suspend fun fetchStores(query: String?): AppResult<List<Store>> =
        when (
            val response = runCatchingToAppResult {
                storeApi.searchStores(
                    keyword = query.normalizedQuery(),
                    page = DefaultStorePage,
                    size = DefaultStorePageSize,
                )
            }
        ) {
            is AppResult.Success -> response.data.unwrap { it.toStores() }
            is AppResult.Failure -> response
        }

    private fun String?.normalizedQuery(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() }
}
