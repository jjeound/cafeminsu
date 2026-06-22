package com.cafeminsu.data.messaging

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.auth.SessionStateHolder
import com.cafeminsu.domain.messaging.DeviceMessagingTokenProvider
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.UserProfile
import com.cafeminsu.domain.repository.FcmTokenRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FcmTokenRegistrarTest {
    @Test
    fun skipsRegistrationWhenNotAuthenticated() = runTest {
        val provider = RecordingTokenProvider("device-token")
        val repository = RecordingFcmTokenRepository()
        val registrar = registrar(provider, repository, AuthState.Guest)

        val result = registrar.register()

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(0, provider.callCount)
        assertEquals(emptyList<String>(), repository.registeredTokens)
    }

    @Test
    fun fetchesDeviceTokenAndRegistersWhenAuthenticated() = runTest {
        val provider = RecordingTokenProvider("device-token")
        val repository = RecordingFcmTokenRepository()
        val registrar = registrar(provider, repository, authenticated())

        val result = registrar.register()

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(1, provider.callCount)
        assertEquals(listOf("device-token"), repository.registeredTokens)
    }

    @Test
    fun explicitTokenBypassesProvider() = runTest {
        val provider = RecordingTokenProvider("device-token")
        val repository = RecordingFcmTokenRepository()
        val registrar = registrar(provider, repository, authenticated())

        registrar.register(explicitToken = "rotated-token")

        assertEquals(0, provider.callCount)
        assertEquals(listOf("rotated-token"), repository.registeredTokens)
    }

    @Test
    fun returnsFailureWhenTokenIsBlank() = runTest {
        val provider = RecordingTokenProvider(null)
        val repository = RecordingFcmTokenRepository()
        val registrar = registrar(provider, repository, authenticated())

        val result = registrar.register()

        assertEquals(AppResult.Failure(DomainError.Unknown), result)
        assertEquals(emptyList<String>(), repository.registeredTokens)
    }

    @Test
    fun propagatesRepositoryFailure() = runTest {
        val provider = RecordingTokenProvider("device-token")
        val repository = RecordingFcmTokenRepository(result = AppResult.Failure(DomainError.Network))
        val registrar = registrar(provider, repository, authenticated())

        val result = registrar.register()

        assertEquals(AppResult.Failure(DomainError.Network), result)
    }

    private fun registrar(
        provider: DeviceMessagingTokenProvider,
        repository: FcmTokenRepository,
        authState: AuthState,
    ): FcmTokenRegistrar =
        FcmTokenRegistrar(
            tokenProvider = provider,
            fcmTokenRepository = repository,
            sessionStateHolder = SessionStateHolder(authState),
        )

    private fun authenticated(): AuthState.Authenticated =
        AuthState.Authenticated(
            user = UserProfile(id = "42", displayName = "민수", phoneLast4 = null),
        )
}

private class RecordingTokenProvider(
    private val token: String?,
) : DeviceMessagingTokenProvider {
    var callCount: Int = 0
        private set

    override suspend fun currentToken(): String? {
        callCount++
        return token
    }
}

private class RecordingFcmTokenRepository(
    private val result: AppResult<Unit> = AppResult.Success(Unit),
) : FcmTokenRepository {
    private val tokens = mutableListOf<String>()

    val registeredTokens: List<String>
        get() = tokens.toList()

    override suspend fun register(token: String): AppResult<Unit> {
        tokens += token
        return result
    }
}
