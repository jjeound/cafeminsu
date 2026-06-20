package com.cafeminsu.ui.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed interface HistoryEvent {
    data class NavigateMenuDetail(val menuItemId: String) : HistoryEvent
}

@HiltViewModel
class HistoryViewModel(
    private val orderRepository: OrderRepository,
    private val nowMillis: () -> Long,
) : ViewModel() {
    @Inject
    constructor(
        orderRepository: OrderRepository,
    ) : this(
        orderRepository = orderRepository,
        nowMillis = { System.currentTimeMillis() },
    )

    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    private val _events = MutableSharedFlow<HistoryEvent>(extraBufferCapacity = EventBufferCapacity)
    private var observeJob: Job? = null

    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    val events: SharedFlow<HistoryEvent> = _events.asSharedFlow()

    init {
        observeHistory()
    }

    fun retry() {
        observeHistory()
    }

    fun onReorder(orderId: String) {
        val content = _uiState.value as? HistoryUiState.Content ?: return
        val menuItemId = content.pastOrders
            .firstOrNull { order -> order.id == orderId }
            ?.reorderMenuItemId
            ?: return

        _events.tryEmit(HistoryEvent.NavigateMenuDetail(menuItemId))
    }

    private fun observeHistory() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            orderRepository.observeOrderHistory()
                .catch { emit(AppResult.Failure(DomainError.Unknown)) }
                .collect { result ->
                    _uiState.value = when (result) {
                        is AppResult.Success -> result.data.toHistoryUiState(nowMillis())
                        is AppResult.Failure -> result.error.toHistoryError()
                    }
                }
        }
    }

    private fun DomainError.toHistoryError(): HistoryUiState.Error =
        HistoryUiState.Error(
            message = toHistoryMessage(),
            retryable = isRetryable(),
        )

    private fun DomainError.toHistoryMessage(): String =
        when (this) {
            DomainError.Network -> "네트워크 연결을 확인하고 다시 시도해 주세요"
            DomainError.Timeout -> "응답이 지연되고 있어요. 잠시 후 다시 시도해 주세요"
            DomainError.Unauthorized -> "로그인이 만료됐어요. 다시 로그인해 주세요"
            DomainError.NotFound -> "주문 내역을 찾지 못했어요"
            is DomainError.Payment -> "결제된 주문을 확인하지 못했어요"
            is DomainError.Validation -> "주문 정보를 확인해 주세요"
            DomainError.Unknown -> "주문 내역을 불러오지 못했어요"
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
        const val EventBufferCapacity = 1
    }
}
