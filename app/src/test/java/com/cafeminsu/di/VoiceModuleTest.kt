package com.cafeminsu.di

import com.cafeminsu.data.voice.AndroidSpeechRecognizer
import com.cafeminsu.domain.voice.VoiceRecognizer
import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceModuleTest {
    @Test
    fun `voice module binds android speech recognizer to voice recognizer contract`() {
        val method = VoiceModule::class.java.getDeclaredMethod(
            "bindVoiceRecognizer",
            AndroidSpeechRecognizer::class.java,
        )

        assertTrue(Modifier.isAbstract(method.modifiers))
        assertEquals(VoiceRecognizer::class.java, method.returnType)
    }
}
