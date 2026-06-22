package com.cafeminsu.domain.proximity

import app.cash.turbine.test
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ProximitySignalRepositoryTest {
    @Test
    fun publishedSignalIsObservedAndKeptLatestPerOrder() = runBlocking {
        val repository = ProximitySignalRepository()

        repository.observe().test {
            assertEquals(emptyMap<String, ProximitySignal>(), awaitItem())

            repository.publish(signal(orderId = "order-1", estimatedArrivalSeconds = 90))
            assertEquals(90, awaitItem().getValue("order-1").estimatedArrivalSeconds)

            // 같은 주문의 새 신호는 이전 값을 덮어쓴다(최신 보존).
            repository.publish(signal(orderId = "order-1", estimatedArrivalSeconds = 30))
            val latest = awaitItem()
            assertEquals(1, latest.size)
            assertEquals(30, latest.getValue("order-1").estimatedArrivalSeconds)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun signalsFromDifferentOrdersCoexist() = runBlocking {
        val repository = ProximitySignalRepository()

        repository.publish(signal(orderId = "order-1", estimatedArrivalSeconds = 60))
        repository.publish(signal(orderId = "order-2", estimatedArrivalSeconds = 20))

        val current = repository.current()
        assertEquals(setOf("order-1", "order-2"), current.keys)
        assertEquals(20, current.getValue("order-2").estimatedArrivalSeconds)
    }

    @Test
    fun clearRemovesOnlyTheTargetedOrder() = runBlocking {
        val repository = ProximitySignalRepository()
        repository.publish(signal(orderId = "order-1", estimatedArrivalSeconds = 60))
        repository.publish(signal(orderId = "order-2", estimatedArrivalSeconds = 20))

        repository.clear("order-1")

        assertEquals(setOf("order-2"), repository.current().keys)
    }

    private fun signal(orderId: String, estimatedArrivalSeconds: Int): ProximitySignal =
        ProximitySignal(
            orderId = orderId,
            rssi = -60,
            estimatedArrivalSeconds = estimatedArrivalSeconds,
            atMillis = 1_000L,
        )
}
