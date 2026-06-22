package com.cafeminsu.domain.proximity

import org.junit.Assert.assertEquals
import org.junit.Test

class ProximityModelsTest {
    @Test
    fun toProximityInputCopiesArrivalAndRssi() {
        val signal = ProximitySignal(
            orderId = "order-1",
            rssi = -62,
            estimatedArrivalSeconds = 45,
            atMillis = 1_000L,
        )

        val input = signal.toProximityInput()

        assertEquals(45, input.estimatedArrivalSeconds)
        assertEquals(-62, input.rssi)
    }

    @Test
    fun toProximityInputClampsNegativeArrivalToZero() {
        // 이미 도착(음수 도착초)은 0으로 보정해 스케줄러가 안전하게 처리하도록 한다.
        val signal = ProximitySignal(
            orderId = "order-2",
            rssi = -48,
            estimatedArrivalSeconds = -30,
            atMillis = 2_000L,
        )

        val input = signal.toProximityInput()

        assertEquals(0, input.estimatedArrivalSeconds)
        assertEquals(-48, input.rssi)
    }
}
