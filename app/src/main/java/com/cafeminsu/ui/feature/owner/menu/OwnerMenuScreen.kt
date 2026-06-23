package com.cafeminsu.ui.feature.owner.menu

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafeminsu.ui.components.CafeChip
import com.cafeminsu.ui.components.EmptyView
import com.cafeminsu.ui.components.ErrorView
import com.cafeminsu.ui.components.LoadingView
import com.cafeminsu.ui.theme.CafeTheme
import java.text.NumberFormat
import java.util.Locale

@Composable
fun OwnerMenuRoute(
    onAddMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OwnerMenuViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CafeTheme.colors.canvas,
        // 부모 AppNavHost Scaffold 가 이미 시스템바 인셋을 적용하므로 여기서 중복 적용하지 않는다.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = CafeTheme.colors.surfaceDark,
                    contentColor = CafeTheme.colors.onDark,
                    actionColor = CafeTheme.colors.primary,
                    shape = CafeTheme.shapes.radiusMd,
                )
            }
        },
    ) { innerPadding ->
        OwnerMenuScreen(
            state = state,
            onFilterSelected = viewModel::selectFilter,
            onSoldOutClick = viewModel::setSoldOut,
            onAddMenuClick = onAddMenuClick,
            onRetry = viewModel::retry,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
fun OwnerMenuScreen(
    state: OwnerMenuUiState,
    onFilterSelected: (OwnerMenuFilter) -> Unit,
    onSoldOutClick: (String) -> Unit,
    onAddMenuClick: () -> Unit,
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
                OwnerMenuUiState.Loading -> LoadingView()
                is OwnerMenuUiState.Content -> OwnerMenuContent(
                    filters = state.filters,
                    menus = state.menus,
                    onFilterSelected = onFilterSelected,
                    onSoldOutClick = onSoldOutClick,
                    onAddMenuClick = onAddMenuClick,
                )

                is OwnerMenuUiState.Empty -> OwnerMenuEmpty(
                    filters = state.filters,
                    message = state.message,
                    onFilterSelected = onFilterSelected,
                    onAddMenuClick = onAddMenuClick,
                )

                is OwnerMenuUiState.Error -> ErrorView(
                    message = state.message,
                    retryable = state.retryable,
                    onRetry = onRetry,
                )
            }
        }
    }
}

@Composable
private fun OwnerMenuContent(
    filters: List<OwnerMenuFilterUiModel>,
    menus: List<OwnerMenuItemUiModel>,
    onFilterSelected: (OwnerMenuFilter) -> Unit,
    onSoldOutClick: (String) -> Unit,
    onAddMenuClick: () -> Unit,
) {
    OwnerMenuHeader(onAddMenuClick = onAddMenuClick)
    OwnerMenuFilters(filters = filters, onFilterSelected = onFilterSelected)
    menus.forEach { menu ->
        OwnerMenuRow(
            menu = menu,
            onSoldOutClick = onSoldOutClick,
        )
    }
}

@Composable
private fun OwnerMenuEmpty(
    filters: List<OwnerMenuFilterUiModel>,
    message: String,
    onFilterSelected: (OwnerMenuFilter) -> Unit,
    onAddMenuClick: () -> Unit,
) {
    OwnerMenuHeader(onAddMenuClick = onAddMenuClick)
    OwnerMenuFilters(filters = filters, onFilterSelected = onFilterSelected)
    EmptyView(
        message = message,
        actionLabel = null,
        onAction = null,
    )
}

@Composable
private fun OwnerMenuHeader(
    onAddMenuClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(HeaderTextWeight),
            text = "메뉴 관리",
            style = CafeTheme.typography.h1,
            color = CafeTheme.colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Surface(
            modifier = Modifier.clickable(
                role = Role.Button,
                onClick = onAddMenuClick,
            ),
            color = Color.Transparent,
            contentColor = CafeTheme.colors.primary,
        ) {
            Box(
                modifier = Modifier.padding(
                    horizontal = CafeTheme.spacing.space2,
                    vertical = CafeTheme.spacing.space2,
                ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+ 메뉴 추가",
                    style = CafeTheme.typography.body,
                    color = CafeTheme.colors.primary,
                )
            }
        }
    }
}

@Composable
private fun OwnerMenuFilters(
    filters: List<OwnerMenuFilterUiModel>,
    onFilterSelected: (OwnerMenuFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space2),
    ) {
        filters.forEach { filter ->
            CafeChip(
                text = filter.label,
                selected = filter.selected,
                onClick = { onFilterSelected(filter.filter) },
            )
        }
    }
}

@Composable
private fun OwnerMenuRow(
    menu: OwnerMenuItemUiModel,
    onSoldOutClick: (String) -> Unit,
) {
    val colors = CafeTheme.colors
    val titleColor = if (menu.isDimmed) colors.muted else colors.ink
    val priceColor = if (menu.isDimmed) colors.muted else colors.primary
    val statusColor = if (menu.isSoldOut) colors.error else colors.success

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CafeTheme.shapes.radiusLg,
        color = colors.surfaceCard,
        contentColor = colors.body,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = CafeTheme.spacing.space4,
                    vertical = CafeTheme.spacing.space4,
                ),
            horizontalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(HeaderTextWeight),
                verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space2),
            ) {
                Text(
                    text = menu.name,
                    style = CafeTheme.typography.h3,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space3),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = menu.price.toWonLabel(),
                        style = CafeTheme.typography.caption,
                        color = priceColor,
                    )
                    OwnerMenuStatus(
                        label = menu.statusLabel,
                        color = statusColor,
                    )
                }
            }

            Switch(
                modifier = Modifier.semantics {
                    contentDescription = "${menu.name} 판매 상태"
                },
                checked = !menu.isSoldOut,
                enabled = !menu.isActionInProgress,
                onCheckedChange = { onSoldOutClick(menu.id) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.onPrimary,
                    checkedTrackColor = colors.primary,
                    checkedBorderColor = colors.primary,
                    uncheckedThumbColor = colors.canvas,
                    uncheckedTrackColor = colors.hairline,
                    uncheckedBorderColor = colors.hairline,
                    disabledCheckedThumbColor = colors.muted,
                    disabledCheckedTrackColor = colors.hairline,
                    disabledUncheckedThumbColor = colors.muted,
                    disabledUncheckedTrackColor = colors.hairline,
                ),
            )
        }
    }
}

@Composable
private fun OwnerMenuStatus(
    label: String,
    color: Color,
) {
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
private fun StatusDot(color: Color) {
    Canvas(modifier = Modifier.size(CafeTheme.spacing.space2)) {
        drawCircle(color = color)
    }
}

private fun Int.toWonLabel(): String =
    "₩${NumberFormat.getNumberInstance(Locale.KOREA).format(this)}"

private const val HeaderTextWeight = 1f
