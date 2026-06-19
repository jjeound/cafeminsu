package com.cafeminsu.ui.theme

import androidx.compose.material3.lightColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CafeColorsTest {
    @Test
    fun cafeLightColorsMapsDesignTokens() {
        val colors = cafeLightColors()

        assertEquals(canvas, colors.canvas)
        assertEquals(ink, colors.ink)
        assertEquals(body, colors.body)
        assertEquals(muted, colors.muted)
        assertEquals(mutedSoft, colors.mutedSoft)
        assertEquals(primary, colors.primary)
        assertEquals(surfaceCard, colors.surfaceCard)
        assertEquals(surfaceDark, colors.surfaceDark)
        assertEquals(hairline, colors.hairline)
        assertEquals(primaryHover, colors.primaryHover)
        assertEquals(accentSoft, colors.accentSoft)
        assertEquals(onPrimary, colors.onPrimary)
        assertEquals(onDark, colors.onDark)
        assertEquals(success, colors.success)
        assertEquals(warning, colors.warning)
        assertEquals(error, colors.error)
        assertEquals(kakaoYellow, colors.kakaoYellow)
    }

    @Test
    fun primaryTokenDoesNotUseMaterialDefaultPrimary() {
        assertNotEquals(lightColorScheme().primary, cafeLightColors().primary)
    }
}
