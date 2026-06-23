package com.cafeminsu.core.data.model

import com.cafeminsu.core.database.model.entity.store.StoreDetailEntity
import com.cafeminsu.core.database.model.entity.store.StoreEntity
import com.cafeminsu.core.database.model.entity.store.StoreSearchEntity
import com.cafeminsu.core.model.media.ImageSource
import com.cafeminsu.core.model.store.StoreDetail
import com.cafeminsu.core.model.store.StoreSummary
import com.cafeminsu.core.network.model.response.store.StoreDetailResponse
import com.cafeminsu.core.network.model.response.store.StoreSearchItemResponse

fun StoreSearchItemResponse.asEntity(): StoreEntity =
    StoreEntity(id = id, name = name, address = address, imageUrl = imageUrl)

fun StoreSearchItemResponse.asEntity(query: String, page: Int, position: Int): StoreSearchEntity =
    StoreSearchEntity(query = query, page = page, position = position, storeId = id)

fun StoreDetailResponse.asEntity(): StoreDetailEntity =
    StoreDetailEntity(
        storeId = id,
        name = name,
        address = address,
        latitude = latitude,
        longitude = longitude,
        phone = phone,
        businessHours = businessHours,
        imageUrl = imageUrl,
    )

fun StoreEntity.asExternalModel(): StoreSummary =
    StoreSummary(
        id = id,
        name = name,
        address = address,
        image = imageUrl?.let(ImageSource::Remote) ?: ImageSource.None,
    )

fun StoreDetailEntity.asExternalModel(): StoreDetail =
    StoreDetail(
        id = storeId,
        name = name,
        address = address,
        latitude = latitude,
        longitude = longitude,
        phone = phone,
        businessHours = businessHours,
        image = imageUrl?.let(ImageSource::Remote) ?: ImageSource.None,
    )
