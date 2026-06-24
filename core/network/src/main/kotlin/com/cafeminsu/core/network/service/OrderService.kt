package com.cafeminsu.core.network.service

import com.cafeminsu.core.network.model.request.order.OrderCreateRequest
import com.cafeminsu.core.network.model.request.order.OrderCancelRequest
import com.cafeminsu.core.network.model.response.order.OrderCreateResponse
import com.cafeminsu.core.network.model.response.order.OrderDetailResponse
import com.cafeminsu.core.network.model.response.order.OrderSummaryResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Query

interface OrderService {

    @POST("api/orders")
    suspend fun createOrder(
        @Body request: OrderCreateRequest,
    ): OrderCreateResponse

    @GET("api/orders/{orderId}")
    suspend fun getOrder(
        @Path("orderId") orderId: Long,
    ): OrderDetailResponse

    @GET("api/orders/my")
    suspend fun getMyOrders(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): List<OrderSummaryResponse>

    @GET("api/orders/my/recent")
    suspend fun getRecentOrders(): List<OrderSummaryResponse>

    @POST("api/orders/{orderId}/cancel")
    suspend fun cancelOrder(
        @Path("orderId") orderId: Long,
        @Body request: OrderCancelRequest,
    )

    @POST("api/orders/reorder/{previousOrderId}")
    suspend fun reorder(
        @Path("previousOrderId") previousOrderId: Long,
    ): OrderCreateResponse
}
