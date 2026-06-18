package com.cafeminsu.ui.feature.my

sealed interface MyUiState {
    data object Loading : MyUiState

    data class Content(
        val profile: MyProfileUiModel,
        val recentOrders: List<MyOrderSummaryUiModel>,
        val settings: List<MySettingItemUiModel>,
        val appMeta: String,
    ) : MyUiState

    data class Empty(
        val profile: MyProfileUiModel,
        val message: String,
        val actionLabel: String,
        val settings: List<MySettingItemUiModel>,
        val appMeta: String,
    ) : MyUiState

    data class Error(
        val message: String,
        val retryable: Boolean,
    ) : MyUiState

    data class NeedsLogin(
        val message: String,
        val actionLabel: String,
    ) : MyUiState
}

data class MyProfileUiModel(
    val displayName: String,
    val phoneLast4: String?,
)

data class MyOrderSummaryUiModel(
    val orderId: String,
    val orderNumber: String,
    val createdAtMillis: Long,
    val totalAmount: Int,
    val statusLabel: String,
)

data class MySettingItemUiModel(
    val id: String,
    val label: String,
)
