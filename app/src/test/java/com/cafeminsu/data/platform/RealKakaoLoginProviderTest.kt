package com.cafeminsu.data.platform

import com.cafeminsu.domain.model.AuthState
import org.junit.Assert.assertEquals
import org.junit.Test

class RealKakaoLoginProviderTest {
    @Test
    fun mapsKakaoProfileToDomainUserWithoutTokenValues() {
        val authState = kakaoProfileToAuthenticatedState(
            kakaoId = "12345",
            nickname = "지원",
        )

        val user = (authState as AuthState.Authenticated).user
        assertEquals("kakao-12345", user.id)
        assertEquals("지원", user.displayName)
        assertEquals(null, user.phoneLast4)
    }

    @Test
    fun blankKakaoProfileFallsBackToSafeDisplayValues() {
        val authState = kakaoProfileToAuthenticatedState(
            kakaoId = null,
            nickname = " ",
        )

        val user = (authState as AuthState.Authenticated).user
        assertEquals("kakao-user", user.id)
        assertEquals("카카오 사용자", user.displayName)
    }
}
