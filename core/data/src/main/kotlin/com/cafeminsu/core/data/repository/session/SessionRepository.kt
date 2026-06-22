package com.cafeminsu.core.data.repository.session

import com.cafeminsu.core.model.session.AuthState
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun observeAuthState(): Flow<AuthState>

    fun refreshOnce(): Flow<AuthState>

    fun login(): Flow<AuthState> = refreshOnce()

    fun logout(): Flow<Unit> = clearSession()

    fun checkNickname(nickname: String): Flow<Boolean> =
        throw UnsupportedOperationException("Not implemented")

    fun completeSignup(nickname: String): Flow<AuthState> =
        throw UnsupportedOperationException("Not implemented")

    fun clearSession(): Flow<Unit>
}
