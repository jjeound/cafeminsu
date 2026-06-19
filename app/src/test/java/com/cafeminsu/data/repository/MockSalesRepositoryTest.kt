package com.cafeminsu.data.repository

import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.SalesPeriod
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockSalesRepositoryTest {
    @Test
    fun weekSeedMatchesOwnerSalesDesignNumbers() = runTest {
        val repository = MockSalesRepository()

        repository.observeSales(SalesPeriod.Week).test {
            val result = awaitItem()

            assertTrue(result is AppResult.Success)
            val summary = (result as AppResult.Success).data
            assertEquals(SalesPeriod.Week, summary.period)
            assertEquals(2_840_000, summary.totalSales)
            assertEquals(12, summary.deltaPercent)
            assertEquals(7, summary.dailySales.size)
            assertEquals("아메리카노", summary.topMenus.first().name)
            assertEquals(2_556_000, summary.payoutAmount)
            assertEquals("6월 24일 입금 예정", summary.payoutDateLabel)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
