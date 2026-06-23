package com.cafeminsu.domain.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockTest {
    @Test
    fun systemClockReturnsCurrentTime() {
        val before = System.currentTimeMillis()
        val now = SystemClock().nowMillis()
        val after = System.currentTimeMillis()

        assertTrue("시스템 시계는 호출 시점 범위 안의 값을 반환해야 한다", now in before..after)
    }

    @Test
    fun fakeClockReturnsFixedValueForDeterministicTests() {
        val fixed = 1_750_000_000_000L
        val clock: Clock = object : Clock {
            override fun nowMillis(): Long = fixed
        }

        assertEquals(fixed, clock.nowMillis())
    }
}
