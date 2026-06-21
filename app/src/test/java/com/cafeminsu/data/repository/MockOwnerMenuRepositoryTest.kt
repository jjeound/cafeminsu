package com.cafeminsu.data.repository

import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.model.NewMenuDraft
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockOwnerMenuRepositoryTest {
    @Test
    fun observeManagedMenusFiltersVisibleMenusByCategory() = runBlocking {
        val repository = MockOwnerMenuRepository(
            menuItems = listOf(
                sampleMenu(id = "americano", categoryId = "coffee"),
                sampleMenu(id = "latte", categoryId = "noncoffee"),
                sampleMenu(id = "hidden-cookie", categoryId = "dessert", isVisible = false),
            ),
        )

        repository.observeManagedMenus("coffee").test {
            val menus = awaitItem().successData()

            assertEquals(listOf("americano"), menus.map { it.id })
            assertTrue(menus.all { it.categoryId == "coffee" })

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setSoldOutUpdatesObservedMenus() = runBlocking {
        val repository = MockOwnerMenuRepository(
            menuItems = listOf(sampleMenu(id = "americano", categoryId = "coffee")),
        )

        repository.observeManagedMenus().test {
            assertFalse(awaitItem().successData().single().isSoldOut)

            val result = repository.setSoldOut("americano", true)

            assertTrue(result.successData().isSoldOut)
            assertTrue(awaitItem().successData().single().isSoldOut)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setVisibleHidesMenuFromObservedMenus() = runBlocking {
        val repository = MockOwnerMenuRepository(
            menuItems = listOf(sampleMenu(id = "americano", categoryId = "coffee")),
        )

        repository.observeManagedMenus().test {
            assertEquals(listOf("americano"), awaitItem().successData().map { it.id })

            val result = repository.setVisible("americano", false)

            assertFalse(result.successData().isVisible)
            assertTrue(awaitItem().successData().isEmpty())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun updatingMissingMenuReturnsNotFound() = runBlocking {
        val repository = MockOwnerMenuRepository(
            menuItems = listOf(sampleMenu(id = "americano", categoryId = "coffee")),
        )

        val soldOutResult = repository.setSoldOut("missing-menu", true)
        val visibleResult = repository.setVisible("missing-menu", false)

        assertEquals(AppResult.Failure(DomainError.NotFound), soldOutResult)
        assertEquals(AppResult.Failure(DomainError.NotFound), visibleResult)
    }

    @Test
    fun addMenuInsertsVisibleMenuIntoObservedMenus() = runBlocking {
        val repository = MockOwnerMenuRepository(menuItems = emptyList())

        repository.observeManagedMenus("dessert").test {
            assertTrue(awaitItem().successData().isEmpty())

            val result = repository.addMenu(
                NewMenuDraft(
                    name = "티라미수",
                    categoryId = "dessert",
                    basePrice = 6_500,
                    description = "마스카포네 듬뿍",
                    imageUrl = "content://img/1",
                    isSoldOut = false,
                ),
            )

            val created = result.successData()
            assertNotNull(created.id)
            assertTrue(created.id.isNotBlank())
            assertEquals("티라미수", created.name)
            assertEquals("dessert", created.categoryId)
            assertEquals(6_500, created.basePrice)
            assertEquals("마스카포네 듬뿍", created.description)
            assertEquals("content://img/1", created.imageUrl)
            assertFalse(created.isSoldOut)
            assertTrue(created.isVisible)

            assertEquals(listOf("티라미수"), awaitItem().successData().map { it.name })

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun addMenuRespectsSoldOutDraft() = runBlocking {
        val repository = MockOwnerMenuRepository(menuItems = emptyList())

        val created = repository.addMenu(
            NewMenuDraft(
                name = "콜드브루",
                categoryId = "coffee",
                basePrice = 5_500,
                description = "",
                imageUrl = null,
                isSoldOut = true,
            ),
        ).successData()

        assertTrue(created.isSoldOut)
        assertTrue(created.options.isEmpty())
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> AppResult<T>.successData(): T {
        assertTrue(this is AppResult.Success<*>)
        return (this as AppResult.Success<T>).data
    }
}

private fun sampleMenu(
    id: String,
    categoryId: String,
    isSoldOut: Boolean = false,
    isVisible: Boolean = true,
): MenuItem =
    MenuItem(
        id = id,
        categoryId = categoryId,
        name = "아메리카노",
        description = "",
        basePrice = 4_500,
        imageUrl = null,
        isSoldOut = isSoldOut,
        options = emptyList(),
        isVisible = isVisible,
    )
