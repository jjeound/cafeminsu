package com.cafeminsu.ui.navigation

object Routes {
    const val HOME = "m01"
    const val MENU = "m02"
    const val MENU_DETAIL_MENU_ID = "menuItemId"
    private const val MENU_DETAIL_BASE = "m03"
    const val MENU_DETAIL = "$MENU_DETAIL_BASE/{$MENU_DETAIL_MENU_ID}"
    const val VOICE = "m04"
    const val CART = "m05"
    const val PAYMENT_ORDER_ID = "orderId"
    private const val PAYMENT_BASE = "m06"
    const val PAYMENT = "$PAYMENT_BASE/{$PAYMENT_ORDER_ID}"
    const val ORDER_STATUS_ORDER_ID = "orderId"
    private const val ORDER_STATUS_BASE = "m07"
    const val ORDER_STATUS = "$ORDER_STATUS_BASE/{$ORDER_STATUS_ORDER_ID}"
    const val STAMP = "m08"
    const val GIFTICON = "m09"
    const val MY = "m10"

    fun menuDetail(menuItemId: String): String = "$MENU_DETAIL_BASE/$menuItemId"
    fun payment(orderId: String): String = "$PAYMENT_BASE/$orderId"
    fun orderStatus(orderId: String): String = "$ORDER_STATUS_BASE/$orderId"
}
