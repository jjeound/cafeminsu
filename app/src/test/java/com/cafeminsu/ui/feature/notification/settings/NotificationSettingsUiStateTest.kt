package com.cafeminsu.ui.feature.notification.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationSettingsUiStateTest {
    @Test
    fun defaultsMatchPreferenceDefaults() {
        val state = NotificationSettingsUiState()

        assertTrue(state.orderStatusEnabled)
        assertTrue(state.promotionEnabled)
        assertFalse(state.marketingEnabled)
    }

    @Test
    fun isEnabledReadsFlagPerCategory() {
        val state = NotificationSettingsUiState(
            orderStatusEnabled = false,
            promotionEnabled = true,
            marketingEnabled = true,
        )

        assertEquals(false, state.isEnabled(NotificationCategory.OrderStatus))
        assertEquals(true, state.isEnabled(NotificationCategory.Promotion))
        assertEquals(true, state.isEnabled(NotificationCategory.Marketing))
    }
}
