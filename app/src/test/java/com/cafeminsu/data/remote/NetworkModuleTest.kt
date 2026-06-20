package com.cafeminsu.data.remote

import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.http.GET

class NetworkModuleTest {
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
    fun okHttpClientUsesArchitectureTimeouts() {
        val client = createOkHttpClient(debug = false)

        assertEquals(10_000, client.connectTimeoutMillis)
        assertEquals(15_000, client.readTimeoutMillis)
    }

    @Test
    fun debugOkHttpClientInstallsBodyLoggingInterceptor() {
        val client = createOkHttpClient(debug = true)

        val loggingInterceptor = client.interceptors
            .filterIsInstance<HttpLoggingInterceptor>()
            .single()

        assertEquals(HttpLoggingInterceptor.Level.BODY, loggingInterceptor.level)
    }

    @Test
    fun releaseOkHttpClientDoesNotInstallLoggingInterceptor() {
        val client = createOkHttpClient(debug = false)

        assertFalse(client.interceptors.any { it is HttpLoggingInterceptor })
    }

    @Test
    fun networkLoggerRedactsTokenHeadersAndJsonFields() {
        assertEquals(
            "Authorization: <redacted>",
            "Authorization: Bearer secret-access".redactSensitiveNetworkValues(),
        )
        assertEquals(
            """{"accessToken":"<redacted>","refreshToken":"<redacted>"}""",
            """{"accessToken":"secret-access","refreshToken":"secret-refresh"}"""
                .redactSensitiveNetworkValues(),
        )
    }

    @Test
    fun retrofitUsesConfiguredBaseUrlAndKeepsOpenApiRootPath() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"isSuccess":true,"code":200,"message":"OK","result":{}}"""),
        )
        val api = createRetrofit(
            baseUrl = server.url("/").toString(),
            moshi = createMoshi(),
            okHttpClient = createOkHttpClient(debug = false),
        ).create(TestApi::class.java)

        api.health()

        assertEquals("/health", server.takeRequest().path)
    }

    @Test
    fun retrofitKeepsApiPathBelowRootBaseUrl() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"isSuccess":true,"code":200,"message":"OK","result":{}}"""),
        )
        val api = createRetrofit(
            baseUrl = server.url("/").toString(),
            moshi = createMoshi(),
            okHttpClient = createOkHttpClient(debug = false),
        ).create(TestApi::class.java)

        api.profile()

        assertEquals("/api/user/profile", server.takeRequest().path)
    }
}

private interface TestApi {
    @GET("health")
    suspend fun health(): ResponseBody

    @GET("api/user/profile")
    suspend fun profile(): ResponseBody
}
