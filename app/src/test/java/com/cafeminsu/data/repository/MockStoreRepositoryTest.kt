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
        val repository = MockStoreRepository(RecordingCartRepository())

        repository.observeNearbyStores().test {
            val stores = awaitItem().successData()

            assertTrue(stores.isNotEmpty())
            assertEquals(stores.sortedBy { it.distanceMeters }, stores)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeNearbyStoresFiltersByQueryAcrossNameAndAddress() = runBlocking {
        val repository = MockStoreRepository(RecordingCartRepository())

        repository.observeNearbyStores("역삼").test {
            val stores = awaitItem().successData()

            assertEquals(listOf("yeoksam"), stores.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun selectStoreUpdatesObservedSelectedStore() = runBlocking {
        val repository = MockStoreRepository(RecordingCartRepository())

        repository.observeSelectedStore().test {
            assertNull(awaitItem())

            assertTrue(repository.selectStore("gangnam") is AppResult.Success)
            assertEquals("gangnam", awaitItem()?.id)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun selectStoreClearsCartOnlyWhenSwitchingToDifferentStore() = runBlocking {
        val cart = RecordingCartRepository()
        val repository = MockStoreRepository(cart)

        // 첫 선택: 이전 매장이 없으므로 장바구니를 비우지 않는다.
        assertTrue(repository.selectStore("gangnam") is AppResult.Success)
        assertEquals(0, cart.clearCount)

        // 다른 매장으로 전환: 매장별 메뉴가 달라 장바구니를 초기화한다.
        assertTrue(repository.selectStore("yeoksam") is AppResult.Success)
        assertEquals(1, cart.clearCount)

        // 같은 매장 재선택: 장바구니를 유지한다.
        assertTrue(repository.selectStore("yeoksam") is AppResult.Success)
        assertEquals(1, cart.clearCount)
    }

    @Test
    fun getStoreReturnsNotFoundForMissingStore() = runBlocking {
        val repository = MockStoreRepository(RecordingCartRepository())

        val result = repository.getStore("missing")

        assertEquals(AppResult.Failure(DomainError.NotFound), result)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> AppResult<T>.successData(): T {
        assertTrue(this is AppResult.Success<*>)
        return (this as AppResult.Success<T>).data
    }
}
