package com.cafeminsu.core.data.repository.order

import com.cafeminsu.core.model.cart.Cart
import com.cafeminsu.core.model.order.Order
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    fun createOrderFromCart(cart: Cart): Flow<Order>

    fun observeOrder(orderId: String): Flow<Order>

    fun observeOrderHistory(): Flow<List<Order>>
}
