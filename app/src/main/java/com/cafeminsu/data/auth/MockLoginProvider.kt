package com.cafeminsu.data.auth

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.auth.KakaoOAuthToken
import com.cafeminsu.domain.auth.LoginProvider
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.UserProfile
import javax.inject.Inject

class MockLoginProvider @Inject constructor() : LoginProvider {
    override suspend fun login(): AppResult<AuthState> =
        AppResult.Success(demoAuthenticatedState())

    override suspend fun loginForServerExchange(): AppResult<KakaoOAuthToken> =
        AppResult.Success(KakaoOAuthToken("mock-kakao-access-token"))

    override suspend fun logout(): AppResult<Unit> =
        AppResult.Success(Unit)

    private fun demoAuthenticatedState(): AuthState.Authenticated =
        AuthState.Authenticated(
            UserProfile(
                id = "demo-user",
                displayName = "민수",
                phoneLast4 = "0000",
            ),
        )
}
