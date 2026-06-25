package com.cafeminsu.data.scheduling

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.domain.model.SelectedOption
import com.cafeminsu.domain.scheduling.AiPrepTimeEstimator
import com.cafeminsu.domain.scheduling.CongestionLevel
import com.cafeminsu.domain.scheduling.ProximitySignal
import com.cafeminsu.domain.scheduling.RulePrepTimeEstimator
import com.cafeminsu.domain.scheduling.SchedulingWeights
import com.cafeminsu.domain.voice.VoiceLlmEngine
import com.squareup.moshi.Moshi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GemmaOrderMetricsPredictorTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val moshi = Moshi.Builder().build()
    private val weights = SchedulingWeights()
    private val ruleEstimator = RulePrepTimeEstimator(weights)

    private fun predictor(engine: VoiceLlmEngine) =
        GemmaOrderMetricsPredictor(engine = engine, ioDispatcher = dispatcher, moshi = moshi, weights = weights)

    // ---- estimatePrepSeconds ----

    @Test
    fun notReadyFallsBackToRulePrepEstimateWithoutGenerating() = runTest {
        val engine = FakeEngine(ready = false)
        val order = order()

        val result = predictor(engine).estimatePrepSeconds(order, CongestionLevel.Mid)

        assertEquals(ruleEstimator.estimateSeconds(order), (result as AppResult.Success).data)
        assertEquals(0, engine.generateCount)
    }

    @Test
    fun validJsonReturnsParsedPrepSeconds() = runTest {
        val engine = FakeEngine(response = """{"prepSeconds":240}""")

        val result = predictor(engine).estimatePrepSeconds(order(), CongestionLevel.Low)

        assertEquals(240, (result as AppResult.Success).data)
    }

    @Test
    fun stripsCodeFencesBeforeParsingPrepSeconds() = runTest {
        val engine = FakeEngine(response = "```json\n{\"prepSeconds\":180}\n```")

        val result = predictor(engine).estimatePrepSeconds(order(), CongestionLevel.High)

        assertEquals(180, (result as AppResult.Success).data)
    }

    @Test
    fun malformedPrepResponseReturnsFailure() = runTest {
        val engine = FakeEngine(response = "잘 모르겠어요")

        val result = predictor(engine).estimatePrepSeconds(order(), CongestionLevel.Low)

        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun enginePrepFailureReturnsFailure() = runTest {
        val engine = FakeEngine(throwOnGenerate = true)

        val result = predictor(engine).estimatePrepSeconds(order(), CongestionLevel.Low)

        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun nonPositivePrepSecondsReturnsFailure() = runTest {
        val engine = FakeEngine(response = """{"prepSeconds":0}""")

        val result = predictor(engine).estimatePrepSeconds(order(), CongestionLevel.Low)

        assertTrue(result is AppResult.Failure)
    }

    // ---- predictCongestion ----

    @Test
    fun notReadyPredictCongestionFallsBackToRule() = runTest {
        val engine = FakeEngine(ready = false)
        // 기본 weights 의 mid 임계치(5) 만큼이면 규칙상 Mid.
        val recent = List(weights.congestionMidThreshold) { order(id = "o$it") }

        val result = predictor(engine).predictCongestion(recent, nowMillis = 0L)

        assertEquals(CongestionLevel.Mid, (result as AppResult.Success).data)
        assertEquals(0, engine.generateCount)
    }

    @Test
    fun validCongestionJsonParses() = runTest {
        val engine = FakeEngine(response = """{"congestion":"High"}""")

        val result = predictor(engine).predictCongestion(listOf(order()), nowMillis = 0L)

        assertEquals(CongestionLevel.High, (result as AppResult.Success).data)
    }

    @Test
    fun malformedCongestionResponseReturnsFailure() = runTest {
        val engine = FakeEngine(response = """{"congestion":"Crowded"}""")

        val result = predictor(engine).predictCongestion(listOf(order()), nowMillis = 0L)

        assertTrue(result is AppResult.Failure)
    }

    // ---- estimateArrivalProbability ----

    @Test
    fun notReadyArrivalProbabilityFallsBackToRule() = runTest {
        val engine = FakeEngine(ready = false)
        // 도착까지 절반 창이면 규칙 임박도 0.5.
        val signal = signal(estimatedArrivalSeconds = weights.arrivingSoonSeconds / 2)

        val result = predictor(engine).estimateArrivalProbability(signal)

        assertEquals(0.5, (result as AppResult.Success).data, 0.0001)
        assertEquals(0, engine.generateCount)
    }

    @Test
    fun validArrivalProbabilityParsesAndClampsToRange() = runTest {
        val engine = FakeEngine(response = """{"probability":1.5}""")

        val result = predictor(engine).estimateArrivalProbability(signal(estimatedArrivalSeconds = 10))

        assertEquals(1.0, (result as AppResult.Success).data, 0.0001)
    }

    @Test
    fun malformedArrivalResponseReturnsFailure() = runTest {
        val engine = FakeEngine(response = "아마도요")

        val result = predictor(engine).estimateArrivalProbability(signal(estimatedArrivalSeconds = 10))

        assertTrue(result is AppResult.Failure)
    }

    // ---- AI 우선·규칙 폴백 (모델 미가용 = step 0 규칙값과 동일) ----

    @Test
    fun aiPrepEstimatorWithUnavailableModelMatchesRule() {
        val gemma = predictor(FakeEngine(ready = false))
        val aiEstimator = AiPrepTimeEstimator(predictor = gemma, fallback = ruleEstimator)
        val order = order(items = listOf(item(quantity = 2, optionCount = 1), item(quantity = 1, optionCount = 3)))

        assertEquals(ruleEstimator.estimateSeconds(order), aiEstimator.estimateSeconds(order))
    }

    private fun order(
        id: String = "o1",
        items: List<CartItem> = listOf(item(quantity = 1, optionCount = 1)),
    ): Order =
        Order(
            id = id,
            orderNumber = "1042",
            items = items,
            totalAmount = 4_500,
            status = OrderStatus.Accepted,
            createdAtMillis = 0L,
        )

    private fun item(quantity: Int, optionCount: Int): CartItem =
        CartItem(
            id = "item-$quantity-$optionCount",
            menuItemId = "americano",
            name = "아메리카노",
            unitPrice = 4_500,
            selectedOptions = (0 until optionCount).map { index ->
                SelectedOption(
                    groupId = "group-$index",
                    optionId = "option-$index",
                    name = "옵션 $index",
                    extraPrice = 0,
                )
            },
            quantity = quantity,
        )

    private fun signal(estimatedArrivalSeconds: Int): ProximitySignal =
        ProximitySignal(
            orderId = "o1",
            rssi = -70,
            estimatedArrivalSeconds = estimatedArrivalSeconds,
            atMillis = 0L,
        )
}

private class FakeEngine(
    private val response: String = "{}",
    private val ready: Boolean = true,
    private val throwOnGenerate: Boolean = false,
) : VoiceLlmEngine {
    var generateCount = 0
        private set

    override suspend fun isReady(): Boolean = ready

    override suspend fun generate(prompt: String): String {
        generateCount += 1
        if (throwOnGenerate) {
            throw RuntimeException("inference failed")
        }
        return response
    }
}
