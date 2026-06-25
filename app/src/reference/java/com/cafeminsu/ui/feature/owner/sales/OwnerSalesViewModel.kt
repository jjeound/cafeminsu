package com.cafeminsu.ui.feature.owner.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.SalesPeriod
import com.cafeminsu.domain.model.SalesSummary
import com.cafeminsu.domain.model.TopMenu
import com.cafeminsu.domain.repository.SalesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OwnerSalesViewModel @Inject constructor(
    private val salesRepository: SalesRepository,
) : ViewModel() {
    private val selectedPeriod = MutableStateFlow(SalesPeriod.Week)
    private val refreshTrigger = MutableStateFlow(0)

    val uiState: StateFlow<OwnerSalesUiState> = combine(
        selectedPeriod,
        refreshTrigger,
    ) { period, _ -> period }
        .flatMapLatest { period ->
            salesRepository.observeSales(period).map { salesResult ->
                mapOwnerSalesState(
                    salesResult = salesResult,
                    selectedPeriod = period,
                )
            }
        }
        .catch {
            emit(
                OwnerSalesUiState.Error(
                    message = "매출 정보를 불러오지 못했어요",
                    retryable = true,
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(StateStopTimeoutMillis),
            initialValue = OwnerSalesUiState.Loading,
        )

    fun selectPeriod(period: SalesPeriod) {
        selectedPeriod.value = period
    }

    fun retry() {
        refreshTrigger.value = refreshTrigger.value + 1
    }

    private fun mapOwnerSalesState(
        salesResult: AppResult<SalesSummary>,
        selectedPeriod: SalesPeriod,
    ): OwnerSalesUiState {
        val periods = selectedPeriod.toPeriodUiModels()
        val summary = when (salesResult) {
            is AppResult.Success -> salesResult.data
            is AppResult.Failure -> return salesResult.error.toOwnerSalesError()
        }

        return if (summary.hasNoSalesData()) {
            OwnerSalesUiState.Empty(
                selectedPeriod = selectedPeriod,
                periods = periods,
                message = "매출 데이터가 없어요",
            )
        } else {
            OwnerSalesUiState.Content(
                selectedPeriod = selectedPeriod,
                periods = periods,
                summary = summary.toOwnerSalesSummaryUiModel(),
            )
        }
    }

    private fun SalesPeriod.toPeriodUiModels(): List<OwnerSalesPeriodUiModel> =
        SalesPeriod.entries.map { period ->
            OwnerSalesPeriodUiModel(
                period = period,
                label = period.ownerSalesLabel,
                selected = period == this,
            )
        }

    private fun SalesSummary.hasNoSalesData(): Boolean =
        totalSales == 0 && dailySales.all { it == 0 } && topMenus.isEmpty()

    private fun SalesSummary.toOwnerSalesSummaryUiModel(): OwnerSalesSummaryUiModel =
        OwnerSalesSummaryUiModel(
            periodSalesLabel = period.salesLabel(),
            totalSalesLabel = totalSales.toWonLabel(),
            deltaLabel = period.deltaLabel(deltaPercent),
            deltaTone = deltaPercent.toDeltaTone(),
            bars = dailySales.toSalesBars(),
            topMenus = topMenus.map { it.toOwnerSalesTopMenuUiModel() },
            payoutAmountLabel = payoutAmount.toWonLabel(),
            payoutDateLabel = payoutDateLabel.orEmpty(),
        )

    private fun SalesPeriod.salesLabel(): String =
        when (this) {
            SalesPeriod.Today -> "오늘 매출"
            SalesPeriod.Week -> "이번 주 매출"
            SalesPeriod.Month -> "이번 달 매출"
        }

    private fun SalesPeriod.deltaCompareLabel(): String =
        when (this) {
            SalesPeriod.Today -> "어제 대비"
            SalesPeriod.Week -> "지난주 대비"
            SalesPeriod.Month -> "지난달 대비"
        }

    private fun SalesPeriod.deltaLabel(deltaPercent: Int?): String {
        if (deltaPercent == null) return "변동 정보 없음"

        val prefix = when {
            deltaPercent > 0 -> "▲"
            deltaPercent < 0 -> "▼"
            else -> ""
        }
        val percent = kotlin.math.abs(deltaPercent)
        return listOf(prefix, "$percent%")
            .filter { it.isNotBlank() }
            .joinToString(separator = " ") + " ${deltaCompareLabel()}"
    }

    private fun Int?.toDeltaTone(): OwnerSalesDeltaTone =
        when {
            this == null || this == 0 -> OwnerSalesDeltaTone.Neutral
            this > 0 -> OwnerSalesDeltaTone.Positive
            else -> OwnerSalesDeltaTone.Negative
        }

    private fun List<Int>.toSalesBars(): List<OwnerSalesBarUiModel> {
        val maxSales = maxOrNull()?.takeIf { it > 0 } ?: 0

        return DayLabels.mapIndexed { index, label ->
            val amount = getOrNull(index) ?: 0
            OwnerSalesBarUiModel(
                label = label,
                ratio = if (maxSales == 0) 0f else amount.toFloat() / maxSales,
                highlighted = maxSales > 0 && amount == maxSales,
            )
        }
    }

    private fun TopMenu.toOwnerSalesTopMenuUiModel(): OwnerSalesTopMenuUiModel =
        OwnerSalesTopMenuUiModel(
            rankLabel = rank.toString(),
            name = name,
            soldCountLabel = "${soldCount}잔",
            salesLabel = sales.toWonLabel(),
        )

    private fun DomainError.toOwnerSalesError(): OwnerSalesUiState.Error =
        OwnerSalesUiState.Error(
            message = toOwnerSalesMessage(),
            retryable = isRetryable(),
        )

    private fun DomainError.toOwnerSalesMessage(): String =
        when (this) {
            DomainError.Network -> "네트워크 연결을 확인하고 다시 시도해 주세요"
            DomainError.Timeout -> "응답이 지연되고 있어요. 잠시 후 다시 시도해 주세요"
            DomainError.Unauthorized -> "점주 로그인이 만료됐어요. 다시 로그인해 주세요"
            DomainError.NotFound -> "매출 정보를 찾지 못했어요"
            is DomainError.Payment -> "정산 정보를 확인하지 못했어요"
            is DomainError.Validation -> "매출 조회 기간을 확인해 주세요"
            DomainError.Unknown -> "매출 정보를 불러오지 못했어요"
        }

    private fun DomainError.isRetryable(): Boolean =
        when (this) {
            DomainError.Network,
            DomainError.Timeout,
            DomainError.Unknown,
            -> true

            DomainError.Unauthorized,
            DomainError.NotFound,
            is DomainError.Payment,
            is DomainError.Validation,
            -> false
        }

    private fun Int.toWonLabel(): String =
        "₩${NumberFormat.getNumberInstance(Locale.KOREA).format(this)}"

    private companion object {
        const val StateStopTimeoutMillis = 5_000L
        val DayLabels = listOf("일", "월", "화", "수", "목", "금", "토")
    }
}
