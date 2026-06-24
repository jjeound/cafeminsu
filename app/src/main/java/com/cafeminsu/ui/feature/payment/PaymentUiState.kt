package com.cafeminsu.ui.feature.payment

import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.ui.feature.order.OrderFailureUiModel

sealed interface PaymentUiState {
    data object Loading : PaymentUiState

    data class Content(
        val orderId: String,
        val orderNumber: String,
        val items: List<CartItem>,
        val totalAmount: Int,
        val methods: List<PaymentMethodUiModel>,
        val selectedMethodId: String,
        val paymentState: PaymentProgress,
        val coupons: List<PaymentCouponUiModel> = emptyList(),
        val selectedCouponId: String? = null,
    ) : PaymentUiState {
        val selectedMethod: PaymentMethodUiModel? =
            methods.firstOrNull { method -> method.id == selectedMethodId }

        val selectedCoupon: PaymentCouponUiModel? =
            coupons.firstOrNull { coupon -> coupon.id == selectedCouponId }

        // 할인은 결제 금액을 넘지 못한다(음수 결제 방지).
        val discountAmount: Int =
            selectedCoupon?.let { coupon -> coupon.discountAmount.coerceAtMost(totalAmount) } ?: 0

        val payableAmount: Int = (totalAmount - discountAmount).coerceAtLeast(0)

        val isPayEnabled: Boolean =
            selectedMethod != null &&
                totalAmount > 0 &&
                paymentState !is PaymentProgress.Processing &&
                paymentState !is PaymentProgress.Approved
    }

    data class Error(
        val message: String,
        val retryable: Boolean,
    ) : PaymentUiState
}

data class PaymentMethodUiModel(
    val id: String,
    val label: String,
)

data class PaymentCouponUiModel(
    val id: String,
    val label: String,
    val discountAmount: Int,
)

sealed interface PaymentProgress {
    data object Idle : PaymentProgress
    data object Processing : PaymentProgress
    data object Approved : PaymentProgress
    data class Failed(val failure: OrderFailureUiModel) : PaymentProgress {
        val message: String = failure.message
    }
    data class NeedsConfirmation(val message: String) : PaymentProgress
}

sealed interface PaymentEvent {
    data class PaymentApproved(val orderId: String) : PaymentEvent
    data class PaymentFailed(val orderId: String) : PaymentEvent
}

fun defaultPaymentMethods(): List<PaymentMethodUiModel> =
    listOf(
        PaymentMethodUiModel(
            id = "credit-card",
            label = "신용카드",
        ),
        PaymentMethodUiModel(
            id = "simple-pay",
            label = "간편결제",
        ),
        PaymentMethodUiModel(
            id = "kakaopay",
            label = "카카오페이",
        ),
    )

enum class PaymentFailureReason {
    LimitExceeded,
    Cancelled,
    InvalidPaymentInfo,
    Network,
    Unknown,
}

fun paymentFailureUiModel(reason: PaymentFailureReason): OrderFailureUiModel =
    when (reason) {
        PaymentFailureReason.LimitExceeded -> OrderFailureUiModel(
            title = "결제에 실패했어요",
            message = "카드 한도 초과 또는 정보 오류로\n결제가 처리되지 않았어요.",
            errorCode = "ERR_PAY_LIMIT_EX",
        )

        PaymentFailureReason.Cancelled -> OrderFailureUiModel(
            title = "결제에 실패했어요",
            message = "결제가 취소됐어요.\n다시 시도해 주세요.",
            errorCode = "ERR_PAY_CANCELLED",
        )

        PaymentFailureReason.InvalidPaymentInfo -> OrderFailureUiModel(
            title = "결제에 실패했어요",
            message = "결제수단 정보가 올바르지 않아요.\n다른 수단으로 다시 시도해 주세요.",
            errorCode = "ERR_PAY_METHOD_INVALID",
        )

        PaymentFailureReason.Network -> OrderFailureUiModel(
            title = "결제에 실패했어요",
            message = "네트워크 연결 때문에 결제가 완료되지 않았어요.\n다시 시도해 주세요.",
            errorCode = "ERR_PAY_NETWORK",
        )

        PaymentFailureReason.Unknown -> OrderFailureUiModel(
            title = "결제에 실패했어요",
            message = "결제가 처리되지 않았어요.\n잠시 후 다시 시도해 주세요.",
            errorCode = "ERR_PAY_UNKNOWN",
        )
    }
