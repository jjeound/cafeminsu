package com.cafeminsu.ui.feature.owner.orders

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.ui.components.CafeChip
import com.cafeminsu.ui.components.EmptyView
import com.cafeminsu.ui.components.ErrorView
import com.cafeminsu.ui.components.LoadingView
import com.cafeminsu.ui.theme.CafeTheme
import java.text.NumberFormat
import java.util.Locale

@Composable
fun OwnerOrdersRoute(
    modifier: Modifier = Modifier,
    viewModel: OwnerOrdersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    OwnerOrdersScreen(
        state = state,
        onFilterSelected = viewModel::selectFilter,
        onAdvanceStatus = viewModel::advanceStatus,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
fun OwnerOrdersScreen(
    state: OwnerOrdersUiState,
    onFilterSelected: (OwnerOrdersFilter) -> Unit,
    onAdvanceStatus: (String) -> Unit,
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
                    bottom = CafeTheme.spacing.space8,
                ),
            verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space4),
        ) {
            when (state) {
                OwnerOrdersUiState.Loading -> LoadingView()
                is OwnerOrdersUiState.Content -> OwnerOrdersContent(
                    selectedFilter = state.selectedFilter,
                    counts = state.counts,
                    orders = state.orders,
                    onFilterSelected = onFilterSelected,
                    onAdvanceStatus = onAdvanceStatus,
                )

                is OwnerOrdersUiState.Empty -> OwnerOrdersEmpty(
                    selectedFilter = state.selectedFilter,
                    counts = state.counts,
                    message = state.message,
                    onFilterSelected = onFilterSelected,
                )

                is OwnerOrdersUiState.Error -> ErrorView(
                    message = state.message,
                    retryable = state.retryable,
                    onRetry = onRetry,
                )
            }
        }
    }
}

@Composable
private fun OwnerOrdersContent(
    selectedFilter: OwnerOrdersFilter,
    counts: OwnerOrdersCountsUiModel,
    orders: List<OwnerOrdersOrderUiModel>,
    onFilterSelected: (OwnerOrdersFilter) -> Unit,
    onAdvanceStatus: (String) -> Unit,
) {
    OwnerOrdersHeader()
    OwnerOrdersFilters(
        selectedFilter = selectedFilter,
        counts = counts,
        onFilterSelected = onFilterSelected,
    )
    orders.forEach { order ->
        OwnerOrdersCard(
            order = order,
            onAdvanceStatus = onAdvanceStatus,
        )
    }
}

@Composable
private fun OwnerOrdersEmpty(
    selectedFilter: OwnerOrdersFilter,
    counts: OwnerOrdersCountsUiModel,
    message: String,
    onFilterSelected: (OwnerOrdersFilter) -> Unit,
) {
    OwnerOrdersHeader()
    OwnerOrdersFilters(
        selectedFilter = selectedFilter,
        counts = counts,
        onFilterSelected = onFilterSelected,
    )
    EmptyView(
        message = message,
        actionLabel = null,
        onAction = null,
    )
}

@Composable
private fun OwnerOrdersHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(HeaderTextWeight),
            text = "주문 관리",
            style = CafeTheme.typography.h1,
            color = CafeTheme.colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space1),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(color = CafeTheme.colors.success)
            Text(
                text = "실시간",
                style = CafeTheme.typography.caption,
                color = CafeTheme.colors.body,
            )
        }
    }
}

@Composable
private fun OwnerOrdersFilters(
    selectedFilter: OwnerOrdersFilter,
    counts: OwnerOrdersCountsUiModel,
    onFilterSelected: (OwnerOrdersFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space2),
    ) {
        CafeChip(
            text = "신규 ${counts.newCount}",
            selected = selectedFilter == OwnerOrdersFilter.New,
            onClick = { onFilterSelected(OwnerOrdersFilter.New) },
        )
        CafeChip(
            text = "준비중 ${counts.preparingCount}",
            selected = selectedFilter == OwnerOrdersFilter.Preparing,
            onClick = { onFilterSelected(OwnerOrdersFilter.Preparing) },
        )
        CafeChip(
            text = "준비완료 ${counts.readyCount}",
            selected = selectedFilter == OwnerOrdersFilter.Ready,
            onClick = { onFilterSelected(OwnerOrdersFilter.Ready) },
        )
    }
}

@Composable
private fun OwnerOrdersCard(
    order: OwnerOrdersOrderUiModel,
    onAdvanceStatus: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CafeTheme.shapes.radiusLg,
        color = CafeTheme.colors.surfaceCard,
        contentColor = CafeTheme.colors.body,
    ) {
        Column(
            modifier = Modifier.padding(CafeTheme.spacing.space4),
            verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space3),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(
                    modifier = Modifier.weight(HeaderTextWeight),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = order.orderNumberLabel,
                        style = CafeTheme.typography.h3,
                        color = CafeTheme.colors.ink,
                    )
                    Text(
                        text = "  ${order.timeLabel}",
                        style = CafeTheme.typography.caption,
                        color = CafeTheme.colors.muted,
                    )
                }
                OwnerOrdersStatus(status = order.status, label = order.statusLabel)
            }

            Text(
                text = order.itemsLabel,
                style = CafeTheme.typography.body,
                color = CafeTheme.colors.body,
            )

            Text(
                text = order.requestLabel,
                style = CafeTheme.typography.caption,
                color = CafeTheme.colors.muted,
            )

            Spacer(modifier = Modifier.height(CafeTheme.spacing.space1))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = order.totalAmount.toWonLabel(),
                    style = CafeTheme.typography.bodyL,
                    color = CafeTheme.colors.primary,
                )
                OwnerOrdersActionButton(
                    text = order.actionLabel,
                    enabled = !order.isActionInProgress,
                    onClick = { onAdvanceStatus(order.id) },
                )
            }
        }
    }
}

@Composable
private fun OwnerOrdersStatus(
    status: OrderStatus,
    label: String,
) {
    val color = when (status) {
        OrderStatus.Accepted -> CafeTheme.colors.warning
        OrderStatus.Preparing -> CafeTheme.colors.primary
        OrderStatus.Ready -> CafeTheme.colors.success
        OrderStatus.PendingPayment,
        OrderStatus.Paid,
        OrderStatus.Completed,
        OrderStatus.Cancelled,
        OrderStatus.Failed,
        -> CafeTheme.colors.muted
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(color = color)
        Text(
            text = label,
            style = CafeTheme.typography.caption,
            color = color,
        )
    }
}

@Composable
private fun OwnerOrdersActionButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing
    val containerColor = if (enabled) colors.primary else colors.hairline
    val contentColor = if (enabled) colors.onPrimary else colors.muted

    Surface(
        modifier = Modifier.clickable(
            enabled = enabled,
            role = Role.Button,
            onClick = onClick,
        ),
        shape = CafeTheme.shapes.radiusMd,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Box(
            modifier = Modifier
                .height(spacing.space8 + spacing.space1)
                .padding(horizontal = spacing.space4),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = CafeTheme.typography.caption,
                color = contentColor,
            )
        }
    }
}

@Composable
private fun StatusDot(color: Color) {
    Canvas(modifier = Modifier.size(CafeTheme.spacing.space2)) {
        drawCircle(color = color)
    }
}

private fun Int.toWonLabel(): String =
    "₩${NumberFormat.getNumberInstance(Locale.KOREA).format(this)}"

private const val HeaderTextWeight = 1f
