package com.cafeminsu.domain.model

sealed interface AuthState {
    data object Unknown : AuthState
    data object Guest : AuthState
    data class Authenticated(val user: UserProfile) : AuthState
    data object Expired : AuthState
}

data class UserProfile(
    val id: String,
    val displayName: String,
    val phoneLast4: String?,
)
