package com.cafeminsu.ui.feature.store

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreUiStateTest {
    @Test
    fun contentCarriesStoresAndSelectedDetail() {
        val store = StoreUiModel(
            id = "gangnam",
            name = "카페민수 강남점",
            address = "서울 강남구 테헤란로 134",
            distanceLabel = "120m",
            status = StoreStatusUiModel.Open,
            statusLabel = "영업중",
        )
        val detail = StoreDetailUiModel(
            id = "gangnam",
            name = "카페민수 강남점",
            statusLabel = "영업중 · 22:00 마감",
            address = "서울 강남구 테헤란로 134",
            phone = "02-3456-7890",
            distanceLabel = "현재 위치에서 120m",
            parkingLabel = "건물 내 30분 무료",
            amenities = listOf("콘센트", "Wi-Fi"),
        )

        val state = StoreUiState.Content(
            query = "",
            stores = listOf(store),
            selectedStore = detail,
        )

        assertEquals("카페민수 강남점", state.stores.single().name)
        assertEquals("현재 위치에서 120m", state.selectedStore?.distanceLabel)
        assertTrue(state.selectedStore?.amenities?.contains("Wi-Fi") == true)
    }
}
