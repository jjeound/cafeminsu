package com.cafeminsu.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 점주가 선택한 매장 id 를 인메모리 [StateFlow] 로 공유한다. 고객용 [SelectedStoreHolder] 는
 * 주문 매장(Store) 영속 holder 라 책임이 달라, 점주 선택은 별도 인메모리 싱글톤으로 둔다.
 * 소비자(Real 점주 리포지토리)는 [current]/[observe] 로 선택 매장을, [select] 로 변경을 받는다.
 */
@Singleton
class SelectedOwnerStoreHolder @Inject constructor() {
    private val selectedStoreId = MutableStateFlow<String?>(null)

    fun observe(): StateFlow<String?> = selectedStoreId

    fun current(): String? = selectedStoreId.value

    fun select(storeId: String) {
        selectedStoreId.value = storeId
    }
}
