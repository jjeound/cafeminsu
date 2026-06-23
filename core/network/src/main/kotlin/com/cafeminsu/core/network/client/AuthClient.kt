package com.cafeminsu.core.network.client

import com.cafeminsu.core.network.model.request.auth.KakaoLoginRequest
import com.cafeminsu.core.network.model.request.auth.SignupRequest
import com.cafeminsu.core.network.model.response.auth.KakaoLoginResponse
import com.cafeminsu.core.network.model.response.auth.NicknameCheckResponse
import com.cafeminsu.core.network.model.response.auth.RefreshResponse
import com.cafeminsu.core.network.model.response.auth.SignupResponse
import com.cafeminsu.core.network.model.response.auth.UserProfileResponse
import com.cafeminsu.core.network.service.AuthService
import com.skydoves.sandwich.ApiResponse
import javax.inject.Inject

class AuthClient @Inject constructor(
    private val authService: AuthService,
) {
    suspend fun kakaoLogin(request: KakaoLoginRequest): ApiResponse<KakaoLoginResponse> = authService.kakaoLogin(request)

    suspend fun refresh(refreshToken: String): ApiResponse<RefreshResponse> = authService.refresh(refreshToken)

    suspend fun getMyProfile(): ApiResponse<UserProfileResponse> = authService.getMyProfile()

    suspend fun checkNickname(nickname: String): ApiResponse<NicknameCheckResponse> = authService.checkNickname(nickname)

    suspend fun signup(request: SignupRequest): ApiResponse<SignupResponse> = authService.signup(request)
}
