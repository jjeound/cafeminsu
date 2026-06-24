package com.cafeminsu.core.data.repository.order

import com.cafeminsu.core.model.order.OrderSummary
import com.cafeminsu.core.model.order.OrderStatus
import kotlinx.coroutines.flow.Flow

interface OwnerOrderRepository {
    fun getStoreOrders(
        storeId: Long,
        status: OrderStatus = OrderStatus.All,
        date: String? = null,
    ): Flow<List<OrderSummary>>

    fun getOrder(orderId: Long): Flow<OrderSummary>

    fun acceptOrder(orderId: Long): Flow<OrderStatus>

    fun markOrderReady(orderId: Long): Flow<OrderStatus>

    fun completeOrder(orderId: Long): Flow<OrderStatus>
}
