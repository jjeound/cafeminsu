package com.cafeminsu.ui.feature.home

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Content(
        val greeting: String,
        val recommendedMenu: HomeRecommendedMenu,
        val recentOrders: List<HomeRecentOrderSummary>,
    ) : HomeUiState

    data class Empty(
        val greeting: String,
        val message: String,
    ) : HomeUiState

    data class Error(
        val message: String,
        val retryable: Boolean,
    ) : HomeUiState
}

data class HomeRecommendedMenu(
    val id: String,
    val name: String,
    val description: String,
    val price: Int,
    val storeName: String?,
)

data class HomeRecentOrderSummary(
    val orderId: String,
    val menuItemId: String,
    val menuName: String,
    val optionSummary: String,
    val orderedAtLabel: String,
    val totalPrice: Int,
)
