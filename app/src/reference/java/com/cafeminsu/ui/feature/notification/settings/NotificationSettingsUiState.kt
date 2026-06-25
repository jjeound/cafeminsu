package com.cafeminsu.ui.feature.notification.settings

/** 알림 설정 화면에서 켜고 끌 수 있는 푸시 알림 카테고리. */
enum class NotificationCategory {
    OrderStatus,
    Promotion,
    Marketing,
}

/**
 * 알림 설정 화면 상태. 카테고리별 on/off 플래그만 담는다(로컬 설정 표시·영속 전용).
 *
 * 기본값은 [com.cafeminsu.data.local.prefs.UserPreferencesDataStore] 의 기본값과 일치시킨다.
 */
data class NotificationSettingsUiState(
    val orderStatusEnabled: Boolean = true,
    val promotionEnabled: Boolean = true,
    val marketingEnabled: Boolean = false,
) {
    fun isEnabled(category: NotificationCategory): Boolean =
        when (category) {
            NotificationCategory.OrderStatus -> orderStatusEnabled
            NotificationCategory.Promotion -> promotionEnabled
            NotificationCategory.Marketing -> marketingEnabled
        }
}
