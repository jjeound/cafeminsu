package com.cafeminsu.data.auth

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.AuthState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockLoginProviderTest {
    @Test
    fun loginReturnsDemoAuthenticatedUserWithoutTokens() = runBlocking {
        val provider = MockLoginProvider()

        val result = provider.login()

        assertTrue(result is AppResult.Success)
        val authState = (result as AppResult.Success<AuthState>).data
        assertTrue(authState is AuthState.Authenticated)
        val user = (authState as AuthState.Authenticated).user
        assertEquals("demo-user", user.id)
        assertEquals("민수", user.displayName)
        assertEquals("0000", user.phoneLast4)
    }

    @Test
    fun logoutReturnsSuccessWithoutPersistedValues() = runBlocking {
        val provider = MockLoginProvider()

        assertTrue(provider.logout() is AppResult.Success<*>)
    }
}
