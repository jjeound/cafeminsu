package com.cafeminsu.ui.feature.coupon

import java.text.NumberFormat
import java.util.Locale

sealed interface CouponUiState {
    data object Loading : CouponUiState

    data class Content(
        val stamp: CouponStampUiModel,
        val coupons: List<CouponItemUiModel>,
    ) : CouponUiState

    data class Error(
        val message: String,
        val retryable: Boolean,
    ) : CouponUiState

    data class NeedsLogin(
        val message: String,
        val actionLabel: String,
    ) : CouponUiState
}

data class CouponStampUiModel(
    val storeName: String,
    val currentCount: Int,
    val goalCount: Int,
) {
    val safeGoalCount: Int = goalCount.coerceAtLeast(EmptyGoalCount)
    val filledCount: Int = currentCount.coerceIn(0, safeGoalCount)
    val remainingCount: Int = (safeGoalCount - filledCount).coerceAtLeast(0)
    val countLabel: String = "$filledCount / $safeGoalCount"
    val guideMessage: String = if (remainingCount == EmptyGoalCount) {
        "무료 음료 쿠폰을 받을 수 있어요"
    } else {
        "스탬프 ${remainingCount}개만 더 모으면 무료 음료 쿠폰!"
    }
    val slots: List<CouponStampSlotUiModel> = (1..safeGoalCount).map { index ->
        CouponStampSlotUiModel(
            index = index,
            filled = index <= filledCount,
        )
    }
}

data class CouponStampSlotUiModel(
    val index: Int,
    val filled: Boolean,
) {
    val label: String = if (filled) {
        "✓"
    } else {
        index.toString()
    }
}

data class CouponItemUiModel(
    val id: String,
    val title: String,
    val expiresLabel: String,
    val available: Boolean,
    val expiringSoon: Boolean,
    val amount: Int? = null,
    val dimmed: Boolean = !available,
) {
    val amountLabel: String? = amount?.let { formatWon(it) }
}

private fun formatWon(amount: Int): String =
    "₩${NumberFormat.getNumberInstance(Locale.KOREA).format(amount)}"

private const val EmptyGoalCount = 0
