package com.cafeminsu.ui.feature.coupon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CouponUiStateTest {
    @Test
    fun stampSlotLabelsUseCheckForFilledAndNumbersForEmptySlots() {
        val stamp = CouponStampUiModel(
            storeName = "강남점",
            currentCount = 7,
            goalCount = 10,
        )

        assertEquals("7 / 10", stamp.countLabel)
        assertEquals(3, stamp.remainingCount)
        assertTrue(stamp.slots[0].filled)
        assertEquals("✓", stamp.slots[0].label)
        assertFalse(stamp.slots[7].filled)
        assertEquals("8", stamp.slots[7].label)
    }
}
