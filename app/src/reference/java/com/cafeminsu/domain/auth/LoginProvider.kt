package com.cafeminsu.domain.auth

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.AuthState

interface LoginProvider {
    suspend fun login(): AppResult<AuthState>
    suspend fun loginForServerExchange(): AppResult<KakaoOAuthToken> =
        AppResult.Failure(DomainError.Unauthorized)

    suspend fun logout(): AppResult<Unit>
}

data class KakaoOAuthToken(
    val value: String,
) {
    override fun toString(): String = "KakaoOAuthToken(value=<redacted>)"
}
