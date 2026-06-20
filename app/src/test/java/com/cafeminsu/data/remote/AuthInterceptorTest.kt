package com.cafeminsu.data.remote

import com.cafeminsu.data.auth.SessionStateHolder
import com.cafeminsu.data.auth.SessionTokenStore
import com.cafeminsu.data.auth.SessionTokens
import com.cafeminsu.domain.model.AuthState
import kotlinx.coroutines.test.runTest
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {
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
    fun protectedRequestAddsBearerTokenAndRefreshesOnceAfterUnauthorized() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))
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
        server.enqueue(MockResponse().setResponseCode(200).setBody("OK"))
        val tokenStore = InMemorySessionTokenStore(
            SessionTokens("old-access-token", "refresh-token"),
        )
        val client = authClient(tokenStore = tokenStore)

        val response = client.newCall(Request.Builder().url(server.url("/api/orders")).build()).execute()

        assertEquals(200, response.code)
        assertEquals("Bearer old-access-token", server.takeRequest().getHeader("Authorization"))
        val refreshRequest = server.takeRequest()
        assertEquals("/api/user/refresh", refreshRequest.path)
        assertNull(refreshRequest.getHeader("Authorization"))
        assertEquals("refresh-token", refreshRequest.getHeader("Refresh-Token"))
        assertEquals("Bearer new-access-token", server.takeRequest().getHeader("Authorization"))
        assertEquals(SessionTokens("new-access-token", "refresh-token"), tokenStore.read())
    }

    @Test
    fun refreshFailureExpiresSessionAndWipesTokens() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(401))
        val tokenStore = InMemorySessionTokenStore(
            SessionTokens("old-access-token", "refresh-token"),
        )
        val stateHolder = SessionStateHolder(AuthState.Authenticated(testUserProfile()))
        val client = authClient(
            tokenStore = tokenStore,
            stateHolder = stateHolder,
        )

        val response = client.newCall(Request.Builder().url(server.url("/api/orders")).build()).execute()

        assertEquals(401, response.code)
        assertNull(tokenStore.read())
        assertEquals(AuthState.Expired, stateHolder.authState.value)
    }

    private fun authClient(
        tokenStore: SessionTokenStore,
        stateHolder: SessionStateHolder = SessionStateHolder(AuthState.Guest),
    ) = createOkHttpClient(
        debug = false,
        authorizationInterceptor = AuthorizationInterceptor(tokenStore),
        authenticator = SessionAuthenticator(
            tokenStore = tokenStore,
            authApi = authApi(),
            sessionStateHolder = stateHolder,
        ),
    )

    private fun authApi(): AuthApi =
        createRetrofit(
            baseUrl = server.url("/").toString(),
            moshi = createMoshi(),
            okHttpClient = createOkHttpClient(debug = false),
        ).create(AuthApi::class.java)
}

private fun testUserProfile() =
    com.cafeminsu.domain.model.UserProfile(
        id = "server-user",
        displayName = "지원",
        phoneLast4 = null,
    )

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
