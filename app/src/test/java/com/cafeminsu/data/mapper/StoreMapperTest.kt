package com.cafeminsu.data.mapper

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.remote.StoreDetailRes
import com.cafeminsu.data.remote.StoreSearchItem
import com.cafeminsu.data.remote.StoreSearchRes
import com.cafeminsu.domain.model.StoreStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class StoreMapperTest {
    @Test
    fun searchResponseMapsPartialStoreFieldsWithDomainDefaults() {
        val result = StoreSearchRes(
            stores = listOf(
                StoreSearchItem(
                    id = 7,
                    name = "카페민수 강남점",
                    address = "서울 강남구 테헤란로 134",
                    imageUrl = "https://cdn.example/store.png",
                ),
            ),
            total = 1,
        ).toStores()

        val store = (result as AppResult.Success).data.single()
        assertEquals("7", store.id)
        assertEquals("카페민수 강남점", store.name)
        assertEquals("", store.phone)
        assertEquals(0, store.distanceMeters)
        assertEquals(StoreStatus.Open, store.status)
    }

    @Test
    fun detailResponseMapsFullStoreFields() {
        val result = StoreDetailRes(
            id = 7,
            name = "카페민수 강남점",
            address = "서울 강남구 테헤란로 134",
            latitude = 37.498,
            longitude = 127.028,
            phone = "02-1234-5678",
            businessHours = "09:00-22:00",
            imageUrl = "https://cdn.example/store.png",
        ).toStore()

        val store = (result as AppResult.Success).data
        assertEquals("7", store.id)
        assertEquals(37.498, store.latitude, 0.0)
        assertEquals(127.028, store.longitude, 0.0)
        assertEquals("09:00-22:00", store.closingTimeLabel)
    }

    @Test
    fun missingStoreIdMapsToUnknownError() {
        val result = StoreSearchRes(
            stores = listOf(
                StoreSearchItem(
                    id = null,
                    name = "이름 없음",
                    address = null,
                    imageUrl = null,
                ),
            ),
            total = 1,
        ).toStores()

        assertEquals(AppResult.Failure(DomainError.Unknown), result)
    }
}
