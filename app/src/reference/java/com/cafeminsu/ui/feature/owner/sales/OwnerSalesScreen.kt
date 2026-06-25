package com.cafeminsu.ui.feature.owner.sales

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafeminsu.domain.model.SalesPeriod
import com.cafeminsu.ui.components.EmptyView
import com.cafeminsu.ui.components.ErrorView
import com.cafeminsu.ui.components.LoadingView
import com.cafeminsu.ui.theme.CafeTheme

@Composable
fun OwnerSalesRoute(
    modifier: Modifier = Modifier,
    viewModel: OwnerSalesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    OwnerSalesScreen(
        state = state,
        onPeriodSelected = viewModel::selectPeriod,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
fun OwnerSalesScreen(
    state: OwnerSalesUiState,
    onPeriodSelected: (SalesPeriod) -> Unit,
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
                OwnerSalesUiState.Loading -> LoadingView()
                is OwnerSalesUiState.Content -> OwnerSalesContent(
                    periods = state.periods,
                    summary = state.summary,
                    onPeriodSelected = onPeriodSelected,
                )

                is OwnerSalesUiState.Empty -> OwnerSalesEmpty(
                    periods = state.periods,
                    message = state.message,
                    onPeriodSelected = onPeriodSelected,
                )

                is OwnerSalesUiState.Error -> ErrorView(
                    message = state.message,
                    retryable = state.retryable,
                    onRetry = onRetry,
                )
            }
        }
    }
}

@Composable
private fun OwnerSalesContent(
    periods: List<OwnerSalesPeriodUiModel>,
    summary: OwnerSalesSummaryUiModel,
    onPeriodSelected: (SalesPeriod) -> Unit,
) {
    OwnerSalesHeader()
    OwnerSalesPeriodSegment(periods = periods, onPeriodSelected = onPeriodSelected)
    OwnerSalesHero(summary = summary)
    OwnerSalesChartCard(bars = summary.bars)
    OwnerSalesTopMenus(topMenus = summary.topMenus)
    OwnerSalesPayoutCard(
        payoutAmountLabel = summary.payoutAmountLabel,
        payoutDateLabel = summary.payoutDateLabel,
    )
}

@Composable
private fun OwnerSalesEmpty(
    periods: List<OwnerSalesPeriodUiModel>,
    message: String,
    onPeriodSelected: (SalesPeriod) -> Unit,
) {
    OwnerSalesHeader()
    OwnerSalesPeriodSegment(periods = periods, onPeriodSelected = onPeriodSelected)
    EmptyView(
        message = message,
        actionLabel = null,
        onAction = null,
    )
}

@Composable
private fun OwnerSalesHeader() {
    Text(
        text = "매출 · 정산",
        style = CafeTheme.typography.h1,
        color = CafeTheme.colors.ink,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun OwnerSalesPeriodSegment(
    periods: List<OwnerSalesPeriodUiModel>,
    onPeriodSelected: (SalesPeriod) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CafeTheme.shapes.radiusPill,
        color = CafeTheme.colors.surfaceCard,
        contentColor = CafeTheme.colors.body,
    ) {
        Row(
            modifier = Modifier.padding(CafeTheme.spacing.space1),
            horizontalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space1),
        ) {
            periods.forEach { period ->
                OwnerSalesPeriodItem(
                    period = period,
                    onPeriodSelected = onPeriodSelected,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun OwnerSalesPeriodItem(
    period: OwnerSalesPeriodUiModel,
    onPeriodSelected: (SalesPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val containerColor = if (period.selected) colors.onPrimary else Color.Transparent
    val contentColor = if (period.selected) colors.ink else colors.muted

    Surface(
        modifier = modifier.clickable(
            role = Role.Tab,
            onClick = { onPeriodSelected(period.period) },
        ),
        shape = CafeTheme.shapes.radiusPill,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Box(
            modifier = Modifier
                .height(CafeTheme.spacing.space10)
                .padding(horizontal = CafeTheme.spacing.space2),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = period.label,
                style = CafeTheme.typography.body,
                color = contentColor,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun OwnerSalesHero(summary: OwnerSalesSummaryUiModel) {
    Column(
        verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space1),
    ) {
        Text(
            text = summary.periodSalesLabel,
            style = CafeTheme.typography.caption,
            color = CafeTheme.colors.muted,
        )
        Text(
            text = summary.totalSalesLabel,
            style = CafeTheme.typography.display,
            color = CafeTheme.colors.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = summary.deltaLabel,
            style = CafeTheme.typography.caption,
            color = summary.deltaTone.toColor(),
        )
    }
}

@Composable
private fun OwnerSalesChartCard(
    bars: List<OwnerSalesBarUiModel>,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CafeTheme.shapes.radiusLg,
        color = CafeTheme.colors.surfaceCard,
        contentColor = CafeTheme.colors.body,
    ) {
        Column(
            modifier = Modifier.padding(CafeTheme.spacing.space4),
            verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space4),
        ) {
            Text(
                text = "요일별 매출",
                style = CafeTheme.typography.caption,
                color = CafeTheme.colors.ink,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CafeTheme.spacing.space18 + CafeTheme.spacing.space6),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                bars.forEach { bar ->
                    OwnerSalesBar(bar = bar)
                }
            }
        }
    }
}

@Composable
private fun OwnerSalesBar(bar: OwnerSalesBarUiModel) {
    val colors = CafeTheme.colors
    val barColor = if (bar.highlighted) colors.primary else colors.accentSoft
    val barHeightWeight = bar.ratio.coerceIn(0f, 1f).takeIf { it > 0f } ?: MinimumBarRatio

    Column(
        modifier = Modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .width(CafeTheme.spacing.space5),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight(barHeightWeight)
                    .width(CafeTheme.spacing.space5)
                    .background(
                        color = barColor,
                        shape = CafeTheme.shapes.radiusSm,
                    ),
            )
        }
        Spacer(modifier = Modifier.height(CafeTheme.spacing.space2))
        Text(
            text = bar.label,
            style = CafeTheme.typography.meta,
            color = colors.muted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun OwnerSalesTopMenus(
    topMenus: List<OwnerSalesTopMenuUiModel>,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space3),
    ) {
        Text(
            text = "인기 메뉴",
            style = CafeTheme.typography.h2,
            color = CafeTheme.colors.ink,
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = CafeTheme.shapes.radiusLg,
            color = CafeTheme.colors.surfaceCard,
            contentColor = CafeTheme.colors.body,
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = CafeTheme.spacing.space4,
                    vertical = CafeTheme.spacing.space3,
                ),
                verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space3),
            ) {
                topMenus.forEach { menu ->
                    OwnerSalesTopMenuRow(menu = menu)
                }
            }
        }
    }
}

@Composable
private fun OwnerSalesTopMenuRow(menu: OwnerSalesTopMenuUiModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = CafeTheme.spacing.space10),
        horizontalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = menu.rankLabel,
            style = CafeTheme.typography.bodyL,
            color = CafeTheme.colors.primary,
        )
        Column(
            modifier = Modifier.weight(ContentWeight),
            verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space1),
        ) {
            Text(
                text = menu.name,
                style = CafeTheme.typography.h3,
                color = CafeTheme.colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = menu.soldCountLabel,
                style = CafeTheme.typography.caption,
                color = CafeTheme.colors.muted,
            )
        }
        Text(
            text = menu.salesLabel,
            style = CafeTheme.typography.bodyL,
            color = CafeTheme.colors.ink,
            maxLines = 1,
        )
    }
}

@Composable
private fun OwnerSalesPayoutCard(
    payoutAmountLabel: String,
    payoutDateLabel: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CafeTheme.shapes.radiusLg,
        color = CafeTheme.colors.surfaceDark,
        contentColor = CafeTheme.colors.onDark,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = CafeTheme.spacing.space4,
                vertical = CafeTheme.spacing.space4,
            ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(ContentWeight),
                verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space1),
            ) {
                Text(
                    text = "정산 예정 금액",
                    style = CafeTheme.typography.body,
                    color = CafeTheme.colors.muted,
                )
                Text(
                    text = payoutDateLabel,
                    style = CafeTheme.typography.caption,
                    color = CafeTheme.colors.muted,
                )
            }
            Text(
                text = payoutAmountLabel,
                style = CafeTheme.typography.h2,
                color = CafeTheme.colors.onDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun OwnerSalesDeltaTone.toColor(): Color =
    when (this) {
        OwnerSalesDeltaTone.Positive -> CafeTheme.colors.success
        OwnerSalesDeltaTone.Negative -> CafeTheme.colors.error
        OwnerSalesDeltaTone.Neutral -> CafeTheme.colors.muted
    }

private const val ContentWeight = 1f
private const val MinimumBarRatio = 0.08f
