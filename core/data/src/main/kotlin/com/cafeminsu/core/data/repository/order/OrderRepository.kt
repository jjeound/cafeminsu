package com.cafeminsu.core.data.repository.order

import com.cafeminsu.core.model.cart.Cart
import com.cafeminsu.core.model.order.OrderSummary
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    fun createOrderFromCart(cart: Cart): Flow<OrderSummary>

    fun observeOrder(orderId: Long): Flow<OrderSummary>

    fun observeOrderHistory(): Flow<List<OrderSummary>>
}
