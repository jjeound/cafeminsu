package com.cafeminsu.domain.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.model.NewMenuDraft
import kotlinx.coroutines.flow.Flow

interface OwnerMenuRepository {
    fun observeManagedMenus(categoryId: String? = null): Flow<AppResult<List<MenuItem>>>
    suspend fun setSoldOut(menuItemId: String, soldOut: Boolean): AppResult<MenuItem>
    suspend fun setVisible(menuItemId: String, visible: Boolean): AppResult<MenuItem>
    suspend fun addMenu(draft: NewMenuDraft): AppResult<MenuItem>
}
