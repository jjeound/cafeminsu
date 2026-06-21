package com.cafeminsu.ui.feature.store

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreMapViewTest {
    @Test
    fun blankKeyFallsBackToPlaceholder() {
        assertFalse(shouldRenderKakaoMap(""))
        assertFalse(shouldRenderKakaoMap("   "))
    }

    @Test
    fun nonBlankKeyRendersRealMap() {
        assertTrue(shouldRenderKakaoMap("085f354e712de993f539fe4561803efe"))
    }
}
