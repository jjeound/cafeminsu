package com.cafeminsu.ui.feature.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.PaymentRequest
import com.cafeminsu.domain.model.PaymentResult
import com.cafeminsu.domain.model.PaymentStatus
import com.cafeminsu.domain.repository.OrderRepository
import com.cafeminsu.domain.repository.PaymentRepository
import com.cafeminsu.domain.repository.RewardRepository
import com.cafeminsu.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@HiltViewModel
class PaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository,
    private val rewardRepository: RewardRepository,
) : ViewModel() {
    private val orderId = savedStateHandle.get<String>(Routes.PAYMENT_ORDER_ID).orEmpty()
    private val _uiState = MutableStateFlow<PaymentUiState>(PaymentUiState.Loading)
    private val _events = MutableSharedFlow<PaymentEvent>(extraBufferCapacity = EventBufferCapacity)
    private var observeJob: Job? = null
    private var paymentInProgress = false
    private var idempotencyKey: String? = null

    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()
    val events: SharedFlow<PaymentEvent> = _events.asSharedFlow()

    init {
        observeOrder()
    }

    fun onSelectMethod(methodId: String) {
        if (paymentInProgress) {
            return
        }

        val content = _uiState.value as? PaymentUiState.Content ?: return
        if (content.methods.none { method -> method.id == methodId }) {
            return
        }

        _uiState.value = content.copy(selectedMethodId = methodId)
    }

    fun onPay() {
        onPaySuccess()
    }

    fun onPaySuccess() {
        payWithMockOutcome(MockPaymentOutcome.Success)
    }

    fun onPayFailure() {
        payWithMockOutcome(MockPaymentOutcome.Failure)
    }

    private fun payWithMockOutcome(outcome: MockPaymentOutcome) {
        if (paymentInProgress) {
            return
        }

        val content = _uiState.value as? PaymentUiState.Content ?: return
        val method = paymentMethods.firstOrNull { method -> method.id == content.selectedMethodId }
            ?: return

        paymentInProgress = true
        _uiState.value = content.copy(paymentState = PaymentProgress.Processing)

        viewModelScope.launch {
            val key = idempotencyKey ?: UUID.randomUUID().toString().also { generatedKey ->
                idempotencyKey = generatedKey
            }
            val request = PaymentRequest(
                orderId = content.orderId,
                amount = content.totalAmount,
                paymentMethodToken = when (outcome) {
                    MockPaymentOutcome.Success -> method.token
                    MockPaymentOutcome.Failure -> MockFailureToken
                },
                idempotencyKey = key,
            )

            when (val result = paySafely(request)) {
                is AppResult.Success -> handlePaymentResult(
                    result = result.data,
                    request = request,
                    allowStatusConfirmation = true,
                )

                is AppResult.Failure -> handlePaymentFailure(
                    error = result.error,
                    request = request,
                )
            }
        }
    }

    fun retry() {
        observeOrder()
    }

    private fun observeOrder() {
        observeJob?.cancel()

        if (orderId.isBlank()) {
            _uiState.value = PaymentUiState.Error(
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
                        is AppResult.Success -> result.data.toPaymentContent()
                        is AppResult.Failure -> result.error.toPaymentError()
                    }
                }
        }
    }

    private suspend fun paySafely(request: PaymentRequest): AppResult<PaymentResult> =
        try {
            paymentRepository.pay(request)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            AppResult.Failure(DomainError.Unknown)
        }

    private suspend fun handlePaymentFailure(
        error: DomainError,
        request: PaymentRequest,
    ) {
        if (error.needsStatusConfirmation()) {
            confirmPaymentStatus(request)
        } else {
            finishPayment(PaymentProgress.Failed(error.toPaymentFailureMessage()))
        }
    }

    private suspend fun handlePaymentResult(
        result: PaymentResult,
        request: PaymentRequest,
        allowStatusConfirmation: Boolean,
    ) {
        if (result.orderId != request.orderId) {
            finishPayment(
                PaymentProgress.NeedsConfirmation("결제 응답의 주문 정보를 확인하지 못했어요. 다시 시도해 주세요"),
            )
            return
        }

        when (result.status) {
            PaymentStatus.Approved -> approvePayment(request.orderId)
            PaymentStatus.Failed -> failPayment(
                orderId = request.orderId,
                progress = PaymentProgress.Failed("결제가 승인되지 않았어요. 다른 결제수단으로 다시 시도해 주세요"),
            )

            PaymentStatus.Cancelled -> failPayment(
                orderId = request.orderId,
                progress = PaymentProgress.Failed("결제가 취소됐어요. 다시 시도해 주세요"),
            )

            PaymentStatus.Pending,
            PaymentStatus.Unknown,
            -> if (allowStatusConfirmation) {
                confirmPaymentStatus(request)
            } else {
                finishPayment(
                    PaymentProgress.NeedsConfirmation("결제 상태를 확정하지 못했어요. 다시 시도해 주세요"),
                )
            }
        }
    }

    private suspend fun confirmPaymentStatus(request: PaymentRequest) {
        val result = try {
            paymentRepository.getPaymentStatus(
                orderId = request.orderId,
                idempotencyKey = request.idempotencyKey,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            AppResult.Failure(DomainError.Unknown)
        }

        when (result) {
            is AppResult.Success -> handlePaymentResult(
                result = result.data,
                request = request,
                allowStatusConfirmation = false,
            )

            is AppResult.Failure -> finishPayment(
                PaymentProgress.NeedsConfirmation(result.error.toPaymentConfirmationMessage()),
            )
        }
    }

    private suspend fun approvePayment(approvedOrderId: String) {
        finishPayment(PaymentProgress.Approved)
        grantStampForApprovedOrder(approvedOrderId)
        _events.emit(PaymentEvent.PaymentApproved(approvedOrderId))
    }

    private suspend fun failPayment(
        orderId: String,
        progress: PaymentProgress.Failed,
    ) {
        finishPayment(progress)
        _events.emit(PaymentEvent.PaymentFailed(orderId))
    }

    private suspend fun grantStampForApprovedOrder(orderId: String) {
        try {
            rewardRepository.grantStampsForPaidOrder(orderId)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            // Non-fatal: payment success remains authoritative; stamp reconciliation can retry later.
        }
    }

    private fun finishPayment(progress: PaymentProgress) {
        paymentInProgress = false
        val content = _uiState.value as? PaymentUiState.Content ?: return
        _uiState.value = content.copy(paymentState = progress)
    }

    private fun Order.toPaymentContent(): PaymentUiState.Content {
        val current = _uiState.value as? PaymentUiState.Content
        val methods = defaultPaymentMethods()
        val selectedMethodId = current?.selectedMethodId
            ?.takeIf { selectedId -> methods.any { method -> method.id == selectedId } }
            ?: methods.first().id

        return PaymentUiState.Content(
            orderId = id,
            orderNumber = orderNumber,
            items = items,
            totalAmount = totalAmount,
            methods = methods,
            selectedMethodId = selectedMethodId,
            paymentState = current?.paymentState ?: PaymentProgress.Idle,
        )
    }

    private fun DomainError.toPaymentError(): PaymentUiState.Error =
        PaymentUiState.Error(
            message = toPaymentLoadMessage(),
            retryable = isRetryable(),
        )

    private fun DomainError.toPaymentLoadMessage(): String =
        when (this) {
            DomainError.Network -> "네트워크 연결을 확인하고 다시 시도해 주세요"
            DomainError.Timeout -> "응답이 지연되고 있어요. 잠시 후 다시 시도해 주세요"
            DomainError.Unauthorized -> "로그인이 만료됐어요. 다시 로그인해 주세요"
            DomainError.NotFound -> "주문을 찾지 못했어요"
            is DomainError.Payment -> "결제 정보를 확인하지 못했어요"
            is DomainError.Validation -> "주문 금액을 확인해 주세요"
            DomainError.Unknown -> "결제 화면을 불러오지 못했어요"
        }

    private fun DomainError.toPaymentFailureMessage(): String =
        when (this) {
            DomainError.Unauthorized -> "로그인이 만료됐어요. 다시 로그인해 주세요"
            DomainError.NotFound -> "주문을 찾지 못했어요"
            is DomainError.Validation -> "주문 금액을 확인해 주세요"
            is DomainError.Payment -> "결제수단을 확인하고 다시 시도해 주세요"
            DomainError.Network,
            DomainError.Timeout,
            DomainError.Unknown,
            -> "결제를 완료하지 못했어요. 다시 시도해 주세요"
        }

    private fun DomainError.toPaymentConfirmationMessage(): String =
        when (this) {
            DomainError.Network -> "네트워크 연결 때문에 결제 상태를 확인하지 못했어요. 다시 시도해 주세요"
            DomainError.Timeout -> "결제 확인이 지연되고 있어요. 다시 시도해 주세요"
            DomainError.NotFound -> "결제 승인 내역을 확인하지 못했어요. 다시 시도해 주세요"
            DomainError.Unauthorized -> "로그인이 만료되어 결제 상태를 확인하지 못했어요"
            is DomainError.Payment -> "결제 상태를 확인하지 못했어요. 다시 시도해 주세요"
            is DomainError.Validation -> "주문 정보를 확인하지 못했어요. 다시 시도해 주세요"
            DomainError.Unknown -> "결제 상태를 확정하지 못했어요. 다시 시도해 주세요"
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

    private fun DomainError.needsStatusConfirmation(): Boolean =
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

private data class PaymentMethod(
    val id: String,
    val token: String,
)

private enum class MockPaymentOutcome {
    Success,
    Failure,
}

private val paymentMethods = listOf(
    PaymentMethod(
        id = "credit-card",
        token = "tok_credit_card_mock",
    ),
    PaymentMethod(
        id = "simple-pay",
        token = "tok_simple_pay_mock",
    ),
    PaymentMethod(
        id = "coupon",
        token = "tok_coupon_mock",
    ),
)

private const val MockFailureToken = "tok_mock_fail"
