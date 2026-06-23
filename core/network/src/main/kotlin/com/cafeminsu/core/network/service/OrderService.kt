package com.cafeminsu.core.network.service

import com.cafeminsu.core.network.model.request.order.OrderCreateRequest
import com.cafeminsu.core.network.model.response.order.OrderCreateResponse
import com.skydoves.sandwich.ApiResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface OrderService {
    @POST("api/orders")
    suspend fun createOrder(@Body request: OrderCreateRequest): ApiResponse<OrderCreateResponse>
}
