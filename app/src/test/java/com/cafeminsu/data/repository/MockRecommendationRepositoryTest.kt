package com.cafeminsu.data.repository

import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.data.mock.MockData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockRecommendationRepositoryTest {
    @Test
    fun emitsFirstAvailableMockMenuAsRecommendation() = runBlocking {
        val repository = MockRecommendationRepository()

        repository.observeTodayRecommendation().test {
            val result = awaitItem()

            assertTrue(result is AppResult.Success)
            val menu = (result as AppResult.Success).data
            val expected = MockData.menuItems.firstOrNull { !it.isSoldOut }
            assertEquals(expected, menu)
            assertEquals(false, menu?.isSoldOut)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
