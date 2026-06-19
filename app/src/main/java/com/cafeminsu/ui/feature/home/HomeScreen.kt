package com.cafeminsu.ui.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafeminsu.ui.components.CafeButton
import com.cafeminsu.ui.components.CafeCard
import com.cafeminsu.ui.components.CafeCardType
import com.cafeminsu.ui.components.EmptyView
import com.cafeminsu.ui.components.ErrorView
import com.cafeminsu.ui.components.LoadingView
import com.cafeminsu.ui.theme.CafeTheme
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HomeRoute(
    onRecommendedOrderClick: (String) -> Unit,
    onCouponClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onRecentOrdersClick: () -> Unit,
    onReorderClick: (String) -> Unit,
    onBrowseMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    HomeScreen(
        state = state,
        onRecommendedOrderClick = onRecommendedOrderClick,
        onCouponClick = onCouponClick,
        onNotificationClick = onNotificationClick,
        onRecentOrdersClick = onRecentOrdersClick,
        onReorderClick = onReorderClick,
        onBrowseMenuClick = onBrowseMenuClick,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onRecommendedOrderClick: (String) -> Unit,
    onCouponClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onRecentOrdersClick: () -> Unit,
    onReorderClick: (String) -> Unit,
    onBrowseMenuClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
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
            when (state) {
                HomeUiState.Loading -> LoadingView()
                is HomeUiState.Content -> HomeContent(
                    content = state,
                    onRecommendedOrderClick = onRecommendedOrderClick,
                    onCouponClick = onCouponClick,
                    onNotificationClick = onNotificationClick,
                    onRecentOrdersClick = onRecentOrdersClick,
                    onReorderClick = onReorderClick,
                    onBrowseMenuClick = onBrowseMenuClick,
                )

                is HomeUiState.Empty -> HomeEmpty(
                    state = state,
                    onNotificationClick = onNotificationClick,
                    onBrowseMenuClick = onBrowseMenuClick,
                )

                is HomeUiState.Error -> ErrorView(
                    message = state.message,
                    retryable = state.retryable,
                    onRetry = onRetry,
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    content: HomeUiState.Content,
    onRecommendedOrderClick: (String) -> Unit,
    onCouponClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onRecentOrdersClick: () -> Unit,
    onReorderClick: (String) -> Unit,
    onBrowseMenuClick: () -> Unit,
) {
    HomeHeader(
        greeting = content.greeting,
        onNotificationClick = onNotificationClick,
    )
    FeaturedMenuCard(
        menu = content.recommendedMenu,
        onClick = { onRecommendedOrderClick(content.recommendedMenu.id) },
    )
    CouponSummaryCard(
        availableCouponCount = content.availableCouponCount,
        onClick = onCouponClick,
    )
    RecentOrdersSection(
        orders = content.recentOrders,
        onRecentOrdersClick = onRecentOrdersClick,
        onReorderClick = onReorderClick,
        onBrowseMenuClick = onBrowseMenuClick,
    )
}

@Composable
private fun HomeEmpty(
    state: HomeUiState.Empty,
    onNotificationClick: () -> Unit,
    onBrowseMenuClick: () -> Unit,
) {
    HomeHeader(
        greeting = state.greeting,
        onNotificationClick = onNotificationClick,
    )
    EmptyView(
        message = state.message,
        actionLabel = "메뉴 보러가기",
        onAction = onBrowseMenuClick,
    )
}

@Composable
private fun HomeHeader(
    greeting: String,
    onNotificationClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(HeaderTextWeight),
            verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space1),
        ) {
            Text(
                text = greeting,
                style = CafeTheme.typography.h1,
                color = CafeTheme.colors.ink,
            )
            Text(
                text = "오늘도 잘 부탁드려요",
                style = CafeTheme.typography.body,
                color = CafeTheme.colors.muted,
            )
        }

        NotificationBellButton(onClick = onNotificationClick)
    }
}

@Composable
private fun NotificationBellButton(onClick: () -> Unit) {
    val spacing = CafeTheme.spacing

    Box(
        modifier = Modifier
            .size(spacing.space10 + spacing.space2)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "알림" },
        contentAlignment = Alignment.Center,
    ) {
        BellIcon(modifier = Modifier.size(spacing.space6))
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = spacing.space2, end = spacing.space2)
                .size(spacing.space2),
            shape = CafeTheme.shapes.radiusPill,
            color = CafeTheme.colors.primary,
            contentColor = CafeTheme.colors.primary,
        ) {}
    }
}

@Composable
private fun BellIcon(modifier: Modifier = Modifier) {
    val color = CafeTheme.colors.ink

    Canvas(modifier = modifier) {
        val stroke = Stroke(
            width = BellStrokeWidth.toPx(),
            cap = StrokeCap.Round,
        )
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
            strokeWidth = BellStrokeWidth.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * BellRightXRatio, size.height * BellBodyTopRatio),
            end = Offset(size.width * BellRightXRatio, size.height * BellBodyBottomRatio),
            strokeWidth = BellStrokeWidth.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * BellBaseLeftRatio, size.height * BellBodyBottomRatio),
            end = Offset(size.width * BellBaseRightRatio, size.height * BellBodyBottomRatio),
            strokeWidth = BellStrokeWidth.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = color,
            radius = BellClapperRadius.toPx(),
            center = Offset(size.width * BellCenterRatio, size.height * BellClapperYRatio),
        )
    }
}

@Composable
private fun FeaturedMenuCard(
    menu: HomeRecommendedMenu,
    onClick: () -> Unit,
) {
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
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "오늘의 추천 메뉴",
                    style = CafeTheme.typography.caption,
                    color = colors.onDark,
                )
                PopularTag()
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.space4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MenuThumbnail(
                    text = "☕",
                    modifier = Modifier.size(spacing.space18 + spacing.space2),
                )
                Column(
                    modifier = Modifier.weight(ContentWeight),
                    verticalArrangement = Arrangement.spacedBy(spacing.space1),
                ) {
                    Text(
                        text = menu.name,
                        style = CafeTheme.typography.h3,
                        color = colors.onDark,
                        maxLines = FeaturedNameMaxLines,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = menu.description,
                        style = CafeTheme.typography.caption,
                        color = colors.muted,
                        maxLines = FeaturedDescriptionMaxLines,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = formatWon(menu.price),
                            style = CafeTheme.typography.h3,
                            color = colors.primary,
                        )
                        Text(
                            text = formatWon(menu.originalPrice),
                            style = CafeTheme.typography.caption.copy(textDecoration = TextDecoration.LineThrough),
                            color = colors.muted,
                        )
                    }
                }
            }

            CafeButton(
                text = "지금 주문하기 ›",
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PopularTag() {
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
            text = "🔥 인기",
            style = CafeTheme.typography.caption,
            color = CafeTheme.colors.primary,
        )
    }
}

@Composable
private fun CouponSummaryCard(
    availableCouponCount: Int,
    onClick: () -> Unit,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    CafeCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {},
        type = CafeCardType.Default,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CouponIcon()
            Column(
                modifier = Modifier.weight(ContentWeight),
                verticalArrangement = Arrangement.spacedBy(spacing.space1),
            ) {
                Text(
                    text = "사용 가능 쿠폰 ${availableCouponCount}장",
                    style = CafeTheme.typography.bodyL,
                    color = colors.ink,
                )
                Text(
                    text = "1잔 무료 쿠폰 · 오늘 만료",
                    style = CafeTheme.typography.caption,
                    color = colors.primary,
                )
            }
            Text(
                text = "›",
                style = CafeTheme.typography.h2,
                color = colors.body,
            )
        }
    }
}

@Composable
private fun CouponIcon() {
    Surface(
        modifier = Modifier.size(CafeTheme.spacing.space8),
        shape = CafeTheme.shapes.radiusLg,
        color = CafeTheme.colors.primary,
        contentColor = CafeTheme.colors.onPrimary,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "쿠",
                style = CafeTheme.typography.caption,
                color = CafeTheme.colors.onPrimary,
            )
        }
    }
}

@Composable
private fun RecentOrdersSection(
    orders: List<HomeRecentOrderSummary>,
    onRecentOrdersClick: () -> Unit,
    onReorderClick: (String) -> Unit,
    onBrowseMenuClick: () -> Unit,
) {
    val spacing = CafeTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "다시 주문하기",
                style = CafeTheme.typography.h2,
                color = CafeTheme.colors.ink,
            )
            Text(
                modifier = Modifier.clickable(onClick = onRecentOrdersClick),
                text = "전체보기 ›",
                style = CafeTheme.typography.caption,
                color = CafeTheme.colors.body,
            )
        }

        if (orders.isEmpty()) {
            EmptyView(
                message = "최근 주문이 없어요",
                actionLabel = "메뉴 보러가기",
                onAction = onBrowseMenuClick,
            )
        } else {
            orders.chunked(RecentOrderColumns).forEach { rowOrders ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.space3),
                ) {
                    rowOrders.forEach { order ->
                        RecentOrderCard(
                            order = order,
                            onClick = { onReorderClick(order.menuItemId) },
                            modifier = Modifier.weight(ContentWeight),
                        )
                    }

                    if (rowOrders.size < RecentOrderColumns) {
                        Spacer(modifier = Modifier.weight(ContentWeight))
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentOrderCard(
    order: HomeRecentOrderSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    CafeCard(
        modifier = modifier
            .height(spacing.space18 * RecentOrderCardHeightMultiplier + spacing.space2)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {},
        type = CafeCardType.Default,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    MenuThumbnail(
                        text = "☕",
                        modifier = Modifier.size(spacing.space8),
                    )
                    Text(
                        text = order.orderedAtLabel,
                        style = CafeTheme.typography.meta,
                        color = colors.muted,
                    )
                }
                Text(
                    text = order.menuName,
                    style = CafeTheme.typography.h3,
                    color = colors.ink,
                    maxLines = RecentOrderNameMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = order.optionSummary,
                    style = CafeTheme.typography.caption,
                    color = colors.muted,
                    maxLines = RecentOrderOptionMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            ReorderPill(price = order.totalPrice)
        }
    }
}

@Composable
private fun ReorderPill(price: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CafeTheme.shapes.radiusPill,
        color = CafeTheme.colors.canvas,
        contentColor = CafeTheme.colors.primary,
        border = BorderStroke(CafeTheme.spacing.space1 / BorderWidthDivider, CafeTheme.colors.hairline),
    ) {
        Box(
            modifier = Modifier.padding(
                horizontal = CafeTheme.spacing.space3,
                vertical = CafeTheme.spacing.space2,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "${formatWon(price)} · 재주문",
                style = CafeTheme.typography.caption,
                color = CafeTheme.colors.primary,
                maxLines = ReorderPillMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MenuThumbnail(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CafeTheme.shapes.radiusLg,
        color = CafeTheme.colors.canvas,
        contentColor = CafeTheme.colors.ink,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = CafeTheme.typography.h2,
                color = CafeTheme.colors.ink,
            )
        }
    }
}

private fun formatWon(amount: Int): String =
    "${NumberFormat.getNumberInstance(Locale.KOREA).format(amount)}원"

private const val HeaderTextWeight = 1f
private const val ContentWeight = 1f
private const val BorderWidthDivider = 4
private const val RecentOrderColumns = 2
private const val RecentOrderCardHeightMultiplier = 2
private const val FeaturedNameMaxLines = 1
private const val FeaturedDescriptionMaxLines = 2
private const val RecentOrderNameMaxLines = 2
private const val RecentOrderOptionMaxLines = 1
private const val ReorderPillMaxLines = 1
private const val BellArcStartAngle = 200f
private const val BellArcSweepAngle = 140f
private const val BellArcLeftRatio = 0.28f
private const val BellArcTopRatio = 0.12f
private const val BellArcWidthRatio = 0.44f
private const val BellArcHeightRatio = 0.34f
private const val BellLeftXRatio = 0.28f
private const val BellRightXRatio = 0.72f
private const val BellBodyTopRatio = 0.32f
private const val BellBodyBottomRatio = 0.74f
private const val BellBaseLeftRatio = 0.22f
private const val BellBaseRightRatio = 0.78f
private const val BellCenterRatio = 0.5f
private const val BellClapperYRatio = 0.84f
private val BellStrokeWidth = 1.5.dp
private val BellClapperRadius = 1.5.dp
