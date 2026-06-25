package com.cafeminsu.ui.feature.owner.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.repository.SelectedOwnerStoreHolder
import com.cafeminsu.domain.auth.OwnerAuthProvider
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.domain.model.OwnerProfile
import com.cafeminsu.domain.model.OwnerStore
import com.cafeminsu.domain.repository.OwnerOrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class OwnerHomeViewModel @Inject constructor(
    private val ownerOrderRepository: OwnerOrderRepository,
    private val ownerAuthProvider: OwnerAuthProvider,
    private val selectedOwnerStoreHolder: SelectedOwnerStoreHolder,
) : ViewModel() {
    private val ownerProfile = MutableStateFlow(DefaultOwnerProfile)
    private val operationError = MutableStateFlow<DomainError?>(null)
    private val processingOrderIds = MutableStateFlow<Set<String>>(emptySet())
    private val isStoreOpenUpdating = MutableStateFlow(false)
    private val stores = MutableStateFlow<List<OwnerStore>>(emptyList())

    // 매장 목록 + 선택 매장 id 를 하나의 헤더 입력으로 묶는다(선택 매장명·드롭다운 노출의 단일 소스).
    private val storeHeader = combine(
        stores,
        selectedOwnerStoreHolder.observe(),
    ) { storeList, selectedId ->
        val selected = storeList.firstOrNull { it.id == selectedId } ?: storeList.firstOrNull()
        StoreHeader(stores = storeList, selectedStore = selected)
    }

    val uiState: StateFlow<OwnerHomeUiState> = combine(
        ownerOrderRepository.observeIncomingOrders(),
        ownerProfile,
        storeHeader,
        operationError,
        combine(processingOrderIds, isStoreOpenUpdating) { ids, updating -> ids to updating },
    ) { orderResult, profile, header, actionError, processing ->
        mapOwnerHomeState(
            orderResult = orderResult,
            ownerProfile = profile,
            storeHeader = header,
            operationError = actionError,
            processingOrderIds = processing.first,
            isStoreOpenUpdating = processing.second,
        )
    }.catch {
        emit(
            OwnerHomeUiState.Error(
                message = "대시보드 정보를 불러오지 못했어요",
                retryable = true,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(StateStopTimeoutMillis),
        initialValue = OwnerHomeUiState.Loading,
    )

    init {
        // 점주 매장 목록을 실연동으로 채운다. 실패해도(빈 계정 등) 대시보드는 폴백 매장명으로 동작(무회귀).
        viewModelScope.launch {
            when (val result = ownerOrderRepository.getStores()) {
                is AppResult.Success -> stores.value = result.data
                is AppResult.Failure -> Unit
            }
        }
    }

    fun retry() {
        operationError.value = null
    }

    fun onSelectStore(storeId: String) {
        selectedOwnerStoreHolder.select(storeId)
    }

    fun setStoreOpen(open: Boolean) {
        if (isStoreOpenUpdating.value) return

        viewModelScope.launch {
            isStoreOpenUpdating.value = true
            when (val result = ownerAuthProvider.setStoreOpen(open)) {
                is AppResult.Success -> {
                    ownerProfile.value = result.data
                    operationError.value = null
                }

                is AppResult.Failure -> {
                    operationError.value = result.error
                }
            }
            isStoreOpenUpdating.value = false
        }
    }

    fun advanceStatus(orderId: String) {
        val content = uiState.value as? OwnerHomeUiState.Content ?: return
        val order = content.pendingOrders.firstOrNull { it.id == orderId } ?: return
        val nextStatus = order.status.nextOwnerStatus() ?: return
        if (processingOrderIds.value.contains(orderId)) return

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

    private fun mapOwnerHomeState(
        orderResult: AppResult<List<Order>>,
        ownerProfile: OwnerProfile,
        storeHeader: StoreHeader,
        operationError: DomainError?,
        processingOrderIds: Set<String>,
        isStoreOpenUpdating: Boolean,
    ): OwnerHomeUiState {
        operationError?.let { return it.toOwnerHomeError() }

        val orders = when (orderResult) {
            is AppResult.Success -> orderResult.data
            is AppResult.Failure -> return orderResult.error.toOwnerHomeError()
        }
        // 실연동 매장명을 우선 쓰고, 목록을 못 받았으면(빈 계정) 프로필 매장명으로 폴백한다.
        val storeName = storeHeader.selectedStore?.name ?: ownerProfile.storeName
        val stats = orders.toOwnerHomeStats()
        if (orders.isEmpty()) {
            return OwnerHomeUiState.Empty(
                storeName = storeName,
                isStoreOpen = ownerProfile.isStoreOpen,
                dateLabel = DashboardDateLabel,
                stats = stats,
                message = "처리할 주문이 없어요",
                isStoreOpenUpdating = isStoreOpenUpdating,
                stores = storeHeader.stores,
                selectedStoreId = storeHeader.selectedStore?.id,
            )
        }

        return OwnerHomeUiState.Content(
            storeName = storeName,
            isStoreOpen = ownerProfile.isStoreOpen,
            dateLabel = DashboardDateLabel,
            stats = stats,
            pendingOrders = orders
                .filter { it.status in DashboardPendingStatuses }
                .sortedByDescending { it.createdAtMillis }
                .take(DashboardOrderLimit)
                .map { it.toOwnerHomeOrderUiModel(processingOrderIds) },
            isStoreOpenUpdating = isStoreOpenUpdating,
            stores = storeHeader.stores,
            selectedStoreId = storeHeader.selectedStore?.id,
        )
    }

    // 매장 목록 + 현재 선택 매장(없으면 첫 매장)을 묶은 헤더 입력.
    private data class StoreHeader(
        val stores: List<OwnerStore>,
        val selectedStore: OwnerStore?,
    )

    private fun List<Order>.toOwnerHomeStats(): OwnerHomeStatsUiModel =
        OwnerHomeStatsUiModel(
            totalSales = sumOf { it.totalAmount },
            orderCount = size,
            newWaitingCount = count { it.status == OrderStatus.Accepted },
        )

    private fun Order.toOwnerHomeOrderUiModel(processingOrderIds: Set<String>): OwnerHomeOrderUiModel =
        OwnerHomeOrderUiModel(
            id = id,
            orderNumberLabel = "#$orderNumber",
            timeLabel = createdAtMillis.toOwnerTimeLabel(),
            status = status,
            statusLabel = status.ownerDashboardStatusLabel(),
            itemSummary = itemSummary(),
            totalAmount = totalAmount,
            actionLabel = status.ownerDashboardActionLabel(),
            isActionInProgress = processingOrderIds.contains(id),
        )

    private fun Order.itemSummary(): String {
        val firstItem = items.firstOrNull() ?: return "주문 항목 없음"
        val extraItemCount = items.size - 1
        return if (extraItemCount > 0) {
            "${firstItem.name} 외 $extraItemCount"
        } else {
            firstItem.name
        }
    }

    private fun Long.toOwnerTimeLabel(): String =
        Instant.ofEpochMilli(this)
            .atZone(SeoulZoneId)
            .format(TimeFormatter)

    private fun OrderStatus.ownerDashboardStatusLabel(): String =
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

    private fun OrderStatus.ownerDashboardActionLabel(): String =
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

    private fun DomainError.toOwnerHomeError(): OwnerHomeUiState.Error =
        OwnerHomeUiState.Error(
            message = toOwnerHomeMessage(),
            retryable = isRetryable(),
        )

    private fun DomainError.toOwnerHomeMessage(): String =
        when (this) {
            DomainError.Network -> "네트워크 연결을 확인하고 다시 시도해 주세요"
            DomainError.Timeout -> "응답이 지연되고 있어요. 잠시 후 다시 시도해 주세요"
            DomainError.Unauthorized -> "점주 로그인이 만료됐어요. 다시 로그인해 주세요"
            DomainError.NotFound -> "주문 정보를 찾지 못했어요"
            is DomainError.Payment -> "결제 정보를 확인하지 못했어요"
            is DomainError.Validation -> "주문 상태를 확인해 주세요"
            DomainError.Unknown -> "대시보드 정보를 불러오지 못했어요"
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
        const val DashboardDateLabel = "6월 19일 (금)"
        const val DashboardOrderLimit = 2

        val DashboardPendingStatuses = setOf(OrderStatus.Accepted, OrderStatus.Preparing)
        val SeoulZoneId: ZoneId = ZoneId.of("Asia/Seoul")
        val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)
        val DefaultOwnerProfile = OwnerProfile(
            id = "owner-demo",
            storeId = "store-gangnam",
            storeName = "강남점",
            loginId = "owner",
            isStoreOpen = true,
        )
    }
}
