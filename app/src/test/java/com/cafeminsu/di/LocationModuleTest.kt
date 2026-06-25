package com.cafeminsu.di

import com.cafeminsu.data.location.AndroidLocationProvider
import com.cafeminsu.domain.location.LocationProvider
import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationModuleTest {
    @Test
    fun `location module binds android provider to location provider contract`() {
        val method = LocationModule::class.java.getDeclaredMethod(
            "bindLocationProvider",
            AndroidLocationProvider::class.java,
        )

        assertTrue(Modifier.isAbstract(method.modifiers))
        assertEquals(LocationProvider::class.java, method.returnType)
    }
}
