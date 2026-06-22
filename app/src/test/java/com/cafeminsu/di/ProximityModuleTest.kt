package com.cafeminsu.di

import com.cafeminsu.data.proximity.SimulatedProximityScanner
import com.cafeminsu.domain.proximity.ProximityScanner
import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProximityModuleTest {
    @Test
    fun `proximity module binds simulated scanner to proximity scanner contract`() {
        val method = ProximityModule::class.java.getDeclaredMethod(
            "bindProximityScanner",
            SimulatedProximityScanner::class.java,
        )

        // 기본 바인딩은 에뮬레이터/CI 안전한 시뮬레이터다(실 BLE 는 빌드플래그로 교체).
        assertTrue(Modifier.isAbstract(method.modifiers))
        assertEquals(ProximityScanner::class.java, method.returnType)
    }
}
