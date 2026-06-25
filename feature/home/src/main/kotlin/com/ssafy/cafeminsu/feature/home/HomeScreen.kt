package com.ssafy.cafeminsu.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.cafeminsu.core.designsystem.component.CafeMinsuButton
import com.ssafy.cafeminsu.core.designsystem.component.CafeMinsuButtonVariant
import com.ssafy.cafeminsu.core.designsystem.theme.CafeMinsuTheme

@Composable
fun HomeRoute(
    onNotificationClick: () -> Unit,
    onOrderAgainClick: (String) -> Unit,
    onBrowseMenuClick: () -> Unit,
    onStoreClick: () -> Unit,
    onMenuClick: () -> Unit,
    onMyClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        state = state,
        onNotificationClick = onNotificationClick,
        onOrderAgainClick = onOrderAgainClick,
        onBrowseMenuClick = onBrowseMenuClick,
        onStoreClick = onStoreClick,
        onMenuClick = onMenuClick,
        onMyClick = onMyClick,
        modifier = modifier,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onNotificationClick: () -> Unit,
    onOrderAgainClick: (String) -> Unit,
    onBrowseMenuClick: () -> Unit,
    onStoreClick: () -> Unit,
    onMenuClick: () -> Unit,
    onMyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeMinsuTheme.colors
    val spacing = CafeMinsuTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .padding(horizontal = spacing.space5, vertical = spacing.space6)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(spacing.space5),
    ) {
        HomeHeader(
            greeting = state.greeting,
            selectedStoreName = state.selectedStoreName,
            onNotificationClick = onNotificationClick,
        )

        FeaturedMenuCard(
            menu = state.recommendedMenu,
            onBrowseMenuClick = onBrowseMenuClick,
        )

        RecentOrdersSection(
            orders = state.recentOrders,
            onOrderAgainClick = onOrderAgainClick,
            onBrowseMenuClick = onBrowseMenuClick,
        )
    }
}

@Composable
private fun HomeHeader(
    greeting: String,
    selectedStoreName: String?,
    onNotificationClick: () -> Unit,
) {
    val colors = CafeMinsuTheme.colors
    val spacing = CafeMinsuTheme.spacing

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.space2),
        ) {
            Text(
                text = greeting,
                style = CafeMinsuTheme.typography.h1,
                color = colors.ink,
            )

            Text(
                text = "오늘의 추천 메뉴와 최근 주문을 확인해보세요.",
                style = CafeMinsuTheme.typography.body,
                color = colors.muted,
            )

            if (!selectedStoreName.isNullOrBlank()) {
                StoreNameTag(storeName = selectedStoreName)
            }
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onNotificationClick)
                .background(colors.surfaceCard),
            contentAlignment = Alignment.Center,
        ) {
            NotificationIcon(color = colors.ink)

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(colors.primary),
            )
        }
    }
}

@Composable
private fun FeaturedMenuCard(
    menu: HomeRecommendedMenu?,
    onBrowseMenuClick: () -> Unit,
) {
    val colors = CafeMinsuTheme.colors
    val spacing = CafeMinsuTheme.spacing

    Surface(
        color = colors.surfaceDark,
        contentColor = colors.onDark,
        shape = CafeMinsuTheme.shapes.radiusXl,
        border = BorderStroke(1.dp, colors.hairline.copy(alpha = 0.08f)),
    ) {
        Column(
            modifier = Modifier.padding(spacing.space5),
            verticalArrangement = Arrangement.spacedBy(spacing.space4),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = "추천 메뉴",
                    style = CafeMinsuTheme.typography.caption,
                    color = colors.mutedSoft,
                )

                val storeName = menu?.storeName
                if (!storeName.isNullOrBlank()) {
                    StoreNameTag(storeName = storeName)
                }
            }

            if (menu == null) {
                Text(
                    text = "아직 추천할 메뉴가 없어요.",
                    style = CafeMinsuTheme.typography.h3,
                    color = colors.onDark,
                )

                Text(
                    text = "매장을 선택하면 메뉴를 불러올 수 있어요.",
                    style = CafeMinsuTheme.typography.body,
                    color = colors.mutedSoft,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.space4),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MenuThumbnail(
                        text = "C",
                        modifier = Modifier.size(spacing.space18 + spacing.space2),
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(spacing.space1),
                    ) {
                        Text(
                            text = menu.name,
                            style = CafeMinsuTheme.typography.h3,
                            color = colors.onDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Text(
                            text = menu.description,
                            style = CafeMinsuTheme.typography.caption,
                            color = colors.muted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Text(
                            text = menu.priceLabel,
                            style = CafeMinsuTheme.typography.h3.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = colors.primary,
                        )
                    }
                }
            }

            CafeMinsuButton(
                text = "메뉴 둘러보기",
                onClick = onBrowseMenuClick,
                modifier = Modifier.fillMaxWidth(),
                variant = CafeMinsuButtonVariant.Secondary,
            )
        }
    }
}

@Composable
private fun StoreNameTag(storeName: String) {
    val colors = CafeMinsuTheme.colors

    Surface(
        color = colors.canvas.copy(alpha = 0.12f),
        shape = CafeMinsuTheme.shapes.radiusPill,
    ) {
        Text(
            text = storeName,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = CafeMinsuTheme.typography.caption,
            color = colors.onDark,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RecentOrdersSection(
    orders: List<HomeRecentOrderUiModel>,
    onOrderAgainClick: (String) -> Unit,
    onBrowseMenuClick: () -> Unit,
) {
    val colors = CafeMinsuTheme.colors
    val spacing = CafeMinsuTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "최근 주문",
                style = CafeMinsuTheme.typography.h2,
                color = colors.ink,
            )

            Text(
                text = "전체보기",
                style = CafeMinsuTheme.typography.body,
                color = colors.primary,
            )
        }

        if (orders.isEmpty()) {
            EmptyOrdersCard(onBrowseMenuClick = onBrowseMenuClick)
        } else {
            orders.chunked(2).forEach { rowOrders ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.space3),
                ) {
                    rowOrders.forEach { order ->
                        RecentOrderCard(
                            order = order,
                            onOrderAgainClick = {
                                onOrderAgainClick(order.id.toString())
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    if (rowOrders.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentOrderCard(
    order: HomeRecentOrderUiModel,
    onOrderAgainClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeMinsuTheme.colors
    val spacing = CafeMinsuTheme.spacing

    Surface(
        modifier = modifier
            .heightIn(min = 176.dp)
            .clickable(onClick = onOrderAgainClick),
        color = colors.surfaceCard,
        shape = CafeMinsuTheme.shapes.radiusLg,
    ) {
        Column(
            modifier = Modifier.padding(spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    MenuThumbnail(
                        modifier = Modifier.size(spacing.space8),
                        text = "#",
                    )

                    Text(
                        text = order.orderedAtLabel,
                        style = CafeMinsuTheme.typography.meta,
                        color = colors.muted,
                    )
                }

                Text(
                    text = order.orderNumber,
                    style = CafeMinsuTheme.typography.h3,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                StatusChip(status = order.statusLabel)
            }

            Text(
                text = order.priceLabel,
                style = CafeMinsuTheme.typography.h3.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = colors.primary,
            )

            CafeMinsuButton(
                text = "다시 주문",
                onClick = onOrderAgainClick,
                modifier = Modifier.fillMaxWidth(),
                variant = CafeMinsuButtonVariant.Primary,
            )
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val colors = CafeMinsuTheme.colors

    Surface(
        color = colors.accentSoft,
        shape = CafeMinsuTheme.shapes.radiusPill,
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = CafeMinsuTheme.typography.caption,
            color = colors.primary,
        )
    }
}

@Composable
private fun EmptyOrdersCard(
    onBrowseMenuClick: () -> Unit,
) {
    val colors = CafeMinsuTheme.colors

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surfaceCard,
        shape = CafeMinsuTheme.shapes.radiusLg,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "아직 최근 주문이 없어요.",
                style = CafeMinsuTheme.typography.body,
                color = colors.muted,
            )

            CafeMinsuButton(
                text = "메뉴 둘러보기",
                onClick = onBrowseMenuClick,
                modifier = Modifier.fillMaxWidth(),
                variant = CafeMinsuButtonVariant.Secondary,
            )
        }
    }
}

@Composable
private fun MenuThumbnail(
    modifier: Modifier = Modifier,
    text: String,
) {
    val colors = CafeMinsuTheme.colors

    Box(
        modifier = modifier
            .clip(CafeMinsuTheme.shapes.radiusLg)
            .background(colors.accentSoft),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = CafeMinsuTheme.typography.h2,
            color = colors.primaryHover,
        )
    }
}

@Composable
private fun NotificationIcon(
    color: Color,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.Canvas(
        modifier = modifier.size(22.dp),
    ) {
        val bellWidth = size.width * 0.56f
        val bellHeight = size.height * 0.56f
        val left = (size.width - bellWidth) / 2f
        val top = size.height * 0.12f

        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(left, top),
            size = androidx.compose.ui.geometry.Size(bellWidth, bellHeight),
            style = Stroke(width = size.width * 0.14f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.18f),
        )

        drawCircle(
            color = color,
            radius = size.width * 0.06f,
            center = androidx.compose.ui.geometry.Offset(
                x = size.width / 2f,
                y = size.height * 0.82f,
            ),
        )
    }
}