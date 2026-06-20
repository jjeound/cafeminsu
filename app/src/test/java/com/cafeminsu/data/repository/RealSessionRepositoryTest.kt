package com.cafeminsu.data.repository

import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.auth.SessionStateHolder
import com.cafeminsu.data.auth.SessionTokenStore
import com.cafeminsu.data.auth.SessionTokens
import com.cafeminsu.data.remote.AuthApi
import com.cafeminsu.data.remote.createMoshi
import com.cafeminsu.data.remote.createOkHttpClient
import com.cafeminsu.data.remote.createRetrofit
import com.cafeminsu.domain.auth.KakaoOAuthToken
import com.cafeminsu.domain.auth.LoginProvider
import com.cafeminsu.domain.model.AuthState
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RealSessionRepositoryTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun loginExchangesKakaoTokenStoresSessionTokensAndEmitsAuthenticatedState() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "isSuccess": true,
                      "code": 200,
                      "message": "OK",
                      "result": {
                        "accessToken": "app-access-token",
                        "refreshToken": "app-refresh-token",
                        "isNewUser": false,
                        "nickname": "지원"
                      }
                    }
                    """.trimIndent(),
                ),
        )
        val tokenStore = InMemorySessionTokenStore()
        val repository = realSessionRepository(
            loginProvider = FakeLoginProvider(kakaoAccessToken = "kakao-access-token"),
            tokenStore = tokenStore,
        )

        repository.observeAuthState().test {
            assertEquals(AuthState.Guest, awaitItem())

            val result = repository.login()

            assertTrue(result is AppResult.Success)
            val authState = (result as AppResult.Success).data
            assertTrue(authState is AuthState.Authenticated)
            assertEquals("지원", (authState as AuthState.Authenticated).user.displayName)
            assertEquals(authState, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(SessionTokens("app-access-token", "app-refresh-token"), tokenStore.read())
        val request = server.takeRequest()
        assertEquals("/api/user/kakao-login", request.path)
        assertEquals("""{"accessToken":"kakao-access-token"}""", request.body.readUtf8())
    }

    @Test
    fun loginExchangeFailureDoesNotStoreSessionTokens() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "isSuccess": false,
                      "code": 401,
                      "message": "invalid kakao token",
                      "result": null
                    }
                    """.trimIndent(),
                ),
        )
        val tokenStore = InMemorySessionTokenStore()
        val repository = realSessionRepository(tokenStore = tokenStore)

        val result = repository.login()

        assertEquals(AppResult.Failure(DomainError.Unauthorized), result)
        assertNull(tokenStore.read())
    }

    @Test
    fun refreshOnceUpdatesStoredAccessTokenAndKeepsAuthenticatedState() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "isSuccess": true,
                      "code": 200,
                      "message": "OK",
                      "result": {
                        "accessToken": "new-access-token"
                      }
                    }
                    """.trimIndent(),
                ),
        )
        val tokenStore = InMemorySessionTokenStore(
            SessionTokens("old-access-token", "refresh-token"),
        )
        val authenticated = authenticatedState("기존 사용자")
        val repository = realSessionRepository(
            tokenStore = tokenStore,
            stateHolder = SessionStateHolder(authenticated),
        )

        val result = repository.refreshOnce()

        assertEquals(AppResult.Success(authenticated), result)
        assertEquals(SessionTokens("new-access-token", "refresh-token"), tokenStore.read())
        assertEquals("refresh-token", server.takeRequest().getHeader("Refresh-Token"))
    }

    @Test
    fun refreshOnceWithStoredTokensAuthenticatesUnknownSession() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "isSuccess": true,
                      "code": 200,
                      "message": "OK",
                      "result": {
                        "accessToken": "new-access-token"
                      }
                    }
                    """.trimIndent(),
                ),
        )
        val tokenStore = InMemorySessionTokenStore(
            SessionTokens("old-access-token", "refresh-token"),
        )
        val repository = realSessionRepository(
            tokenStore = tokenStore,
            stateHolder = SessionStateHolder(AuthState.Unknown),
        )

        val result = repository.refreshOnce()

        assertTrue(result is AppResult.Success)
        assertTrue((result as AppResult.Success).data is AuthState.Authenticated)
    }

    @Test
    fun refreshUnauthorizedExpiresAndWipesStoredTokens() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))
        val tokenStore = InMemorySessionTokenStore(
            SessionTokens("old-access-token", "refresh-token"),
        )
        val stateHolder = SessionStateHolder(authenticatedState("기존 사용자"))
        val repository = realSessionRepository(
            tokenStore = tokenStore,
            stateHolder = stateHolder,
        )

        val result = repository.refreshOnce()

        assertEquals(AppResult.Success(AuthState.Expired), result)
        assertNull(tokenStore.read())
        assertEquals(AuthState.Expired, stateHolder.authState.value)
    }

    @Test
    fun logoutWipesTokensImmediatelyAndReturnsGuest() = runTest {
        val tokenStore = InMemorySessionTokenStore(
            SessionTokens("app-access-token", "app-refresh-token"),
        )
        val loginProvider = FakeLoginProvider(kakaoAccessToken = "kakao-access-token")
        val stateHolder = SessionStateHolder(authenticatedState("기존 사용자"))
        val repository = realSessionRepository(
            loginProvider = loginProvider,
            tokenStore = tokenStore,
            stateHolder = stateHolder,
        )

        val result = repository.logout()

        assertEquals(AppResult.Success(Unit), result)
        assertNull(tokenStore.read())
        assertTrue(loginProvider.logoutCalled)
        assertEquals(AuthState.Guest, stateHolder.authState.value)
    }

    private fun realSessionRepository(
        loginProvider: LoginProvider = FakeLoginProvider(kakaoAccessToken = "kakao-access-token"),
        tokenStore: SessionTokenStore = InMemorySessionTokenStore(),
        stateHolder: SessionStateHolder = SessionStateHolder(AuthState.Guest),
    ): RealSessionRepository =
        RealSessionRepository(
            loginProvider = loginProvider,
            authApi = authApi(),
            tokenStore = tokenStore,
            sessionStateHolder = stateHolder,
        )

    private fun authApi(): AuthApi =
        createRetrofit(
            baseUrl = server.url("/").toString(),
            moshi = createMoshi(),
            okHttpClient = createOkHttpClient(debug = false),
        ).create(AuthApi::class.java)
}

private class FakeLoginProvider(
    private val kakaoAccessToken: String,
) : LoginProvider {
    var logoutCalled: Boolean = false
        private set

    override suspend fun login(): AppResult<AuthState> =
        AppResult.Success(authenticatedState("카카오 사용자"))

    override suspend fun loginForServerExchange(): AppResult<KakaoOAuthToken> =
        AppResult.Success(KakaoOAuthToken(kakaoAccessToken))

    override suspend fun logout(): AppResult<Unit> {
        logoutCalled = true
        return AppResult.Success(Unit)
    }
}

private class InMemorySessionTokenStore(
    initialTokens: SessionTokens? = null,
) : SessionTokenStore {
    private var tokens: SessionTokens? = initialTokens

    override suspend fun read(): SessionTokens? = tokens

    override suspend fun save(tokens: SessionTokens) {
        this.tokens = tokens
    }

    override suspend fun updateAccessToken(accessToken: String) {
        tokens = tokens?.copy(accessToken = accessToken)
    }

    override suspend fun clear() {
        tokens = null
    }
}

private fun authenticatedState(displayName: String): AuthState.Authenticated =
    AuthState.Authenticated(
        user = com.cafeminsu.domain.model.UserProfile(
            id = "server-user",
            displayName = displayName,
            phoneLast4 = null,
        ),
    )
