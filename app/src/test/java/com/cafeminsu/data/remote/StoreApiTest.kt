package com.cafeminsu.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class StoreApiTest {
    @Test
    fun storeSearchDtoKeepsOpenApiFields() {
        val response = StoreSearchRes(
            stores = listOf(
                StoreSearchItem(
                    id = 7,
                    name = "카페민수 강남점",
                    address = "서울 강남구 테헤란로 134",
                    imageUrl = "https://cdn.example/store.png",
                ),
            ),
            total = 1,
        )

        assertEquals(7L, response.stores?.single()?.id)
        assertEquals("카페민수 강남점", response.stores?.single()?.name)
        assertEquals(1L, response.total)
    }

    @Test
    fun storeSearchUsesOpenApiPagingDefaults() {
        assertEquals(0, DefaultStorePage)
        assertEquals(20, DefaultStorePageSize)
    }
}
