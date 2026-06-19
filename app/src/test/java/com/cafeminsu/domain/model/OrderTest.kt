package com.cafeminsu.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OrderTest {
    @Test
    fun exposesOrderAndPaymentDomainModels() {
        val order = Order(
            id = "order-1",
            orderNumber = "A012",
            items = emptyList(),
            totalAmount = 12000,
            status = OrderStatus.PendingPayment,
            createdAtMillis = 1_725_000_000_000L,
        )
        val paymentRequest = PaymentRequest(
            orderId = order.id,
            amount = order.totalAmount,
            paymentMethodToken = "pg-token",
            idempotencyKey = "idem-1",
        )
        val paymentResult = PaymentResult(
            orderId = order.id,
            paymentId = "payment-1",
            status = PaymentStatus.Unknown,
            approvedAtMillis = null,
        )

        assertEquals(OrderStatus.PendingPayment, order.status)
        assertEquals(12000, paymentRequest.amount)
        assertEquals("pg-token", paymentRequest.paymentMethodToken)
        assertEquals(PaymentStatus.Unknown, paymentResult.status)
        assertNull(paymentResult.approvedAtMillis)
    }

    @Test
    fun exposesAllOrderAndPaymentStatuses() {
        assertEquals(8, OrderStatus.entries.size)
        assertEquals(5, PaymentStatus.entries.size)
    }
}
