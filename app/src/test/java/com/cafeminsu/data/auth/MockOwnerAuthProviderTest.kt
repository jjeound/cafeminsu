package com.cafeminsu.data.auth

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.OwnerProfile
import com.cafeminsu.domain.model.OwnerStore
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
    fun getStoresExposesMultipleSeededStores() = runBlocking {
        val provider = MockOwnerAuthProvider()

        val result = provider.getStores()

        assertTrue(result is AppResult.Success)
        val stores = (result as AppResult.Success<List<OwnerStore>>).data
        assertEquals(listOf("강남점", "홍대점", "판교점"), stores.map { it.name })
    }

    @Test
    fun selectStoreSwitchesActiveStoreInProfile() = runBlocking {
        val provider = MockOwnerAuthProvider()

        val result = provider.selectStore("store-hongdae")

        assertTrue(result is AppResult.Success)
        val profile = (result as AppResult.Success<OwnerProfile>).data
        assertEquals("store-hongdae", profile.storeId)
        assertEquals("홍대점", profile.storeName)
    }

    @Test
    fun selectStoreWithUnknownIdReturnsFailure() = runBlocking {
        val provider = MockOwnerAuthProvider()

        assertTrue(provider.selectStore("store-unknown") is AppResult.Failure)
    }

    @Test
    fun setStoreOpenAppliesToSelectedStoreOnly() = runBlocking {
        val provider = MockOwnerAuthProvider()

        // 강남점(기본 선택)을 닫고 홍대점으로 전환하면 홍대점 기본 영업중 상태가 그대로 유지된다.
        provider.setStoreOpen(open = false)
        val switched = provider.selectStore("store-hongdae")

        assertEquals(true, (switched as AppResult.Success<OwnerProfile>).data.isStoreOpen)

        val backToGangnam = provider.selectStore("store-gangnam")
        assertEquals(false, (backToGangnam as AppResult.Success<OwnerProfile>).data.isStoreOpen)
    }

    @Test
    fun logoutReturnsSuccess() = runBlocking {
        val provider = MockOwnerAuthProvider()

        assertTrue(provider.logout() is AppResult.Success<*>)
    }
}
