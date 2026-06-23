package com.cafeminsu.core.data.repository.menu

import com.cafeminsu.core.model.menu.MenuCategory
import com.cafeminsu.core.model.menu.MenuDetail
import com.cafeminsu.core.model.menu.MenuSummary
import kotlinx.coroutines.flow.Flow

interface MenuRepository {
    fun observeCategories(storeId: Long): Flow<List<MenuCategory>>

    fun observeMenus(storeId: Long, category: String? = null): Flow<List<MenuSummary>>

    fun getMenu(menuId: Long): Flow<MenuDetail>

    fun refreshMenus(storeId: Long, category: String? = null): Flow<Unit>
}
