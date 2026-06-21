package com.cafeminsu.domain.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.AuthState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRepositoryTest {
    @Test
    fun exposesSessionRepositoryContract() = runBlocking {
        val repository = object : SessionRepository {
            override fun observeAuthState(): Flow<AuthState> =
                flowOf(AuthState.Guest)

            override suspend fun refreshOnce(): AppResult<AuthState> =
                AppResult.Success(AuthState.Guest)

            override suspend fun clearSession(): AppResult<Unit> =
                AppResult.Success(Unit)
        }

        assertTrue(repository.refreshOnce() is AppResult.Success)
        assertTrue(repository.login() is AppResult.Success)
        assertTrue(repository.logout() is AppResult.Success)
        assertTrue(repository.clearSession() is AppResult.Success)
        assertTrue(repository.checkNickname("민수") is AppResult.Failure)
        assertTrue(repository.completeSignup("민수") is AppResult.Failure)
    }
}
