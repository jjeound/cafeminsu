package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.core.map
import com.cafeminsu.data.mapper.toOwnerOrderStatus
import com.cafeminsu.data.mapper.toOwnerOrders
import com.cafeminsu.data.mapper.toOwnerStores
import com.cafeminsu.data.mapper.toServerOrderStatus
import com.cafeminsu.data.remote.OrderCancelReq
import com.cafeminsu.data.remote.OrderStatusRes
import com.cafeminsu.data.remote.OwnerOrderApi
import com.cafeminsu.data.remote.runCatchingToAppResult
import com.cafeminsu.di.IoDispatcher
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.domain.model.OwnerStore
import com.cafeminsu.domain.repository.OwnerOrderRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class RealOwnerOrderRepository @Inject constructor(
    private val ownerOrderApi: OwnerOrderApi,
    private val selectedOwnerStoreHolder: SelectedOwnerStoreHolder,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : OwnerOrderRepository {
    // 점주 화면이 상태 전이(접수/준비완료/픽업완료)를 즉시 반영하도록 주문 목록을 메모리에
    // 캐시한다. advanceStatus 가 이 캐시를 갱신하면 옵저버가 재방출한다(Mock 과 동일 계약).
    private val cachedOrders = MutableStateFlow<AppResult<List<Order>>?>(null)
    private val loadMutex = Mutex()

    // 선택 매장이 바뀌면 캐시를 비우고 해당 매장으로 재조회한다(flatMapLatest 가 이전 로드를 취소).
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeIncomingOrders(filter: OrderStatus?): Flow<AppResult<List<Order>>> =
        // StateFlow 라 동일 선택값은 이미 conflate 되어 재방출되지 않는다(별도 distinct 불필요).
        selectedOwnerStoreHolder.observe()
            .flatMapLatest { selectedStoreId ->
                flow {
                    loadMutex.withLock {
                        cachedOrders.value = loadIncomingOrders(filter, selectedStoreId)
                    }
                    emitAll(
                        cachedOrders
                            .filterNotNull()
                            .map { result -> result.applyFilter(filter) },
                    )
                }
            }
            .flowOn(ioDispatcher)

    override suspend fun getStores(): AppResult<List<OwnerStore>> =
        withContext(ioDispatcher) {
            runCatchingToAppResult { ownerOrderApi.getMyStores() }.map { it.toOwnerStores() }
        }

    private fun AppResult<List<Order>>.applyFilter(filter: OrderStatus?): AppResult<List<Order>> =
        when (this) {
            is AppResult.Success ->
                if (filter == null) this else AppResult.Success(data.filter { it.status == filter })

            is AppResult.Failure -> this
        }

    private suspend fun loadIncomingOrders(
        filter: OrderStatus?,
        selectedStoreId: String?,
    ): AppResult<List<Order>> {
        val storeId = when (val result = resolveStoreId(selectedStoreId)) {
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

    // 선택 매장이 있으면 그 매장으로, 없으면 stores/my 첫 매장으로 해석한다(무회귀: 단일 매장은 기존과 동일).
    // owner-login 응답엔 storeId 가 없어 신뢰하지 않는다.
    private suspend fun resolveStoreId(selectedStoreId: String?): AppResult<Long?> =
        when (val response = runCatchingToAppResult { ownerOrderApi.getMyStores() }) {
            is AppResult.Success -> {
                val stores = response.data
                val chosen = selectedStoreId
                    ?.let { id -> stores.firstOrNull { it.id?.toString() == id } }
                    ?: stores.firstOrNull()
                AppResult.Success(chosen?.id)
            }

            is AppResult.Failure -> response
        }

    override suspend fun advanceStatus(orderId: String, to: OrderStatus): AppResult<Order> =
        withContext(ioDispatcher) {
            val serverOrderId = orderId.toLongOrNull()
                ?: return@withContext AppResult.Failure(DomainError.NotFound)

            val statusResult: AppResult<OrderStatus> = when (to) {
                // 접수하기: 신규(PENDING) 주문을 서버 accept 엔드포인트로 접수하고 준비중으로 확정한다.
                OrderStatus.Preparing ->
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

            statusResult.map { status -> applyAdvancedStatus(orderId, status) }
        }

    // 서버가 확정한 상태를 우선 쓰고(운영 액션도 낙관적 UI 금지), 본문이 없으면 요청 target 으로 확정.
    private fun AppResult<OrderStatusRes?>.confirmedStatus(fallback: OrderStatus): AppResult<OrderStatus> =
        map { response -> response?.status.toOwnerOrderStatus() ?: fallback }

    // 캐시의 해당 주문 상태만 갱신해 옵저버가 재방출하도록 한다(항목/금액/번호는 보존).
    // 캐시가 비어 있으면(observe 전에 직접 호출) id/상태만 담은 최소 Order 로 확정한다.
    private fun applyAdvancedStatus(orderId: String, status: OrderStatus): Order {
        var updated: Order? = null
        cachedOrders.update { current ->
            val success = current as? AppResult.Success ?: return@update current
            AppResult.Success(
                success.data.map { order ->
                    if (order.id == orderId) {
                        order.copy(status = status).also { updated = it }
                    } else {
                        order
                    }
                },
            )
        }
        return updated ?: advancedOrder(orderId, status)
    }

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
