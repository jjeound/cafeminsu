package com.cafeminsu.ui.feature.stamp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafeminsu.domain.model.StampEvent
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
fun StampRoute(
    onBrowseMenuClick: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StampViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    StampScreen(
        state = state,
        onBrowseMenuClick = onBrowseMenuClick,
        onLoginClick = onLoginClick,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
fun StampScreen(
    state: StampUiState,
    onBrowseMenuClick: () -> Unit,
    onLoginClick: () -> Unit,
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
            Text(
                text = "스탬프",
                style = CafeTheme.typography.h1,
                color = CafeTheme.colors.ink,
            )

            when (state) {
                StampUiState.Loading -> LoadingView()
                is StampUiState.Content -> StampContent(state = state)
                is StampUiState.Empty -> StampEmpty(
                    state = state,
                    onBrowseMenuClick = onBrowseMenuClick,
                )

                is StampUiState.Error -> ErrorView(
                    message = state.message,
                    retryable = state.retryable,
                    onRetry = onRetry,
                )

                is StampUiState.NeedsLogin -> EmptyView(
                    message = state.message,
                    actionLabel = state.actionLabel,
                    onAction = onLoginClick,
                )
            }
        }
    }
}

@Composable
private fun StampContent(state: StampUiState.Content) {
    Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space5)) {
        StampProgressCard(
            currentCount = state.currentCount,
            goalCount = state.goalCount,
            remainingCount = state.remainingCount,
            isGoalReached = state.isGoalReached,
        )
        StampHistoryList(history = state.history)
    }
}

@Composable
private fun StampEmpty(
    state: StampUiState.Empty,
    onBrowseMenuClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space5)) {
        StampProgressCard(
            currentCount = state.currentCount,
            goalCount = state.goalCount,
            remainingCount = state.remainingCount,
            isGoalReached = state.isGoalReached,
        )
        EmptyView(
            message = state.message,
            actionLabel = "메뉴 보러가기",
            onAction = onBrowseMenuClick,
        )
    }
}

@Composable
private fun StampProgressCard(
    currentCount: Int,
    goalCount: Int,
    remainingCount: Int,
    isGoalReached: Boolean,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing
    val progressMessage = if (isGoalReached) {
        "무료 음료를 받을 수 있어요"
    } else {
        "무료 음료까지 ${remainingCount}개 남았어요"
    }

    CafeCard(
        modifier = Modifier.fillMaxWidth(),
        type = CafeCardType.Default,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "스탬프 진행",
                    style = CafeTheme.typography.h2,
                    color = colors.ink,
                )
                Text(
                    text = "$currentCount/$goalCount",
                    style = CafeTheme.typography.h2,
                    color = if (isGoalReached) colors.success else colors.primary,
                )
            }

            StampDots(
                currentCount = currentCount,
                goalCount = goalCount,
                isGoalReached = isGoalReached,
            )

            Text(
                text = progressMessage,
                style = CafeTheme.typography.body,
                color = colors.body,
            )
        }
    }
}

@Composable
private fun StampDots(
    currentCount: Int,
    goalCount: Int,
    isGoalReached: Boolean,
) {
    val rows = (0 until goalCount.coerceAtLeast(EmptyGoalCount)).chunked(StampDotsPerRow)

    Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space2)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space2)) {
                row.forEach { index ->
                    StampDot(
                        filled = index < currentCount,
                        isGoalReached = isGoalReached,
                    )
                }
            }
        }
    }
}

@Composable
private fun StampDot(
    filled: Boolean,
    isGoalReached: Boolean,
) {
    val colors = CafeTheme.colors
    val color = when {
        filled && isGoalReached -> colors.success
        filled -> colors.primary
        else -> colors.hairline
    }

    Surface(
        modifier = Modifier.size(CafeTheme.spacing.space5),
        shape = CircleShape,
        color = color,
        contentColor = color,
    ) {}
}

@Composable
private fun StampHistoryList(history: List<StampEvent>) {
    Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space4)) {
        Text(
            text = "적립 내역",
            style = CafeTheme.typography.h2,
            color = CafeTheme.colors.ink,
        )

        history.forEach { event ->
            StampHistoryRow(event = event)
        }
    }
}

@Composable
private fun StampHistoryRow(event: StampEvent) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    CafeCard(
        modifier = Modifier.fillMaxWidth(),
        type = CafeCardType.Info,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(HistoryTextWeight),
                verticalArrangement = Arrangement.spacedBy(spacing.space1),
            ) {
                Text(
                    text = "주문 ${event.orderId}",
                    style = CafeTheme.typography.h3,
                    color = colors.ink,
                )
                Text(
                    text = formatStampDate(event.createdAtMillis),
                    style = CafeTheme.typography.caption,
                    color = colors.muted,
                )
            }

            Text(
                text = "+${event.count}개",
                style = CafeTheme.typography.bodyL,
                color = colors.primary,
            )
        }
    }
}

private fun formatStampDate(createdAtMillis: Long): String =
    Instant.ofEpochMilli(createdAtMillis)
        .atZone(ZoneId.systemDefault())
        .format(stampDateFormatter)

private val stampDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.KOREA)

private const val EmptyGoalCount = 0
private const val StampDotsPerRow = 5
private const val HistoryTextWeight = 1f
