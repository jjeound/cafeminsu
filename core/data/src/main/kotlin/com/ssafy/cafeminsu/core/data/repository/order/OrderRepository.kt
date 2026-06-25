package com.ssafy.cafeminsu.core.data.repository.order

import com.ssafy.cafeminsu.core.model.order.OrderStatus
import com.ssafy.cafeminsu.core.model.order.OrderSummary
import com.ssafy.cafeminsu.core.network.model.request.order.OrderCancelRequest
import com.ssafy.cafeminsu.core.network.model.request.order.OrderCreateRequest
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    fun createOrder(request: OrderCreateRequest): Flow<OrderSummary>

    fun getOrder(orderId: Long): Flow<OrderSummary>

    fun getMyOrders(
        status: OrderStatus = OrderStatus.All,
        page: Int = 0,
        size: Int = 20,
    ): Flow<List<OrderSummary>>

    fun getRecentOrders(): Flow<List<OrderSummary>>

    fun cancelOrder(orderId: Long, request: OrderCancelRequest): Flow<Unit>

    fun reorder(previousOrderId: Long): Flow<OrderSummary>

}
