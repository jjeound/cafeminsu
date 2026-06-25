package com.ssafy.cafeminsu.core.model.reward

data class GiftSendRequest(
    val amount: Int,
    val channel: GiftChannel,
    val recipientRef: String,
    val message: String = "",
)
