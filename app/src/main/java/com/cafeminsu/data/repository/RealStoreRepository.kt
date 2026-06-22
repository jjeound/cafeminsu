package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.local.store.StoreLocalDataSource
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

@Singleton
class RealStoreRepository @Inject constructor(
    @Unauthenticated
    private val storeApi: StoreApi,
    private val selectedStoreHolder: SelectedStoreHolder,
    private val localDataSource: StoreLocalDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : StoreRepository {
    override fun observeNearbyStores(query: String?): Flow<AppResult<List<Store>>> =
        flow {
            when (val result = fetchStores(query)) {
                is AppResult.Success -> {
                    // 네트워크 성공 → 캐시 write-through 후 emit.
                    localDataSource.replaceStores(result.data)
                    emit(result)
                    // 목록 API는 좌표를 주지 않아 지도 마커가 안 찍힌다 → 좌표 없는 매장만 상세에서 받아
                    // 채운 목록을 한 번 더 emit한다(좌표가 실제로 바뀐 경우에만).
                    if (result.data.any { !it.hasCoordinate() }) {
                        val located = result.data.withCoordinates()
                        if (located != result.data) {
                            emit(AppResult.Success(located))
                        }
                    }
                }

                is AppResult.Failure -> {
                    // 네트워크 실패 → 캐시가 있으면 오프라인 폴백, 없으면 실패 그대로.
                    val cached = localDataSource.cachedStores()
                    if (cached.isNotEmpty()) {
                        emit(AppResult.Success(cached))
                    } else {
                        emit(result)
                    }
                }
            }
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

    // 좌표 없는 매장은 상세 API로 좌표만 보강한다. 매장 수가 적어 병렬 조회, 실패한 건은 원본 유지.
    private suspend fun List<Store>.withCoordinates(): List<Store> = coroutineScope {
        map { store ->
            async {
                if (store.hasCoordinate()) {
                    store
                } else {
                    when (val detail = getStore(store.id)) {
                        is AppResult.Success -> store.copy(
                            latitude = detail.data.latitude,
                            longitude = detail.data.longitude,
                        )

                        is AppResult.Failure -> store
                    }
                }
            }
        }.awaitAll()
    }

    private fun Store.hasCoordinate(): Boolean =
        latitude != UnknownCoordinate || longitude != UnknownCoordinate

    private fun String?.normalizedQuery(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() }

    private companion object {
        const val UnknownCoordinate = 0.0
    }
}
