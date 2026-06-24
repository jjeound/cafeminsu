package com.cafeminsu.di

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.auth.OwnerAuthProvider
import com.cafeminsu.domain.model.OwnerProfile
import com.cafeminsu.domain.model.OwnerStore
import org.junit.Assert.assertSame
import org.junit.Test

class OwnerAuthModuleTest {
    @Test
    fun blankBaseUrlSelectsMockOwnerAuthProvider() {
        val realProvider = FakeOwnerAuthProvider()
        val mockProvider = FakeOwnerAuthProvider()

        val selected = selectOwnerAuthProvider(
            baseUrl = "",
            realFactory = { realProvider },
            mockFactory = { mockProvider },
        )

        assertSame(mockProvider, selected)
    }

    @Test
    fun nonBlankBaseUrlSelectsRealOwnerAuthProvider() {
        val realProvider = FakeOwnerAuthProvider()
        val mockProvider = FakeOwnerAuthProvider()

        val selected = selectOwnerAuthProvider(
            baseUrl = "https://cafeminsu.duckdns.org/",
            realFactory = { realProvider },
            mockFactory = { mockProvider },
        )

        assertSame(realProvider, selected)
    }
}

private class FakeOwnerAuthProvider : OwnerAuthProvider {
    override suspend fun login(loginId: String, password: String): AppResult<OwnerProfile> =
        AppResult.Failure(com.cafeminsu.core.DomainError.Unknown)

    override suspend fun logout(): AppResult<Unit> = AppResult.Success(Unit)

    override suspend fun setStoreOpen(open: Boolean): AppResult<OwnerProfile> =
        AppResult.Failure(com.cafeminsu.core.DomainError.Unknown)

    override suspend fun getStores(): AppResult<List<OwnerStore>> =
        AppResult.Failure(com.cafeminsu.core.DomainError.Unknown)

    override suspend fun selectStore(storeId: String): AppResult<OwnerProfile> =
        AppResult.Failure(com.cafeminsu.core.DomainError.Unknown)
}
