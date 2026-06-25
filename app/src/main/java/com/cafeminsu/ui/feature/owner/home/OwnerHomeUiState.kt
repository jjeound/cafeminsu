package com.cafeminsu.ui.feature.owner.home

import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.domain.model.OwnerStore

sealed interface OwnerHomeUiState {
    data object Loading : OwnerHomeUiState

    data class Content(
        val storeName: String,
        val isStoreOpen: Boolean,
        val dateLabel: String,
        val stats: OwnerHomeStatsUiModel,
        val pendingOrders: List<OwnerHomeOrderUiModel>,
        val isStoreOpenUpdating: Boolean,
        val stores: List<OwnerStoreUiModel> = emptyList(),
    ) : OwnerHomeUiState

    data class Empty(
        val storeName: String,
        val isStoreOpen: Boolean,
        val dateLabel: String,
        val stats: OwnerHomeStatsUiModel,
        val message: String,
        val isStoreOpenUpdating: Boolean,
        val stores: List<OwnerStoreUiModel> = emptyList(),
    ) : OwnerHomeUiState

    data class Error(
        val message: String,
        val retryable: Boolean,
    ) : OwnerHomeUiState
}

data class OwnerHomeStatsUiModel(
    val totalSales: Int,
    val orderCount: Int,
    val newWaitingCount: Int,
)

data class OwnerStoreUiModel(
    val id: String,
    val name: String,
    val isSelected: Boolean,
)

data class OwnerHomeOrderUiModel(
    val id: String,
    val orderNumberLabel: String,
    val timeLabel: String,
    val status: OrderStatus,
    val statusLabel: String,
    val itemSummary: String,
    val totalAmount: Int,
    val actionLabel: String,
    val isActionInProgress: Boolean,
)
