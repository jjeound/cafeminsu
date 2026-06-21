package com.cafeminsu.data.repository

import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.auth.SessionStateHolder
import com.cafeminsu.data.remote.NotificationApi
import com.cafeminsu.data.remote.createMoshi
import com.cafeminsu.data.remote.createOkHttpClient
import com.cafeminsu.data.remote.createRetrofit
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
    fun baseResponseErrorMapsToFailure() = runTest(testDispatcher) {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "isSuccess": false,
                      "code": 404,
                      "message": "알림 없음",
                      "result": null
                    }
                    """.trimIndent(),
                ),
        )
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
        val repository = realNotificationRepository(authState = AuthState.Guest)

        repository.observeNotifications().test {
            assertEquals(AppResult.Failure(DomainError.Unauthorized), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(0, server.requestCount)
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
    ): RealNotificationRepository =
        RealNotificationRepository(
            notificationApi = notificationApi(),
            sessionStateHolder = SessionStateHolder(authState),
            ioDispatcher = testDispatcher,
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
                {
                  "isSuccess": true,
                  "code": 200,
                  "message": "OK",
                  "result": [
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
                }
                """.trimIndent(),
            )

    private fun emptyNotificationListResponse(): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                {
                  "isSuccess": true,
                  "code": 200,
                  "message": "OK",
                  "result": []
                }
                """.trimIndent(),
            )

    private fun successVoidResponse(): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                {
                  "isSuccess": true,
                  "code": 200,
                  "message": "OK",
                  "result": null
                }
                """.trimIndent(),
            )

    private fun authenticatedState(): AuthState.Authenticated =
        AuthState.Authenticated(
            user = UserProfile(
                id = "42",
                displayName = "민수",
                phoneLast4 = null,
            ),
        )
}
