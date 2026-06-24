package com.cafeminsu.core.network.service

import com.cafeminsu.core.network.model.response.payment.StorePaymentsResponse
import com.cafeminsu.core.network.model.response.payment.StoreSalesSummaryResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OwnerPaymentService {
    @GET("api/stores/{storeId}/sales-summary")
    suspend fun getSalesSummary(
        @Path("storeId") storeId: Long,
        @Query("from") from: String,
        @Query("to") to: String,
    ): StoreSalesSummaryResponse

    @GET("api/stores/{storeId}/payments")
    suspend fun getStorePayments(
        @Path("storeId") storeId: Long,
        @Query("from") from: String,
        @Query("to") to: String,
    ): StorePaymentsResponse
}
