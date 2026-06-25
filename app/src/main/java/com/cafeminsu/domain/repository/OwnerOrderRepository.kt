package com.cafeminsu.domain.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import kotlinx.coroutines.flow.Flow

interface OwnerOrderRepository {
    fun observeIncomingOrders(filter: OrderStatus? = null): Flow<AppResult<List<Order>>>
    suspend fun advanceStatus(orderId: String, to: OrderStatus): AppResult<Order>
}
