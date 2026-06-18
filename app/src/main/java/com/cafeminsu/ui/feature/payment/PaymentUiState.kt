package com.cafeminsu.ui.feature.payment

import com.cafeminsu.domain.model.CartItem

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
    ) : PaymentUiState {
        val selectedMethod: PaymentMethodUiModel? =
            methods.firstOrNull { method -> method.id == selectedMethodId }

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

sealed interface PaymentProgress {
    data object Idle : PaymentProgress
    data object Processing : PaymentProgress
    data object Approved : PaymentProgress
    data class Failed(val message: String) : PaymentProgress
    data class NeedsConfirmation(val message: String) : PaymentProgress
}

sealed interface PaymentEvent {
    data class PaymentApproved(val orderId: String) : PaymentEvent
}

fun defaultPaymentMethods(): List<PaymentMethodUiModel> =
    listOf(
        PaymentMethodUiModel(
            id = "minsupay",
            label = "민수페이",
        ),
        PaymentMethodUiModel(
            id = "registered-card",
            label = "카드(등록됨)",
        ),
    )
