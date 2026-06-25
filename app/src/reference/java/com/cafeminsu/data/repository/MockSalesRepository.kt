package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.SalesPeriod
import com.cafeminsu.domain.model.SalesSummary
import com.cafeminsu.domain.model.TopMenu
import com.cafeminsu.domain.repository.SalesRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

@Singleton
class MockSalesRepository @Inject constructor() : SalesRepository {
    private val sales = MutableStateFlow(ownerSalesSeed)

    override fun observeSales(period: SalesPeriod): Flow<AppResult<SalesSummary>> =
        sales.map { summaries ->
            AppResult.Success(summaries.getValue(period))
        }
}

private val ownerSalesSeed = mapOf(
    SalesPeriod.Today to SalesSummary(
        period = SalesPeriod.Today,
        totalSales = 482_000,
        orderCount = 37,
        deltaPercent = 8,
        dailySales = listOf(58_000, 64_000, 72_000, 83_000, 94_000, 111_000, 0),
        topMenus = listOf(
            TopMenu(rank = 1, name = "아메리카노", soldCount = 24, sales = 108_000),
            TopMenu(rank = 2, name = "카페라떼", soldCount = 16, sales = 80_000),
            TopMenu(rank = 3, name = "바닐라라떼", soldCount = 12, sales = 66_000),
        ),
        payoutAmount = 433_800,
        payoutDateLabel = "6월 24일 입금 예정",
    ),
    SalesPeriod.Week to SalesSummary(
        period = SalesPeriod.Week,
        totalSales = 2_840_000,
        orderCount = 214,
        deltaPercent = 12,
        dailySales = listOf(420_000, 560_000, 380_000, 610_000, 520_000, 780_000, 690_000),
        topMenus = listOf(
            TopMenu(rank = 1, name = "아메리카노", soldCount = 142, sales = 639_000),
            TopMenu(rank = 2, name = "카페라떼", soldCount = 98, sales = 490_000),
            TopMenu(rank = 3, name = "바닐라라떼", soldCount = 61, sales = 335_500),
        ),
        payoutAmount = 2_556_000,
        payoutDateLabel = "6월 24일 입금 예정",
    ),
    SalesPeriod.Month to SalesSummary(
        period = SalesPeriod.Month,
        totalSales = 12_840_000,
        orderCount = 942,
        deltaPercent = -3,
        dailySales = listOf(1_820_000, 1_640_000, 1_910_000, 2_120_000, 1_760_000, 2_240_000, 1_350_000),
        topMenus = listOf(
            TopMenu(rank = 1, name = "아메리카노", soldCount = 638, sales = 2_871_000),
            TopMenu(rank = 2, name = "카페라떼", soldCount = 412, sales = 2_060_000),
            TopMenu(rank = 3, name = "바닐라라떼", soldCount = 288, sales = 1_584_000),
        ),
        payoutAmount = 11_556_000,
        payoutDateLabel = "7월 1일 입금 예정",
    ),
)
