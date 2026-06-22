package com.cafeminsu.core.model.session

sealed interface AuthState {
    data object Unknown : AuthState
    data object Guest : AuthState
    data class Authenticated(
        val user: UserProfile,
        val role: UserRole = UserRole.Customer,
        val isNewUser: Boolean = false,
    ) : AuthState
    data object Expired : AuthState
}
