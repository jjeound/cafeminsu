package com.cafeminsu.domain.repository

import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.MenuCategory
import com.cafeminsu.domain.model.MenuItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuRepositoryTest {
    @Test
    fun exposesMenuRepositoryContract() = runBlocking {
        val repository = object : MenuRepository {
            override fun observeCategories(): Flow<AppResult<List<MenuCategory>>> =
                flowOf(AppResult.Success(emptyList()))

            override fun observeMenus(categoryId: String?): Flow<AppResult<List<MenuItem>>> =
                flowOf(AppResult.Success(emptyList()))

            override suspend fun getMenu(menuItemId: String): AppResult<MenuItem> =
                AppResult.Failure(com.cafeminsu.core.DomainError.NotFound)

            override suspend fun refreshMenus(): AppResult<Unit> =
                AppResult.Success(Unit)
        }

        assertTrue(repository.refreshMenus() is AppResult.Success)
    }
}
