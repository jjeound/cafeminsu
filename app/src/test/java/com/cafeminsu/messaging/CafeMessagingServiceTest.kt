package com.cafeminsu.messaging

import org.junit.Assert.assertEquals
import org.junit.Test

class CafeMessagingServiceTest {
    @Test
    fun cafeMessagingServiceKeepsManifestRegisteredName() {
        // AndroidManifest 의 <service android:name=".messaging.CafeMessagingService"> 와 일치해야 한다.
        assertEquals(
            "com.cafeminsu.messaging.CafeMessagingService",
            CafeMessagingService::class.java.name,
        )
    }
}
