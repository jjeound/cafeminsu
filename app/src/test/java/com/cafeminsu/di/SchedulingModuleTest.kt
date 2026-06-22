package com.cafeminsu.di

import com.cafeminsu.domain.scheduling.RulePrepTimeEstimator
import com.cafeminsu.domain.scheduling.SchedulingWeights
import com.cafeminsu.domain.time.SystemClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SchedulingModuleTest {
    @Test
    fun providesDefaultWeights() {
        assertEquals(SchedulingWeights(), SchedulingModule.provideSchedulingWeights())
    }

    @Test
    fun providesSystemClock() {
        val clock = SchedulingModule.provideClock()

        assertTrue(clock is SystemClock)
        assertTrue("시스템 시계는 양수 시각을 반환", clock.nowMillis() > 0L)
    }

    @Test
    fun providesRulePrepTimeEstimatorUsingWeights() {
        val weights = SchedulingModule.provideSchedulingWeights()

        val estimator = SchedulingModule.providePrepTimeEstimator(weights)

        assertTrue(estimator is RulePrepTimeEstimator)
    }
}
