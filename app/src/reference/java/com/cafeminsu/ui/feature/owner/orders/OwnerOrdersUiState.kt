package com.cafeminsu.ui.feature.owner.orders

import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.domain.scheduling.SchedulingBadge

sealed interface OwnerOrdersUiState {
    data object Loading : OwnerOrdersUiState

    data class Content(
        val selectedFilter: OwnerOrdersFilter,
        val counts: OwnerOrdersCountsUiModel,
        val orders: List<OwnerOrdersOrderUiModel>,
    ) : OwnerOrdersUiState

    data class Empty(
        val selectedFilter: OwnerOrdersFilter,
        val counts: OwnerOrdersCountsUiModel,
        val message: String,
    ) : OwnerOrdersUiState

    data class Error(
        val message: String,
        val retryable: Boolean,
    ) : OwnerOrdersUiState
}

enum class OwnerOrdersFilter(
    val status: OrderStatus,
) {
    New(OrderStatus.Accepted),
    Preparing(OrderStatus.Preparing),
    Ready(OrderStatus.Ready),
}

data class OwnerOrdersCountsUiModel(
    val newCount: Int,
    val preparingCount: Int,
    val readyCount: Int,
)

data class OwnerOrdersOrderUiModel(
    val id: String,
    val orderNumberLabel: String,
    val timeLabel: String,
    val status: OrderStatus,
    val statusLabel: String,
    val itemsLabel: String,
    val requestLabel: String,
    val totalAmount: Int,
    val actionLabel: String,
    val isActionInProgress: Boolean,
    val priorityBadge: SchedulingBadge = SchedulingBadge.Normal,
    val etaLabel: String? = null,
)
