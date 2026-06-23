package com.cafeminsu.ui.feature.notification.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeminsu.data.local.prefs.UserPreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 마이 > 알림설정 화면 ViewModel.
 *
 * 카테고리별 on/off 플래그를 [UserPreferencesDataStore] 에서 관찰해 [NotificationSettingsUiState] 로 노출하고,
 * 토글 시 같은 DataStore 에 영속한다. 로컬 설정 표시·영속까지가 범위이며(금전 액션 아님) 토글은 낙관적 UI 를
 * 허용한다 — 실제 서버 구독/푸시 게이팅 연동은 범위 밖.
 */
@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val preferences: UserPreferencesDataStore,
) : ViewModel() {
    val uiState: StateFlow<NotificationSettingsUiState> = combine(
        preferences.observeOrderStatusNotification(),
        preferences.observePromotionNotification(),
        preferences.observeMarketingNotification(),
    ) { orderStatus, promotion, marketing ->
        NotificationSettingsUiState(
            orderStatusEnabled = orderStatus,
            promotionEnabled = promotion,
            marketingEnabled = marketing,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(StateStopTimeoutMillis),
        initialValue = NotificationSettingsUiState(),
    )

    fun onToggle(category: NotificationCategory, enabled: Boolean) {
        viewModelScope.launch {
            when (category) {
                NotificationCategory.OrderStatus -> preferences.setOrderStatusNotification(enabled)
                NotificationCategory.Promotion -> preferences.setPromotionNotification(enabled)
                NotificationCategory.Marketing -> preferences.setMarketingNotification(enabled)
            }
        }
    }

    private companion object {
        const val StateStopTimeoutMillis = 5_000L
    }
}
