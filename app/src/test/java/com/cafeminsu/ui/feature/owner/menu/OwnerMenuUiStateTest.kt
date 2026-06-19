package com.cafeminsu.ui.feature.owner.menu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnerMenuUiStateTest {
    @Test
    fun exposesOwnerMenuUiModels() {
        val filters = OwnerMenuFilter.entries.map { filter ->
            OwnerMenuFilterUiModel(
                filter = filter,
                label = filter.label,
                selected = filter == OwnerMenuFilter.All,
            )
        }
        val menu = OwnerMenuItemUiModel(
            id = "americano",
            name = "아메리카노",
            price = 4_500,
            isSoldOut = false,
            statusLabel = "판매중",
            isDimmed = false,
            isActionInProgress = false,
        )
        val state = OwnerMenuUiState.Content(
            selectedFilter = OwnerMenuFilter.All,
            filters = filters,
            menus = listOf(menu),
        )

        assertEquals(OwnerMenuFilter.All, state.selectedFilter)
        assertEquals("전체", state.filters.first().label)
        assertTrue(state.filters.first().selected)
        assertEquals("아메리카노", state.menus.single().name)
        assertEquals("판매중", state.menus.single().statusLabel)
        assertFalse(state.menus.single().isDimmed)
    }
}
