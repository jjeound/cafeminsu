package com.cafeminsu.core.data.repository.payment

import com.cafeminsu.core.common.network.CafeMinsuDispatcher
import com.cafeminsu.core.common.network.Dispatcher
import com.cafeminsu.core.data.model.asExternalModel
import com.cafeminsu.core.model.sales.StorePaymentHistory
import com.cafeminsu.core.model.sales.StoreSalesSummary
import com.cafeminsu.core.network.client.OwnerPaymentClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class DefaultOwnerPaymentRepository @Inject constructor(
    private val client: OwnerPaymentClient,
    @Dispatcher(CafeMinsuDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : OwnerPaymentRepository {
    override fun getSalesSummary(storeId: Long, from: String, to: String): Flow<StoreSalesSummary> = flow {
        emit(client.getSalesSummary(storeId, from, to).asExternalModel())
    }.flowOn(ioDispatcher)

    override fun getPaymentHistory(storeId: Long, from: String, to: String): Flow<StorePaymentHistory> = flow {
        emit(client.getStorePayments(storeId, from, to).asExternalModel())
    }.flowOn(ioDispatcher)
}
