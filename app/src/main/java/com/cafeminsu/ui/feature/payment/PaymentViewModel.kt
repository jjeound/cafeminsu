package com.cafeminsu.ui.feature.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.Gifticon
import com.cafeminsu.domain.model.GifticonStatus
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.PaymentRequest
import com.cafeminsu.domain.model.PaymentResult
import com.cafeminsu.domain.model.PaymentStatus
import com.cafeminsu.domain.repository.CartRepository
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
    private val cartRepository: CartRepository,
) : ViewModel() {
    private val orderId = savedStateHandle.get<String>(Routes.PAYMENT_ORDER_ID).orEmpty()
    private val _uiState = MutableStateFlow<PaymentUiState>(PaymentUiState.Loading)
    private val _events = MutableSharedFlow<PaymentEvent>(extraBufferCapacity = EventBufferCapacity)
    private var observeJob: Job? = null
    private var paymentInProgress = false
    private var idempotencyKey: String? = null
    private var latestOrder: Order? = null
    private var availableGifticons: List<Gifticon> = emptyList()
    private var selectedCouponId: String? = null

    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()
    val events: SharedFlow<PaymentEvent> = _events.asSharedFlow()

    init {
        observeOrder()
        observeGifticons()
    }

    fun onToggleCoupon(couponId: String) {
        if (paymentInProgress) {
            return
        }

        val content = _uiState.value as? PaymentUiState.Content ?: return
        if (content.coupons.none { coupon -> coupon.id == couponId }) {
            return
        }

        // 이미 선택된 쿠폰을 다시 누르면 적용 해제한다.
        selectedCouponId = if (selectedCouponId == couponId) null else couponId
        _uiState.value = content.copy(selectedCouponId = selectedCouponId)
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

    fun onRetryFailure() {
        payWithMockOutcome(MockPaymentOutcome.Success)
    }

    fun onDismissFailure() {
        val content = _uiState.value as? PaymentUiState.Content ?: return
        if (content.paymentState is PaymentProgress.Failed) {
            _uiState.value = content.copy(paymentState = PaymentProgress.Idle)
        }
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
                amount = content.payableAmount,
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
                        is AppResult.Success -> {
                            latestOrder = result.data
                            result.data.toPaymentContent()
                        }

                        is AppResult.Failure -> result.error.toPaymentError()
                    }
                }
        }
    }

    private fun observeGifticons() {
        viewModelScope.launch {
            rewardRepository.observeGifticons()
                .catch { /* 기프티콘은 선택 사항 — 실패해도 결제는 진행 가능 */ }
                .collect { result ->
                    availableGifticons = when (result) {
                        is AppResult.Success -> result.data
                        is AppResult.Failure -> emptyList()
                    }
                    val order = latestOrder ?: return@collect
                    if (_uiState.value is PaymentUiState.Content) {
                        _uiState.value = order.toPaymentContent()
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
            finishPayment(PaymentProgress.Failed(paymentFailureUiModel(error.toPaymentFailureReason())))
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
                progress = PaymentProgress.Failed(paymentFailureUiModel(PaymentFailureReason.LimitExceeded)),
            )

            PaymentStatus.Cancelled -> failPayment(
                orderId = request.orderId,
                progress = PaymentProgress.Failed(paymentFailureUiModel(PaymentFailureReason.Cancelled)),
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
        redeemSelectedCoupon()
        clearCartAfterApproval()
        _events.emit(PaymentEvent.PaymentApproved(approvedOrderId))
    }

    private suspend fun clearCartAfterApproval() {
        // 결제 승인이 확정된 이후에만 장바구니를 비운다(낙관적 처리 금지).
        // clear()는 AppResult를 반환하므로 실패는 무시한다 — 성공 흐름을 막지 않는다.
        cartRepository.clear()
    }

    private suspend fun redeemSelectedCoupon() {
        val gifticonId = selectedCouponId ?: return
        try {
            rewardRepository.markGifticonUsed(gifticonId)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            // 비치명적: 결제 승인이 우선이며 기프티콘 사용 처리는 추후 재시도로 정합화할 수 있다.
        }
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

        val couponModels = availableGifticons
            .filter { gifticon -> gifticon.status == GifticonStatus.Available }
            .map { gifticon -> gifticon.toPaymentCouponUiModel(items) }
        // 더 이상 사용할 수 없는 쿠폰이 선택돼 있으면 선택을 해제한다.
        val validCouponId = selectedCouponId?.takeIf { id -> couponModels.any { it.id == id } }
        selectedCouponId = validCouponId

        return PaymentUiState.Content(
            orderId = id,
            orderNumber = orderNumber,
            items = items,
            totalAmount = totalAmount,
            methods = methods,
            selectedMethodId = selectedMethodId,
            paymentState = current?.paymentState ?: PaymentProgress.Idle,
            coupons = couponModels,
            selectedCouponId = validCouponId,
        )
    }

    private fun Gifticon.toPaymentCouponUiModel(items: List<CartItem>): PaymentCouponUiModel {
        // 금액권(amount>0)은 잔액만큼만 할인하고,
        // 금액 정보가 없는 교환권(무료 음료 등)만 가장 비싼 한 잔 가격으로 폴백한다.
        val discount = if (amount > 0) amount else items.maxOfOrNull { item -> item.unitPrice } ?: 0
        return PaymentCouponUiModel(
            id = id,
            label = title,
            discountAmount = discount,
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

    private fun DomainError.toPaymentFailureReason(): PaymentFailureReason =
        when (this) {
            DomainError.Network,
            DomainError.Timeout,
            -> PaymentFailureReason.Network

            is DomainError.Payment,
            is DomainError.Validation,
            -> PaymentFailureReason.InvalidPaymentInfo

            DomainError.Unauthorized,
            DomainError.NotFound,
            DomainError.Unknown,
            -> PaymentFailureReason.Unknown
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
        id = "kakaopay",
        token = "tok_kakaopay_mock",
    ),
    PaymentMethod(
        id = "coupon",
        token = "tok_coupon_mock",
    ),
)

private const val MockFailureToken = "tok_mock_fail"
