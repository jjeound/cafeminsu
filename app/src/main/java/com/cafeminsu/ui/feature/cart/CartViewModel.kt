package com.cafeminsu.ui.feature.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.Cart
import com.cafeminsu.domain.model.CartInvalidReason
import com.cafeminsu.domain.model.CartValidation
import com.cafeminsu.domain.repository.CartRepository
import com.cafeminsu.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val orderRepository: OrderRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<CartUiState>(CartUiState.Loading)
    private val _events = MutableSharedFlow<CartEvent>(extraBufferCapacity = EventBufferCapacity)
    private var latestCart: Cart? = null
    private var checkoutInProgress = false

    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()
    val events: SharedFlow<CartEvent> = _events.asSharedFlow()

    init {
        observeCart()
    }

    fun onQuantityChange(cartItemId: String, quantity: Int) {
        if (quantity < RemoveQuantity) {
            return
        }

        viewModelScope.launch {
            when (val result = cartRepository.updateQuantity(cartItemId, quantity)) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> _uiState.value = result.error.toCartError()
            }
        }
    }

    fun onRemove(cartItemId: String) {
        viewModelScope.launch {
            when (val result = cartRepository.removeItem(cartItemId)) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> _uiState.value = result.error.toCartError()
            }
        }
    }

    fun onCheckout() {
        if (checkoutInProgress) {
            return
        }

        val cart = latestCart ?: return
        checkoutInProgress = true
        _uiState.value = cart.toCartUiState(checkoutInProgress = true)

        viewModelScope.launch {
            when (val validationResult = cartRepository.validateForCheckout()) {
                is AppResult.Success -> handleCheckoutValidation(cart, validationResult.data)
                is AppResult.Failure -> failCheckout(validationResult.error)
            }
        }
    }

    fun retry() {
        latestCart?.let { cart ->
            _uiState.value = cart.toCartUiState(checkoutInProgress = checkoutInProgress)
        }
    }

    private fun observeCart() {
        viewModelScope.launch {
            cartRepository.observeCart()
                .catch { emit(AppResult.Failure(DomainError.Unknown)) }
                .collect { result ->
                    when (result) {
                        is AppResult.Success -> {
                            latestCart = result.data
                            _uiState.value = result.data.toCartUiState(
                                checkoutInProgress = checkoutInProgress,
                            )
                        }

                        is AppResult.Failure -> {
                            _uiState.value = result.error.toCartError()
                        }
                    }
                }
        }
    }

    private suspend fun handleCheckoutValidation(
        cart: Cart,
        validation: CartValidation,
    ) {
        when (validation) {
            CartValidation.Valid -> createOrder(cart.copy(validation = CartValidation.Valid))
            is CartValidation.Invalid -> {
                checkoutInProgress = false
                _uiState.value = cart.toCartUiState(
                    checkoutInProgress = false,
                    validation = validation,
                )
            }
        }
    }

    private suspend fun createOrder(cart: Cart) {
        when (val result = orderRepository.createOrderFromCart(cart)) {
            is AppResult.Success -> {
                checkoutInProgress = false
                latestCart?.let { currentCart ->
                    _uiState.value = currentCart.toCartUiState(checkoutInProgress = false)
                }
                _events.emit(CartEvent.NavigateToPayment(result.data.id))
            }

            is AppResult.Failure -> failCheckout(result.error)
        }
    }

    private fun failCheckout(error: DomainError) {
        checkoutInProgress = false
        _uiState.value = error.toCartError()
    }

    private fun Cart.toCartUiState(
        checkoutInProgress: Boolean,
        validation: CartValidation = this.validation,
    ): CartUiState =
        if (items.isEmpty()) {
            CartUiState.Empty(
                message = "담은 메뉴가 없어요",
                minimumOrderAmount = minimumOrderAmount,
                validation = validation.ensureEmptyReason(),
                checkoutInProgress = checkoutInProgress,
            )
        } else {
            CartUiState.Content(
                items = items,
                subtotal = subtotal,
                minimumOrderAmount = minimumOrderAmount,
                validation = validation,
                checkoutInProgress = checkoutInProgress,
            )
        }

    private fun CartValidation.ensureEmptyReason(): CartValidation =
        when (this) {
            CartValidation.Valid -> CartValidation.Invalid(listOf(CartInvalidReason.Empty))
            is CartValidation.Invalid -> this
        }

    private fun DomainError.toCartError(): CartUiState.Error =
        CartUiState.Error(
            message = toCartMessage(),
            retryable = isRetryable(),
        )

    private fun DomainError.toCartMessage(): String =
        when (this) {
            DomainError.Network -> "네트워크 연결을 확인하고 다시 시도해 주세요"
            DomainError.Timeout -> "응답이 지연되고 있어요. 잠시 후 다시 시도해 주세요"
            DomainError.Unauthorized -> "로그인이 만료됐어요. 다시 로그인해 주세요"
            DomainError.NotFound -> "장바구니 항목을 찾지 못했어요"
            is DomainError.Payment -> "결제 정보를 확인하지 못했어요"
            is DomainError.Validation -> "장바구니를 확인해 주세요"
            DomainError.Unknown -> "장바구니를 불러오지 못했어요"
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
        const val RemoveQuantity = 0
    }
}
