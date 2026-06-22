package com.cafeminsu.di

import com.cafeminsu.data.messaging.FirebaseDeviceMessagingTokenProvider
import com.cafeminsu.domain.messaging.DeviceMessagingTokenProvider
import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessagingModuleTest {
    @Test
    fun bindsFirebaseDeviceMessagingTokenProviderToContract() {
        val method = MessagingModule::class.java.getDeclaredMethod(
            "bindDeviceMessagingTokenProvider",
            FirebaseDeviceMessagingTokenProvider::class.java,
        )

        assertTrue(Modifier.isAbstract(method.modifiers))
        assertEquals(DeviceMessagingTokenProvider::class.java, method.returnType)
    }
}
