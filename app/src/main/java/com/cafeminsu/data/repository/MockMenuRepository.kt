package com.cafeminsu.data.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.mock.MockData
import com.cafeminsu.domain.model.MenuCategory
import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.repository.MenuRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class MockMenuRepository(
    categories: List<MenuCategory> = MockData.menuCategories,
    menuItems: List<MenuItem> = MockData.menuItems,
) : MenuRepository {
    private val categoryState = MutableStateFlow(categories.sortedBy { it.sortOrder })
    private val menuState = MutableStateFlow(menuItems)

    override fun observeCategories(): Flow<AppResult<List<MenuCategory>>> =
        categoryState.map { AppResult.Success(it) }

    override fun observeMenus(categoryId: String?): Flow<AppResult<List<MenuItem>>> =
        menuState.map { menus ->
            AppResult.Success(
                if (categoryId == null) {
                    menus
                } else {
                    menus.filter { it.categoryId == categoryId }
                },
            )
        }

    override suspend fun getMenu(menuItemId: String): AppResult<MenuItem> {
        val menu = menuState.value.firstOrNull { it.id == menuItemId }
        return if (menu == null) {
            AppResult.Failure(DomainError.NotFound)
        } else {
            AppResult.Success(menu)
        }
    }

    override suspend fun refreshMenus(): AppResult<Unit> {
        categoryState.value = MockData.menuCategories.sortedBy { it.sortOrder }
        menuState.value = MockData.menuItems
        return AppResult.Success(Unit)
    }
}
