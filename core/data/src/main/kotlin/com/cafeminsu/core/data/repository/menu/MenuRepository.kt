package com.cafeminsu.core.data.repository.menu

import com.cafeminsu.core.model.menu.MenuDetail
import com.cafeminsu.core.model.menu.MenuSummary
import kotlinx.coroutines.flow.Flow

interface MenuRepository {
    fun getMenuSummaries(storeId: Long): Flow<List<MenuSummary>>
    fun getMenu(id: Long): Flow<MenuDetail>
}
