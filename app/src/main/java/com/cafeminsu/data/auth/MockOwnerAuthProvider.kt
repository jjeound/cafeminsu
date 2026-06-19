package com.cafeminsu.data.auth

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.auth.OwnerAuthProvider
import com.cafeminsu.domain.model.OwnerProfile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockOwnerAuthProvider @Inject constructor() : OwnerAuthProvider {
    private var ownerProfile = demoOwnerProfile(loginId = DefaultLoginId)

    override suspend fun login(loginId: String, password: String): AppResult<OwnerProfile> {
        ownerProfile = demoOwnerProfile(loginId = loginId)
        return AppResult.Success(ownerProfile)
    }

    override suspend fun logout(): AppResult<Unit> =
        AppResult.Success(Unit)

    override suspend fun setStoreOpen(open: Boolean): AppResult<OwnerProfile> {
        ownerProfile = ownerProfile.copy(isStoreOpen = open)
        return AppResult.Success(ownerProfile)
    }

    private fun demoOwnerProfile(loginId: String): OwnerProfile =
        OwnerProfile(
            id = "owner-demo",
            storeId = "store-gangnam",
            storeName = "강남점",
            loginId = loginId,
            isStoreOpen = true,
        )

    private companion object {
        const val DefaultLoginId = "owner"
    }
}
