package com.cafeminsu.data.auth

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.OwnerProfile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockOwnerAuthProviderTest {
    @Test
    fun loginReturnsDemoOwnerProfileForAnyInputWithoutPersistingPassword() = runBlocking {
        val provider = MockOwnerAuthProvider()

        val result = provider.login(loginId = "owner", password = "owner-secret")

        assertTrue(result is AppResult.Success)
        val profile = (result as AppResult.Success<OwnerProfile>).data
        assertEquals("owner-demo", profile.id)
        assertEquals("store-gangnam", profile.storeId)
        assertEquals("강남점", profile.storeName)
        assertEquals("owner", profile.loginId)
        assertEquals(true, profile.isStoreOpen)
        assertEquals(false, profile.toString().contains("owner-secret"))
    }

    @Test
    fun setStoreOpenReflectsLatestOpenState() = runBlocking {
        val provider = MockOwnerAuthProvider()

        val closed = provider.setStoreOpen(open = false)
        val opened = provider.setStoreOpen(open = true)

        assertTrue(closed is AppResult.Success)
        assertTrue(opened is AppResult.Success)
        assertEquals(false, (closed as AppResult.Success<OwnerProfile>).data.isStoreOpen)
        assertEquals(true, (opened as AppResult.Success<OwnerProfile>).data.isStoreOpen)
    }

    @Test
    fun logoutReturnsSuccess() = runBlocking {
        val provider = MockOwnerAuthProvider()

        assertTrue(provider.logout() is AppResult.Success<*>)
    }
}
