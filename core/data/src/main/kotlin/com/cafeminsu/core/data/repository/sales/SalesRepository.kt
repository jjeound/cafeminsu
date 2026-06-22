package com.cafeminsu.core.data.repository.sales

import com.cafeminsu.core.model.sales.SalesPeriod
import com.cafeminsu.core.model.sales.SalesSummary
import kotlinx.coroutines.flow.Flow

interface SalesRepository {
    fun observeSales(period: SalesPeriod): Flow<SalesSummary>
}
