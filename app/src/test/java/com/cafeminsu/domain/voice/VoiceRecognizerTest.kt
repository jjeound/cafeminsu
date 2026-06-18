package com.cafeminsu.domain.voice

import app.cash.turbine.test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceRecognizerTest {
    @Test
    fun `fake recognizer exposes partial final and end events through flow`() = runTest {
        val recognizer = FakeVoiceRecognizer()

        recognizer.events.test {
            recognizer.emit(VoiceRecognitionEvent.Partial("아메"))
            recognizer.emit(VoiceRecognitionEvent.Final("아메리카노 한 잔"))
            recognizer.emit(VoiceRecognitionEvent.EndOfSpeech)

            assertEquals(VoiceRecognitionEvent.Partial("아메"), awaitItem())
            assertEquals(VoiceRecognitionEvent.Final("아메리카노 한 잔"), awaitItem())
            assertEquals(VoiceRecognitionEvent.EndOfSpeech, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `fake recognizer tracks start stop and destroy lifecycle calls`() {
        val recognizer = FakeVoiceRecognizer()

        recognizer.start()
        recognizer.stop()
        recognizer.destroy()

        assertTrue(recognizer.started)
        assertTrue(recognizer.stopped)
        assertTrue(recognizer.destroyed)
    }

    @Test
    fun `error event carries framework independent reason`() {
        val event = VoiceRecognitionEvent.Error(VoiceRecognitionError.PermissionDenied)

        assertEquals(VoiceRecognitionError.PermissionDenied, event.reason)
    }

    private class FakeVoiceRecognizer : VoiceRecognizer {
        private val mutableEvents = MutableSharedFlow<VoiceRecognitionEvent>(extraBufferCapacity = 4)

        override val events: Flow<VoiceRecognitionEvent> = mutableEvents.asSharedFlow()
        var started = false
            private set
        var stopped = false
            private set
        var destroyed = false
            private set

        override fun start() {
            started = true
        }

        override fun stop() {
            stopped = true
        }

        override fun destroy() {
            destroyed = true
        }

        fun emit(event: VoiceRecognitionEvent) {
            mutableEvents.tryEmit(event)
        }
    }
}
