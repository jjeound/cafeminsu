package com.cafeminsu.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AppResultTest {
    @Test
    fun successMapTransformsValue() {
        val result = AppResult.Success(21)

        val mapped = result.map { it * 2 }

        assertEquals(AppResult.Success(42), mapped)
        assertTrue(mapped.isSuccess)
        assertFalse(mapped.isFailure)
    }

    @Test
    fun failureMapPreservesDomainError() {
        val error = DomainError.Payment("card approval failed")
        val result: AppResult<Int> = AppResult.Failure(error)

        val mapped = result.map { it * 2 }

        assertTrue(mapped is AppResult.Failure)
        assertSame(error, (mapped as AppResult.Failure).error)
        assertFalse(mapped.isSuccess)
        assertTrue(mapped.isFailure)
    }

    @Test
    fun foldCallsSuccessLambda() {
        val result = AppResult.Success("minsu")

        val folded = result.fold(
            onSuccess = { "success: $it" },
            onFailure = { "failure: $it" },
        )

        assertEquals("success: minsu", folded)
    }

    @Test
    fun foldCallsFailureLambda() {
        val result: AppResult<String> = AppResult.Failure(DomainError.Timeout)

        val folded = result.fold(
            onSuccess = { "success: $it" },
            onFailure = { "failure: $it" },
        )

        assertEquals("failure: Timeout", folded)
    }

    @Test
    fun getOrNullReturnsValueOnlyForSuccess() {
        val success = AppResult.Success("americano")
        val failure: AppResult<String> = AppResult.Failure(DomainError.Network)

        assertEquals("americano", success.getOrNull())
        assertNull(failure.getOrNull())
    }

    @Test
    fun domainErrorVariantsAreDistinct() {
        assertNotEquals(DomainError.Network, DomainError.Timeout)
        assertNotEquals(DomainError.Unauthorized, DomainError.NotFound)
        assertNotEquals(DomainError.Unknown, DomainError.Network)
        assertNotEquals(DomainError.Payment("x"), DomainError.Payment("y"))
        assertEquals(DomainError.Payment("x"), DomainError.Payment("x"))
        assertNotEquals(DomainError.Validation("quantity"), DomainError.Validation("option"))
        assertEquals(DomainError.Validation("quantity"), DomainError.Validation("quantity"))
    }
}
