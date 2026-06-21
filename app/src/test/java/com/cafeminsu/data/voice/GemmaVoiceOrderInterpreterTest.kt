package com.cafeminsu.data.voice

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.model.MenuOption
import com.cafeminsu.domain.model.MenuOptionGroup
import com.cafeminsu.domain.voice.VoiceLlmEngine
import com.squareup.moshi.Moshi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GemmaVoiceOrderInterpreterTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val moshi = Moshi.Builder().build()

    private fun interpreter(engine: VoiceLlmEngine) =
        GemmaVoiceOrderInterpreter(engine = engine, ioDispatcher = dispatcher, moshi = moshi)

    @Test
    fun promptCarriesMenuIdsAndNamesAndValidJsonMapsToOrder() = runTest {
        val engine = FakeEngine(
            response = """{"items":[{"menuId":"americano","quantity":2,"optionIds":["iced"]}],"unmatched":[]}""",
        )

        val result = interpreter(engine).interpret("아메리카노 두 잔 아이스로", sampleMenu())

        assertTrue(engine.lastPrompt!!.contains("americano"))
        assertTrue(engine.lastPrompt!!.contains("아메리카노"))

        val order = (result as AppResult.Success).data
        val item = order.items.single()
        assertEquals("americano", item.menuItemId)
        assertEquals("아메리카노", item.name)
        assertEquals(2, item.quantity)
        assertEquals(listOf("iced"), item.selectedOptions.map { it.optionId })
        assertTrue(order.unmatched.isEmpty())
    }

    @Test
    fun stripsCodeFencesBeforeParsing() = runTest {
        val engine = FakeEngine(
            response = "```json\n{\"items\":[{\"menuId\":\"americano\",\"quantity\":1}],\"unmatched\":[]}\n```",
        )

        val order = (interpreter(engine).interpret("아메리카노", sampleMenu()) as AppResult.Success).data

        assertEquals("americano", order.items.single().menuItemId)
        assertEquals(1, order.items.single().quantity)
    }

    @Test
    fun unknownMenuIdGoesToUnmatched() = runTest {
        val engine = FakeEngine(
            response = """{"items":[{"menuId":"pizza","quantity":1}],"unmatched":["딸기라떼"]}""",
        )

        val order = (interpreter(engine).interpret("피자랑 딸기라떼", sampleMenu()) as AppResult.Success).data

        assertTrue(order.items.isEmpty())
        assertTrue(order.unmatched.contains("pizza"))
        assertTrue(order.unmatched.contains("딸기라떼"))
    }

    @Test
    fun malformedResponseReturnsFailure() = runTest {
        val engine = FakeEngine(response = "죄송해요 이해 못했어요")

        val result = interpreter(engine).interpret("아메리카노", sampleMenu())

        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun engineFailureReturnsFailure() = runTest {
        val engine = FakeEngine(throwOnGenerate = true)

        val result = interpreter(engine).interpret("아메리카노", sampleMenu())

        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun modelNotReadyReturnsFailureWithoutGenerating() = runTest {
        val engine = FakeEngine(ready = false)

        val result = interpreter(engine).interpret("아메리카노", sampleMenu())

        assertTrue(result is AppResult.Failure)
        assertEquals(0, engine.generateCount)
    }

    @Test
    fun blankTranscriptReturnsEmptyOrderWithoutGenerating() = runTest {
        val engine = FakeEngine()

        val order = (interpreter(engine).interpret("   ", sampleMenu()) as AppResult.Success).data

        assertTrue(order.items.isEmpty())
        assertTrue(order.unmatched.isEmpty())
        assertEquals(0, engine.generateCount)
    }

    @Test
    fun quantityFloorsToOneWhenMissingOrInvalid() = runTest {
        val engine = FakeEngine(
            response = """{"items":[{"menuId":"americano","quantity":0}],"unmatched":[]}""",
        )

        val order = (interpreter(engine).interpret("아메리카노", sampleMenu()) as AppResult.Success).data

        assertEquals(1, order.items.single().quantity)
    }

    @Test
    fun ignoresUnavailableOrUnknownOptionIds() = runTest {
        val engine = FakeEngine(
            response = """{"items":[{"menuId":"americano","quantity":1,"optionIds":["unknown","iced"]}],"unmatched":[]}""",
        )

        val order = (interpreter(engine).interpret("아메리카노 아이스", sampleMenu()) as AppResult.Success).data

        assertEquals(listOf("iced"), order.items.single().selectedOptions.map { it.optionId })
    }

    private fun sampleMenu(): List<MenuItem> =
        listOf(
            MenuItem(
                id = "americano",
                categoryId = "coffee",
                name = "아메리카노",
                description = "고소한 기본 커피",
                basePrice = 4_000,
                imageUrl = null,
                isSoldOut = false,
                options = listOf(
                    MenuOptionGroup(
                        id = "temperature",
                        name = "온도",
                        required = true,
                        minSelect = 1,
                        maxSelect = 1,
                        options = listOf(
                            MenuOption(id = "hot", name = "Hot", extraPrice = 0, isAvailable = true),
                            MenuOption(id = "iced", name = "Iced", extraPrice = 0, isAvailable = true),
                        ),
                    ),
                ),
            ),
        )
}

private class FakeEngine(
    private val response: String = "{}",
    private val ready: Boolean = true,
    private val throwOnGenerate: Boolean = false,
) : VoiceLlmEngine {
    var lastPrompt: String? = null
        private set
    var generateCount = 0
        private set

    override suspend fun isReady(): Boolean = ready

    override suspend fun generate(prompt: String): String {
        generateCount += 1
        lastPrompt = prompt
        if (throwOnGenerate) {
            throw RuntimeException("inference failed")
        }
        return response
    }
}
