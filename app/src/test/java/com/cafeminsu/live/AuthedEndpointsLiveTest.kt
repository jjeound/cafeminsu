package com.cafeminsu.live

import com.cafeminsu.data.remote.AuthApi
import com.cafeminsu.data.remote.NotificationApi
import com.cafeminsu.data.remote.OrderApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * 인증(Bearer) 엔드포인트 라이브 스모크. 게이트 + 토큰 미설정 시 [assumeLiveAuth] 로 전부 skip.
 * 토큰은 환경값에서만 읽어 Authorization 헤더로 부착하며, 코드/로그에 토큰·PII 를 남기지 않는다.
 * 읽기(GET) 전용 — 서버 상태를 바꾸지 않는다. 단언은 "2xx + 역직렬화 성공" 수준.
 */
class AuthedEndpointsLiveTest {
    @Test
    fun getMyProfileParsesUserProfileRes() = runBlocking {
        assumeLiveAuth()
        val profile = authApi().getMyProfile()
        assertNotNull("UserProfileRes.id 가 null", profile.id)
    }

    @Test
    fun getMyOrdersParsesOrderListItemRes() = runBlocking {
        assumeLiveAuth()
        val orders = orderApi().getMyOrders()
        assertNotNull("주문 목록 역직렬화 실패", orders)
        orders.forEach { assertNotNull("order id 가 null", it.orderId) }
    }

    @Test
    fun getNotificationsParsesNotificationListItemRes() = runBlocking {
        assumeLiveAuth()
        val notifications = notificationApi().getNotifications()
        assertNotNull("알림 목록 역직렬화 실패", notifications)
        notifications.forEach { assertNotNull("notification id 가 null", it.id) }
    }

    private fun authApi(): AuthApi =
        liveRetrofit(LiveServer.liveToken()).create(AuthApi::class.java)

    private fun orderApi(): OrderApi =
        liveRetrofit(LiveServer.liveToken()).create(OrderApi::class.java)

    private fun notificationApi(): NotificationApi =
        liveRetrofit(LiveServer.liveToken()).create(NotificationApi::class.java)
}
