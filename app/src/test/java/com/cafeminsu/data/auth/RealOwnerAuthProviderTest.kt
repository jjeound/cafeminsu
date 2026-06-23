package com.cafeminsu.data.auth

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.local.prefs.UserPreferencesDataStore
import com.cafeminsu.data.remote.AuthApi
import com.cafeminsu.data.remote.AuthorizationInterceptor
import com.cafeminsu.data.remote.createMoshi
import com.cafeminsu.data.remote.createOkHttpClient
import com.cafeminsu.data.remote.createRetrofit
import com.cafeminsu.domain.model.OwnerProfile
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RealOwnerAuthProviderTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

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
    fun loginAuthenticatesStoresTokensAndReturnsOwnerProfile() = runTest {
        server.enqueue(ownerLoginResponse(nickname = "강남점장"))
        val tokenStore = InMemorySessionTokenStore()
        val provider = realOwnerAuthProvider(tokenStore)

        val result = provider.login(loginId = "owner02", password = "cafe5678")

        assertTrue(result is AppResult.Success)
        val profile = (result as AppResult.Success<OwnerProfile>).data
        assertEquals("owner02", profile.loginId)
        assertEquals("강남점장", profile.storeName)
        assertEquals(true, profile.isStoreOpen)
        assertEquals(SessionTokens("owner-access", "owner-refresh"), tokenStore.read())

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/user/owner-login", request.path)
        assertEquals("""{"loginId":"owner02","password":"cafe5678"}""", request.body.readUtf8())
        // owner-login is a public endpoint: the interceptor must not attach a bearer token.
        assertNull(request.getHeader("Authorization"))
    }

    @Test
    fun loginFailureReturnsUnauthorizedAndDoesNotStoreTokens() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))
        val tokenStore = InMemorySessionTokenStore()
        val provider = realOwnerAuthProvider(tokenStore)

        val result = provider.login(loginId = "owner02", password = "wrong")

        assertEquals(AppResult.Failure(DomainError.Unauthorized), result)
        assertNull(tokenStore.read())
    }

    @Test
    fun setStoreOpenTogglesStoreStateAfterLogin() = runTest {
        server.enqueue(ownerLoginResponse(nickname = "강남점장"))
        val provider = realOwnerAuthProvider(InMemorySessionTokenStore())
        provider.login(loginId = "owner02", password = "cafe5678")

        val closed = provider.setStoreOpen(open = false)
        val opened = provider.setStoreOpen(open = true)

        assertTrue(closed is AppResult.Success)
        assertTrue(opened is AppResult.Success)
        assertEquals(false, (closed as AppResult.Success<OwnerProfile>).data.isStoreOpen)
        assertEquals(true, (opened as AppResult.Success<OwnerProfile>).data.isStoreOpen)
    }

    @Test
    fun setStoreOpenPersistsAndReLoginReflectsToggle() = runTest {
        val preferences = newPreferences()

        server.enqueue(ownerLoginResponse(nickname = "강남점장"))
        val firstSession = realOwnerAuthProvider(InMemorySessionTokenStore(), preferences)
        firstSession.login(loginId = "owner02", password = "cafe5678")
        firstSession.setStoreOpen(open = false)

        // 새 세션(앱 재시작 모사)이 같은 DataStore 에서 영속된 영업 토글을 복원해야 한다.
        server.enqueue(ownerLoginResponse(nickname = "강남점장"))
        val secondSession = realOwnerAuthProvider(InMemorySessionTokenStore(), preferences)
        val result = secondSession.login(loginId = "owner02", password = "cafe5678")

        assertTrue(result is AppResult.Success)
        assertEquals(false, (result as AppResult.Success<OwnerProfile>).data.isStoreOpen)
    }

    @Test
    fun setStoreOpenWithoutLoginReturnsUnauthorized() = runTest {
        val provider = realOwnerAuthProvider(InMemorySessionTokenStore())

        assertEquals(AppResult.Failure(DomainError.Unauthorized), provider.setStoreOpen(open = false))
    }

    @Test
    fun logoutClearsStoredTokens() = runTest {
        val tokenStore = InMemorySessionTokenStore(SessionTokens("owner-access", "owner-refresh"))
        val provider = realOwnerAuthProvider(tokenStore)

        val result = provider.logout()

        assertEquals(AppResult.Success(Unit), result)
        assertNull(tokenStore.read())
    }

    private fun TestScope.realOwnerAuthProvider(
        tokenStore: SessionTokenStore,
        preferences: UserPreferencesDataStore = newPreferences(),
    ): RealOwnerAuthProvider =
        RealOwnerAuthProvider(
            authApi = createRetrofit(
                baseUrl = server.url("/").toString(),
                moshi = createMoshi(),
                okHttpClient = createOkHttpClient(
                    debug = false,
                    authorizationInterceptor = AuthorizationInterceptor(tokenStore),
                ),
            ).create(AuthApi::class.java),
            tokenStore = tokenStore,
            preferences = preferences,
        )

    private fun TestScope.newPreferences(): UserPreferencesDataStore =
        UserPreferencesDataStore(
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(StandardTestDispatcher(testScheduler) + Job()),
                produceFile = { File(tempFolder.newFolder(), "user.preferences_pb") },
            ),
        )

    private fun ownerLoginResponse(nickname: String): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                {
                  "accessToken": "owner-access",
                  "refreshToken": "owner-refresh",
                  "nickname": "$nickname"
                }
                """.trimIndent(),
            )
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
