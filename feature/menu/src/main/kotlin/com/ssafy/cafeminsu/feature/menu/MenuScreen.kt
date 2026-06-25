package com.ssafy.cafeminsu.feature.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.cafeminsu.core.designsystem.theme.CafeMinsuTheme

@Composable
fun MenuRoute(
    onVoiceClick: () -> Unit = {},
    onCartClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onMenuClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MenuViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MenuScreen(
        state = uiState,
        onCategoryClick = viewModel::onCategoryClick,
        onMenuClick = onMenuClick,
        onBackClick = onBackClick,
        onCartClick = onCartClick,
        modifier = modifier,
    )
}

@Composable
fun MenuScreen(
    state: MenuUiState,
    onCategoryClick: (String) -> Unit,
    onMenuClick: (Long) -> Unit,
    onBackClick: () -> Unit,
    onCartClick: () -> Unit,
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
        verticalArrangement = Arrangement.spacedBy(spacing.space4),
    ) {
        MenuHeader(
            cartCount = state.cartCount,
            onBackClick = onBackClick,
            onCartClick = onCartClick,
        )

        MenuCategoryTabs(
            categories = state.categories,
            selectedCategoryId = state.selectedCategoryId,
            onCategoryClick = onCategoryClick,
        )

        if (state.menus.isEmpty()) {
            EmptyMenuCard()
        } else {
            state.menus.forEach { menu ->
                MenuListItem(
                    menu = menu,
                    onClick = { onMenuClick(menu.id) },
                )
            }
        }
    }
}

@Composable
private fun MenuHeader(
    cartCount: Int,
    onBackClick: () -> Unit,
    onCartClick: () -> Unit,
) {
    val colors = CafeMinsuTheme.colors
    val spacing = CafeMinsuTheme.spacing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "뒤로가기",
                tint = colors.ink,
            )
        }

        Text(
            text = "메뉴",
            modifier = Modifier
                .weight(1f)
                .padding(start = spacing.space2),
            style = CafeMinsuTheme.typography.h1,
            color = colors.ink,
        )

        CartButton(
            cartCount = cartCount,
            onClick = onCartClick,
        )
    }
}

@Composable
private fun CartButton(
    cartCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeMinsuTheme.colors

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(colors.surfaceCard)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.ShoppingCart,
            contentDescription = "장바구니",
            tint = colors.ink,
        )

        if (cartCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .sizeIn(minWidth = 18.dp, minHeight = 18.dp)
                    .clip(CircleShape)
                    .background(colors.primary)
                    .padding(horizontal = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = cartCount.coerceAtMost(99).toString(),
                    style = CafeMinsuTheme.typography.caption,
                    color = colors.onPrimary,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MenuCategoryTabs(
    categories: List<MenuCategoryUiModel>,
    selectedCategoryId: String,
    onCategoryClick: (String) -> Unit,
) {
    val colors = CafeMinsuTheme.colors
    val spacing = CafeMinsuTheme.spacing

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(spacing.space2),
        verticalArrangement = Arrangement.spacedBy(spacing.space2),
    ) {
        categories.forEach { category ->
            val selected = category.id == selectedCategoryId

            Surface(
                modifier = Modifier.clickable(
                    onClick = { onCategoryClick(category.id) },
                ),
                shape = CafeMinsuTheme.shapes.radiusPill,
                color = if (selected) colors.surfaceDark else colors.surfaceCard,
            ) {
                Text(
                    text = category.label,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = CafeMinsuTheme.typography.caption.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = if (selected) colors.onDark else colors.ink,
                )
            }
        }
    }
}

@Composable
private fun MenuListItem(
    menu: MenuUiModel,
    onClick: () -> Unit,
) {
    val colors = CafeMinsuTheme.colors

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = !menu.soldOut,
                onClick = onClick,
            ),
        color = colors.surfaceCard,
        shape = CafeMinsuTheme.shapes.radiusLg,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MenuThumbnail(soldOut = menu.soldOut)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = menu.name,
                        style = CafeMinsuTheme.typography.h3,
                        color = colors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (menu.soldOut) {
                        Spacer(modifier = Modifier.width(8.dp))
                        SoldOutBadge()
                    }
                }

                Text(
                    text = menu.description,
                    style = CafeMinsuTheme.typography.caption,
                    color = colors.muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = menu.priceLabel,
                    style = CafeMinsuTheme.typography.bodyL.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = colors.ink,
                )
            }
        }
    }
}

@Composable
private fun EmptyMenuCard() {
    val colors = CafeMinsuTheme.colors

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surfaceCard,
        shape = CafeMinsuTheme.shapes.radiusLg,
    ) {
        Text(
            text = "표시할 메뉴가 없어요.",
            modifier = Modifier.padding(16.dp),
            style = CafeMinsuTheme.typography.body,
            color = colors.muted,
        )
    }
}

@Composable
private fun MenuThumbnail(
    soldOut: Boolean,
) {
    val colors = CafeMinsuTheme.colors

    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CafeMinsuTheme.shapes.radiusMd)
            .background(if (soldOut) colors.hairline else colors.accentSoft),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (soldOut) "✕" else "☕",
            style = CafeMinsuTheme.typography.h2,
            color = if (soldOut) colors.mutedSoft else colors.primaryHover,
        )
    }
}

@Composable
private fun SoldOutBadge() {
    val colors = CafeMinsuTheme.colors

    Surface(
        color = colors.error,
        shape = CafeMinsuTheme.shapes.radiusPill,
    ) {
        Text(
            text = "품절",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = CafeMinsuTheme.typography.caption,
            color = colors.onPrimary,
        )
    }
}