package com.cafeminsu.ui.feature.gift

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafeminsu.domain.model.GiftChannel
import com.cafeminsu.ui.components.CafeButton
import com.cafeminsu.ui.components.CafeButtonVariant
import com.cafeminsu.ui.components.CafeTextField
import com.cafeminsu.ui.components.CafeTopBar
import com.cafeminsu.ui.components.EmptyView
import com.cafeminsu.ui.components.ErrorView
import com.cafeminsu.ui.components.LoadingView
import com.cafeminsu.ui.theme.CafeTheme

@Composable
fun GiftRoute(
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GiftViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
        }
    }

    GiftScreen(
        state = state,
        onBackClick = onBackClick,
        onLoginClick = onLoginClick,
        onRetry = viewModel::retry,
        onAmountSelected = viewModel::onAmountSelected,
        onCustomAmountChanged = viewModel::onCustomAmountChanged,
        onChannelSelected = viewModel::onChannelSelected,
        onRecipientChanged = viewModel::onRecipientChanged,
        onMessageChanged = viewModel::onMessageChanged,
        onSendClick = viewModel::sendGift,
        modifier = modifier,
    )
}

@Composable
fun GiftScreen(
    state: GiftUiState,
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit,
    onRetry: () -> Unit,
    onAmountSelected: (GiftAmountOption) -> Unit,
    onChannelSelected: (GiftChannel) -> Unit,
    onRecipientChanged: (String) -> Unit,
    onMessageChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCustomAmountChanged: (String) -> Unit = {},
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = CafeTheme.colors.canvas,
        contentColor = CafeTheme.colors.body,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CafeTopBar(
                title = "선물하기",
                navigationIcon = { Text(text = "‹") },
                onNavigationClick = onBackClick,
            )

            when (state) {
                GiftUiState.Loading -> StateContainer {
                    LoadingView()
                }

                is GiftUiState.Content -> GiftForm(
                    state = state,
                    onAmountSelected = onAmountSelected,
                    onCustomAmountChanged = onCustomAmountChanged,
                    onChannelSelected = onChannelSelected,
                    onRecipientChanged = onRecipientChanged,
                    onMessageChanged = onMessageChanged,
                    onSendClick = onSendClick,
                )

                is GiftUiState.Error -> StateContainer {
                    ErrorView(
                        message = state.message,
                        retryable = state.retryable,
                        onRetry = onRetry,
                    )
                }

                is GiftUiState.NeedsLogin -> StateContainer {
                    EmptyView(
                        message = state.message,
                        actionLabel = state.actionLabel,
                        onAction = onLoginClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun StateContainer(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(CafeTheme.spacing.space5),
    ) {
        content()
    }
}

@Composable
private fun GiftForm(
    state: GiftUiState.Content,
    onAmountSelected: (GiftAmountOption) -> Unit,
    onCustomAmountChanged: (String) -> Unit,
    onChannelSelected: (GiftChannel) -> Unit,
    onRecipientChanged: (String) -> Unit,
    onMessageChanged: (String) -> Unit,
    onSendClick: () -> Unit,
) {
    val spacing = CafeTheme.spacing

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(FormWeight)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = spacing.space5,
                    top = spacing.space6,
                    end = spacing.space5,
                    bottom = spacing.space5,
                ),
            verticalArrangement = Arrangement.spacedBy(spacing.space5),
        ) {
            GiftPreviewCard(amountLabel = state.previewAmountLabel)
            GiftAmountSection(
                state = state,
                onAmountSelected = onAmountSelected,
                onCustomAmountChanged = onCustomAmountChanged,
            )
            GiftChannelSection(
                selectedChannel = state.selectedChannel,
                onChannelSelected = onChannelSelected,
            )
            GiftInputSection(
                state = state,
                onRecipientChanged = onRecipientChanged,
                onMessageChanged = onMessageChanged,
            )
        }

        Box(
            modifier = Modifier.padding(
                start = spacing.space5,
                end = spacing.space5,
                bottom = spacing.space5,
            ),
        ) {
            CafeButton(
                text = state.primaryButtonText,
                onClick = onSendClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.canSend,
                variant = CafeButtonVariant.Primary,
            )
        }
    }
}

@Composable
private fun GiftPreviewCard(amountLabel: String) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CafeTheme.shapes.radiusLg,
        color = colors.primary,
        contentColor = colors.onPrimary,
    ) {
        Column(
            modifier = Modifier.padding(spacing.space5),
            verticalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            Text(
                text = "✱  CAFEMINSO",
                style = CafeTheme.typography.caption,
                color = colors.onPrimary,
            )
            Text(
                text = amountLabel,
                style = CafeTheme.typography.display,
                color = colors.onPrimary,
            )
            Text(
                text = "금액형 기프티콘",
                style = CafeTheme.typography.body,
                color = colors.onPrimary,
            )
        }
    }
}

@Composable
private fun GiftAmountSection(
    state: GiftUiState.Content,
    onAmountSelected: (GiftAmountOption) -> Unit,
    onCustomAmountChanged: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space3)) {
        SectionLabel(text = "금액 선택")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space2),
        ) {
            GiftAmountOption.entries.forEach { option ->
                SelectablePill(
                    text = option.label,
                    selected = option == state.selectedAmountOption,
                    onClick = { onAmountSelected(option) },
                    modifier = Modifier.weight(AmountOptionWeight),
                )
            }
        }

        if (state.selectedAmountOption == GiftAmountOption.Custom) {
            CafeTextField(
                value = state.customAmountText,
                onValueChange = onCustomAmountChanged,
                placeholder = "금액 입력",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
    }
}

@Composable
private fun GiftChannelSection(
    selectedChannel: GiftChannel,
    onChannelSelected: (GiftChannel) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space3)) {
        SectionLabel(text = "받는 방식")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space2),
        ) {
            GiftChannelCard(
                title = "카카오톡",
                subtitle = "친구 선택",
                selected = selectedChannel == GiftChannel.KakaoTalk,
                onClick = { onChannelSelected(GiftChannel.KakaoTalk) },
                modifier = Modifier.weight(ChannelCardWeight),
            )
            GiftChannelCard(
                title = "문자 (SMS)",
                subtitle = "연락처 입력",
                selected = selectedChannel == GiftChannel.Sms,
                onClick = { onChannelSelected(GiftChannel.Sms) },
                modifier = Modifier.weight(ChannelCardWeight),
            )
        }
    }
}

@Composable
private fun GiftInputSection(
    state: GiftUiState.Content,
    onRecipientChanged: (String) -> Unit,
    onMessageChanged: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space3)) {
        SectionLabel(text = "받는 사람")
        CafeTextField(
            value = state.recipient,
            onValueChange = onRecipientChanged,
            placeholder = state.recipientPlaceholder,
        )
        SectionLabel(text = "선물 메시지 (선택)")
        CafeTextField(
            value = state.message,
            onValueChange = onMessageChanged,
            placeholder = "오늘 하루 수고 많았어 ☕",
            singleLine = false,
            modifier = Modifier.heightIn(min = CafeTheme.spacing.space14),
        )
    }
}

@Composable
private fun GiftChannelCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing
    val containerColor = if (selected) {
        colors.surfaceCard
    } else {
        colors.canvas
    }
    val border = if (selected) {
        null
    } else {
        BorderStroke(spacing.space1 / BorderWidthDivider, colors.hairline)
    }

    Surface(
        onClick = onClick,
        modifier = modifier.height(spacing.space14 + spacing.space2),
        shape = CafeTheme.shapes.radiusMd,
        color = containerColor,
        contentColor = colors.body,
        border = border,
    ) {
        Column(
            modifier = Modifier.padding(spacing.space4),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = CafeTheme.typography.bodyL,
                color = colors.ink,
            )
            Text(
                text = subtitle,
                style = CafeTheme.typography.caption,
                color = colors.muted,
            )
        }
    }
}

@Composable
private fun SelectablePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing
    val containerColor = if (selected) {
        colors.surfaceDark
    } else {
        colors.canvas
    }
    val contentColor = if (selected) {
        colors.onDark
    } else {
        colors.ink
    }
    val border = if (selected) {
        null
    } else {
        BorderStroke(spacing.space1 / BorderWidthDivider, colors.hairline)
    }

    Surface(
        onClick = onClick,
        modifier = modifier.height(spacing.space10),
        shape = CafeTheme.shapes.radiusMd,
        color = containerColor,
        contentColor = contentColor,
        border = border,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = CafeTheme.typography.body,
                color = contentColor,
            )
        }
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

private const val FormWeight = 1f
private const val AmountOptionWeight = 1f
private const val ChannelCardWeight = 1f
private const val BorderWidthDivider = 4
