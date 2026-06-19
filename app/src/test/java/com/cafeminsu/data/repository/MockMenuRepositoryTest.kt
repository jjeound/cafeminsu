package com.cafeminsu.data.repository

import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockMenuRepositoryTest {
    @Test
    fun observeCategoriesEmitsSortedNonEmptySuccess() = runBlocking {
        val repository = MockMenuRepository()

        repository.observeCategories().test {
            val result = awaitItem()

            val categories = result.successData()
            assertTrue(categories.isNotEmpty())
            assertEquals(categories.sortedBy { it.sortOrder }, categories)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeMenusFiltersByCategory() = runBlocking {
        val repository = MockMenuRepository()

        repository.observeCategories().test {
            val category = awaitItem().successData().first()

            repository.observeMenus(category.id).test {
                val menus = awaitItem().successData()

                assertTrue(menus.isNotEmpty())
                assertTrue(menus.all { it.categoryId == category.id })
                cancelAndIgnoreRemainingEvents()
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getMenuReturnsNotFoundForMissingMenu() = runBlocking {
        val repository = MockMenuRepository()

        val result = repository.getMenu("missing-menu")

        assertEquals(AppResult.Failure(DomainError.NotFound), result)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> AppResult<T>.successData(): T {
        assertTrue(this is AppResult.Success<*>)
        return (this as AppResult.Success<T>).data
    }
}
