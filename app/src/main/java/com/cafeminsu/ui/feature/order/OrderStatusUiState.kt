package com.cafeminsu.ui.feature.order

import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus

sealed interface OrderStatusUiState {
    data object Loading : OrderStatusUiState

    data class Content(
        val orderId: String,
        val orderNumber: String,
        val status: OrderStatus,
        val headerTitle: String,
        val statusMessage: String,
        val items: List<CartItem>,
        val totalAmount: Int,
        val steps: List<OrderStatusStepUiModel>,
    ) : OrderStatusUiState

    data class Error(
        val message: String,
        val retryable: Boolean,
    ) : OrderStatusUiState
}

data class OrderStatusStepUiModel(
    val status: OrderStatus,
    val label: String,
    val description: String,
    val state: OrderStatusStepState,
)

enum class OrderStatusStepState {
    Completed,
    Current,
    Upcoming,
}

fun Order.toOrderStatusContent(): OrderStatusUiState.Content =
    OrderStatusUiState.Content(
        orderId = id,
        orderNumber = orderNumber,
        status = status,
        headerTitle = status.orderStatusHeaderTitle(),
        statusMessage = status.orderStatusMessage(),
        items = items,
        totalAmount = totalAmount,
        steps = orderStatusSteps(status),
    )

fun OrderStatus.orderStatusLabel(): String =
    when (this) {
        OrderStatus.PendingPayment -> "결제 확인 중"
        OrderStatus.Paid -> "결제 완료"
        OrderStatus.Accepted -> "주문 접수"
        OrderStatus.Preparing -> "준비 중"
        OrderStatus.Ready -> "픽업 준비 완료"
        OrderStatus.Completed -> "픽업 완료"
        OrderStatus.Cancelled -> "주문 취소"
        OrderStatus.Failed -> "주문 실패"
    }

fun OrderStatus.orderStatusDescription(): String =
    when (this) {
        OrderStatus.PendingPayment -> "결제 상태를 확인하고 있어요"
        OrderStatus.Paid -> "결제가 완료되어 주문을 전달했어요"
        OrderStatus.Accepted -> "매장에서 주문을 확인했어요"
        OrderStatus.Preparing -> "음료를 만들고 있어요"
        OrderStatus.Ready -> "카운터에서 찾아가세요"
        OrderStatus.Completed -> "픽업이 완료됐어요"
        OrderStatus.Cancelled -> "주문이 취소됐어요"
        OrderStatus.Failed -> "주문을 완료하지 못했어요"
    }

fun OrderStatus.orderStatusHeaderTitle(): String =
    when (this) {
        OrderStatus.PendingPayment -> "결제 확인 중이에요"
        OrderStatus.Paid,
        OrderStatus.Accepted,
        OrderStatus.Preparing,
        OrderStatus.Ready,
        -> "주문이 들어갔어요"

        OrderStatus.Completed -> "픽업이 완료됐어요"
        OrderStatus.Cancelled -> "주문이 취소됐어요"
        OrderStatus.Failed -> "주문을 확인해 주세요"
    }

fun OrderStatus.orderStatusMessage(): String =
    when (this) {
        OrderStatus.PendingPayment -> "결제 확정 전에는 주문 완료로 표시하지 않아요."
        OrderStatus.Paid -> "주문번호를 확인하고 잠시만 기다려 주세요."
        OrderStatus.Accepted -> "매장에서 주문을 확인했어요."
        OrderStatus.Preparing -> "음료를 준비하고 있어요."
        OrderStatus.Ready -> "픽업대에서 주문번호를 보여 주세요."
        OrderStatus.Completed -> "이용해 주셔서 감사합니다."
        OrderStatus.Cancelled -> "필요하면 다시 주문해 주세요."
        OrderStatus.Failed -> "결제 또는 주문 상태를 다시 확인해 주세요."
    }

fun orderStatusSteps(status: OrderStatus): List<OrderStatusStepUiModel> {
    if (status == OrderStatus.Cancelled || status == OrderStatus.Failed) {
        return listOf(status.toStep(OrderStatusStepState.Current))
    }

    val currentIndex = activeOrderStatuses.indexOf(status)
    return activeOrderStatuses.mapIndexed { index, stepStatus ->
        val state = when {
            index < currentIndex -> OrderStatusStepState.Completed
            index == currentIndex -> OrderStatusStepState.Current
            else -> OrderStatusStepState.Upcoming
        }
        stepStatus.toStep(state)
    }
}

private fun OrderStatus.toStep(state: OrderStatusStepState): OrderStatusStepUiModel =
    OrderStatusStepUiModel(
        status = this,
        label = orderStatusLabel(),
        description = orderStatusDescription(),
        state = state,
    )

private val activeOrderStatuses = listOf(
    OrderStatus.PendingPayment,
    OrderStatus.Paid,
    OrderStatus.Accepted,
    OrderStatus.Preparing,
    OrderStatus.Ready,
    OrderStatus.Completed,
)
