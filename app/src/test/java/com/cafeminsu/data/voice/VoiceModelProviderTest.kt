package com.cafeminsu.data.voice

import javax.inject.Inject
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceModelProviderTest {
    @Test
    fun `model file name uses litertlm extension`() {
        assertTrue(VoiceModelProvider.MODEL_FILE_NAME.endsWith(".litertlm"))
    }

    @Test
    fun `has injectable constructor`() {
        val hasInjectConstructor = VoiceModelProvider::class.java.declaredConstructors.any { constructor ->
            constructor.getAnnotation(Inject::class.java) != null
        }

        assertTrue(hasInjectConstructor)
    }
}
