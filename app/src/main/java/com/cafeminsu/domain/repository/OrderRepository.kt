package com.cafeminsu.domain.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.Cart
import com.cafeminsu.domain.model.Order
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    suspend fun createOrderFromCart(cart: Cart): AppResult<Order>
    fun observeOrder(orderId: String): Flow<AppResult<Order>>
    fun observeOrderHistory(): Flow<AppResult<List<Order>>>

    // 홈 "다시 주문하기"용 최근 주문(orders/my/recent). 전체 내역(observeOrderHistory)과 분리.
    fun observeRecentOrders(): Flow<AppResult<List<Order>>>
}
