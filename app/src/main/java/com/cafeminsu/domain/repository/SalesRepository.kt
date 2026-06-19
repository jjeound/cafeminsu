package com.cafeminsu.domain.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.SalesPeriod
import com.cafeminsu.domain.model.SalesSummary
import kotlinx.coroutines.flow.Flow

interface SalesRepository {
    fun observeSales(period: SalesPeriod): Flow<AppResult<SalesSummary>>
}
