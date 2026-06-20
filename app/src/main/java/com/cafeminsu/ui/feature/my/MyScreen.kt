package com.cafeminsu.ui.feature.my

import android.widget.Toast
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

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
        onNotificationSettingsClick = {
            Toast.makeText(context, "알림설정은 준비 중이에요", Toast.LENGTH_SHORT).show()
        },
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
            Text(
                text = "⚙",
                style = CafeTheme.typography.h2,
                color = CafeTheme.colors.ink,
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
                Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
                    Text(
                        text = "${profile.displayName} 님",
                        style = CafeTheme.typography.h2,
                        color = colors.onDark,
                    )
                    TierBadge(label = profile.tierLabel)
                }
            }

            HorizontalDivider(color = colors.ink.copy(alpha = DarkDividerAlpha))
            StatsRow(stats = stats)
        }
    }
}

@Composable
private fun TierBadge(label: String) {
    Surface(
        shape = CafeTheme.shapes.radiusPill,
        color = CafeTheme.colors.accentSoft,
        contentColor = CafeTheme.colors.primary,
    ) {
        Text(
            modifier = Modifier.padding(
                horizontal = CafeTheme.spacing.space3,
                vertical = CafeTheme.spacing.space1,
            ),
            text = label,
            style = CafeTheme.typography.caption,
            color = CafeTheme.colors.primary,
        )
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
        Text(
            text = "›",
            style = CafeTheme.typography.h2,
            color = colors.muted,
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
    val color = CafeTheme.colors.primary

    Canvas(modifier = modifier) {
        val stroke = Stroke(
            width = IconStrokeWidth.toPx(),
            cap = StrokeCap.Round,
        )
        when (id) {
            HistoryQuickMenuId -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * IconInsetRatio, size.height * IconTopRatio),
                    size = Size(size.width * IconBodyWidthRatio, size.height * IconBodyHeightRatio),
                    cornerRadius = CornerRadius(IconCornerRadius.toPx()),
                    style = stroke,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * IconLineLeftRatio, size.height * IconLineOneYRatio),
                    end = Offset(size.width * IconLineRightRatio, size.height * IconLineOneYRatio),
                    strokeWidth = IconStrokeWidth.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * IconLineLeftRatio, size.height * IconLineTwoYRatio),
                    end = Offset(size.width * IconLineRightRatio, size.height * IconLineTwoYRatio),
                    strokeWidth = IconStrokeWidth.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            GiftQuickMenuId -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * IconInsetRatio, size.height * GiftBoxTopRatio),
                    size = Size(size.width * IconBodyWidthRatio, size.height * GiftBoxHeightRatio),
                    cornerRadius = CornerRadius(IconCornerRadius.toPx()),
                    style = stroke,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * IconCenterRatio, size.height * GiftBoxTopRatio),
                    end = Offset(size.width * IconCenterRatio, size.height * GiftBoxBottomRatio),
                    strokeWidth = IconStrokeWidth.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * IconInsetRatio, size.height * GiftRibbonYRatio),
                    end = Offset(size.width * IconRightRatio, size.height * GiftRibbonYRatio),
                    strokeWidth = IconStrokeWidth.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * GiftBowLeftRatio, size.height * GiftBowYRatio),
                    end = Offset(size.width * IconCenterRatio, size.height * GiftBoxTopRatio),
                    strokeWidth = IconStrokeWidth.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * GiftBowRightRatio, size.height * GiftBowYRatio),
                    end = Offset(size.width * IconCenterRatio, size.height * GiftBoxTopRatio),
                    strokeWidth = IconStrokeWidth.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            CouponQuickMenuId -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * IconInsetRatio, size.height * CouponTopRatio),
                    size = Size(size.width * IconBodyWidthRatio, size.height * CouponHeightRatio),
                    cornerRadius = CornerRadius(IconCornerRadius.toPx()),
                    style = stroke,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * CouponFoldRatio, size.height * CouponTopRatio),
                    end = Offset(size.width * CouponFoldRatio, size.height * CouponBottomRatio),
                    strokeWidth = IconStrokeWidth.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            else -> {
                drawArc(
                    color = color,
                    startAngle = BellArcStartAngle,
                    sweepAngle = BellArcSweepAngle,
                    useCenter = false,
                    topLeft = Offset(size.width * BellArcLeftRatio, size.height * BellArcTopRatio),
                    size = Size(size.width * BellArcWidthRatio, size.height * BellArcHeightRatio),
                    style = stroke,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * BellLeftXRatio, size.height * BellBodyTopRatio),
                    end = Offset(size.width * BellLeftXRatio, size.height * BellBodyBottomRatio),
                    strokeWidth = IconStrokeWidth.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * BellRightXRatio, size.height * BellBodyTopRatio),
                    end = Offset(size.width * BellRightXRatio, size.height * BellBodyBottomRatio),
                    strokeWidth = IconStrokeWidth.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * BellBaseLeftRatio, size.height * BellBodyBottomRatio),
                    end = Offset(size.width * BellBaseRightRatio, size.height * BellBodyBottomRatio),
                    strokeWidth = IconStrokeWidth.toPx(),
                    cap = StrokeCap.Round,
                )
                drawCircle(
                    color = color,
                    radius = BellClapperRadius.toPx(),
                    center = Offset(size.width * IconCenterRatio, size.height * BellClapperYRatio),
                )
            }
        }
    }
}

private const val HistoryQuickMenuId = "history"
private const val GiftQuickMenuId = "gift"
private const val CouponQuickMenuId = "coupon"
private const val LogoutSettingId = "logout"
private const val StatCellWeight = 1f
private const val QuickMenuTileWeight = 1f
private const val SettingsLabelWeight = 1f
private const val DarkDividerAlpha = 0.28f
private val IconStrokeWidth = 1.5.dp
private val IconCornerRadius = 4.dp
private const val IconInsetRatio = 0.18f
private const val IconRightRatio = 0.82f
private const val IconTopRatio = 0.14f
private const val IconBodyWidthRatio = 0.64f
private const val IconBodyHeightRatio = 0.72f
private const val IconLineLeftRatio = 0.34f
private const val IconLineRightRatio = 0.66f
private const val IconLineOneYRatio = 0.42f
private const val IconLineTwoYRatio = 0.58f
private const val IconCenterRatio = 0.5f
private const val GiftBoxTopRatio = 0.30f
private const val GiftBoxHeightRatio = 0.54f
private const val GiftBoxBottomRatio = 0.84f
private const val GiftRibbonYRatio = 0.48f
private const val GiftBowLeftRatio = 0.34f
private const val GiftBowRightRatio = 0.66f
private const val GiftBowYRatio = 0.16f
private const val CouponTopRatio = 0.24f
private const val CouponHeightRatio = 0.52f
private const val CouponBottomRatio = 0.76f
private const val CouponFoldRatio = 0.64f
private const val BellArcStartAngle = 200f
private const val BellArcSweepAngle = 140f
private const val BellArcLeftRatio = 0.25f
private const val BellArcTopRatio = 0.16f
private const val BellArcWidthRatio = 0.5f
private const val BellArcHeightRatio = 0.42f
private const val BellLeftXRatio = 0.28f
private const val BellRightXRatio = 0.72f
private const val BellBodyTopRatio = 0.38f
private const val BellBodyBottomRatio = 0.68f
private const val BellBaseLeftRatio = 0.22f
private const val BellBaseRightRatio = 0.78f
private const val BellClapperYRatio = 0.78f
private val BellClapperRadius = 1.5.dp
