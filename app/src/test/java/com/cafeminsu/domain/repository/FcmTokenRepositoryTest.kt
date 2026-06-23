package com.cafeminsu.domain.repository

import com.cafeminsu.core.AppResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class FcmTokenRepositoryTest {
    @Test
    fun fcmTokenRepositoryContractRegistersDeviceToken() = runBlocking {
        val repository = ContractFakeFcmTokenRepository()

        val result = repository.register("device-token-xyz")

        assertEquals(AppResult.Success(Unit), result)
        assertEquals("device-token-xyz", repository.lastRegisteredToken)
    }
}

private class ContractFakeFcmTokenRepository : FcmTokenRepository {
    var lastRegisteredToken: String? = null
        private set

    override suspend fun register(token: String): AppResult<Unit> {
        lastRegisteredToken = token
        return AppResult.Success(Unit)
    }
}
