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
        assertEquals(false, exchange.authState.isNewUser)
    }

    @Test
    fun kakaoLoginResponseCarriesNewUserSignalToAuthenticatedState() {
        val response = KakaoLoginRes(
            accessToken = "access-token",
            refreshToken = "refresh-token",
            isNewUser = true,
            nickname = null,
        )

        val result = response.toLoginExchange()

        assertTrue(result is AppResult.Success)
        val exchange = (result as AppResult.Success).data
        assertEquals(true, exchange.authState.isNewUser)
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
        assertEquals(false, authState.isNewUser)
    }

    @Test
    fun ownerLoginResponseMapsToTokensAndOwnerProfileWithStoreNameFromNickname() {
        val response = OwnerLoginRes(
            accessToken = "owner-access",
            refreshToken = "owner-refresh",
            nickname = "강남점장",
        )

        val result = response.toOwnerLoginExchange(loginId = "owner02")

        assertTrue(result is AppResult.Success)
        val exchange = (result as AppResult.Success).data
        assertEquals(SessionTokens("owner-access", "owner-refresh"), exchange.tokens)
        assertEquals("owner02", exchange.ownerProfile.loginId)
        assertEquals("강남점장", exchange.ownerProfile.storeName)
        assertEquals(true, exchange.ownerProfile.isStoreOpen)
    }

    @Test
    fun ownerLoginResponseFallsBackToLoginIdWhenNicknameBlank() {
        val response = OwnerLoginRes(
            accessToken = "owner-access",
            refreshToken = "owner-refresh",
            nickname = "  ",
        )

        val result = response.toOwnerLoginExchange(loginId = "owner02")

        assertTrue(result is AppResult.Success)
        assertEquals("owner02", (result as AppResult.Success).data.ownerProfile.storeName)
    }

    @Test
    fun ownerLoginResponseWithMissingTokenMapsToUnknownError() {
        val response = OwnerLoginRes(
            accessToken = null,
            refreshToken = "owner-refresh",
            nickname = "강남점장",
        )

        assertEquals(
            AppResult.Failure(DomainError.Unknown),
            response.toOwnerLoginExchange(loginId = "owner02"),
        )
    }

    @Test
    fun nicknameCheckResponseMapsAvailability() {
        val available = NicknameCheckRes(available = true)
        val duplicated = NicknameCheckRes(available = false)

        assertEquals(AppResult.Success(true), available.toAvailability())
        assertEquals(AppResult.Success(false), duplicated.toAvailability())
    }

    @Test
    fun signupResponseMapsToCompletedAuthenticatedState() {
        val response = SignupRes(userId = 77, nickname = "새민수")

        val authState = response.toAuthenticatedState()

        assertEquals("77", authState.user.id)
        assertEquals("새민수", authState.user.displayName)
        assertEquals(UserRole.Customer, authState.role)
        assertEquals(false, authState.isNewUser)
    }
}
