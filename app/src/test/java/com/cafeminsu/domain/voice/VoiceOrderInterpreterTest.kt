package com.cafeminsu.domain.voice

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.MenuItem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceOrderInterpreterTest {
    @Test
    fun interpreterReturnsParsedOrderThroughContract() = runTest {
        val expected = ParsedOrder(
            items = listOf(
                ParsedOrderItem(
                    menuItemId = "americano",
                    name = "아메리카노",
                    quantity = 1,
                    selectedOptions = emptyList(),
                ),
            ),
            unmatched = emptyList(),
        )
        val interpreter = object : VoiceOrderInterpreter {
            override suspend fun interpret(transcript: String, menu: List<MenuItem>): AppResult<ParsedOrder> =
                AppResult.Success(expected)
        }

        val result = interpreter.interpret("아메리카노", emptyList())

        assertTrue(result is AppResult.Success)
        assertEquals(expected, (result as AppResult.Success).data)
    }

    @Test
    fun engineExposesReadinessAndGenerateThroughContract() = runTest {
        val engine = object : VoiceLlmEngine {
            override suspend fun isReady(): Boolean = true

            override suspend fun generate(prompt: String): String = "ok:$prompt"
        }

        assertTrue(engine.isReady())
        assertEquals("ok:hello", engine.generate("hello"))
    }
}
