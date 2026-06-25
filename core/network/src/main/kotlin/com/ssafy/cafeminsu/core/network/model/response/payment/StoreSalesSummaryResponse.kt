package com.ssafy.cafeminsu.core.network.model.response.payment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StoreSalesSummaryResponse(
    @SerialName(value = "totalSales") val totalSales: Int,
    @SerialName(value = "dailySales") val dailySales: List<DailySalesResponse>,
    @SerialName(value = "topMenus") val topMenus: List<TopMenuSalesResponse>,
)

@Serializable
data class DailySalesResponse(
    @SerialName(value = "date") val date: String,
    @SerialName(value = "amount") val amount: Int,
    @SerialName(value = "orderCount") val orderCount: Int,
)

@Serializable
data class TopMenuSalesResponse(
    @SerialName(value = "menuId") val menuId: Long,
    @SerialName(value = "name") val name: String,
    @SerialName(value = "quantity") val quantity: Int,
    @SerialName(value = "amount") val amount: Int,
)
