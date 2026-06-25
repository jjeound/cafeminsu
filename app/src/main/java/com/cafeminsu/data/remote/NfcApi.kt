package com.cafeminsu.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.POST

interface NfcApi {
    @POST("api/nfc/claim")
    suspend fun claim(
        @Body request: NfcClaimReq,
    ): NfcClaimRes
}

@JsonClass(generateAdapter = true)
data class NfcClaimReq(
    val tagCode: String,
)

@JsonClass(generateAdapter = true)
data class NfcClaimRes(
    val gifticonId: Long?,
    val amount: Int?,
    val expiresAt: String?,
    val message: String?,
)

/** 에러 공통 포맷. HTTP status 로 큰 분류, body `code` 로 세부 분기(400 두 케이스 구분에 필수). */
@JsonClass(generateAdapter = true)
data class NfcErrorBody(
    val code: String?,
    val message: String?,
)
