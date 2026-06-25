package com.cafeminsu.data.repository

import com.cafeminsu.data.local.prefs.UserPreferencesDataStore
import com.cafeminsu.di.ApplicationScope
import com.cafeminsu.domain.model.Store
import com.squareup.moshi.Moshi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 현재 주문 매장을 인메모리 [StateFlow] 로 노출하면서, 선택 매장을 [UserPreferencesDataStore] 에
 * JSON 으로 영속해 앱 재시작 시 복원한다. 소비자(Real* 리포지토리)와의 동기 접근 계약
 * ([current]/[observe]/[select]) 는 그대로 유지한다.
 */
@Singleton
class SelectedStoreHolder @Inject constructor(
    private val preferences: UserPreferencesDataStore,
    moshi: Moshi,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val storeAdapter = moshi.adapter(Store::class.java)
    private val selectedStore = MutableStateFlow<Store?>(null)

    init {
        scope.launch {
            val restored = preferences.observeSelectedStore().first()?.let(::decode)
            // 사용자가 이미 매장을 골랐다면(인메모리 우선) 복원으로 덮어쓰지 않는다.
            if (restored != null && selectedStore.value == null) {
                selectedStore.value = restored
            }
        }
    }

    fun observe(): StateFlow<Store?> = selectedStore

    fun current(): Store? = selectedStore.value

    fun select(store: Store) {
        selectedStore.value = store
        scope.launch {
            preferences.setSelectedStore(storeAdapter.toJson(store))
        }
    }

    private fun decode(json: String): Store? =
        runCatching { storeAdapter.fromJson(json) }.getOrNull()
}
