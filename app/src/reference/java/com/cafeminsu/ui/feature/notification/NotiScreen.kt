package com.cafeminsu.ui.feature.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafeminsu.R
import com.cafeminsu.domain.model.NotificationType
import com.cafeminsu.ui.components.CafeTopBar
import com.cafeminsu.ui.components.EmptyView
import com.cafeminsu.ui.components.ErrorView
import com.cafeminsu.ui.components.LoadingView
import com.cafeminsu.ui.theme.CafeTheme

@Composable
fun NotiRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotiViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.markAllRead()
    }

    NotiScreen(
        state = state,
        onBackClick = onBackClick,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
fun NotiScreen(
    state: NotiUiState,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = CafeTheme.colors.canvas,
        contentColor = CafeTheme.colors.body,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CafeTopBar(
                title = "알림",
                navigationIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_left),
                        contentDescription = "뒤로",
                        tint = CafeTheme.colors.ink,
                    )
                },
                onNavigationClick = onBackClick,
            )

            when (state) {
                NotiUiState.Loading -> LoadingView(
                    modifier = Modifier.padding(CafeTheme.spacing.space5),
                )

                is NotiUiState.Content -> NotiContent(state = state)
                is NotiUiState.Empty -> EmptyView(
                    message = state.message,
                    actionLabel = null,
                    onAction = null,
                    modifier = Modifier.padding(CafeTheme.spacing.space5),
                )

                is NotiUiState.Error -> ErrorView(
                    message = state.message,
                    retryable = state.retryable,
                    onRetry = onRetry,
                    modifier = Modifier.padding(CafeTheme.spacing.space5),
                )
            }
        }
    }
}

@Composable
private fun NotiContent(state: NotiUiState.Content) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = CafeTheme.spacing.space6),
    ) {
        state.groups.forEach { group ->
            item(key = "${group.label}-header") {
                GroupHeader(label = group.label)
            }
            items(
                items = group.items,
                key = { it.id },
            ) { notification ->
                NotificationRow(notification = notification)
            }
        }
    }
}

@Composable
private fun GroupHeader(label: String) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = CafeTheme.spacing.space5,
                top = CafeTheme.spacing.space5,
                end = CafeTheme.spacing.space5,
                bottom = CafeTheme.spacing.space2,
            ),
        text = label,
        style = CafeTheme.typography.caption,
        color = CafeTheme.colors.muted,
    )
}

@Composable
private fun NotificationRow(notification: NotiItemUiModel) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing
    val iconSize = spacing.space8 + spacing.space1 / IconSizeAdjustmentDivider

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.canvas),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = spacing.space5,
                    vertical = spacing.space3,
                ),
            horizontalArrangement = Arrangement.spacedBy(spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NotificationTypeIcon(type = notification.type)

            Column(
                modifier = Modifier.weight(NotificationTextWeight),
                verticalArrangement = Arrangement.spacedBy(spacing.space1),
            ) {
                Text(
                    text = notification.title,
                    style = CafeTheme.typography.bodyL,
                    color = colors.ink,
                )
                Text(
                    text = notification.body,
                    style = CafeTheme.typography.body,
                    color = colors.muted,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = notification.timeLabel,
                    style = CafeTheme.typography.meta,
                    color = colors.mutedSoft,
                )
                if (notification.unread) {
                    Surface(
                        modifier = Modifier.size(spacing.space2),
                        shape = CafeTheme.shapes.radiusPill,
                        color = colors.primary,
                        contentColor = colors.primary,
                    ) {}
                } else {
                    Spacer(modifier = Modifier.width(spacing.space2))
                }
            }
        }

        Box(
            modifier = Modifier
                .padding(start = spacing.space5 + iconSize + spacing.space3)
                .fillMaxWidth()
                .height(DividerHeight)
                .background(colors.hairline),
        )
    }
}

@Composable
private fun NotificationTypeIcon(type: NotificationType) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing
    val iconSize = spacing.space8 + spacing.space1 / IconSizeAdjustmentDivider

    Surface(
        modifier = Modifier.size(iconSize),
        shape = CircleShape,
        color = type.iconContainerColor(),
        contentColor = colors.onPrimary,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(type.iconRes()),
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(spacing.space5),
            )
        }
    }
}

@Composable
private fun NotificationType.iconContainerColor(): Color {
    val colors = CafeTheme.colors
    return when (this) {
        NotificationType.OrderAccepted,
        NotificationType.OrderReady,
        NotificationType.OrderCompleted,
        -> colors.primary

        NotificationType.StampEarned -> colors.warning
        NotificationType.GifticonReceived -> colors.success
    }
}

@androidx.annotation.DrawableRes
private fun NotificationType.iconRes(): Int =
    when (this) {
        NotificationType.OrderAccepted,
        NotificationType.OrderReady,
        NotificationType.OrderCompleted,
        -> R.drawable.ic_receipt

        NotificationType.StampEarned -> R.drawable.ic_star
        NotificationType.GifticonReceived -> R.drawable.ic_gift
    }

private val DividerHeight = androidx.compose.ui.unit.Dp.Hairline
private const val NotificationTextWeight = 1f
private const val IconSizeAdjustmentDivider = 2
