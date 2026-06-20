package com.cafeminsu.ui.feature.order

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafeminsu.ui.components.CafeButton
import com.cafeminsu.ui.components.CafeButtonVariant
import com.cafeminsu.ui.components.CafeCard
import com.cafeminsu.ui.components.CafeCardType
import com.cafeminsu.ui.components.ErrorView
import com.cafeminsu.ui.components.LoadingView
import com.cafeminsu.ui.theme.CafeTheme

@Composable
fun OrderResultRoute(
    onCloseClick: () -> Unit,
    onStatusClick: (String) -> Unit,
    onHomeClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OrderResultViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    OrderResultScreen(
        state = state,
        onCloseClick = onCloseClick,
        onStatusClick = onStatusClick,
        onHomeClick = onHomeClick,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
fun OrderResultScreen(
    state: OrderResultUiState,
    onCloseClick: () -> Unit,
    onStatusClick: (String) -> Unit,
    onHomeClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = CafeTheme.colors.canvas,
        contentColor = CafeTheme.colors.body,
    ) {
        when (state) {
            OrderResultUiState.Loading -> LoadingView(
                modifier = Modifier.padding(CafeTheme.spacing.space5),
            )

            is OrderResultUiState.Content -> OrderSuccessContent(
                summary = state.summary,
                onCloseClick = onCloseClick,
                onStatusClick = onStatusClick,
                onHomeClick = onHomeClick,
            )

            is OrderResultUiState.Failure -> ErrorView(
                modifier = Modifier.padding(CafeTheme.spacing.space5),
                message = state.message,
                retryable = state.retryable,
                onRetry = onRetry,
            )
        }
    }
}

@Composable
fun OrderFailureDialog(
    failure: OrderFailureUiModel,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ink.copy(alpha = DialogScrimAlpha))
            .padding(horizontal = spacing.space8),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = CafeTheme.shapes.radiusXl,
            color = colors.canvas,
            contentColor = colors.body,
        ) {
            Column(
                modifier = Modifier.padding(
                    start = spacing.space4,
                    top = spacing.space8,
                    end = spacing.space4,
                    bottom = spacing.space4,
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.space3),
            ) {
                FailureIcon()

                Text(
                    text = failure.title,
                    style = CafeTheme.typography.h2,
                    color = colors.ink,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = failure.message,
                    style = CafeTheme.typography.body,
                    color = colors.muted,
                    textAlign = TextAlign.Center,
                )

                ErrorCodeChip(errorCode = failure.errorCode)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.space1),
                ) {
                    CafeButton(
                        text = "취소",
                        onClick = onCancel,
                        modifier = Modifier.weight(ActionButtonWeight),
                        variant = CafeButtonVariant.Secondary,
                    )
                    CafeButton(
                        text = "다시 시도",
                        onClick = onRetry,
                        modifier = Modifier.weight(ActionButtonWeight),
                        variant = CafeButtonVariant.Primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderSuccessContent(
    summary: OrderSuccessSummary,
    onCloseClick: () -> Unit,
    onStatusClick: (String) -> Unit,
    onHomeClick: () -> Unit,
) {
    val spacing = CafeTheme.spacing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = spacing.space5),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(spacing.space14),
            contentAlignment = Alignment.CenterEnd,
        ) {
            CloseButton(onClick = onCloseClick)
        }

        Spacer(modifier = Modifier.height(spacing.space6))
        SuccessIcon()
        Spacer(modifier = Modifier.height(spacing.space8))

        Text(
            text = "주문이 완료됐어요",
            style = CafeTheme.typography.display,
            color = CafeTheme.colors.ink,
            textAlign = TextAlign.Center,
        )
        Text(
            modifier = Modifier.padding(top = spacing.space2),
            text = "준비가 끝나면 알림을 보내드릴게요",
            style = CafeTheme.typography.body,
            color = CafeTheme.colors.muted,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(spacing.space8))
        OrderSummaryCard(summary = summary)
        Spacer(modifier = Modifier.height(spacing.space5))
        StampEarnedBanner(message = summary.stampMessage)

        Spacer(modifier = Modifier.weight(ContentBottomWeight))

        CafeButton(
            text = "주문 상태 보기",
            onClick = { onStatusClick(summary.orderId) },
            modifier = Modifier.fillMaxWidth(),
            variant = CafeButtonVariant.Primary,
        )
        Spacer(modifier = Modifier.height(spacing.space3))
        CafeButton(
            text = "홈으로 이동",
            onClick = onHomeClick,
            modifier = Modifier.fillMaxWidth(),
            variant = CafeButtonVariant.Secondary,
        )
        Spacer(modifier = Modifier.height(spacing.space5))
    }
}

@Composable
private fun CloseButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(CafeTheme.spacing.space10 + CafeTheme.spacing.space2)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "×",
            style = CafeTheme.typography.h1,
            color = CafeTheme.colors.ink,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SuccessIcon() {
    Box(
        modifier = Modifier
            .size(CafeTheme.spacing.space18 + CafeTheme.spacing.space6)
            .clip(CircleShape)
            .background(CafeTheme.colors.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "✓",
            style = CafeTheme.typography.display,
            color = CafeTheme.colors.onPrimary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FailureIcon() {
    Box(
        modifier = Modifier
            .size(CafeTheme.spacing.space10 + CafeTheme.spacing.space6)
            .clip(CircleShape)
            .background(CafeTheme.colors.error.copy(alpha = FailureIconBackgroundAlpha)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "×",
            style = CafeTheme.typography.display,
            color = CafeTheme.colors.error,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun OrderSummaryCard(summary: OrderSuccessSummary) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    CafeCard(
        modifier = Modifier.fillMaxWidth(),
        type = CafeCardType.Product,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
                Text(
                    text = "주문 번호",
                    style = CafeTheme.typography.caption,
                    color = colors.muted,
                )
                Text(
                    text = summary.orderNumber,
                    style = CafeTheme.typography.h1,
                    color = colors.onDark,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(spacing.space1 / BorderWidthDivider)
                    .background(colors.muted.copy(alpha = SummaryDividerAlpha)),
            )

            SummaryRow(label = "픽업 매장", value = summary.pickupStoreName)
            SummaryRow(label = "예상 완성", value = summary.estimatedReadyLabel)
            SummaryRow(label = "결제 금액", value = summary.paidAmountLabel)
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = CafeTheme.typography.caption,
            color = CafeTheme.colors.muted,
        )
        Text(
            text = value,
            style = CafeTheme.typography.bodyL,
            color = CafeTheme.colors.onDark,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun StampEarnedBanner(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CafeTheme.shapes.radiusLg,
        color = CafeTheme.colors.surfaceCard,
        contentColor = CafeTheme.colors.body,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = CafeTheme.spacing.space4,
                vertical = CafeTheme.spacing.space4,
            ),
            horizontalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "☆",
                style = CafeTheme.typography.h2,
                color = CafeTheme.colors.primary,
            )
            Text(
                text = message,
                style = CafeTheme.typography.bodyL,
                color = CafeTheme.colors.ink,
            )
        }
    }
}

@Composable
private fun ErrorCodeChip(errorCode: String) {
    Surface(
        shape = CafeTheme.shapes.radiusPill,
        color = CafeTheme.colors.surfaceCard,
        contentColor = CafeTheme.colors.muted,
    ) {
        Text(
            modifier = Modifier.padding(
                horizontal = CafeTheme.spacing.space3,
                vertical = CafeTheme.spacing.space1,
            ),
            text = errorCode,
            style = CafeTheme.typography.caption,
            color = CafeTheme.colors.muted,
        )
    }
}

private const val DialogScrimAlpha = 0.45f
private const val FailureIconBackgroundAlpha = 0.12f
private const val SummaryDividerAlpha = 0.24f
private const val BorderWidthDivider = 4
private const val ActionButtonWeight = 1f
private const val ContentBottomWeight = 1f
