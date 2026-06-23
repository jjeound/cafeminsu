package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.core.map
import com.cafeminsu.data.mapper.toOwnerOrderStatus
import com.cafeminsu.data.mapper.toOwnerOrders
import com.cafeminsu.data.mapper.toServerOrderStatus
import com.cafeminsu.data.remote.OrderCancelReq
import com.cafeminsu.data.remote.OrderStatusRes
import com.cafeminsu.data.remote.OwnerOrderApi
import com.cafeminsu.data.remote.runCatchingToAppResult
import com.cafeminsu.di.IoDispatcher
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.domain.repository.OwnerOrderRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

@Singleton
class RealOwnerOrderRepository @Inject constructor(
    private val ownerOrderApi: OwnerOrderApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : OwnerOrderRepository {
    override fun observeIncomingOrders(filter: OrderStatus?): Flow<AppResult<List<Order>>> =
        flow {
            emit(loadIncomingOrders(filter))
        }.flowOn(ioDispatcher)

    private suspend fun loadIncomingOrders(filter: OrderStatus?): AppResult<List<Order>> {
        val storeId = when (val result = resolveStoreId()) {
            // stores/my 가 비어 있으면(테스트 계정 등) 주문 호출 없이 안전하게 빈 목록을 낸다.
            is AppResult.Success -> result.data ?: return AppResult.Success(emptyList())
            is AppResult.Failure -> return result
        }

        return when (
            val response = runCatchingToAppResult {
                ownerOrderApi.getStoreOrders(
                    storeId = storeId,
                    status = filter?.toServerOrderStatus(),
                )
            }
        ) {
            is AppResult.Success -> response.data.toOwnerOrders()
            is AppResult.Failure -> response
        }
    }

    // 점주 매장은 stores/my 첫 매장으로 해석한다. owner-login 응답엔 storeId 가 없어 신뢰하지 않는다.
    private suspend fun resolveStoreId(): AppResult<Long?> =
        when (val response = runCatchingToAppResult { ownerOrderApi.getMyStores() }) {
            is AppResult.Success -> AppResult.Success(response.data.firstOrNull()?.id)
            is AppResult.Failure -> response
        }

    override suspend fun advanceStatus(orderId: String, to: OrderStatus): AppResult<Order> =
        withContext(ioDispatcher) {
            val serverOrderId = orderId.toLongOrNull()
                ?: return@withContext AppResult.Failure(DomainError.NotFound)

            // 서버에 대응 엔드포인트가 없는 Preparing 은 서버 호출 없이 로컬 전이로 확정한다.
            if (to == OrderStatus.Preparing) {
                return@withContext AppResult.Success(advancedOrder(orderId, to))
            }

            val statusResult: AppResult<OrderStatus> = when (to) {
                OrderStatus.Accepted ->
                    runCatchingToAppResult { ownerOrderApi.acceptOrder(serverOrderId) }
                        .confirmedStatus(to)

                OrderStatus.Ready ->
                    runCatchingToAppResult { ownerOrderApi.readyOrder(serverOrderId) }
                        .confirmedStatus(to)

                OrderStatus.Completed ->
                    runCatchingToAppResult { ownerOrderApi.completeOrder(serverOrderId) }
                        .confirmedStatus(to)

                OrderStatus.Cancelled ->
                    runCatchingToAppResult {
                        ownerOrderApi.cancelOrder(serverOrderId, OrderCancelReq(reason = CancelReasonNone))
                    }.map { to }

                else -> return@withContext AppResult.Failure(DomainError.Validation("status"))
            }

            statusResult.map { status -> advancedOrder(orderId, status) }
        }

    // 서버가 확정한 상태를 우선 쓰고(운영 액션도 낙관적 UI 금지), 본문이 없으면 요청 target 으로 확정.
    private fun AppResult<OrderStatusRes?>.confirmedStatus(fallback: OrderStatus): AppResult<OrderStatus> =
        map { response -> response?.status.toOwnerOrderStatus() ?: fallback }

    // 상태 전이 결과는 id/상태만 확정하면 충분하다(전이 응답엔 항목/금액/번호가 없음).
    private fun advancedOrder(orderId: String, status: OrderStatus): Order =
        Order(
            id = orderId,
            orderNumber = EmptyOrderNumber,
            items = emptyList(),
            totalAmount = NoAmount,
            status = status,
            createdAtMillis = NoTimestamp,
        )

    private companion object {
        const val CancelReasonNone = ""
        const val EmptyOrderNumber = ""
        const val NoAmount = 0
        const val NoTimestamp = 0L
    }
}
