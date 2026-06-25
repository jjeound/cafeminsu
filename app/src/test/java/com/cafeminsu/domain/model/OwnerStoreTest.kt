package com.cafeminsu.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class OwnerStoreTest {
    @Test
    fun keepsServerStoreIdentityAsStrings() {
        val store = OwnerStore(id = "7", name = "강남점")

        assertEquals("7", store.id)
        assertEquals("강남점", store.name)
    }
}
