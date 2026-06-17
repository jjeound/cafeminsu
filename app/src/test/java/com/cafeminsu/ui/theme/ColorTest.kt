package com.cafeminsu.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ColorTest {
    @Test
    fun allColorTokensAreSpecified() {
        val tokens = listOf(
            canvas,
            ink,
            body,
            muted,
            mutedSoft,
            primary,
            surfaceCard,
            surfaceDark,
            hairline,
            primaryHover,
            accentSoft,
            onPrimary,
            onDark,
            success,
            warning,
            error,
        )

        tokens.forEach { token ->
            assertNotEquals(Color.Unspecified, token)
        }
    }
}
