package com.ssafy.cafeminsu.feature.home

data class HomeUiState(
    val greeting: String = "카페민수",
    val selectedStoreName: String? = null,
    val recommendedMenu: HomeRecommendedMenu? = null,
    val recentOrders: List<HomeRecentOrderUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

data class HomeRecommendedMenu(
    val id: Long,
    val name: String,
    val description: String,
    val priceLabel: String,
    val storeName: String?,
)

data class HomeRecentOrderUiModel(
    val id: Long,
    val orderNumber: String,
    val statusLabel: String,
    val priceLabel: String,
    val orderedAtLabel: String,
)