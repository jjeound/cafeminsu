package com.cafeminsu.ui.feature.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafeminsu.ui.components.CafeCard
import com.cafeminsu.ui.components.CafeCardType
import com.cafeminsu.ui.components.CafeChip
import com.cafeminsu.ui.components.EmptyView
import com.cafeminsu.ui.components.ErrorView
import com.cafeminsu.ui.components.LoadingView
import com.cafeminsu.ui.theme.CafeTheme

@Composable
fun MenuRoute(
    onMenuClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MenuViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    MenuScreen(
        state = state,
        onCategorySelect = viewModel::onCategorySelect,
        onMenuClick = onMenuClick,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
fun MenuScreen(
    state: MenuUiState,
    onCategorySelect: (String) -> Unit,
    onMenuClick: (String) -> Unit,
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
                .padding(
                    start = CafeTheme.spacing.space5,
                    top = CafeTheme.spacing.space6,
                    end = CafeTheme.spacing.space5,
                    bottom = CafeTheme.spacing.space6,
                ),
            verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space5),
        ) {
            Text(
                text = "메뉴",
                style = CafeTheme.typography.h1,
                color = CafeTheme.colors.ink,
            )

            when (state) {
                MenuUiState.Loading -> LoadingView()
                is MenuUiState.Content -> MenuContent(
                    state = state,
                    onCategorySelect = onCategorySelect,
                    onMenuClick = onMenuClick,
                )

                is MenuUiState.Empty -> MenuEmpty(
                    state = state,
                    onCategorySelect = onCategorySelect,
                )

                is MenuUiState.Error -> ErrorView(
                    message = state.message,
                    retryable = state.retryable,
                    onRetry = onRetry,
                )
            }
        }
    }
}

@Composable
private fun MenuContent(
    state: MenuUiState.Content,
    onCategorySelect: (String) -> Unit,
    onMenuClick: (String) -> Unit,
) {
    MenuCategoryTabs(
        categories = state.categories,
        selectedCategoryId = state.selectedCategoryId,
        onCategorySelect = onCategorySelect,
    )

    MenuGrid(
        menus = state.menus,
        onMenuClick = onMenuClick,
    )
}

@Composable
private fun MenuEmpty(
    state: MenuUiState.Empty,
    onCategorySelect: (String) -> Unit,
) {
    if (state.categories.isNotEmpty()) {
        MenuCategoryTabs(
            categories = state.categories,
            selectedCategoryId = state.selectedCategoryId,
            onCategorySelect = onCategorySelect,
        )
    }

    EmptyView(
        message = state.message,
        actionLabel = null,
        onAction = null,
    )
}

@Composable
private fun MenuCategoryTabs(
    categories: List<MenuCategoryUiModel>,
    selectedCategoryId: String?,
    onCategorySelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space2),
    ) {
        categories.forEach { category ->
            CafeChip(
                text = category.name,
                selected = category.id == selectedCategoryId,
                onClick = { onCategorySelect(category.id) },
            )
        }
    }
}

@Composable
private fun MenuGrid(
    menus: List<MenuItemUiModel>,
    onMenuClick: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(MenuGridColumns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = CafeTheme.spacing.space6),
        horizontalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space4),
        verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space4),
    ) {
        items(
            items = menus,
            key = { it.id },
        ) { menu ->
            MenuProductCard(
                menu = menu,
                onClick = { onMenuClick(menu.id) },
            )
        }
    }
}

@Composable
private fun MenuProductCard(
    menu: MenuItemUiModel,
    onClick: () -> Unit,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    CafeCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(spacing.space18 * ProductCardHeightMultiplier + spacing.space4)
            .clickable(
                enabled = !menu.isSoldOut,
                onClick = onClick,
            ),
        type = CafeCardType.Product,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        modifier = Modifier.weight(ProductNameWeight),
                        text = menu.name,
                        style = CafeTheme.typography.h3,
                        color = colors.onDark,
                        maxLines = ProductNameMaxLines,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (menu.isSoldOut) {
                        Spacer(modifier = Modifier.width(spacing.space2))
                        SoldOutBadge()
                    }
                }

                Text(
                    text = menu.description,
                    style = CafeTheme.typography.body,
                    color = colors.onDark,
                    maxLines = ProductDescriptionMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = "${menu.price}원",
                style = CafeTheme.typography.caption,
                color = colors.primary,
            )
        }
    }
}

@Composable
private fun SoldOutBadge() {
    Surface(
        shape = CafeTheme.shapes.radiusSm,
        color = CafeTheme.colors.accentSoft,
        contentColor = CafeTheme.colors.primary,
    ) {
        Text(
            modifier = Modifier.padding(
                horizontal = CafeTheme.spacing.space2,
                vertical = CafeTheme.spacing.space1,
            ),
            text = "품절",
            style = CafeTheme.typography.caption,
            color = CafeTheme.colors.primary,
        )
    }
}

private const val MenuGridColumns = 2
private const val ProductCardHeightMultiplier = 2
private const val ProductNameWeight = 1f
private const val ProductNameMaxLines = 2
private const val ProductDescriptionMaxLines = 2
