package com.cafeminsu.data.voice

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.di.IoDispatcher
import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.model.SelectedOption
import com.cafeminsu.domain.voice.ParsedOrder
import com.cafeminsu.domain.voice.ParsedOrderItem
import com.cafeminsu.domain.voice.VoiceLlmEngine
import com.cafeminsu.domain.voice.VoiceOrderInterpreter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Gemma(온디바이스 LLM)로 음성 주문 transcript를 [ParsedOrder]로 해석한다.
 *
 * [buildPrompt]/[parseResponse]는 순수 함수라 단위 테스트로 검증하고, 실제 추론은 [VoiceLlmEngine]에 위임한다.
 * LLM이 흔들려도 메뉴 id 대조로 방어한다(없는 id는 unmatched, 옵션은 가용한 것만 채택).
 */
class GemmaVoiceOrderInterpreter @Inject constructor(
    private val engine: VoiceLlmEngine,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    moshi: Moshi,
) : VoiceOrderInterpreter {
    private val responseAdapter = moshi.adapter(VoiceOrderResponseDto::class.java)

    override suspend fun interpret(transcript: String, menu: List<MenuItem>): AppResult<ParsedOrder> =
        withContext(ioDispatcher) {
            if (transcript.isBlank()) {
                return@withContext AppResult.Success(ParsedOrder(items = emptyList(), unmatched = emptyList()))
            }
            if (!engine.isReady()) {
                return@withContext AppResult.Failure(DomainError.Unknown)
            }

            val raw = try {
                engine.generate(buildPrompt(transcript, menu))
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                return@withContext AppResult.Failure(DomainError.Unknown)
            }

            parseResponse(raw, menu)
        }

    private fun buildPrompt(transcript: String, menu: List<MenuItem>): String {
        val menuLines = menu.joinToString(separator = "\n") { item ->
            val optionIds = item.options.flatMap { group -> group.options.map { option -> option.id } }
            val optionPart = if (optionIds.isEmpty()) "" else " options=[${optionIds.joinToString(",")}]"
            "- id=${item.id} name=${item.name}$optionPart"
        }

        return """
            너는 카페 음성 주문을 구조화하는 파서야. 아래 메뉴의 id만 사용해서 주문을 해석해.
            설명 없이 JSON 객체 하나만 출력해. 형식:
            {"items":[{"menuId":"<메뉴id>","quantity":<1이상 정수>,"optionIds":["<옵션id>"]}],"unmatched":["<해석못한말>"]}

            메뉴:
            $menuLines

            주문: "$transcript"
            JSON:
        """.trimIndent()
    }

    private fun parseResponse(raw: String, menu: List<MenuItem>): AppResult<ParsedOrder> {
        val json = extractJsonObject(raw) ?: return AppResult.Failure(DomainError.Unknown)
        val response = try {
            responseAdapter.fromJson(json)
        } catch (_: Throwable) {
            null
        } ?: return AppResult.Failure(DomainError.Unknown)

        val menuById = menu.associateBy { item -> item.id }
        val items = mutableListOf<ParsedOrderItem>()
        val unmatched = mutableListOf<String>()

        response.items.orEmpty().forEach { dto ->
            val menuId = dto.menuId?.trim().orEmpty()
            val menuItem = menuById[menuId]
            if (menuItem == null) {
                if (menuId.isNotBlank()) {
                    unmatched += menuId
                }
            } else {
                items += ParsedOrderItem(
                    menuItemId = menuItem.id,
                    name = menuItem.name,
                    quantity = (dto.quantity ?: DefaultQuantity).coerceAtLeast(MinQuantity),
                    selectedOptions = menuItem.resolveOptions(dto.optionIds.orEmpty()),
                    isSoldOut = menuItem.isSoldOut,
                )
            }
        }

        unmatched += response.unmatched.orEmpty()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return AppResult.Success(ParsedOrder(items = items, unmatched = unmatched))
    }

    private fun MenuItem.resolveOptions(optionIds: List<String>): List<SelectedOption> {
        if (optionIds.isEmpty()) return emptyList()
        val wanted = optionIds.map { it.trim() }.filter { it.isNotBlank() }.toSet()
        if (wanted.isEmpty()) return emptyList()

        val selected = mutableListOf<SelectedOption>()
        options.forEach { group ->
            group.options.forEach { option ->
                val alreadyAdded = selected.any { it.optionId == option.id }
                if (option.isAvailable && option.id in wanted && !alreadyAdded) {
                    selected += SelectedOption(
                        groupId = group.id,
                        optionId = option.id,
                        name = option.name,
                        extraPrice = option.extraPrice,
                    )
                }
            }
        }
        return selected
    }

    private fun extractJsonObject(raw: String): String? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return raw.substring(start, end + 1)
    }

    private companion object {
        const val DefaultQuantity = 1
        const val MinQuantity = 1
    }
}

@JsonClass(generateAdapter = true)
data class VoiceOrderResponseDto(
    val items: List<VoiceOrderItemDto>?,
    val unmatched: List<String>?,
)

@JsonClass(generateAdapter = true)
data class VoiceOrderItemDto(
    val menuId: String?,
    val quantity: Int?,
    val optionIds: List<String>?,
)
