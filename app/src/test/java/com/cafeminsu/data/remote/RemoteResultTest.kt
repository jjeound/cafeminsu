package com.cafeminsu.data.remote

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.http.GET

class RemoteResultTest {
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
    fun throwableMapsIoExceptionsToNetworkErrors() {
        assertEquals(DomainError.Network, IOException("offline").toDomainError())
    }

    @Test
    fun throwableMapsSocketTimeoutBeforeGenericIoException() {
        assertEquals(DomainError.Timeout, SocketTimeoutException("timeout").toDomainError())
    }

    @Test
    fun throwableMapsHttpStatusCodesToDomainErrors() {
        assertEquals(DomainError.Unauthorized, httpException(401).toDomainError())
        assertEquals(DomainError.NotFound, httpException(404).toDomainError())
        assertEquals(DomainError.Unknown, httpException(500).toDomainError())
    }

    @Test
    fun runCatchingToAppResultWrapsSuccess() = runTest {
        val result = runCatchingToAppResult { "ok" }

        assertEquals(AppResult.Success("ok"), result)
    }

    @Test
    fun runCatchingToAppResultMapsRetrofitHttpExceptionWithoutThrowing() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))
        val api = createRetrofit(
            baseUrl = server.url("/").toString(),
            moshi = createMoshi(),
            okHttpClient = createOkHttpClient(debug = false),
        ).create(ProtectedApi::class.java)

        val result = runCatchingToAppResult { api.profile() }

        assertEquals(AppResult.Failure(DomainError.Unauthorized), result)
        assertEquals("/api/user/profile", server.takeRequest().path)
    }
}

private fun httpException(code: Int): HttpException =
    HttpException(
        Response.error<Unit>(
            code,
            "error".toResponseBody("application/json".toMediaType()),
        ),
    )

private interface ProtectedApi {
    @GET("api/user/profile")
    suspend fun profile(): ResponseBody
}
