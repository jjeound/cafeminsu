package com.cafeminsu.ui.feature.gifticon

import com.cafeminsu.domain.model.Gifticon
import com.cafeminsu.domain.model.GifticonStatus

sealed interface GifticonListUiState {
    data object Loading : GifticonListUiState

    data class Content(
        val gifticons: List<Gifticon>,
    ) : GifticonListUiState

    data class Empty(
        val message: String,
        val actionLabel: String,
    ) : GifticonListUiState

    data class Error(
        val message: String,
        val retryable: Boolean,
    ) : GifticonListUiState

    data class NeedsLogin(
        val message: String,
        val actionLabel: String,
    ) : GifticonListUiState
}

sealed interface GifticonDetailUiState {
    data object Loading : GifticonDetailUiState

    data class Content(
        val gifticon: Gifticon,
        val message: String? = null,
    ) : GifticonDetailUiState {
        val canUse: Boolean = gifticon.status == GifticonStatus.Available
    }

    data class Error(
        val message: String,
        val retryable: Boolean,
    ) : GifticonDetailUiState

    data class NeedsLogin(
        val message: String,
        val actionLabel: String,
    ) : GifticonDetailUiState
}
