package com.cafeminsu.ui.feature.gift

import com.cafeminsu.domain.model.GiftChannel
import java.text.NumberFormat
import java.util.Locale

sealed interface GiftUiState {
    data object Loading : GiftUiState

    data class Content(
        val selectedAmountOption: GiftAmountOption,
        val selectedChannel: GiftChannel,
        val recipient: String,
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
        val recipientPlaceholder: String = when (selectedChannel) {
            GiftChannel.KakaoTalk -> "카카오 친구 선택"
            GiftChannel.Sms -> "연락처 입력"
        }
        val canSend: Boolean = !sending && selectedAmount > EmptyAmount && recipient.isNotBlank()
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

    data class LaunchKakaoShare(
        val target: KakaoShareTarget,
        override val message: String,
    ) : GiftEvent
}

data class KakaoShareTarget(
    val shareLink: String?,
    val deepLink: String?,
)

internal fun formatGiftNumber(amount: Int): String = formatNumber(amount)

private fun formatNumber(amount: Int): String =
    NumberFormat.getNumberInstance(Locale.KOREA).format(amount)

private const val EmptyAmount = 0
