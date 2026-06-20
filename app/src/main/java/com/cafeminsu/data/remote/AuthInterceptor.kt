package com.cafeminsu.data.remote

import com.cafeminsu.core.AppResult
import com.cafeminsu.data.auth.SessionStateHolder
import com.cafeminsu.data.auth.SessionTokenStore
import com.cafeminsu.data.auth.SessionTokens
import com.cafeminsu.domain.model.AuthState
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class AuthorizationInterceptor @Inject constructor(
    private val tokenStore: SessionTokenStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.isAuthEndpoint()) {
            return chain.proceed(request)
        }

        val accessToken = runBlocking { tokenStore.read()?.accessToken }
            ?: return chain.proceed(request)

        return chain.proceed(
            request.newBuilder()
                .header(AuthorizationHeader, "Bearer $accessToken")
                .build(),
        )
    }
}

class SessionAuthenticator @Inject constructor(
    private val tokenStore: SessionTokenStore,
    private val authApi: AuthApi,
    private val sessionStateHolder: SessionStateHolder,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.isAuthEndpoint() || response.priorResponseCount() > MaxAuthAttempts) {
            return expireSession()
        }

        val tokens = runBlocking { tokenStore.read() } ?: return expireSession()
        val accessToken = refreshAccessToken(tokens) ?: return expireSession()

        return response.request.newBuilder()
            .header(AuthorizationHeader, "Bearer $accessToken")
            .build()
    }

    private fun refreshAccessToken(tokens: SessionTokens): String? {
        val response = runBlocking {
            runCatchingToAppResult {
                authApi.refresh(tokens.refreshToken)
            }
        }

        val result = when (response) {
            is AppResult.Success -> response.data.unwrap { it.toAccessToken() }
            is AppResult.Failure -> response
        }

        return when (result) {
            is AppResult.Success -> {
                runBlocking { tokenStore.updateAccessToken(result.data) }
                result.data
            }

            is AppResult.Failure -> null
        }
    }

    private fun expireSession(): Request? {
        runBlocking { tokenStore.clear() }
        sessionStateHolder.update(AuthState.Expired)
        return null
    }

    private fun Response.priorResponseCount(): Int {
        var response: Response? = this
        var result = 1
        while (response?.priorResponse != null) {
            result += 1
            response = response.priorResponse
        }
        return result
    }

    private companion object {
        const val MaxAuthAttempts = 1
    }
}

private fun Request.isAuthEndpoint(): Boolean =
    url.encodedPath in AuthEndpointPaths

private val AuthEndpointPaths = setOf(
    "/api/user/kakao-login",
    "/api/user/refresh",
    "/api/user/logout",
)

private const val AuthorizationHeader = "Authorization"
