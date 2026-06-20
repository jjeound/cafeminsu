package com.cafeminsu.domain.auth

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.AuthState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginProviderTest {
    @Test
    fun exposesLoginProviderContract() = runBlocking {
        val provider = object : LoginProvider {
            override suspend fun login(): AppResult<AuthState> =
                AppResult.Success(AuthState.Guest)

            override suspend fun logout(): AppResult<Unit> =
                AppResult.Success(Unit)
        }

        assertTrue(provider.login() is AppResult.Success)
        assertTrue(provider.loginForServerExchange() is AppResult.Failure)
        assertTrue(provider.logout() is AppResult.Success)
    }

    @Test
    fun kakaoOAuthTokenDoesNotExposeRawValueInToString() {
        val token = KakaoOAuthToken("kakao-secret-token")

        assertEquals("kakao-secret-token", token.value)
        assertFalse(token.toString().contains("kakao-secret-token"))
    }
}
