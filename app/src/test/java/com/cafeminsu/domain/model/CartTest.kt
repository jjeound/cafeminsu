package com.cafeminsu.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class CartTest {
    @Test
    fun exposesCartDomainModelsAndValidationReasons() {
        val selectedOption = SelectedOption(
            groupId = "size",
            optionId = "large",
            name = "라지",
            extraPrice = 700,
        )
        val cartItem = CartItem(
            id = "cart-1",
            menuItemId = "latte",
            name = "카페라떼",
            unitPrice = 5200,
            selectedOptions = listOf(selectedOption),
            quantity = 2,
        )
        val validation = CartValidation.Invalid(
            reasons = listOf(
                CartInvalidReason.SoldOut(menuItemId = "latte"),
                CartInvalidReason.PriceChanged(menuItemId = "latte", latestPrice = 5500),
                CartInvalidReason.OptionUnavailable(optionId = "large"),
                CartInvalidReason.StoreClosed,
            ),
        )
        val cart = Cart(
            items = listOf(cartItem),
            subtotal = 11800,
            validation = validation,
        )

        assertEquals(11800, cart.subtotal)
        assertEquals(2, cart.items.single().quantity)
        assertSame(CartValidation.Valid, CartValidation.Valid)
        assertEquals(4, validation.reasons.size)
    }

    @Test
    fun exposesEmptyCartInvalidReason() {
        val validation = CartValidation.Invalid(
            reasons = listOf(CartInvalidReason.Empty),
        )

        assertSame(CartInvalidReason.Empty, validation.reasons.single())
    }
}
