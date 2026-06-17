package com.cafeminsu.ui.feature.home

import androidx.lifecycle.ViewModel
import com.cafeminsu.data.repository.MockMenuRepository
import com.cafeminsu.data.repository.MockSessionRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeViewModelTest {
    @Test
    fun homeViewModelAcceptsRepositoryContracts() {
        val viewModel = HomeViewModel(
            menuRepository = MockMenuRepository(),
            sessionRepository = MockSessionRepository(),
        )

        assertEquals(ViewModel::class.java, viewModel::class.java.superclass)
    }
}
