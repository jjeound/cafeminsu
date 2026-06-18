package com.cafeminsu.ui.feature.order

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.repository.OrderRepository
import com.cafeminsu.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@HiltViewModel
class OrderStatusViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val orderRepository: OrderRepository,
) : ViewModel() {
    private val orderId = savedStateHandle.get<String>(Routes.ORDER_STATUS_ORDER_ID).orEmpty()
    private val _uiState = MutableStateFlow<OrderStatusUiState>(OrderStatusUiState.Loading)
    private var observeJob: Job? = null

    val uiState: StateFlow<OrderStatusUiState> = _uiState.asStateFlow()

    init {
        observeOrder()
    }

    fun retry() {
        observeOrder()
    }

    private fun observeOrder() {
        observeJob?.cancel()

        if (orderId.isBlank()) {
            _uiState.value = OrderStatusUiState.Error(
                message = "주문을 찾지 못했어요",
                retryable = false,
            )
            return
        }

        observeJob = viewModelScope.launch {
            orderRepository.observeOrder(orderId)
                .catch { emit(AppResult.Failure(DomainError.Unknown)) }
                .collect { result ->
                    _uiState.value = when (result) {
                        is AppResult.Success -> result.data.toOrderStatusContent()
                        is AppResult.Failure -> result.error.toOrderStatusError()
                    }
                }
        }
    }

    private fun DomainError.toOrderStatusError(): OrderStatusUiState.Error =
        OrderStatusUiState.Error(
            message = toOrderStatusMessage(),
            retryable = true,
        )

    private fun DomainError.toOrderStatusMessage(): String =
        when (this) {
            DomainError.Network -> "네트워크 연결을 확인하고 다시 시도해 주세요"
            DomainError.Timeout -> "응답이 지연되고 있어요. 잠시 후 다시 시도해 주세요"
            DomainError.Unauthorized -> "로그인이 만료됐어요. 다시 로그인해 주세요"
            DomainError.NotFound -> "주문을 찾지 못했어요"
            is DomainError.Payment -> "결제 상태를 확인하지 못했어요"
            is DomainError.Validation -> "주문 정보를 확인해 주세요"
            DomainError.Unknown -> "주문 상태를 불러오지 못했어요"
        }
}
