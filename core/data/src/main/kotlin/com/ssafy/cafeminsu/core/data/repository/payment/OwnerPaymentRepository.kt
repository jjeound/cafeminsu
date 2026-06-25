package com.ssafy.cafeminsu.core.data.repository.payment

import com.ssafy.cafeminsu.core.model.sales.StorePaymentHistory
import com.ssafy.cafeminsu.core.model.sales.StoreSalesSummary
import kotlinx.coroutines.flow.Flow

interface OwnerPaymentRepository {
    fun getSalesSummary(storeId: Long, from: String, to: String): Flow<StoreSalesSummary>
    fun getPaymentHistory(storeId: Long, from: String, to: String): Flow<StorePaymentHistory>
}
