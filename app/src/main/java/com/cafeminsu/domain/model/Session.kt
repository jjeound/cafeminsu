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

/** 로그인한 점주가 운영하는 매장(헤더 매장 선택용). 단일 매장이면 1개만 노출한다. */
data class OwnerStore(
    val id: String,
    val name: String,
)
