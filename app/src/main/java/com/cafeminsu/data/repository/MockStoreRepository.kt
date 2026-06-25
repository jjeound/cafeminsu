package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.mock.MockData
import com.cafeminsu.domain.model.Store
import com.cafeminsu.domain.repository.CartRepository
import com.cafeminsu.domain.repository.StoreRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

@Singleton
class MockStoreRepository(
    stores: List<Store>,
    private val cartRepository: CartRepository,
) : StoreRepository {
    @Inject
    constructor(cartRepository: CartRepository) : this(
        stores = MockData.nearbyStores,
        cartRepository = cartRepository,
    )

    private val storeState = MutableStateFlow(stores.sortedBy { it.distanceMeters })
    private val selectedStoreState = MutableStateFlow<Store?>(null)

    override fun observeNearbyStores(query: String?): Flow<AppResult<List<Store>>> =
        storeState.map { stores ->
            AppResult.Success(
                stores
                    .filter { store -> store.matches(query) }
                    .sortedBy { store -> store.distanceMeters },
            )
        }

    override suspend fun getStore(storeId: String): AppResult<Store> {
        val store = storeState.value.firstOrNull { it.id == storeId }
        return if (store == null) {
            AppResult.Failure(DomainError.NotFound)
        } else {
            AppResult.Success(store)
        }
    }

    override suspend fun selectStore(storeId: String): AppResult<Unit> =
        when (val result = getStore(storeId)) {
            is AppResult.Success -> {
                // 매장별 메뉴가 다르므로 다른 매장으로 바꾸면 기존 장바구니를 비운다.
                val previous = selectedStoreState.value
                if (previous != null && previous.id != result.data.id) {
                    cartRepository.clear()
                }
                selectedStoreState.value = result.data
                AppResult.Success(Unit)
            }

            is AppResult.Failure -> result
        }

    override fun observeSelectedStore(): Flow<Store?> = selectedStoreState

    private fun Store.matches(query: String?): Boolean {
        val normalizedQuery = query?.trim()?.lowercase().orEmpty()
        return normalizedQuery.isBlank() ||
            name.lowercase().contains(normalizedQuery) ||
            address.lowercase().contains(normalizedQuery)
    }
}
