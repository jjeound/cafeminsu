package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.auth.SessionStateHolder
import com.cafeminsu.data.auth.SessionTokenStore
import com.cafeminsu.data.remote.AuthApi
import com.cafeminsu.data.remote.KakaoLoginReq
import com.cafeminsu.data.remote.runCatchingToAppResult
import com.cafeminsu.data.remote.toAccessToken
import com.cafeminsu.data.remote.toLoginExchange
import com.cafeminsu.data.remote.unwrap
import com.cafeminsu.domain.auth.LoginProvider
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.UserProfile
import com.cafeminsu.domain.repository.SessionRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class RealSessionRepository @Inject constructor(
    private val loginProvider: LoginProvider,
    private val authApi: AuthApi,
    private val tokenStore: SessionTokenStore,
    private val sessionStateHolder: SessionStateHolder,
) : SessionRepository {
    override fun observeAuthState(): Flow<AuthState> =
        sessionStateHolder.observe()

    override suspend fun login(): AppResult<AuthState> {
        val kakaoToken = when (val result = loginProvider.loginForServerExchange()) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> return result
        }

        val exchangeResult = when (
            val response = runCatchingToAppResult {
                authApi.kakaoLogin(KakaoLoginReq(accessToken = kakaoToken.value))
            }
        ) {
            is AppResult.Success -> response.data.unwrap { it.toLoginExchange() }
            is AppResult.Failure -> response
        }

        return when (exchangeResult) {
            is AppResult.Success -> {
                tokenStore.save(exchangeResult.data.tokens)
                sessionStateHolder.update(exchangeResult.data.authState)
                AppResult.Success(exchangeResult.data.authState)
            }

            is AppResult.Failure -> exchangeResult
        }
    }

    override suspend fun refreshOnce(): AppResult<AuthState> {
        val tokens = tokenStore.read()
            ?: return AppResult.Success(AuthState.Guest).also {
                sessionStateHolder.update(AuthState.Guest)
            }

        val refreshResult = when (
            val response = runCatchingToAppResult {
                authApi.refresh(tokens.refreshToken)
            }
        ) {
            is AppResult.Success -> response.data.unwrap { it.toAccessToken() }
            is AppResult.Failure -> response
        }

        return when (refreshResult) {
            is AppResult.Success -> {
                tokenStore.updateAccessToken(refreshResult.data)
                val authState = sessionStateHolder.authState.value
                    .takeIf { it is AuthState.Authenticated }
                    ?: defaultAuthenticatedState()
                sessionStateHolder.update(authState)
                AppResult.Success(authState)
            }

            is AppResult.Failure -> {
                if (refreshResult.error == DomainError.Unauthorized) {
                    expireSession()
                    AppResult.Success(AuthState.Expired)
                } else {
                    refreshResult
                }
            }
        }
    }

    override suspend fun logout(): AppResult<Unit> {
        tokenStore.clear()
        sessionStateHolder.update(AuthState.Guest)
        return loginProvider.logout()
    }

    override suspend fun clearSession(): AppResult<Unit> {
        tokenStore.clear()
        sessionStateHolder.update(AuthState.Guest)
        return AppResult.Success(Unit)
    }

    private suspend fun expireSession() {
        tokenStore.clear()
        sessionStateHolder.update(AuthState.Expired)
    }

    private fun defaultAuthenticatedState(): AuthState.Authenticated =
        AuthState.Authenticated(
            user = UserProfile(
                id = "server-user",
                displayName = "카페민수 사용자",
                phoneLast4 = null,
            ),
        )
}
