package com.cafeminsu.ui.feature.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafeminsu.R
import com.cafeminsu.ui.components.CafeChip
import com.cafeminsu.ui.components.EmptyView
import com.cafeminsu.ui.components.ErrorView
import com.cafeminsu.ui.components.LoadingView
import com.cafeminsu.ui.theme.CafeTheme
import java.text.NumberFormat
import java.util.Locale

@Composable
fun MenuRoute(
    onMenuClick: (String) -> Unit,
    onVoiceClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MenuViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    MenuScreen(
        state = state,
        onCategorySelect = viewModel::onCategorySelect,
        onMenuClick = onMenuClick,
        onVoiceClick = onVoiceClick,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
fun MenuScreen(
    state: MenuUiState,
    onCategorySelect: (String) -> Unit,
    onMenuClick: (String) -> Unit,
    onVoiceClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = CafeTheme.colors.canvas,
        contentColor = CafeTheme.colors.body,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = CafeTheme.spacing.space5,
                        top = CafeTheme.spacing.space6,
                        end = CafeTheme.spacing.space5,
                    ),
            ) {
                MenuHeader(storeName = state.storeName)

                Spacer(modifier = Modifier.height(CafeTheme.spacing.space3))

                when (state) {
                    MenuUiState.Loading -> LoadingView(modifier = Modifier.fillMaxWidth())
                    is MenuUiState.Content -> MenuContent(
                        state = state,
                        onCategorySelect = onCategorySelect,
                        onMenuClick = onMenuClick,
                        modifier = Modifier.weight(MenuContentWeight),
                    )

                    is MenuUiState.Empty -> MenuEmpty(
                        state = state,
                        onCategorySelect = onCategorySelect,
                        modifier = Modifier.weight(MenuContentWeight),
                    )

                    is MenuUiState.Error -> ErrorView(
                        message = state.message,
                        retryable = state.retryable,
                        onRetry = onRetry,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            VoiceFloatingButton(
                onClick = onVoiceClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = CafeTheme.spacing.space5,
                        bottom = CafeTheme.spacing.space4,
                    ),
            )
        }
    }
}

@Composable
private fun MenuHeader(
    storeName: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(HeaderTitleWeight),
            verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space1),
        ) {
            Text(
                text = storeName,
                style = CafeTheme.typography.h1,
                color = CafeTheme.colors.ink,
            )
            Text(
                text = "오늘의 추천 메뉴",
                style = CafeTheme.typography.caption,
                color = CafeTheme.colors.muted,
            )
        }

        Spacer(modifier = Modifier.width(CafeTheme.spacing.space4))

        Box(
            modifier = Modifier
                .size(CafeTheme.spacing.space10)
                .semantics { contentDescription = "검색" },
            contentAlignment = Alignment.Center,
        ) {
            SearchIcon(modifier = Modifier.size(CafeTheme.spacing.space6))
        }
    }
}

@Composable
private fun MenuContent(
    state: MenuUiState.Content,
    onCategorySelect: (String) -> Unit,
    onMenuClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        MenuCategoryTabs(
            categories = state.categories,
            selectedCategoryId = state.selectedCategoryId,
            onCategorySelect = onCategorySelect,
        )

        Spacer(modifier = Modifier.height(CafeTheme.spacing.space5))

        MenuList(
            menus = state.menus,
            onMenuClick = onMenuClick,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun MenuEmpty(
    state: MenuUiState.Empty,
    onCategorySelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (state.categories.isNotEmpty()) {
            MenuCategoryTabs(
                categories = state.categories,
                selectedCategoryId = state.selectedCategoryId,
                onCategorySelect = onCategorySelect,
            )

            Spacer(modifier = Modifier.height(CafeTheme.spacing.space5))
        }

        EmptyView(
            message = state.message,
            actionLabel = null,
            onAction = null,
        )
    }
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
private fun MenuList(
    menus: List<MenuItemUiModel>,
    onMenuClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = CafeTheme.spacing.space18),
    ) {
        itemsIndexed(
            items = menus,
            key = { _, menu -> menu.id },
        ) { index, menu ->
            MenuListItem(
                menu = menu,
                onClick = { onMenuClick(menu.id) },
            )

            if (index < menus.lastIndex) {
                HorizontalDivider(color = CafeTheme.colors.hairline)
            }
        }
    }
}

@Composable
private fun MenuListItem(
    menu: MenuItemUiModel,
    onClick: () -> Unit,
) {
    val spacing = CafeTheme.spacing
    val itemModifier = Modifier
        .fillMaxWidth()
        .semantics(mergeDescendants = true) {}
        .then(
            if (menu.isEnabled) {
                Modifier.clickable(onClick = onClick)
            } else {
                Modifier
            },
        )
        .alpha(if (menu.isEnabled) EnabledMenuAlpha else DisabledMenuAlpha)
        .padding(vertical = spacing.space4)

    Row(
        modifier = itemModifier,
        horizontalArrangement = Arrangement.spacedBy(spacing.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MenuThumbnail()

        Column(
            modifier = Modifier.weight(MenuInfoWeight),
            verticalArrangement = Arrangement.spacedBy(spacing.space1),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(MenuInfoWeight),
                    text = menu.name,
                    style = CafeTheme.typography.h3,
                    color = CafeTheme.colors.ink,
                    maxLines = MenuNameMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )

                if (menu.isSoldOut) {
                    Spacer(modifier = Modifier.width(spacing.space2))
                    SoldOutBadge()
                }
            }

            Text(
                text = menu.description,
                style = CafeTheme.typography.caption,
                color = CafeTheme.colors.muted,
                maxLines = MenuDescriptionMaxLines,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = menu.price.toPriceLabel(),
                style = CafeTheme.typography.bodyL,
                color = CafeTheme.colors.ink,
            )
        }
    }
}

@Composable
private fun MenuThumbnail(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(CafeTheme.spacing.space18 - CafeTheme.spacing.space2),
        shape = CafeTheme.shapes.radiusSm,
        color = CafeTheme.colors.surfaceCard,
        contentColor = CafeTheme.colors.surfaceCard,
    ) {}
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

@Composable
private fun VoiceFloatingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(CafeTheme.spacing.space10)
            .semantics { contentDescription = "음성 주문" },
        shape = CafeTheme.shapes.radiusPill,
        color = CafeTheme.colors.primary,
        contentColor = CafeTheme.colors.onPrimary,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            MicIcon(modifier = Modifier.size(CafeTheme.spacing.space5))
        }
    }
}

@Composable
private fun SearchIcon(
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(R.drawable.ic_search),
        contentDescription = null,
        tint = CafeTheme.colors.ink,
        modifier = modifier,
    )
}

@Composable
private fun MicIcon(
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(R.drawable.ic_mic),
        contentDescription = null,
        tint = CafeTheme.colors.onPrimary,
        modifier = modifier,
    )
}

private val MenuUiState.storeName: String
    get() = when (this) {
        is MenuUiState.Content -> this.storeName
        is MenuUiState.Empty -> this.storeName
        is MenuUiState.Error,
        MenuUiState.Loading,
        -> DefaultMenuStoreName
    }

private fun Int.toPriceLabel(): String =
    "${NumberFormat.getNumberInstance(Locale.KOREA).format(this)}원"

private const val HeaderTitleWeight = 1f
private const val MenuContentWeight = 1f
private const val MenuInfoWeight = 1f
private const val EnabledMenuAlpha = 1f
private const val DisabledMenuAlpha = 0.42f
private const val MenuNameMaxLines = 1
private const val MenuDescriptionMaxLines = 1
