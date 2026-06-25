package com.ssafy.cafeminsu.core.data.kakao

interface KakaoAuthDataSource {
    suspend fun getAccessToken(): String
    suspend fun logout()
}
