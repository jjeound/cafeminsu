package com.cafeminsu.ui.navigation

object Routes {
    const val HOME = "m01"
    const val MENU = "m02"
    const val MENU_DETAIL = "m03"
    const val MENU_DETAIL_MENU_ID = "menuItemId"
    const val MENU_DETAIL_PATTERN = "$MENU_DETAIL/{$MENU_DETAIL_MENU_ID}"
    const val VOICE = "m04"
    const val CART = "m05"
    const val PAYMENT = "m06"
    const val ORDER_STATUS = "m07"
    const val STAMP = "m08"
    const val GIFTICON = "m09"
    const val MY = "m10"

    fun menuDetail(menuItemId: String): String = "$MENU_DETAIL/$menuItemId"
}
