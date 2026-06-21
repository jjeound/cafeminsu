package com.cafeminsu.domain.model

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

enum class UserRole {
    Customer,
    Owner,
}

data class UserProfile(
    val id: String,
    val displayName: String,
    val phoneLast4: String?,
)

data class OwnerProfile(
    val id: String,
    val storeId: String,
    val storeName: String,
    val loginId: String,
    val isStoreOpen: Boolean,
)
