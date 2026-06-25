package com.ssafy.cafeminsu.core.network.client

import com.ssafy.cafeminsu.core.network.model.request.order.OrderCancelRequest
import com.ssafy.cafeminsu.core.network.model.request.order.OrderCreateRequest
import com.ssafy.cafeminsu.core.network.model.response.order.OrderCreateResponse
import com.ssafy.cafeminsu.core.network.model.response.order.OrderDetailResponse
import com.ssafy.cafeminsu.core.network.model.response.order.OrderSummaryResponse
import com.ssafy.cafeminsu.core.network.service.OrderService
import javax.inject.Inject

class OrderClient @Inject constructor(
    private val orderService: OrderService,
) {
    suspend fun createOrder(request: OrderCreateRequest): OrderCreateResponse =
        orderService.createOrder(request)

    suspend fun getOrder(orderId: Long): OrderDetailResponse =
        orderService.getOrder(orderId)

    suspend fun getMyOrders(
        status: String? = null,
        page: Int = 0,
        size: Int = 20,
    ): List<OrderSummaryResponse> =
        orderService.getMyOrders(status, page, size)

    suspend fun getRecentOrders(): List<OrderSummaryResponse> =
        orderService.getRecentOrders()

    suspend fun cancelOrder(orderId: Long, request: OrderCancelRequest) {
        orderService.cancelOrder(orderId, request)
    }

    suspend fun reorder(previousOrderId: Long): OrderCreateResponse =
        orderService.reorder(previousOrderId)
}
