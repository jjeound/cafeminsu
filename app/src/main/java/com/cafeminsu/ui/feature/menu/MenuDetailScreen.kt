package com.cafeminsu.ui.feature.menu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafeminsu.ui.components.CafeButton
import com.cafeminsu.ui.components.CafeButtonVariant
import com.cafeminsu.ui.components.CafeCard
import com.cafeminsu.ui.components.CafeCardType
import com.cafeminsu.ui.components.CafeChip
import com.cafeminsu.ui.components.CafeSnackbarHost
import com.cafeminsu.ui.components.CafeSnackbarType
import com.cafeminsu.ui.components.ErrorView
import com.cafeminsu.ui.components.LoadingView
import com.cafeminsu.ui.components.cafeSnackbar
import com.cafeminsu.ui.theme.CafeTheme
import java.text.NumberFormat
import java.util.Locale

@Composable
fun MenuDetailRoute(
    onAddedToCart: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MenuDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                MenuDetailEvent.AddedToCart -> {
                    snackbarHostState.cafeSnackbar(
                        message = "장바구니에 담았어요",
                        type = CafeSnackbarType.Success,
                    )
                    onAddedToCart()
                }
            }
        }
    }

    MenuDetailScreen(
        state = state,
        onOptionToggle = viewModel::onOptionToggle,
        onQuantityChange = viewModel::onQuantityChange,
        onAddToCart = viewModel::onAddToCart,
        onRetry = viewModel::retry,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
fun MenuDetailScreen(
    state: MenuDetailUiState,
    onOptionToggle: (groupId: String, optionId: String) -> Unit,
    onQuantityChange: (Int) -> Unit,
    onAddToCart: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CafeTheme.colors.canvas,
        snackbarHost = { CafeSnackbarHost(snackbarHostState) },
        bottomBar = {
            if (state is MenuDetailUiState.Content) {
                AddToCartBar(
                    state = state,
                    onQuantityChange = onQuantityChange,
                    onAddToCart = onAddToCart,
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
                    .verticalScroll(rememberScrollState())
                    .padding(screenPadding()),
                verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space5),
            ) {
                Text(
                    text = "메뉴 상세",
                    style = CafeTheme.typography.h1,
                    color = CafeTheme.colors.ink,
                )

                when (state) {
                    MenuDetailUiState.Loading -> LoadingView()
                    is MenuDetailUiState.Content -> MenuDetailContent(
                        state = state,
                        onOptionToggle = onOptionToggle,
                    )

                    is MenuDetailUiState.Error -> ErrorView(
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
private fun MenuDetailContent(
    state: MenuDetailUiState.Content,
    onOptionToggle: (groupId: String, optionId: String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space5),
    ) {
        MenuSummaryCard(state)

        state.optionGroups.forEach { group ->
            OptionGroupCard(
                group = group,
                onOptionToggle = onOptionToggle,
            )
        }

        if (state.optionGroups.isEmpty()) {
            CafeCard(type = CafeCardType.Info) {
                Text(
                    text = "추가 옵션 없이 바로 담을 수 있어요",
                    style = CafeTheme.typography.body,
                    color = CafeTheme.colors.body,
                )
            }
        }
    }
}

@Composable
private fun MenuSummaryCard(state: MenuDetailUiState.Content) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    CafeCard(
        modifier = Modifier.fillMaxWidth(),
        type = CafeCardType.Product,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(MenuNameWeight),
                    verticalArrangement = Arrangement.spacedBy(spacing.space2),
                ) {
                    Text(
                        text = state.name,
                        style = CafeTheme.typography.h2,
                        color = colors.onDark,
                    )
                    Text(
                        text = state.description,
                        style = CafeTheme.typography.body,
                        color = colors.onDark,
                    )
                }

                if (state.isSoldOut) {
                    Spacer(modifier = Modifier.width(spacing.space3))
                    SoldOutBadge()
                }
            }

            Text(
                text = formatWon(state.basePrice),
                style = CafeTheme.typography.bodyL,
                color = colors.primary,
            )
        }
    }
}

@Composable
private fun OptionGroupCard(
    group: MenuDetailOptionGroupUiModel,
    onOptionToggle: (groupId: String, optionId: String) -> Unit,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    CafeCard(
        modifier = Modifier.fillMaxWidth(),
        type = CafeCardType.Default,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
                Text(
                    text = group.name,
                    style = CafeTheme.typography.h3,
                    color = colors.ink,
                )
                Text(
                    text = group.helperText,
                    style = CafeTheme.typography.caption,
                    color = if (group.isSatisfied) colors.muted else colors.error,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(spacing.space2),
            ) {
                group.options.forEach { option ->
                    CafeChip(
                        text = option.label(),
                        selected = option.selected,
                        enabled = option.isAvailable,
                        onClick = { onOptionToggle(group.id, option.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AddToCartBar(
    state: MenuDetailUiState.Content,
    onQuantityChange: (Int) -> Unit,
    onAddToCart: () -> Unit,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.canvas),
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
            val addError = state.addStatus as? MenuDetailAddStatus.Error
            if (state.isSoldOut) {
                Text(
                    text = "품절된 메뉴예요",
                    style = CafeTheme.typography.caption,
                    color = colors.error,
                )
            } else if (addError != null) {
                Text(
                    text = addError.message,
                    style = CafeTheme.typography.caption,
                    color = colors.error,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuantityStepper(
                    quantity = state.quantity,
                    onQuantityChange = onQuantityChange,
                )

                Text(
                    text = formatWon(state.totalPrice),
                    style = CafeTheme.typography.bodyL,
                    color = colors.primary,
                )
            }

            CafeButton(
                text = "담기 · ${formatWon(state.totalPrice)}",
                onClick = onAddToCart,
                modifier = Modifier.fillMaxWidth(),
                variant = CafeButtonVariant.Primary,
                enabled = state.canAddToCart,
            )
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
private fun screenPadding(): PaddingValues =
    PaddingValues(
        start = CafeTheme.spacing.space5,
        top = CafeTheme.spacing.space6,
        end = CafeTheme.spacing.space5,
        bottom = CafeTheme.spacing.space6,
    )

private fun MenuDetailOptionUiModel.label(): String =
    if (extraPrice == NoExtraPrice) {
        name
    } else {
        "$name +${formatWon(extraPrice)}"
    }

private fun formatWon(amount: Int): String =
    "${NumberFormat.getNumberInstance(Locale.KOREA).format(amount)}원"

private const val BorderWidthDivider = 4
private const val MenuNameWeight = 1f
private const val MinQuantity = 1
private const val QuantityStep = 1
private const val NoExtraPrice = 0
