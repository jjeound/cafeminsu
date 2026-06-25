package com.cafeminsu.ui.feature.menu

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.cafeminsu.R
import com.cafeminsu.ui.components.CafeButton
import com.cafeminsu.ui.components.CafeButtonVariant
import com.cafeminsu.ui.components.CafeSnackbarHost
import com.cafeminsu.ui.components.CafeTopBar
import com.cafeminsu.ui.components.ErrorView
import com.cafeminsu.ui.components.LoadingView
import com.cafeminsu.ui.theme.CafeTheme
import java.text.NumberFormat
import java.util.Locale

@Composable
fun MenuDetailRoute(
    onAddedToCart: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MenuDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                // 담기 성공 시 스낵바를 기다리지 않고 즉시 이전 화면으로 돌아간다(체감 지연 제거).
                MenuDetailEvent.AddedToCart -> onAddedToCart()
            }
        }
    }

    MenuDetailScreen(
        state = state,
        onBackClick = onBackClick,
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
    onBackClick: () -> Unit,
    onOptionToggle: (groupId: String, optionId: String) -> Unit,
    onQuantityChange: (Int) -> Unit,
    onAddToCart: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var favorite by rememberSaveable { mutableStateOf(false) }
    val colors = CafeTheme.colors

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.canvas,
        topBar = {
            CafeTopBar(
                title = "메뉴 상세",
                navigationIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_left),
                        contentDescription = null,
                        tint = colors.ink,
                    )
                },
                onNavigationClick = onBackClick,
                actionIcon = {
                    Icon(
                        painter = painterResource(
                            if (favorite) R.drawable.ic_heart_filled else R.drawable.ic_heart,
                        ),
                        contentDescription = if (favorite) "찜 해제" else "찜하기",
                        tint = if (favorite) colors.primary else colors.ink,
                    )
                },
                onActionClick = { favorite = !favorite },
            )
        },
        snackbarHost = { CafeSnackbarHost(snackbarHostState) },
        bottomBar = {
            if (state is MenuDetailUiState.Content) {
                AddToCartBar(
                    state = state,
                    onAddToCart = onAddToCart,
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
                MenuDetailUiState.Loading -> LoadingView(
                    modifier = Modifier.padding(CafeTheme.spacing.space5),
                )

                is MenuDetailUiState.Content -> MenuDetailContent(
                    state = state,
                    onOptionToggle = onOptionToggle,
                    onQuantityChange = onQuantityChange,
                )

                is MenuDetailUiState.Error -> ErrorView(
                    message = state.message,
                    retryable = state.retryable,
                    onRetry = onRetry,
                    modifier = Modifier.padding(CafeTheme.spacing.space5),
                )
            }
        }
    }
}

@Composable
private fun MenuDetailContent(
    state: MenuDetailUiState.Content,
    onOptionToggle: (groupId: String, optionId: String) -> Unit,
    onQuantityChange: (Int) -> Unit,
) {
    val spacing = CafeTheme.spacing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        MenuImageHero(imageUrl = state.imageUrl, categoryId = state.categoryId)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding()),
            verticalArrangement = Arrangement.spacedBy(spacing.space5),
        ) {
            MenuInfo(state = state)

            HorizontalDivider(color = CafeTheme.colors.hairline)

            state.optionGroups.forEach { group ->
                OptionGroup(
                    group = group,
                    onOptionToggle = onOptionToggle,
                )
            }

            QuantitySection(
                quantity = state.quantity,
                onQuantityChange = onQuantityChange,
            )
        }
    }
}

@Composable
private fun MenuImageHero(imageUrl: String?, categoryId: String) {
    val spacing = CafeTheme.spacing
    val colors = CafeTheme.colors
    val defaultImage = painterResource(defaultMenuImageRes(categoryId))

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(spacing.space18 + spacing.space18 + spacing.space18),
        color = colors.surfaceCard,
        contentColor = colors.primary,
    ) {
        Box(contentAlignment = Alignment.Center) {
            AsyncImage(
                model = imageUrl?.takeIf { it.isNotBlank() },
                contentDescription = null,
                modifier = Modifier
                    .size(spacing.space18 + spacing.space18)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                placeholder = defaultImage,
                error = defaultImage,
                fallback = defaultImage,
            )
        }
    }
}

@Composable
private fun MenuInfo(state: MenuDetailUiState.Content) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                modifier = Modifier.weight(ContentWeight),
                text = state.name,
                style = CafeTheme.typography.h1,
                color = colors.ink,
                maxLines = MenuNameMaxLines,
                overflow = TextOverflow.Ellipsis,
            )

            if (state.isSoldOut) {
                Spacer(modifier = Modifier.width(spacing.space3))
                SoldOutBadge()
            }
        }

        Text(
            text = state.description,
            style = CafeTheme.typography.body,
            color = colors.muted,
        )

        Text(
            text = formatWon(state.basePrice),
            style = CafeTheme.typography.h2,
            color = colors.primary,
        )
    }
}

@Composable
private fun OptionGroup(
    group: MenuDetailOptionGroupUiModel,
    onOptionToggle: (groupId: String, optionId: String) -> Unit,
) {
    val spacing = CafeTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.space1),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = group.name,
                style = CafeTheme.typography.body,
                color = CafeTheme.colors.body,
            )

            if (group.required) {
                Text(
                    text = "*필수",
                    style = CafeTheme.typography.caption,
                    color = CafeTheme.colors.muted,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space2),
        ) {
            group.options.forEach { option ->
                SegmentedOption(
                    option = option,
                    onClick = { onOptionToggle(group.id, option.id) },
                    modifier = Modifier.weight(SegmentWeight),
                )
            }
        }
    }
}

@Composable
private fun SegmentedOption(
    option: MenuDetailOptionUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing
    val containerColor = if (option.selected) colors.surfaceDark else colors.surfaceCard
    val contentColor = when {
        !option.isAvailable -> colors.muted
        option.selected -> colors.onDark
        else -> colors.ink
    }
    val border = if (option.selected) {
        null
    } else {
        BorderStroke(spacing.space1 / BorderWidthDivider, colors.hairline)
    }

    Surface(
        onClick = onClick,
        modifier = modifier.height(spacing.space10 + spacing.space3),
        enabled = option.isAvailable,
        shape = CafeTheme.shapes.radiusMd,
        color = containerColor,
        contentColor = contentColor,
        border = border,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = spacing.space2),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = option.label(),
                style = CafeTheme.typography.body,
                color = contentColor,
                maxLines = OptionLabelMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun QuantitySection(
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "수량",
            style = CafeTheme.typography.body,
            color = CafeTheme.colors.body,
        )

        QuantityStepper(
            quantity = quantity,
            onQuantityChange = onQuantityChange,
        )
    }
}

@Composable
private fun QuantityStepper(
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
) {
    val spacing = CafeTheme.spacing

    Surface(
        shape = CafeTheme.shapes.radiusPill,
        color = CafeTheme.colors.surfaceCard,
        contentColor = CafeTheme.colors.ink,
        border = BorderStroke(spacing.space1 / BorderWidthDivider, CafeTheme.colors.hairline),
    ) {
        Row(
            modifier = Modifier.height(spacing.space10 + spacing.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuantityButton(
                text = "−",
                enabled = quantity > MinQuantity,
                onClick = { onQuantityChange(quantity - QuantityStep) },
            )

            Box(
                modifier = Modifier.width(spacing.space10),
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
                enabled = quantity < MaxQuantity,
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
    val spacing = CafeTheme.spacing

    Surface(
        onClick = onClick,
        modifier = Modifier.size(spacing.space10 + spacing.space2),
        enabled = enabled,
        shape = CircleShape,
        color = Color.Transparent,
        contentColor = if (enabled) colors.ink else colors.muted,
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
private fun AddToCartBar(
    state: MenuDetailUiState.Content,
    onAddToCart: () -> Unit,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.canvas,
        contentColor = colors.body,
    ) {
        Column {
            HorizontalDivider(color = colors.hairline)

            Column(
                modifier = Modifier.padding(
                    horizontal = spacing.space5,
                    vertical = spacing.space3,
                ),
                verticalArrangement = Arrangement.spacedBy(spacing.space2),
            ) {
                AddStatusText(state)

                CafeButton(
                    text = if (state.isEditing) {
                        "변경사항 저장 · ${formatWon(state.totalPrice)}"
                    } else {
                        "장바구니 담기 · ${formatWon(state.totalPrice)}"
                    },
                    onClick = onAddToCart,
                    modifier = Modifier.fillMaxWidth(),
                    variant = CafeButtonVariant.Primary,
                    enabled = state.canAddToCart,
                )
            }
        }
    }
}

@Composable
private fun AddStatusText(state: MenuDetailUiState.Content) {
    val addError = state.addStatus as? MenuDetailAddStatus.Error
    val message = when {
        state.isSoldOut -> "품절된 메뉴예요"
        addError != null -> addError.message
        else -> null
    }

    if (message != null) {
        Text(
            text = message,
            style = CafeTheme.typography.caption,
            color = CafeTheme.colors.error,
        )
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
private fun contentPadding(): PaddingValues =
    PaddingValues(
        start = CafeTheme.spacing.space5,
        top = CafeTheme.spacing.space5,
        end = CafeTheme.spacing.space5,
        bottom = CafeTheme.spacing.space6,
    )

private fun MenuDetailOptionUiModel.label(): String =
    if (extraPrice == NoExtraPrice) {
        name
    } else {
        "$name (+${formatNumber(extraPrice)})"
    }

private fun formatWon(amount: Int): String =
    "${formatNumber(amount)}원"

private fun formatNumber(amount: Int): String =
    NumberFormat.getNumberInstance(Locale.KOREA).format(amount)

private const val BorderWidthDivider = 4
private const val ContentWeight = 1f
private const val SegmentWeight = 1f
private const val MenuNameMaxLines = 2
private const val OptionLabelMaxLines = 1
private const val MinQuantity = 1
private const val MaxQuantity = 20
private const val QuantityStep = 1
private const val NoExtraPrice = 0
