package com.cafeminsu.ui.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class ElevationTest {
    @Test
    fun elevationTokensMatchDesignShadows() {
        assertEquals(1.dp, elevCard.offsetY)
        assertEquals(2.dp, elevCard.blurRadius)
        assertEquals(ink.copy(alpha = 0.04f), elevCard.color)

        assertEquals(8.dp, elevOverlay.offsetY)
        assertEquals(24.dp, elevOverlay.blurRadius)
        assertEquals(ink.copy(alpha = 0.08f), elevOverlay.color)
    }
}
