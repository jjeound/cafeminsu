package com.cafeminsu.domain.auth

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.OwnerProfile
import com.cafeminsu.domain.model.OwnerStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnerAuthProviderTest {
    @Test
    fun ownerAuthProviderContractReturnsOwnerProfileWithoutPassword() = runBlocking {
        val provider = FakeOwnerAuthProvider()

        val result = provider.login(loginId = "owner", password = "owner-secret")

        assertTrue(result is AppResult.Success)
        val profile = (result as AppResult.Success<OwnerProfile>).data
        assertEquals("owner", profile.loginId)
        assertEquals(false, profile.toString().contains("owner-secret"))
    }
}

private class FakeOwnerAuthProvider : OwnerAuthProvider {
    override suspend fun login(loginId: String, password: String): AppResult<OwnerProfile> =
        AppResult.Success(
            OwnerProfile(
                id = "owner-demo",
                storeId = "store-gangnam",
                storeName = "강남점",
                loginId = loginId,
                isStoreOpen = true,
            ),
        )

    override suspend fun logout(): AppResult<Unit> =
        AppResult.Success(Unit)

    override suspend fun setStoreOpen(open: Boolean): AppResult<OwnerProfile> =
        AppResult.Success(
            OwnerProfile(
                id = "owner-demo",
                storeId = "store-gangnam",
                storeName = "강남점",
                loginId = "owner",
                isStoreOpen = open,
            ),
        )

    override suspend fun getStores(): AppResult<List<OwnerStore>> =
        AppResult.Success(listOf(OwnerStore(id = "store-gangnam", name = "강남점")))

    override suspend fun selectStore(storeId: String): AppResult<OwnerProfile> =
        AppResult.Success(
            OwnerProfile(
                id = "owner-demo",
                storeId = storeId,
                storeName = "강남점",
                loginId = "owner",
                isStoreOpen = true,
            ),
        )
}
