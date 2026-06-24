package com.cafeminsu.ui.feature.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeUiStateTest {
    @Test
    fun recommendedMenuKeepsPriceAndStoreName() {
        val menu = HomeRecommendedMenu(
            id = "menu-1",
            name = "민수 시그니처 라떼",
            description = "고소한 헤이즐넛 시럽 + 따뜻한 우유",
            price = 5_500,
            storeName = "민수 강남점",
        )

        assertEquals(5_500, menu.price)
        assertEquals("민수 강남점", menu.storeName)
    }

    @Test
    fun recommendedMenuAllowsNullStoreName() {
        val menu = HomeRecommendedMenu(
            id = "menu-1",
            name = "민수 시그니처 라떼",
            description = "고소한 헤이즐넛 시럽 + 따뜻한 우유",
            price = 5_500,
            storeName = null,
        )

        assertNull(menu.storeName)
    }

    @Test
    fun recentOrderKeepsReorderTargetMenuId() {
        val order = HomeRecentOrderSummary(
            orderId = "order-1",
            menuItemId = "menu-1",
            menuName = "아메리카노 ICE",
            optionSummary = "샷 추가 · 톨",
            orderedAtLabel = "어제",
            totalPrice = 4_500,
        )

        assertEquals("order-1", order.orderId)
        assertEquals("menu-1", order.menuItemId)
    }
}
