package com.cafeminsu.data.local.order

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 고객 주문 내역(HISTORY) 오프라인 캐시 행.
 *
 * 캐시 전용이라 도메인 [com.cafeminsu.domain.model.Order] 의 단순 투영만 보관한다.
 * enum(status) 은 name 문자열로, 항목(`List<CartItem>`) 은 [OrderCacheMapper] 에서 Moshi JSON 으로
 * 직렬화해 [itemsJson] 한 컬럼에 담는다. 결제 카드정보·토큰은 도메인에도 캐시에도 담지 않는다(SECURITY.md).
 */
@Entity(tableName = "order_history")
data class OrderEntity(
    @PrimaryKey val id: String,
    val orderNumber: String,
    val totalAmount: Int,
    val status: String,
    val createdAtMillis: Long,
    val itemsJson: String,
)
