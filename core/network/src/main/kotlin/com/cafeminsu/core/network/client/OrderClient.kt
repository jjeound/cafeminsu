package com.cafeminsu.core.network.client

import com.cafeminsu.core.network.model.request.order.OrderCreateRequest
import com.cafeminsu.core.network.model.response.order.OrderCreateResponse
import com.cafeminsu.core.network.service.OrderService
import com.skydoves.sandwich.ApiResponse
import javax.inject.Inject

class OrderClient @Inject constructor(
    private val orderService: OrderService,
) {
    suspend fun createOrder(request: OrderCreateRequest): ApiResponse<OrderCreateResponse> =
        orderService.createOrder(request)
}
