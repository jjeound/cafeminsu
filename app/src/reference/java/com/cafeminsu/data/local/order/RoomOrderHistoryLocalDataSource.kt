package com.cafeminsu.data.local.order

import com.cafeminsu.domain.model.Order
import com.squareup.moshi.Moshi
import javax.inject.Inject

/**
 * 고객 주문 내역 로컬 캐시 접근. 리포지토리는 Room DAO 가 아니라 이 인터페이스에 의존해
 * 단위 테스트에서 가짜로 대체할 수 있게 한다.
 */
interface OrderHistoryLocalDataSource {
    suspend fun cachedHistory(): List<Order>

    suspend fun replaceHistory(orders: List<Order>)
}

class RoomOrderHistoryLocalDataSource @Inject constructor(
    private val orderHistoryDao: OrderHistoryDao,
    private val moshi: Moshi,
) : OrderHistoryLocalDataSource {
    override suspend fun cachedHistory(): List<Order> =
        orderHistoryDao.getAll().map { it.toOrder(moshi) }

    override suspend fun replaceHistory(orders: List<Order>) {
        // 목록 전체 교체: 서버에서 사라진 주문이 캐시에 남지 않도록 비운 뒤 다시 채운다.
        orderHistoryDao.clear()
        orderHistoryDao.upsertAll(orders.map { it.toOrderEntity(moshi) })
    }
}
