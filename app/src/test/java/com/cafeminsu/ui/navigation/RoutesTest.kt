package com.cafeminsu.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class RoutesTest {
    @Test
    fun routesMatchMCodeContract() {
        assertEquals("m01", Routes.HOME)
        assertEquals("m02", Routes.MENU)
        assertEquals("m03/{menuItemId}", Routes.MENU_DETAIL)
        assertEquals("m04", Routes.VOICE)
        assertEquals("m05", Routes.CART)
        assertEquals("m06", Routes.PAYMENT)
        assertEquals("m07", Routes.ORDER_STATUS)
        assertEquals("m08", Routes.STAMP)
        assertEquals("m09", Routes.GIFTICON)
        assertEquals("m10", Routes.MY)
    }

    @Test
    fun menuDetailRouteCarriesMenuItemId() {
        assertEquals("m03/americano", Routes.menuDetail("americano"))
    }
}
