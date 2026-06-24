package com.cafeminsu.ui.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomBarVisibilityTest {
    @Test
    fun homeShowsBottomBar() {
        assertTrue(shouldShowBottomBar(Routes.HOME))
    }

    @Test
    fun storeShowsBottomBar() {
        assertTrue(shouldShowBottomBar(Routes.STORE))
    }

    @Test
    fun myShowsBottomBar() {
        assertTrue(shouldShowBottomBar(Routes.MY))
    }

    @Test
    fun menuHidesBottomBar() {
        assertFalse(shouldShowBottomBar(Routes.MENU))
    }

    @Test
    fun unknownRouteHidesBottomBar() {
        assertFalse(shouldShowBottomBar(null))
    }
}
