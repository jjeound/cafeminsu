package com.cafeminsu.ui.feature.owner.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.domain.proximity.ProximitySignal
import com.cafeminsu.domain.proximity.ProximitySignalRepository
import com.cafeminsu.domain.proximity.toProximityInput
import com.cafeminsu.domain.repository.OwnerOrderRepository
import com.cafeminsu.domain.scheduling.CongestionCalculator
import com.cafeminsu.domain.scheduling.CongestionLevel
import com.cafeminsu.domain.scheduling.OrderScheduler
import com.cafeminsu.domain.scheduling.PrepTimeEstimator
import com.cafeminsu.domain.scheduling.ProximityInput
import com.cafeminsu.domain.scheduling.ScheduledOrder
import com.cafeminsu.domain.scheduling.SchedulingSignals
import com.cafeminsu.domain.time.Clock
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.math.ceil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class OwnerOrdersViewModel @Inject constructor(
    private val ownerOrderRepository: OwnerOrderRepository,
    private val orderScheduler: OrderScheduler,
    private val congestionCalculator: CongestionCalculator,
    private val prepTimeEstimator: PrepTimeEstimator,
    private val proximitySignalRepository: ProximitySignalRepository,
    private val clock: Clock,
) : ViewModel() {
    private val selectedFilter = MutableStateFlow(OwnerOrdersFilter.New)
    private val operationError = MutableStateFlow<DomainError?>(null)
    private val processingOrderIds = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<OwnerOrdersUiState> = combine(
        ownerOrderRepository.observeIncomingOrders(),
        selectedFilter,
        operationError,
        processingOrderIds,
        proximitySignalRepository.observe(),
    ) { orderResult, filter, actionError, processingIds, proximitySignals ->
        mapOwnerOrdersState(
            orderResult = orderResult,
            selectedFilter = filter,
            operationError = actionError,
            processingOrderIds = processingIds,
            proximitySignals = proximitySignals,
        )
    }.catch {
        emit(
            OwnerOrdersUiState.Error(
                message = "주문 정보를 불러오지 못했어요",
                retryable = true,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(StateStopTimeoutMillis),
        initialValue = OwnerOrdersUiState.Loading,
    )

    fun selectFilter(filter: OwnerOrdersFilter) {
        selectedFilter.value = filter
        operationError.value = null
    }

    fun retry() {
        operationError.value = null
    }

    fun advanceStatus(orderId: String) {
        if (processingOrderIds.value.contains(orderId)) return

        val order = when (val state = uiState.value) {
            is OwnerOrdersUiState.Content -> state.orders.firstOrNull { it.id == orderId }
            is OwnerOrdersUiState.Empty,
            is OwnerOrdersUiState.Error,
            OwnerOrdersUiState.Loading,
            -> null
        } ?: return
        val nextStatus = order.status.nextOwnerStatus() ?: return

        viewModelScope.launch {
            processingOrderIds.value = processingOrderIds.value + orderId
            when (val result = ownerOrderRepository.advanceStatus(orderId = orderId, to = nextStatus)) {
                is AppResult.Success -> {
                    operationError.value = null
                }

                is AppResult.Failure -> {
                    operationError.value = result.error
                }
            }
            processingOrderIds.value = processingOrderIds.value - orderId
        }
    }

    private fun mapOwnerOrdersState(
        orderResult: AppResult<List<Order>>,
        selectedFilter: OwnerOrdersFilter,
        operationError: DomainError?,
        processingOrderIds: Set<String>,
        proximitySignals: Map<String, ProximitySignal>,
    ): OwnerOrdersUiState {
        operationError?.let { return it.toOwnerOrdersError() }

        val orders = when (orderResult) {
            is AppResult.Success -> orderResult.data
            is AppResult.Failure -> return orderResult.error.toOwnerOrdersError()
        }
        val counts = orders.toOwnerOrdersCounts()
        val nowMillis = clock.nowMillis()
        val congestion = congestionCalculator.level(orders.activeOrderCount())
        val filtered = orders.filter { it.status == selectedFilter.status }
        val signals = filtered.associate { order ->
            order.id to order.toSchedulingSignals(
                nowMillis = nowMillis,
                congestion = congestion,
                // 비콘 근접 신호가 있으면 스케줄러 입력으로 변환해 우선순위·ArrivingSoon 뱃지에 반영.
                proximity = proximitySignals[order.id]?.toProximityInput(),
            )
        }
        val filteredOrders = orderScheduler
            .schedule(orders = filtered, signals = signals, nowMillis = nowMillis)
            .map { it.toOwnerOrdersOrderUiModel(processingOrderIds, nowMillis) }

        if (filteredOrders.isEmpty()) {
            return OwnerOrdersUiState.Empty(
                selectedFilter = selectedFilter,
                counts = counts,
                message = "새 주문이 없어요",
            )
        }

        return OwnerOrdersUiState.Content(
            selectedFilter = selectedFilter,
            counts = counts,
            orders = filteredOrders,
        )
    }

    private fun List<Order>.toOwnerOrdersCounts(): OwnerOrdersCountsUiModel =
        OwnerOrdersCountsUiModel(
            newCount = count { it.status == OrderStatus.Accepted },
            preparingCount = count { it.status == OrderStatus.Preparing },
            readyCount = count { it.status == OrderStatus.Ready },
        )

    private fun List<Order>.activeOrderCount(): Int =
        count { it.status == OrderStatus.Accepted || it.status == OrderStatus.Preparing }

    private fun Order.toSchedulingSignals(
        nowMillis: Long,
        congestion: CongestionLevel,
        proximity: ProximityInput?,
    ): SchedulingSignals =
        SchedulingSignals(
            orderId = id,
            waitingSeconds = ((nowMillis - createdAtMillis) / MillisPerSecond).coerceAtLeast(0L),
            prepSeconds = prepTimeEstimator.estimateSeconds(this),
            quantity = items.sumOf { it.quantity },
            congestion = congestion,
            proximity = proximity,
        )

    private fun ScheduledOrder.toOwnerOrdersOrderUiModel(
        processingOrderIds: Set<String>,
        nowMillis: Long,
    ): OwnerOrdersOrderUiModel =
        OwnerOrdersOrderUiModel(
            id = order.id,
            orderNumberLabel = "#${order.orderNumber}",
            timeLabel = order.createdAtMillis.toOwnerTimeLabel(),
            status = order.status,
            statusLabel = order.status.ownerOrdersStatusLabel(),
            itemsLabel = order.items.joinToString(separator = "\n") { it.toOwnerOrderItemLine() },
            requestLabel = OwnerRequestLabel,
            totalAmount = order.totalAmount,
            actionLabel = order.status.ownerOrdersActionLabel(),
            isActionInProgress = processingOrderIds.contains(order.id),
            priorityBadge = badge,
            etaLabel = estimatedReadyAtMillis.toEtaLabel(nowMillis),
        )

    private fun Long.toEtaLabel(nowMillis: Long): String? {
        val remainingMillis = this - nowMillis
        if (remainingMillis <= 0L) return null
        val minutes = ceil(remainingMillis.toDouble() / MillisPerMinute).toInt().coerceAtLeast(1)
        return "약 ${minutes}분"
    }

    private fun CartItem.toOwnerOrderItemLine(): String {
        val normalizedName = name
            .replace("(", " (")
            .replace(") ", ") · ")
        val optionNames = selectedOptions
            .map { it.name }
            .filterNot { normalizedName.contains(it) }
        val detail = listOf(normalizedName) + optionNames
        return detail.joinToString(separator = " · ") + " · $quantity"
    }

    private fun Long.toOwnerTimeLabel(): String =
        Instant.ofEpochMilli(this)
            .atZone(SeoulZoneId)
            .format(TimeFormatter)

    private fun OrderStatus.ownerOrdersStatusLabel(): String =
        when (this) {
            OrderStatus.Accepted -> "신규"
            OrderStatus.Preparing -> "준비중"
            OrderStatus.Ready -> "준비완료"
            OrderStatus.Completed -> "픽업완료"
            OrderStatus.PendingPayment,
            OrderStatus.Paid,
            OrderStatus.Cancelled,
            OrderStatus.Failed,
            -> "확인필요"
        }

    private fun OrderStatus.ownerOrdersActionLabel(): String =
        when (this) {
            OrderStatus.Accepted -> "접수하기"
            OrderStatus.Preparing -> "준비완료"
            OrderStatus.Ready -> "픽업완료"
            OrderStatus.PendingPayment,
            OrderStatus.Paid,
            OrderStatus.Completed,
            OrderStatus.Cancelled,
            OrderStatus.Failed,
            -> "확인"
        }

    private fun OrderStatus.nextOwnerStatus(): OrderStatus? =
        when (this) {
            OrderStatus.Accepted -> OrderStatus.Preparing
            OrderStatus.Preparing -> OrderStatus.Ready
            OrderStatus.Ready -> OrderStatus.Completed
            OrderStatus.PendingPayment,
            OrderStatus.Paid,
            OrderStatus.Completed,
            OrderStatus.Cancelled,
            OrderStatus.Failed,
            -> null
        }

    private fun DomainError.toOwnerOrdersError(): OwnerOrdersUiState.Error =
        OwnerOrdersUiState.Error(
            message = toOwnerOrdersMessage(),
            retryable = isRetryable(),
        )

    private fun DomainError.toOwnerOrdersMessage(): String =
        when (this) {
            DomainError.Network -> "네트워크 연결을 확인하고 다시 시도해 주세요"
            DomainError.Timeout -> "응답이 지연되고 있어요. 잠시 후 다시 시도해 주세요"
            DomainError.Unauthorized -> "점주 로그인이 만료됐어요. 다시 로그인해 주세요"
            DomainError.NotFound -> "주문 정보를 찾지 못했어요"
            is DomainError.Payment -> "결제 정보를 확인하지 못했어요"
            is DomainError.Validation -> "주문 상태를 확인해 주세요"
            DomainError.Unknown -> "주문 정보를 불러오지 못했어요"
        }

    private fun DomainError.isRetryable(): Boolean =
        when (this) {
            DomainError.Network,
            DomainError.Timeout,
            DomainError.Unknown,
            -> true

            DomainError.Unauthorized,
            DomainError.NotFound,
            is DomainError.Payment,
            is DomainError.Validation,
            -> false
        }

    private companion object {
        const val StateStopTimeoutMillis = 5_000L
        const val OwnerRequestLabel = "포장 · 요청: 얼음 적게"
        const val MillisPerSecond = 1_000L
        const val MillisPerMinute = 60_000L

        val SeoulZoneId: ZoneId = ZoneId.of("Asia/Seoul")
        val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)
    }
}
