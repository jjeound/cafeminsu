package com.cafeminsu.data.mapper

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.remote.MenuSummary
import com.cafeminsu.data.remote.StoreOrderItemRes
import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus

fun List<StoreOrderItemRes>.toOwnerOrders(): AppResult<List<Order>> {
    val mapped = map { item ->
        when (val result = item.toOwnerOrder()) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> return result
        }
    }
    return AppResult.Success(mapped)
}

private fun StoreOrderItemRes.toOwnerOrder(): AppResult<Order> {
    val id = orderId ?: return AppResult.Failure(DomainError.Unknown)
    val mappedStatus = status.toOwnerOrderStatus()
        ?: return AppResult.Failure(DomainError.Unknown)

    return AppResult.Success(
        Order(
            id = id.toString(),
            orderNumber = orderNumber.orEmpty(),
            items = items.orEmpty().toOwnerCartItems(orderId = id),
            totalAmount = totalAmount ?: OwnerDefaultAmount,
            status = mappedStatus,
            // 날짜 파싱은 고객 OrderMapper 의 헬퍼를 재사용한다.
            createdAtMillis = createdAt.toEpochMillis(),
        ),
    )
}

private fun List<MenuSummary>.toOwnerCartItems(orderId: Long): List<CartItem> =
    mapNotNull { summary ->
        val menuId = summary.menuId ?: return@mapNotNull null
        CartItem(
            id = "$orderId-$menuId",
            menuItemId = menuId.toString(),
            name = summary.menuName.orEmpty(),
            // 요약 응답엔 단가가 없어 0 으로 둔다(점주 큐는 총액만 표시).
            unitPrice = OwnerDefaultAmount,
            selectedOptions = emptyList(),
            quantity = summary.quantity ?: OwnerDefaultQuantity,
        )
    }

// 점주 큐는 결제 완료된 주문만 들어온다 — 서버 PENDING(접수 대기)을 도메인 Paid 로 본다.
internal fun String?.toOwnerOrderStatus(): OrderStatus? =
    when (this) {
        "PENDING" -> OrderStatus.Paid
        "ACCEPTED" -> OrderStatus.Accepted
        "READY" -> OrderStatus.Ready
        "DONE" -> OrderStatus.Completed
        "CANCELLED" -> OrderStatus.Cancelled
        else -> null
    }

// 도메인 상태 → 서버 주문 상태 쿼리. 서버에 대응이 없는 상태는 null(필터 미적용).
internal fun OrderStatus.toServerOrderStatus(): String? =
    when (this) {
        OrderStatus.Paid -> "PENDING"
        OrderStatus.Accepted -> "ACCEPTED"
        OrderStatus.Ready -> "READY"
        OrderStatus.Completed -> "DONE"
        OrderStatus.Cancelled -> "CANCELLED"
        OrderStatus.PendingPayment,
        OrderStatus.Preparing,
        OrderStatus.Failed,
        -> null
    }

private const val OwnerDefaultAmount = 0
private const val OwnerDefaultQuantity = 1
