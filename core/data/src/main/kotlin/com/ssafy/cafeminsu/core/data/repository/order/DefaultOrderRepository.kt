package com.ssafy.cafeminsu.core.data.repository.order

import com.ssafy.cafeminsu.core.common.network.CafeMinsuDispatcher
import com.ssafy.cafeminsu.core.common.network.Dispatcher
import com.ssafy.cafeminsu.core.data.model.asExternalModel
import com.ssafy.cafeminsu.core.data.model.asNetworkValue
import com.ssafy.cafeminsu.core.model.order.OrderStatus
import com.ssafy.cafeminsu.core.model.order.OrderSummary
import com.ssafy.cafeminsu.core.network.client.OrderClient
import com.ssafy.cafeminsu.core.network.model.request.order.OrderCancelRequest
import com.ssafy.cafeminsu.core.network.model.request.order.OrderCreateRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class DefaultOrderRepository @Inject constructor(
    private val client: OrderClient,
    @Dispatcher(CafeMinsuDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : OrderRepository {
    override fun createOrder(request: OrderCreateRequest): Flow<OrderSummary> = flow {
        emit(client.createOrder(request).asExternalModel(System.currentTimeMillis()))
    }.flowOn(ioDispatcher)

    override fun getOrder(orderId: Long): Flow<OrderSummary> = flow {
        emit(client.getOrder(orderId).asExternalModel())
    }.flowOn(ioDispatcher)

    override fun getMyOrders(
        status: OrderStatus,
        page: Int,
        size: Int,
    ): Flow<List<OrderSummary>> = flow {
        emit(client.getMyOrders(status.asNetworkValue(), page, size).map { it.asExternalModel() })
    }.flowOn(ioDispatcher)

    override fun getRecentOrders(): Flow<List<OrderSummary>> = flow {
        emit(client.getRecentOrders().map { it.asExternalModel() })
    }.flowOn(ioDispatcher)

    override fun cancelOrder(orderId: Long, request: OrderCancelRequest): Flow<Unit> = flow {
        emit(client.cancelOrder(orderId, request))
    }.flowOn(ioDispatcher)

    override fun reorder(previousOrderId: Long): Flow<OrderSummary> = flow {
        emit(client.reorder(previousOrderId).asExternalModel(System.currentTimeMillis()))
    }.flowOn(ioDispatcher)
}
