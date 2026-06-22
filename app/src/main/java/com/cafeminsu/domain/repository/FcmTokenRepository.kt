package com.cafeminsu.domain.repository

import com.cafeminsu.core.AppResult

/**
 * 디바이스 FCM 토큰을 서버(`POST api/user/fcm-token`)에 등록/갱신한다.
 * 인증(Bearer)이 필요한 호출이며, 실패는 예외 전파 없이 [AppResult.Failure]로 표현한다.
 */
interface FcmTokenRepository {
    suspend fun register(token: String): AppResult<Unit>
}
