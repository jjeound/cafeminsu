package com.cafeminsu.data.local.notification

import com.cafeminsu.domain.model.AppNotification
import javax.inject.Inject

/**
 * 알림 목록 로컬 캐시 접근. 리포지토리는 Room DAO 가 아니라 이 인터페이스에 의존해
 * 단위 테스트에서 가짜로 대체할 수 있게 한다.
 */
interface NotificationLocalDataSource {
    suspend fun cachedNotifications(): List<AppNotification>

    suspend fun replaceNotifications(notifications: List<AppNotification>)
}

class RoomNotificationLocalDataSource @Inject constructor(
    private val notificationDao: NotificationDao,
) : NotificationLocalDataSource {
    override suspend fun cachedNotifications(): List<AppNotification> =
        notificationDao.getAll().toAppNotifications()

    override suspend fun replaceNotifications(notifications: List<AppNotification>) {
        // 목록 전체 교체: 서버에서 사라진 알림이 캐시에 남지 않도록 비운 뒤 다시 채운다.
        notificationDao.clear()
        notificationDao.upsertAll(notifications.toNotificationEntities())
    }
}
