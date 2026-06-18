package com.cafeminsu.data.repository

import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.AuthState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockSessionRepositoryTest {
    @Test
    fun observeAuthStateStartsAsDemoAuthenticatedUser() = runBlocking {
        val repository = MockSessionRepository()

        repository.observeAuthState().test {
            val authState = awaitItem()
            assertTrue(authState is AuthState.Authenticated)
            val authenticated = authState as AuthState.Authenticated
            assertEquals("demo-user", authenticated.user.id)
            assertEquals("민수", authenticated.user.displayName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun refreshOnceReturnsDemoAuthenticatedUserWithoutTokens() = runBlocking {
        val repository = MockSessionRepository()

        val authState = repository.refreshOnce().successData()
        assertTrue(authState is AuthState.Authenticated)
        val authenticated = authState as AuthState.Authenticated
        assertEquals("demo-user", authenticated.user.id)
        assertEquals("민수", authenticated.user.displayName)
    }

    @Test
    fun clearSessionReturnsGuestWithoutTokens() = runBlocking {
        val repository = MockSessionRepository()

        assertTrue(repository.clearSession() is AppResult.Success<*>)

        repository.observeAuthState().test {
            assertEquals(AuthState.Guest, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun AppResult<AuthState>.successData(): AuthState {
        assertTrue(this is AppResult.Success<*>)
        return (this as AppResult.Success<AuthState>).data
    }
}
