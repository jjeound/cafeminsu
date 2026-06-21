package com.cafeminsu.di

import com.cafeminsu.data.voice.AndroidSpeechRecognizer
import com.cafeminsu.data.voice.GemmaLlmEngine
import com.cafeminsu.data.voice.GemmaVoiceOrderInterpreter
import com.cafeminsu.domain.voice.VoiceLlmEngine
import com.cafeminsu.domain.voice.VoiceOrderInterpreter
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

    @Test
    fun `voice module binds gemma engine to voice llm engine contract`() {
        val method = VoiceModule::class.java.getDeclaredMethod(
            "bindVoiceLlmEngine",
            GemmaLlmEngine::class.java,
        )

        assertTrue(Modifier.isAbstract(method.modifiers))
        assertEquals(VoiceLlmEngine::class.java, method.returnType)
    }

    @Test
    fun `voice module binds gemma interpreter to voice order interpreter contract`() {
        val method = VoiceModule::class.java.getDeclaredMethod(
            "bindVoiceOrderInterpreter",
            GemmaVoiceOrderInterpreter::class.java,
        )

        assertTrue(Modifier.isAbstract(method.modifiers))
        assertEquals(VoiceOrderInterpreter::class.java, method.returnType)
    }
}
