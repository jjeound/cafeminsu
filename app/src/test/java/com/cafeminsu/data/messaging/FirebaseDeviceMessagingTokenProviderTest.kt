package com.cafeminsu.data.messaging

import com.cafeminsu.domain.messaging.DeviceMessagingTokenProvider
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseDeviceMessagingTokenProviderTest {
    @Test
    fun implementsDeviceMessagingTokenProviderContract() {
        // 생성자는 Firebase SDK를 건드리지 않는다(토큰 조회는 currentToken 시점에만).
        val provider: Any = FirebaseDeviceMessagingTokenProvider()

        assertTrue(provider is DeviceMessagingTokenProvider)
    }
}
