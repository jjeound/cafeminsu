package com.cafeminsu.ui.theme

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class TypeTest {
    @Test
    fun typographyMatchesDesignScale() {
        val typography = cafeTypography()

        assertEquals(32.sp, typography.display.fontSize)
        assertEquals(40.sp, typography.display.lineHeight)
        assertEquals(FontWeight.Black, typography.display.fontWeight)
        assertEquals(ink, typography.display.color)

        assertEquals(24.sp, typography.h1.fontSize)
        assertEquals(32.sp, typography.h1.lineHeight)
        assertEquals(FontWeight.Bold, typography.h1.fontWeight)

        assertEquals(20.sp, typography.h2.fontSize)
        assertEquals(28.sp, typography.h2.lineHeight)
        assertEquals(FontWeight.Bold, typography.h2.fontWeight)

        assertEquals(17.sp, typography.h3.fontSize)
        assertEquals(24.sp, typography.h3.lineHeight)
        assertEquals(FontWeight.Medium, typography.h3.fontWeight)

        assertEquals(16.sp, typography.bodyL.fontSize)
        assertEquals(24.sp, typography.bodyL.lineHeight)
        assertEquals(FontWeight.Medium, typography.bodyL.fontWeight)
        assertEquals(body, typography.bodyL.color)

        assertEquals(14.sp, typography.body.fontSize)
        assertEquals(20.sp, typography.body.lineHeight)
        assertEquals(FontWeight.Normal, typography.body.fontWeight)

        assertEquals(12.sp, typography.caption.fontSize)
        assertEquals(16.sp, typography.caption.lineHeight)
        assertEquals(muted, typography.caption.color)

        assertEquals(12.sp, typography.meta.fontSize)
        assertEquals(16.sp, typography.meta.lineHeight)
        assertEquals(mutedSoft, typography.meta.color)
    }
}
