package com.cafeminsu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.cafeminsu.core.DataUiState
import com.cafeminsu.ui.theme.CafeTheme

@Composable
fun LoadingView(
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(spacing.space5),
        verticalArrangement = Arrangement.spacedBy(spacing.space3),
    ) {
        Text(
            text = "불러오는 중",
            style = CafeTheme.typography.body,
            color = colors.muted,
        )

        repeat(LoadingPlaceholderRows) { index ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth(
                        if (index == LoadingPrimaryRowIndex) {
                            LoadingPrimaryWidthFraction
                        } else {
                            LoadingSecondaryWidthFraction
                        },
                    )
                    .height(spacing.space4),
                shape = CafeTheme.shapes.radiusMd,
                color = colors.surfaceCard,
                contentColor = colors.surfaceCard,
            ) {}
        }
    }
}

@Composable
fun EmptyView(
    message: String,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    CafeCard(
        modifier = modifier.fillMaxWidth(),
        type = CafeCardType.Info,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.space4),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = message,
                style = CafeTheme.typography.body,
                color = colors.muted,
            )

            if (actionLabel != null && onAction != null) {
                CafeButton(
                    text = actionLabel,
                    onClick = onAction,
                    variant = CafeButtonVariant.Secondary,
                )
            }
        }
    }
}

@Composable
fun ErrorView(
    message: String,
    retryable: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CafeTheme.shapes.radiusLg,
        color = colors.canvas,
        contentColor = colors.body,
        border = BorderStroke(spacing.space1 / BorderWidthDivider, colors.error),
    ) {
        Column(
            modifier = Modifier.padding(spacing.space5),
            verticalArrangement = Arrangement.spacedBy(spacing.space4),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.space3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ErrorIcon()

                Column(
                    verticalArrangement = Arrangement.spacedBy(spacing.space1),
                ) {
                    Text(
                        text = "문제가 생겼어요",
                        style = CafeTheme.typography.h3,
                        color = colors.ink,
                    )
                    Text(
                        text = message,
                        style = CafeTheme.typography.body,
                        color = colors.body,
                    )
                }
            }

            if (retryable) {
                CafeButton(
                    text = "다시 시도",
                    onClick = onRetry,
                    variant = CafeButtonVariant.Secondary,
                )
            }
        }
    }
}

@Composable
fun OfflineBanner(
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CafeTheme.shapes.radiusMd,
        color = colors.surfaceCard,
        contentColor = colors.body,
        border = BorderStroke(spacing.space1 / BorderWidthDivider, colors.warning),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = spacing.space4,
                vertical = spacing.space3,
            ),
            horizontalArrangement = Arrangement.spacedBy(spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(color = colors.warning)

            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.space1),
            ) {
                Text(
                    text = "오프라인 상태입니다",
                    style = CafeTheme.typography.h3,
                    color = colors.ink,
                )
                Text(
                    text = "저장된 내용을 읽기 전용으로 표시합니다.",
                    style = CafeTheme.typography.body,
                    color = colors.body,
                )
            }
        }
    }
}

enum class CafeSnackbarType {
    Success,
    Error,
    Warning,
}

@Composable
fun CafeSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
    ) { snackbarData ->
        val cafeVisuals = snackbarData.visuals as? CafeSnackbarVisuals
        val type = cafeVisuals?.type ?: CafeSnackbarType.Success

        Surface(
            shape = CafeTheme.shapes.radiusMd,
            color = colors.surfaceDark,
            contentColor = colors.onDark,
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = spacing.space4,
                    vertical = spacing.space3,
                ),
                horizontalArrangement = Arrangement.spacedBy(spacing.space3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusDot(color = snackbarColor(type))

                Text(
                    modifier = Modifier.weight(SnackbarMessageWeight),
                    text = snackbarData.visuals.message,
                    style = CafeTheme.typography.body,
                    color = colors.onDark,
                )

                snackbarData.visuals.actionLabel?.let { actionLabel ->
                    TextButton(
                        onClick = snackbarData::performAction,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = colors.onDark,
                        ),
                    ) {
                        Text(
                            text = actionLabel,
                            style = CafeTheme.typography.bodyL,
                            color = colors.onDark,
                        )
                    }
                }

                if (snackbarData.visuals.withDismissAction) {
                    TextButton(
                        onClick = snackbarData::dismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = colors.onDark,
                        ),
                    ) {
                        Text(
                            text = "닫기",
                            style = CafeTheme.typography.bodyL,
                            color = colors.onDark,
                        )
                    }
                }
            }
        }
    }
}

suspend fun SnackbarHostState.cafeSnackbar(
    message: String,
    type: CafeSnackbarType = CafeSnackbarType.Success,
    actionLabel: String? = null,
    duration: SnackbarDuration = SnackbarDuration.Short,
    withDismissAction: Boolean = false,
): SnackbarResult = showSnackbar(
    CafeSnackbarVisuals(
        message = message,
        type = type,
        actionLabel = actionLabel,
        duration = duration,
        withDismissAction = withDismissAction,
    ),
)

@Composable
fun <T> DataUiStateContent(
    state: DataUiState<T>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    emptyActionLabel: String? = null,
    onEmptyAction: (() -> Unit)? = null,
    content: @Composable (T) -> Unit,
) {
    when (state) {
        DataUiState.Loading -> LoadingView(modifier = modifier)
        is DataUiState.Content -> Box(modifier = modifier) {
            content(state.data)
        }

        is DataUiState.Empty -> EmptyView(
            message = state.message,
            actionLabel = emptyActionLabel,
            onAction = onEmptyAction,
            modifier = modifier,
        )

        is DataUiState.Error -> ErrorView(
            message = state.message,
            retryable = state.retryable,
            onRetry = onRetry,
            modifier = modifier,
        )

        is DataUiState.Offline -> OfflineContent(
            cached = state.cached,
            modifier = modifier,
            content = content,
        )
    }
}

private data class CafeSnackbarVisuals(
    override val message: String,
    val type: CafeSnackbarType,
    override val actionLabel: String?,
    override val duration: SnackbarDuration,
    override val withDismissAction: Boolean,
) : SnackbarVisuals

@Composable
private fun <T> OfflineContent(
    cached: T?,
    modifier: Modifier,
    content: @Composable (T) -> Unit,
) {
    val spacing = CafeTheme.spacing

    Column(modifier = modifier.fillMaxWidth()) {
        OfflineBanner()
        Spacer(modifier = Modifier.height(spacing.space4))

        if (cached != null) {
            content(cached)
        } else {
            EmptyView(
                message = "저장된 데이터가 없습니다",
                actionLabel = null,
                onAction = null,
            )
        }
    }
}

@Composable
private fun ErrorIcon() {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Surface(
        modifier = Modifier.size(spacing.space6),
        shape = CafeTheme.shapes.radiusPill,
        color = colors.error,
        contentColor = colors.onPrimary,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "!",
                style = CafeTheme.typography.bodyL,
                color = colors.onPrimary,
            )
        }
    }
}

@Composable
private fun StatusDot(
    color: Color,
) {
    val spacing = CafeTheme.spacing

    Surface(
        modifier = Modifier.size(spacing.space3),
        shape = CafeTheme.shapes.radiusPill,
        color = color,
        contentColor = color,
    ) {}
}

@Composable
private fun snackbarColor(type: CafeSnackbarType): Color {
    val colors = CafeTheme.colors

    return when (type) {
        CafeSnackbarType.Success -> colors.success
        CafeSnackbarType.Error -> colors.error
        CafeSnackbarType.Warning -> colors.warning
    }
}

private const val LoadingPlaceholderRows = 3
private const val LoadingPrimaryRowIndex = 0
private const val LoadingPrimaryWidthFraction = 0.76f
private const val LoadingSecondaryWidthFraction = 0.54f
private const val SnackbarMessageWeight = 1f
private const val BorderWidthDivider = 4
