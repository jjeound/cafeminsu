package com.cafeminsu.ui.navigation

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.UserProfile
import com.cafeminsu.domain.model.UserRole
import com.cafeminsu.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class StartDestinationTest {
    @Test
    fun ownerAuthenticatedMapsToOwnerHome() {
        assertEquals(Routes.OWNER_HOME, resolveStartDestination(authenticated(UserRole.Owner)))
    }

    @Test
    fun customerAuthenticatedMapsToHome() {
        assertEquals(Routes.HOME, resolveStartDestination(authenticated(UserRole.Customer)))
    }

    @Test
    fun guestExpiredUnknownMapToLogin() {
        assertEquals(Routes.LOGIN, resolveStartDestination(AuthState.Guest))
        assertEquals(Routes.LOGIN, resolveStartDestination(AuthState.Expired))
        assertEquals(Routes.LOGIN, resolveStartDestination(AuthState.Unknown))
    }

    @Test
    fun initialGuestResolvesToLogin() = runTest {
        val repo = FakeSessionRepository(AuthState.Guest)
        assertEquals(Routes.LOGIN, resolveInitialStartDestination(repo))
    }

    @Test
    fun initialOwnerResolvesToOwnerHome() = runTest {
        val repo = FakeSessionRepository(authenticated(UserRole.Owner))
        assertEquals(Routes.OWNER_HOME, resolveInitialStartDestination(repo))
    }

    @Test
    fun unknownTriggersRefreshAndUsesRefreshedState() = runTest {
        val repo = FakeSessionRepository(
            initial = AuthState.Unknown,
            refreshResult = AppResult.Success(authenticated(UserRole.Customer)),
        )
        assertEquals(Routes.HOME, resolveInitialStartDestination(repo))
    }

    @Test
    fun unknownWithFailedRefreshFallsBackToLogin() = runTest {
        val repo = FakeSessionRepository(
            initial = AuthState.Unknown,
            refreshResult = AppResult.Failure(DomainError.Network),
        )
        assertEquals(Routes.LOGIN, resolveInitialStartDestination(repo))
    }

    private fun authenticated(role: UserRole): AuthState.Authenticated =
        AuthState.Authenticated(
            user = UserProfile(id = "u1", displayName = "민수", phoneLast4 = null),
            role = role,
        )

    private class FakeSessionRepository(
        initial: AuthState,
        private val refreshResult: AppResult<AuthState> = AppResult.Success(initial),
    ) : SessionRepository {
        private val authState = MutableStateFlow(initial)

        override fun observeAuthState(): Flow<AuthState> = authState

        override suspend fun refreshOnce(): AppResult<AuthState> = refreshResult

        override suspend fun clearSession(): AppResult<Unit> = AppResult.Success(Unit)
    }
}
