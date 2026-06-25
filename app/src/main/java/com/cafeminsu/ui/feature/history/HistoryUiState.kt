package com.cafeminsu.ui.feature.history

import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed interface HistoryUiState {
    data object Loading : HistoryUiState

    data class Content(
        val activeOrder: HistoryActiveOrderUiModel?,
        val pastOrders: List<HistoryPastOrderUiModel>,
    ) : HistoryUiState

    data class Empty(
        val title: String,
        val message: String,
    ) : HistoryUiState

    data class Error(
        val message: String,
        val retryable: Boolean,
    ) : HistoryUiState
}

data class HistoryActiveOrderUiModel(
    val id: String,
    val orderNumber: String,
    val itemSummary: String,
    val amountLabel: String,
    val steps: List<HistoryStepUiModel>,
)

data class HistoryPastOrderUiModel(
    val id: String,
    val storeName: String,
    val dateLabel: String,
    val itemSummary: String,
    val amountLabel: String,
    val reorderMenuItemId: String?,
)

data class HistoryStepUiModel(
    val label: String,
    val state: HistoryStepState,
)

enum class HistoryStepState {
    Completed,
    Current,
    Upcoming,
}

fun List<Order>.toHistoryUiState(nowMillis: Long): HistoryUiState {
    val sortedOrders = sortedByDescending { order -> order.createdAtMillis }
    // 진행중(PENDING/ACCEPTED/READY) 주문 중 가장 최근 1건만 상단 강조 카드로 노출한다.
    val activeOrder = sortedOrders
        .firstOrNull { order -> order.status.isActiveHistoryStatus() }
        ?.toActiveOrderUiModel()
    // 지난 주문 목록은 완료(DONE) 주문만 노출한다(진행중은 위 강조 카드로만).
    val pastOrders = sortedOrders
        .filter { order -> order.status == OrderStatus.Completed }
        .map { order -> order.toPastOrderUiModel(nowMillis) }

    return if (activeOrder == null && pastOrders.isEmpty()) {
        HistoryUiState.Empty(
            title = EmptyHistoryTitle,
            message = EmptyHistoryMessage,
        )
    } else {
        HistoryUiState.Content(
            activeOrder = activeOrder,
            pastOrders = pastOrders,
        )
    }
}

fun historyOrderSteps(status: OrderStatus): List<HistoryStepUiModel> {
    val currentIndex = status.historyStepIndex()
    return historyStepLabels.mapIndexed { index, label ->
        val state = when {
            index < currentIndex -> HistoryStepState.Completed
            index == currentIndex -> HistoryStepState.Current
            else -> HistoryStepState.Upcoming
        }
        HistoryStepUiModel(
            label = label,
            state = state,
        )
    }
}

private fun Order.toActiveOrderUiModel(): HistoryActiveOrderUiModel =
    HistoryActiveOrderUiModel(
        id = id,
        orderNumber = orderNumber.asDisplayOrderNumber(),
        itemSummary = items.activeItemSummary(),
        amountLabel = formatWon(totalAmount),
        steps = historyOrderSteps(status),
    )

private fun Order.toPastOrderUiModel(nowMillis: Long): HistoryPastOrderUiModel =
    HistoryPastOrderUiModel(
        id = id,
        storeName = storeName.ifBlank { DefaultStoreName },
        dateLabel = formatHistoryDate(
            createdAtMillis = createdAtMillis,
            nowMillis = nowMillis,
        ),
        itemSummary = items.pastItemSummary(),
        amountLabel = formatWon(totalAmount),
        reorderMenuItemId = items.firstOrNull()?.menuItemId,
    )

private fun OrderStatus.isActiveHistoryStatus(): Boolean =
    when (this) {
        OrderStatus.PendingPayment,
        OrderStatus.Paid,
        OrderStatus.Accepted,
        OrderStatus.Preparing,
        OrderStatus.Ready,
        -> true

        OrderStatus.Completed,
        OrderStatus.Cancelled,
        OrderStatus.Failed,
        -> false
    }

private fun OrderStatus.historyStepIndex(): Int =
    when (this) {
        OrderStatus.PendingPayment,
        OrderStatus.Paid,
        -> ReceivedStepIndex

        OrderStatus.Accepted -> AcceptedStepIndex
        OrderStatus.Preparing -> PreparingStepIndex
        OrderStatus.Ready,
        OrderStatus.Completed,
        -> CompleteStepIndex

        OrderStatus.Cancelled,
        OrderStatus.Failed,
        -> ReceivedStepIndex
    }

private fun String.asDisplayOrderNumber(): String =
    if (startsWith(OrderNumberPrefix)) {
        this
    } else {
        "$OrderNumberPrefix$this"
    }

private fun List<CartItem>.activeItemSummary(): String =
    when {
        isEmpty() -> EmptyItemSummary
        size == SingleItemSize -> first().quantitySummary()
        else -> "${first().name} 외 ${size - 1}개"
    }

private fun List<CartItem>.pastItemSummary(): String =
    when {
        isEmpty() -> EmptyItemSummary
        size == SingleItemSize -> first().quantitySummary()
        else -> joinToString(separator = ", ") { item -> item.name }
    }

private fun CartItem.quantitySummary(): String =
    "$name ✕ $quantity"

private fun formatWon(amount: Int): String =
    "${NumberFormat.getNumberInstance(Locale.KOREA).format(amount)}원"

private fun formatHistoryDate(
    createdAtMillis: Long,
    nowMillis: Long,
): String {
    val zone = ZoneId.systemDefault()
    val createdAt = Instant.ofEpochMilli(createdAtMillis).atZone(zone)
    val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
    val createdDate = createdAt.toLocalDate()
    val nowDate = now.toLocalDate()

    return when {
        createdDate == nowDate -> "오늘 ${createdAt.format(timeFormatter)}"
        createdDate == nowDate.minusDays(YesterdayOffsetDays) -> "어제 ${createdAt.format(timeFormatter)}"
        else -> createdAt.format(monthDayFormatter)
    }
}

private val historyStepLabels = listOf("접수", "수락", "준비중", "완료")

private val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm", Locale.KOREA)

private val monthDayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M월 d일", Locale.KOREA)

private const val EmptyHistoryTitle = "아직 주문 내역이 없어요"
private const val EmptyHistoryMessage = "첫 번째 한 잔을 주문해보세요"
private const val EmptyItemSummary = "주문 항목 없음"
private const val DefaultStoreName = "강남역"
private const val OrderNumberPrefix = "#"
private const val SingleItemSize = 1
private const val ReceivedStepIndex = 0
private const val AcceptedStepIndex = 1
private const val PreparingStepIndex = 2
private const val CompleteStepIndex = 3
private const val YesterdayOffsetDays = 1L
