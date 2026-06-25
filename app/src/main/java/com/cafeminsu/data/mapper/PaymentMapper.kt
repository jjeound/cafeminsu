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
    // 서버가 확정한 카드 결제 금액. 전액 기프티콘이면 0.
    val cardAmount: Int,
    // 전액 기프티콘이면 Approved(즉시 PAID), 카드 포함이면 Pending(READY → authorize/verify 필요).
    val status: PaymentStatus,
    // 전액 기프티콘 PAID 응답에 포함되는 결제 ID. 카드 흐름(READY)에서는 null.
    val paymentId: String?,
)

fun PaymentPrepareRes.toPreparedPayment(orderId: String): AppResult<PreparedPayment> {
    val merchantUid = merchantUid
        ?.takeIf { it.isNotBlank() }
        ?: return AppResult.Failure(DomainError.Unknown)
    // cardAmount가 권위 값이며, 구버전 서버 호환을 위해 amount로 폴백한다.
    val cardAmount = cardAmount ?: amount ?: return AppResult.Failure(DomainError.Unknown)
    val mappedStatus = status.toPreparedStatus()
    val mappedPaymentId = paymentId?.toString()
    // 전액 기프티콘 PAID 확정 응답은 결제 ID가 반드시 있어야 한다.
    if (mappedStatus == PaymentStatus.Approved && mappedPaymentId == null) {
        return AppResult.Failure(DomainError.Unknown)
    }

    return AppResult.Success(
        PreparedPayment(
            orderId = orderId,
            merchantUid = merchantUid,
            cardAmount = cardAmount,
            status = mappedStatus,
            paymentId = mappedPaymentId,
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

// prepare 응답 상태 매핑: PAID(전액 기프티콘)만 즉시 승인, 그 외(READY·미지정)는 카드 흐름으로 진행.
private fun String?.toPreparedStatus(): PaymentStatus =
    when (this) {
        "PAID" -> PaymentStatus.Approved
        else -> PaymentStatus.Pending
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
