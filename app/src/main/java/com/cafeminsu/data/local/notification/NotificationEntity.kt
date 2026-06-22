package com.cafeminsu.data.local.notification

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 알림 목록 오프라인 캐시 행.
 *
 * 캐시 전용이라 도메인 [com.cafeminsu.domain.model.AppNotification] 의 단순 투영만 보관한다.
 * enum([com.cafeminsu.domain.model.NotificationType]) 은 [NotificationCacheMapper] 에서 name 문자열로 직렬화한다.
 * 알림 본문/제목은 PII 가 아닌 사용자 대상 카피이며 토큰·결제정보는 담지 않는다(SECURITY.md).
 */
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val body: String,
    val createdAtMillis: Long,
    val read: Boolean,
)
