package com.cafeminsu.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class DataUiStateTest {
    @Test
    fun exposesSharedDataUiStateContract() {
        val loading: DataUiState<String> = DataUiState.Loading
        val content = DataUiState.Content("메뉴")
        val empty = DataUiState.Empty("비어 있습니다")
        val error = DataUiState.Error("다시 시도해 주세요", retryable = true)
        val offline = DataUiState.Offline<String>(cached = null)

        assertSame(DataUiState.Loading, loading)
        assertEquals("메뉴", content.data)
        assertEquals("비어 있습니다", empty.message)
        assertEquals("다시 시도해 주세요", error.message)
        assertEquals(true, error.retryable)
        assertNull(offline.cached)
    }
}
