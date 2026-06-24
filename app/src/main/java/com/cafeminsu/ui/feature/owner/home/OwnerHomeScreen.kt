package com.cafeminsu.ui.feature.owner.home

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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.ui.components.EmptyView
import com.cafeminsu.ui.components.ErrorView
import com.cafeminsu.ui.components.LoadingView
import com.cafeminsu.ui.theme.CafeTheme
import java.text.NumberFormat
import java.util.Locale

@Composable
fun OwnerHomeRoute(
    onViewAllOrders: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OwnerHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    OwnerHomeScreen(
        state = state,
        onToggleStoreOpen = viewModel::setStoreOpen,
        onSelectStore = viewModel::selectStore,
        onAdvanceStatus = viewModel::advanceStatus,
        onViewAllOrders = onViewAllOrders,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
fun OwnerHomeScreen(
    state: OwnerHomeUiState,
    onToggleStoreOpen: (Boolean) -> Unit,
    onAdvanceStatus: (String) -> Unit,
    onViewAllOrders: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onSelectStore: (String) -> Unit = {},
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
            verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space5),
        ) {
            when (state) {
                OwnerHomeUiState.Loading -> LoadingView()
                is OwnerHomeUiState.Content -> OwnerHomeContent(
                    storeName = state.storeName,
                    stores = state.stores,
                    isStoreOpen = state.isStoreOpen,
                    dateLabel = state.dateLabel,
                    stats = state.stats,
                    pendingOrders = state.pendingOrders,
                    isStoreOpenUpdating = state.isStoreOpenUpdating,
                    onToggleStoreOpen = onToggleStoreOpen,
                    onSelectStore = onSelectStore,
                    onAdvanceStatus = onAdvanceStatus,
                    onViewAllOrders = onViewAllOrders,
                )

                is OwnerHomeUiState.Empty -> OwnerHomeEmpty(
                    state = state,
                    onToggleStoreOpen = onToggleStoreOpen,
                    onSelectStore = onSelectStore,
                    onViewAllOrders = onViewAllOrders,
                )

                is OwnerHomeUiState.Error -> ErrorView(
                    message = state.message,
                    retryable = state.retryable,
                    onRetry = onRetry,
                )
            }
        }
    }
}

@Composable
private fun OwnerHomeContent(
    storeName: String,
    stores: List<OwnerStoreUiModel>,
    isStoreOpen: Boolean,
    dateLabel: String,
    stats: OwnerHomeStatsUiModel,
    pendingOrders: List<OwnerHomeOrderUiModel>,
    isStoreOpenUpdating: Boolean,
    onToggleStoreOpen: (Boolean) -> Unit,
    onSelectStore: (String) -> Unit,
    onAdvanceStatus: (String) -> Unit,
    onViewAllOrders: () -> Unit,
) {
    OwnerHomeHeader(
        storeName = storeName,
        stores = stores,
        isStoreOpen = isStoreOpen,
        enabled = !isStoreOpenUpdating,
        onToggleStoreOpen = onToggleStoreOpen,
        onSelectStore = onSelectStore,
    )
    OwnerHomeTodayHeader(dateLabel = dateLabel)
    OwnerHomeStatsRow(stats = stats)
    OwnerHomeOrdersSection(
        pendingOrders = pendingOrders,
        onViewAllOrders = onViewAllOrders,
        onAdvanceStatus = onAdvanceStatus,
    )
}

@Composable
private fun OwnerHomeEmpty(
    state: OwnerHomeUiState.Empty,
    onToggleStoreOpen: (Boolean) -> Unit,
    onSelectStore: (String) -> Unit,
    onViewAllOrders: () -> Unit,
) {
    OwnerHomeHeader(
        storeName = state.storeName,
        stores = state.stores,
        isStoreOpen = state.isStoreOpen,
        enabled = !state.isStoreOpenUpdating,
        onToggleStoreOpen = onToggleStoreOpen,
        onSelectStore = onSelectStore,
    )
    OwnerHomeTodayHeader(dateLabel = state.dateLabel)
    OwnerHomeStatsRow(stats = state.stats)
    OwnerHomeSectionHeader(onViewAllOrders = onViewAllOrders)
    EmptyView(
        message = state.message,
        actionLabel = null,
        onAction = null,
    )
}

@Composable
private fun OwnerHomeHeader(
    storeName: String,
    stores: List<OwnerStoreUiModel>,
    isStoreOpen: Boolean,
    enabled: Boolean,
    onToggleStoreOpen: (Boolean) -> Unit,
    onSelectStore: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OwnerStoreSelector(
            storeName = storeName,
            stores = stores,
            onSelectStore = onSelectStore,
            modifier = Modifier.weight(HeaderTextWeight),
        )

        StoreOpenPill(
            isStoreOpen = isStoreOpen,
            enabled = enabled,
            onClick = { onToggleStoreOpen(!isStoreOpen) },
        )
    }
}

@Composable
private fun OwnerStoreSelector(
    storeName: String,
    stores: List<OwnerStoreUiModel>,
    onSelectStore: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 매장이 2개 이상일 때만 드롭다운으로 전환 가능. 1개뿐이면 종전처럼 단순 표시한다.
    val canSwitch = stores.size > 1
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Text(
            modifier = if (canSwitch) {
                Modifier.clickable(role = Role.DropdownList) { expanded = true }
            } else {
                Modifier
            },
            text = "$storeName ▾",
            style = CafeTheme.typography.h1,
            color = CafeTheme.colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        DropdownMenu(
            expanded = expanded && canSwitch,
            onDismissRequest = { expanded = false },
            containerColor = CafeTheme.colors.surfaceCard,
        ) {
            stores.forEach { store ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = store.name,
                            style = CafeTheme.typography.bodyL,
                            color = if (store.isSelected) {
                                CafeTheme.colors.primary
                            } else {
                                CafeTheme.colors.ink
                            },
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelectStore(store.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun StoreOpenPill(
    isStoreOpen: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing
    val clickModifier = if (enabled) {
        Modifier.clickable(role = Role.Switch, onClick = onClick)
    } else {
        Modifier
    }

    Surface(
        modifier = clickModifier,
        shape = CafeTheme.shapes.radiusPill,
        color = colors.surfaceCard,
        contentColor = colors.body,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = spacing.space3,
                vertical = spacing.space2,
            ),
            horizontalArrangement = Arrangement.spacedBy(spacing.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(color = if (isStoreOpen) colors.success else colors.muted)
            Text(
                text = if (isStoreOpen) "영업중" else "영업종료",
                style = CafeTheme.typography.caption,
                color = colors.ink,
            )
        }
    }
}

@Composable
private fun OwnerHomeTodayHeader(dateLabel: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space1),
    ) {
        Text(
            text = dateLabel,
            style = CafeTheme.typography.caption,
            color = CafeTheme.colors.muted,
        )
        Text(
            text = "오늘의 매장 현황",
            style = CafeTheme.typography.h2,
            color = CafeTheme.colors.ink,
        )
    }
}

@Composable
private fun OwnerHomeStatsRow(stats: OwnerHomeStatsUiModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space3),
    ) {
        StatCard(
            label = "오늘 매출",
            value = stats.totalSales.toWonLabel(),
            valueColor = CafeTheme.colors.ink,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = "주문",
            value = "${stats.orderCount}건",
            valueColor = CafeTheme.colors.ink,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = "신규 대기",
            value = "${stats.newWaitingCount}건",
            valueColor = CafeTheme.colors.primary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.heightIn(min = CafeTheme.spacing.space18),
        shape = CafeTheme.shapes.radiusLg,
        color = CafeTheme.colors.surfaceCard,
        contentColor = CafeTheme.colors.body,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = CafeTheme.spacing.space3,
                vertical = CafeTheme.spacing.space3,
            ),
            verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space2),
        ) {
            Text(
                text = label,
                style = CafeTheme.typography.caption,
                color = CafeTheme.colors.muted,
                maxLines = 1,
            )
            Text(
                text = value,
                style = CafeTheme.typography.h2,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun OwnerHomeOrdersSection(
    pendingOrders: List<OwnerHomeOrderUiModel>,
    onViewAllOrders: () -> Unit,
    onAdvanceStatus: (String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space4),
    ) {
        OwnerHomeSectionHeader(onViewAllOrders = onViewAllOrders)

        if (pendingOrders.isEmpty()) {
            EmptyView(
                message = "처리할 주문이 없어요",
                actionLabel = null,
                onAction = null,
            )
        } else {
            pendingOrders.forEach { order ->
                OwnerOrderCard(
                    order = order,
                    onAdvanceStatus = onAdvanceStatus,
                )
            }
        }
    }
}

@Composable
private fun OwnerHomeSectionHeader(onViewAllOrders: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "지금 처리할 주문",
            style = CafeTheme.typography.h2,
            color = CafeTheme.colors.ink,
        )
        Text(
            modifier = Modifier.clickable(role = Role.Button, onClick = onViewAllOrders),
            text = "전체 보기 →",
            style = CafeTheme.typography.caption,
            color = CafeTheme.colors.primary,
        )
    }
}

@Composable
private fun OwnerOrderCard(
    order: OwnerHomeOrderUiModel,
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
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(HeaderTextWeight),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = order.orderNumberLabel,
                        style = CafeTheme.typography.caption,
                        color = CafeTheme.colors.ink,
                    )
                    Text(
                        text = " · ${order.timeLabel}",
                        style = CafeTheme.typography.caption,
                        color = CafeTheme.colors.ink,
                    )
                }
                OwnerOrderStatus(status = order.status, label = order.statusLabel)
            }

            Text(
                text = order.itemSummary,
                style = CafeTheme.typography.body,
                color = CafeTheme.colors.body,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

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
                OwnerOrderActionButton(
                    text = order.actionLabel,
                    enabled = !order.isActionInProgress,
                    onClick = { onAdvanceStatus(order.id) },
                )
            }
        }
    }
}

@Composable
private fun OwnerOrderStatus(
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
private fun OwnerOrderActionButton(
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
