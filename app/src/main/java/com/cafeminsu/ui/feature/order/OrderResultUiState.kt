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
        pickupStoreName = storeName.ifBlank { DefaultPickupStoreName },
        estimatedReadyLabel = DefaultEstimatedReadyLabel,
        paidAmountLabel = formatWon(totalAmount),
        stampMessage = stampMessage,
    )
}

private fun formatWon(amount: Int): String =
    "${NumberFormat.getNumberInstance(Locale.KOREA).format(amount)}원"

// 주문 상세에 매장명이 비어 있을 때만 쓰는 폴백. 실제 매장명은 Order.storeName(서버 주문 상세)에서 온다.
private const val DefaultPickupStoreName = "카페민수"
private const val DefaultEstimatedReadyLabel = "약 8분 후"
private const val DefaultGrantedStampCount = 1
