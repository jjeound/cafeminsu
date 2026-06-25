package com.cafeminsu.domain.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.domain.model.OwnerStore
import kotlinx.coroutines.flow.Flow

interface OwnerOrderRepository {
    fun observeIncomingOrders(filter: OrderStatus? = null): Flow<AppResult<List<Order>>>
    suspend fun advanceStatus(orderId: String, to: OrderStatus): AppResult<Order>

    /** 점주가 운영하는 매장 목록(stores/my). 대시보드 매장명 표시·선택의 단일 소스. */
    suspend fun getStores(): AppResult<List<OwnerStore>>
}
