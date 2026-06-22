package com.cafeminsu.ui.feature.cart

import com.cafeminsu.domain.model.CartInvalidReason
import com.cafeminsu.domain.model.CartValidation
import com.cafeminsu.domain.model.OrderType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CartUiStateTest {
    @Test
    fun contentCanCheckoutOnlyWhenValidationIsValidAndNotInProgress() {
        assertTrue(content(validation = CartValidation.Valid, checkoutInProgress = false).canCheckout)
        assertFalse(content(validation = CartValidation.Valid, checkoutInProgress = true).canCheckout)
        assertFalse(
            content(
                validation = CartValidation.Invalid(listOf(CartInvalidReason.SoldOut("latte"))),
                checkoutInProgress = false,
            ).canCheckout,
        )
    }

    private fun content(
        validation: CartValidation,
        checkoutInProgress: Boolean,
    ): CartUiState.Content =
        CartUiState.Content(
            items = emptyList(),
            subtotal = 0,
            validation = validation,
            checkoutInProgress = checkoutInProgress,
            orderType = OrderType.DineIn,
            requestNote = "",
        )
}
