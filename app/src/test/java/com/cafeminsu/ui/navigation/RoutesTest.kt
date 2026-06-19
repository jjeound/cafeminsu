package com.cafeminsu.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class RoutesTest {
    @Test
    fun routesMatchRedesignScreenCodeContract() {
        assertEquals("splash", Routes.SPLASH)
        assertEquals("login", Routes.LOGIN)
        assertEquals("home", Routes.HOME)
        assertEquals("noti", Routes.NOTI)
        assertEquals("store", Routes.STORE)
        assertEquals("store_detail", Routes.STORE_DETAIL)
        assertEquals("menu", Routes.MENU)
        assertEquals("menu_detail/{menuItemId}", Routes.MENU_DETAIL)
        assertEquals("voice", Routes.VOICE)
        assertEquals("cart", Routes.CART)
        assertEquals("pay/{orderId}", Routes.PAY)
        assertEquals("order_ok/{orderId}", Routes.ORDER_OK)
        assertEquals("order_fail", Routes.ORDER_FAIL)
        assertEquals("my", Routes.MY)
        assertEquals("coupon", Routes.COUPON)
        assertEquals("gift", Routes.GIFT)
        assertEquals("history", Routes.HISTORY)
    }

    @Test
    fun menuDetailRouteCarriesMenuItemId() {
        assertEquals("menu_detail/americano", Routes.menuDetail("americano"))
    }

    @Test
    fun payRouteCarriesOrderId() {
        assertEquals("pay/order-42", Routes.pay("order-42"))
    }

    @Test
    fun orderOkRouteCarriesOrderId() {
        assertEquals("order_ok/order-42", Routes.orderOk("order-42"))
    }

    @Test
    fun historyDetailRouteCarriesOrderId() {
        assertEquals("history/order-42", Routes.history("order-42"))
    }
}
