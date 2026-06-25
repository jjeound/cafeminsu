package com.cafeminsu.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MenuDetailDestinationTest {
    @Test
    fun fromHomeContinuesToOrderMenu() {
        assertEquals(Routes.MENU, menuDetailAddedDestination(Routes.HOME))
    }

    @Test
    fun fromMenuPopsBack() {
        assertNull(menuDetailAddedDestination(Routes.MENU))
    }

    @Test
    fun fromCartPopsBack() {
        assertNull(menuDetailAddedDestination(Routes.CART))
    }

    @Test
    fun fromHistoryContinuesToOrderMenu() {
        assertEquals(Routes.MENU, menuDetailAddedDestination(Routes.HISTORY))
    }

    @Test
    fun fromHistoryDetailContinuesToOrderMenu() {
        assertEquals(Routes.MENU, menuDetailAddedDestination(Routes.HISTORY_DETAIL))
    }

    @Test
    fun fromUnknownPopsBack() {
        assertNull(menuDetailAddedDestination(null))
    }
}
