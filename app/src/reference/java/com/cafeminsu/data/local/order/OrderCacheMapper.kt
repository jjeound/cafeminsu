package com.cafeminsu.data.local.order

import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

/**
 * 도메인 [Order] ↔ 캐시 [OrderEntity] 순수 매핑.
 *
 * enum(status) 은 name 문자열로 직렬화하고, 항목(`List<CartItem>`) 은 Room 컬럼 하나에 담기 위해
 * Moshi JSON 으로 직렬화한다. 손상된 JSON·미지의 status 는 기본값으로 흡수해 캐시 손상이 화면
 * 오류로 번지지 않게 한다(MenuCacheMapper/StoreCacheMapper 와 동일 정책).
 */
fun Order.toOrderEntity(moshi: Moshi): OrderEntity =
    OrderEntity(
        id = id,
        orderNumber = orderNumber,
        totalAmount = totalAmount,
        status = status.name,
        createdAtMillis = createdAtMillis,
        itemsJson = moshi.cartItemsAdapter().toJson(items),
    )

fun OrderEntity.toOrder(moshi: Moshi): Order =
    Order(
        id = id,
        orderNumber = orderNumber,
        items = moshi.decodeCartItems(itemsJson),
        totalAmount = totalAmount,
        status = status.toOrderStatus(),
        createdAtMillis = createdAtMillis,
    )

private fun String.toOrderStatus(): OrderStatus =
    OrderStatus.entries.firstOrNull { it.name == this } ?: OrderStatus.PendingPayment

private fun Moshi.decodeCartItems(json: String): List<CartItem> =
    runCatching { cartItemsAdapter().fromJson(json) }.getOrNull().orEmpty()

private fun Moshi.cartItemsAdapter(): JsonAdapter<List<CartItem>> {
    val type = Types.newParameterizedType(List::class.java, CartItem::class.java)
    return adapter(type)
}
