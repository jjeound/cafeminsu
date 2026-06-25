package com.cafeminsu.core.data.repository.menu

import com.cafeminsu.core.model.menu.MenuDetail
import com.cafeminsu.core.model.menu.MenuSummary
import kotlinx.coroutines.flow.Flow

interface MenuRepository {
    fun getMenuSummaries(
        storeId: Long,
        category: String = "",
    ): Flow<List<MenuSummary>>

    fun getMenu(
        menuId: Long,
    ): Flow<MenuDetail?>

    suspend fun syncMenuSummaries(
        storeId: Long,
    )

    suspend fun syncMenu(
        menuId: Long,
    )
}
