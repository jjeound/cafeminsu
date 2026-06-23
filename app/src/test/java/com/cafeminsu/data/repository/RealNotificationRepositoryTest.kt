package com.cafeminsu.data.repository

import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.auth.SessionStateHolder
import com.cafeminsu.data.local.notification.NotificationLocalDataSource
import com.cafeminsu.data.remote.NotificationApi
import com.cafeminsu.data.remote.createMoshi
import com.cafeminsu.data.remote.createOkHttpClient
import com.cafeminsu.data.remote.createRetrofit
import com.cafeminsu.domain.model.AppNotification
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.NotificationType
import com.cafeminsu.domain.model.UserProfile
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RealNotificationRepositoryTest {
    private lateinit var server: MockWebServer
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun observeNotificationsFetchesAndMapsSortedNotificationsWithoutUserIdQuery() = runTest(testDispatcher) {
        server.enqueue(notificationListResponse())
        val repository = realNotificationRepository()

        repository.observeNotifications().test {
            val result = awaitItem()

            assertTrue(result is AppResult.Success)
            val notifications = (result as AppResult.Success).data
            assertEquals(listOf("71", "72", "73"), notifications.map { it.id })
            assertEquals(
                listOf(
                    NotificationType.OrderReady,
                    NotificationType.StampEarned,
                    NotificationType.GifticonReceived,
                ),
                notifications.map { it.type },
            )
            assertEquals(false, notifications[0].read)
            assertEquals(true, notifications[1].read)
            cancelAndIgnoreRemainingEvents()
        }

        val request = server.takeRequest()
        assertEquals("/api/notifications", request.requestUrl?.encodedPath)
        assertEquals(null, request.requestUrl?.queryParameter("userId"))
        assertEquals(null, request.requestUrl?.queryParameter("limit"))
    }

    @Test
    fun observeNotificationsEmitsEmptyList() = runTest(testDispatcher) {
        server.enqueue(emptyNotificationListResponse())
        val repository = realNotificationRepository()

        repository.observeNotifications().test {
            assertEquals(AppResult.Success(emptyList<Any>()), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun markAllReadCallsPatchEndpointAndReturnsSuccess() = runTest(testDispatcher) {
        server.enqueue(successVoidResponse())
        val repository = realNotificationRepository()

        val result = repository.markAllRead()

        assertEquals(AppResult.Success(Unit), result)
        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/notifications/read-all", request.requestUrl?.encodedPath)
        assertEquals(null, request.requestUrl?.queryParameter("userId"))
    }

    @Test
    fun notFoundHttpStatusMapsToFailure() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(404))
        val repository = realNotificationRepository()

        repository.observeNotifications().test {
            assertEquals(AppResult.Failure(DomainError.NotFound), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun httpErrorMapsToFailure() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(401))
        val repository = realNotificationRepository()

        val result = repository.markAllRead()

        assertEquals(AppResult.Failure(DomainError.Unauthorized), result)
    }

    @Test
    fun guestSessionBlocksNetworkBeforeCall() = runTest(testDispatcher) {
        val cache = FakeNotificationLocalDataSource(initial = cachedNotifications())
        val repository = realNotificationRepository(authState = AuthState.Guest, local = cache)

        repository.observeNotifications().test {
            assertEquals(AppResult.Failure(DomainError.Unauthorized), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        // 미인증이면 네트워크는 물론 캐시도 읽지 않는다(다른 사용자 데이터 누출 방지).
        assertEquals(0, server.requestCount)
        assertEquals(0, cache.cachedCount)
        assertEquals(0, cache.replaceCount)
    }

    @Test
    fun observeNotificationsWritesThroughToCacheOnSuccess() = runTest(testDispatcher) {
        server.enqueue(notificationListResponse())
        val cache = FakeNotificationLocalDataSource()
        val repository = realNotificationRepository(local = cache)

        repository.observeNotifications().test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(1, cache.replaceCount)
        assertEquals(listOf("71", "72", "73"), cache.stored().map { it.id })
    }

    @Test
    fun observeNotificationsFallsBackToCacheOnNetworkFailure() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(500))
        val cache = FakeNotificationLocalDataSource(initial = cachedNotifications())
        val repository = realNotificationRepository(local = cache)

        repository.observeNotifications().test {
            val result = awaitItem()
            assertTrue(result is AppResult.Success)
            assertEquals(listOf("61", "62"), (result as AppResult.Success).data.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeNotificationsEmitsFailureWhenNetworkFailsAndCacheEmpty() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(500))
        val repository = realNotificationRepository(local = FakeNotificationLocalDataSource())

        repository.observeNotifications().test {
            assertTrue(awaitItem() is AppResult.Failure)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun guestSessionBlocksMarkAllReadBeforeNetworkCall() = runTest(testDispatcher) {
        val repository = realNotificationRepository(authState = AuthState.Guest)

        val result = repository.markAllRead()

        assertEquals(AppResult.Failure(DomainError.Unauthorized), result)
        assertEquals(0, server.requestCount)
    }

    private fun realNotificationRepository(
        authState: AuthState = authenticatedState(),
        local: NotificationLocalDataSource = FakeNotificationLocalDataSource(),
    ): RealNotificationRepository =
        RealNotificationRepository(
            notificationApi = notificationApi(),
            sessionStateHolder = SessionStateHolder(authState),
            localDataSource = local,
            ioDispatcher = testDispatcher,
        )

    private fun cachedNotifications(): List<AppNotification> =
        listOf(
            AppNotification(
                id = "61",
                type = NotificationType.OrderReady,
                title = "캐시 알림 61",
                body = "",
                createdAtMillis = 20L,
                read = false,
            ),
            AppNotification(
                id = "62",
                type = NotificationType.StampEarned,
                title = "캐시 알림 62",
                body = "",
                createdAtMillis = 10L,
                read = true,
            ),
        )

    private fun notificationApi(): NotificationApi =
        createRetrofit(
            baseUrl = server.url("/").toString(),
            moshi = createMoshi(),
            okHttpClient = createOkHttpClient(debug = false),
        ).create(NotificationApi::class.java)

    private fun notificationListResponse(): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                [
                  {
                    "id": 73,
                    "title": "기프티콘이 도착했어요",
                    "body": "선물함에서 확인해주세요",
                    "type": "GIFTICON",
                    "isRead": false,
                    "relatedEntityId": 55,
                    "createdAt": "2026-06-19T01:20:30Z"
                  },
                  {
                    "id": 71,
                    "title": "주문이 준비됐어요",
                    "body": "주문번호 A-2419 — 픽업대에서 수령해주세요",
                    "type": "ORDER",
                    "isRead": false,
                    "relatedEntityId": 2419,
                    "createdAt": "2026-06-20T01:20:30Z"
                  },
                  {
                    "id": 72,
                    "title": "스탬프가 적립됐어요",
                    "body": "스탬프 1개가 적립됐어요",
                    "type": "STAMP",
                    "isRead": true,
                    "relatedEntityId": 88,
                    "createdAt": "2026-06-20T01:15:30Z"
                  },
                  {
                    "id": 74,
                    "title": "새 소식",
                    "body": "지원하지 않는 타입",
                    "type": "SYSTEM",
                    "isRead": false,
                    "relatedEntityId": null,
                    "createdAt": "2026-06-21T01:20:30Z"
                  }
                ]
                """.trimIndent(),
            )

    private fun emptyNotificationListResponse(): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody("[]")

    private fun successVoidResponse(): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody("{}")

    private fun authenticatedState(): AuthState.Authenticated =
        AuthState.Authenticated(
            user = UserProfile(
                id = "42",
                displayName = "민수",
                phoneLast4 = null,
            ),
        )
}

private class FakeNotificationLocalDataSource(
    initial: List<AppNotification> = emptyList(),
) : NotificationLocalDataSource {
    private var store: List<AppNotification> = initial
    var replaceCount: Int = 0
        private set
    var cachedCount: Int = 0
        private set

    override suspend fun cachedNotifications(): List<AppNotification> {
        cachedCount++
        return store
    }

    override suspend fun replaceNotifications(notifications: List<AppNotification>) {
        store = notifications
        replaceCount++
    }

    fun stored(): List<AppNotification> = store
}
