package com.cafeminsu.ui.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class SpacingTest {
    @Test
    fun spacingMatchesFourPointGridTokens() {
        assertEquals(4.dp, space1)
        assertEquals(8.dp, space2)
        assertEquals(12.dp, space3)
        assertEquals(16.dp, space4)
        assertEquals(20.dp, space5)
        assertEquals(24.dp, space6)
        assertEquals(32.dp, space8)
        assertEquals(40.dp, space10)
        assertEquals(56.dp, space14)
        assertEquals(72.dp, space18)
    }
}
