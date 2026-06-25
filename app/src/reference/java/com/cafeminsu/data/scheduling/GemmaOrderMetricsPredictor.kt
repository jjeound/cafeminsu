package com.cafeminsu.data.scheduling

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.di.IoDispatcher
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.proximity.ProximitySignal
import com.cafeminsu.domain.scheduling.CongestionCalculator
import com.cafeminsu.domain.scheduling.CongestionLevel
import com.cafeminsu.domain.scheduling.OrderMetricsPredictor
import com.cafeminsu.domain.scheduling.RulePrepTimeEstimator
import com.cafeminsu.domain.scheduling.SchedulingWeights
import com.cafeminsu.domain.voice.VoiceLlmEngine
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Gemma(온디바이스 LLM)로 스케줄러 입력값(제조시간·혼잡도·도착확률)을 보정 추정하는 [OrderMetricsPredictor] 구현.
 *
 * 음성 주문([com.cafeminsu.data.voice.GemmaVoiceOrderInterpreter])과 **동일 패턴**을 재사용한다:
 * buildPrompt(구조화 컨텍스트 + JSON 출력 지시) → [VoiceLlmEngine.generate] → 코드펜스 제거 → Moshi 파싱 → 도메인 검증.
 *
 * 폴백 규칙(블로킹/blocked 아님 — 정상 폴백):
 * - 모델 미가용([VoiceLlmEngine.isReady]==false): 추론 없이 step 0 규칙 추정치를 [AppResult.Success] 로 반환.
 * - 추론 예외/깨진(비JSON) 응답: [AppResult.Failure] 로 감싸 호출측이 규칙으로 폴백하게 한다.
 *
 * 보안: 추론 입력/출력에 PII·주문 민감정보를 **로깅하지 않는다**(`docs/SECURITY.md §4·§5`).
 */
class GemmaOrderMetricsPredictor @Inject constructor(
    private val engine: VoiceLlmEngine,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    moshi: Moshi,
    private val weights: SchedulingWeights = SchedulingWeights(),
) : OrderMetricsPredictor {
    private val rulePrepEstimator = RulePrepTimeEstimator(weights)
    private val congestionCalculator = CongestionCalculator(weights)

    private val prepAdapter = moshi.adapter(PrepEstimateDto::class.java)
    private val congestionAdapter = moshi.adapter(CongestionEstimateDto::class.java)
    private val arrivalAdapter = moshi.adapter(ArrivalEstimateDto::class.java)

    override suspend fun estimatePrepSeconds(order: Order, congestion: CongestionLevel): AppResult<Int> =
        withContext(ioDispatcher) {
            if (!engine.isReady()) {
                return@withContext AppResult.Success(rulePrepEstimator.estimateSeconds(order))
            }
            val raw = generateOrNull(buildPrepPrompt(order, congestion))
                ?: return@withContext AppResult.Failure(DomainError.Unknown)

            val seconds = parse(raw, prepAdapter)?.prepSeconds?.takeIf { it > 0 }
                ?: return@withContext AppResult.Failure(DomainError.Unknown)
            AppResult.Success(seconds)
        }

    override suspend fun predictCongestion(recentOrders: List<Order>, nowMillis: Long): AppResult<CongestionLevel> =
        withContext(ioDispatcher) {
            if (!engine.isReady()) {
                return@withContext AppResult.Success(congestionCalculator.level(recentOrders.size))
            }
            val raw = generateOrNull(buildCongestionPrompt(recentOrders))
                ?: return@withContext AppResult.Failure(DomainError.Unknown)

            val level = parse(raw, congestionAdapter)?.congestion?.toCongestionLevel()
                ?: return@withContext AppResult.Failure(DomainError.Unknown)
            AppResult.Success(level)
        }

    override suspend fun estimateArrivalProbability(signal: ProximitySignal): AppResult<Double> =
        withContext(ioDispatcher) {
            if (!engine.isReady()) {
                return@withContext AppResult.Success(ruleArrivalProbability(signal))
            }
            val raw = generateOrNull(buildArrivalPrompt(signal))
                ?: return@withContext AppResult.Failure(DomainError.Unknown)

            val probability = parse(raw, arrivalAdapter)?.probability
                ?: return@withContext AppResult.Failure(DomainError.Unknown)
            AppResult.Success(probability.coerceIn(0.0, 1.0))
        }

    /** 규칙 도착 임박도(0..1). [OrderScheduler] 의 근접 임박도와 동일한 창 환산식을 따른다. */
    private fun ruleArrivalProbability(signal: ProximitySignal): Double {
        val window = weights.arrivingSoonSeconds.toDouble()
        val urgency = (weights.arrivingSoonSeconds - signal.estimatedArrivalSeconds) / window
        return urgency.coerceIn(0.0, 1.0)
    }

    private suspend fun generateOrNull(prompt: String): String? =
        try {
            engine.generate(prompt)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            null
        }

    private fun <T> parse(raw: String, adapter: JsonAdapter<T>): T? {
        val json = extractJsonObject(raw) ?: return null
        return try {
            adapter.fromJson(json)
        } catch (_: Throwable) {
            null
        }
    }

    private fun buildPrepPrompt(order: Order, congestion: CongestionLevel): String {
        val itemLines = order.items.joinToString(separator = "\n") { item ->
            "- ${item.name} x${item.quantity} 옵션 ${item.selectedOptions.size}개"
        }
        return """
            너는 카페 주문의 예상 제조 시간을 초 단위로 추정하는 보조기야.
            설명 없이 JSON 객체 하나만 출력해. 형식: {"prepSeconds":<1 이상 정수>}

            매장 혼잡도: ${congestion.name}
            주문 항목:
            $itemLines
            JSON:
        """.trimIndent()
    }

    private fun buildCongestionPrompt(recentOrders: List<Order>): String {
        val totalQuantity = recentOrders.sumOf { order -> order.items.sumOf { it.quantity } }
        return """
            너는 카페 혼잡도를 추정하는 보조기야.
            설명 없이 JSON 객체 하나만 출력해. 형식: {"congestion":"Low" 또는 "Mid" 또는 "High"}

            최근 활성 주문 수: ${recentOrders.size}
            최근 주문 총 수량: $totalQuantity
            JSON:
        """.trimIndent()
    }

    private fun buildArrivalPrompt(signal: ProximitySignal): String =
        """
            너는 고객의 매장 도착 확률을 추정하는 보조기야.
            설명 없이 JSON 객체 하나만 출력해. 형식: {"probability":<0.0 이상 1.0 이하 실수>}

            신호 세기(rssi, dBm): ${signal.rssi}
            예상 도착까지(초): ${signal.estimatedArrivalSeconds}
            JSON:
        """.trimIndent()

    private fun String.toCongestionLevel(): CongestionLevel? =
        when (trim().lowercase()) {
            "low" -> CongestionLevel.Low
            "mid" -> CongestionLevel.Mid
            "high" -> CongestionLevel.High
            else -> null
        }

    private fun extractJsonObject(raw: String): String? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return raw.substring(start, end + 1)
    }
}

@JsonClass(generateAdapter = true)
data class PrepEstimateDto(
    val prepSeconds: Int?,
)

@JsonClass(generateAdapter = true)
data class CongestionEstimateDto(
    val congestion: String?,
)

@JsonClass(generateAdapter = true)
data class ArrivalEstimateDto(
    val probability: Double?,
)
