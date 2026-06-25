package com.ssafy.cafeminsu.core.data.model

import com.ssafy.cafeminsu.core.database.model.entity.menu.MenuEntity
import com.ssafy.cafeminsu.core.database.model.entity.menu.MenuOptionEntity
import com.ssafy.cafeminsu.core.database.model.relation.menu.MenuWithOptions
import com.ssafy.cafeminsu.core.model.menu.MenuDetail
import com.ssafy.cafeminsu.core.model.menu.MenuOption
import com.ssafy.cafeminsu.core.model.menu.MenuSummary
import com.ssafy.cafeminsu.core.network.model.response.menu.MenuDetailResponse
import com.ssafy.cafeminsu.core.network.model.response.menu.MenuListItemResponse
import com.ssafy.cafeminsu.core.network.model.response.menu.MenuOptionResponse

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

fun MenuListItemResponse.asExternalModel(): com.ssafy.cafeminsu.core.model.menu.MenuSummary =
    _root_ide_package_.com.ssafy.cafeminsu.core.model.menu.MenuSummary(
        id = id,
        name = name,
        price = price,
        category = category,
        image = imageUrl.orEmpty(),
        isAvailable = isAvailable,
    )

fun MenuDetailResponse.asExternalModel(): com.ssafy.cafeminsu.core.model.menu.MenuDetail =
    _root_ide_package_.com.ssafy.cafeminsu.core.model.menu.MenuDetail(
        id = id,
        name = name,
        description = description,
        price = price,
        category = category,
        image = imageUrl.orEmpty(),
        isAvailable = isAvailable,
        options = options.map(MenuOptionResponse::asExternalModel),
    )

fun MenuOptionResponse.asExternalModel(): com.ssafy.cafeminsu.core.model.menu.MenuOption =
    _root_ide_package_.com.ssafy.cafeminsu.core.model.menu.MenuOption(
        id = id,
        groupName = group,
        name = name,
        additionalPrice = additionalPrice,
        isDefault = isDefault,
    )

fun MenuEntity.asExternalModel(): com.ssafy.cafeminsu.core.model.menu.MenuSummary =
    _root_ide_package_.com.ssafy.cafeminsu.core.model.menu.MenuSummary(
        id = id,
        name = name,
        price = price,
        category = category,
        image = imageUrl,
        isAvailable = isAvailable,
    )

fun MenuEntity.asExternalModel(
    options: List<MenuOptionEntity>,
): com.ssafy.cafeminsu.core.model.menu.MenuDetail =
    _root_ide_package_.com.ssafy.cafeminsu.core.model.menu.MenuDetail(
        id = id,
        name = name,
        description = description,
        price = price,
        category = category,
        image = imageUrl,
        isAvailable = isAvailable,
        options = options.map(MenuOptionEntity::asExternalModel),
    )

fun MenuWithOptions.asExternalModel(): com.ssafy.cafeminsu.core.model.menu.MenuDetail =
    menu.asExternalModel(options)

fun MenuOptionEntity.asExternalModel(): com.ssafy.cafeminsu.core.model.menu.MenuOption =
    _root_ide_package_.com.ssafy.cafeminsu.core.model.menu.MenuOption(
        id = id,
        groupName = groupName,
        name = name,
        additionalPrice = additionalPrice,
        isDefault = isDefault,
    )