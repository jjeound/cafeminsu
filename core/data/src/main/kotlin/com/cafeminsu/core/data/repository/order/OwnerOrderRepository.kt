package com.cafeminsu.core.data.repository.order

import com.cafeminsu.core.model.order.OrderSummary
import com.cafeminsu.core.model.order.OrderStatus
import kotlinx.coroutines.flow.Flow

interface OwnerOrderRepository {
    fun observeIncomingOrders(filter: OrderStatus? = null): Flow<List<OrderSummary>>

    fun advanceStatus(orderId: Long, to: OrderStatus): Flow<OrderSummary>
}
