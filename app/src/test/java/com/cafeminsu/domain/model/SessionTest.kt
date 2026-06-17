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
    }
}
