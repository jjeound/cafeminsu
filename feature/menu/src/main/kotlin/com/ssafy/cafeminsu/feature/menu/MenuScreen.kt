package com.ssafy.cafeminsu.feature.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ssafy.cafeminsu.core.designsystem.component.CafeMinsuButton
import com.ssafy.cafeminsu.core.designsystem.component.CafeMinsuButtonVariant
import com.ssafy.cafeminsu.core.designsystem.theme.CafeMinsuTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Composable
fun MenuRoute(
    modifier: Modifier = Modifier,
    viewModel: MenuViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    MenuScreen(
        state = uiState,
        onCategoryClick = viewModel::onCategoryClick,
        onMenuClick = viewModel::onMenuClick,
        onCartClick = viewModel::onCartClick,
        modifier = modifier,
    )
}

@Composable
fun MenuScreen(
    state: MenuUiState,
    onCategoryClick: (String) -> Unit,
    onMenuClick: (String) -> Unit,
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
        MenuHeader(cartCount = state.cartCount, onCartClick = onCartClick)
        MenuCategoryTabs(
            categories = state.categories,
            selectedCategoryId = state.selectedCategoryId,
            onCategoryClick = onCategoryClick,
        )

        state.menus.forEach { menu ->
            MenuListItem(menu = menu, onClick = { onMenuClick(menu.id) })
        }
    }
}

@Composable
private fun MenuHeader(
    cartCount: Int,
    onCartClick: () -> Unit,
) {
    val colors = CafeMinsuTheme.colors
    val spacing = CafeMinsuTheme.spacing

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space2), modifier = Modifier.weight(1f)) {
            Text(text = "메뉴", style = CafeMinsuTheme.typography.h1, color = colors.ink)
            Text(text = "원하는 음료를 빠르게 골라보세요.", style = CafeMinsuTheme.typography.body, color = colors.muted)
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onCartClick)
                .background(colors.surfaceCard),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "🛒", style = CafeMinsuTheme.typography.h3)
            if (cartCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 6.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(colors.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = cartCount.toString(), style = CafeMinsuTheme.typography.caption, color = colors.onPrimary)
                }
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

    FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.space2), verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
        categories.forEach { category ->
            val selected = category.id == selectedCategoryId
            Surface(
                modifier = Modifier.clickable(onClick = { onCategoryClick(category.id) }),
                shape = CafeMinsuTheme.shapes.radiusPill,
                color = if (selected) colors.surfaceDark else colors.surfaceCard,
            ) {
                Text(
                    text = category.label,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = CafeMinsuTheme.typography.caption.copy(fontWeight = FontWeight.Medium),
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
            .clickable(onClick = onClick),
        color = colors.surfaceCard,
        shape = CafeMinsuTheme.shapes.radiusLg,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MenuThumbnail(soldOut = menu.soldOut)

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                    style = CafeMinsuTheme.typography.bodyL.copy(fontWeight = FontWeight.Bold),
                    color = colors.ink,
                )
            }
        }
    }
}

@Composable
private fun MenuThumbnail(soldOut: Boolean) {
    val colors = CafeMinsuTheme.colors
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CafeMinsuTheme.shapes.radiusMd)
            .background(if (soldOut) colors.hairline else colors.accentSoft),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (soldOut) "×" else "☕",
            style = CafeMinsuTheme.typography.h2,
            color = if (soldOut) colors.mutedSoft else colors.primaryHover,
        )
    }
}

@Composable
private fun SoldOutBadge() {
    val colors = CafeMinsuTheme.colors
    Surface(color = colors.error, shape = CafeMinsuTheme.shapes.radiusPill) {
        Text(
            text = "품절",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = CafeMinsuTheme.typography.caption,
            color = colors.onPrimary,
        )
    }
}

class MenuViewModel : ViewModel() {
    private val baseCategories = listOf(
        MenuCategoryUiModel("all", "전체"),
        MenuCategoryUiModel("coffee", "커피"),
        MenuCategoryUiModel("tea", "티"),
        MenuCategoryUiModel("dessert", "디저트"),
    )

    private val allMenus = listOf(
        MenuUiModel("menu-1", "아메리카노", "가장 기본적인 커피 맛", "4,000원", "coffee"),
        MenuUiModel("menu-2", "카페라떼", "부드러운 우유 거품이 올라간 라떼", "4,800원", "coffee"),
        MenuUiModel("menu-3", "유자차", "상큼한 향이 좋은 따뜻한 티", "5,100원", "tea"),
        MenuUiModel("menu-4", "초코 크루아상", "겉은 바삭하고 속은 촉촉한 디저트", "3,900원", "dessert", soldOut = true),
    )

    private val mutableUiState = MutableStateFlow(
        MenuUiState(
            categories = baseCategories,
            selectedCategoryId = "all",
            menus = allMenus,
            cartCount = 2,
        ),
    )
    val uiState: StateFlow<MenuUiState> = mutableUiState.asStateFlow()

    fun onCategoryClick(categoryId: String) {
        mutableUiState.update { state ->
            val menus = if (categoryId == "all") allMenus else allMenus.filter { it.categoryId == categoryId }
            state.copy(selectedCategoryId = categoryId, menus = menus)
        }
    }

    fun onMenuClick(menuId: String) {
        mutableUiState.update { state ->
            state.copy(cartCount = state.cartCount + 1)
        }
    }

    fun onCartClick() = Unit
}

data class MenuUiState(
    val categories: List<MenuCategoryUiModel> = emptyList(),
    val selectedCategoryId: String = "all",
    val menus: List<MenuUiModel> = emptyList(),
    val cartCount: Int = 0,
)

data class MenuCategoryUiModel(
    val id: String,
    val label: String,
)

data class MenuUiModel(
    val id: String,
    val name: String,
    val description: String,
    val priceLabel: String,
    val categoryId: String,
    val soldOut: Boolean = false,
)
