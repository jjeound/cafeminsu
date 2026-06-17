package com.cafeminsu.domain.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.AuthState
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun observeAuthState(): Flow<AuthState>
    suspend fun refreshOnce(): AppResult<AuthState>
    suspend fun clearSession(): AppResult<Unit>
}
