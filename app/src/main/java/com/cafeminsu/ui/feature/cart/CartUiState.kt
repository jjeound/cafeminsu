package com.cafeminsu.ui.feature.cart

import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.CartValidation

sealed interface CartUiState {
    data object Loading : CartUiState

    data class Content(
        val items: List<CartItem>,
        val subtotal: Int,
        val minimumOrderAmount: Int,
        val validation: CartValidation,
        val checkoutInProgress: Boolean,
    ) : CartUiState {
        val canCheckout: Boolean
            get() = validation == CartValidation.Valid && !checkoutInProgress
    }

    data class Empty(
        val message: String,
        val minimumOrderAmount: Int,
        val validation: CartValidation,
        val checkoutInProgress: Boolean,
    ) : CartUiState

    data class Error(
        val message: String,
        val retryable: Boolean,
    ) : CartUiState
}

sealed interface CartEvent {
    data class NavigateToPayment(val orderId: String) : CartEvent
}
