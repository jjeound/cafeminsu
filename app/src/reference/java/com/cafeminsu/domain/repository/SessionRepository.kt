package com.cafeminsu.domain.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.AuthState
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun observeAuthState(): Flow<AuthState>
    suspend fun refreshOnce(): AppResult<AuthState>
    suspend fun login(): AppResult<AuthState> = refreshOnce()
    suspend fun logout(): AppResult<Unit> = clearSession()
    suspend fun checkNickname(nickname: String): AppResult<Boolean> =
        AppResult.Failure(DomainError.Unknown)
    suspend fun completeSignup(nickname: String): AppResult<AuthState> =
        AppResult.Failure(DomainError.Unknown)
    suspend fun clearSession(): AppResult<Unit>
}
