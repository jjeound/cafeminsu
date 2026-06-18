package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.UserProfile
import com.cafeminsu.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockSessionRepository @Inject constructor() : SessionRepository {
    private val authState = MutableStateFlow<AuthState>(demoAuthenticatedState())

    override fun observeAuthState(): Flow<AuthState> = authState

    override suspend fun refreshOnce(): AppResult<AuthState> {
        if (authState.value == AuthState.Unknown) {
            authState.value = demoAuthenticatedState()
        }
        return AppResult.Success(authState.value)
    }

    override suspend fun clearSession(): AppResult<Unit> {
        authState.value = AuthState.Guest
        return AppResult.Success(Unit)
    }

    private fun demoAuthenticatedState(): AuthState.Authenticated =
        AuthState.Authenticated(
            UserProfile(
                id = "demo-user",
                displayName = "민수",
                phoneLast4 = "0000",
            ),
        )
}
