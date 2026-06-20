package com.cafeminsu.ui.feature.payment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PaymentUiStateTest {
    @Test
    fun paymentMethodsExposeLabelsWithoutPaymentTokens() {
        val methods = defaultPaymentMethods()

        assertEquals(listOf("신용카드", "간편결제", "쿠폰"), methods.map { it.label })
        assertFalse(methods.any { method -> method.id.contains("tok_") })
    }

    @Test
    fun contentDisablesPayWhileProcessingOrApproved() {
        val content = PaymentUiState.Content(
            orderId = "order-1",
            orderNumber = "M001",
            items = emptyList(),
            totalAmount = 12_000,
            methods = defaultPaymentMethods(),
            selectedMethodId = defaultPaymentMethods().first().id,
            paymentState = PaymentProgress.Idle,
        )

        assertEquals(true, content.isPayEnabled)
        assertEquals(false, content.copy(paymentState = PaymentProgress.Processing).isPayEnabled)
        assertEquals(false, content.copy(paymentState = PaymentProgress.Approved).isPayEnabled)
    }
}
