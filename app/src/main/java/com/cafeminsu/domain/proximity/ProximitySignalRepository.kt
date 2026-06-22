package com.cafeminsu.domain.proximity

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 최신 근접 신호를 주문별로 보관하는 인메모리 공유 상태(`SelectedStoreHolder` 패턴).
 *
 * 스캐너가 [publish] 로 신호를 넣고, 스케줄링 화면([com.cafeminsu.ui.feature.owner.orders.OwnerOrdersViewModel])이
 * [observe] 로 읽어 [com.cafeminsu.domain.scheduling.SchedulingSignals.proximity] 를 채운다.
 * 근접 값은 로깅하지 않는다(`docs/SECURITY.md §9`).
 */
@Singleton
class ProximitySignalRepository @Inject constructor() {
    private val signals = MutableStateFlow<Map<String, ProximitySignal>>(emptyMap())

    fun observe(): StateFlow<Map<String, ProximitySignal>> = signals

    fun current(): Map<String, ProximitySignal> = signals.value

    fun publish(signal: ProximitySignal) {
        signals.value = signals.value + (signal.orderId to signal)
    }

    fun clear(orderId: String) {
        signals.value = signals.value - orderId
    }
}
