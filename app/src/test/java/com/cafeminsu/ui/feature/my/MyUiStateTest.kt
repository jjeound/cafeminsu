package com.cafeminsu.ui.feature.my

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MyUiStateTest {
    @Test
    fun contentCarriesProfileOrdersSettingsAndAppMeta() {
        val state = MyUiState.Content(
            profile = MyProfileUiModel(
                displayName = "민수",
                phoneLast4 = "1234",
            ),
            recentOrders = listOf(
                MyOrderSummaryUiModel(
                    orderId = "order-1",
                    orderNumber = "M001",
                    createdAtMillis = 1_803_974_400_000L,
                    totalAmount = 5_500,
                    statusLabel = "결제 완료",
                ),
            ),
            settings = listOf(MySettingItemUiModel(id = "logout", label = "로그아웃")),
            appMeta = "앱 버전 1.0",
        )

        assertEquals("민수", state.profile.displayName)
        assertEquals("1234", state.profile.phoneLast4)
        assertEquals("M001", state.recentOrders.single().orderNumber)
        assertEquals("로그아웃", state.settings.single().label)
        assertEquals("앱 버전 1.0", state.appMeta)
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
