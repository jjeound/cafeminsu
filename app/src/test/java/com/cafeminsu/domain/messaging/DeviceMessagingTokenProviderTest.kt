package com.cafeminsu.domain.messaging

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceMessagingTokenProviderTest {
    @Test
    fun contractReturnsCurrentToken() = runBlocking {
        val provider = FixedDeviceMessagingTokenProvider("device-token")

        assertEquals("device-token", provider.currentToken())
    }

    @Test
    fun contractReturnsNullWhenTokenUnavailable() = runBlocking {
        val provider = FixedDeviceMessagingTokenProvider(null)

        assertEquals(null, provider.currentToken())
    }
}

private class FixedDeviceMessagingTokenProvider(
    private val token: String?,
) : DeviceMessagingTokenProvider {
    override suspend fun currentToken(): String? = token
}
