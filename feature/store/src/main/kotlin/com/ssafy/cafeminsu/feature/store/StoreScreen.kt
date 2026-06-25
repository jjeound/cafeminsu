package com.ssafy.cafeminsu.feature.store

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
fun StoreRoute(
    modifier: Modifier = Modifier,
    viewModel: StoreViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    StoreScreen(
        state = uiState,
        onQueryChange = viewModel::onQueryChange,
        onStoreClick = viewModel::onStoreClick,
        onDismissDetail = viewModel::onDismissDetail,
        onStartOrder = viewModel::onStartOrder,
        modifier = modifier,
    )
}

@Composable
fun StoreScreen(
    state: StoreUiState,
    onQueryChange: (String) -> Unit,
    onStoreClick: (String) -> Unit,
    onDismissDetail: () -> Unit,
    onStartOrder: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeMinsuTheme.colors
    val spacing = CafeMinsuTheme.spacing
    val content = state as StoreUiState.Content

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .padding(horizontal = spacing.space5),
        verticalArrangement = Arrangement.spacedBy(spacing.space4),
    ) {
        item {
            Column(
                modifier = Modifier.padding(top = spacing.space6),
                verticalArrangement = Arrangement.spacedBy(spacing.space2),
            ) {
                Text(
                    text = "매장 찾기",
                    style = CafeMinsuTheme.typography.h1,
                    color = colors.ink,
                )
                Text(
                    text = "현재 위치를 기준으로 가까운 매장을 확인해보세요.",
                    style = CafeMinsuTheme.typography.body,
                    color = colors.muted,
                )
            }
        }

        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = content.query,
                onValueChange = onQueryChange,
                placeholder = { Text(text = "매장명 또는 지역 검색") },
                singleLine = true,
            )
        }

        item {
            StoreMapPreview(
                stores = content.stores,
                selectedStoreId = content.selectedStore?.id,
                onStoreClick = onStoreClick,
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "가까운 매장",
                    style = CafeMinsuTheme.typography.h2,
                    color = colors.ink,
                )
                Text(
                    text = "${content.stores.size}개",
                    style = CafeMinsuTheme.typography.caption,
                    color = colors.muted,
                )
            }
        }

        items(content.stores, key = { it.id }) { store ->
            StoreCard(
                store = store,
                selected = store.id == content.selectedStore?.id,
                onClick = { onStoreClick(store.id) },
            )
        }

        item {
            Spacer(modifier = Modifier.height(spacing.space8))
        }
    }

    content.selectedStore?.let { store ->
        StoreDetailBar(
            store = store,
            onDismiss = onDismissDetail,
            onStartOrder = { onStartOrder(store.id) },
        )
    }
}

@Composable
private fun StoreMapPreview(
    stores: List<StoreUiModel>,
    selectedStoreId: String?,
    onStoreClick: (String) -> Unit,
) {
    val colors = CafeMinsuTheme.colors

    Surface(
        color = colors.surfaceCard,
        shape = CafeMinsuTheme.shapes.radiusXl,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(color = colors.canvas)
                for (index in 0..6) {
                    val x = size.width * (index / 6f)
                    drawLine(
                        color = colors.hairline,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1f,
                    )
                }
                for (index in 0..4) {
                    val y = size.height * (index / 4f)
                    drawLine(
                        color = colors.hairline,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f,
                    )
                }

                stores.forEachIndexed { index, store ->
                    val x = size.width * (0.18f + index * 0.24f)
                    val y = size.height * (0.22f + (index % 2) * 0.28f)
                    drawCircle(
                        color = if (store.id == selectedStoreId) colors.primary else colors.ink,
                        radius = if (store.id == selectedStoreId) 16f else 12f,
                        center = Offset(x, y),
                    )
                    drawCircle(
                        color = colors.onPrimary,
                        radius = 4f,
                        center = Offset(x, y),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "지도 미리보기",
                    style = CafeMinsuTheme.typography.caption,
                    color = colors.muted,
                )
                Text(
                    text = "매장 마커를 눌러보세요.",
                    style = CafeMinsuTheme.typography.body,
                    color = colors.ink,
                )
            }
        }
    }
}

@Composable
private fun StoreCard(
    store: StoreUiModel,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = CafeMinsuTheme.colors
    val background = if (selected) colors.surfaceDark else colors.surfaceCard
    val contentColor = if (selected) colors.onDark else colors.ink

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = background,
        shape = CafeMinsuTheme.shapes.radiusLg,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CafeMinsuTheme.shapes.radiusMd)
                    .background(if (selected) colors.primary else colors.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                LocationPinIcon(color = if (selected) colors.onPrimary else colors.primaryHover)
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = store.name,
                    style = CafeMinsuTheme.typography.h3,
                    color = contentColor,
                )
                Text(
                    text = store.address,
                    style = CafeMinsuTheme.typography.caption,
                    color = if (selected) colors.mutedSoft else colors.muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${store.distanceLabel} · ${store.statusLabel}",
                    style = CafeMinsuTheme.typography.caption,
                    color = if (selected) colors.mutedSoft else colors.muted,
                )
            }
        }
    }
}

@Composable
private fun StoreDetailBar(
    store: StoreUiModel,
    onDismiss: () -> Unit,
    onStartOrder: () -> Unit,
) {
    val colors = CafeMinsuTheme.colors

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = colors.surfaceDark,
        shape = CafeMinsuTheme.shapes.radiusXl,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = store.name, style = CafeMinsuTheme.typography.h3, color = colors.onDark)
                    Text(text = store.address, style = CafeMinsuTheme.typography.caption, color = colors.mutedSoft)
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss)
                        .background(colors.canvas.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "×", style = CafeMinsuTheme.typography.h3, color = colors.onDark)
                }
            }

            CafeMinsuButton(
                text = "주문 시작하기",
                onClick = onStartOrder,
                modifier = Modifier.fillMaxWidth(),
                variant = CafeMinsuButtonVariant.Secondary,
            )
        }
    }
}

@Composable
private fun LocationPinIcon(color: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        drawCircle(color = color, radius = size.minDimension * 0.32f, center = center)
        drawCircle(color = Color.White, radius = size.minDimension * 0.12f, center = center)
        drawCircle(
            color = color,
            radius = size.minDimension * 0.42f,
            center = center,
            style = Stroke(width = size.minDimension * 0.08f),
        )
    }
}

class StoreViewModel : ViewModel() {
    private val stores = listOf(
        StoreUiModel("store-1", "카페민수 강남점", "서울 강남구 테헤란로 1", "320m", "영업중"),
        StoreUiModel("store-2", "카페민수 역삼점", "서울 강남구 논현로 2", "540m", "영업중"),
        StoreUiModel("store-3", "카페민수 선릉점", "서울 강남구 선릉로 3", "1.1km", "준비중"),
    )

    private val mutableUiState = MutableStateFlow(
        StoreUiState.Content(
            query = "",
            stores = stores,
            selectedStore = null,
        ),
    )

    val uiState: StateFlow<StoreUiState> = mutableUiState.asStateFlow()

    fun onQueryChange(query: String) {
        mutableUiState.update { state ->
            state.copy(
                query = query,
                stores = stores.filter {
                    query.isBlank() || it.name.contains(query, ignoreCase = true) || it.address.contains(query, ignoreCase = true)
                },
                selectedStore = state.selectedStore?.takeIf { selected ->
                    query.isBlank() || selected.name.contains(query, ignoreCase = true) || selected.address.contains(query, ignoreCase = true)
                },
            )
        }
    }

    fun onStoreClick(storeId: String) {
        mutableUiState.update { state ->
            state.copy(selectedStore = state.stores.firstOrNull { it.id == storeId })
        }
    }

    fun onDismissDetail() {
        mutableUiState.update { state -> state.copy(selectedStore = null) }
    }

    fun onStartOrder(storeId: String) {
        onStoreClick(storeId)
    }
}

sealed interface StoreUiState {
    data class Content(
        val query: String,
        val stores: List<StoreUiModel>,
        val selectedStore: StoreUiModel? = null,
    ) : StoreUiState
}

data class StoreUiModel(
    val id: String,
    val name: String,
    val address: String,
    val distanceLabel: String,
    val statusLabel: String,
)
