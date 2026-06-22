package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.auth.SessionStateHolder
import com.cafeminsu.data.remote.FcmTokenApi
import com.cafeminsu.data.remote.createMoshi
import com.cafeminsu.data.remote.createOkHttpClient
import com.cafeminsu.data.remote.createRetrofit
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.UserProfile
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RealFcmTokenRepositoryTest {
    private lateinit var server: MockWebServer
    private val testDispatcher = StandardTestDispatcher()

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
    fun registerPostsTokenToFcmEndpointWithoutUserIdQuery() = runTest(testDispatcher) {
        server.enqueue(successVoidResponse())
        val repository = realFcmTokenRepository()

        val result = repository.register("device-token-abc")

        assertEquals(AppResult.Success(Unit), result)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/user/fcm-token", request.requestUrl?.encodedPath)
        assertEquals(null, request.requestUrl?.queryParameter("userId"))
        assertTrue(request.body.readUtf8().contains("\"fcmToken\":\"device-token-abc\""))
    }

    @Test
    fun notFoundHttpStatusMapsToFailure() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(404))
        val repository = realFcmTokenRepository()

        val result = repository.register("device-token-abc")

        assertEquals(AppResult.Failure(DomainError.NotFound), result)
    }

    @Test
    fun httpErrorMapsToFailure() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(401))
        val repository = realFcmTokenRepository()

        val result = repository.register("device-token-abc")

        assertEquals(AppResult.Failure(DomainError.Unauthorized), result)
    }

    @Test
    fun guestSessionBlocksNetworkBeforeCall() = runTest(testDispatcher) {
        val repository = realFcmTokenRepository(authState = AuthState.Guest)

        val result = repository.register("device-token-abc")

        assertEquals(AppResult.Failure(DomainError.Unauthorized), result)
        assertEquals(0, server.requestCount)
    }

    private fun realFcmTokenRepository(
        authState: AuthState = authenticatedState(),
    ): RealFcmTokenRepository =
        RealFcmTokenRepository(
            fcmTokenApi = fcmTokenApi(),
            sessionStateHolder = SessionStateHolder(authState),
            ioDispatcher = testDispatcher,
        )

    private fun fcmTokenApi(): FcmTokenApi =
        createRetrofit(
            baseUrl = server.url("/").toString(),
            moshi = createMoshi(),
            okHttpClient = createOkHttpClient(debug = false),
        ).create(FcmTokenApi::class.java)

    private fun successVoidResponse(): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody("{}")

    private fun authenticatedState(): AuthState.Authenticated =
        AuthState.Authenticated(
            user = UserProfile(
                id = "42",
                displayName = "민수",
                phoneLast4 = null,
            ),
        )
}
