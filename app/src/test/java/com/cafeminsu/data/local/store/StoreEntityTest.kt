package com.cafeminsu.data.local.store

import org.junit.Assert.assertEquals
import org.junit.Test

class StoreEntityTest {
    @Test
    fun retainsValuesAndSupportsCopy() {
        val entity = StoreEntity(
            id = "7",
            name = "카페민수 강남점",
            address = "서울 강남구 테헤란로 134",
            phone = "02-1234-5678",
            distanceMeters = 120,
            latitude = 37.498,
            longitude = 127.028,
            status = "ClosingSoon",
            closingTimeLabel = "22:00 마감",
            amenities = "Outlet,Wifi",
        )

        assertEquals("7", entity.id)
        assertEquals("Outlet,Wifi", entity.amenities)
        assertEquals("준비중", entity.copy(name = "준비중").name)
        assertEquals(entity, entity.copy())
    }
}
