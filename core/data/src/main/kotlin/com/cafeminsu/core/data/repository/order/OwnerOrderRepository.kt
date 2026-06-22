package com.cafeminsu.core.data.repository.order

import com.cafeminsu.core.model.order.Order
import com.cafeminsu.core.model.order.OrderStatus
import kotlinx.coroutines.flow.Flow

interface OwnerOrderRepository {
    fun observeIncomingOrders(filter: OrderStatus? = null): Flow<List<Order>>

    fun advanceStatus(orderId: String, to: OrderStatus): Flow<Order>
}
