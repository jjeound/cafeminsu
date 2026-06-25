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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ssafy.cafeminsu.core.designsystem.component.CafeMinsuButton
import com.ssafy.cafeminsu.core.designsystem.component.CafeMinsuButtonVariant
import com.ssafy.cafeminsu.core.designsystem.theme.CafeMinsuTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Composable
fun HomeRoute(
    onStoreClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    onMyClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    HomeScreen(
        state = uiState,
        onNotificationClick = viewModel::onNotificationClick,
        onOrderAgainClick = viewModel::onOrderAgainClick,
        onBrowseMenuClick = viewModel::onBrowseMenuClick,
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
            onNotificationClick = onNotificationClick,
        )

        FeaturedMenuCard(
            menu = state.recommendedMenu,
            onBrowseMenuClick = onBrowseMenuClick,
        )

        QuickDestinationRow(
            onStoreClick = onStoreClick,
            onMenuClick = onMenuClick,
            onMyClick = onMyClick,
        )

        RecentOrdersSection(
            orders = state.recentOrders,
            onOrderAgainClick = onOrderAgainClick,
        )
    }
}

@Composable
private fun HomeHeader(
    greeting: String,
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
            verticalArrangement = Arrangement.spacedBy(spacing.space1),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = greeting,
                style = CafeMinsuTheme.typography.h1,
                color = colors.ink,
            )
            Text(
                text = "오늘도 가까운 매장에서 바로 주문해보세요.",
                style = CafeMinsuTheme.typography.body,
                color = colors.muted,
            )
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
        }
    }
}

@Composable
private fun FeaturedMenuCard(
    menu: HomeRecommendedMenu,
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
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(spacing.space2),
                ) {
                    Text(
                        text = "오늘의 추천",
                        style = CafeMinsuTheme.typography.caption,
                        color = colors.mutedSoft,
                    )
                    Text(
                        text = menu.name,
                        style = CafeMinsuTheme.typography.h2,
                        color = colors.onDark,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = menu.description,
                        style = CafeMinsuTheme.typography.body,
                        color = colors.mutedSoft,
                    )
                }

                MenuThumbnail()
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StoreNameTag(menu.storeName)
                Text(
                    text = menu.priceLabel,
                    style = CafeMinsuTheme.typography.h3.copy(fontWeight = FontWeight.Bold),
                    color = colors.onDark,
                )
            }

            CafeMinsuButton(
                text = "메뉴 보러가기",
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
    orders: List<RecentOrderUiModel>,
    onOrderAgainClick: (String) -> Unit,
) {
    val colors = CafeMinsuTheme.colors
    val spacing = CafeMinsuTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
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
                text = "전체 보기",
                style = CafeMinsuTheme.typography.body,
                color = colors.primary,
            )
        }

        orders.forEach { order ->
            RecentOrderCard(
                order = order,
                onOrderAgainClick = { onOrderAgainClick(order.id) },
            )
        }
    }
}

@Composable
private fun QuickDestinationRow(
    onStoreClick: () -> Unit,
    onMenuClick: () -> Unit,
    onMyClick: () -> Unit,
) {
    val colors = CafeMinsuTheme.colors
    val spacing = CafeMinsuTheme.spacing

    Row(horizontalArrangement = Arrangement.spacedBy(spacing.space3), modifier = Modifier.fillMaxWidth()) {
        DestinationCard(
            title = "매장",
            subtitle = "가까운 점포",
            onClick = onStoreClick,
            modifier = Modifier.weight(1f),
            accent = colors.primary,
        )
        DestinationCard(
            title = "메뉴",
            subtitle = "주문 시작",
            onClick = onMenuClick,
            modifier = Modifier.weight(1f),
            accent = colors.success,
        )
        DestinationCard(
            title = "마이",
            subtitle = "내 정보",
            onClick = onMyClick,
            modifier = Modifier.weight(1f),
            accent = colors.warning,
        )
    }
}

@Composable
private fun DestinationCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color,
) {
    val colors = CafeMinsuTheme.colors
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = colors.surfaceCard,
        shape = CafeMinsuTheme.shapes.radiusLg,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
            Text(text = title, style = CafeMinsuTheme.typography.bodyL.copy(fontWeight = FontWeight.Bold), color = colors.ink)
            Text(text = subtitle, style = CafeMinsuTheme.typography.caption, color = colors.muted)
        }
    }
}

@Composable
private fun RecentOrderCard(
    order: RecentOrderUiModel,
    onOrderAgainClick: () -> Unit,
) {
    val colors = CafeMinsuTheme.colors
    val spacing = CafeMinsuTheme.spacing

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surfaceCard,
        shape = CafeMinsuTheme.shapes.radiusLg,
    ) {
        Row(
            modifier = Modifier.padding(spacing.space4),
            horizontalArrangement = Arrangement.spacedBy(spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CafeMinsuTheme.shapes.radiusMd)
                    .background(colors.canvas),
                contentAlignment = Alignment.Center,
            ) {
                MenuThumbnail()
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
                Text(
                    text = order.menuName,
                    style = CafeMinsuTheme.typography.h3,
                    color = colors.ink,
                )
                Text(
                    text = order.storeName,
                    style = CafeMinsuTheme.typography.caption,
                    color = colors.muted,
                )
                Text(
                    text = order.orderedAt,
                    style = CafeMinsuTheme.typography.caption,
                    color = colors.mutedSoft,
                )
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
                Text(
                    text = order.priceLabel,
                    style = CafeMinsuTheme.typography.bodyL.copy(fontWeight = FontWeight.Bold),
                    color = colors.ink,
                )
                CafeMinsuButton(
                    text = "재주문",
                    onClick = onOrderAgainClick,
                    variant = CafeMinsuButtonVariant.Primary,
                )
            }
        }
    }
}

@Composable
private fun MenuThumbnail() {
    val colors = CafeMinsuTheme.colors
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CafeMinsuTheme.shapes.radiusLg)
            .background(colors.accentSoft),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "☕",
            style = CafeMinsuTheme.typography.h2,
            color = colors.primaryHover,
        )
    }
}

@Composable
private fun NotificationIcon(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(22.dp)) {
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
            center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height * 0.82f),
        )
    }
}

class HomeViewModel : ViewModel() {
    private val mutableUiState = MutableStateFlow(
        HomeUiState(
            greeting = "민수님, 안녕하세요",
            recommendedMenu = HomeRecommendedMenu(
                id = "recommended-1",
                name = "달콤한 카페모카",
                description = "점심 뒤에 잘 어울리는 진한 초코 풍미의 메뉴예요.",
                storeName = "카페민수 강남점",
                priceLabel = "5,200원",
            ),
            recentOrders = listOf(
                RecentOrderUiModel(
                    id = "order-1",
                    menuName = "아메리카노",
                    storeName = "카페민수 역삼점",
                    orderedAt = "오늘 10:24",
                    priceLabel = "4,000원",
                ),
                RecentOrderUiModel(
                    id = "order-2",
                    menuName = "바닐라 라떼",
                    storeName = "카페민수 선릉점",
                    orderedAt = "어제 18:10",
                    priceLabel = "5,100원",
                ),
            ),
        ),
    )
    val uiState: StateFlow<HomeUiState> = mutableUiState.asStateFlow()

    fun onNotificationClick() = Unit

    fun onOrderAgainClick(orderId: String) {
        mutableUiState.update { state ->
            state.copy(greeting = "선택한 주문을 다시 담는 중: $orderId")
        }
    }

    fun onBrowseMenuClick() = Unit
}

data class HomeUiState(
    val greeting: String = "카페민수",
    val recommendedMenu: HomeRecommendedMenu = HomeRecommendedMenu(),
    val recentOrders: List<RecentOrderUiModel> = emptyList(),
)

data class HomeRecommendedMenu(
    val id: String = "",
    val name: String = "오늘의 추천 메뉴",
    val description: String = "메뉴가 준비되면 여기에 표시됩니다.",
    val storeName: String = "가까운 매장",
    val priceLabel: String = "0원",
)

data class RecentOrderUiModel(
    val id: String,
    val menuName: String,
    val storeName: String,
    val orderedAt: String,
    val priceLabel: String,
)
