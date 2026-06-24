package com.cafeminsu.core.model.sales

data class StoreSalesSummary(
    val totalSales: Int,
    val dailySales: List<DailySales>,
    val topMenus: List<SoldMenu>,
)

data class DailySales(val date: String, val amount: Int, val orderCount: Int)

data class SoldMenu(val menuId: Long, val name: String, val quantity: Int, val amount: Int)
