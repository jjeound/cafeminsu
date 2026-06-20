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
    val channel: GiftChannel,
    val recipientRef: String,
    val message: String?,
)

enum class GiftChannel {
    KakaoTalk,
    Sms,
}

data class GiftSendResult(
    val giftId: String,
    val sentAtMillis: Long,
)
