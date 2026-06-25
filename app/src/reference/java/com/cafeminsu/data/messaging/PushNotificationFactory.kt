package com.cafeminsu.data.messaging

/**
 * 표시할 로컬 알림 한 건의 내용. Android 프레임워크에 비종속(순수)이라 단위 테스트 가능.
 */
data class PushNotificationContent(
    val channelId: String,
    val channelName: String,
    val notificationId: Int,
    val title: String,
    val body: String,
)

/**
 * FCM 메시지(notification 블록 + data 페이로드)를 표시용 [PushNotificationContent]로 변환한다.
 * 제목/본문이 모두 비어 있으면 null 을 반환해(표시할 것 없음) 빈 알림을 막는다.
 */
object PushNotificationFactory {
    const val CHANNEL_ID = "cafeminsu_orders"
    const val CHANNEL_NAME = "주문·적립 알림"

    const val DATA_KEY_TITLE = "title"
    const val DATA_KEY_BODY = "body"
    const val DATA_KEY_TYPE = "type"
    const val DATA_KEY_RELATED_ID = "relatedEntityId"

    const val DEFAULT_NOTIFICATION_ID = 1001

    private const val DEFAULT_TITLE = "민수"

    fun from(
        notificationTitle: String?,
        notificationBody: String?,
        data: Map<String, String>,
    ): PushNotificationContent? {
        val title = notificationTitle?.takeIf { it.isNotBlank() }
            ?: data[DATA_KEY_TITLE]?.takeIf { it.isNotBlank() }
        val body = notificationBody?.takeIf { it.isNotBlank() }
            ?: data[DATA_KEY_BODY]?.takeIf { it.isNotBlank() }

        if (title == null && body == null) {
            return null
        }

        return PushNotificationContent(
            channelId = CHANNEL_ID,
            channelName = CHANNEL_NAME,
            notificationId = notificationId(data),
            title = title ?: DEFAULT_TITLE,
            body = body.orEmpty(),
        )
    }

    private fun notificationId(data: Map<String, String>): Int {
        val type = data[DATA_KEY_TYPE].orEmpty()
        val relatedId = data[DATA_KEY_RELATED_ID].orEmpty()
        if (type.isEmpty() && relatedId.isEmpty()) {
            return DEFAULT_NOTIFICATION_ID
        }
        return "$type:$relatedId".hashCode()
    }
}
