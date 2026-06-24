package com.cafeminsu.core.network.service

import com.cafeminsu.core.network.model.response.order.OrderDetailResponse
import com.cafeminsu.core.network.model.response.order.OrderStatusResponse
import com.cafeminsu.core.network.model.response.order.OwnerOrderSummaryResponse
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface OwnerOrderService {

    @GET("api/stores/{storeId}/orders")
    suspend fun getStoreOrders(
        @Path("storeId") storeId: Long,
        @Query("status") status: String? = null,
        @Query("date") date: String? = null,
    ): List<OwnerOrderSummaryResponse>

    @GET("api/orders/{orderId}")
    suspend fun getOrder(
        @Path("orderId") orderId: Long,
    ): OrderDetailResponse

    @PATCH("api/orders/{orderId}/accept")
    suspend fun acceptOrder(
        @Path("orderId") orderId: Long,
    ): OrderStatusResponse

    @PATCH("api/orders/{orderId}/ready")
    suspend fun markOrderReady(
        @Path("orderId") orderId: Long,
    ): OrderStatusResponse

    @PATCH("api/orders/{orderId}/complete")
    suspend fun completeOrder(
        @Path("orderId") orderId: Long,
    ): OrderStatusResponse
}
