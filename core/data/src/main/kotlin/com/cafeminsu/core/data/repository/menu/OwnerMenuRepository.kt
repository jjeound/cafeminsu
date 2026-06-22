package com.cafeminsu.core.data.repository.menu

import com.cafeminsu.core.model.menu.MenuItem
import com.cafeminsu.core.model.menu.NewMenuDraft
import kotlinx.coroutines.flow.Flow

interface OwnerMenuRepository {
    fun observeManagedMenus(categoryId: String? = null): Flow<List<MenuItem>>

    fun setSoldOut(menuItemId: String, soldOut: Boolean): Flow<MenuItem>

    fun setVisible(menuItemId: String, visible: Boolean): Flow<MenuItem>

    fun addMenu(draft: NewMenuDraft): Flow<MenuItem>
}
