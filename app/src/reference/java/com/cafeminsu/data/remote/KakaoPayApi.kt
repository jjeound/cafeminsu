package com.cafeminsu.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 카카오페이 결제 프록시 API. 앱은 카카오페이 서버를 직접 호출하지 않고 자사 백엔드 프록시만 호출한다.
 * 어드민키(시크릿)는 서버 전용이며 앱은 보관·로깅하지 않는다(SECURITY §3).
 */
interface KakaoPayApi {
    @POST("api/payments/kakaopay/ready")
    suspend fun ready(
        @Body request: KakaoPayReadyReq,
    ): KakaoPayReadyRes

    @POST("api/payments/kakaopay/approve")
    suspend fun approve(
        @Body request: KakaoPayApproveReq,
    ): KakaoPayApproveRes
}

@JsonClass(generateAdapter = true)
data class KakaoPayReadyReq(
    val merchantUid: String,
    val amount: Int,
)

@JsonClass(generateAdapter = true)
data class KakaoPayReadyRes(
    val tid: String?,
    val redirectUrl: String?,
)

@JsonClass(generateAdapter = true)
data class KakaoPayApproveReq(
    val tid: String,
    val pgToken: String,
    val merchantUid: String,
)

@JsonClass(generateAdapter = true)
data class KakaoPayApproveRes(
    // 기존 verify(impUid, merchantUid) 의 impUid 슬롯에 들어갈 PG 결제 토큰.
    val paymentToken: String?,
)
