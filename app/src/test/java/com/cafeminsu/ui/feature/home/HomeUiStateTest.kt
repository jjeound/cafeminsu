package com.cafeminsu.ui.feature.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeUiStateTest {
    @Test
    fun recommendedMenuKeepsCurrentAndOriginalPrices() {
        val menu = HomeRecommendedMenu(
            id = "menu-1",
            name = "민수 시그니처 라떼",
            description = "고소한 헤이즐넛 시럽 + 따뜻한 우유",
            price = 5_500,
            originalPrice = 6_000,
        )

        assertEquals(5_500, menu.price)
        assertEquals(6_000, menu.originalPrice)
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
