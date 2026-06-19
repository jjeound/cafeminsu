package com.cafeminsu.ui.feature.payment

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.SelectedOption
import com.cafeminsu.ui.components.CafeButton
import com.cafeminsu.ui.components.CafeButtonVariant
import com.cafeminsu.ui.components.CafeCard
import com.cafeminsu.ui.components.CafeCardType
import com.cafeminsu.ui.components.CafeChip
import com.cafeminsu.ui.components.ErrorView
import com.cafeminsu.ui.components.LoadingView
import com.cafeminsu.ui.theme.CafeTheme
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PaymentRoute(
    onPaymentApproved: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PaymentViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is PaymentEvent.PaymentApproved -> onPaymentApproved(event.orderId)
            }
        }
    }

    PaymentScreen(
        state = state,
        onSelectMethod = viewModel::onSelectMethod,
        onPay = viewModel::onPay,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
fun PaymentScreen(
    state: PaymentUiState,
    onSelectMethod: (String) -> Unit,
    onPay: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CafeTheme.colors.canvas,
        bottomBar = {
            if (state is PaymentUiState.Content) {
                PaymentActionBar(
                    state = state,
                    onPay = onPay,
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
                    text = "결제",
                    style = CafeTheme.typography.h1,
                    color = CafeTheme.colors.ink,
                )

                when (state) {
                    PaymentUiState.Loading -> LoadingView()
                    is PaymentUiState.Content -> PaymentContent(
                        state = state,
                        onSelectMethod = onSelectMethod,
                        modifier = Modifier.weight(ContentWeight),
                    )

                    is PaymentUiState.Error -> ErrorView(
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
private fun PaymentContent(
    state: PaymentUiState.Content,
    onSelectMethod: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = CafeTheme.spacing.space6),
        verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space5),
    ) {
        item {
            OrderSummaryCard(state = state)
        }

        item {
            PaymentMethodCard(
                methods = state.methods,
                selectedMethodId = state.selectedMethodId,
                enabled = state.paymentState !is PaymentProgress.Processing,
                onSelectMethod = onSelectMethod,
            )
        }

        item {
            PaymentProgressMessage(progress = state.paymentState)
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
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
                    Text(
                        text = "주문번호",
                        style = CafeTheme.typography.caption,
                        color = colors.muted,
                    )
                    Text(
                        text = state.orderNumber,
                        style = CafeTheme.typography.h3,
                        color = colors.ink,
                    )
                }

                Text(
                    text = formatWon(state.totalAmount),
                    style = CafeTheme.typography.h2,
                    color = colors.primary,
                )
            }

            HorizontalDivider(color = colors.hairline)

            Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
                state.items.forEach { item ->
                    OrderItemRow(item = item)
                }
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
        Column(
            modifier = Modifier.weight(ItemTextWeight),
            verticalArrangement = Arrangement.spacedBy(spacing.space1),
        ) {
            Text(
                text = "${item.name} ${item.quantity}개",
                style = CafeTheme.typography.bodyL,
                color = colors.ink,
            )
            Text(
                text = item.selectedOptions.summaryText(),
                style = CafeTheme.typography.caption,
                color = colors.muted,
            )
        }

        Spacer(modifier = Modifier.width(spacing.space3))

        Text(
            text = formatWon(item.unitPrice * item.quantity),
            style = CafeTheme.typography.bodyL,
            color = colors.body,
        )
    }
}

@Composable
private fun PaymentMethodCard(
    methods: List<PaymentMethodUiModel>,
    selectedMethodId: String,
    enabled: Boolean,
    onSelectMethod: (String) -> Unit,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    CafeCard(
        modifier = Modifier.fillMaxWidth(),
        type = CafeCardType.Info,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
            Text(
                text = "결제수단",
                style = CafeTheme.typography.h3,
                color = colors.ink,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                methods.forEach { method ->
                    CafeChip(
                        text = method.label,
                        selected = method.id == selectedMethodId,
                        onClick = { onSelectMethod(method.id) },
                        enabled = enabled,
                    )
                }
            }
        }
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
            modifier = Modifier.padding(spacing.space5),
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
    onPay: () -> Unit,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing
    val processing = state.paymentState is PaymentProgress.Processing

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "결제 금액",
                    style = CafeTheme.typography.caption,
                    color = colors.muted,
                )
                Text(
                    text = formatWon(state.totalAmount),
                    style = CafeTheme.typography.h3,
                    color = colors.primary,
                )
            }

            CafeButton(
                text = if (processing) "결제 처리 중" else "${formatWon(state.totalAmount)} 결제",
                onClick = onPay,
                modifier = Modifier.fillMaxWidth(),
                variant = CafeButtonVariant.Primary,
                enabled = state.isPayEnabled,
                icon = if (processing) {
                    {
                        CircularProgressIndicator(
                            modifier = Modifier.size(spacing.space5),
                            color = colors.muted,
                            strokeWidth = spacing.space1 / ProgressStrokeDivider,
                        )
                    }
                } else {
                    null
                },
            )
        }
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

private fun formatWon(amount: Int): String =
    "${NumberFormat.getNumberInstance(Locale.KOREA).format(amount)}원"

private enum class PaymentMessageTone {
    Success,
    Warning,
    Error,
}

private const val BorderWidthDivider = 4
private const val ProgressStrokeDivider = 2
private const val ContentWeight = 1f
private const val ItemTextWeight = 1f
