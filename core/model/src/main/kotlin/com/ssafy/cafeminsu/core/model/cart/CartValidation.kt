package com.ssafy.cafeminsu.core.model.cart

sealed interface CartValidation {
    data object Valid : CartValidation
    data class Invalid(val reasons: List<CartInvalidReason>) : CartValidation
}
