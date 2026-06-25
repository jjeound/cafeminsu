package com.cafeminsu.domain.time

/**
 * 현재 시각 추상화. 스케줄링/ETA 계산을 결정론적으로 테스트하기 위해 시간 접근을 인터페이스 뒤로 둔다.
 * 도메인은 안드로이드에 의존하지 않으므로 표준 JVM 시계만 사용한다.
 */
interface Clock {
    fun nowMillis(): Long
}

/** 운영 환경 기본 구현. 테스트는 고정값을 반환하는 가짜 [Clock]을 사용한다. */
class SystemClock : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
