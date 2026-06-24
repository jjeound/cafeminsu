package com.cafeminsu.domain.location

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocationProviderTest {
    @Test
    fun returnsConfiguredLatLngWhenAvailable() = runTest {
        val provider: LocationProvider = FakeLocationProvider(37.5665 to 126.9780)

        assertEquals(37.5665 to 126.9780, provider.currentLatLng())
    }

    @Test
    fun returnsNullWhenLocationUnavailable() = runTest {
        val provider: LocationProvider = FakeLocationProvider(null)

        assertNull(provider.currentLatLng())
    }

    private class FakeLocationProvider(
        private val latLng: Pair<Double, Double>?,
    ) : LocationProvider {
        override suspend fun currentLatLng(): Pair<Double, Double>? = latLng
    }
}
