package com.cafeminsu.ui.feature.notification

import com.cafeminsu.domain.model.NotificationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotiUiStateTest {
    @Test
    fun contentStateCarriesGroupedNotificationRows() {
        val state = NotiUiState.Content(
            groups = listOf(
                NotiGroupUiModel(
                    label = "오늘",
                    items = listOf(
                        NotiItemUiModel(
                            id = "noti-1",
                            type = NotificationType.OrderReady,
                            title = "주문이 준비됐어요",
                            body = "주문번호 A-2419 — 픽업대에서 수령해주세요",
                            timeLabel = "방금",
                            unread = true,
                        ),
                    ),
                ),
            ),
        )

        assertEquals("오늘", state.groups.single().label)
        assertEquals("방금", state.groups.single().items.single().timeLabel)
        assertTrue(state.groups.single().items.single().unread)
    }
}
