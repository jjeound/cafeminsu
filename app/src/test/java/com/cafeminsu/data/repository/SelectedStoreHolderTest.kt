package com.cafeminsu.data.repository

import app.cash.turbine.test
import com.cafeminsu.domain.model.Store
import com.cafeminsu.domain.model.StoreStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SelectedStoreHolderTest {
    @Test
    fun selectedStoreStartsEmptyAndEmitsUpdates() = runTest {
        val holder = SelectedStoreHolder()

        holder.observe().test {
            assertNull(awaitItem())

            holder.select(sampleStore(id = "7"))

            assertEquals("7", awaitItem()?.id)
            assertEquals("7", holder.current()?.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun sampleStore(id: String): Store =
        Store(
            id = id,
            name = "카페민수 강남점",
            address = "서울 강남구 테헤란로 134",
            phone = "02-1234-5678",
            distanceMeters = 120,
            latitude = 37.498,
            longitude = 127.028,
            status = StoreStatus.Open,
            closingTimeLabel = "22:00 마감",
            amenities = emptyList(),
        )
}
