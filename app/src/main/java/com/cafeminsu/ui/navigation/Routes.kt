package com.cafeminsu.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val HOME = "home"
    const val NOTI = "noti"
    const val STORE = "store"
    const val STORE_DETAIL = "store_detail"
    const val MENU = "menu"
    const val MENU_DETAIL_MENU_ID = "menuItemId"
    const val MENU_DETAIL_CART_ITEM_ID = "cartItemId"
    private const val MENU_DETAIL_BASE = "menu_detail"
    const val MENU_DETAIL =
        "$MENU_DETAIL_BASE/{$MENU_DETAIL_MENU_ID}?$MENU_DETAIL_CART_ITEM_ID={$MENU_DETAIL_CART_ITEM_ID}"
    const val VOICE = "voice"
    const val CART = "cart"
    const val PAY_ORDER_ID = "orderId"
    private const val PAY_BASE = "pay"
    const val PAY = "$PAY_BASE/{$PAY_ORDER_ID}"
    const val ORDER_OK_ORDER_ID = "orderId"
    private const val ORDER_OK_BASE = "order_ok"
    const val ORDER_OK = "$ORDER_OK_BASE/{$ORDER_OK_ORDER_ID}"
    const val ORDER_FAIL = "order_fail"
    const val MY = "my"
    const val NOTI_SETTINGS = "noti_settings"
    const val COUPON = "coupon"
    const val GIFT = "gift"
    const val HISTORY = "history"
    const val HISTORY_ORDER_ID = "orderId"
    const val HISTORY_DETAIL = "$HISTORY/{$HISTORY_ORDER_ID}"
    const val OWNER_LOGIN = "owner_login"
    const val OWNER_HOME = "owner_home"
    const val OWNER_ORDERS = "owner_orders"
    const val OWNER_MENU = "owner_menu"
    const val OWNER_MENU_ADD = "owner_menu_add"
    const val OWNER_SALES = "owner_sales"

    const val PAYMENT_ORDER_ID = PAY_ORDER_ID
    const val PAYMENT = PAY
    const val ORDER_STATUS_ORDER_ID = HISTORY_ORDER_ID
    const val ORDER_STATUS = HISTORY_DETAIL
    const val STAMP = COUPON
    const val GIFTICON = GIFT
    const val GIFTICON_ID = "gifticonId"
    private const val GIFTICON_DETAIL_BASE = GIFT
    const val GIFTICON_DETAIL = "$GIFTICON_DETAIL_BASE/{$GIFTICON_ID}"

    fun menuDetail(menuItemId: String, cartItemId: String? = null): String =
        if (cartItemId.isNullOrBlank()) {
            "$MENU_DETAIL_BASE/$menuItemId"
        } else {
            "$MENU_DETAIL_BASE/$menuItemId?$MENU_DETAIL_CART_ITEM_ID=$cartItemId"
        }
    fun pay(orderId: String): String = "$PAY_BASE/$orderId"
    fun payment(orderId: String): String = pay(orderId)
    fun orderOk(orderId: String): String = "$ORDER_OK_BASE/$orderId"
    fun history(orderId: String): String = "$HISTORY/$orderId"
    fun orderStatus(orderId: String): String = history(orderId)
    fun gifticonDetail(gifticonId: String): String = "$GIFTICON_DETAIL_BASE/$gifticonId"
}
