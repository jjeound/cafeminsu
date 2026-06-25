package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.core.map
import com.cafeminsu.data.mapper.toOwnerOrderStatus
import com.cafeminsu.data.mapper.toOwnerOrders
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
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class RealOwnerOrderRepository @Inject constructor(
    private val ownerOrderApi: OwnerOrderApi,
    private val ownerSelectedStoreHolder: OwnerSelectedStoreHolder,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : OwnerOrderRepository {
    // 점주 화면이 상태 전이(접수/준비완료/픽업완료)를 즉시 반영하도록 주문 목록을 메모리에
    // 캐시한다. advanceStatus 가 이 캐시를 갱신하면 옵저버가 재방출한다(Mock 과 동일 계약).
    private val cachedOrders = MutableStateFlow<AppResult<List<Order>>?>(null)

    override fun observeIncomingOrders(filter: OrderStatus?): Flow<AppResult<List<Order>>> =
        channelFlow {
            // 선택 매장(holder)이 바뀌면 이전 새로고침 루프를 취소하고(collectLatest) 새 매장 기준으로
            // 다시 로드한다. 매장이 정해진 동안에는 RefreshIntervalMillis 간격으로 주문을 계속 새로
            // 불러와 캐시에 반영해 "지금 처리할 주문"이 자동으로 갱신되도록 한다.
            launch {
                ownerSelectedStoreHolder.selectedStoreId.collectLatest { selectedStoreId ->
                    while (coroutineContext.isActive) {
                        cachedOrders.value = loadIncomingOrders(selectedStoreId)
                        delay(RefreshIntervalMillis)
                    }
                }
            }
            // advanceStatus 의 즉시 반영을 위해 캐시를 단일 방출원으로 유지한다.
            cachedOrders
                .filterNotNull()
                .map { result -> result.applyFilter(filter) }
                .collect { send(it) }
        }.flowOn(ioDispatcher)

    private fun AppResult<List<Order>>.applyFilter(filter: OrderStatus?): AppResult<List<Order>> =
        when (this) {
            is AppResult.Success ->
                if (filter == null) this else AppResult.Success(data.filter { it.status == filter })

            is AppResult.Failure -> this
        }

    // 점주 화면은 필터 없이 전체를 불러와 클라이언트에서 탭별로 거른다(applyFilter). 공유 캐시이므로
    // 서버 조회는 항상 전체(status=null)로 한다.
    private suspend fun loadIncomingOrders(selectedStoreId: String?): AppResult<List<Order>> {
        val storeId = when (val result = resolveStoreId(selectedStoreId)) {
            // stores/my 가 비어 있으면(테스트 계정 등) 주문 호출 없이 안전하게 빈 목록을 낸다.
            is AppResult.Success -> result.data ?: return AppResult.Success(emptyList())
            is AppResult.Failure -> return result
        }

        return when (
            val response = runCatchingToAppResult {
                ownerOrderApi.getStoreOrders(
                    storeId = storeId,
                    status = null,
                )
            }
        ) {
            is AppResult.Success -> response.data.toOwnerOrders()
            is AppResult.Failure -> response
        }
    }

    // 점주가 명시적으로 고른 매장이 있으면 그 매장을 쓰고, 아직 없으면(초기) stores/my 첫 매장으로
    // 폴백한다. owner-login 응답엔 storeId 가 없어 신뢰하지 않는다.
    private suspend fun resolveStoreId(selectedStoreId: String?): AppResult<Long?> {
        selectedStoreId?.toLongOrNull()?.let { return AppResult.Success(it) }
        return when (val response = runCatchingToAppResult { ownerOrderApi.getMyStores() }) {
            is AppResult.Success -> AppResult.Success(response.data.firstOrNull()?.id)
            is AppResult.Failure -> response
        }
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

        // 처리할 주문 자동 새로고침 간격. 점주 화면 구독 중에만 주기적으로 다시 불러온다.
        const val RefreshIntervalMillis = 10_000L
    }
}
