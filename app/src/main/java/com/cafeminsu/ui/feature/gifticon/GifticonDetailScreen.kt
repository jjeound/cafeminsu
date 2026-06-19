package com.cafeminsu.ui.feature.gifticon

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafeminsu.domain.model.Gifticon
import com.cafeminsu.ui.components.CafeButton
import com.cafeminsu.ui.components.CafeCard
import com.cafeminsu.ui.components.CafeCardType
import com.cafeminsu.ui.components.EmptyView
import com.cafeminsu.ui.components.ErrorView
import com.cafeminsu.ui.components.LoadingView
import com.cafeminsu.ui.theme.CafeTheme

@Composable
fun GifticonDetailRoute(
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GifticonDetailViewModel = hiltViewModel(),
) {
    SecureGifticonWindow()
    val state by viewModel.uiState.collectAsState()

    GifticonDetailScreen(
        state = state,
        onUse = viewModel::onUse,
        onLoginClick = onLoginClick,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
fun GifticonDetailScreen(
    state: GifticonDetailUiState,
    onUse: () -> Unit,
    onLoginClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    nowMillis: Long = System.currentTimeMillis(),
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = CafeTheme.colors.canvas,
        contentColor = CafeTheme.colors.body,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = CafeTheme.spacing.space5,
                    top = CafeTheme.spacing.space6,
                    end = CafeTheme.spacing.space5,
                    bottom = CafeTheme.spacing.space6,
                ),
            verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space5),
        ) {
            Text(
                text = "기프티콘 상세",
                style = CafeTheme.typography.h1,
                color = CafeTheme.colors.ink,
            )

            when (state) {
                GifticonDetailUiState.Loading -> LoadingView()
                is GifticonDetailUiState.Content -> GifticonDetailContent(
                    state = state,
                    nowMillis = nowMillis,
                    onUse = onUse,
                )

                is GifticonDetailUiState.Error -> ErrorView(
                    message = state.message,
                    retryable = state.retryable,
                    onRetry = onRetry,
                )

                is GifticonDetailUiState.NeedsLogin -> EmptyView(
                    message = state.message,
                    actionLabel = state.actionLabel,
                    onAction = onLoginClick,
                )
            }
        }
    }
}

@Composable
private fun GifticonDetailContent(
    state: GifticonDetailUiState.Content,
    nowMillis: Long,
    onUse: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space5)) {
        GifticonSummaryCard(
            gifticon = state.gifticon,
            nowMillis = nowMillis,
        )
        GifticonCodeCard(gifticon = state.gifticon)

        state.message?.let { message ->
            CafeCard(
                modifier = Modifier.fillMaxWidth(),
                type = CafeCardType.Info,
            ) {
                Text(
                    text = message,
                    style = CafeTheme.typography.body,
                    color = CafeTheme.colors.success,
                )
            }
        }

        CafeButton(
            text = "사용하기",
            onClick = onUse,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.canUse,
        )
    }
}

@Composable
private fun GifticonSummaryCard(
    gifticon: Gifticon,
    nowMillis: Long,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    CafeCard(
        modifier = Modifier.fillMaxWidth(),
        type = CafeCardType.Default,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier.weight(DetailTitleWeight),
                    verticalArrangement = Arrangement.spacedBy(spacing.space1),
                ) {
                    Text(
                        text = gifticon.title,
                        style = CafeTheme.typography.h2,
                        color = colors.ink,
                    )
                    Text(
                        text = "만료일 ${formatGifticonDate(gifticon.expiresAtMillis)}",
                        style = CafeTheme.typography.caption,
                        color = colors.muted,
                    )
                }

                GifticonStatusBadge(
                    gifticon = gifticon,
                    nowMillis = nowMillis,
                )
            }
        }
    }
}

@Composable
private fun GifticonCodeCard(gifticon: Gifticon) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    CafeCard(
        modifier = Modifier.fillMaxWidth(),
        type = CafeCardType.Info,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
            Text(
                text = "매장 제시용 코드",
                style = CafeTheme.typography.h2,
                color = colors.ink,
            )

            GifticonCodeBlock(
                label = "바코드",
                value = gifticon.barcodeValue,
            )
            GifticonCodeBlock(
                label = "QR",
                value = gifticon.qrValue,
            )
        }
    }
}

@Composable
private fun GifticonCodeBlock(
    label: String,
    value: String,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CafeTheme.shapes.radiusMd,
        color = colors.surfaceCard,
        contentColor = colors.body,
        border = BorderStroke(spacing.space1 / BorderWidthDivider, colors.hairline),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = spacing.space4,
                vertical = spacing.space3,
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.space2),
        ) {
            Text(
                text = label,
                style = CafeTheme.typography.h3,
                color = colors.ink,
            )
            Text(
                text = value,
                style = CafeTheme.typography.bodyL,
                color = colors.body,
            )
        }
    }
}

@Composable
private fun SecureGifticonWindow() {
    val context = LocalContext.current

    DisposableEffect(context) {
        val window = context.findActivity()?.window
        window?.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )

        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private const val BorderWidthDivider = 4
private const val DetailTitleWeight = 1f
