package com.ssafy.cafeminsu.core.model.sales

sealed interface SalesTrend {
    data object Unavailable : SalesTrend

    data class Percent(val value: Int) : SalesTrend
}
