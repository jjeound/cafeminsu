package com.ssafy.cafeminsu.core.network.di

import com.ssafy.cafeminsu.core.datastore.SessionPreferencesDataSource
import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.firstOrNull

class AuthorizationInterceptor @Inject constructor(
    private val sessionPreferences: SessionPreferencesDataSource,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.isAuthEndpoint()) return chain.proceed(request)

        val accessToken = runBlocking { sessionPreferences.tokens.firstOrNull()?.accessToken }
            ?.takeIf { it.isNotBlank() }
            ?: return chain.proceed(request)

        val authorizedRequest = request.newBuilder()
            .header(AUTHORIZATION_HEADER, "Bearer $accessToken")
            .build()

        return chain.proceed(authorizedRequest)
    }

    private fun Request.isAuthEndpoint(): Boolean = url.encodedPath in AuthEndpointPaths

    private companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private val AuthEndpointPaths = setOf(
            "/api/user/kakao-login",
            "/api/user/owner-login",
            "/api/user/refresh",
            "/api/user/logout",
        )
    }
}
