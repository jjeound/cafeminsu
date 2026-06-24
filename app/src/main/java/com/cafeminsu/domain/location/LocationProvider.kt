package com.cafeminsu.domain.location

/**
 * 현재 기기 위치(위도, 경도)를 제공한다. 안드로이드 비종속 도메인 경계.
 * 권한이 없거나 위치를 알 수 없으면 null 을 반환한다(예외를 던지지 않는다).
 */
interface LocationProvider {
    suspend fun currentLatLng(): Pair<Double, Double>?
}
