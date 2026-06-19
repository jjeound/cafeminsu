package com.cafeminsu.ui.feature.owner.sales

import com.cafeminsu.domain.model.SalesPeriod

sealed interface OwnerSalesUiState {
    data object Loading : OwnerSalesUiState

    data class Content(
        val selectedPeriod: SalesPeriod,
        val periods: List<OwnerSalesPeriodUiModel>,
        val summary: OwnerSalesSummaryUiModel,
    ) : OwnerSalesUiState

    data class Empty(
        val selectedPeriod: SalesPeriod,
        val periods: List<OwnerSalesPeriodUiModel>,
        val message: String,
    ) : OwnerSalesUiState

    data class Error(
        val message: String,
        val retryable: Boolean,
    ) : OwnerSalesUiState
}

data class OwnerSalesPeriodUiModel(
    val period: SalesPeriod,
    val label: String,
    val selected: Boolean,
)

data class OwnerSalesSummaryUiModel(
    val periodSalesLabel: String,
    val totalSalesLabel: String,
    val deltaLabel: String,
    val deltaTone: OwnerSalesDeltaTone,
    val bars: List<OwnerSalesBarUiModel>,
    val topMenus: List<OwnerSalesTopMenuUiModel>,
    val payoutAmountLabel: String,
    val payoutDateLabel: String,
)

data class OwnerSalesBarUiModel(
    val label: String,
    val ratio: Float,
    val highlighted: Boolean,
)

data class OwnerSalesTopMenuUiModel(
    val rankLabel: String,
    val name: String,
    val soldCountLabel: String,
    val salesLabel: String,
)

enum class OwnerSalesDeltaTone {
    Positive,
    Negative,
    Neutral,
}

val SalesPeriod.ownerSalesLabel: String
    get() = when (this) {
        SalesPeriod.Today -> "오늘"
        SalesPeriod.Week -> "이번 주"
        SalesPeriod.Month -> "이번 달"
    }
