package com.cafeminsu.domain.model

enum class SalesPeriod {
    Today,
    Week,
    Month,
}

data class TopMenu(
    val rank: Int,
    val name: String,
    val soldCount: Int,
    val sales: Int,
)

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
