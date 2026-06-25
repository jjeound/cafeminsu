package com.cafeminsu.ui.feature.store

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
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
    val query by viewModel.searchQuery.collectAsState()
    val context = LocalContext.current

    // 매장 선택 화면 진입 시 위치 권한을 1회 요청한다(이미 허용돼 있으면 생략). 허용되면 지도가 내 위치로 이동.
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { /* 결과는 다음 지도 렌더에서 hasLocationPermission으로 반영된다. */ }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission(context)) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                StoreEvent.NavigateToMenu -> onNavigateToMenu()
            }
        }
    }

    StoreScreen(
        state = state,
        query = query,
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
    query: String,
    onQueryChange: (String) -> Unit,
    onStoreClick: (String) -> Unit,
    onDismissStoreDetail: () -> Unit,
    onStartOrder: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    mapContent: @Composable (List<StoreMapMarker>, UserLocationUiModel?, (String) -> Unit) -> Unit =
        { markers, userLocation, onMarkerClick ->
            StoreMapView(
                markers = markers,
                userLocation = userLocation,
                onMarkerClick = onMarkerClick,
            )
        },
) {
    val content = state as? StoreUiState.Content
    val selectedStore = content?.selectedStore
    val mapMarkers = content?.stores?.map { it.toMapMarker() }.orEmpty()
    val userLocation = content?.userLocation

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
                mapContent(mapMarkers, userLocation, onStoreClick)
                NearbyStoresHeader()

                when (state) {
                    StoreUiState.Loading -> LoadingView()
                    is StoreUiState.Content -> StoreList(
                        stores = state.stores,
                        selectedStoreId = selectedStore?.id,
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
    Column(
        modifier = Modifier.fillMaxWidth(),
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

private fun StoreUiModel.toMapMarker(): StoreMapMarker =
    StoreMapMarker(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
    )

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
    selectedStoreId: String?,
    onStoreClick: (String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space3),
    ) {
        stores.forEach { store ->
            StoreCard(
                store = store,
                selected = store.id == selectedStoreId,
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
                    // 거리를 알 수 없으면("") 핀을 그리지 않는다("0m" 노출 금지).
                    if (store.distanceLabel.isNotBlank()) {
                        Spacer(modifier = Modifier.width(spacing.space2))
                        DistancePill(
                            label = store.distanceLabel,
                            selected = selected,
                        )
                    }
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
        // 거리를 알 수 없으면("") 거리 행을 숨긴다("0m" 노출 금지).
        if (store.distanceLabel.isNotBlank()) {
            StoreInfoRow(label = "거리", value = store.distanceLabel)
            StoreInfoDivider()
        }
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
