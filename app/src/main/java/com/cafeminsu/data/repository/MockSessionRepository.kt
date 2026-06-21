package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.auth.LoginProvider
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.UserProfile
import com.cafeminsu.domain.model.UserRole
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

    override suspend fun checkNickname(nickname: String): AppResult<Boolean> {
        val normalized = nickname.trim()
        if (normalized.isBlank()) {
            return AppResult.Failure(DomainError.Validation("nickname"))
        }
        return AppResult.Success(normalized !in ReservedNicknames)
    }

    override suspend fun completeSignup(nickname: String): AppResult<AuthState> {
        val normalized = nickname.trim()
        if (normalized.isBlank()) {
            return AppResult.Failure(DomainError.Validation("nickname"))
        }

        val current = authState.value as? AuthState.Authenticated
        val completed = AuthState.Authenticated(
            user = UserProfile(
                id = current?.user?.id ?: DemoSignupUserId,
                displayName = normalized,
                phoneLast4 = current?.user?.phoneLast4,
            ),
            role = current?.role ?: UserRole.Customer,
            isNewUser = false,
        )
        authState.value = completed
        return AppResult.Success(completed)
    }

    override suspend fun logout(): AppResult<Unit> {
        val result = loginProvider.logout()
        authState.value = AuthState.Guest
        return result
    }

    override suspend fun clearSession(): AppResult<Unit> = logout()

    private companion object {
        const val DemoSignupUserId = "demo-user"
        val ReservedNicknames = setOf("이미사용중", "admin", "owner")
    }
}
