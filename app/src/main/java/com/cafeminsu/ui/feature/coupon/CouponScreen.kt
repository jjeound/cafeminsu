package com.cafeminsu.ui.feature.coupon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafeminsu.R
import com.cafeminsu.ui.components.CafeButton
import com.cafeminsu.ui.components.CafeButtonVariant
import com.cafeminsu.ui.components.CafeCard
import com.cafeminsu.ui.components.CafeCardType
import com.cafeminsu.ui.components.CafeTopBar
import com.cafeminsu.ui.components.EmptyView
import com.cafeminsu.ui.components.ErrorView
import com.cafeminsu.ui.components.LoadingView
import com.cafeminsu.ui.theme.CafeTheme

@Composable
fun CouponRoute(
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit,
    onNfcClaimClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CouponViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    CouponScreen(
        state = state,
        onBackClick = onBackClick,
        onLoginClick = onLoginClick,
        onRetry = viewModel::retry,
        onNfcClaimClick = onNfcClaimClick,
        modifier = modifier,
    )
}

@Composable
fun CouponScreen(
    state: CouponUiState,
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onNfcClaimClick: () -> Unit = {},
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = CafeTheme.colors.canvas,
        contentColor = CafeTheme.colors.body,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CafeTopBar(
                title = "쿠폰",
                navigationIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_left),
                        contentDescription = null,
                        tint = CafeTheme.colors.ink,
                    )
                },
                onNavigationClick = onBackClick,
            )

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
                CafeButton(
                    text = "NFC 쿠폰 받기",
                    onClick = onNfcClaimClick,
                    modifier = Modifier.fillMaxWidth(),
                    variant = CafeButtonVariant.Secondary,
                )

                when (state) {
                    CouponUiState.Loading -> LoadingView()
                    is CouponUiState.Content -> CouponContent(state = state)
                    is CouponUiState.Error -> ErrorView(
                        message = state.message,
                        retryable = state.retryable,
                        onRetry = onRetry,
                    )

                    is CouponUiState.NeedsLogin -> EmptyView(
                        message = state.message,
                        actionLabel = state.actionLabel,
                        onAction = onLoginClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun CouponContent(state: CouponUiState.Content) {
    Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space5)) {
        CouponStampCard(stamp = state.stamp)
        CouponList(coupons = state.coupons)
    }
}

@Composable
private fun CouponStampCard(stamp: CouponStampUiModel) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    CafeCard(
        modifier = Modifier.fillMaxWidth(),
        type = CafeCardType.Product,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
                    Text(
                        text = "${stamp.storeName} 스탬프",
                        style = CafeTheme.typography.caption,
                        color = colors.muted,
                    )
                    Text(
                        text = stamp.countLabel,
                        style = CafeTheme.typography.display,
                        color = colors.onDark,
                    )
                }
                Surface(
                    modifier = Modifier.size(spacing.space14),
                    shape = CafeTheme.shapes.radiusMd,
                    color = colors.canvas,
                    contentColor = colors.primary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "✱",
                            style = CafeTheme.typography.h2,
                            color = colors.primary,
                        )
                    }
                }
            }

            Text(
                text = stamp.guideMessage,
                style = CafeTheme.typography.caption,
                color = colors.primary,
            )

            StampGrid(slots = stamp.slots)
        }
    }
}

@Composable
private fun StampGrid(slots: List<CouponStampSlotUiModel>) {
    val rows = slots.chunked(StampColumns)

    Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space2)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space2),
            ) {
                row.forEach { slot ->
                    StampSlot(
                        slot = slot,
                        modifier = Modifier.weight(StampSlotWeight),
                    )
                }
            }
        }
    }
}

@Composable
private fun StampSlot(
    slot: CouponStampSlotUiModel,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing
    val containerColor = if (slot.filled) {
        colors.primary
    } else {
        colors.ink
    }
    val contentColor = if (slot.filled) {
        colors.onPrimary
    } else {
        colors.muted
    }

    Surface(
        modifier = modifier.height(spacing.space6 + spacing.space1),
        shape = CafeTheme.shapes.radiusPill,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = slot.label,
                style = CafeTheme.typography.caption,
                color = contentColor,
            )
        }
    }
}

@Composable
private fun CouponList(coupons: List<CouponItemUiModel>) {
    Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space4)) {
        Text(
            text = "보유 쿠폰 (${coupons.size})",
            style = CafeTheme.typography.caption,
            color = CafeTheme.colors.muted,
        )

        coupons.forEach { coupon ->
            CouponCard(coupon = coupon)
        }
    }
}

@Composable
private fun CouponCard(coupon: CouponItemUiModel) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing
    val dateColor = if (coupon.expiringSoon) {
        colors.warning
    } else {
        colors.muted
    }

    CafeCard(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (coupon.dimmed) DimmedAlpha else FullAlpha),
        type = CafeCardType.Default,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(spacing.space8),
                shape = CafeTheme.shapes.radiusPill,
                color = colors.primary,
                contentColor = colors.onPrimary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "쿠폰",
                        style = CafeTheme.typography.meta,
                        color = colors.onPrimary,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(CouponTextWeight),
                verticalArrangement = Arrangement.spacedBy(spacing.space1),
            ) {
                Text(
                    text = coupon.title,
                    style = CafeTheme.typography.h3,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = coupon.expiresLabel,
                    style = CafeTheme.typography.caption,
                    color = dateColor,
                )
            }

            Text(
                text = "›",
                style = CafeTheme.typography.h3,
                color = colors.muted,
            )
        }
    }
}

private const val StampColumns = 5
private const val StampSlotWeight = 1f
private const val CouponTextWeight = 1f
private const val DimmedAlpha = 0.45f
private const val FullAlpha = 1f
