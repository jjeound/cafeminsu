package com.cafeminsu.data.repository

import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockStoreRepositoryTest {
    @Test
    fun observeNearbyStoresEmitsDistanceSortedStores() = runBlocking {
        val repository = MockStoreRepository()

        repository.observeNearbyStores().test {
            val stores = awaitItem().successData()

            assertTrue(stores.isNotEmpty())
            assertEquals(stores.sortedBy { it.distanceMeters }, stores)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeNearbyStoresFiltersByQueryAcrossNameAndAddress() = runBlocking {
        val repository = MockStoreRepository()

        repository.observeNearbyStores("역삼").test {
            val stores = awaitItem().successData()

            assertEquals(listOf("yeoksam"), stores.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun selectStoreUpdatesObservedSelectedStore() = runBlocking {
        val repository = MockStoreRepository()

        repository.observeSelectedStore().test {
            assertNull(awaitItem())

            assertTrue(repository.selectStore("gangnam") is AppResult.Success)
            assertEquals("gangnam", awaitItem()?.id)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getStoreReturnsNotFoundForMissingStore() = runBlocking {
        val repository = MockStoreRepository()

        val result = repository.getStore("missing")

        assertEquals(AppResult.Failure(DomainError.NotFound), result)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> AppResult<T>.successData(): T {
        assertTrue(this is AppResult.Success<*>)
        return (this as AppResult.Success<T>).data
    }
}
