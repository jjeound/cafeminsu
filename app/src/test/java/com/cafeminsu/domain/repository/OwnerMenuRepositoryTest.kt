package com.cafeminsu.domain.repository

import app.cash.turbine.test
import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.model.NewMenuDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnerMenuRepositoryTest {
    @Test
    fun ownerMenuRepositoryContractObservesManagedMenusByCategory() = runBlocking {
        val repository = FakeOwnerMenuRepository(
            initialMenus = listOf(
                sampleOwnerMenu(id = "americano", categoryId = "coffee"),
                sampleOwnerMenu(id = "cookie", categoryId = "dessert"),
            ),
        )

        repository.observeManagedMenus("coffee").test {
            assertEquals(listOf("americano"), awaitItem().successData().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun ownerMenuRepositoryContractUpdatesSoldOutAndVisible() = runBlocking {
        val repository = FakeOwnerMenuRepository(
            initialMenus = listOf(sampleOwnerMenu(id = "americano", categoryId = "coffee")),
        )

        repository.observeManagedMenus().test {
            assertFalse(awaitItem().successData().single().isSoldOut)

            val soldOut = repository.setSoldOut("americano", true).successData()
            assertTrue(soldOut.isSoldOut)
            assertTrue(awaitItem().successData().single().isSoldOut)

            val hidden = repository.setVisible("americano", false).successData()
            assertFalse(hidden.isVisible)
            assertTrue(awaitItem().successData().isEmpty())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun ownerMenuRepositoryContractAddsMenu() = runBlocking {
        val repository = FakeOwnerMenuRepository(initialMenus = emptyList())

        repository.observeManagedMenus("coffee").test {
            assertTrue(awaitItem().successData().isEmpty())

            val created = repository.addMenu(
                NewMenuDraft(
                    name = "콜드브루",
                    categoryId = "coffee",
                    basePrice = 5_500,
                    description = "",
                    imageUrl = null,
                    isSoldOut = false,
                ),
            ).successData()

            assertEquals("콜드브루", created.name)
            assertEquals(listOf("콜드브루"), awaitItem().successData().map { it.name })

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> AppResult<T>.successData(): T {
        assertTrue(this is AppResult.Success<*>)
        return (this as AppResult.Success<T>).data
    }
}

private class FakeOwnerMenuRepository(
    initialMenus: List<MenuItem>,
) : OwnerMenuRepository {
    private val menus = MutableStateFlow(initialMenus)

    override fun observeManagedMenus(categoryId: String?): Flow<AppResult<List<MenuItem>>> =
        menus.map { currentMenus ->
            AppResult.Success(
                currentMenus
                    .filter { it.isVisible }
                    .filter { categoryId == null || it.categoryId == categoryId },
            )
        }

    override suspend fun setSoldOut(menuItemId: String, soldOut: Boolean): AppResult<MenuItem> {
        val updated = menus.value.first { it.id == menuItemId }.copy(isSoldOut = soldOut)
        menus.value = menus.value.map { menu ->
            if (menu.id == menuItemId) updated else menu
        }
        return AppResult.Success(updated)
    }

    override suspend fun setVisible(menuItemId: String, visible: Boolean): AppResult<MenuItem> {
        val updated = menus.value.first { it.id == menuItemId }.copy(isVisible = visible)
        menus.value = menus.value.map { menu ->
            if (menu.id == menuItemId) updated else menu
        }
        return AppResult.Success(updated)
    }

    override suspend fun addMenu(draft: NewMenuDraft): AppResult<MenuItem> {
        val created = MenuItem(
            id = "menu-${menus.value.size}",
            categoryId = draft.categoryId,
            name = draft.name,
            description = draft.description,
            basePrice = draft.basePrice,
            imageUrl = draft.imageUrl,
            isSoldOut = draft.isSoldOut,
            options = emptyList(),
            isVisible = true,
        )
        menus.value = menus.value + created
        return AppResult.Success(created)
    }
}

private fun sampleOwnerMenu(
    id: String,
    categoryId: String,
): MenuItem =
    MenuItem(
        id = id,
        categoryId = categoryId,
        name = "아메리카노",
        description = "",
        basePrice = 4_500,
        imageUrl = null,
        isSoldOut = false,
        options = emptyList(),
    )
