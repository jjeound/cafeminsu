package com.cafeminsu.core.data.model

import com.cafeminsu.core.database.model.entity.order.OrderEntity
import com.cafeminsu.core.model.order.OrderStatus
import com.cafeminsu.core.model.order.OrderSummary
import com.cafeminsu.core.network.model.response.order.OrderCreateResponse

fun OrderCreateResponse.asEntity(createdAtMillis: Long): OrderEntity =
    OrderEntity(id = orderId, orderNumber = orderNumber, totalAmount = totalAmount, status = status, createdAtMillis = createdAtMillis)

fun OrderEntity.asExternalModel(): OrderSummary =
    OrderSummary(id = id, orderNumber = orderNumber, totalAmount = totalAmount, status = status.asOrderStatus(), createdAtMillis = createdAtMillis)

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
