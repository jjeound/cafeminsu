package com.cafeminsu.domain.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.AuthState
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun observeAuthState(): Flow<AuthState>
    suspend fun refreshOnce(): AppResult<AuthState>
    suspend fun login(): AppResult<AuthState> = refreshOnce()
    suspend fun logout(): AppResult<Unit> = clearSession()
    suspend fun clearSession(): AppResult<Unit>
}
