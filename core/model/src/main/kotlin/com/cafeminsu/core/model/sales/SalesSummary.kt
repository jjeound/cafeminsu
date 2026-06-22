package com.cafeminsu.core.model.sales

data class SalesSummary(
    val period: SalesPeriod,
    val totalSales: Int,
    val orderCount: Int,
    val deltaPercent: Int?,
    val dailySales: List<Int>,
    val topMenus: List<TopMenu>,
    val payoutAmount: Int,
    val payoutDateLabel: String?,
)
