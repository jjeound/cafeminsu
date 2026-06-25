package com.cafeminsu.ui.feature.gift

import android.content.Context
import android.content.Intent
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafeminsu.R
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
    onClaimEntryClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GiftViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                // 구매는 이미 성공. 클레임 링크를 인텐트로 카카오톡에 공유한다(미설치 시 시스템 공유 시트).
                is GiftEvent.ShareGiftLink -> shareGiftLinkToKakao(context, event.shareText)

                else -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    GiftScreen(
        state = state,
        onBackClick = onBackClick,
        onLoginClick = onLoginClick,
        onClaimEntryClick = onClaimEntryClick,
        onRetry = viewModel::retry,
        onAmountSelected = viewModel::onAmountSelected,
        onCustomAmountChanged = viewModel::onCustomAmountChanged,
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
    onMessageChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCustomAmountChanged: (String) -> Unit = {},
    onClaimEntryClick: () -> Unit = {},
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = CafeTheme.colors.canvas,
        contentColor = CafeTheme.colors.body,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CafeTopBar(
                title = "선물하기",
                navigationIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_left),
                        contentDescription = null,
                        tint = CafeTheme.colors.ink,
                    )
                },
                onNavigationClick = onBackClick,
                actionIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_ticket),
                        contentDescription = "선물 등록",
                        tint = CafeTheme.colors.ink,
                    )
                },
                onActionClick = onClaimEntryClick,
            )

            when (state) {
                GiftUiState.Loading -> StateContainer {
                    LoadingView()
                }

                is GiftUiState.Content -> GiftForm(
                    state = state,
                    onAmountSelected = onAmountSelected,
                    onCustomAmountChanged = onCustomAmountChanged,
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
            GiftInputSection(
                state = state,
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
private fun GiftInputSection(
    state: GiftUiState.Content,
    onMessageChanged: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space3)) {
        // 카카오톡 단일 채널: 구매 후 인텐트 공유로 전달하므로 별도 수신자 입력이 없다(대화방은 카톡 앱에서 선택).
        SectionLabel(text = "받는 사람")
        KakaoShareNotice()
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
private fun KakaoShareNotice() {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = spacing.space10 + spacing.space3),
        shape = CafeTheme.shapes.radiusMd,
        color = colors.surfaceCard,
        contentColor = colors.body,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "구매 후 카카오톡으로 공유해요",
                style = CafeTheme.typography.body,
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

// 구매 성공 후 클레임 링크(공유 텍스트)를 ACTION_SEND 인텐트로 카카오톡에 공유한다.
// 카카오톡을 우선 대상으로 시도하고, 미설치/실패 시 시스템 공유 시트로 폴백한다.
// 공유 단계 실패는 선물 실패가 아니다(구매는 이미 성공). 링크/코드는 로깅하지 않는다(SECURITY §4).
private fun shareGiftLinkToKakao(context: Context, shareText: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }

    val toKakao = Intent(sendIntent)
        .setPackage(KakaoTalkPackage)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val launchedKakao = runCatching { context.startActivity(toKakao) }.isSuccess
    if (launchedKakao) return

    val chooser = Intent.createChooser(sendIntent, "선물 공유")
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val launchedChooser = runCatching { context.startActivity(chooser) }.isSuccess
    if (!launchedChooser) {
        Toast.makeText(context, "선물 링크를 공유하지 못했어요", Toast.LENGTH_SHORT).show()
    }
}

private const val KakaoTalkPackage = "com.kakao.talk"
private const val FormWeight = 1f
private const val AmountOptionWeight = 1f
private const val BorderWidthDivider = 4
