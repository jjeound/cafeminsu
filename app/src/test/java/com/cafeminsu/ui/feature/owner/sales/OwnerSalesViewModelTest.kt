package com.cafeminsu.ui.feature.owner.sales

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.SalesPeriod
import com.cafeminsu.domain.model.SalesSummary
import com.cafeminsu.domain.model.TopMenu
import com.cafeminsu.domain.repository.SalesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class OwnerSalesViewModelTest {
    @get:Rule
    val mainDispatcherRule: TestWatcher = OwnerSalesMainDispatcherRule()

    @Test
    fun defaultStateShowsWeekSalesSummary() = runTest {
        val repository = FakeSalesRepository()
        val viewModel = OwnerSalesViewModel(salesRepository = repository)

        viewModel.uiState.test {
            val content = awaitContent()

            assertEquals(SalesPeriod.Week, content.selectedPeriod)
            assertEquals(listOf("오늘", "이번 주", "이번 달"), content.periods.map { it.label })
            assertEquals("이번 주 매출", content.summary.periodSalesLabel)
            assertEquals("₩2,840,000", content.summary.totalSalesLabel)
            assertEquals("▲ 12% 지난주 대비", content.summary.deltaLabel)
            assertEquals(OwnerSalesDeltaTone.Positive, content.summary.deltaTone)
            assertEquals(listOf(SalesPeriod.Week), repository.observedPeriods)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun selectingPeriodRefreshesSummary() = runTest {
        val repository = FakeSalesRepository()
        val viewModel = OwnerSalesViewModel(salesRepository = repository)

        viewModel.uiState.test {
            awaitContent()

            viewModel.selectPeriod(SalesPeriod.Today)
            val today = awaitContent()

            assertEquals(SalesPeriod.Today, today.selectedPeriod)
            assertEquals("오늘 매출", today.summary.periodSalesLabel)
            assertEquals("₩482,000", today.summary.totalSalesLabel)
            assertEquals(listOf(SalesPeriod.Week, SalesPeriod.Today), repository.observedPeriods)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun dailySalesMapToBarRatiosAndHighestHighlight() = runTest {
        val viewModel = OwnerSalesViewModel(
            salesRepository = FakeSalesRepository(
                initialResults = mapOf(
                    SalesPeriod.Week to AppResult.Success(
                        sampleSummary(
                            dailySales = listOf(0, 500, 1_000, 250, 750, 1_000, 125),
                        ),
                    ),
                ),
            ),
        )

        viewModel.uiState.test {
            val content = awaitContent()

            assertEquals(listOf("일", "월", "화", "수", "목", "금", "토"), content.summary.bars.map { it.label })
            assertEquals(0f, content.summary.bars[0].ratio)
            assertEquals(0.5f, content.summary.bars[1].ratio)
            assertEquals(1f, content.summary.bars[2].ratio)
            assertTrue(content.summary.bars[2].highlighted)
            assertTrue(content.summary.bars[5].highlighted)
            assertFalse(content.summary.bars[1].highlighted)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun negativeDeltaUsesDownLabelAndErrorTone() = runTest {
        val viewModel = OwnerSalesViewModel(
            salesRepository = FakeSalesRepository(
                initialResults = mapOf(
                    SalesPeriod.Week to AppResult.Success(sampleSummary(deltaPercent = -5)),
                ),
            ),
        )

        viewModel.uiState.test {
            val content = awaitContent()

            assertEquals("▼ 5% 지난주 대비", content.summary.deltaLabel)
            assertEquals(OwnerSalesDeltaTone.Negative, content.summary.deltaTone)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun topMenusMapRankSoldCountAndWonLabels() = runTest {
        val viewModel = OwnerSalesViewModel(
            salesRepository = FakeSalesRepository(
                initialResults = mapOf(
                    SalesPeriod.Week to AppResult.Success(
                        sampleSummary(
                            topMenus = listOf(
                                TopMenu(rank = 1, name = "아메리카노", soldCount = 142, sales = 639_000),
                                TopMenu(rank = 2, name = "카페라떼", soldCount = 98, sales = 490_000),
                            ),
                        ),
                    ),
                ),
            ),
        )

        viewModel.uiState.test {
            val content = awaitContent()

            assertEquals(listOf("1", "2"), content.summary.topMenus.map { it.rankLabel })
            assertEquals(listOf("142잔", "98잔"), content.summary.topMenus.map { it.soldCountLabel })
            assertEquals(listOf("₩639,000", "₩490,000"), content.summary.topMenus.map { it.salesLabel })

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun noSalesDataProducesEmptyState() = runTest {
        val viewModel = OwnerSalesViewModel(
            salesRepository = FakeSalesRepository(
                initialResults = mapOf(
                    SalesPeriod.Week to AppResult.Success(
                        sampleSummary(
                            totalSales = 0,
                            dailySales = List(7) { 0 },
                            topMenus = emptyList(),
                        ),
                    ),
                ),
            ),
        )

        viewModel.uiState.test {
            val empty = awaitEmpty()

            assertEquals(SalesPeriod.Week, empty.selectedPeriod)
            assertEquals("매출 데이터가 없어요", empty.message)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun repositoryFailureProducesErrorState() = runTest {
        val viewModel = OwnerSalesViewModel(
            salesRepository = FakeSalesRepository(
                initialResults = mapOf(SalesPeriod.Week to AppResult.Failure(DomainError.Network)),
            ),
        )

        viewModel.uiState.test {
            val error = awaitErrorState()

            assertEquals("네트워크 연결을 확인하고 다시 시도해 주세요", error.message)
            assertTrue(error.retryable)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun retryReobservesSelectedPeriod() = runTest {
        val repository = FakeSalesRepository(
            initialResults = mapOf(SalesPeriod.Week to AppResult.Failure(DomainError.Network)),
        )
        val viewModel = OwnerSalesViewModel(salesRepository = repository)

        viewModel.uiState.test {
            awaitErrorState()

            viewModel.retry()
            runCurrent()

            assertEquals(listOf(SalesPeriod.Week, SalesPeriod.Week), repository.observedPeriods)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun ReceiveTurbine<OwnerSalesUiState>.awaitContent(): OwnerSalesUiState.Content {
        while (true) {
            when (val state = awaitItem()) {
                is OwnerSalesUiState.Content -> return state
                OwnerSalesUiState.Loading -> Unit
                is OwnerSalesUiState.Empty -> error("Expected content but was $state")
                is OwnerSalesUiState.Error -> error("Expected content but was $state")
            }
        }
    }

    private suspend fun ReceiveTurbine<OwnerSalesUiState>.awaitEmpty(): OwnerSalesUiState.Empty {
        while (true) {
            when (val state = awaitItem()) {
                is OwnerSalesUiState.Empty -> return state
                OwnerSalesUiState.Loading -> Unit
                is OwnerSalesUiState.Content -> error("Expected empty but was $state")
                is OwnerSalesUiState.Error -> error("Expected empty but was $state")
            }
        }
    }

    private suspend fun ReceiveTurbine<OwnerSalesUiState>.awaitErrorState(): OwnerSalesUiState.Error {
        while (true) {
            when (val state = awaitItem()) {
                is OwnerSalesUiState.Error -> return state
                OwnerSalesUiState.Loading,
                is OwnerSalesUiState.Content,
                is OwnerSalesUiState.Empty,
                -> Unit
            }
        }
    }
}

private class FakeSalesRepository(
    initialResults: Map<SalesPeriod, AppResult<SalesSummary>> = mapOf(
        SalesPeriod.Today to AppResult.Success(sampleSummary(period = SalesPeriod.Today, totalSales = 482_000)),
        SalesPeriod.Week to AppResult.Success(sampleSummary()),
        SalesPeriod.Month to AppResult.Success(sampleSummary(period = SalesPeriod.Month, totalSales = 12_840_000)),
    ),
) : SalesRepository {
    private val results = MutableStateFlow(initialResults)
    val observedPeriods = mutableListOf<SalesPeriod>()

    override fun observeSales(period: SalesPeriod): Flow<AppResult<SalesSummary>> {
        observedPeriods += period
        return results.map { summaries ->
            summaries[period] ?: AppResult.Failure(DomainError.NotFound)
        }
    }
}

private fun sampleSummary(
    period: SalesPeriod = SalesPeriod.Week,
    totalSales: Int = 2_840_000,
    deltaPercent: Int? = 12,
    dailySales: List<Int> = listOf(420_000, 560_000, 380_000, 610_000, 520_000, 780_000, 690_000),
    topMenus: List<TopMenu> = listOf(
        TopMenu(rank = 1, name = "아메리카노", soldCount = 142, sales = 639_000),
        TopMenu(rank = 2, name = "카페라떼", soldCount = 98, sales = 490_000),
        TopMenu(rank = 3, name = "바닐라라떼", soldCount = 61, sales = 335_500),
    ),
): SalesSummary =
    SalesSummary(
        period = period,
        totalSales = totalSales,
        orderCount = 214,
        deltaPercent = deltaPercent,
        dailySales = dailySales,
        topMenus = topMenus,
        payoutAmount = 2_556_000,
        payoutDateLabel = "6월 24일 입금 예정",
    )

@OptIn(ExperimentalCoroutinesApi::class)
private class OwnerSalesMainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
