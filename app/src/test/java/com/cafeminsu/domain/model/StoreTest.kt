package com.cafeminsu.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreTest {
    @Test
    fun exposesStoreDomainModel() {
        val store = Store(
            id = "gangnam",
            name = "카페민수 강남점",
            address = "서울 강남구 테헤란로 134",
            phone = "02-3456-7890",
            distanceMeters = 120,
            latitude = 37.498,
            longitude = 127.028,
            status = StoreStatus.Open,
            closingTimeLabel = "22:00 마감",
            amenities = listOf(
                StoreAmenity.Outlet,
                StoreAmenity.Wifi,
                StoreAmenity.DriveThru,
                StoreAmenity.Terrace,
                StoreAmenity.Parking,
            ),
        )

        assertEquals("gangnam", store.id)
        assertEquals(StoreStatus.Open, store.status)
        assertEquals(120, store.distanceMeters)
        assertTrue(store.amenities.contains(StoreAmenity.Wifi))
    }
}
