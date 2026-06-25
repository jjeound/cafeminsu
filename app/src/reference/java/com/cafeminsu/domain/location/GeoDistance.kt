package com.cafeminsu.domain.location

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** 지구 평균 반지름(m). 하버사인 거리 계산의 단일 상수. */
private const val EarthRadiusMeters = 6_371_000.0

/**
 * 두 위경도 좌표 사이의 대권 거리(m)를 하버사인 공식으로 계산한다(반올림한 정수 m).
 * 안드로이드 비종속 순수 함수.
 */
fun haversineMeters(
    lat1: Double,
    lng1: Double,
    lat2: Double,
    lng2: Double,
): Int {
    val lat1Rad = Math.toRadians(lat1)
    val lat2Rad = Math.toRadians(lat2)
    val deltaLatRad = Math.toRadians(lat2 - lat1)
    val deltaLngRad = Math.toRadians(lng2 - lng1)

    val sinDeltaLatHalf = sin(deltaLatRad / 2)
    val sinDeltaLngHalf = sin(deltaLngRad / 2)
    val a = sinDeltaLatHalf * sinDeltaLatHalf +
        cos(lat1Rad) * cos(lat2Rad) * sinDeltaLngHalf * sinDeltaLngHalf
    // asin 입력은 부동소수 오차로 1.0 을 약간 넘을 수 있어 clamp 한다.
    val c = 2 * asin(min(1.0, sqrt(a)))

    return Math.round(EarthRadiusMeters * c).toInt()
}
