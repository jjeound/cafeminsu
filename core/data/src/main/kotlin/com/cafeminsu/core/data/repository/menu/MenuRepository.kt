package com.cafeminsu.core.data.repository.menu

import com.cafeminsu.core.model.menu.MenuCategory
import com.cafeminsu.core.model.menu.MenuItem
import kotlinx.coroutines.flow.Flow

interface MenuRepository {
    fun observeCategories(): Flow<List<MenuCategory>>

    fun observeMenus(categoryId: String? = null): Flow<List<MenuItem>>

    fun getMenu(menuItemId: String): Flow<MenuItem>

    fun refreshMenus(): Flow<Unit>
}
