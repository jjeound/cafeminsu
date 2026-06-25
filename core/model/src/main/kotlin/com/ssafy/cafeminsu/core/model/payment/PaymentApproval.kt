package com.ssafy.cafeminsu.core.model.payment

sealed interface PaymentApproval {
    data object NotApproved : PaymentApproval

    data class Approved(val approvedAtMillis: Long) : PaymentApproval
}
