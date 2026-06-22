package com.cafeminsu.data.proximity

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.proximity.ProximitySignal
import com.cafeminsu.domain.time.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SimulatedProximityScannerTest {
    @Test
    fun beforeStartEmitsNothing() = runTest {
        val scanner = SimulatedProximityScanner(FixedClock(NowMillis))

        scanner.observe().test {
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun startEmitsApproachingSeriesThenCompletes() = runTest {
        val scanner = SimulatedProximityScanner(FixedClock(NowMillis))

        scanner.observe().test {
            scanner.start()

            val first = awaitSignal()
            val second = awaitSignal()
            val third = awaitSignal()
            val arrived = awaitSignal()
            awaitComplete()

            // 도착까지 남은 시간이 점점 줄어든다(고객이 매장에 가까워진다).
            assertEquals(
                listOf(90, 60, 30, 0),
                listOf(first, second, third, arrived).map { it.estimatedArrivalSeconds },
            )
            // 가까워질수록 신호 세기(rssi)가 0에 가까워진다(강해진다).
            assertTrue(arrived.rssi > first.rssi)
            assertEquals(NowMillis, first.atMillis)
        }
    }

    @Test
    fun stopAfterStartCompletesTheStream() = runTest(UnconfinedTestDispatcher()) {
        val scanner = SimulatedProximityScanner(FixedClock(NowMillis))

        scanner.observe().test {
            scanner.start()
            // Unconfined: 첫 신호 방출 후 다음 delay 에서 멈춘 상태.
            assertEquals(90, awaitSignal().estimatedArrivalSeconds)

            scanner.stop()
            // 중단되면 더 방출하지 않고 흐름이 완료된다.
            awaitComplete()
        }
    }

    private suspend fun ReceiveTurbine<AppResult<ProximitySignal>>.awaitSignal(): ProximitySignal =
        when (val item = awaitItem()) {
            is AppResult.Success -> item.data
            is AppResult.Failure -> error("Expected a proximity signal but was $item")
        }

    private class FixedClock(private val now: Long) : Clock {
        override fun nowMillis(): Long = now
    }

    private companion object {
        const val NowMillis = 1_750_000_000_000L
    }
}
