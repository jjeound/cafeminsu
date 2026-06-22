package com.cafeminsu.domain.messaging

/**
 * 현재 디바이스의 푸시 메시징 토큰(FCM)을 제공한다.
 * 토큰을 얻을 수 없는 환경(미초기화·권한·하드웨어 부재 등)에서는 예외 대신 null 을 반환한다.
 */
interface DeviceMessagingTokenProvider {
    suspend fun currentToken(): String?
}
