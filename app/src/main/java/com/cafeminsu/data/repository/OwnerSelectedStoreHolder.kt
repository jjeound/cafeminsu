package com.cafeminsu.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 점주가 운영 화면에서 고른 활성 매장 id 를 인메모리 [StateFlow] 로 노출한다.
 * 매장 전환([selectStore])은 [com.cafeminsu.data.auth.RealOwnerAuthProvider] 가 기록하고,
 * 주문 목록([RealOwnerOrderRepository]) 이 이를 관찰해 선택 매장 기준으로 다시 로드한다.
 * 고객용 [SelectedStoreHolder] 와는 별개로, 점주 세션 전용이며 로그아웃 시 비운다.
 */
@Singleton
class OwnerSelectedStoreHolder @Inject constructor() {
    private val _selectedStoreId = MutableStateFlow<String?>(null)

    val selectedStoreId: StateFlow<String?> = _selectedStoreId.asStateFlow()

    fun current(): String? = _selectedStoreId.value

    fun select(storeId: String) {
        _selectedStoreId.value = storeId
    }

    fun clear() {
        _selectedStoreId.value = null
    }
}
