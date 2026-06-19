package com.cafeminsu.ui.feature.my

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.domain.model.UserProfile
import com.cafeminsu.domain.repository.OrderRepository
import com.cafeminsu.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MyViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val orderRepository: OrderRepository,
) : ViewModel() {
    private val logoutError = MutableStateFlow<DomainError?>(null)

    val uiState: StateFlow<MyUiState> = combine(
        sessionRepository.observeAuthState(),
        orderRepository.observeOrderHistory(),
        logoutError,
    ) { authState, orderHistoryResult, currentLogoutError ->
        if (currentLogoutError != null) {
            currentLogoutError.toMyError()
        } else {
            mapMyState(
                authState = authState,
                orderHistoryResult = orderHistoryResult,
            )
        }
    }.catch {
        emit(
            MyUiState.Error(
                message = "마이페이지를 불러오지 못했어요",
                retryable = true,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(StateStopTimeoutMillis),
        initialValue = MyUiState.Loading,
    )

    fun retry() {
        viewModelScope.launch {
            logoutError.value = null
            runCatching {
                sessionRepository.refreshOnce()
            }
        }
    }

    fun onLogout() {
        viewModelScope.launch {
            val result = runCatching {
                sessionRepository.clearSession()
            }.getOrElse {
                AppResult.Failure(DomainError.Unknown)
            }

            if (result is AppResult.Failure) {
                logoutError.value = result.error
            }
        }
    }

    private fun mapMyState(
        authState: AuthState,
        orderHistoryResult: AppResult<List<Order>>,
    ): MyUiState =
        when (authState) {
            AuthState.Unknown -> MyUiState.Loading
            AuthState.Guest,
            AuthState.Expired,
            -> MyUiState.NeedsLogin(
                message = "로그인이 필요해요",
                actionLabel = "다시 로그인하기",
            )

            is AuthState.Authenticated -> when (orderHistoryResult) {
                is AppResult.Success -> authState.user.toMyState(orderHistoryResult.data)
                is AppResult.Failure -> orderHistoryResult.error.toMyError()
            }
        }

    private fun UserProfile.toMyState(orders: List<Order>): MyUiState {
        val profile = MyProfileUiModel(
            displayName = displayName,
            phoneLast4 = phoneLast4,
        )
        val settings = defaultSettings()

        return if (orders.isEmpty()) {
            MyUiState.Empty(
                profile = profile,
                message = "주문 내역이 없어요",
                actionLabel = "메뉴 보러가기",
                settings = settings,
                appMeta = AppMeta,
            )
        } else {
            MyUiState.Content(
                profile = profile,
                recentOrders = orders
                    .sortedByDescending { it.createdAtMillis }
                    .map { it.toMyOrderSummary() },
                settings = settings,
                appMeta = AppMeta,
            )
        }
    }

    private fun Order.toMyOrderSummary(): MyOrderSummaryUiModel =
        MyOrderSummaryUiModel(
            orderId = id,
            orderNumber = orderNumber,
            createdAtMillis = createdAtMillis,
            totalAmount = totalAmount,
            statusLabel = status.toMyStatusLabel(),
        )

    private fun OrderStatus.toMyStatusLabel(): String =
        when (this) {
            OrderStatus.PendingPayment -> "결제 확인 중"
            OrderStatus.Paid -> "결제 완료"
            OrderStatus.Accepted -> "주문 접수"
            OrderStatus.Preparing -> "준비 중"
            OrderStatus.Ready -> "픽업 준비 완료"
            OrderStatus.Completed -> "픽업 완료"
            OrderStatus.Cancelled -> "주문 취소"
            OrderStatus.Failed -> "주문 실패"
        }

    private fun defaultSettings(): List<MySettingItemUiModel> =
        listOf(
            MySettingItemUiModel(
                id = LogoutSettingId,
                label = "로그아웃",
            ),
        )

    private fun DomainError.toMyError(): MyUiState.Error =
        MyUiState.Error(
            message = toMyMessage(),
            retryable = isRetryable(),
        )

    private fun DomainError.toMyMessage(): String =
        when (this) {
            DomainError.Network -> "네트워크 연결을 확인하고 다시 시도해 주세요"
            DomainError.Timeout -> "응답이 지연되고 있어요. 잠시 후 다시 시도해 주세요"
            DomainError.Unauthorized -> "로그인이 만료됐어요. 다시 로그인해 주세요"
            DomainError.NotFound -> "주문 내역을 찾지 못했어요"
            is DomainError.Payment -> "주문 내역을 확인하지 못했어요"
            is DomainError.Validation -> "주문 내역을 확인해 주세요"
            DomainError.Unknown -> "마이페이지를 불러오지 못했어요"
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
        const val LogoutSettingId = "logout"
        const val AppMeta = "앱 버전 1.0"
        const val StateStopTimeoutMillis = 5_000L
    }
}
