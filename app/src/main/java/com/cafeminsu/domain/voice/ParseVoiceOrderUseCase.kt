package com.cafeminsu.domain.voice

import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.model.MenuOption
import com.cafeminsu.domain.model.MenuOptionGroup
import com.cafeminsu.domain.model.SelectedOption
import javax.inject.Inject

class ParseVoiceOrderUseCase @Inject constructor() {
    operator fun invoke(transcript: String, menu: List<MenuItem>): ParsedOrder {
        val fragments = splitTranscript(transcript)
        if (fragments.isEmpty()) {
            return ParsedOrder(items = emptyList(), unmatched = emptyList())
        }

        val items = mutableListOf<ParsedOrderItem>()
        val unmatched = mutableListOf<String>()

        fragments.forEach { fragment ->
            val menuItem = findMenuItem(fragment, menu)
            if (menuItem == null) {
                unmatched += fragment
            } else {
                items += ParsedOrderItem(
                    menuItemId = menuItem.id,
                    name = menuItem.name,
                    quantity = parseQuantity(fragment),
                    selectedOptions = parseSelectedOptions(fragment, menuItem),
                    isSoldOut = menuItem.isSoldOut,
                )
            }
        }

        return ParsedOrder(items = items, unmatched = unmatched)
    }

    private fun splitTranscript(transcript: String): List<String> {
        return itemSeparator
            .split(transcript)
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun findMenuItem(fragment: String, menu: List<MenuItem>): MenuItem? {
        val normalizedFragment = normalize(fragment)
        if (normalizedFragment.isBlank()) {
            return null
        }

        return menu
            .asSequence()
            .mapNotNull { menuItem ->
                val score = menuItem.matchScore(normalizedFragment)
                if (score > NO_MATCH_SCORE) {
                    MenuMatch(menuItem = menuItem, score = score)
                } else {
                    null
                }
            }
            .maxWithOrNull(
                compareBy<MenuMatch> { it.score }
                    .thenBy { normalize(it.menuItem.name).length },
            )
            ?.menuItem
    }

    private fun MenuItem.matchScore(normalizedFragment: String): Int {
        val normalizedName = normalize(name)
        if (normalizedName.isBlank()) {
            return NO_MATCH_SCORE
        }

        if (normalizedFragment.contains(normalizedName)) {
            return FULL_NAME_MATCH_SCORE + normalizedName.length
        }

        if (normalizedName.contains(normalizedFragment) && normalizedFragment.length >= MIN_PARTIAL_MATCH_LENGTH) {
            return PARTIAL_NAME_MATCH_SCORE + normalizedFragment.length
        }

        return name
            .split(nameTokenSeparator)
            .asSequence()
            .map(::normalize)
            .filter { it.length >= MIN_PARTIAL_MATCH_LENGTH }
            .filter { normalizedFragment.contains(it) }
            .maxOfOrNull { TOKEN_MATCH_SCORE + it.length }
            ?: NO_MATCH_SCORE
    }

    private fun parseQuantity(fragment: String): Int {
        digitQuantityPattern.find(fragment)
            ?.groupValues
            ?.get(DIGIT_QUANTITY_GROUP)
            ?.toIntOrNull()
            ?.takeIf { it >= MIN_QUANTITY }
            ?.let { return it }

        koreanQuantityWithCounterPattern.find(fragment)
            ?.groupValues
            ?.get(KOREAN_COUNTER_QUANTITY_GROUP)
            ?.let { koreanQuantities[it] }
            ?.let { return it }

        koreanStandaloneQuantityPattern.find(fragment)
            ?.groupValues
            ?.get(KOREAN_STANDALONE_QUANTITY_GROUP)
            ?.let { koreanQuantities[it] }
            ?.let { return it }

        return DEFAULT_QUANTITY
    }

    private fun parseSelectedOptions(
        fragment: String,
        menuItem: MenuItem,
    ): List<SelectedOption> {
        val selectedOptions = mutableListOf<SelectedOption>()
        val normalizedFragment = normalize(fragment)

        optionIntents.forEach { intent ->
            if (intent.isTriggeredBy(fragment)) {
                menuItem.findOptionFor(intent)?.let { match ->
                    selectedOptions.addOption(match.group, match.option)
                }
            }
        }

        menuItem.options.forEach { group ->
            group.options
                .asSequence()
                .filter { it.isAvailable }
                .filter { normalize(it.name).length >= MIN_PARTIAL_MATCH_LENGTH }
                .filter { normalizedFragment.contains(normalize(it.name)) }
                .forEach { option -> selectedOptions.addOption(group, option) }
        }

        return selectedOptions
    }

    private fun MenuItem.findOptionFor(intent: OptionIntent): OptionMatch? {
        val hintedGroups = if (intent.groupHints.isEmpty()) {
            options
        } else {
            options.filter { group -> group.matchesAny(intent.groupHints) }
        }

        return hintedGroups.firstOptionMatching(intent)
            ?: options.firstOptionMatching(intent)
    }

    private fun List<MenuOptionGroup>.firstOptionMatching(intent: OptionIntent): OptionMatch? {
        return firstNotNullOfOrNull { group ->
            group.options
                .firstOrNull { option -> option.isAvailable && option.matchesAny(intent.optionHints) }
                ?.let { option -> OptionMatch(group = group, option = option) }
        }
    }

    private fun MenuOptionGroup.matchesAny(hints: Set<String>): Boolean {
        val normalizedId = normalize(id)
        val normalizedName = normalize(name)
        return hints.any { hint ->
            val normalizedHint = normalize(hint)
            normalizedId.contains(normalizedHint) ||
                normalizedName.contains(normalizedHint) ||
                normalizedHint.contains(normalizedId) ||
                normalizedHint.contains(normalizedName)
        }
    }

    private fun MenuOption.matchesAny(hints: Set<String>): Boolean {
        val normalizedName = normalize(name)
        return hints.any { hint ->
            val normalizedHint = normalize(hint)
            normalizedName.contains(normalizedHint) || normalizedHint.contains(normalizedName)
        }
    }

    private fun MutableList<SelectedOption>.addOption(
        group: MenuOptionGroup,
        option: MenuOption,
    ) {
        if (any { selected -> selected.optionId == option.id }) {
            return
        }

        val selectedInGroup = count { selected -> selected.groupId == group.id }
        if (group.maxSelect == SINGLE_SELECT_LIMIT && selectedInGroup >= SINGLE_SELECT_LIMIT) {
            return
        }
        if (group.maxSelect > SINGLE_SELECT_LIMIT && selectedInGroup >= group.maxSelect) {
            return
        }

        this += SelectedOption(
            groupId = group.id,
            optionId = option.id,
            name = option.name,
            extraPrice = option.extraPrice,
        )
    }

    private fun OptionIntent.isTriggeredBy(fragment: String): Boolean {
        return triggers.any { trigger -> trigger.containsMatchIn(fragment) }
    }

    private data class MenuMatch(
        val menuItem: MenuItem,
        val score: Int,
    )

    private data class OptionMatch(
        val group: MenuOptionGroup,
        val option: MenuOption,
    )

    private data class OptionIntent(
        val triggers: List<Regex>,
        val optionHints: Set<String>,
        val groupHints: Set<String> = emptySet(),
    )

    private companion object {
        private const val NO_MATCH_SCORE = 0
        private const val TOKEN_MATCH_SCORE = 1_000
        private const val PARTIAL_NAME_MATCH_SCORE = 5_000
        private const val FULL_NAME_MATCH_SCORE = 10_000
        private const val MIN_PARTIAL_MATCH_LENGTH = 2
        private const val DEFAULT_QUANTITY = 1
        private const val MIN_QUANTITY = 1
        private const val SINGLE_SELECT_LIMIT = 1
        private const val DIGIT_QUANTITY_GROUP = 1
        private const val KOREAN_COUNTER_QUANTITY_GROUP = 1
        private const val KOREAN_STANDALONE_QUANTITY_GROUP = 2

        private val itemSeparator = Regex("\\s*(?:,|，|、|그리고|및|랑|하고)\\s*")
        private val nameTokenSeparator = Regex("\\s+")
        private val ignoredCharacters = Regex("[^\\p{L}\\p{N}]+")
        private val removableParticles = Regex(
            "(은|는|이|가|을|를|도|만|으로|로)(?=\\s|$|한|하나|두|둘|세|셋|네|넷|다섯|여섯|일곱|여덟|아홉|열|\\d)",
        )
        private val digitQuantityPattern = Regex("(\\d+)\\s*(?:잔|개)")
        private val koreanQuantityWithCounterPattern = Regex(
            "(한|하나|두|둘|세|셋|네|넷|다섯|여섯|일곱|여덟|아홉|열)\\s*(?:잔|개)",
        )
        private val koreanStandaloneQuantityPattern = Regex(
            "(^|\\s)(하나|둘|셋|넷|다섯|여섯|일곱|여덟|아홉|열)(?=\\s|$)",
        )
        private val koreanQuantities = mapOf(
            "한" to 1,
            "하나" to 1,
            "두" to 2,
            "둘" to 2,
            "세" to 3,
            "셋" to 3,
            "네" to 4,
            "넷" to 4,
            "다섯" to 5,
            "여섯" to 6,
            "일곱" to 7,
            "여덟" to 8,
            "아홉" to 9,
            "열" to 10,
        )
        private val optionIntents = listOf(
            OptionIntent(
                triggers = listOf(Regex("따뜻|뜨겁|핫|hot", RegexOption.IGNORE_CASE)),
                optionHints = setOf("Hot", "핫", "따뜻", "뜨겁"),
                groupHints = setOf("온도", "temperature", "temp"),
            ),
            OptionIntent(
                triggers = listOf(Regex("아이스|차갑|시원|ice|iced", RegexOption.IGNORE_CASE)),
                optionHints = setOf("Iced", "Ice", "아이스", "차갑", "시원"),
                groupHints = setOf("온도", "temperature", "temp"),
            ),
            OptionIntent(
                triggers = listOf(Regex("샷\\s*추가|extra\\s*shot", RegexOption.IGNORE_CASE)),
                optionHints = setOf("샷 추가", "샷", "Extra Shot"),
            ),
            OptionIntent(
                triggers = listOf(Regex("연하게|연한|light", RegexOption.IGNORE_CASE)),
                optionHints = setOf("연하게", "연한", "Light"),
            ),
        )

        private fun normalize(value: String): String {
            return value
                .lowercase()
                .replace(removableParticles, "")
                .replace(ignoredCharacters, "")
        }
    }
}
