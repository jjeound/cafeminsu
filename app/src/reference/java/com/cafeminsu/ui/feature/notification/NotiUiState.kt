package com.cafeminsu.ui.feature.notification

import com.cafeminsu.domain.model.NotificationType

sealed interface NotiUiState {
    data object Loading : NotiUiState

    data class Content(
        val groups: List<NotiGroupUiModel>,
    ) : NotiUiState

    data class Empty(
        val message: String,
    ) : NotiUiState

    data class Error(
        val message: String,
        val retryable: Boolean,
    ) : NotiUiState
}

data class NotiGroupUiModel(
    val label: String,
    val items: List<NotiItemUiModel>,
)

data class NotiItemUiModel(
    val id: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val timeLabel: String,
    val unread: Boolean,
)
