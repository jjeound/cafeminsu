package com.cafeminsu.ui.feature.order

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.SelectedOption
import com.cafeminsu.ui.components.CafeCard
import com.cafeminsu.ui.components.CafeCardType
import com.cafeminsu.ui.components.ErrorView
import com.cafeminsu.ui.components.LoadingView
import com.cafeminsu.ui.theme.CafeTheme
import java.text.NumberFormat
import java.util.Locale

@Composable
fun OrderStatusRoute(
    modifier: Modifier = Modifier,
    viewModel: OrderStatusViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    OrderStatusScreen(
        state = state,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
fun OrderStatusScreen(
    state: OrderStatusUiState,
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
                .padding(screenPadding()),
            verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space6),
        ) {
            when (state) {
                OrderStatusUiState.Loading -> {
                    Text(
                        text = "주문 상태",
                        style = CafeTheme.typography.h1,
                        color = CafeTheme.colors.ink,
                    )
                    LoadingView()
                }

                is OrderStatusUiState.Content -> OrderStatusContent(state = state)

                is OrderStatusUiState.Error -> {
                    Text(
                        text = "주문 상태",
                        style = CafeTheme.typography.h1,
                        color = CafeTheme.colors.ink,
                    )
                    ErrorView(
                        message = state.message,
                        retryable = state.retryable,
                        onRetry = onRetry,
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderStatusContent(
    state: OrderStatusUiState.Content,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space6),
    ) {
        OrderStatusHeader(state = state)
        OrderProgress(steps = state.steps)
        OrderSummary(
            items = state.items,
            totalAmount = state.totalAmount,
        )
    }
}

@Composable
private fun OrderStatusHeader(
    state: OrderStatusUiState.Content,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space3)) {
        Text(
            text = state.headerTitle,
            style = CafeTheme.typography.display,
            color = CafeTheme.colors.ink,
        )
        Text(
            text = "주문번호 ${state.orderNumber}",
            style = CafeTheme.typography.h2,
            color = CafeTheme.colors.primary,
        )
        Text(
            text = state.statusMessage,
            style = CafeTheme.typography.body,
            color = CafeTheme.colors.body,
        )
    }
}

@Composable
private fun OrderProgress(
    steps: List<OrderStatusStepUiModel>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space4)) {
        Text(
            text = "진행 상태",
            style = CafeTheme.typography.h2,
            color = CafeTheme.colors.ink,
        )

        Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space3)) {
            steps.forEach { step ->
                OrderProgressStep(step = step)
            }
        }
    }
}

@Composable
private fun OrderProgressStep(
    step: OrderStatusStepUiModel,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing
    val stepColor = when (step.state) {
        OrderStatusStepState.Completed -> colors.success
        OrderStatusStepState.Current -> colors.primary
        OrderStatusStepState.Upcoming -> colors.hairline
    }
    val labelColor = when (step.state) {
        OrderStatusStepState.Completed -> colors.success
        OrderStatusStepState.Current -> colors.primary
        OrderStatusStepState.Upcoming -> colors.muted
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.space3),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = spacing.space1)
                .size(spacing.space3)
                .background(stepColor, CircleShape),
        )

        Column(
            modifier = Modifier.weight(ProgressTextWeight),
            verticalArrangement = Arrangement.spacedBy(spacing.space1),
        ) {
            Text(
                text = step.label,
                style = CafeTheme.typography.h3,
                color = labelColor,
            )
            Text(
                text = step.description,
                style = CafeTheme.typography.body,
                color = colors.body,
            )
        }
    }
}

@Composable
private fun OrderSummary(
    items: List<CartItem>,
    totalAmount: Int,
) {
    val spacing = CafeTheme.spacing

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
                    text = "주문 내역",
                    style = CafeTheme.typography.h2,
                    color = CafeTheme.colors.ink,
                )
                Text(
                    text = formatWon(totalAmount),
                    style = CafeTheme.typography.h3,
                    color = CafeTheme.colors.primary,
                )
            }

            items.forEachIndexed { index, item ->
                if (index > FirstItemIndex) {
                    Spacer(modifier = Modifier.height(spacing.space1))
                }
                OrderSummaryItem(item = item)
            }
        }
    }
}

@Composable
private fun OrderSummaryItem(
    item: CartItem,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(SummaryNameWeight),
            verticalArrangement = Arrangement.spacedBy(spacing.space1),
        ) {
            Text(
                text = item.name,
                style = CafeTheme.typography.h3,
                color = colors.ink,
            )
            Text(
                text = item.selectedOptions.summaryText(),
                style = CafeTheme.typography.caption,
                color = colors.muted,
            )
        }

        Spacer(modifier = Modifier.width(spacing.space3))

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(spacing.space1),
        ) {
            Text(
                text = "${item.quantity}잔",
                style = CafeTheme.typography.caption,
                color = colors.muted,
            )
            Text(
                text = formatWon(item.unitPrice * item.quantity),
                style = CafeTheme.typography.bodyL,
                color = colors.primary,
            )
        }
    }
}

@Composable
private fun screenPadding(): PaddingValues =
    PaddingValues(
        start = CafeTheme.spacing.space5,
        top = CafeTheme.spacing.space6,
        end = CafeTheme.spacing.space5,
        bottom = CafeTheme.spacing.space6,
    )

private fun List<SelectedOption>.summaryText(): String =
    if (isEmpty()) {
        "기본 옵션"
    } else {
        joinToString(separator = ", ") { option -> option.name }
    }

private fun formatWon(amount: Int): String =
    "${NumberFormat.getNumberInstance(Locale.KOREA).format(amount)}원"

private const val ProgressTextWeight = 1f
private const val SummaryNameWeight = 1f
private const val FirstItemIndex = 0
