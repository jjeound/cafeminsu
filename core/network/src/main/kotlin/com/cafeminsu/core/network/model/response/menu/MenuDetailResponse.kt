package com.cafeminsu.core.network.model.response.menu

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.cafeminsu.core.model.menu.MenuDetail
import com.cafeminsu.core.model.media.ImageSource

@Serializable
data class MenuDetailResponse(
    @SerialName(value = "id") val id: Long,
    @SerialName(value = "name") val name: String,
    @SerialName(value = "description") val description: String,
    @SerialName(value = "price") val price: Int,
    @SerialName(value = "category") val category: String,
    @SerialName(value = "imageUrl") val imageUrl: String?,
    @SerialName(value = "isAvailable") val isAvailable: Boolean,
    @SerialName(value = "options") val options: List<MenuOptionResponse>,
)

fun MenuDetailResponse.asExternalModel(): MenuDetail =
    MenuDetail(
        id = id,
        name = name,
        description = description,
        price = price,
        category = category,
        image = imageUrl?.let(ImageSource::Remote) ?: ImageSource.None,
        isAvailable = isAvailable,
        options = options.map(MenuOptionResponse::asExternalModel),
    )
