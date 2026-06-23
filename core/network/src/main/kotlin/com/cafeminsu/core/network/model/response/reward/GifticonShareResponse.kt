package com.cafeminsu.core.network.model.response.reward

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GifticonShareResponse(
    @SerialName(value = "shareLink") val shareLink: String,
    @SerialName(value = "deepLink") val deepLink: String,
)
