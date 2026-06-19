package com.cafeminsu.ui.feature.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.AppNotification
import com.cafeminsu.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class NotiViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {
    private var currentTimeMillis: () -> Long = { System.currentTimeMillis() }
    private var zoneId: ZoneId = ZoneId.systemDefault()

    internal constructor(
        notificationRepository: NotificationRepository,
        currentTimeMillis: () -> Long,
        zoneId: ZoneId,
    ) : this(notificationRepository) {
        this.currentTimeMillis = currentTimeMillis
        this.zoneId = zoneId
    }

    val uiState: StateFlow<NotiUiState> = notificationRepository
        .observeNotifications()
        .map { result ->
            when (result) {
                is AppResult.Success -> result.data.toUiState()
                is AppResult.Failure -> result.error.toNotiError()
            }
        }
        .catch {
            emit(
                NotiUiState.Error(
                    message = "알림을 불러오지 못했어요",
                    retryable = true,
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(StateStopTimeoutMillis),
            initialValue = NotiUiState.Loading,
        )

    fun markAllRead() {
        viewModelScope.launch {
            notificationRepository.markAllRead()
        }
    }

    fun retry() {
        markAllRead()
    }

    private fun List<AppNotification>.toUiState(): NotiUiState {
        if (isEmpty()) {
            return NotiUiState.Empty(message = "받은 알림이 없어요")
        }

        val sorted = sortedByDescending { it.createdAtMillis }
        val grouped = sorted
            .groupBy { it.groupLabel() }
            .map { (label, notifications) ->
                NotiGroupUiModel(
                    label = label,
                    items = notifications.map { it.toUiModel() },
                )
            }

        return NotiUiState.Content(groups = grouped)
    }

    private fun AppNotification.toUiModel(): NotiItemUiModel =
        NotiItemUiModel(
            id = id,
            type = type,
            title = title,
            body = body,
            timeLabel = timeLabel(createdAtMillis),
            unread = !read,
        )

    private fun AppNotification.groupLabel(): String {
        val createdDate = createdAtMillis.toLocalDate()
        val today = currentTimeMillis().toLocalDate()

        return when (createdDate) {
            today -> "오늘"
            today.minusDays(1) -> "어제"
            else -> createdDate.format(groupDateFormatter)
        }
    }

    private fun timeLabel(createdAtMillis: Long): String {
        val created = Instant.ofEpochMilli(createdAtMillis).atZone(zoneId)
        val createdDate = created.toLocalDate()
        val today = currentTimeMillis().toLocalDate()

        return when (createdDate) {
            today -> todayTimeLabel(createdAtMillis)
            today.minusDays(1) -> "어제 ${created.format(timeFormatter)}"
            else -> created.format(fullDateFormatter)
        }
    }

    private fun todayTimeLabel(createdAtMillis: Long): String {
        val elapsedMillis = (currentTimeMillis() - createdAtMillis).coerceAtLeast(0L)
        val elapsedMinutes = elapsedMillis / MinuteMillis

        return when {
            elapsedMinutes <= RecentMinuteThreshold -> "방금"
            elapsedMinutes < HourMinutes -> "${elapsedMinutes}분 전"
            else -> "${elapsedMinutes / HourMinutes}시간 전"
        }
    }

    private fun Long.toLocalDate(): LocalDate =
        Instant.ofEpochMilli(this)
            .atZone(zoneId)
            .toLocalDate()

    private fun DomainError.toNotiError(): NotiUiState.Error =
        NotiUiState.Error(
            message = toNotiMessage(),
            retryable = isRetryable(),
        )

    private fun DomainError.toNotiMessage(): String =
        when (this) {
            DomainError.Network -> "네트워크 연결을 확인하고 다시 시도해 주세요"
            DomainError.Timeout -> "응답이 지연되고 있어요. 잠시 후 다시 시도해 주세요"
            DomainError.Unauthorized -> "로그인이 만료됐어요. 다시 로그인해 주세요"
            DomainError.NotFound -> "알림을 찾지 못했어요"
            is DomainError.Payment -> "결제 알림을 확인하지 못했어요"
            is DomainError.Validation -> "알림 요청을 확인해 주세요"
            DomainError.Unknown -> "알림을 불러오지 못했어요"
        }

    private fun DomainError.isRetryable(): Boolean =
        when (this) {
            DomainError.Network,
            DomainError.Timeout,
            DomainError.Unknown,
            -> true

            DomainError.Unauthorized,
            DomainError.NotFound,
            is DomainError.Payment,
            is DomainError.Validation,
            -> false
        }

    private companion object {
        const val StateStopTimeoutMillis = 5_000L
        const val MinuteMillis = 60L * 1000L
        const val HourMinutes = 60L
        const val RecentMinuteThreshold = 1L

        val timeFormatter: DateTimeFormatter =
            DateTimeFormatter.ofPattern("HH:mm", Locale.KOREA)
        val fullDateFormatter: DateTimeFormatter =
            DateTimeFormatter.ofPattern("M월 d일 HH:mm", Locale.KOREA)
        val groupDateFormatter: DateTimeFormatter =
            DateTimeFormatter.ofPattern("M월 d일", Locale.KOREA)
    }
}
