package com.cafeminsu.core

sealed interface DataUiState<out T> {
    data object Loading : DataUiState<Nothing>
    data class Content<T>(val data: T) : DataUiState<T>
    data class Empty(val message: String) : DataUiState<Nothing>
    data class Error(val message: String, val retryable: Boolean) : DataUiState<Nothing>
    data class Offline<T>(val cached: T?) : DataUiState<T>
}
