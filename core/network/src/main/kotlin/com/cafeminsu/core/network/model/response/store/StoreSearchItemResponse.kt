package com.cafeminsu.core.network.model.response.store

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.cafeminsu.core.model.store.StoreSummary
import com.cafeminsu.core.model.media.ImageSource

@Serializable
data class StoreSearchItemResponse(
    @SerialName(value = "id") val id: Long,
    @SerialName(value = "name") val name: String,
    @SerialName(value = "address") val address: String,
    @SerialName(value = "imageUrl") val imageUrl: String?,
)

fun StoreSearchItemResponse.asExternalModel(): StoreSummary =
    StoreSummary(
        id = id,
        name = name,
        address = address,
        image = imageUrl?.let(ImageSource::Remote) ?: ImageSource.None,
    )
