package com.ssafy.cafeminsu.core.model.sales

data class SalesSummary(
    val period: SalesPeriod,
    val totalSales: Int,
    val orderCount: Int,
    val trend: SalesTrend,
    val dailySales: List<Int>,
    val topMenus: List<TopMenu>,
    val payoutAmount: Int,
    val payoutStatus: PayoutStatus,
)
