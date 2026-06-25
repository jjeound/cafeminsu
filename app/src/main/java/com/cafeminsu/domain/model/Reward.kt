package com.cafeminsu.domain.model

data class StampCard(
    val userId: String,
    val currentCount: Int,
    val goalCount: Int,
    val history: List<StampEvent>,
)

data class StampEvent(
    val id: String,
    val orderId: String,
    val count: Int,
    val createdAtMillis: Long,
)

data class Gifticon(
    val id: String,
    val title: String,
    val barcodeValue: String,
    val qrValue: String,
    val expiresAtMillis: Long,
    val status: GifticonStatus,
    // 금액형 기프티콘의 잔액(원). 0이면 금액 정보가 없는 교환권(무료 음료 등).
    val amount: Int = 0,
)

enum class GifticonStatus {
    Available,
    Used,
    Expired,
}

data class Coupon(
    val id: String,
    val type: CouponType,
    val title: String,
    val amount: Int?,
    val expiresAtMillis: Long,
    val status: CouponStatus,
)

enum class CouponType {
    FreeDrink,
    Amount,
}

enum class CouponStatus {
    Available,
    Used,
    Expired,
}

data class GiftSendRequest(
    val amount: Int,
    val message: String?,
)

data class GiftSendResult(
    val giftId: String,
    val sentAtMillis: Long,
    val shareLink: String? = null,
    val claimCode: String? = null,
)
