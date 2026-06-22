package com.cafeminsu.domain.scheduling

import org.junit.Assert.assertEquals
import org.junit.Test

class CongestionCalculatorTest {
    private val weights = SchedulingWeights()
    private val calculator = CongestionCalculator(weights)

    @Test
    fun noActiveOrdersIsLow() {
        assertEquals(CongestionLevel.Low, calculator.level(0))
    }

    @Test
    fun belowMidThresholdIsLow() {
        assertEquals(CongestionLevel.Low, calculator.level(weights.congestionMidThreshold - 1))
    }

    @Test
    fun atMidThresholdIsMid() {
        assertEquals(CongestionLevel.Mid, calculator.level(weights.congestionMidThreshold))
    }

    @Test
    fun belowHighThresholdIsMid() {
        assertEquals(CongestionLevel.Mid, calculator.level(weights.congestionHighThreshold - 1))
    }

    @Test
    fun atHighThresholdIsHigh() {
        assertEquals(CongestionLevel.High, calculator.level(weights.congestionHighThreshold))
    }

    @Test
    fun wellAboveHighThresholdIsHigh() {
        assertEquals(CongestionLevel.High, calculator.level(weights.congestionHighThreshold * 2))
    }
}
