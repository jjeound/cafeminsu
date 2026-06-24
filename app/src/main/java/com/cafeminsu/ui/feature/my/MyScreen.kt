package com.cafeminsu.ui.feature.my

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafeminsu.R
import com.cafeminsu.ui.components.CafeCard
import com.cafeminsu.ui.components.CafeCardType
import com.cafeminsu.ui.components.EmptyView
import com.cafeminsu.ui.components.ErrorView
import com.cafeminsu.ui.components.LoadingView
import com.cafeminsu.ui.components.LogoutConfirmDialog
import com.cafeminsu.ui.theme.CafeTheme

@Composable
fun MyRoute(
    onHistoryClick: () -> Unit,
    onGiftClick: () -> Unit,
    onCouponClick: () -> Unit,
    onNotificationSettingsClick: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                MyEvent.NavigateLogin -> onLoginClick()
            }
        }
    }

    MyScreen(
        state = state,
        onHistoryClick = onHistoryClick,
        onGiftClick = onGiftClick,
        onCouponClick = onCouponClick,
        onNotificationSettingsClick = onNotificationSettingsClick,
        onLoginClick = onLoginClick,
        onLogoutClick = viewModel::onLogout,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
fun MyScreen(
    state: MyUiState,
    onHistoryClick: () -> Unit,
    onGiftClick: () -> Unit,
    onCouponClick: () -> Unit,
    onNotificationSettingsClick: () -> Unit,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLogoutConfirm by remember { mutableStateOf(false) }

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
            verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space6),
        ) {
            MyHeader()

            when (state) {
                MyUiState.Loading -> LoadingView()
                is MyUiState.Content -> MyContent(
                    state = state,
                    onHistoryClick = onHistoryClick,
                    onGiftClick = onGiftClick,
                    onCouponClick = onCouponClick,
                    onNotificationSettingsClick = onNotificationSettingsClick,
                    onLogoutClick = { showLogoutConfirm = true },
                )

                is MyUiState.Empty -> EmptyView(
                    message = state.message,
                    actionLabel = state.actionLabel,
                    onAction = onLoginClick,
                )

                is MyUiState.Error -> ErrorView(
                    message = state.message,
                    retryable = state.retryable,
                    onRetry = onRetry,
                )

                is MyUiState.NeedsLogin -> EmptyView(
                    message = state.message,
                    actionLabel = state.actionLabel,
                    onAction = onLoginClick,
                )
            }
        }
    }

    if (showLogoutConfirm) {
        LogoutConfirmDialog(
            onDismiss = { showLogoutConfirm = false },
            onConfirm = {
                showLogoutConfirm = false
                onLogoutClick()
            },
        )
    }
}

@Composable
private fun MyHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "MY",
            style = CafeTheme.typography.h1,
            color = CafeTheme.colors.ink,
        )
        Box(
            modifier = Modifier
                .size(CafeTheme.spacing.space10 + CafeTheme.spacing.space2)
                .semantics { contentDescription = "설정" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_settings),
                contentDescription = null,
                tint = CafeTheme.colors.ink,
                modifier = Modifier.size(CafeTheme.spacing.space6),
            )
        }
    }
}

@Composable
private fun MyContent(
    state: MyUiState.Content,
    onHistoryClick: () -> Unit,
    onGiftClick: () -> Unit,
    onCouponClick: () -> Unit,
    onNotificationSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space6)) {
        ProfileCard(
            profile = state.profile,
            stats = state.stats,
        )
        QuickMenuSection(
            quickMenus = state.quickMenus,
            onHistoryClick = onHistoryClick,
            onGiftClick = onGiftClick,
            onCouponClick = onCouponClick,
            onNotificationSettingsClick = onNotificationSettingsClick,
        )
        SettingsList(
            settings = state.settings,
            onLogoutClick = onLogoutClick,
        )
    }
}

@Composable
private fun ProfileCard(
    profile: MyProfileUiModel,
    stats: MyStatsUiModel,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    CafeCard(
        modifier = Modifier.fillMaxWidth(),
        type = CafeCardType.Product,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space5)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.space4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(spacing.space14),
                    shape = CafeTheme.shapes.radiusPill,
                    color = colors.primary,
                    contentColor = colors.onPrimary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = profile.initial,
                            style = CafeTheme.typography.h2,
                            color = colors.onPrimary,
                        )
                    }
                }
                Text(
                    text = "${profile.displayName} 님",
                    style = CafeTheme.typography.h2,
                    color = colors.onDark,
                )
            }

            HorizontalDivider(color = colors.ink.copy(alpha = DarkDividerAlpha))
            StatsRow(stats = stats)
        }
    }
}

@Composable
private fun StatsRow(stats: MyStatsUiModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatCell(
            value = stats.orderCount.toString(),
            label = "주문",
            modifier = Modifier.weight(StatCellWeight),
        )
        StatDivider()
        StatCell(
            value = "${stats.stampCount}/${stats.stampGoalCount}",
            label = "스탬프",
            modifier = Modifier.weight(StatCellWeight),
        )
        StatDivider()
        StatCell(
            value = stats.couponCount.toString(),
            label = "쿠폰",
            modifier = Modifier.weight(StatCellWeight),
        )
    }
}

@Composable
private fun StatCell(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space1),
    ) {
        Text(
            text = value,
            style = CafeTheme.typography.h3,
            color = CafeTheme.colors.onDark,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = label,
            style = CafeTheme.typography.caption,
            color = CafeTheme.colors.muted,
            maxLines = 1,
        )
    }
}

@Composable
private fun StatDivider() {
    Spacer(
        modifier = Modifier
            .width(1.dp)
            .height(CafeTheme.spacing.space8)
            .background(CafeTheme.colors.ink.copy(alpha = DarkDividerAlpha))
    )
}

@Composable
private fun QuickMenuSection(
    quickMenus: List<MyQuickMenuUiModel>,
    onHistoryClick: () -> Unit,
    onGiftClick: () -> Unit,
    onCouponClick: () -> Unit,
    onNotificationSettingsClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space4)) {
        SectionTitle(text = "빠른 메뉴")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space2),
        ) {
            quickMenus.forEach { quickMenu ->
                QuickMenuTile(
                    quickMenu = quickMenu,
                    onClick = when (quickMenu.id) {
                        HistoryQuickMenuId -> onHistoryClick
                        GiftQuickMenuId -> onGiftClick
                        CouponQuickMenuId -> onCouponClick
                        else -> onNotificationSettingsClick
                    },
                    modifier = Modifier.weight(QuickMenuTileWeight),
                )
            }
        }
    }
}

@Composable
private fun QuickMenuTile(
    quickMenu: MyQuickMenuUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = CafeTheme.spacing

    Surface(
        modifier = modifier
            .height(spacing.space18 + spacing.space5)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {},
        shape = CafeTheme.shapes.radiusLg,
        color = CafeTheme.colors.surfaceCard,
        contentColor = CafeTheme.colors.primary,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = spacing.space2,
                vertical = spacing.space4,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            QuickMenuIcon(
                id = quickMenu.id,
                modifier = Modifier.size(spacing.space8),
            )
            Text(
                text = quickMenu.label,
                style = CafeTheme.typography.caption,
                color = CafeTheme.colors.ink,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SettingsList(
    settings: List<MySettingItemUiModel>,
    onLogoutClick: () -> Unit,
) {
    CafeCard(
        modifier = Modifier.fillMaxWidth(),
        type = CafeCardType.Default,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            settings.forEachIndexed { index, item ->
                SettingRow(
                    item = item,
                    onClick = if (item.id == LogoutSettingId) onLogoutClick else null,
                )
                if (index != settings.lastIndex) {
                    HorizontalDivider(color = CafeTheme.colors.hairline)
                }
            }
        }
    }
}

@Composable
private fun SettingRow(
    item: MySettingItemUiModel,
    onClick: (() -> Unit)?,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing
    val rowModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Row(
        modifier = rowModifier
            .fillMaxWidth()
            .heightIn(min = spacing.space10 + spacing.space3)
            .padding(vertical = spacing.space3),
        horizontalArrangement = Arrangement.spacedBy(spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = item.label,
            modifier = Modifier.weight(SettingsLabelWeight),
            style = CafeTheme.typography.body,
            color = if (item.isDestructive) colors.primary else colors.ink,
        )
        item.trailingText?.let { trailingText ->
            Text(
                text = trailingText,
                style = CafeTheme.typography.meta,
                color = colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = colors.muted,
            modifier = Modifier.size(CafeTheme.spacing.space5),
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = CafeTheme.typography.h2,
        color = CafeTheme.colors.ink,
    )
}

@Composable
private fun QuickMenuIcon(
    id: String,
    modifier: Modifier = Modifier,
) {
    val iconRes = when (id) {
        HistoryQuickMenuId -> R.drawable.ic_history
        GiftQuickMenuId -> R.drawable.ic_gift
        CouponQuickMenuId -> R.drawable.ic_coupon
        else -> R.drawable.ic_bell
    }
    Icon(
        painter = painterResource(iconRes),
        contentDescription = null,
        tint = CafeTheme.colors.primary,
        modifier = modifier,
    )
}

private const val HistoryQuickMenuId = "history"
private const val GiftQuickMenuId = "gift"
private const val CouponQuickMenuId = "coupon"
private const val LogoutSettingId = "logout"
private const val StatCellWeight = 1f
private const val QuickMenuTileWeight = 1f
private const val SettingsLabelWeight = 1f
private const val DarkDividerAlpha = 0.28f
