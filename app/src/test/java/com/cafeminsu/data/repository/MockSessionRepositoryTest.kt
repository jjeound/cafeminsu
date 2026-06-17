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
    fun observeAuthStateStartsAsGuest() = runBlocking {
        val repository = MockSessionRepository()

        repository.observeAuthState().test {
            assertEquals(AuthState.Guest, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun refreshOnceAndClearSessionReturnGuestWithoutTokens() = runBlocking {
        val repository = MockSessionRepository()

        assertEquals(AuthState.Guest, repository.refreshOnce().successData())
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
