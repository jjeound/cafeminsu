package com.cafeminsu.core.model.media

sealed interface ImageSource {
    data object None : ImageSource

    data class Remote(val url: String) : ImageSource

    data class Local(val uri: String) : ImageSource
}
