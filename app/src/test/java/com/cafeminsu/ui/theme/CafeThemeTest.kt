package com.cafeminsu.ui.theme

import androidx.compose.material3.lightColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CafeThemeTest {
    @Test
    fun cafeColorSchemeUsesCafeTokens() {
        val colors = cafeLightColors()
        val scheme = cafeColorScheme(colors)

        assertEquals(colors.primary, scheme.primary)
        assertEquals(colors.onPrimary, scheme.onPrimary)
        assertEquals(colors.canvas, scheme.background)
        assertEquals(colors.surfaceCard, scheme.surface)
        assertEquals(colors.ink, scheme.onSurface)
        assertEquals(colors.error, scheme.error)
    }

    @Test
    fun cafeColorSchemeDoesNotExposeMaterialDefaultPrimary() {
        assertNotEquals(lightColorScheme().primary, cafeColorScheme().primary)
    }
}
