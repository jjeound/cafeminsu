package com.cafeminsu.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import com.cafeminsu.R
import com.cafeminsu.core.DataUiState
import com.cafeminsu.ui.theme.CafeTheme

@Composable
fun LoadingView(
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Box(
        modifier = modifier
            .testTag(LoadingViewTestTag)
            .fillMaxWidth()
            .heightIn(min = spacing.space18 * LoadingViewMinHeightUnits)
            .background(colors.canvas)
            .padding(spacing.space5),
        contentAlignment = Alignment.Center,
    ) {
        LoadingArcSpinner(
            modifier = Modifier.size(spacing.space18 + spacing.space2),
        )
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
    message: String = DefaultErrorMessage,
    retryable: Boolean = true,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier,
    title: String = DefaultErrorTitle,
    errorCode: String? = DefaultNetworkErrorCode,
    showTopBar: Boolean = false,
    onBackClick: (() -> Unit)? = null,
    onContactSupport: (() -> Unit)? = null,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = spacing.space18 * ErrorViewMinHeightUnits),
        color = colors.canvas,
        contentColor = colors.body,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (showTopBar) {
                CafeTopBar(
                    title = "오류",
                    navigationIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_left),
                            contentDescription = null,
                            tint = CafeTheme.colors.ink,
                        )
                    },
                    onNavigationClick = onBackClick,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.space5),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(
                    modifier = Modifier.height(
                        if (showTopBar) {
                            spacing.space18 * ErrorTopBarContentSpacerUnits + spacing.space5
                        } else {
                            spacing.space18 * ErrorContentTopSpacerUnits
                        },
                    ),
                )

                ErrorNetworkIcon()
                Spacer(modifier = Modifier.height(spacing.space8))

                Text(
                    text = title,
                    style = CafeTheme.typography.h2,
                    color = colors.ink,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(spacing.space2))
                Text(
                    text = message,
                    style = CafeTheme.typography.body,
                    color = colors.muted,
                    textAlign = TextAlign.Center,
                )

                if (errorCode != null) {
                    Spacer(modifier = Modifier.height(spacing.space5))
                    Surface(
                        shape = CafeTheme.shapes.radiusPill,
                        color = colors.surfaceCard,
                        contentColor = colors.muted,
                    ) {
                        Text(
                            modifier = Modifier.padding(
                                horizontal = spacing.space2,
                                vertical = spacing.space1,
                            ),
                            text = errorCode,
                            style = CafeTheme.typography.meta,
                            color = colors.muted,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(spacing.space18 * ErrorActionTopSpacerUnits))

                if (retryable) {
                    CafeButton(
                        text = "다시 시도",
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(modifier = Modifier.height(spacing.space4))
                TextButton(
                    onClick = { onContactSupport?.invoke() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colors.muted,
                    ),
                ) {
                    Text(
                        text = "고객센터 문의",
                        style = CafeTheme.typography.body,
                        color = colors.muted,
                    )
                }
            }
        }
    }
}

@Composable
fun LogoutConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.widthIn(min = spacing.space18 * LogoutDialogMinWidthUnits),
            shape = CafeTheme.shapes.radiusXl,
            color = colors.canvas,
            contentColor = colors.body,
        ) {
            Column(
                modifier = Modifier.padding(
                    start = spacing.space4,
                    top = spacing.space10,
                    end = spacing.space4,
                    bottom = spacing.space4,
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.space8),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(spacing.space3),
                ) {
                    Text(
                        text = "로그아웃 하시겠어요?",
                        style = CafeTheme.typography.h2,
                        color = colors.ink,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "로그인 정보를 잊지 않도록\n계정 정보를 확인해주세요.",
                        style = CafeTheme.typography.body,
                        color = colors.muted,
                        textAlign = TextAlign.Center,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.space1),
                ) {
                    CafeButton(
                        text = "취소",
                        onClick = onDismiss,
                        modifier = Modifier.weight(DialogButtonWeight),
                        variant = CafeButtonVariant.Secondary,
                    )
                    CafeButton(
                        text = "로그아웃",
                        onClick = onConfirm,
                        modifier = Modifier.weight(DialogButtonWeight),
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingArcSpinner(
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val infiniteTransition = rememberInfiniteTransition(label = "loadingArcTransition")
    val rotation by infiniteTransition.animateFloat(
        initialValue = LoadingArcStartRotation,
        targetValue = LoadingArcEndRotation,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = LoadingArcRotationMillis,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "loadingArcRotation",
    )

    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension / LoadingArcStrokeDivisor
        val arcSize = Size(
            width = size.width - strokeWidth,
            height = size.height - strokeWidth,
        )
        val topLeft = Offset(
            x = strokeWidth / LoadingArcOffsetDivisor,
            y = strokeWidth / LoadingArcOffsetDivisor,
        )

        drawArc(
            color = colors.accentSoft.copy(alpha = LoadingArcTrackAlpha),
            startAngle = LoadingArcTrackStartAngle + rotation,
            sweepAngle = LoadingArcTrackSweepAngle,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
        drawArc(
            color = colors.primary,
            startAngle = LoadingArcPrimaryStartAngle + rotation,
            sweepAngle = LoadingArcPrimarySweepAngle,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun ErrorNetworkIcon() {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Surface(
        modifier = Modifier.size(spacing.space18 * ErrorIconSizeUnits),
        shape = CafeTheme.shapes.radiusPill,
        color = colors.accentSoft.copy(alpha = ErrorIconBackgroundAlpha),
        contentColor = colors.error,
    ) {
        Box(contentAlignment = Alignment.Center) {
            WifiOffGlyph(
                modifier = Modifier.size(spacing.space14),
            )
        }
    }
}

@Composable
private fun WifiOffGlyph(
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors

    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension / WifiStrokeDivisor
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        val center = Offset(
            x = size.width / WifiCenterDivisor,
            y = size.height * WifiCenterYFraction,
        )

        fun drawWifiArc(widthFraction: Float, heightFraction: Float, topFraction: Float) {
            val arcWidth = size.width * widthFraction
            val arcHeight = size.height * heightFraction
            drawArc(
                color = colors.error,
                startAngle = WifiArcStartAngle,
                sweepAngle = WifiArcSweepAngle,
                useCenter = false,
                topLeft = Offset(
                    x = center.x - arcWidth / WifiCenterDivisor,
                    y = size.height * topFraction,
                ),
                size = Size(arcWidth, arcHeight),
                style = stroke,
            )
        }

        drawWifiArc(WifiOuterWidthFraction, WifiOuterHeightFraction, WifiOuterTopFraction)
        drawWifiArc(WifiMiddleWidthFraction, WifiMiddleHeightFraction, WifiMiddleTopFraction)
        drawWifiArc(WifiInnerWidthFraction, WifiInnerHeightFraction, WifiInnerTopFraction)
        drawLine(
            color = colors.error,
            start = Offset(size.width * WifiSlashStartXFraction, size.height * WifiSlashStartYFraction),
            end = Offset(size.width * WifiSlashEndXFraction, size.height * WifiSlashEndYFraction),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
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

private const val LoadingViewTestTag = "loading_view"
private const val DefaultErrorTitle = "연결에 실패했어요"
private const val DefaultErrorMessage = "네트워크 상태를 확인하고\n다시 시도해주세요."
private const val DefaultNetworkErrorCode = "ERR_NETWORK_408"
private const val LoadingViewMinHeightUnits = 10
private const val LoadingArcStartRotation = 0f
private const val LoadingArcEndRotation = 360f
private const val LoadingArcRotationMillis = 1_200
private const val LoadingArcStrokeDivisor = 16f
private const val LoadingArcOffsetDivisor = 2f
private const val LoadingArcTrackAlpha = 0.56f
private const val LoadingArcTrackStartAngle = 116f
private const val LoadingArcTrackSweepAngle = 300f
private const val LoadingArcPrimaryStartAngle = 126f
private const val LoadingArcPrimarySweepAngle = 246f
private const val ErrorViewMinHeightUnits = 10
private const val ErrorTopBarContentSpacerUnits = 2
private const val ErrorContentTopSpacerUnits = 2
private const val ErrorActionTopSpacerUnits = 2
private const val ErrorIconSizeUnits = 2
private const val ErrorIconBackgroundAlpha = 0.5f
private const val WifiStrokeDivisor = 18f
private const val WifiCenterDivisor = 2f
private const val WifiCenterYFraction = 0.82f
private const val WifiArcStartAngle = 210f
private const val WifiArcSweepAngle = 120f
private const val WifiOuterWidthFraction = 0.88f
private const val WifiOuterHeightFraction = 0.72f
private const val WifiOuterTopFraction = 0.06f
private const val WifiMiddleWidthFraction = 0.62f
private const val WifiMiddleHeightFraction = 0.5f
private const val WifiMiddleTopFraction = 0.28f
private const val WifiInnerWidthFraction = 0.36f
private const val WifiInnerHeightFraction = 0.28f
private const val WifiInnerTopFraction = 0.5f
private const val WifiSlashStartXFraction = 0.18f
private const val WifiSlashStartYFraction = 0.18f
private const val WifiSlashEndXFraction = 0.82f
private const val WifiSlashEndYFraction = 0.82f
private const val LogoutDialogMinWidthUnits = 4
private const val DialogButtonWeight = 1f
private const val SnackbarMessageWeight = 1f
private const val BorderWidthDivider = 4
