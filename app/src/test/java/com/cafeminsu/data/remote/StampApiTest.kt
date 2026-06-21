package com.cafeminsu.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class StampApiTest {
    @Test
    fun stampSummaryDtoKeepsOpenApiFields() {
        val summary = StampSummaryRes(
            storeId = 11,
            storeName = "카페민수 강남점",
            count = 7,
        )

        assertEquals(11L, summary.storeId)
        assertEquals("카페민수 강남점", summary.storeName)
        assertEquals(7, summary.count)
    }

    @Test
    fun stampDetailDtoKeepsHistoryFields() {
        val detail = StampDetailRes(
            storeId = 11,
            storeName = "카페민수 강남점",
            count = 8,
            histories = listOf(
                HistoryItem(
                    earnedCount = 2,
                    createdAt = "2026-06-20T01:15:30Z",
                ),
            ),
        )

        assertEquals(2, detail.histories?.single()?.earnedCount)
        assertEquals("2026-06-20T01:15:30Z", detail.histories?.single()?.createdAt)
    }
}
