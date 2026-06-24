package com.cafeminsu.core.data.model

import com.cafeminsu.core.model.media.ImageSource
import com.cafeminsu.core.network.model.response.store.NearbyStoreResponse
import com.cafeminsu.core.network.model.response.store.OwnerStoreResponse
import org.junit.Assert.assertEquals
import org.junit.Test

class StoreResponseMapperTest {
    @Test
    fun `maps nearby store response to domain model`() {
        val response = NearbyStoreResponse(
            id = 1L,
            name = "민수 카페",
            distance = 230.5,
            imageUrl = "https://example.com/store.jpg",
        )

        val store = response.asExternalModel()

        assertEquals(1L, store.id)
        assertEquals("민수 카페", store.name)
        assertEquals(230.5, store.distanceMeters, 0.0)
        assertEquals(ImageSource.Remote("https://example.com/store.jpg"), store.image)
    }

    @Test
    fun `maps owned store response to domain model`() {
        val response = OwnerStoreResponse(
            id = 2L,
            name = "민수 카페 2호점",
            imageUrl = null,
        )

        val store = response.asExternalModel()

        assertEquals(2L, store.id)
        assertEquals("민수 카페 2호점", store.name)
        assertEquals(ImageSource.None, store.image)
    }
}
