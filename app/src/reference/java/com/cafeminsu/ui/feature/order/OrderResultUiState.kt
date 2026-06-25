package com.cafeminsu.ui.feature.order

import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.StampCard
import java.text.NumberFormat
import java.util.Locale

sealed interface OrderResultUiState {
    data object Loading : OrderResultUiState

    data class Content(
        val summary: OrderSuccessSummary,
    ) : OrderResultUiState

    data class Failure(
        val message: String,
        val retryable: Boolean,
    ) : OrderResultUiState
}

data class OrderSuccessSummary(
    val orderId: String,
    val orderNumber: String,
    val pickupStoreName: String,
    val estimatedReadyLabel: String,
    val paidAmountLabel: String,
    val stampMessage: String,
)

data class OrderFailureUiModel(
    val title: String,
    val message: String,
    val errorCode: String,
)

internal fun Order.toOrderSuccessSummary(stampCard: StampCard?): OrderSuccessSummary {
    val grantedStampCount = stampCard
        ?.history
        ?.firstOrNull { event -> event.orderId == id }
        ?.count
        ?: DefaultGrantedStampCount
    val stampMessage = if (stampCard != null) {
        "스탬프 ${grantedStampCount}개가 적립됐어요 (${stampCard.currentCount}/${stampCard.goalCount})"
    } else {
        "스탬프 적립 내역을 확인하고 있어요"
    }

    return OrderSuccessSummary(
        orderId = id,
        orderNumber = orderNumber,
        pickupStoreName = DefaultPickupStoreName,
        estimatedReadyLabel = DefaultEstimatedReadyLabel,
        paidAmountLabel = formatWon(totalAmount),
        stampMessage = stampMessage,
    )
}

private fun formatWon(amount: Int): String =
    "${NumberFormat.getNumberInstance(Locale.KOREA).format(amount)}원"

private const val DefaultPickupStoreName = "카페민수 강남점"
private const val DefaultEstimatedReadyLabel = "약 8분 후"
private const val DefaultGrantedStampCount = 1
