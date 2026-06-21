package com.cafeminsu.ui.feature.store

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafeminsu.R
import com.cafeminsu.ui.components.CafeButton
import com.cafeminsu.ui.components.CafeCard
import com.cafeminsu.ui.components.CafeCardType
import com.cafeminsu.ui.components.CafeChip
import com.cafeminsu.ui.components.CafeTextField
import com.cafeminsu.ui.components.EmptyView
import com.cafeminsu.ui.components.ErrorView
import com.cafeminsu.ui.components.LoadingView
import com.cafeminsu.ui.theme.CafeTheme

@Composable
fun StoreRoute(
    onNavigateToMenu: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StoreViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                StoreEvent.NavigateToMenu -> onNavigateToMenu()
            }
        }
    }

    StoreScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onStoreClick = viewModel::onStoreClick,
        onDismissStoreDetail = viewModel::onDismissStoreDetail,
        onStartOrder = viewModel::onStartOrder,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
fun StoreScreen(
    state: StoreUiState,
    onQueryChange: (String) -> Unit,
    onStoreClick: (String) -> Unit,
    onDismissStoreDetail: () -> Unit,
    onStartOrder: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedStore = (state as? StoreUiState.Content)?.selectedStore
    val query = when (state) {
        is StoreUiState.Content -> state.query
        is StoreUiState.Empty -> state.query
        is StoreUiState.Error,
        StoreUiState.Loading,
        -> ""
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = CafeTheme.colors.canvas,
        contentColor = CafeTheme.colors.body,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
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
                StoreHeader()
                StoreSearchField(query = query, onQueryChange = onQueryChange)
                StoreMap()
                NearbyStoresHeader()

                when (state) {
                    StoreUiState.Loading -> LoadingView()
                    is StoreUiState.Content -> StoreList(
                        stores = state.stores,
                        onStoreClick = onStoreClick,
                    )

                    is StoreUiState.Empty -> EmptyView(
                        message = state.message,
                        actionLabel = null,
                        onAction = null,
                    )

                    is StoreUiState.Error -> ErrorView(
                        message = state.message,
                        retryable = state.retryable,
                        onRetry = onRetry,
                    )
                }
            }

            if (selectedStore != null) {
                StoreDetailSheet(
                    store = selectedStore,
                    onDismiss = onDismissStoreDetail,
                    onStartOrder = { onStartOrder(selectedStore.id) },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
private fun StoreHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space3),
        ) {
            Text(
                text = "매장 선택",
                style = CafeTheme.typography.h1,
                color = CafeTheme.colors.ink,
            )
            Text(
                text = "오늘 어디서 한 잔 하실까요?",
                style = CafeTheme.typography.body,
                color = CafeTheme.colors.muted,
            )
        }

        SearchIcon(
            modifier = Modifier.size(CafeTheme.spacing.space8),
            color = CafeTheme.colors.ink,
        )
    }
}

@Composable
private fun StoreSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    CafeTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = "현재 위치 또는 매장명 검색",
        leadingIcon = {
            LocationPinIcon(
                modifier = Modifier.size(CafeTheme.spacing.space6),
                color = CafeTheme.colors.muted,
            )
        },
    )
}

@Composable
private fun StoreMap(
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(spacing.space18 * MapHeightMultiplier + spacing.space3),
        shape = CafeTheme.shapes.radiusXl,
        color = colors.surfaceCard,
        contentColor = colors.ink,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val verticalGap = size.width / MapGridLineCount
                    val horizontalGap = size.height / MapGridLineCount
                    repeat(MapGridLineCount + 1) { index ->
                        drawLine(
                            color = colors.hairline,
                            start = Offset(x = verticalGap * index, y = 0f),
                            end = Offset(x = verticalGap * index, y = size.height),
                        )
                        drawLine(
                            color = colors.hairline,
                            start = Offset(x = 0f, y = horizontalGap * index),
                            end = Offset(x = size.width, y = horizontalGap * index),
                        )
                    }
                }
                .padding(spacing.space5),
        ) {
            Surface(
                shape = CafeTheme.shapes.radiusPill,
                color = colors.canvas,
                contentColor = colors.ink,
            ) {
                Text(
                    modifier = Modifier.padding(
                        horizontal = spacing.space4,
                        vertical = spacing.space2,
                    ),
                    text = "내 주변 지도",
                    style = CafeTheme.typography.caption,
                    color = colors.ink,
                )
            }

            MapMarker(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun MapMarker(
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors

    Box(
        modifier = modifier
            .size(CafeTheme.spacing.space10)
            .background(
                color = colors.primary,
                shape = CafeTheme.shapes.radiusPill,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(CafeTheme.spacing.space2)
                .background(
                    color = colors.onPrimary,
                    shape = CafeTheme.shapes.radiusPill,
                ),
        )
    }
}

@Composable
private fun NearbyStoresHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "가까운 매장",
            style = CafeTheme.typography.h2,
            color = CafeTheme.colors.ink,
        )
        Text(
            text = "전체 보기",
            style = CafeTheme.typography.body,
            color = CafeTheme.colors.primary,
        )
    }
}

@Composable
private fun StoreList(
    stores: List<StoreUiModel>,
    onStoreClick: (String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space3),
    ) {
        stores.forEachIndexed { index, store ->
            StoreCard(
                store = store,
                selected = index == SelectedStoreCardIndex,
                onClick = { onStoreClick(store.id) },
            )
        }
    }
}

@Composable
private fun StoreCard(
    store: StoreUiModel,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing
    val contentColor = if (selected) colors.onDark else colors.ink
    val supportingColor = if (selected) colors.mutedSoft else colors.muted

    CafeCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        type = if (selected) CafeCardType.Product else CafeCardType.Default,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StoreThumbnail(selected = selected)

            Column(
                modifier = Modifier.weight(StoreTextWeight),
                verticalArrangement = Arrangement.spacedBy(spacing.space1),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier.weight(StoreTextWeight, fill = false),
                        text = store.name,
                        style = CafeTheme.typography.h3,
                        color = contentColor,
                        maxLines = StoreNameMaxLines,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.width(spacing.space2))
                    DistancePill(
                        label = store.distanceLabel,
                        selected = selected,
                    )
                }

                Text(
                    text = store.address,
                    style = CafeTheme.typography.caption,
                    color = supportingColor,
                    maxLines = AddressMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )

                StoreStatusRow(
                    status = store.status,
                    label = store.statusLabel,
                )
            }
        }
    }
}

@Composable
private fun StoreThumbnail(selected: Boolean) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Surface(
        modifier = Modifier.size(spacing.space14 + spacing.space2),
        shape = CafeTheme.shapes.radiusMd,
        color = if (selected) colors.canvas else colors.hairline,
        contentColor = colors.primary,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(spacing.space8 + spacing.space1)
                    .background(
                        color = colors.primary,
                        shape = CafeTheme.shapes.radiusPill,
                    ),
            )
        }
    }
}

@Composable
private fun DistancePill(
    label: String,
    selected: Boolean,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Surface(
        shape = CafeTheme.shapes.radiusPill,
        color = if (selected) colors.body else colors.hairline,
        contentColor = if (selected) colors.onDark else colors.body,
    ) {
        Text(
            modifier = Modifier.padding(
                horizontal = spacing.space3,
                vertical = spacing.space1,
            ),
            text = label,
            style = CafeTheme.typography.caption,
            color = if (selected) colors.onDark else colors.body,
        )
    }
}

@Composable
private fun StoreStatusRow(
    status: StoreStatusUiModel,
    label: String,
) {
    val colors = CafeTheme.colors
    val statusColor = when (status) {
        StoreStatusUiModel.Open -> colors.success
        StoreStatusUiModel.ClosingSoon -> colors.warning
        StoreStatusUiModel.Closed -> colors.error
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(CafeTheme.spacing.space1)
                .background(
                    color = statusColor,
                    shape = CafeTheme.shapes.radiusPill,
                ),
        )
        Text(
            text = label,
            style = CafeTheme.typography.caption,
            color = statusColor,
        )
    }
}

@Composable
private fun StoreDetailSheet(
    store: StoreDetailUiModel,
    onDismiss: () -> Unit,
    onStartOrder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.ink.copy(alpha = ScrimAlpha))
                .clickable(onClick = onDismiss),
        )

        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = CafeTheme.shapes.radiusXl,
            color = colors.canvas,
            contentColor = colors.body,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = spacing.space5,
                        top = spacing.space3,
                        end = spacing.space5,
                        bottom = spacing.space5,
                    ),
                verticalArrangement = Arrangement.spacedBy(spacing.space5),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .width(spacing.space10)
                            .height(spacing.space1)
                            .background(
                                color = colors.hairline,
                                shape = CafeTheme.shapes.radiusPill,
                            ),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
                    Text(
                        text = store.name,
                        style = CafeTheme.typography.h2,
                        color = colors.ink,
                    )
                    StoreStatusRow(
                        status = StoreStatusUiModel.Open,
                        label = store.statusLabel,
                    )
                }

                StoreInfoRows(store = store)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                ) {
                    store.amenities.forEach { amenity ->
                        CafeChip(
                            text = amenity,
                            selected = false,
                            onClick = {},
                        )
                    }
                }

                CafeButton(
                    text = "이 매장에서 주문하기",
                    onClick = onStartOrder,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun StoreInfoRows(store: StoreDetailUiModel) {
    Column {
        StoreInfoRow(label = "주소", value = store.address)
        StoreInfoDivider()
        StoreInfoRow(label = "전화", value = store.phone)
        StoreInfoDivider()
        StoreInfoRow(label = "거리", value = store.distanceLabel)
        StoreInfoDivider()
        StoreInfoRow(label = "주차", value = store.parkingLabel)
    }
}

@Composable
private fun StoreInfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = CafeTheme.spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.width(CafeTheme.spacing.space14),
            text = label,
            style = CafeTheme.typography.caption,
            color = CafeTheme.colors.muted,
        )
        Text(
            modifier = Modifier.weight(StoreTextWeight),
            text = value,
            style = CafeTheme.typography.body,
            color = CafeTheme.colors.ink,
        )
    }
}

@Composable
private fun StoreInfoDivider() {
    HorizontalDivider(
        color = CafeTheme.colors.hairline,
        thickness = CafeTheme.spacing.space1 / DividerThicknessDivider,
    )
}

@Composable
private fun SearchIcon(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(R.drawable.ic_search),
        contentDescription = null,
        tint = color,
        modifier = modifier,
    )
}

@Composable
private fun LocationPinIcon(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = size.minDimension / IconStrokeDivider)
        drawCircle(
            color = color,
            radius = size.minDimension / PinCircleRadiusDivider,
            center = Offset(x = size.width / PinCenterXDivider, y = size.height / PinCenterYDivider),
            style = stroke,
        )
        drawCircle(
            color = color,
            radius = size.minDimension / PinDotRadiusDivider,
            center = Offset(x = size.width / PinCenterXDivider, y = size.height / PinCenterYDivider),
        )
        drawLine(
            color = color,
            start = Offset(x = size.width / PinCenterXDivider, y = size.height * PinTailStartFraction),
            end = Offset(x = size.width / PinCenterXDivider, y = size.height * PinTailEndFraction),
            strokeWidth = stroke.width,
        )
    }
}

private const val MapHeightMultiplier = 2
private const val MapGridLineCount = 4
private const val SelectedStoreCardIndex = 0
private const val StoreTextWeight = 1f
private const val StoreNameMaxLines = 1
private const val AddressMaxLines = 1
private const val DividerThicknessDivider = 4
private const val ScrimAlpha = 0.38f
private const val IconStrokeDivider = 12f
private const val PinCircleRadiusDivider = 3.7f
private const val PinDotRadiusDivider = 14f
private const val PinCenterXDivider = 2f
private const val PinCenterYDivider = 2.6f
private const val PinTailStartFraction = 0.56f
private const val PinTailEndFraction = 0.88f
