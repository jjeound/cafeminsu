package com.cafeminsu.data.local.menu

import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.model.MenuOption
import com.cafeminsu.domain.model.MenuOptionGroup
import com.squareup.moshi.Moshi
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomMenuLocalDataSourceTest {
    private val dao = mockk<MenuDao>(relaxed = true)
    private val moshi = Moshi.Builder().build()
    private val dataSource = RoomMenuLocalDataSource(dao, moshi)

    @Test
    fun cachedMenusMapsDaoEntitiesToDomainIncludingOptions() = runTest {
        val menu = sampleMenu()
        coEvery { dao.byStore("11") } returns listOf(menu.toMenuEntity("11", moshi))

        assertEquals(listOf(menu), dataSource.cachedMenus("11"))
    }

    @Test
    fun replaceMenusClearsStoreThenUpsertsMappedEntities() = runTest {
        val captured = slot<List<MenuEntity>>()
        coEvery { dao.upsertAll(capture(captured)) } returns Unit

        dataSource.replaceMenus("11", listOf(sampleMenu()))

        // 사라진 메뉴가 남지 않도록 해당 매장만 비운 뒤 채우는 순서를 보장한다.
        coVerifyOrder {
            dao.clearStore("11")
            dao.upsertAll(any())
        }
        assertEquals("101", captured.captured.single().id)
        assertEquals("11", captured.captured.single().storeId)
    }

    private fun sampleMenu(): MenuItem =
        MenuItem(
            id = "101",
            categoryId = "커피",
            name = "바닐라라떼",
            description = "부드러운 라떼",
            basePrice = 5_500,
            imageUrl = "https://cdn.example/latte.png",
            isSoldOut = false,
            options = listOf(
                MenuOptionGroup(
                    id = "온도",
                    name = "온도",
                    required = true,
                    minSelect = 1,
                    maxSelect = 1,
                    options = listOf(
                        MenuOption(id = "1", name = "HOT", extraPrice = 0, isAvailable = true),
                    ),
                ),
            ),
            isVisible = true,
        )
}
