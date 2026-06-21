package com.cafeminsu.data.voice

import com.cafeminsu.domain.voice.VoiceLlmEngine
import javax.inject.Inject
import org.junit.Assert.assertTrue
import org.junit.Test

class GemmaLlmEngineTest {
    @Test
    fun `gemma engine is replaceable through voice llm engine contract`() {
        assertTrue(VoiceLlmEngine::class.java.isAssignableFrom(GemmaLlmEngine::class.java))
    }

    @Test
    fun `gemma engine has injectable constructor`() {
        val hasInjectConstructor = GemmaLlmEngine::class.java.declaredConstructors.any { constructor ->
            constructor.getAnnotation(Inject::class.java) != null
        }

        assertTrue(hasInjectConstructor)
    }
}
