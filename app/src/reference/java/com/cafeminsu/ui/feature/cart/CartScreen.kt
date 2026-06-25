package com.cafeminsu.ui.feature.cart

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafeminsu.R
import com.cafeminsu.domain.model.CartInvalidReason
import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.CartValidation
import com.cafeminsu.domain.model.OrderType
import com.cafeminsu.domain.model.SelectedOption
import com.cafeminsu.ui.components.CafeButton
import com.cafeminsu.ui.components.CafeButtonVariant
import com.cafeminsu.ui.components.CafeCard
import com.cafeminsu.ui.components.CafeCardType
import com.cafeminsu.ui.components.CafeTextField
import com.cafeminsu.ui.components.CafeTopBar
import com.cafeminsu.ui.components.EmptyView
import com.cafeminsu.ui.components.ErrorView
import com.cafeminsu.ui.components.LoadingView
import com.cafeminsu.ui.theme.CafeTheme
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CartRoute(
    onPaymentRequested: (String) -> Unit,
    onBrowseMenuClick: () -> Unit,
    onItemClick: (menuItemId: String, cartItemId: String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CartViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is CartEvent.NavigateToPayment -> onPaymentRequested(event.orderId)
            }
        }
    }

    CartScreen(
        state = state,
        onBackClick = onBackClick,
        onQuantityChange = viewModel::onQuantityChange,
        onOrderTypeSelected = viewModel::onOrderTypeSelected,
        onRequestNoteChange = viewModel::onRequestNoteChange,
        onCheckout = viewModel::onCheckout,
        onRetry = viewModel::retry,
        onBrowseMenuClick = onBrowseMenuClick,
        onItemClick = onItemClick,
        modifier = modifier,
    )
}

@Composable
fun CartScreen(
    state: CartUiState,
    onBackClick: () -> Unit,
    onQuantityChange: (cartItemId: String, quantity: Int) -> Unit,
    onOrderTypeSelected: (OrderType) -> Unit,
    onRequestNoteChange: (String) -> Unit,
    onCheckout: () -> Unit,
    onRetry: () -> Unit,
    onBrowseMenuClick: () -> Unit,
    onItemClick: (menuItemId: String, cartItemId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.canvas,
        topBar = {
            CafeTopBar(
                title = "장바구니",
                navigationIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_left),
                        contentDescription = null,
                        tint = colors.ink,
                    )
                },
                onNavigationClick = onBackClick,
            )
        },
        bottomBar = {
            if (state is CartUiState.Content) {
                CheckoutBar(
                    state = state,
                    onCheckout = onCheckout,
                )
            }
        },
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = colors.canvas,
            contentColor = colors.body,
        ) {
            when (state) {
                CartUiState.Loading -> LoadingView(modifier = Modifier.padding(screenPadding()))
                is CartUiState.Content -> CartContent(
                    state = state,
                    onQuantityChange = onQuantityChange,
                    onOrderTypeSelected = onOrderTypeSelected,
                    onRequestNoteChange = onRequestNoteChange,
                    onItemClick = onItemClick,
                )

                is CartUiState.Empty -> EmptyView(
                    modifier = Modifier.padding(screenPadding()),
                    message = state.message,
                    actionLabel = "메뉴 보러가기",
                    onAction = onBrowseMenuClick,
                )

                is CartUiState.Error -> ErrorView(
                    modifier = Modifier.padding(screenPadding()),
                    message = state.message,
                    retryable = state.retryable,
                    onRetry = onRetry,
                )
            }
        }
    }
}

@Composable
private fun CartContent(
    state: CartUiState.Content,
    onQuantityChange: (cartItemId: String, quantity: Int) -> Unit,
    onOrderTypeSelected: (OrderType) -> Unit,
    onRequestNoteChange: (String) -> Unit,
    onItemClick: (menuItemId: String, cartItemId: String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding(),
        verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space4),
    ) {
        item {
            OrderTypeSection(
                selectedOrderType = state.orderType,
                onOrderTypeSelected = onOrderTypeSelected,
            )
        }

        item {
            SectionLabel(text = "주문 항목 (${state.items.size})")
        }

        items(
            items = state.items,
            key = { item -> item.id },
        ) { item ->
            CartItemCard(
                item = item,
                onQuantityChange = onQuantityChange,
                onClick = { onItemClick(item.menuItemId, item.id) },
            )
        }

        item {
            RequestNoteSection(
                requestNote = state.requestNote,
                onRequestNoteChange = onRequestNoteChange,
            )
        }
    }
}

@Composable
private fun OrderTypeSection(
    selectedOrderType: OrderType,
    onOrderTypeSelected: (OrderType) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space3)) {
        SectionLabel(text = "주문 방식")
        OrderTypeToggle(
            selectedOrderType = selectedOrderType,
            onOrderTypeSelected = onOrderTypeSelected,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = CafeTheme.typography.caption,
        color = CafeTheme.colors.muted,
    )
}

@Composable
private fun OrderTypeToggle(
    selectedOrderType: OrderType,
    onOrderTypeSelected: (OrderType) -> Unit,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CafeTheme.shapes.radiusLg,
        color = colors.surfaceCard,
        contentColor = colors.ink,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.space1),
            horizontalArrangement = Arrangement.spacedBy(spacing.space1),
        ) {
            OrderTypeSegment(
                text = "매장에서 먹기",
                selected = selectedOrderType == OrderType.DineIn,
                onClick = { onOrderTypeSelected(OrderType.DineIn) },
                modifier = Modifier.weight(SegmentWeight),
            )
            OrderTypeSegment(
                text = "포장 (픽업)",
                selected = selectedOrderType == OrderType.Takeout,
                onClick = { onOrderTypeSelected(OrderType.Takeout) },
                modifier = Modifier.weight(SegmentWeight),
            )
        }
    }
}

@Composable
private fun OrderTypeSegment(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors

    Surface(
        onClick = onClick,
        modifier = modifier.height(CafeTheme.spacing.space10),
        shape = CafeTheme.shapes.radiusMd,
        color = if (selected) colors.canvas else colors.surfaceCard,
        contentColor = if (selected) colors.ink else colors.muted,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = CafeTheme.typography.bodyL,
                color = if (selected) colors.ink else colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CartItemCard(
    item: CartItem,
    onQuantityChange: (cartItemId: String, quantity: Int) -> Unit,
    onClick: () -> Unit,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    CafeCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        type = CafeCardType.Default,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(spacing.space10 + spacing.space2)
                    .background(
                        color = colors.primary.copy(alpha = ThumbnailAlpha),
                        shape = CafeTheme.shapes.radiusMd,
                    ),
            )

            Column(
                modifier = Modifier.weight(ItemTextWeight),
                verticalArrangement = Arrangement.spacedBy(spacing.space1),
            ) {
                Text(
                    text = item.name,
                    style = CafeTheme.typography.h3,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.selectedOptions.summaryText(),
                    style = CafeTheme.typography.caption,
                    color = colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(spacing.space2),
            ) {
                Text(
                    text = formatWon(item.unitPrice * item.quantity),
                    style = CafeTheme.typography.bodyL,
                    color = colors.ink,
                    maxLines = 1,
                )
                QuantityStepper(
                    quantity = item.quantity,
                    onQuantityChange = { quantity -> onQuantityChange(item.id, quantity) },
                )
            }
        }
    }
}

@Composable
private fun QuantityStepper(
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Surface(
        shape = CafeTheme.shapes.radiusPill,
        color = colors.canvas,
        contentColor = colors.ink,
    ) {
        Row(
            modifier = Modifier
                .height(spacing.space8)
                .width(spacing.space18 + spacing.space3)
                .padding(horizontal = spacing.space1),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuantityButton(
                text = "−",
                enabled = true,
                onClick = { onQuantityChange(quantity - QuantityStep) },
            )
            Text(
                text = quantity.toString(),
                modifier = Modifier.weight(QuantityTextWeight),
                style = CafeTheme.typography.caption,
                color = colors.ink,
                textAlign = TextAlign.Center,
            )
            QuantityButton(
                text = "+",
                enabled = true,
                onClick = { onQuantityChange(quantity + QuantityStep) },
            )
        }
    }
}

@Composable
private fun QuantityButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = CafeTheme.colors

    Surface(
        onClick = onClick,
        modifier = Modifier.size(CafeTheme.spacing.space6),
        enabled = enabled,
        shape = CafeTheme.shapes.radiusPill,
        color = colors.canvas,
        contentColor = if (enabled) colors.ink else colors.muted,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = CafeTheme.typography.bodyL,
                color = if (enabled) colors.ink else colors.muted,
            )
        }
    }
}

@Composable
private fun RequestNoteSection(
    requestNote: String,
    onRequestNoteChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space3)) {
        SectionLabel(text = "요청사항")
        CafeTextField(
            value = requestNote,
            onValueChange = onRequestNoteChange,
            placeholder = "예) 얼음 적게 부탁드려요",
        )
    }
}

@Composable
private fun CheckoutBar(
    state: CartUiState.Content,
    onCheckout: () -> Unit,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.canvas,
        contentColor = colors.body,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = spacing.space5,
                vertical = spacing.space4,
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.space4),
        ) {
            ValidationBanner(
                validation = state.validation,
                items = state.items,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = "총 결제 금액",
                    style = CafeTheme.typography.body,
                    color = colors.body,
                )
                Text(
                    text = formatWon(state.subtotal),
                    style = CafeTheme.typography.display,
                    color = colors.ink,
                )
            }

            CafeButton(
                text = "결제하기",
                onClick = onCheckout,
                modifier = Modifier.fillMaxWidth(),
                variant = CafeButtonVariant.Primary,
                enabled = state.canCheckout,
            )
        }
    }
}

@Composable
private fun ValidationBanner(
    validation: CartValidation,
    items: List<CartItem>,
) {
    val message = validation.message(items) ?: return
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing
    val borderColor = if (validation.hasHardBlocker()) {
        colors.error
    } else {
        colors.warning
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CafeTheme.shapes.radiusMd,
        color = colors.surfaceCard,
        contentColor = colors.body,
        border = BorderStroke(spacing.space1 / BorderWidthDivider, borderColor),
    ) {
        Text(
            modifier = Modifier.padding(
                horizontal = spacing.space4,
                vertical = spacing.space3,
            ),
            text = message,
            style = CafeTheme.typography.body,
            color = colors.body,
        )
    }
}

@Composable
private fun contentPadding(): PaddingValues =
    PaddingValues(
        start = CafeTheme.spacing.space5,
        top = CafeTheme.spacing.space5,
        end = CafeTheme.spacing.space5,
        bottom = CafeTheme.spacing.space8,
    )

@Composable
private fun screenPadding(): PaddingValues =
    PaddingValues(
        start = CafeTheme.spacing.space5,
        top = CafeTheme.spacing.space5,
        end = CafeTheme.spacing.space5,
        bottom = CafeTheme.spacing.space6,
    )

private fun List<SelectedOption>.summaryText(): String =
    if (isEmpty()) {
        "기본 옵션"
    } else {
        joinToString(separator = " · ") { option -> option.name }
    }

private fun CartValidation.message(items: List<CartItem>): String? =
    when (this) {
        CartValidation.Valid -> null
        is CartValidation.Invalid -> reasons.joinToString(separator = "\n") { reason ->
            reason.message(items)
        }
    }

private fun CartInvalidReason.message(items: List<CartItem>): String =
    when (this) {
        CartInvalidReason.Empty -> "담은 메뉴가 없어요"
        is CartInvalidReason.SoldOut -> "${items.nameFor(menuItemId)}은 지금 품절이에요"
        is CartInvalidReason.PriceChanged -> "${items.nameFor(menuItemId)} 가격이 ${formatWon(latestPrice)}로 바뀌었어요"
        is CartInvalidReason.OptionUnavailable -> "선택할 수 없는 옵션이 있어요"
        CartInvalidReason.StoreClosed -> "영업 시간이 지나 주문할 수 없어요"
    }

private fun CartValidation.hasHardBlocker(): Boolean =
    this is CartValidation.Invalid &&
        reasons.any { reason ->
            reason is CartInvalidReason.SoldOut ||
                reason is CartInvalidReason.PriceChanged ||
                reason is CartInvalidReason.OptionUnavailable ||
                reason == CartInvalidReason.StoreClosed
        }

private fun List<CartItem>.nameFor(menuItemId: String): String =
    firstOrNull { item -> item.menuItemId == menuItemId }?.name ?: "담긴 메뉴"

private fun formatWon(amount: Int): String =
    "${NumberFormat.getNumberInstance(Locale.KOREA).format(amount)}원"

private const val BorderWidthDivider = 4
private const val ItemTextWeight = 1f
private const val QuantityStep = 1
private const val QuantityTextWeight = 1f
private const val SegmentWeight = 1f
private const val ThumbnailAlpha = 0.55f
