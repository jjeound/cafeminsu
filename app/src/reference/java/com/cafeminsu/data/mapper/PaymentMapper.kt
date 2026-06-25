package com.cafeminsu.data.mapper

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.remote.PaymentDetailRes
import com.cafeminsu.data.remote.PaymentPrepareRes
import com.cafeminsu.data.remote.PaymentVerifyRes
import com.cafeminsu.domain.model.PaymentResult
import com.cafeminsu.domain.model.PaymentStatus

data class PreparedPayment(
    val orderId: String,
    val merchantUid: String,
    val amount: Int,
)

fun PaymentPrepareRes.toPreparedPayment(orderId: String): AppResult<PreparedPayment> {
    val merchantUid = merchantUid
        ?.takeIf { it.isNotBlank() }
        ?: return AppResult.Failure(DomainError.Unknown)
    val amount = amount ?: return AppResult.Failure(DomainError.Unknown)

    return AppResult.Success(
        PreparedPayment(
            orderId = orderId,
            merchantUid = merchantUid,
            amount = amount,
        ),
    )
}

fun PaymentVerifyRes.toPaymentResult(orderId: String): AppResult<PaymentResult> {
    val id = paymentId ?: return AppResult.Failure(DomainError.Unknown)
    val mappedStatus = status.toPaymentStatus()
        ?: return AppResult.Failure(DomainError.Unknown)

    return AppResult.Success(
        PaymentResult(
            orderId = orderId,
            paymentId = id.toString(),
            status = mappedStatus,
            approvedAtMillis = null,
        ),
    )
}

fun PaymentDetailRes.toPaymentResult(): AppResult<PaymentResult> {
    val id = paymentId ?: return AppResult.Failure(DomainError.Unknown)
    val mappedOrderId = orderId ?: return AppResult.Failure(DomainError.Unknown)
    val mappedStatus = status.toPaymentStatus()
        ?: return AppResult.Failure(DomainError.Unknown)

    return AppResult.Success(
        PaymentResult(
            orderId = mappedOrderId.toString(),
            paymentId = id.toString(),
            status = mappedStatus,
            approvedAtMillis = if (mappedStatus == PaymentStatus.Approved) {
                paidAt.toEpochMillisOrNull()
            } else {
                null
            },
        ),
    )
}

private fun String?.toPaymentStatus(): PaymentStatus? =
    when (this) {
        "READY" -> PaymentStatus.Pending
        "PAID" -> PaymentStatus.Approved
        "FAILED" -> PaymentStatus.Failed
        "REFUNDED" -> PaymentStatus.Cancelled
        else -> null
    }

private fun String?.toEpochMillisOrNull(): Long? =
    this?.let { value -> parseServerEpochMillis(value).takeIf { millis -> millis != 0L } }
