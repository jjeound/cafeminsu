package com.cafeminsu.ui.feature.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafeminsu.ui.components.CafeButton
import com.cafeminsu.ui.components.CafeButtonVariant
import com.cafeminsu.ui.components.CafeCard
import com.cafeminsu.ui.components.CafeCardType
import com.cafeminsu.ui.components.CafeTopBar
import com.cafeminsu.ui.components.ErrorView
import com.cafeminsu.ui.components.LoadingView
import com.cafeminsu.ui.theme.CafeTheme

@Composable
fun HistoryRoute(
    onBackClick: () -> Unit,
    onReorderClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is HistoryEvent.NavigateMenuDetail -> onReorderClick(event.menuItemId)
            }
        }
    }

    HistoryScreen(
        state = state,
        onBackClick = onBackClick,
        onRetry = viewModel::retry,
        onReorderClick = viewModel::onReorder,
        modifier = modifier,
    )
}

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    onReorderClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = CafeTheme.colors.canvas,
        contentColor = CafeTheme.colors.body,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CafeTopBar(
                title = "주문내역",
                navigationIcon = { Text(text = "‹") },
                onNavigationClick = onBackClick,
            )

            when (state) {
                HistoryUiState.Loading -> StateContainer {
                    LoadingView()
                }

                is HistoryUiState.Content -> HistoryContent(
                    state = state,
                    onReorderClick = onReorderClick,
                )

                is HistoryUiState.Empty -> HistoryEmpty(state = state)

                is HistoryUiState.Error -> StateContainer {
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
private fun HistoryContent(
    state: HistoryUiState.Content,
    onReorderClick: (String) -> Unit,
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
        verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space4),
    ) {
        state.activeOrder?.let { activeOrder ->
            ActiveOrderCard(order = activeOrder)
            Spacer(modifier = Modifier.height(CafeTheme.spacing.space1))
        }

        Text(
            text = "지난 주문",
            style = CafeTheme.typography.caption,
            color = CafeTheme.colors.muted,
        )

        state.pastOrders.forEach { order ->
            PastOrderCard(
                order = order,
                onReorderClick = { onReorderClick(order.id) },
            )
        }
    }
}

@Composable
private fun ActiveOrderCard(order: HistoryActiveOrderUiModel) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    CafeCard(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        type = CafeCardType.Product,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "진행중인 주문",
                    style = CafeTheme.typography.caption,
                    color = colors.muted,
                )
                Surface(
                    modifier = Modifier.size(spacing.space3),
                    shape = CircleShape,
                    color = colors.success,
                    contentColor = colors.success,
                ) {}
            }

            Text(
                text = order.orderNumber,
                style = CafeTheme.typography.h1,
                color = colors.onDark,
            )

            HistoryStepper(steps = order.steps)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(ContentWeight),
                    text = order.itemSummary,
                    style = CafeTheme.typography.body,
                    color = colors.onDark,
                    maxLines = SummaryMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(spacing.space3))
                Text(
                    text = order.amountLabel,
                    style = CafeTheme.typography.bodyL,
                    color = colors.onDark,
                )
            }
        }
    }
}

@Composable
private fun HistoryStepper(steps: List<HistoryStepUiModel>) {
    Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space2)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            steps.forEachIndexed { index, step ->
                StepDot(
                    step = step,
                    modifier = Modifier.weight(StepWeight),
                )
                if (index != steps.lastIndex) {
                    StepConnector(
                        completed = steps[index + 1].state != HistoryStepState.Upcoming,
                        modifier = Modifier.weight(ConnectorWeight),
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            steps.forEach { step ->
                Text(
                    modifier = Modifier.weight(StepWeight),
                    text = step.label,
                    style = CafeTheme.typography.caption,
                    color = step.labelColor(),
                    textAlign = TextAlign.Center,
                    maxLines = StepLabelMaxLines,
                )
                if (step != steps.last()) {
                    Spacer(modifier = Modifier.weight(ConnectorWeight))
                }
            }
        }
    }
}

@Composable
private fun StepDot(
    step: HistoryStepUiModel,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val dotSize = if (step.state == HistoryStepState.Current) {
        CafeTheme.spacing.space4
    } else {
        CafeTheme.spacing.space3
    }
    val dotColor = when (step.state) {
        HistoryStepState.Completed,
        HistoryStepState.Current,
        -> colors.primary

        HistoryStepState.Upcoming -> colors.muted.copy(alpha = UpcomingAlpha)
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(dotSize),
            shape = CircleShape,
            color = dotColor,
            contentColor = dotColor,
        ) {}
    }
}

@Composable
private fun StepConnector(
    completed: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(StepConnectorHeight)
            .background(
                if (completed) {
                    CafeTheme.colors.primary
                } else {
                    CafeTheme.colors.muted.copy(alpha = UpcomingAlpha)
                },
            ),
    )
}

@Composable
private fun HistoryStepUiModel.labelColor(): Color =
    when (state) {
        HistoryStepState.Completed,
        HistoryStepState.Current,
        -> CafeTheme.colors.onDark

        HistoryStepState.Upcoming -> CafeTheme.colors.muted
    }

@Composable
private fun PastOrderCard(
    order: HistoryPastOrderUiModel,
    onReorderClick: () -> Unit,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    CafeCard(
        modifier = Modifier.fillMaxWidth(),
        type = CafeCardType.Default,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(ContentWeight),
                    verticalArrangement = Arrangement.spacedBy(spacing.space1),
                ) {
                    Text(
                        text = order.storeName,
                        style = CafeTheme.typography.h3,
                        color = colors.ink,
                    )
                    Text(
                        text = order.dateLabel,
                        style = CafeTheme.typography.caption,
                        color = colors.muted,
                    )
                }
                Spacer(modifier = Modifier.width(spacing.space3))
                Text(
                    text = order.amountLabel,
                    style = CafeTheme.typography.bodyL,
                    color = colors.ink,
                )
            }

            Text(
                text = order.itemSummary,
                style = CafeTheme.typography.body,
                color = colors.body,
                maxLines = SummaryMaxLines,
                overflow = TextOverflow.Ellipsis,
            )

            CafeButton(
                text = "↻ 재주문",
                onClick = onReorderClick,
                modifier = Modifier.fillMaxWidth(),
                variant = CafeButtonVariant.Secondary,
                enabled = order.reorderMenuItemId != null,
            )
        }
    }
}

@Composable
private fun HistoryEmpty(state: HistoryUiState.Empty) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = CafeTheme.spacing.space5),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space3),
        ) {
            ReceiptEmptyIcon()
            Text(
                modifier = Modifier.padding(top = CafeTheme.spacing.space3),
                text = state.title,
                style = CafeTheme.typography.h2,
                color = CafeTheme.colors.ink,
                textAlign = TextAlign.Center,
            )
            Text(
                text = state.message,
                style = CafeTheme.typography.body,
                color = CafeTheme.colors.muted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ReceiptEmptyIcon() {
    val colors = CafeTheme.colors

    Surface(
        modifier = Modifier.size(EmptyIconContainerSize),
        shape = CircleShape,
        color = colors.accentSoft,
        contentColor = colors.primary,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = EmptyIconStrokeWidth.toPx()
            val receiptWidth = size.width * ReceiptWidthRatio
            val receiptHeight = size.height * ReceiptHeightRatio
            val left = (size.width - receiptWidth) / CenterDivider
            val top = size.height * ReceiptTopRatio
            val bottom = top + receiptHeight
            val right = left + receiptWidth
            val notchY = bottom - size.height * ReceiptNotchHeightRatio
            val notchWidth = receiptWidth / ReceiptNotchDivider
            val path = Path().apply {
                moveTo(left, top)
                lineTo(right, top)
                lineTo(right, bottom)
                lineTo(right - notchWidth, notchY)
                lineTo(right - notchWidth * ReceiptNotchMiddleMultiplier, bottom)
                lineTo(left + notchWidth * ReceiptNotchMiddleMultiplier, notchY)
                lineTo(left + notchWidth, bottom)
                lineTo(left, notchY)
                close()
            }

            drawPath(
                path = path,
                color = colors.primary,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                ),
            )
            drawLine(
                color = colors.primary,
                start = Offset(
                    x = left + receiptWidth * ReceiptLineStartRatio,
                    y = top + receiptHeight * FirstReceiptLineYRatio,
                ),
                end = Offset(
                    x = right - receiptWidth * ReceiptLineStartRatio,
                    y = top + receiptHeight * FirstReceiptLineYRatio,
                ),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = colors.primary,
                start = Offset(
                    x = left + receiptWidth * ReceiptLineStartRatio,
                    y = top + receiptHeight * SecondReceiptLineYRatio,
                ),
                end = Offset(
                    x = right - receiptWidth * ReceiptLineStartRatio,
                    y = top + receiptHeight * SecondReceiptLineYRatio,
                ),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun StateContainer(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(CafeTheme.spacing.space5),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

private const val ContentWeight = 1f
private const val StepWeight = 1f
private const val ConnectorWeight = 1.2f
private const val UpcomingAlpha = 0.28f
private const val SummaryMaxLines = 1
private const val StepLabelMaxLines = 1
private const val CenterDivider = 2f
private const val ReceiptWidthRatio = 0.36f
private const val ReceiptHeightRatio = 0.44f
private const val ReceiptTopRatio = 0.28f
private const val ReceiptNotchHeightRatio = 0.08f
private const val ReceiptNotchDivider = 4f
private const val ReceiptNotchMiddleMultiplier = 2f
private const val ReceiptLineStartRatio = 0.28f
private const val FirstReceiptLineYRatio = 0.3f
private const val SecondReceiptLineYRatio = 0.5f

private val StepConnectorHeight: Dp = 2.dp
private val EmptyIconContainerSize: Dp = 140.dp
private val EmptyIconStrokeWidth: Dp = 3.dp
