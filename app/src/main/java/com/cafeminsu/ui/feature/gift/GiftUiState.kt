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
        val selectedFriendName: String? = null,
        val customAmountText: String = "",
        val sending: Boolean = false,
    ) : GiftUiState {
        val selectedAmount: Int = selectedAmountOption.amount
            ?: customAmountText.filter(Char::isDigit).toIntOrNull()
            ?: EmptyAmount
        val selectedAmountLabel: String = formatNumber(selectedAmount)
        val previewAmountLabel: String = "₩ $selectedAmountLabel"
        val primaryButtonText: String = "구매하고 선물 보내기 · ${formatNumber(selectedAmount)}원"
        val recipientPlaceholder: String = "연락처 입력"
        val hasSelectedFriend: Boolean = !selectedFriendName.isNullOrBlank()
        val friendPickLabel: String = selectedFriendName?.takeIf { it.isNotBlank() } ?: "카카오톡 친구 선택"
        val canSend: Boolean = !sending && selectedAmount > EmptyAmount &&
            when (selectedChannel) {
                GiftChannel.KakaoTalk -> hasSelectedFriend
                GiftChannel.Sms -> recipient.isNotBlank()
            }
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
     * 구매 성공 후 선택한 친구에게 카카오톡 메시지를 전송하라는 신호.
     * UI 레이어가 [receiverUuid] 로 메시지를 보내고, 실패/미가입 시 [target] 으로 공유 폴백한다.
     * 메시지/공유 단계 실패는 선물 실패가 아니다(구매는 이미 성공).
     */
    data class SendKakaoMessage(
        val receiverUuid: String,
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
