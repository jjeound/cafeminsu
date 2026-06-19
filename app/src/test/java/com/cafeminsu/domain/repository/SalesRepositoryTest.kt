package com.cafeminsu.domain.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.SalesPeriod
import com.cafeminsu.domain.model.SalesSummary
import com.cafeminsu.domain.model.TopMenu
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SalesRepositoryTest {
    @Test
    fun observeSalesReturnsSummaryWrappedInAppResult() = runTest {
        val repository: SalesRepository = FakeSalesRepository()

        repository.observeSales(SalesPeriod.Week).collect { result ->
            val summary = (result as AppResult.Success).data

            assertEquals(SalesPeriod.Week, summary.period)
            assertEquals(2_840_000, summary.totalSales)
        }
    }
}

private class FakeSalesRepository : SalesRepository {
    override fun observeSales(period: SalesPeriod): Flow<AppResult<SalesSummary>> =
        flowOf(
            AppResult.Success(
                SalesSummary(
                    period = period,
                    totalSales = 2_840_000,
                    orderCount = 214,
                    deltaPercent = 12,
                    dailySales = listOf(320_000, 410_000, 550_000),
                    topMenus = listOf(
                        TopMenu(rank = 1, name = "아메리카노", soldCount = 142, sales = 639_000),
                    ),
                    payoutAmount = 2_556_000,
                    payoutDateLabel = "6월 24일 입금 예정",
                ),
            ),
        )
}
