package com.cafeminsu.ui.feature.order

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.StampCard
import com.cafeminsu.domain.repository.OrderRepository
import com.cafeminsu.domain.repository.RewardRepository
import com.cafeminsu.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@HiltViewModel
class OrderResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val orderRepository: OrderRepository,
    private val rewardRepository: RewardRepository,
) : ViewModel() {
    private val orderId = savedStateHandle.get<String>(Routes.ORDER_OK_ORDER_ID).orEmpty()
    private val _uiState = MutableStateFlow<OrderResultUiState>(OrderResultUiState.Loading)
    private var observeJob: Job? = null

    val uiState: StateFlow<OrderResultUiState> = _uiState.asStateFlow()

    init {
        observeResult()
    }

    fun retry() {
        observeResult()
    }

    private fun observeResult() {
        observeJob?.cancel()

        if (orderId.isBlank()) {
            _uiState.value = OrderResultUiState.Failure(
                message = "주문을 찾지 못했어요",
                retryable = false,
            )
            return
        }

        observeJob = viewModelScope.launch {
            orderRepository.observeOrder(orderId)
                .combine(
                    rewardRepository.observeStampCard()
                        .catch { emit(AppResult.Failure(DomainError.Unknown)) },
                ) { orderResult, stampResult ->
                    orderResult.toOrderResultUiState(stampResult.successDataOrNull())
                }
                .catch {
                    emit(
                        OrderResultUiState.Failure(
                            message = "주문 결과를 불러오지 못했어요",
                            retryable = true,
                        ),
                    )
                }
                .collect { state -> _uiState.value = state }
        }
    }

    private fun AppResult<Order>.toOrderResultUiState(stampCard: StampCard?): OrderResultUiState =
        when (this) {
            is AppResult.Success -> OrderResultUiState.Content(
                summary = data.toOrderSuccessSummary(stampCard),
            )

            is AppResult.Failure -> error.toOrderResultError()
        }

    private fun <T> AppResult<T>.successDataOrNull(): T? =
        (this as? AppResult.Success<T>)?.data

    private fun DomainError.toOrderResultError(): OrderResultUiState.Failure =
        OrderResultUiState.Failure(
            message = when (this) {
                DomainError.Network -> "네트워크 연결을 확인하고 다시 시도해 주세요"
                DomainError.Timeout -> "응답이 지연되고 있어요. 잠시 후 다시 시도해 주세요"
                DomainError.Unauthorized -> "로그인이 만료됐어요. 다시 로그인해 주세요"
                DomainError.NotFound -> "주문을 찾지 못했어요"
                is DomainError.Payment -> "결제된 주문을 확인하지 못했어요"
                is DomainError.Validation -> "주문 정보를 확인해 주세요"
                DomainError.Unknown -> "주문 결과를 불러오지 못했어요"
            },
            retryable = when (this) {
                DomainError.Network,
                DomainError.Timeout,
                DomainError.Unknown,
                -> true

                DomainError.Unauthorized,
                DomainError.NotFound,
                is DomainError.Payment,
                is DomainError.Validation,
                -> false
            },
        )
}
