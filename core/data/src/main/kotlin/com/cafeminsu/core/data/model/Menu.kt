package com.cafeminsu.core.data.model

import com.cafeminsu.core.database.model.entity.menu.MenuDetailEntity
import com.cafeminsu.core.database.model.entity.menu.MenuEntity
import com.cafeminsu.core.database.model.entity.menu.MenuOptionEntity
import com.cafeminsu.core.model.media.ImageSource
import com.cafeminsu.core.model.menu.MenuDetail
import com.cafeminsu.core.model.menu.MenuOption
import com.cafeminsu.core.model.menu.MenuSummary
import com.cafeminsu.core.network.model.response.menu.MenuDetailResponse
import com.cafeminsu.core.network.model.response.menu.MenuListItemResponse
import com.cafeminsu.core.network.model.response.menu.MenuOptionResponse

fun MenuListItemResponse.asEntity(storeId: Long): MenuEntity =
    MenuEntity(
        id = id,
        storeId = storeId,
        name = name,
        price = price,
        category = category,
        imageUrl = imageUrl,
        isAvailable = isAvailable
    )

fun MenuDetailResponse.asEntity(): MenuDetailEntity =
    MenuDetailEntity(
        menuId = id,
        name = name,
        description = description,
        price = price,
        category = category,
        imageUrl = imageUrl,
        isAvailable = isAvailable
    )

fun MenuOptionResponse.asEntity(menuId: Long): MenuOptionEntity =
    MenuOptionEntity(
        id = id,
        menuId = menuId,
        groupName = group,
        name = name,
        additionalPrice = additionalPrice,
        isDefault = isDefault
    )

fun MenuListItemResponse.asExternalModel() = MenuSummary(
    id,
    name,
    price,
    category,
    imageUrl?.let(ImageSource::Remote) ?: ImageSource.None,
    isAvailable
)

fun MenuDetailResponse.asExternalModel() = MenuDetail(
    id,
    name,
    description,
    price,
    category,
    imageUrl?.let(ImageSource::Remote) ?: ImageSource.None,
    isAvailable,
    options.map(MenuOptionResponse::asExternalModel)
)

fun MenuOptionResponse.asExternalModel() =
    MenuOption(id, group, name, additionalPrice, isDefault)

fun MenuEntity.asExternalModel(): MenuSummary =
    MenuSummary(
        id = id,
        name = name,
        price = price,
        category = category,
        image = imageUrl?.let(ImageSource::Remote) ?: ImageSource.None,
        isAvailable = isAvailable
    )

fun MenuDetailEntity.asExternalModel(options: List<MenuOptionEntity>): MenuDetail =
    MenuDetail(
        id = menuId,
        name = name,
        description = description,
        price = price,
        category = category,
        image = imageUrl?.let(ImageSource::Remote) ?: ImageSource.None,
        isAvailable = isAvailable,
        options = options.map(MenuOptionEntity::asExternalModel),
    )

fun MenuOptionEntity.asExternalModel(): MenuOption =
    MenuOption(
        id = id,
        groupName = groupName,
        name = name,
        additionalPrice = additionalPrice,
        isDefault = isDefault
    )
