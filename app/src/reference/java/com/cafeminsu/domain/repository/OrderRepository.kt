package com.cafeminsu.domain.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.Cart
import com.cafeminsu.domain.model.Order
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    suspend fun createOrderFromCart(cart: Cart): AppResult<Order>
    fun observeOrder(orderId: String): Flow<AppResult<Order>>
    fun observeOrderHistory(): Flow<AppResult<List<Order>>>
}
