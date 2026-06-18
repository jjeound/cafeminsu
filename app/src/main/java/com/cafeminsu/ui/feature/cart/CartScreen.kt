package com.cafeminsu.ui.feature.cart

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafeminsu.domain.model.CartInvalidReason
import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.CartValidation
import com.cafeminsu.domain.model.SelectedOption
import com.cafeminsu.ui.components.CafeButton
import com.cafeminsu.ui.components.CafeButtonVariant
import com.cafeminsu.ui.components.CafeCard
import com.cafeminsu.ui.components.CafeCardType
import com.cafeminsu.ui.components.EmptyView
import com.cafeminsu.ui.components.ErrorView
import com.cafeminsu.ui.components.LoadingView
import com.cafeminsu.ui.theme.CafeTheme
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CartRoute(
    onOrderCreated: (String) -> Unit,
    onBrowseMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CartViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is CartEvent.NavigateToOrderStatus -> onOrderCreated(event.orderId)
            }
        }
    }

    CartScreen(
        state = state,
        onQuantityChange = viewModel::onQuantityChange,
        onRemove = viewModel::onRemove,
        onCheckout = viewModel::onCheckout,
        onRetry = viewModel::retry,
        onBrowseMenuClick = onBrowseMenuClick,
        modifier = modifier,
    )
}

@Composable
fun CartScreen(
    state: CartUiState,
    onQuantityChange: (cartItemId: String, quantity: Int) -> Unit,
    onRemove: (cartItemId: String) -> Unit,
    onCheckout: () -> Unit,
    onRetry: () -> Unit,
    onBrowseMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CafeTheme.colors.canvas,
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
            color = CafeTheme.colors.canvas,
            contentColor = CafeTheme.colors.body,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(screenPadding()),
                verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space5),
            ) {
                Text(
                    text = "장바구니",
                    style = CafeTheme.typography.h1,
                    color = CafeTheme.colors.ink,
                )

                when (state) {
                    CartUiState.Loading -> LoadingView()
                    is CartUiState.Content -> CartContent(
                        state = state,
                        onQuantityChange = onQuantityChange,
                        onRemove = onRemove,
                        modifier = Modifier.weight(ContentWeight),
                    )

                    is CartUiState.Empty -> EmptyView(
                        message = state.message,
                        actionLabel = "메뉴 보러가기",
                        onAction = onBrowseMenuClick,
                    )

                    is CartUiState.Error -> ErrorView(
                        message = state.message,
                        retryable = state.retryable,
                        onRetry = onRetry,
                    )
                }
            }
        }
    }
}

@Composable
private fun CartContent(
    state: CartUiState.Content,
    onQuantityChange: (cartItemId: String, quantity: Int) -> Unit,
    onRemove: (cartItemId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = CafeTheme.spacing.space6),
        verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space4),
    ) {
        items(
            items = state.items,
            key = { item -> item.id },
        ) { item ->
            CartItemRow(
                item = item,
                onQuantityChange = onQuantityChange,
                onRemove = onRemove,
            )
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onQuantityChange: (cartItemId: String, quantity: Int) -> Unit,
    onRemove: (cartItemId: String) -> Unit,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    CafeCard(
        modifier = Modifier.fillMaxWidth(),
        type = CafeCardType.Default,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(ItemNameWeight),
                    verticalArrangement = Arrangement.spacedBy(spacing.space1),
                ) {
                    Text(
                        text = item.name,
                        style = CafeTheme.typography.h3,
                        color = colors.ink,
                    )
                    Text(
                        text = item.selectedOptions.summaryText(),
                        style = CafeTheme.typography.caption,
                        color = colors.muted,
                    )
                }

                Spacer(modifier = Modifier.width(spacing.space3))
                CafeButton(
                    text = "삭제",
                    onClick = { onRemove(item.id) },
                    modifier = Modifier.width(spacing.space18 + spacing.space4),
                    variant = CafeButtonVariant.Ghost,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuantityStepper(
                    quantity = item.quantity,
                    onQuantityChange = { quantity -> onQuantityChange(item.id, quantity) },
                )

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(spacing.space1),
                ) {
                    Text(
                        text = "단가 ${formatWon(item.unitPrice)}",
                        style = CafeTheme.typography.caption,
                        color = colors.muted,
                    )
                    Text(
                        text = "소계 ${formatWon(item.unitPrice * item.quantity)}",
                        style = CafeTheme.typography.bodyL,
                        color = colors.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuantityStepper(
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
) {
    val spacing = CafeTheme.spacing

    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QuantityButton(
            text = "-",
            enabled = quantity > MinQuantity,
            onClick = { onQuantityChange(quantity - QuantityStep) },
        )
        Box(
            modifier = Modifier
                .height(spacing.space10 + spacing.space2)
                .width(spacing.space14),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = quantity.toString(),
                style = CafeTheme.typography.bodyL,
                color = CafeTheme.colors.ink,
            )
        }
        QuantityButton(
            text = "+",
            enabled = true,
            onClick = { onQuantityChange(quantity + QuantityStep) },
        )
    }
}

@Composable
private fun QuantityButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Surface(
        onClick = onClick,
        modifier = Modifier
            .height(spacing.space10 + spacing.space2)
            .width(spacing.space10 + spacing.space2),
        enabled = enabled,
        shape = CafeTheme.shapes.radiusMd,
        color = colors.canvas,
        contentColor = if (enabled) colors.ink else colors.muted,
        border = BorderStroke(spacing.space1 / BorderWidthDivider, colors.hairline),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = CafeTheme.typography.h3,
                color = if (enabled) colors.ink else colors.muted,
            )
        }
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
        border = BorderStroke(spacing.space1 / BorderWidthDivider, colors.hairline),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = spacing.space5,
                vertical = spacing.space4,
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            ValidationBanner(
                validation = state.validation,
                items = state.items,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
                    Text(
                        text = "합계",
                        style = CafeTheme.typography.caption,
                        color = colors.muted,
                    )
                    Text(
                        text = "최소 주문 ${formatWon(state.minimumOrderAmount)}",
                        style = CafeTheme.typography.caption,
                        color = colors.muted,
                    )
                }
                Text(
                    text = formatWon(state.subtotal),
                    style = CafeTheme.typography.h3,
                    color = colors.primary,
                )
            }

            CafeButton(
                text = if (state.checkoutInProgress) "주문 생성 중" else "주문하기",
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
private fun screenPadding(): PaddingValues =
    PaddingValues(
        start = CafeTheme.spacing.space5,
        top = CafeTheme.spacing.space6,
        end = CafeTheme.spacing.space5,
        bottom = CafeTheme.spacing.space6,
    )

private fun List<SelectedOption>.summaryText(): String =
    if (isEmpty()) {
        "기본 옵션"
    } else {
        joinToString(separator = ", ") { option -> option.name }
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
        is CartInvalidReason.BelowMinimumAmount -> "최소 주문 금액까지 ${formatWon(shortage)} 부족해요"
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
private const val ContentWeight = 1f
private const val ItemNameWeight = 1f
private const val MinQuantity = 1
private const val QuantityStep = 1
