package com.cafeminsu.ui.feature.gifticon

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafeminsu.domain.model.Gifticon
import com.cafeminsu.domain.model.GifticonStatus
import com.cafeminsu.ui.components.CafeButton
import com.cafeminsu.ui.components.CafeButtonVariant
import com.cafeminsu.ui.components.CafeCard
import com.cafeminsu.ui.components.CafeCardType
import com.cafeminsu.ui.components.EmptyView
import com.cafeminsu.ui.components.ErrorView
import com.cafeminsu.ui.components.LoadingView
import com.cafeminsu.ui.theme.CafeTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun GifticonRoute(
    onGifticonClick: (String) -> Unit,
    onStampClick: () -> Unit,
    onLoginClick: () -> Unit,
    onNfcClaimClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GifticonViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    GifticonScreen(
        state = state,
        onGifticonClick = onGifticonClick,
        onStampClick = onStampClick,
        onLoginClick = onLoginClick,
        onRetry = viewModel::retry,
        onNfcClaimClick = onNfcClaimClick,
        modifier = modifier,
    )
}

@Composable
fun GifticonScreen(
    state: GifticonListUiState,
    onGifticonClick: (String) -> Unit,
    onStampClick: () -> Unit,
    onLoginClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onNfcClaimClick: () -> Unit = {},
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
                text = "기프티콘",
                style = CafeTheme.typography.h1,
                color = CafeTheme.colors.ink,
            )

            CafeButton(
                text = "NFC 쿠폰 받기",
                onClick = onNfcClaimClick,
                modifier = Modifier.fillMaxWidth(),
                variant = CafeButtonVariant.Secondary,
            )

            when (state) {
                GifticonListUiState.Loading -> LoadingView()
                is GifticonListUiState.Content -> GifticonList(
                    gifticons = state.gifticons,
                    nowMillis = nowMillis,
                    onGifticonClick = onGifticonClick,
                )

                is GifticonListUiState.Empty -> EmptyView(
                    message = state.message,
                    actionLabel = state.actionLabel,
                    onAction = onStampClick,
                )

                is GifticonListUiState.Error -> ErrorView(
                    message = state.message,
                    retryable = state.retryable,
                    onRetry = onRetry,
                )

                is GifticonListUiState.NeedsLogin -> EmptyView(
                    message = state.message,
                    actionLabel = state.actionLabel,
                    onAction = onLoginClick,
                )
            }
        }
    }
}

@Composable
private fun GifticonList(
    gifticons: List<Gifticon>,
    nowMillis: Long,
    onGifticonClick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space4)) {
        gifticons.forEach { gifticon ->
            GifticonListItem(
                gifticon = gifticon,
                nowMillis = nowMillis,
                onClick = { onGifticonClick(gifticon.id) },
            )
        }
    }
}

@Composable
private fun GifticonListItem(
    gifticon: Gifticon,
    nowMillis: Long,
    onClick: () -> Unit,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    CafeCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        type = CafeCardType.Info,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(GifticonTitleWeight),
                verticalArrangement = Arrangement.spacedBy(spacing.space1),
            ) {
                Text(
                    text = gifticon.title,
                    style = CafeTheme.typography.h3,
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

@Composable
internal fun GifticonStatusBadge(
    gifticon: Gifticon,
    nowMillis: Long,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing
    val expiringSoon = gifticon.isExpiringSoon(nowMillis)
    val label = when {
        expiringSoon -> "곧 만료"
        gifticon.status == GifticonStatus.Available -> "사용 가능"
        gifticon.status == GifticonStatus.Used -> "사용 완료"
        gifticon.status == GifticonStatus.Expired -> "만료됨"
        else -> "상태 확인"
    }
    val containerColor = when {
        expiringSoon -> colors.warning
        gifticon.status == GifticonStatus.Available -> colors.accentSoft
        gifticon.status == GifticonStatus.Used -> colors.hairline
        gifticon.status == GifticonStatus.Expired -> colors.surfaceDark
        else -> colors.surfaceCard
    }
    val contentColor = when {
        expiringSoon -> colors.onPrimary
        gifticon.status == GifticonStatus.Available -> colors.primary
        gifticon.status == GifticonStatus.Used -> colors.muted
        gifticon.status == GifticonStatus.Expired -> colors.onDark
        else -> colors.body
    }

    Surface(
        shape = CafeTheme.shapes.radiusPill,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Text(
            modifier = Modifier.padding(
                horizontal = spacing.space3,
                vertical = spacing.space1,
            ),
            text = label,
            style = CafeTheme.typography.caption,
            color = contentColor,
        )
    }
}

internal fun formatGifticonDate(expiresAtMillis: Long): String =
    Instant.ofEpochMilli(expiresAtMillis)
        .atZone(ZoneId.systemDefault())
        .format(gifticonDateFormatter)

private fun Gifticon.isExpiringSoon(nowMillis: Long): Boolean {
    val remainingMillis = expiresAtMillis - nowMillis
    return status == GifticonStatus.Available &&
        remainingMillis in 0..ExpiringSoonWindowMillis
}

private val gifticonDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREA)

private const val GifticonTitleWeight = 1f
private const val ExpiringSoonWindowMillis = 1000L * 60L * 60L * 24L * 7L
