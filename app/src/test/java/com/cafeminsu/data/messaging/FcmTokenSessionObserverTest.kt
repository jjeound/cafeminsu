package com.cafeminsu.data.messaging

import com.cafeminsu.core.AppResult
import com.cafeminsu.data.auth.SessionStateHolder
import com.cafeminsu.domain.messaging.DeviceMessagingTokenProvider
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.UserProfile
import com.cafeminsu.domain.repository.FcmTokenRepository
import com.cafeminsu.domain.repository.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FcmTokenSessionObserverTest {
    @Test
    fun registersTokenWhenSessionBecomesAuthenticated() = runTest {
        val repository = CollectingFcmTokenRepository()
        val observer = observer(
            sessionStates = flowOf(AuthState.Guest, authenticated()),
            repository = repository,
        )

        observer.start()
        advanceUntilIdle()

        assertEquals(listOf("device-token"), repository.registeredTokens)
    }

    @Test
    fun doesNotRegisterWhileGuest() = runTest {
        val repository = CollectingFcmTokenRepository()
        val observer = observer(
            sessionStates = flowOf(AuthState.Unknown, AuthState.Guest),
            repository = repository,
        )

        observer.start()
        advanceUntilIdle()

        assertEquals(emptyList<String>(), repository.registeredTokens)
    }

    @Test
    fun registersOnceForRepeatedAuthenticatedEmissions() = runTest {
        val repository = CollectingFcmTokenRepository()
        val authenticated = authenticated()
        val observer = observer(
            sessionStates = flowOf(authenticated, authenticated),
            repository = repository,
        )

        observer.start()
        advanceUntilIdle()

        assertEquals(listOf("device-token"), repository.registeredTokens)
    }

    private fun kotlinx.coroutines.test.TestScope.observer(
        sessionStates: Flow<AuthState>,
        repository: FcmTokenRepository,
    ): FcmTokenSessionObserver {
        val registrar = FcmTokenRegistrar(
            tokenProvider = FixedTokenProvider("device-token"),
            fcmTokenRepository = repository,
            sessionStateHolder = SessionStateHolder(authenticated()),
        )
        return FcmTokenSessionObserver(
            sessionRepository = FakeSessionRepository(sessionStates),
            registrar = registrar,
            appScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
        )
    }

    private fun authenticated(): AuthState.Authenticated =
        AuthState.Authenticated(
            user = UserProfile(id = "42", displayName = "민수", phoneLast4 = null),
        )
}

private class FixedTokenProvider(
    private val token: String?,
) : DeviceMessagingTokenProvider {
    override suspend fun currentToken(): String? = token
}

private class CollectingFcmTokenRepository : FcmTokenRepository {
    private val tokens = mutableListOf<String>()

    val registeredTokens: List<String>
        get() = tokens.toList()

    override suspend fun register(token: String): AppResult<Unit> {
        tokens += token
        return AppResult.Success(Unit)
    }
}

private class FakeSessionRepository(
    private val states: Flow<AuthState>,
) : SessionRepository {
    override fun observeAuthState(): Flow<AuthState> = states

    override suspend fun refreshOnce(): AppResult<AuthState> =
        AppResult.Success(AuthState.Guest)

    override suspend fun clearSession(): AppResult<Unit> = AppResult.Success(Unit)
}
