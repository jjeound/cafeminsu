package com.cafeminsu.ui.feature.gift

import java.text.NumberFormat
import java.util.Locale

sealed interface GiftUiState {
    data object Loading : GiftUiState

    data class Content(
        val selectedAmountOption: GiftAmountOption,
        val message: String,
        val customAmountText: String = "",
        val sending: Boolean = false,
    ) : GiftUiState {
        val selectedAmount: Int = selectedAmountOption.amount
            ?: customAmountText.filter(Char::isDigit).toIntOrNull()
            ?: EmptyAmount
        val selectedAmountLabel: String = formatNumber(selectedAmount)
        val previewAmountLabel: String = "₩ $selectedAmountLabel"
        val primaryButtonText: String = "구매하고 선물 보내기 · ${formatNumber(selectedAmount)}원"
        // 카카오톡 단일 채널: 수신자 미지정 구매 후 인텐트로 공유하므로 금액만 있으면 전송 가능.
        val canSend: Boolean = !sending && selectedAmount > EmptyAmount
    }

    data class Error(
        val message: String,
        val retryable: Boolean,
    ) : GiftUiState

    data class NeedsLogin(
        val message: String,
        val actionLabel: String,
    ) : GiftUiState
}

enum class GiftAmountOption(
    val label: String,
    val amount: Int?,
) {
    FiveThousand(label = "5,000", amount = 5_000),
    TenThousand(label = "10,000", amount = 10_000),
    TwentyThousand(label = "20,000", amount = 20_000),
    Custom(label = "직접입력", amount = null),
}

sealed interface GiftEvent {
    val message: String

    data class SendSucceeded(
        override val message: String,
    ) : GiftEvent

    data class SendFailed(
        override val message: String,
    ) : GiftEvent

    /**
     * 구매 성공 후 클레임 링크를 카카오톡으로 공유하라는 신호.
     * UI 레이어가 [shareText] 를 `ACTION_SEND` 인텐트로 카카오톡에 공유한다(미설치 시 시스템 공유 시트).
     * 공유 단계 실패는 선물 실패가 아니다(구매는 이미 성공). 링크/코드는 로깅하지 않는다(SECURITY §4).
     */
    data class ShareGiftLink(
        val shareText: String,
        override val message: String,
    ) : GiftEvent
}

internal fun formatGiftNumber(amount: Int): String = formatNumber(amount)

private fun formatNumber(amount: Int): String =
    NumberFormat.getNumberInstance(Locale.KOREA).format(amount)

private const val EmptyAmount = 0
