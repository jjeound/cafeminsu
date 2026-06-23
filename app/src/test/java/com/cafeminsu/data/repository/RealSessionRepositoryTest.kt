package com.cafeminsu.data.repository

import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.auth.SessionStateHolder
import com.cafeminsu.data.auth.SessionTokenStore
import com.cafeminsu.data.auth.SessionTokens
import com.cafeminsu.data.remote.AuthApi
import com.cafeminsu.data.remote.AuthorizationInterceptor
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
                      "accessToken": "app-access-token",
                      "refreshToken": "app-refresh-token",
                      "isNewUser": false,
                      "nickname": "지원"
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
            assertEquals(false, authState.isNewUser)
            assertEquals(authState, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(SessionTokens("app-access-token", "app-refresh-token"), tokenStore.read())
        val request = server.takeRequest()
        assertEquals("/api/user/kakao-login", request.path)
        assertEquals("""{"accessToken":"kakao-access-token"}""", request.body.readUtf8())
    }

    @Test
    fun loginExchangeCarriesNewUserSignalToAuthenticatedState() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "accessToken": "app-access-token",
                      "refreshToken": "app-refresh-token",
                      "isNewUser": true,
                      "nickname": null
                    }
                    """.trimIndent(),
                ),
        )
        val stateHolder = SessionStateHolder(AuthState.Guest)
        val repository = realSessionRepository(stateHolder = stateHolder)

        val result = repository.login()

        assertTrue(result is AppResult.Success)
        val authState = (result as AppResult.Success).data
        assertTrue(authState is AuthState.Authenticated)
        assertEquals(true, (authState as AuthState.Authenticated).isNewUser)
        assertEquals(authState, stateHolder.authState.value)
    }

    @Test
    fun loginExchangeFailureDoesNotStoreSessionTokens() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))
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
                      "accessToken": "new-access-token"
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
                      "accessToken": "new-access-token"
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
    fun checkNicknameMapsAvailableAndDuplicatedResponsesWithBearerToken() = runTest {
        server.enqueue(nicknameCheckResponse(available = true))
        server.enqueue(nicknameCheckResponse(available = false))
        val repository = realSessionRepository(
            tokenStore = InMemorySessionTokenStore(SessionTokens("access-token", "refresh-token")),
            stateHolder = SessionStateHolder(authenticatedState("신규 사용자", isNewUser = true)),
        )

        assertEquals(AppResult.Success(true), repository.checkNickname("새민수"))
        assertEquals(AppResult.Success(false), repository.checkNickname("이미사용중"))

        val firstRequest = server.takeRequest()
        assertEquals("/api/user/nickname/check", firstRequest.requestUrl?.encodedPath)
        assertEquals("새민수", firstRequest.requestUrl?.queryParameter("nickname"))
        assertEquals(null, firstRequest.requestUrl?.queryParameter("userId"))
        assertEquals("Bearer access-token", firstRequest.getHeader("Authorization"))

        val secondRequest = server.takeRequest()
        assertEquals("이미사용중", secondRequest.requestUrl?.queryParameter("nickname"))
        assertEquals("Bearer access-token", secondRequest.getHeader("Authorization"))
    }

    @Test
    fun checkNicknameValidationAndServerErrorReturnFailure() = runTest {
        val repository = realSessionRepository(
            tokenStore = InMemorySessionTokenStore(SessionTokens("access-token", "refresh-token")),
            stateHolder = SessionStateHolder(authenticatedState("신규 사용자", isNewUser = true)),
        )

        assertEquals(AppResult.Failure(DomainError.Validation("nickname")), repository.checkNickname("  "))
        assertEquals(0, server.requestCount)

        server.enqueue(MockResponse().setResponseCode(404))

        assertEquals(AppResult.Failure(DomainError.NotFound), repository.checkNickname("새민수"))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun completeSignupPostsNicknameAndUpdatesSessionWithoutChangingTokens() = runTest {
        server.enqueue(signupResponse(userId = 77, nickname = "새민수"))
        val tokenStore = InMemorySessionTokenStore(SessionTokens("access-token", "refresh-token"))
        val repository = realSessionRepository(
            tokenStore = tokenStore,
            stateHolder = SessionStateHolder(authenticatedState("카카오 사용자", isNewUser = true)),
        )

        repository.observeAuthState().test {
            val initial = awaitItem()
            assertTrue(initial is AuthState.Authenticated)
            assertEquals(true, (initial as AuthState.Authenticated).isNewUser)

            val result = repository.completeSignup("새민수")

            assertTrue(result is AppResult.Success)
            val authState = (result as AppResult.Success).data
            assertTrue(authState is AuthState.Authenticated)
            val authenticated = authState as AuthState.Authenticated
            assertEquals("77", authenticated.user.id)
            assertEquals("새민수", authenticated.user.displayName)
            assertEquals(false, authenticated.isNewUser)
            assertEquals(authenticated, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(SessionTokens("access-token", "refresh-token"), tokenStore.read())
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/user/signup", request.requestUrl?.encodedPath)
        assertEquals(null, request.requestUrl?.queryParameter("userId"))
        assertEquals("Bearer access-token", request.getHeader("Authorization"))
        assertTrue(request.body.readUtf8().contains("\"nickname\":\"새민수\""))
    }

    @Test
    fun completeSignupValidationServerErrorAndMissingTokenReturnFailure() = runTest {
        val authenticatedRepository = realSessionRepository(
            tokenStore = InMemorySessionTokenStore(SessionTokens("access-token", "refresh-token")),
            stateHolder = SessionStateHolder(authenticatedState("카카오 사용자", isNewUser = true)),
        )

        assertEquals(
            AppResult.Failure(DomainError.Validation("nickname")),
            authenticatedRepository.completeSignup(" "),
        )
        assertEquals(0, server.requestCount)

        server.enqueue(MockResponse().setResponseCode(404))

        assertEquals(AppResult.Failure(DomainError.NotFound), authenticatedRepository.completeSignup("새민수"))
        assertEquals(1, server.requestCount)

        val missingTokenRepository = realSessionRepository(
            tokenStore = InMemorySessionTokenStore(),
            stateHolder = SessionStateHolder(authenticatedState("카카오 사용자", isNewUser = true)),
        )

        assertEquals(AppResult.Failure(DomainError.Unauthorized), missingTokenRepository.completeSignup("새민수"))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun unauthenticatedStateBlocksSignupCallsBeforeNetwork() = runTest {
        val repository = realSessionRepository(
            tokenStore = InMemorySessionTokenStore(SessionTokens("access-token", "refresh-token")),
            stateHolder = SessionStateHolder(AuthState.Guest),
        )

        assertEquals(AppResult.Failure(DomainError.Unauthorized), repository.checkNickname("새민수"))
        assertEquals(AppResult.Failure(DomainError.Unauthorized), repository.completeSignup("새민수"))
        assertEquals(0, server.requestCount)
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
            authApi = authApi(tokenStore),
            tokenStore = tokenStore,
            sessionStateHolder = stateHolder,
        )

    private fun authApi(tokenStore: SessionTokenStore): AuthApi =
        createRetrofit(
            baseUrl = server.url("/").toString(),
            moshi = createMoshi(),
            okHttpClient = createOkHttpClient(
                debug = false,
                authorizationInterceptor = AuthorizationInterceptor(tokenStore),
            ),
        ).create(AuthApi::class.java)

    private fun nicknameCheckResponse(available: Boolean): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                {
                  "available": $available
                }
                """.trimIndent(),
            )

    private fun signupResponse(userId: Long, nickname: String): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                {
                  "userId": $userId,
                  "nickname": "$nickname"
                }
                """.trimIndent(),
            )
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

private fun authenticatedState(
    displayName: String,
    isNewUser: Boolean = false,
): AuthState.Authenticated =
    AuthState.Authenticated(
        user = com.cafeminsu.domain.model.UserProfile(
            id = "server-user",
            displayName = displayName,
            phoneLast4 = null,
        ),
        isNewUser = isNewUser,
    )
