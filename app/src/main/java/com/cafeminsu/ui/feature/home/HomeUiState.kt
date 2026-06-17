package com.cafeminsu.ui.feature.home

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Content(
        val greeting: String,
        val recommendedMenus: List<HomeMenuSummary>,
        val stampSummary: HomeStampSummary,
        val ongoingOrder: HomeOngoingOrderSummary?,
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

data class HomeMenuSummary(
    val id: String,
    val name: String,
    val description: String,
    val price: Int,
)

data class HomeStampSummary(
    val currentCount: Int,
    val goalCount: Int,
) {
    val progress: Float =
        if (goalCount <= 0) {
            EmptyProgress
        } else {
            currentCount.coerceIn(0, goalCount).toFloat() / goalCount.toFloat()
        }

    val remainingCount: Int = (goalCount - currentCount).coerceAtLeast(0)
}

data class HomeOngoingOrderSummary(
    val orderNumber: String,
    val title: String,
    val status: String,
)

private const val EmptyProgress = 0f
