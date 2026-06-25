package com.ssafy.cafeminsu.core.network.model.response.order

import com.ssafy.cafeminsu.core.network.model.response.payment.PaymentDetailResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrderDetailResponse(
    @SerialName(value = "orderId") val orderId: Long,
    @SerialName(value = "orderNumber") val orderNumber: String,
    @SerialName(value = "storeId") val storeId: Long,
    @SerialName(value = "storeName") val storeName: String,
    @SerialName(value = "orderType") val orderType: String,
    @SerialName(value = "orderMethod") val orderMethod: String,
    @SerialName(value = "status") val status: String,
    @SerialName(value = "totalAmount") val totalAmount: Int,
    @SerialName(value = "cancelReason") val cancelReason: String?,
    @SerialName(value = "items") val items: List<OrderDetailItemResponse>,
    @SerialName(value = "payment") val payment: PaymentDetailResponse?,
    @SerialName(value = "createdAt") val createdAt: String,
)
