package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.auth.LoginProvider
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockSessionRepository @Inject constructor(
    private val loginProvider: LoginProvider,
) : SessionRepository {
    private val authState = MutableStateFlow<AuthState>(AuthState.Guest)

    override fun observeAuthState(): Flow<AuthState> = authState

    override suspend fun refreshOnce(): AppResult<AuthState> {
        if (authState.value == AuthState.Unknown) {
            authState.value = AuthState.Guest
        }
        return AppResult.Success(authState.value)
    }

    override suspend fun login(): AppResult<AuthState> {
        val result = loginProvider.login()
        if (result is AppResult.Success) {
            authState.value = result.data
        }
        return result
    }

    override suspend fun logout(): AppResult<Unit> {
        val result = loginProvider.logout()
        authState.value = AuthState.Guest
        return result
    }

    override suspend fun clearSession(): AppResult<Unit> = logout()
}
