package com.cafeminsu.data.local.store

import com.cafeminsu.domain.model.Store
import com.cafeminsu.domain.model.StoreAmenity
import com.cafeminsu.domain.model.StoreStatus
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomStoreLocalDataSourceTest {
    private val dao = mockk<StoreDao>(relaxed = true)
    private val dataSource = RoomStoreLocalDataSource(dao)

    @Test
    fun cachedStoresMapsDaoEntitiesToDomain() = runTest {
        coEvery { dao.getAll() } returns listOf(
            StoreEntity(
                id = "7",
                name = "카페민수 강남점",
                address = "서울 강남구 테헤란로 134",
                phone = "02-1234-5678",
                distanceMeters = 120,
                latitude = 37.498,
                longitude = 127.028,
                status = "Open",
                closingTimeLabel = "22:00 마감",
                amenities = "Wifi,Outlet",
            ),
        )

        val stores = dataSource.cachedStores()

        val store = stores.single()
        assertEquals("7", store.id)
        assertEquals(StoreStatus.Open, store.status)
        assertEquals(listOf(StoreAmenity.Wifi, StoreAmenity.Outlet), store.amenities)
    }

    @Test
    fun replaceStoresClearsThenUpsertsMappedEntities() = runTest {
        val captured = slot<List<StoreEntity>>()
        coEvery { dao.upsertAll(capture(captured)) } returns Unit

        dataSource.replaceStores(
            listOf(
                Store(
                    id = "7",
                    name = "강남점",
                    address = "addr",
                    phone = "p",
                    distanceMeters = 120,
                    latitude = 37.498,
                    longitude = 127.028,
                    status = StoreStatus.Open,
                    closingTimeLabel = null,
                    amenities = listOf(StoreAmenity.Wifi),
                ),
            ),
        )

        // 사라진 매장이 남지 않도록 비운 뒤 채우는 순서를 보장한다.
        coVerifyOrder {
            dao.clear()
            dao.upsertAll(any())
        }
        assertEquals("7", captured.captured.single().id)
        assertEquals("Wifi", captured.captured.single().amenities)
    }
}
