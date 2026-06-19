package com.cafeminsu.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class SessionTest {
    @Test
    fun exposesAuthStateAndUserProfile() {
        val userProfile = UserProfile(
            id = "user-1",
            displayName = "민수",
            phoneLast4 = "1234",
        )
        val authState: AuthState = AuthState.Authenticated(userProfile)

        assertSame(AuthState.Unknown, AuthState.Unknown)
        assertSame(AuthState.Guest, AuthState.Guest)
        assertSame(AuthState.Expired, AuthState.Expired)
        assertEquals(userProfile, (authState as AuthState.Authenticated).user)
        assertEquals(UserRole.Customer, authState.role)
    }

    @Test
    fun authenticatedStateCanCarryOwnerRoleWithoutBreakingDefaultCustomerRole() {
        val userProfile = UserProfile(
            id = "owner-user",
            displayName = "강남점 점주",
            phoneLast4 = null,
        )

        val defaultAuthState = AuthState.Authenticated(userProfile)
        val ownerAuthState = AuthState.Authenticated(
            user = userProfile,
            role = UserRole.Owner,
        )

        assertEquals(UserRole.Customer, defaultAuthState.role)
        assertEquals(UserRole.Owner, ownerAuthState.role)
    }

    @Test
    fun exposesOwnerProfileWithoutPasswordFields() {
        val ownerProfile = OwnerProfile(
            id = "owner-1",
            storeId = "store-gangnam",
            storeName = "강남점",
            loginId = "owner",
            isStoreOpen = true,
        )

        assertEquals("owner-1", ownerProfile.id)
        assertEquals("store-gangnam", ownerProfile.storeId)
        assertEquals("강남점", ownerProfile.storeName)
        assertEquals("owner", ownerProfile.loginId)
        assertEquals(true, ownerProfile.isStoreOpen)
        assertEquals(false, ownerProfile.toString().contains("password", ignoreCase = true))
    }
}
