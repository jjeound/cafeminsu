package com.ssafy.cafeminsu.core.network.client

import com.ssafy.cafeminsu.core.network.model.request.auth.KakaoLoginRequest
import com.ssafy.cafeminsu.core.network.model.request.auth.SignupRequest
import com.ssafy.cafeminsu.core.network.model.response.auth.KakaoLoginResponse
import com.ssafy.cafeminsu.core.network.model.response.auth.NicknameCheckResponse
import com.ssafy.cafeminsu.core.network.model.response.auth.RefreshResponse
import com.ssafy.cafeminsu.core.network.model.response.auth.SignupResponse
import com.ssafy.cafeminsu.core.network.model.response.auth.UserProfileResponse
import com.ssafy.cafeminsu.core.network.service.AuthService
import javax.inject.Inject

class AuthClient @Inject constructor(
    private val authService: AuthService,
) {
    suspend fun kakaoLogin(request: KakaoLoginRequest): KakaoLoginResponse = authService.kakaoLogin(request)

    suspend fun refresh(refreshToken: String): RefreshResponse = authService.refresh(refreshToken)

    suspend fun getMyProfile(): UserProfileResponse = authService.getMyProfile()

    suspend fun checkNickname(nickname: String): NicknameCheckResponse = authService.checkNickname(nickname)

    suspend fun signup(request: SignupRequest): SignupResponse = authService.signup(request)
}
