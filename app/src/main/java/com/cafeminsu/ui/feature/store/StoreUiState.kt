package com.cafeminsu.ui.feature.store

sealed interface StoreUiState {
    data object Loading : StoreUiState

    data class Content(
        val query: String,
        val stores: List<StoreUiModel>,
        val selectedStore: StoreDetailUiModel?,
    ) : StoreUiState

    data class Empty(
        val query: String,
        val message: String,
    ) : StoreUiState

    data class Error(
        val message: String,
        val retryable: Boolean,
    ) : StoreUiState
}

data class StoreUiModel(
    val id: String,
    val name: String,
    val address: String,
    val distanceLabel: String,
    val status: StoreStatusUiModel,
    val statusLabel: String,
    val latitude: Double,
    val longitude: Double,
)

data class StoreDetailUiModel(
    val id: String,
    val name: String,
    val statusLabel: String,
    val address: String,
    val phone: String,
    val distanceLabel: String,
    val parkingLabel: String,
    val amenities: List<String>,
)

enum class StoreStatusUiModel {
    Open,
    ClosingSoon,
    Closed,
}

sealed interface StoreEvent {
    data object NavigateToMenu : StoreEvent
}
