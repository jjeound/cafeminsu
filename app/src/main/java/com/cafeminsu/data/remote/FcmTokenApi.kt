package com.cafeminsu.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.POST

interface FcmTokenApi {
    @POST("api/user/fcm-token")
    suspend fun updateFcmToken(
        @Body request: FcmTokenReq,
    ): BaseResponse<FcmTokenRes>
}

@JsonClass(generateAdapter = true)
data class FcmTokenReq(
    val fcmToken: String,
)

@JsonClass(generateAdapter = true)
data class FcmTokenRes(
    val ignored: String? = null,
)
