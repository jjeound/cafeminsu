package com.cafeminsu.data.local.store

import com.cafeminsu.domain.model.Store
import com.cafeminsu.domain.model.StoreAmenity
import com.cafeminsu.domain.model.StoreStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class StoreCacheMapperTest {
    @Test
    fun roundTripPreservesAllFieldsIncludingAmenities() {
        val store = Store(
            id = "7",
            name = "카페민수 강남점",
            address = "서울 강남구 테헤란로 134",
            phone = "02-1234-5678",
            distanceMeters = 120,
            latitude = 37.498,
            longitude = 127.028,
            status = StoreStatus.ClosingSoon,
            closingTimeLabel = "22:00 마감",
            amenities = listOf(StoreAmenity.Outlet, StoreAmenity.Wifi, StoreAmenity.Terrace),
        )

        assertEquals(store, store.toStoreEntity().toStore())
    }

    @Test
    fun emptyAmenitiesAndNullLabelRoundTrip() {
        val store = Store(
            id = "8",
            name = "카페민수 역삼점",
            address = "서울 강남구 역삼로 1",
            phone = "",
            distanceMeters = 0,
            latitude = 0.0,
            longitude = 0.0,
            status = StoreStatus.Open,
            closingTimeLabel = null,
            amenities = emptyList(),
        )

        val entity = store.toStoreEntity()

        assertEquals("", entity.amenities)
        assertEquals(null, entity.closingTimeLabel)
        assertEquals(store, entity.toStore())
    }

    @Test
    fun listExtensionsMapEachElement() {
        val stores = listOf(
            Store(
                id = "7",
                name = "강남점",
                address = "addr-1",
                phone = "p1",
                distanceMeters = 120,
                latitude = 37.498,
                longitude = 127.028,
                status = StoreStatus.Open,
                closingTimeLabel = null,
                amenities = listOf(StoreAmenity.Wifi),
            ),
            Store(
                id = "8",
                name = "역삼점",
                address = "addr-2",
                phone = "p2",
                distanceMeters = 300,
                latitude = 0.0,
                longitude = 0.0,
                status = StoreStatus.Closed,
                closingTimeLabel = "20:00 마감",
                amenities = emptyList(),
            ),
        )

        assertEquals(stores, stores.toStoreEntities().toStores())
    }
}
