package com.cafeminsu.ui.feature.payment

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.SelectedOption
import com.cafeminsu.ui.components.CafeButton
import com.cafeminsu.ui.components.CafeButtonVariant
import com.cafeminsu.ui.components.CafeCard
import com.cafeminsu.ui.components.CafeCardType
import com.cafeminsu.ui.components.CafeTopBar
import com.cafeminsu.ui.components.ErrorView
import com.cafeminsu.ui.components.LoadingView
import com.cafeminsu.ui.theme.CafeTheme
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PaymentRoute(
    onPaymentApproved: (String) -> Unit,
    onPaymentFailed: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PaymentViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is PaymentEvent.PaymentApproved -> onPaymentApproved(event.orderId)
                is PaymentEvent.PaymentFailed -> onPaymentFailed()
            }
        }
    }

    PaymentScreen(
        state = state,
        onBackClick = onBackClick,
        onSelectMethod = viewModel::onSelectMethod,
        onPaymentSuccess = viewModel::onPaySuccess,
        onPaymentFailure = viewModel::onPayFailure,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
fun PaymentScreen(
    state: PaymentUiState,
    onBackClick: () -> Unit,
    onSelectMethod: (String) -> Unit,
    onPaymentSuccess: () -> Unit,
    onPaymentFailure: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.canvas,
        topBar = {
            CafeTopBar(
                title = "결제",
                navigationIcon = {
                    Text(
                        text = "‹",
                        style = CafeTheme.typography.h2,
                        color = colors.ink,
                    )
                },
                onNavigationClick = onBackClick,
            )
        },
        bottomBar = {
            if (state is PaymentUiState.Content) {
                PaymentActionBar(
                    state = state,
                    onPaymentSuccess = onPaymentSuccess,
                    onPaymentFailure = onPaymentFailure,
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
                PaymentUiState.Loading -> LoadingView(modifier = Modifier.padding(screenPadding()))
                is PaymentUiState.Content -> PaymentContent(
                    state = state,
                    onSelectMethod = onSelectMethod,
                )

                is PaymentUiState.Error -> ErrorView(
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
private fun PaymentContent(
    state: PaymentUiState.Content,
    onSelectMethod: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding(),
        verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space4),
    ) {
        item {
            PaymentInfoBanner()
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space3)) {
                SectionLabel(text = "결제 수단")
                PaymentMethodSegments(
                    methods = state.methods,
                    selectedMethodId = state.selectedMethodId,
                    enabled = state.paymentState !is PaymentProgress.Processing,
                    onSelectMethod = onSelectMethod,
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space3)) {
                SectionLabel(text = "주문 요약")
                OrderSummaryCard(state = state)
            }
        }

        item {
            PaymentProgressMessage(progress = state.paymentState)
        }
    }
}

@Composable
private fun PaymentInfoBanner() {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CafeTheme.shapes.radiusMd,
        color = colors.accentSoft,
        contentColor = colors.muted,
    ) {
        Text(
            modifier = Modifier.padding(
                horizontal = spacing.space3,
                vertical = spacing.space2,
            ),
            text = "ⓘ PG 미연동 — Mock 성공/실패 분기로 대체",
            style = CafeTheme.typography.caption,
            color = colors.muted,
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
private fun PaymentMethodSegments(
    methods: List<PaymentMethodUiModel>,
    selectedMethodId: String,
    enabled: Boolean,
    onSelectMethod: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space2),
    ) {
        methods.forEach { method ->
            PaymentMethodSegment(
                text = method.label,
                selected = method.id == selectedMethodId,
                enabled = enabled,
                onClick = { onSelectMethod(method.id) },
                modifier = Modifier.weight(SegmentWeight),
            )
        }
    }
}

@Composable
private fun PaymentMethodSegment(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Surface(
        onClick = onClick,
        modifier = modifier.height(spacing.space10),
        enabled = enabled,
        shape = CafeTheme.shapes.radiusMd,
        color = if (selected) colors.surfaceDark else colors.canvas,
        contentColor = if (selected) colors.onDark else colors.ink,
        border = if (selected) {
            null
        } else {
            BorderStroke(spacing.space1 / BorderWidthDivider, colors.hairline)
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.space2),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = CafeTheme.typography.caption,
                color = when {
                    !enabled -> colors.muted
                    selected -> colors.onDark
                    else -> colors.ink
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun OrderSummaryCard(state: PaymentUiState.Content) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    CafeCard(
        modifier = Modifier.fillMaxWidth(),
        type = CafeCardType.Default,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
            state.items.forEach { item ->
                OrderItemRow(item = item)
            }

            HorizontalDivider(color = colors.hairline)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "총 결제 금액",
                    style = CafeTheme.typography.bodyL,
                    color = colors.ink,
                )
                Text(
                    text = formatWon(state.totalAmount),
                    style = CafeTheme.typography.h3,
                    color = colors.ink,
                )
            }
        }
    }
}

@Composable
private fun OrderItemRow(item: CartItem) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = item.paymentLineText(),
            modifier = Modifier.weight(ItemTextWeight),
            style = CafeTheme.typography.body,
            color = colors.body,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.width(spacing.space3))

        Text(
            text = formatWon(item.unitPrice * item.quantity),
            style = CafeTheme.typography.bodyL,
            color = colors.ink,
            maxLines = 1,
        )
    }
}

@Composable
private fun PaymentProgressMessage(progress: PaymentProgress) {
    when (progress) {
        PaymentProgress.Idle -> Unit
        PaymentProgress.Processing -> PaymentStateCard(
            title = "결제 처리 중",
            message = "승인 결과를 확인하고 있어요.",
            tone = PaymentMessageTone.Warning,
        )

        PaymentProgress.Approved -> PaymentStateCard(
            title = "결제 승인 완료",
            message = "주문 상태 화면에서 준비 과정을 확인해 주세요.",
            tone = PaymentMessageTone.Success,
        )

        is PaymentProgress.Failed -> PaymentStateCard(
            title = "결제를 완료하지 못했어요",
            message = progress.message,
            tone = PaymentMessageTone.Error,
        )

        is PaymentProgress.NeedsConfirmation -> PaymentStateCard(
            title = "결제 상태 확인 필요",
            message = progress.message,
            tone = PaymentMessageTone.Warning,
        )
    }
}

@Composable
private fun PaymentStateCard(
    title: String,
    message: String,
    tone: PaymentMessageTone,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing
    val borderColor = when (tone) {
        PaymentMessageTone.Success -> colors.success
        PaymentMessageTone.Warning -> colors.warning
        PaymentMessageTone.Error -> colors.error
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CafeTheme.shapes.radiusLg,
        color = colors.canvas,
        contentColor = colors.body,
        border = BorderStroke(spacing.space1 / BorderWidthDivider, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space2),
        ) {
            Text(
                text = title,
                style = CafeTheme.typography.h3,
                color = colors.ink,
            )
            Text(
                text = message,
                style = CafeTheme.typography.body,
                color = colors.body,
            )
        }
    }
}

@Composable
private fun PaymentActionBar(
    state: PaymentUiState.Content,
    onPaymentSuccess: () -> Unit,
    onPaymentFailure: () -> Unit,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.canvas,
        contentColor = colors.body,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = spacing.space5,
                vertical = spacing.space4,
            ),
            horizontalArrangement = Arrangement.spacedBy(spacing.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CafeButton(
                text = "결제 실패",
                onClick = onPaymentFailure,
                modifier = Modifier.weight(ActionButtonWeight),
                variant = CafeButtonVariant.Secondary,
                enabled = state.isPayEnabled,
            )
            CafeButton(
                text = "결제 성공",
                onClick = onPaymentSuccess,
                modifier = Modifier.weight(ActionButtonWeight),
                variant = CafeButtonVariant.Primary,
                enabled = state.isPayEnabled,
            )
        }
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

private fun CartItem.paymentLineText(): String {
    val options = selectedOptions.paymentSummary()
    val nameWithOptions = if (options.isBlank()) {
        name
    } else {
        "$name ($options)"
    }
    return "$nameWithOptions ✕ $quantity"
}

private fun List<SelectedOption>.paymentSummary(): String =
    joinToString(separator = "/") { option -> option.name }

private fun formatWon(amount: Int): String =
    "${NumberFormat.getNumberInstance(Locale.KOREA).format(amount)}원"

private enum class PaymentMessageTone {
    Success,
    Warning,
    Error,
}

private const val ActionButtonWeight = 1f
private const val BorderWidthDivider = 4
private const val ItemTextWeight = 1f
private const val SegmentWeight = 1f
