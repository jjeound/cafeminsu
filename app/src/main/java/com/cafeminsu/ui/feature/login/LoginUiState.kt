package com.cafeminsu.ui.feature.login

data class LoginUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface LoginEvent {
    data object NavigateHome : LoginEvent
    data object NavigateSignup : LoginEvent
    data class ShowSnackbar(val message: String) : LoginEvent
}
