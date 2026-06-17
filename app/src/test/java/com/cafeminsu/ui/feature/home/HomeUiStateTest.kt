package com.cafeminsu.ui.feature.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeUiStateTest {
    @Test
    fun stampSummaryCalculatesProgressAndRemainingCount() {
        val summary = HomeStampSummary(
            currentCount = 4,
            goalCount = 10,
        )

        assertEquals(0.4f, summary.progress, FloatCompareDelta)
        assertEquals(6, summary.remainingCount)
    }

    @Test
    fun stampSummaryHandlesInvalidGoalAsEmptyProgress() {
        val summary = HomeStampSummary(
            currentCount = 4,
            goalCount = 0,
        )

        assertEquals(0f, summary.progress, FloatCompareDelta)
        assertEquals(0, summary.remainingCount)
    }

    private companion object {
        const val FloatCompareDelta = 0.001f
    }
}
