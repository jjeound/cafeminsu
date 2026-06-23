package com.cafeminsu.core.data.repository.menu

import com.cafeminsu.core.model.menu.MenuSummary
import com.cafeminsu.core.model.menu.NewMenuDraft
import kotlinx.coroutines.flow.Flow

interface OwnerMenuRepository {
    fun observeManagedMenus(categoryId: String? = null): Flow<List<MenuSummary>>

    fun setSoldOut(menuItemId: Long, soldOut: Boolean): Flow<MenuSummary>

    fun setVisible(menuItemId: Long, visible: Boolean): Flow<MenuSummary>

    fun addMenu(draft: NewMenuDraft): Flow<MenuSummary>
}
