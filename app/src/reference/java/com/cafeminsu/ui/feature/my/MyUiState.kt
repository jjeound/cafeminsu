package com.cafeminsu.ui.feature.my

sealed interface MyUiState {
    data object Loading : MyUiState

    data class Content(
        val profile: MyProfileUiModel,
        val stats: MyStatsUiModel,
        val quickMenus: List<MyQuickMenuUiModel>,
        val settings: List<MySettingItemUiModel>,
    ) : MyUiState

    data class Empty(
        val message: String,
        val actionLabel: String,
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

sealed interface MyEvent {
    data object NavigateLogin : MyEvent
}

data class MyProfileUiModel(
    val displayName: String,
    val initial: String,
    val tierLabel: String,
)

data class MyStatsUiModel(
    val orderCount: Int,
    val stampCount: Int,
    val stampGoalCount: Int,
    val couponCount: Int,
)

data class MyQuickMenuUiModel(
    val id: String,
    val label: String,
)

data class MySettingItemUiModel(
    val id: String,
    val label: String,
    val trailingText: String? = null,
    val isDestructive: Boolean = false,
)
