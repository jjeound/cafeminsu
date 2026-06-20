package com.cafeminsu.ui.feature.my

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MyUiStateTest {
    @Test
    fun contentCarriesProfileStatsQuickMenusAndSettings() {
        val state = MyUiState.Content(
            profile = MyProfileUiModel(
                displayName = "진지원",
                initial = "진",
                tierLabel = "GOLD",
            ),
            stats = MyStatsUiModel(
                orderCount = 12,
                stampCount = 7,
                stampGoalCount = 10,
                couponCount = 3,
            ),
            quickMenus = listOf(MyQuickMenuUiModel(id = "history", label = "주문내역")),
            settings = listOf(
                MySettingItemUiModel(id = "version", label = "버전 정보", trailingText = "v1.0.0"),
            ),
        )

        assertEquals("진지원", state.profile.displayName)
        assertEquals("진", state.profile.initial)
        assertEquals("GOLD", state.profile.tierLabel)
        assertEquals(12, state.stats.orderCount)
        assertEquals(7, state.stats.stampCount)
        assertEquals(3, state.stats.couponCount)
        assertEquals("주문내역", state.quickMenus.single().label)
        assertEquals("v1.0.0", state.settings.single().trailingText)
    }

    @Test
    fun needsLoginRepresentsProtectedScreenState() {
        val state: MyUiState = MyUiState.NeedsLogin(
            message = "로그인이 필요해요",
            actionLabel = "다시 로그인하기",
        )

        assertTrue(state is MyUiState.NeedsLogin)
    }
}
