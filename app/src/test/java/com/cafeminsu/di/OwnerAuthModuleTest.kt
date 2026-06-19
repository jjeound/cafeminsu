package com.cafeminsu.di

import com.cafeminsu.data.auth.MockOwnerAuthProvider
import com.cafeminsu.domain.auth.OwnerAuthProvider
import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnerAuthModuleTest {
    @Test
    fun ownerAuthModuleBindsOwnerAuthProviderToMockSingleton() {
        val method = OwnerAuthModule::class.java.getDeclaredMethod(
            "bindOwnerAuthProvider",
            MockOwnerAuthProvider::class.java,
        )

        assertTrue(Modifier.isAbstract(method.modifiers))
        assertEquals(OwnerAuthProvider::class.java, method.returnType)
    }
}
