package com.cafeminsu.core.data.model

import com.cafeminsu.core.database.model.entity.order.OrderEntity
import com.cafeminsu.core.model.order.OrderStatus
import com.cafeminsu.core.model.order.OrderSummary
import com.cafeminsu.core.network.model.response.order.OrderCreateResponse
import com.cafeminsu.core.network.model.response.order.OrderDetailResponse
import com.cafeminsu.core.network.model.response.order.OrderStatusResponse
import com.cafeminsu.core.network.model.response.order.OrderSummaryResponse
import com.cafeminsu.core.network.model.response.order.OwnerOrderSummaryResponse
import java.time.Instant

fun OrderCreateResponse.asEntity(createdAtMillis: Long): OrderEntity =
    OrderEntity(id = orderId, orderNumber = orderNumber, totalAmount = totalAmount, status = status, createdAtMillis = createdAtMillis)

fun OrderEntity.asExternalModel(): OrderSummary =
    OrderSummary(id = id, orderNumber = orderNumber, totalAmount = totalAmount, status = status.asOrderStatus(), createdAtMillis = createdAtMillis)

fun OrderCreateResponse.asExternalModel(createdAtMillis: Long): OrderSummary =
    OrderSummary(
        id = orderId,
        orderNumber = orderNumber,
        totalAmount = totalAmount,
        status = status.asOrderStatus(),
        createdAtMillis = createdAtMillis,
    )

fun OrderDetailResponse.asExternalModel(): OrderSummary =
    OrderSummary(
        id = orderId,
        orderNumber = orderNumber,
        totalAmount = totalAmount,
        status = status.asOrderStatus(),
        createdAtMillis = Instant.parse(createdAt).toEpochMilli(),
    )

fun OrderSummaryResponse.asExternalModel(): OrderSummary =
    OrderSummary(
        id = orderId,
        orderNumber = orderNumber,
        totalAmount = totalAmount,
        status = status.asOrderStatus(),
        createdAtMillis = Instant.parse(createdAt).toEpochMilli(),
    )

fun OwnerOrderSummaryResponse.asExternalModel(): OrderSummary =
    OrderSummary(
        id = orderId,
        orderNumber = orderNumber,
        totalAmount = totalAmount,
        status = status.asOrderStatus(),
        createdAtMillis = Instant.parse(createdAt).toEpochMilli(),
    )

fun OrderStatusResponse.asExternalModel(): OrderStatus = status.asOrderStatus()

fun OrderStatus.asNetworkValue(): String? =
    when (this) {
        OrderStatus.All -> null
        OrderStatus.PendingPayment -> "PENDING"
        OrderStatus.Paid -> "PAID"
        OrderStatus.Accepted -> "ACCEPTED"
        OrderStatus.Preparing -> "PREPARING"
        OrderStatus.Ready -> "READY"
        OrderStatus.Completed -> "DONE"
        OrderStatus.Cancelled -> "CANCELLED"
        OrderStatus.Failed -> "FAILED"
    }

private fun String.asOrderStatus(): OrderStatus =
    when (this) {
        "ALL" -> OrderStatus.All
        "PENDING" -> OrderStatus.PendingPayment
        "PAID" -> OrderStatus.Paid
        "ACCEPTED" -> OrderStatus.Accepted
        "PREPARING" -> OrderStatus.Preparing
        "READY" -> OrderStatus.Ready
        "DONE" -> OrderStatus.Completed
        "CANCELLED" -> OrderStatus.Cancelled
        else -> OrderStatus.Failed
    }
