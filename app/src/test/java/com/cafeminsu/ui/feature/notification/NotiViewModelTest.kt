package com.cafeminsu.ui.feature.notification

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.AppNotification
import com.cafeminsu.domain.model.NotificationType
import com.cafeminsu.domain.repository.NotificationRepository
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class NotiViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun notificationResultsProduceContentGroupedByTodayAndYesterday() = runTest {
        val repository = FakeNotificationRepository(
            AppResult.Success(
                listOf(
                    sampleNotification(
                        id = "ready",
                        type = NotificationType.OrderReady,
                        title = "주문이 준비됐어요",
                        createdAtMillis = todayAt(hour = 9, minute = 59),
                        read = false,
                    ),
                    sampleNotification(
                        id = "accepted",
                        type = NotificationType.OrderAccepted,
                        title = "주문이 수락됐어요",
                        createdAtMillis = todayAt(hour = 9, minute = 55),
                        read = false,
                    ),
                    sampleNotification(
                        id = "gift",
                        type = NotificationType.GifticonReceived,
                        title = "기프티콘이 도착했어요",
                        createdAtMillis = yesterdayAt(hour = 19, minute = 42),
                        read = true,
                    ),
                ),
            ),
        )
        val viewModel = viewModel(repository)

        viewModel.uiState.test {
            val content = awaitContent()

            assertEquals(listOf("오늘", "어제"), content.groups.map { it.label })
            assertEquals(listOf("ready", "accepted"), content.groups[0].items.map { it.id })
            assertEquals("방금", content.groups[0].items[0].timeLabel)
            assertEquals("5분 전", content.groups[0].items[1].timeLabel)
            assertTrue(content.groups[0].items[0].unread)
            assertEquals("어제 19:42", content.groups[1].items.single().timeLabel)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emptyNotificationResultProducesEmptyState() = runTest {
        val viewModel = viewModel(
            FakeNotificationRepository(AppResult.Success(emptyList())),
        )

        viewModel.uiState.test {
            val empty = awaitEmpty()

            assertEquals("받은 알림이 없어요", empty.message)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun notificationFailureProducesRetryableErrorState() = runTest {
        val viewModel = viewModel(
            FakeNotificationRepository(AppResult.Failure(DomainError.Network)),
        )

        viewModel.uiState.test {
            val error = awaitErrorState()

            assertEquals("네트워크 연결을 확인하고 다시 시도해 주세요", error.message)
            assertTrue(error.retryable)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun markAllReadClearsUnreadIndicators() = runTest {
        val repository = FakeNotificationRepository(
            AppResult.Success(
                listOf(
                    sampleNotification(
                        id = "ready",
                        type = NotificationType.OrderReady,
                        title = "주문이 준비됐어요",
                        createdAtMillis = todayAt(hour = 9, minute = 59),
                        read = false,
                    ),
                    sampleNotification(
                        id = "stamp",
                        type = NotificationType.StampEarned,
                        title = "스탬프 적립",
                        createdAtMillis = todayAt(hour = 9, minute = 55),
                        read = false,
                    ),
                ),
            ),
        )
        val viewModel = viewModel(repository)

        viewModel.uiState.test {
            assertTrue(awaitContent().groups.flatMap { it.items }.any { it.unread })

            viewModel.markAllRead()

            val updated = awaitContent()
            assertEquals(1, repository.markAllReadCallCount)
            assertFalse(updated.groups.flatMap { it.items }.any { it.unread })

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun viewModel(repository: FakeNotificationRepository): NotiViewModel =
        NotiViewModel(
            notificationRepository = repository,
            currentTimeMillis = { NowMillis },
            zoneId = SeoulZone,
        )

    private fun sampleNotification(
        id: String,
        type: NotificationType,
        title: String,
        createdAtMillis: Long,
        read: Boolean,
    ): AppNotification =
        AppNotification(
            id = id,
            type = type,
            title = title,
            body = "주문번호 A-2419",
            createdAtMillis = createdAtMillis,
            read = read,
        )

    private fun todayAt(hour: Int, minute: Int): Long =
        LocalDateTime.of(2026, 6, 19, hour, minute)
            .atZone(SeoulZone)
            .toInstant()
            .toEpochMilli()

    private fun yesterdayAt(hour: Int, minute: Int): Long =
        LocalDateTime.of(2026, 6, 18, hour, minute)
            .atZone(SeoulZone)
            .toInstant()
            .toEpochMilli()

    private suspend fun ReceiveTurbine<NotiUiState>.awaitSettledState(): NotiUiState {
        val state = awaitItem()
        return if (state == NotiUiState.Loading) {
            awaitItem()
        } else {
            state
        }
    }

    private suspend fun ReceiveTurbine<NotiUiState>.awaitContent(): NotiUiState.Content {
        val state = awaitSettledState()
        assertTrue(state is NotiUiState.Content)
        return state as NotiUiState.Content
    }

    private suspend fun ReceiveTurbine<NotiUiState>.awaitEmpty(): NotiUiState.Empty {
        val state = awaitSettledState()
        assertTrue(state is NotiUiState.Empty)
        return state as NotiUiState.Empty
    }

    private suspend fun ReceiveTurbine<NotiUiState>.awaitErrorState(): NotiUiState.Error {
        val state = awaitSettledState()
        assertTrue(state is NotiUiState.Error)
        return state as NotiUiState.Error
    }

    private companion object {
        val SeoulZone: ZoneId = ZoneId.of("Asia/Seoul")
        val NowMillis: Long = LocalDateTime.of(2026, 6, 19, 10, 0)
            .atZone(SeoulZone)
            .toInstant()
            .toEpochMilli()
    }
}

private class FakeNotificationRepository(
    initialResult: AppResult<List<AppNotification>>,
) : NotificationRepository {
    private val notifications = MutableStateFlow(initialResult)
    var markAllReadCallCount: Int = 0
        private set

    override fun observeNotifications(): Flow<AppResult<List<AppNotification>>> = notifications

    override suspend fun markAllRead(): AppResult<Unit> {
        markAllReadCallCount += 1
        val current = notifications.value
        if (current is AppResult.Success) {
            notifications.value = AppResult.Success(current.data.map { it.copy(read = true) })
        }
        return AppResult.Success(Unit)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
