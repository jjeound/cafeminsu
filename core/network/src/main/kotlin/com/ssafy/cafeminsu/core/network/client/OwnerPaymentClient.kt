package com.ssafy.cafeminsu.core.network.client

import com.ssafy.cafeminsu.core.network.model.response.payment.StorePaymentsResponse
import com.ssafy.cafeminsu.core.network.model.response.payment.StoreSalesSummaryResponse
import com.ssafy.cafeminsu.core.network.service.OwnerPaymentService
import javax.inject.Inject

class OwnerPaymentClient @Inject constructor(
    private val ownerPaymentService: OwnerPaymentService,
) {
    suspend fun getSalesSummary(storeId: Long, from: String, to: String): StoreSalesSummaryResponse =
        ownerPaymentService.getSalesSummary(storeId, from, to)

    suspend fun getStorePayments(storeId: Long, from: String, to: String): StorePaymentsResponse =
        ownerPaymentService.getStorePayments(storeId, from, to)
}
