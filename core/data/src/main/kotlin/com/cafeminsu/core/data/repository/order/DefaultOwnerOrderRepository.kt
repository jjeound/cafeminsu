package com.cafeminsu.core.data.repository.order

import com.cafeminsu.core.common.network.CafeMinsuDispatcher
import com.cafeminsu.core.common.network.Dispatcher
import com.cafeminsu.core.data.model.asExternalModel
import com.cafeminsu.core.data.model.asNetworkValue
import com.cafeminsu.core.model.order.OrderStatus
import com.cafeminsu.core.model.order.OrderSummary
import com.cafeminsu.core.network.client.OwnerOrderClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class DefaultOwnerOrderRepository @Inject constructor(
    private val client: OwnerOrderClient,
    @Dispatcher(CafeMinsuDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : OwnerOrderRepository {
    override fun getStoreOrders(
        storeId: Long,
        status: OrderStatus,
        date: String?,
    ): Flow<List<OrderSummary>> = flow {
        emit(client.getStoreOrders(storeId, status.asNetworkValue(), date).map { it.asExternalModel() })
    }.flowOn(ioDispatcher)

    override fun getOrder(orderId: Long): Flow<OrderSummary> = flow {
        emit(client.getOrder(orderId).asExternalModel())
    }.flowOn(ioDispatcher)

    override fun acceptOrder(orderId: Long): Flow<OrderStatus> = flow {
        emit(client.acceptOrder(orderId).asExternalModel())
    }.flowOn(ioDispatcher)

    override fun markOrderReady(orderId: Long): Flow<OrderStatus> = flow {
        emit(client.markOrderReady(orderId).asExternalModel())
    }.flowOn(ioDispatcher)

    override fun completeOrder(orderId: Long): Flow<OrderStatus> = flow {
        emit(client.completeOrder(orderId).asExternalModel())
    }.flowOn(ioDispatcher)
}
