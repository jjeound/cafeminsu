package com.cafeminsu.core.network.model.response.menu

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.cafeminsu.core.model.menu.MenuSummary
import com.cafeminsu.core.model.media.ImageSource

@Serializable
data class MenuListItemResponse(
    @SerialName(value = "id") val id: Long,
    @SerialName(value = "name") val name: String,
    @SerialName(value = "price") val price: Int,
    @SerialName(value = "category") val category: String,
    @SerialName(value = "imageUrl") val imageUrl: String?,
    @SerialName(value = "isAvailable") val isAvailable: Boolean,
)

fun MenuListItemResponse.asExternalModel(): MenuSummary =
    MenuSummary(
        id = id,
        name = name,
        price = price,
        category = category,
        image = imageUrl?.let(ImageSource::Remote) ?: ImageSource.None,
        isAvailable = isAvailable,
    )
