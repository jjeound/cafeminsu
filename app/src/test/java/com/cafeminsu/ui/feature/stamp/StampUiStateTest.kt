package com.cafeminsu.ui.feature.stamp

import com.cafeminsu.domain.model.StampEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StampUiStateTest {
    @Test
    fun contentProgressIsClampedToGoal() {
        val content = StampUiState.Content(
            currentCount = 12,
            goalCount = 10,
            history = listOf(sampleStampEvent()),
        )

        assertEquals(1f, content.progress, 0.001f)
        assertTrue(content.isGoalReached)
    }

    @Test
    fun emptyProgressHandlesInvalidGoalCount() {
        val empty = StampUiState.Empty(
            currentCount = 3,
            goalCount = 0,
            message = "아직 적립 내역이 없어요",
        )

        assertEquals(0f, empty.progress, 0.001f)
        assertFalse(empty.isGoalReached)
    }

    private fun sampleStampEvent(): StampEvent =
        StampEvent(
            id = "stamp-1",
            orderId = "order-1",
            count = 1,
            createdAtMillis = 1_803_974_400_000L,
        )
}
