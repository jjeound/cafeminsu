package com.cafeminsu.core.data.model

import com.cafeminsu.core.database.model.entity.menu.MenuEntity
import com.cafeminsu.core.database.model.entity.menu.MenuOptionEntity
import com.cafeminsu.core.database.model.relation.menu.MenuWithOptions
import com.cafeminsu.core.model.menu.MenuDetail
import com.cafeminsu.core.model.menu.MenuOption
import com.cafeminsu.core.model.menu.MenuSummary
import com.cafeminsu.core.network.model.response.menu.MenuDetailResponse
import com.cafeminsu.core.network.model.response.menu.MenuListItemResponse
import com.cafeminsu.core.network.model.response.menu.MenuOptionResponse

fun MenuListItemResponse.asEntity(
    storeId: Long,
): MenuEntity =
    MenuEntity(
        id = id,
        storeId = storeId,
        name = name,
        description = "",
        price = price,
        category = category,
        imageUrl = imageUrl.orEmpty(),
        isAvailable = isAvailable,
    )

fun MenuDetailResponse.asEntity(
    storeId: Long,
): MenuEntity =
    MenuEntity(
        id = id,
        storeId = storeId,
        name = name,
        description = description,
        price = price,
        category = category,
        imageUrl = imageUrl.orEmpty(),
        isAvailable = isAvailable,
    )

fun MenuOptionResponse.asEntity(
    menuId: Long,
): MenuOptionEntity =
    MenuOptionEntity(
        id = id,
        menuId = menuId,
        groupName = group,
        name = name,
        additionalPrice = additionalPrice,
        isDefault = isDefault,
    )

fun MenuDetailResponse.asOptionEntities(): List<MenuOptionEntity> =
    options.map { option ->
        option.asEntity(menuId = id)
    }

fun MenuListItemResponse.asExternalModel(): MenuSummary =
    MenuSummary(
        id = id,
        name = name,
        price = price,
        category = category,
        image = imageUrl.orEmpty(),
        isAvailable = isAvailable,
    )

fun MenuDetailResponse.asExternalModel(): MenuDetail =
    MenuDetail(
        id = id,
        name = name,
        description = description,
        price = price,
        category = category,
        image = imageUrl.orEmpty(),
        isAvailable = isAvailable,
        options = options.map(MenuOptionResponse::asExternalModel),
    )

fun MenuOptionResponse.asExternalModel(): MenuOption =
    MenuOption(
        id = id,
        groupName = group,
        name = name,
        additionalPrice = additionalPrice,
        isDefault = isDefault,
    )

fun MenuEntity.asExternalModel(): MenuSummary =
    MenuSummary(
        id = id,
        name = name,
        price = price,
        category = category,
        image = imageUrl,
        isAvailable = isAvailable,
    )

fun MenuEntity.asExternalModel(
    options: List<MenuOptionEntity>,
): MenuDetail =
    MenuDetail(
        id = id,
        name = name,
        description = description,
        price = price,
        category = category,
        image = imageUrl,
        isAvailable = isAvailable,
        options = options.map(MenuOptionEntity::asExternalModel),
    )

fun MenuWithOptions.asExternalModel(): MenuDetail =
    menu.asExternalModel(options)

fun MenuOptionEntity.asExternalModel(): MenuOption =
    MenuOption(
        id = id,
        groupName = groupName,
        name = name,
        additionalPrice = additionalPrice,
        isDefault = isDefault,
    )