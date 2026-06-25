package com.cafeminsu.data.repository

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SelectedOwnerStoreHolderTest {
    @Test
    fun selectedStoreStartsEmptyAndEmitsUpdates() = runTest {
        val holder = SelectedOwnerStoreHolder()

        holder.observe().test {
            assertNull(awaitItem())

            holder.select("8")

            assertEquals("8", awaitItem())
            assertEquals("8", holder.current())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun reselectingSameStoreKeepsCurrentSelection() = runTest {
        val holder = SelectedOwnerStoreHolder()

        holder.select("7")
        holder.select("7")

        assertEquals("7", holder.current())
    }
}
