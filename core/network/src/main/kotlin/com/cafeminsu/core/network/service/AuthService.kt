package com.cafeminsu.core.network.service

import com.cafeminsu.core.network.model.request.auth.KakaoLoginRequest
import com.cafeminsu.core.network.model.request.auth.SignupRequest
import com.cafeminsu.core.network.model.response.auth.KakaoLoginResponse
import com.cafeminsu.core.network.model.response.auth.NicknameCheckResponse
import com.cafeminsu.core.network.model.response.auth.RefreshResponse
import com.cafeminsu.core.network.model.response.auth.SignupResponse
import com.cafeminsu.core.network.model.response.auth.UserProfileResponse
import com.skydoves.sandwich.ApiResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthService {
    @POST("api/user/kakao-login")
    suspend fun kakaoLogin(@Body request: KakaoLoginRequest): ApiResponse<KakaoLoginResponse>

    @POST("api/user/refresh")
    suspend fun refresh(@Header("Refresh-Token") refreshToken: String): ApiResponse<RefreshResponse>

    @GET("api/user/profile")
    suspend fun getMyProfile(): ApiResponse<UserProfileResponse>

    @GET("api/user/nickname/check")
    suspend fun checkNickname(@Query("nickname") nickname: String): ApiResponse<NicknameCheckResponse>

    @POST("api/user/signup")
    suspend fun signup(@Body request: SignupRequest): ApiResponse<SignupResponse>
}
