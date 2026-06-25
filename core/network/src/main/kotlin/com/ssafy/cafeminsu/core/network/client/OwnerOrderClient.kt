package com.ssafy.cafeminsu.core.network.client

import com.ssafy.cafeminsu.core.network.model.response.order.OrderDetailResponse
import com.ssafy.cafeminsu.core.network.model.response.order.OrderStatusResponse
import com.ssafy.cafeminsu.core.network.model.response.order.OwnerOrderSummaryResponse
import com.ssafy.cafeminsu.core.network.service.OwnerOrderService
import javax.inject.Inject

class OwnerOrderClient @Inject constructor(
    private val ownerOrderService: OwnerOrderService,
) {
    suspend fun getStoreOrders(
        storeId: Long,
        status: String? = null,
        date: String? = null,
    ): List<OwnerOrderSummaryResponse> = ownerOrderService.getStoreOrders(storeId, status, date)

    suspend fun getOrder(orderId: Long): OrderDetailResponse = ownerOrderService.getOrder(orderId)

    suspend fun acceptOrder(orderId: Long): OrderStatusResponse = ownerOrderService.acceptOrder(orderId)

    suspend fun markOrderReady(orderId: Long): OrderStatusResponse =
        ownerOrderService.markOrderReady(orderId)

    suspend fun completeOrder(orderId: Long): OrderStatusResponse =
        ownerOrderService.completeOrder(orderId)
}
