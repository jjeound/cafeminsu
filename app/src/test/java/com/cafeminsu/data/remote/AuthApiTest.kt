package com.cafeminsu.data.remote

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.auth.SessionTokens
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthApiTest {
    @Test
    fun kakaoLoginResponseMapsToTokensAndAuthenticatedState() {
        val response = KakaoLoginRes(
            accessToken = "access-token",
            refreshToken = "refresh-token",
            isNewUser = false,
            nickname = "지원",
        )

        val result = response.toLoginExchange()

        assertTrue(result is AppResult.Success)
        val exchange = (result as AppResult.Success).data
        assertEquals(SessionTokens("access-token", "refresh-token"), exchange.tokens)
        assertEquals("지원", exchange.authState.user.displayName)
        assertEquals(UserRole.Customer, exchange.authState.role)
    }

    @Test
    fun userProfileResponseMapsServerIdAndOwnerRole() {
        val response = UserProfileRes(
            id = 42,
            nickname = "점주",
            profileImageUrl = null,
            role = "OWNER",
        )

        val authState = response.toAuthenticatedState()

        assertEquals("42", authState.user.id)
        assertEquals("점주", authState.user.displayName)
        assertEquals(UserRole.Owner, authState.role)
    }

    @Test
    fun failedBaseResponseMapsCodeToDomainError() {
        val response = BaseResponse<KakaoLoginRes>(
            isSuccess = false,
            code = 401,
            message = "unauthorized",
            result = null,
        )

        val result = response.unwrap { it.toLoginExchange() }

        assertEquals(AppResult.Failure(DomainError.Unauthorized), result)
    }
}
