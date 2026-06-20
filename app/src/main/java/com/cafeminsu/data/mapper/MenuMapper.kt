package com.cafeminsu.data.mapper

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.remote.MenuDetailRes
import com.cafeminsu.data.remote.MenuListItemRes
import com.cafeminsu.data.remote.OptionRes
import com.cafeminsu.domain.model.MenuCategory
import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.model.MenuOption
import com.cafeminsu.domain.model.MenuOptionGroup

fun List<MenuListItemRes>.toMenuCategories(): List<MenuCategory> =
    mapNotNull { it.category.normalizedCategory() }
        .distinct()
        .mapIndexed { index, category ->
            MenuCategory(
                id = category,
                name = category,
                sortOrder = index + SortOrderOffset,
            )
        }
        .sortedBy { it.sortOrder }

fun List<MenuListItemRes>.toMenuItems(): AppResult<List<MenuItem>> {
    val mapped = map { item ->
        when (val result = item.toMenuItem()) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> return result
        }
    }
    return AppResult.Success(mapped)
}

fun MenuDetailRes.toMenuItem(): AppResult<MenuItem> {
    val menuId = id ?: return AppResult.Failure(DomainError.Unknown)
    return AppResult.Success(
        MenuItem(
            id = menuId.toString(),
            categoryId = category.normalizedCategory().orEmpty(),
            name = name.orEmpty(),
            description = description.orEmpty(),
            basePrice = price ?: DefaultPrice,
            imageUrl = imageUrl,
            isSoldOut = isAvailable != true,
            options = options.orEmpty().toOptionGroups(),
            isVisible = true,
        ),
    )
}

private fun MenuListItemRes.toMenuItem(): AppResult<MenuItem> {
    val menuId = id ?: return AppResult.Failure(DomainError.Unknown)
    return AppResult.Success(
        MenuItem(
            id = menuId.toString(),
            categoryId = category.normalizedCategory().orEmpty(),
            name = name.orEmpty(),
            description = "",
            basePrice = price ?: DefaultPrice,
            imageUrl = imageUrl,
            isSoldOut = isAvailable != true,
            options = emptyList(),
            isVisible = true,
        ),
    )
}

private fun List<OptionRes>.toOptionGroups(): List<MenuOptionGroup> =
    groupBy { it.optionGroup.normalizedOptionGroup() }
        .map { (groupName, options) ->
            val mappedOptions = options.mapNotNull { it.toMenuOption() }
            MenuOptionGroup(
                id = groupName,
                name = groupName,
                required = false,
                minSelect = 0,
                maxSelect = mappedOptions.size.coerceAtLeast(MinOptionMaxSelect),
                options = mappedOptions,
            )
        }

private fun OptionRes.toMenuOption(): MenuOption? {
    val id = optionId ?: return null
    return MenuOption(
        id = id.toString(),
        name = optionName.orEmpty(),
        extraPrice = optionPrice ?: DefaultPrice,
        isAvailable = true,
    )
}

private fun String?.normalizedCategory(): String? =
    normalize().takeIf { it.isNotEmpty() }

private fun String?.normalizedOptionGroup(): String =
    normalize().ifEmpty { DefaultOptionGroupName }

private fun String?.normalize(): String =
    this?.trim().orEmpty()

private const val SortOrderOffset = 1
private const val DefaultPrice = 0
private const val MinOptionMaxSelect = 1
private const val DefaultOptionGroupName = "옵션"
