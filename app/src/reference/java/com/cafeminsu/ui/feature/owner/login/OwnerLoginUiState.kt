package com.cafeminsu.ui.feature.owner.login

data class OwnerLoginUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface OwnerLoginEvent {
    data object NavigateOwnerHome : OwnerLoginEvent
    data class ShowSnackbar(val message: String) : OwnerLoginEvent
}
