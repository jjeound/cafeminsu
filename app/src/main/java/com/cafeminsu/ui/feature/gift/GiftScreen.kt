package com.cafeminsu.ui.feature.gift

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafeminsu.BuildConfig
import com.cafeminsu.R
import com.cafeminsu.core.AppResult
import com.cafeminsu.data.platform.RealKakaoTalkScopeProvider
import com.cafeminsu.domain.model.GiftChannel
import com.cafeminsu.ui.components.CafeButton
import com.cafeminsu.ui.components.CafeButtonVariant
import com.cafeminsu.ui.components.CafeTextField
import com.cafeminsu.ui.components.CafeTopBar
import com.cafeminsu.ui.components.EmptyView
import com.cafeminsu.ui.components.ErrorView
import com.cafeminsu.ui.components.LoadingView
import com.cafeminsu.ui.theme.CafeTheme
import com.kakao.sdk.friend.client.selectFriend
import com.kakao.sdk.friend.core.PickerClient
import com.kakao.sdk.friend.core.model.OpenPickerFriendRequestParams
import com.kakao.sdk.friend.core.model.SelectedUser
import com.kakao.sdk.friend.core.model.ViewType
import com.kakao.sdk.share.ShareClient
import com.kakao.sdk.talk.TalkApiClient
import com.kakao.sdk.template.model.Link
import com.kakao.sdk.template.model.TextTemplate
import kotlin.coroutines.resume
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

@Composable
fun GiftRoute(
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GiftViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                // 구매는 이미 성공. 친구에게 메시지 전송 → 실패/미가입 시 공유 폴백(내부에서 안내).
                is GiftEvent.SendKakaoMessage ->
                    sendKakaoMessage(context, event.receiverUuid, event.target)

                else -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
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
        onPickFriendClick = {
            coroutineScope.launch {
                pickKakaoFriend(context, viewModel::onFriendSelected)
            }
        },
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
    onPickFriendClick: () -> Unit,
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
                navigationIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_left),
                        contentDescription = null,
                        tint = CafeTheme.colors.ink,
                    )
                },
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
                    onPickFriendClick = onPickFriendClick,
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
    onPickFriendClick: () -> Unit,
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
                onPickFriendClick = onPickFriendClick,
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
    onPickFriendClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space3)) {
        SectionLabel(text = "받는 사람")
        when (state.selectedChannel) {
            GiftChannel.KakaoTalk -> FriendPickButton(
                label = state.friendPickLabel,
                selected = state.hasSelectedFriend,
                onClick = onPickFriendClick,
            )

            GiftChannel.Sms -> CafeTextField(
                value = state.recipient,
                onValueChange = onRecipientChanged,
                placeholder = state.recipientPlaceholder,
            )
        }
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
private fun FriendPickButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(spacing.space10 + spacing.space3),
        shape = CafeTheme.shapes.radiusMd,
        color = colors.surfaceCard,
        contentColor = colors.body,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = CafeTheme.typography.body,
                color = if (selected) colors.ink else colors.muted,
            )
        }
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
        modifier = modifier.heightIn(min = spacing.space18),
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

// 친구 피커: friends/talk_message 스코프를 증분 동의로 보장(step0)한 뒤 단일 친구를 선택한다.
// 선택된 친구의 uuid/표시이름은 로깅하지 않고, 서버 구매 요청에도 보내지 않는다(메시지 전송 전용, SECURITY §4).
private suspend fun pickKakaoFriend(
    context: Context,
    onSelected: (uuid: String, displayName: String) -> Unit,
) {
    if (BuildConfig.KAKAO_NATIVE_APP_KEY.isBlank()) {
        showToast(context, "카카오톡 친구 선택을 사용할 수 없어요")
        return
    }
    val activity = context.findActivity()
    if (activity == null) {
        showToast(context, "카카오톡 친구 선택을 사용할 수 없어요")
        return
    }

    val scopeResult = RealKakaoTalkScopeProvider().ensureFriendMessageScopes(activity)
    if (scopeResult is AppResult.Failure) {
        showToast(context, "친구 메시지 권한이 필요해요")
        return
    }

    val friend = selectSingleFriend(activity)
    if (friend == null) {
        showToast(context, "친구를 선택하지 못했어요")
        return
    }
    onSelected(friend.uuid, friend.displayName)
}

private suspend fun selectSingleFriend(activity: Activity): PickedFriend? =
    suspendCancellableCoroutine { continuation ->
        PickerClient.instance.selectFriend(
            activity,
            OpenPickerFriendRequestParams("선물할 친구 선택"),
            ViewType.POPUP,
            false,
        ) { selectedUsers, error ->
            val user = selectedUsers?.users?.firstOrNull()
            val uuid = user?.uuid
            val friend = if (error == null && !uuid.isNullOrBlank()) {
                PickedFriend(uuid = uuid, displayName = user.pickerDisplayName())
            } else {
                null
            }
            if (continuation.isActive) {
                continuation.resume(friend)
            }
        }
    }

private data class PickedFriend(
    val uuid: String,
    val displayName: String,
)

private fun SelectedUser.pickerDisplayName(): String =
    profileNickname?.takeIf { it.isNotBlank() }
        ?: maskingProfileNickname?.takeIf { it.isNotBlank() }
        ?: "카카오 친구"

// 구매 성공 후 선택한 친구에게 카카오톡 메시지(클레임 링크 버튼)를 보낸다.
// 미설치/권한없음/전송실패/앱 미가입 친구면 공유(ShareClient)로 폴백한다.
// 메시지/공유 실패는 선물 실패가 아니다(구매는 이미 성공). 링크/수신자는 로깅하지 않는다(SECURITY §4).
private fun sendKakaoMessage(
    context: Context,
    receiverUuid: String,
    target: KakaoShareTarget,
) {
    val webLink = target.bestLink()
    if (webLink == null) {
        showToast(context, "선물이 준비됐어요")
        return
    }

    TalkApiClient.instance.sendDefaultMessage(
        listOf(receiverUuid),
        giftLinkTemplate(webLink),
    ) { _, error ->
        when {
            error == null -> showToast(context, "선물을 보냈어요")
            launchKakaoShare(context, target) -> Unit
            else -> showToast(context, "선물 링크를 전달하지 못했어요. 잠시 후 다시 시도해 주세요")
        }
    }
}

// 서버 구매/공유로 받은 링크를 카카오톡 공유 SDK로 띄운다(메시지 폴백).
// 공유를 시도했으면 true, 키/링크 부재로 시도조차 못 하면 false.
private fun launchKakaoShare(context: Context, target: KakaoShareTarget): Boolean {
    if (BuildConfig.KAKAO_NATIVE_APP_KEY.isBlank()) return false
    val webLink = target.bestLink() ?: return false

    if (ShareClient.instance.isKakaoTalkSharingAvailable(context)) {
        ShareClient.instance.shareDefault(context, giftLinkTemplate(webLink)) { result, _ ->
            if (result != null) {
                runCatching { context.startActivity(result.intent) }
            } else {
                openWebShare(context, webLink)
            }
        }
    } else {
        openWebShare(context, webLink)
    }
    return true
}

private fun openWebShare(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

private fun KakaoShareTarget.bestLink(): String? =
    shareLink?.takeIf { it.isNotBlank() }
        ?: deepLink?.takeIf { it.isNotBlank() }

private fun giftLinkTemplate(webLink: String): TextTemplate =
    TextTemplate(
        text = ShareMessage,
        link = Link(webUrl = webLink, mobileWebUrl = webLink),
    )

private fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private const val ShareMessage = "카페민수 기프티콘이 도착했어요. 아래 링크에서 확인해 주세요."
private const val FormWeight = 1f
private const val AmountOptionWeight = 1f
private const val ChannelCardWeight = 1f
private const val BorderWidthDivider = 4
