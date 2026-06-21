package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.model.NewMenuDraft
import com.cafeminsu.domain.repository.OwnerMenuRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

@Singleton
class MockOwnerMenuRepository(
    menuItems: List<MenuItem>,
) : OwnerMenuRepository {
    @Inject
    constructor() : this(menuItems = ownerMenuSeed)

    private val menuState = MutableStateFlow(menuItems)

    override fun observeManagedMenus(categoryId: String?): Flow<AppResult<List<MenuItem>>> =
        menuState.map { menus ->
            AppResult.Success(
                menus
                    .filter { it.isVisible }
                    .filter { categoryId == null || it.categoryId == categoryId },
            )
        }

    override suspend fun setSoldOut(menuItemId: String, soldOut: Boolean): AppResult<MenuItem> {
        val menu = menuState.value.firstOrNull { it.id == menuItemId }
            ?: return AppResult.Failure(DomainError.NotFound)
        val updated = menu.copy(isSoldOut = soldOut)
        menuState.value = menuState.value.map { current ->
            if (current.id == menuItemId) updated else current
        }
        return AppResult.Success(updated)
    }

    override suspend fun setVisible(menuItemId: String, visible: Boolean): AppResult<MenuItem> {
        val menu = menuState.value.firstOrNull { it.id == menuItemId }
            ?: return AppResult.Failure(DomainError.NotFound)
        val updated = menu.copy(isVisible = visible)
        menuState.value = menuState.value.map { current ->
            if (current.id == menuItemId) updated else current
        }
        return AppResult.Success(updated)
    }

    override suspend fun addMenu(draft: NewMenuDraft): AppResult<MenuItem> {
        val created = MenuItem(
            id = "menu-${UUID.randomUUID()}",
            categoryId = draft.categoryId,
            name = draft.name,
            description = draft.description,
            basePrice = draft.basePrice,
            imageUrl = draft.imageUrl,
            isSoldOut = draft.isSoldOut,
            options = emptyList(),
            isVisible = true,
        )
        menuState.value = menuState.value + created
        return AppResult.Success(created)
    }
}

private val ownerMenuSeed: List<MenuItem> = listOf(
    ownerMenuItem(
        id = "americano",
        categoryId = "coffee",
        name = "아메리카노",
        basePrice = 4_500,
    ),
    ownerMenuItem(
        id = "cafe-latte",
        categoryId = "coffee",
        name = "카페라떼",
        basePrice = 5_000,
    ),
    ownerMenuItem(
        id = "vanilla-latte",
        categoryId = "noncoffee",
        name = "바닐라라떼",
        basePrice = 5_500,
        isSoldOut = true,
    ),
    ownerMenuItem(
        id = "cold-brew",
        categoryId = "coffee",
        name = "콜드브루",
        basePrice = 5_500,
    ),
    ownerMenuItem(
        id = "choco-cookie",
        categoryId = "dessert",
        name = "초코쿠키",
        basePrice = 3_500,
        isSoldOut = true,
    ),
    ownerMenuItem(
        id = "tiramisu",
        categoryId = "dessert",
        name = "티라미수",
        basePrice = 6_500,
    ),
)

private fun ownerMenuItem(
    id: String,
    categoryId: String,
    name: String,
    basePrice: Int,
    isSoldOut: Boolean = false,
): MenuItem =
    MenuItem(
        id = id,
        categoryId = categoryId,
        name = name,
        description = "",
        basePrice = basePrice,
        imageUrl = null,
        isSoldOut = isSoldOut,
        options = emptyList(),
    )
