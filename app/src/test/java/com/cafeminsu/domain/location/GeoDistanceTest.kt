package com.cafeminsu.domain.location

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoDistanceTest {
    @Test
    fun identicalPointsHaveZeroDistance() {
        val meters = haversineMeters(
            lat1 = 37.5665,
            lng1 = 126.9780,
            lat2 = 37.5665,
            lng2 = 126.9780,
        )

        assertEquals(0, meters)
    }

    @Test
    fun seoulToBusanIsAboutThreeHundredTwentyFiveKilometers() {
        // 서울시청(37.5665, 126.9780) ↔ 부산시청(35.1796, 129.0756) ≈ 325km.
        val meters = haversineMeters(
            lat1 = 37.5665,
            lng1 = 126.9780,
            lat2 = 35.1796,
            lng2 = 129.0756,
        )

        val expectedMeters = 325_000
        val toleranceMeters = 5_000
        assertTrue(
            "expected≈$expectedMeters but was $meters",
            abs(meters - expectedMeters) <= toleranceMeters,
        )
    }

    @Test
    fun distanceIsSymmetric() {
        val forward = haversineMeters(
            lat1 = 37.5665,
            lng1 = 126.9780,
            lat2 = 35.1796,
            lng2 = 129.0756,
        )
        val backward = haversineMeters(
            lat1 = 35.1796,
            lng1 = 129.0756,
            lat2 = 37.5665,
            lng2 = 126.9780,
        )

        assertEquals(forward, backward)
    }
}
