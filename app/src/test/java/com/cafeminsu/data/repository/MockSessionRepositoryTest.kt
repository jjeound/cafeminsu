package com.cafeminsu.data.repository

import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.auth.MockLoginProvider
import com.cafeminsu.domain.model.AuthState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockSessionRepositoryTest {
    @Test
    fun observeAuthStateStartsAsGuest() = runBlocking {
        val repository = mockSessionRepository()

        repository.observeAuthState().test {
            assertEquals(AuthState.Guest, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun refreshOnceReturnsGuestWhenNoSessionExists() = runBlocking {
        val repository = mockSessionRepository()

        val authState = repository.refreshOnce().successData()
        assertEquals(AuthState.Guest, authState)
    }

    @Test
    fun loginReturnsDemoAuthenticatedUserWithoutTokens() = runBlocking {
        val repository = mockSessionRepository()

        val authState = repository.login().successData()
        assertTrue(authState is AuthState.Authenticated)
        val authenticated = authState as AuthState.Authenticated
        assertEquals("demo-user", authenticated.user.id)
        assertEquals("민수", authenticated.user.displayName)
        assertEquals(false, authenticated.isNewUser)
    }

    @Test
    fun checkNicknameReturnsAvailableByDefaultAndDuplicateForReservedNickname() = runBlocking {
        val repository = mockSessionRepository()

        assertEquals(AppResult.Success(true), repository.checkNickname("새민수"))
        assertEquals(AppResult.Success(false), repository.checkNickname("이미사용중"))
    }

    @Test
    fun checkNicknameRejectsBlankNicknameBeforeMockSuccess() = runBlocking {
        val repository = mockSessionRepository()

        assertEquals(AppResult.Failure(DomainError.Validation("nickname")), repository.checkNickname("   "))
    }

    @Test
    fun completeSignupUpdatesMockSessionDisplayNameAndClearsNewUserSignal() = runBlocking {
        val repository = mockSessionRepository()

        repository.observeAuthState().test {
            assertEquals(AuthState.Guest, awaitItem())

            val result = repository.completeSignup("새민수")

            assertTrue(result is AppResult.Success)
            val authState = (result as AppResult.Success).data
            assertTrue(authState is AuthState.Authenticated)
            val authenticated = authState as AuthState.Authenticated
            assertEquals("새민수", authenticated.user.displayName)
            assertEquals(false, authenticated.isNewUser)
            assertEquals(authenticated, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun completeSignupRejectsBlankNicknameBeforeMockSuccess() = runBlocking {
        val repository = mockSessionRepository()

        assertEquals(AppResult.Failure(DomainError.Validation("nickname")), repository.completeSignup(" "))
    }

    @Test
    fun logoutReturnsGuestWithoutTokens() = runBlocking {
        val repository = mockSessionRepository()

        assertTrue(repository.login() is AppResult.Success<*>)
        assertTrue(repository.logout() is AppResult.Success<*>)

        repository.observeAuthState().test {
            assertEquals(AuthState.Guest, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun clearSessionUsesLogoutWipePath() = runBlocking {
        val repository = mockSessionRepository()

        assertTrue(repository.login() is AppResult.Success<*>)
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

    private fun mockSessionRepository(): MockSessionRepository =
        MockSessionRepository(MockLoginProvider())
}
