package com.cafeminsu.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class ShapeTest {
    @Test
    fun shapesMatchDesignRadii() {
        assertEquals(RoundedCornerShape(8.dp), radiusSm)
        assertEquals(RoundedCornerShape(12.dp), radiusMd)
        assertEquals(RoundedCornerShape(16.dp), radiusLg)
        assertEquals(RoundedCornerShape(24.dp), radiusXl)
        assertEquals(RoundedCornerShape(999.dp), radiusPill)
    }

    @Test
    fun materialShapesUseCafeRadii() {
        val materialShapes = cafeMaterialShapes(cafeShapes())

        assertEquals(radiusSm, materialShapes.extraSmall)
        assertEquals(radiusMd, materialShapes.small)
        assertEquals(radiusLg, materialShapes.medium)
        assertEquals(radiusXl, materialShapes.large)
        assertEquals(radiusXl, materialShapes.extraLarge)
    }
}
