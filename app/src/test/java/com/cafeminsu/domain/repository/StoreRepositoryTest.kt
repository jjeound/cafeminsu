package com.cafeminsu.domain.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.Store
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreRepositoryTest {
    @Test
    fun exposesStoreRepositoryContract() = runBlocking {
        val repository = object : StoreRepository {
            override fun observeNearbyStores(query: String?): Flow<AppResult<List<Store>>> =
                flowOf(AppResult.Success(emptyList()))

            override suspend fun getStore(storeId: String): AppResult<Store> =
                AppResult.Failure(com.cafeminsu.core.DomainError.NotFound)

            override suspend fun selectStore(storeId: String): AppResult<Unit> =
                AppResult.Success(Unit)

            override fun observeSelectedStore(): Flow<Store?> =
                flowOf(null)
        }

        assertTrue(repository.selectStore("gangnam") is AppResult.Success)
    }
}
