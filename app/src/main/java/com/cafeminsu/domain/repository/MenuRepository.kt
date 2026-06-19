package com.cafeminsu.domain.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.MenuCategory
import com.cafeminsu.domain.model.MenuItem
import kotlinx.coroutines.flow.Flow

interface MenuRepository {
    fun observeCategories(): Flow<AppResult<List<MenuCategory>>>
    fun observeMenus(categoryId: String? = null): Flow<AppResult<List<MenuItem>>>
    suspend fun getMenu(menuItemId: String): AppResult<MenuItem>
    suspend fun refreshMenus(): AppResult<Unit>
}
