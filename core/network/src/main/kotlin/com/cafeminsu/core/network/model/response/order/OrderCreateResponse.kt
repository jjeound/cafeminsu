package com.cafeminsu.core.network.model.response.order

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.cafeminsu.core.model.order.OrderStatus
import com.cafeminsu.core.model.order.OrderSummary

@Serializable
data class OrderCreateResponse(
    @SerialName(value = "orderId") val orderId: Long,
    @SerialName(value = "orderNumber") val orderNumber: String,
    @SerialName(value = "totalAmount") val totalAmount: Int,
    @SerialName(value = "status") val status: String,
)

fun OrderCreateResponse.asExternalModel(createdAtMillis: Long): OrderSummary =
    OrderSummary(
        id = orderId,
        orderNumber = orderNumber,
        totalAmount = totalAmount,
        status = status.asOrderStatus(),
        createdAtMillis = createdAtMillis,
    )

private fun String.asOrderStatus(): OrderStatus =
    when (this) {
        "PENDING" -> OrderStatus.PendingPayment
        "PAID" -> OrderStatus.Paid
        "ACCEPTED" -> OrderStatus.Accepted
        "PREPARING" -> OrderStatus.Preparing
        "READY" -> OrderStatus.Ready
        "DONE" -> OrderStatus.Completed
        "CANCELLED" -> OrderStatus.Cancelled
        else -> OrderStatus.Failed
    }
