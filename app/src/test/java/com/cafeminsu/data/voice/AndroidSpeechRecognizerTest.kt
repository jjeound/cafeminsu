package com.cafeminsu.data.voice

import com.cafeminsu.domain.voice.VoiceRecognizer
import javax.inject.Inject
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidSpeechRecognizerTest {
    @Test
    fun `android implementation is replaceable through voice recognizer contract`() {
        assertTrue(VoiceRecognizer::class.java.isAssignableFrom(AndroidSpeechRecognizer::class.java))
    }

    @Test
    fun `android implementation has injectable constructor`() {
        val hasInjectConstructor = AndroidSpeechRecognizer::class.java.declaredConstructors.any { constructor ->
            constructor.getAnnotation(Inject::class.java) != null
        }

        assertTrue(hasInjectConstructor)
    }
}
