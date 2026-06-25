package com.ssafy.cafeminsu.core.network.service

import com.ssafy.cafeminsu.core.network.model.request.auth.KakaoLoginRequest
import com.ssafy.cafeminsu.core.network.model.request.auth.LocationRequest
import com.ssafy.cafeminsu.core.network.model.request.auth.NicknameUpdateRequest
import com.ssafy.cafeminsu.core.network.model.request.auth.OwnerLoginRequest
import com.ssafy.cafeminsu.core.network.model.request.auth.SignupRequest
import com.ssafy.cafeminsu.core.network.model.request.auth.FcmTokenRequest
import com.ssafy.cafeminsu.core.network.model.response.auth.KakaoLoginResponse
import com.ssafy.cafeminsu.core.network.model.response.auth.NicknameCheckResponse
import com.ssafy.cafeminsu.core.network.model.response.auth.RefreshResponse
import com.ssafy.cafeminsu.core.network.model.response.auth.SignupResponse
import com.ssafy.cafeminsu.core.network.model.response.auth.UserProfileResponse
import com.ssafy.cafeminsu.core.network.model.response.auth.LocationResponse
import com.ssafy.cafeminsu.core.network.model.response.auth.NicknameUpdateResponse
import com.ssafy.cafeminsu.core.network.model.response.auth.OwnerLoginResponse
import com.ssafy.cafeminsu.core.network.model.response.auth.PublicUserProfileResponse
import com.ssafy.cafeminsu.core.network.model.response.auth.UserRoleResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.PATCH
import retrofit2.http.Path

interface AuthService {
    @POST("api/user/kakao-login")
    suspend fun kakaoLogin(@Body request: KakaoLoginRequest): KakaoLoginResponse

    @POST("api/user/refresh")
    suspend fun refresh(@Header("Refresh-Token") refreshToken: String): RefreshResponse

    @GET("api/user/profile")
    suspend fun getMyProfile(): UserProfileResponse

    @GET("api/user/profile/{userId}")
    suspend fun getUserProfile(@Path("userId") userId: Long): PublicUserProfileResponse

    @GET("api/user/nickname/check")
    suspend fun checkNickname(@Query("nickname") nickname: String): NicknameCheckResponse

    @POST("api/user/signup")
    suspend fun signup(@Body request: SignupRequest): SignupResponse

    @PATCH("api/user/nickname")
    suspend fun updateNickname(@Body request: NicknameUpdateRequest): NicknameUpdateResponse

    @GET("api/user/location")
    suspend fun getLocation(): LocationResponse

    @POST("api/user/location")
    suspend fun saveLocation(@Body request: LocationRequest)

    @POST("api/user/fcm-token")
    suspend fun updateFcmToken(@Body request: FcmTokenRequest)

    @POST("api/user/owner-login")
    suspend fun ownerLogin(@Body request: OwnerLoginRequest): OwnerLoginResponse

    @POST("api/user/logout")
    suspend fun logout(@Header("Refresh-Token") refreshToken: String)

    @POST("api/user/become-owner")
    suspend fun becomeOwner(): UserRoleResponse
}
