package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MockFcmTokenRepositoryTest {
    @Test
    fun registerRecordsTokenAndSucceeds() = runTest {
        val repository = MockFcmTokenRepository()

        val result = repository.register("tok-1")

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(listOf("tok-1"), repository.registeredTokens)
    }

    @Test
    fun registerKeepsLatestTokensInOrder() = runTest {
        val repository = MockFcmTokenRepository()

        repository.register("tok-1")
        repository.register("tok-2")

        assertEquals(listOf("tok-1", "tok-2"), repository.registeredTokens)
    }
}
