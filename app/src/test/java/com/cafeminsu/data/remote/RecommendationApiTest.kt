package com.cafeminsu.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class RecommendationApiTest {
    @Test
    fun todayRecommendationDtosKeepLiveSpecFields() {
        val response = TodayRecommendationRes(
            recommendations = listOf(
                RecommendationItemRes(menuId = 101, quantity = 1, optionIds = listOf(11, 12)),
                RecommendationItemRes(menuId = 102),
            ),
        )

        assertEquals(2, response.recommendations?.size)
        val first = response.recommendations?.first()
        assertEquals(101L, first?.menuId)
        assertEquals(1, first?.quantity)
        assertEquals(listOf(11L, 12L), first?.optionIds)
        assertEquals(102L, response.recommendations?.last()?.menuId)
    }
}
