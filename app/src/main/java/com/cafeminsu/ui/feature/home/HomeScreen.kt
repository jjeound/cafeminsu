package com.cafeminsu.ui.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafeminsu.ui.components.CafeButton
import com.cafeminsu.ui.components.CafeCard
import com.cafeminsu.ui.components.CafeCardType
import com.cafeminsu.ui.components.EmptyView
import com.cafeminsu.ui.components.ErrorView
import com.cafeminsu.ui.components.LoadingView
import com.cafeminsu.ui.theme.CafeTheme

@Composable
fun HomeRoute(
    onMenuClick: (String) -> Unit,
    onBrowseMenuClick: () -> Unit,
    onVoiceOrderClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    HomeScreen(
        state = state,
        onMenuClick = onMenuClick,
        onBrowseMenuClick = onBrowseMenuClick,
        onVoiceOrderClick = onVoiceOrderClick,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onMenuClick: (String) -> Unit,
    onBrowseMenuClick: () -> Unit,
    onVoiceOrderClick: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space8),
        ) {
            when (state) {
                HomeUiState.Loading -> LoadingView()
                is HomeUiState.Content -> HomeContent(
                    content = state,
                    onMenuClick = onMenuClick,
                    onVoiceOrderClick = onVoiceOrderClick,
                )

                is HomeUiState.Empty -> HomeEmpty(
                    state = state,
                    onBrowseMenuClick = onBrowseMenuClick,
                    onVoiceOrderClick = onVoiceOrderClick,
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
    onMenuClick: (String) -> Unit,
    onVoiceOrderClick: () -> Unit,
) {
    Greeting(text = content.greeting)
    VoiceOrderButton(onClick = onVoiceOrderClick)

    content.ongoingOrder?.let { ongoingOrder ->
        OngoingOrderCard(ongoingOrder)
    }

    StampSummaryCard(summary = content.stampSummary)

    RecommendedMenus(
        menus = content.recommendedMenus,
        onMenuClick = onMenuClick,
    )
}

@Composable
private fun HomeEmpty(
    state: HomeUiState.Empty,
    onBrowseMenuClick: () -> Unit,
    onVoiceOrderClick: () -> Unit,
) {
    Greeting(text = state.greeting)
    VoiceOrderButton(onClick = onVoiceOrderClick)
    EmptyView(
        message = state.message,
        actionLabel = "메뉴 보러가기",
        onAction = onBrowseMenuClick,
    )
}

@Composable
private fun Greeting(text: String) {
    Text(
        text = text,
        style = CafeTheme.typography.display,
        color = CafeTheme.colors.ink,
    )
}

@Composable
private fun VoiceOrderButton(onClick: () -> Unit) {
    CafeButton(
        text = "음성으로 주문하기",
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun StampSummaryCard(summary: HomeStampSummary) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing
    val remainingMessage = if (summary.remainingCount == 0) {
        "쿠폰을 받을 수 있어요"
    } else {
        "무료 음료까지 ${summary.remainingCount}개 남았어요"
    }

    CafeCard(
        modifier = Modifier.fillMaxWidth(),
        type = CafeCardType.Default,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "스탬프",
                    style = CafeTheme.typography.h2,
                    color = colors.ink,
                )
                Text(
                    text = "${summary.currentCount}/${summary.goalCount}",
                    style = CafeTheme.typography.h2,
                    color = colors.primary,
                )
            }

            LinearProgressIndicator(
                progress = { summary.progress },
                modifier = Modifier.fillMaxWidth(),
                color = colors.primary,
                trackColor = colors.hairline,
            )

            Text(
                text = remainingMessage,
                style = CafeTheme.typography.body,
                color = colors.body,
            )
        }
    }
}

@Composable
private fun RecommendedMenus(
    menus: List<HomeMenuSummary>,
    onMenuClick: (String) -> Unit,
) {
    HomeSection(title = "추천 메뉴") {
        Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space4)) {
            menus.forEach { menu ->
                RecommendedMenuCard(
                    menu = menu,
                    onClick = { onMenuClick(menu.id) },
                )
            }
        }
    }
}

@Composable
private fun RecommendedMenuCard(
    menu: HomeMenuSummary,
    onClick: () -> Unit,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    CafeCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        type = CafeCardType.Product,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
            Text(
                text = menu.name,
                style = CafeTheme.typography.h3,
                color = colors.onDark,
            )
            Text(
                text = menu.description,
                style = CafeTheme.typography.body,
                color = colors.onDark,
            )
            Spacer(modifier = Modifier.height(spacing.space1))
            Text(
                text = "${menu.price}원",
                style = CafeTheme.typography.bodyL,
                color = colors.primary,
            )
        }
    }
}

@Composable
private fun OngoingOrderCard(order: HomeOngoingOrderSummary) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    CafeCard(
        modifier = Modifier.fillMaxWidth(),
        type = CafeCardType.Info,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
            Text(
                text = "진행 중 주문",
                style = CafeTheme.typography.h2,
                color = colors.ink,
            )
            Text(
                text = order.title,
                style = CafeTheme.typography.bodyL,
                color = colors.body,
            )
            Text(
                text = "${order.orderNumber} · ${order.status}",
                style = CafeTheme.typography.caption,
                color = colors.muted,
            )
        }
    }
}

@Composable
private fun HomeSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space4)) {
        Text(
            text = title,
            style = CafeTheme.typography.h2,
            color = CafeTheme.colors.ink,
        )
        content()
    }
}
