package com.cafeminsu.ui.feature.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafeminsu.R
import com.cafeminsu.domain.model.SelectedOption
import com.cafeminsu.domain.voice.ParsedOrderItem
import com.cafeminsu.ui.components.CafeButton
import com.cafeminsu.ui.theme.CafeTheme
import java.text.NumberFormat
import java.util.Locale

@Composable
fun VoiceRoute(
    onNavigateToCart: () -> Unit,
    onNavigateToMenu: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VoiceViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onPermissionResult(granted)
    }

    LaunchedEffect(Unit) {
        viewModel.onPermissionResult(context.hasRecordAudioPermission())
    }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                VoiceEvent.NavigateToCart -> onNavigateToCart()
            }
        }
    }

    VoiceScreen(
        state = state,
        onRequestPermission = {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        },
        onConfirm = viewModel::onConfirm,
        onRetry = {
            if (context.hasRecordAudioPermission()) {
                viewModel.onPermissionResult(granted = true)
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        onNavigateToMenu = onNavigateToMenu,
        onOpenSettings = { context.openAppSettings() },
        modifier = modifier,
    )
}

@Composable
fun VoiceScreen(
    state: VoiceUiState,
    onRequestPermission: () -> Unit,
    onConfirm: () -> Unit,
    onRetry: () -> Unit,
    onNavigateToMenu: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors

    Surface(
        modifier = modifier.fillMaxSize(),
        color = colors.surfaceDark,
        contentColor = colors.onDark,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            VoiceTopBar(
                onBackClick = onNavigateToMenu,
                onCloseClick = onNavigateToMenu,
            )

            Column(
                modifier = Modifier
                    .weight(ContentWeight)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = CafeTheme.spacing.space5,
                        top = CafeTheme.spacing.space4,
                        end = CafeTheme.spacing.space5,
                        bottom = CafeTheme.spacing.space5,
                    ),
                verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space5),
            ) {
                VoiceHeadline()
                VoicePulseSection(state = state)
                TranscriptPanel(state = state)

                if (state is VoiceUiState.PermissionRequired) {
                    PermissionRationalePanel()
                }

                AiResultPanel(state = state)
            }

            VoiceBottomActions(
                state = state,
                onRequestPermission = onRequestPermission,
                onRetry = onRetry,
                onConfirm = onConfirm,
                onNavigateToMenu = onNavigateToMenu,
                onOpenSettings = onOpenSettings,
            )
        }
    }
}

@Composable
private fun VoiceTopBar(
    onBackClick: () -> Unit,
    onCloseClick: () -> Unit,
) {
    val spacing = CafeTheme.spacing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(spacing.space14)
            .padding(horizontal = spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VoiceTopBarIcon(
            iconRes = R.drawable.ic_chevron_left,
            contentDescription = "뒤로",
            onClick = onBackClick,
        )
        Text(
            text = "음성으로 주문",
            modifier = Modifier.weight(ContentWeight),
            style = CafeTheme.typography.bodyL,
            color = CafeTheme.colors.onDark,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        VoiceTopBarIcon(
            iconRes = R.drawable.ic_close,
            contentDescription = "닫기",
            onClick = onCloseClick,
        )
    }
}

@Composable
private fun VoiceTopBarIcon(
    @androidx.annotation.DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val spacing = CafeTheme.spacing

    Box(
        modifier = Modifier
            .size(spacing.space10 + spacing.space2)
            .semantics { this.contentDescription = contentDescription }
            .clickable(
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = CafeTheme.colors.onDark,
            modifier = Modifier.size(spacing.space6),
        )
    }
}

@Composable
private fun VoiceHeadline() {
    Text(
        text = "원하시는 메뉴를\n말씀해주세요",
        modifier = Modifier.fillMaxWidth(),
        style = CafeTheme.typography.display,
        color = CafeTheme.colors.onDark,
    )
}

@Composable
private fun VoicePulseSection(state: VoiceUiState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space3),
    ) {
        VoicePulse(isListening = state is VoiceUiState.Listening)
        ListeningStatusLabel(text = state.listeningStatusText())
    }
}

@Composable
private fun VoicePulse(isListening: Boolean) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing
    val transition = rememberInfiniteTransition(label = "voice-pulse")
    val animatedScale by transition.animateFloat(
        initialValue = PulseScaleMin,
        targetValue = PulseScaleMax,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = PulseDurationMillis),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "voice-pulse-scale",
    )
    val animatedAlpha by transition.animateFloat(
        initialValue = PulseAlphaMin,
        targetValue = PulseAlphaMax,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = PulseDurationMillis),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "voice-pulse-alpha",
    )
    val scale = if (isListening) animatedScale else PulseScaleMin
    val alpha = if (isListening) animatedAlpha else PulseIdleAlpha
    val outerSize = spacing.space18 * PulseOuterSizeMultiplier + spacing.space10
    val innerSize = spacing.space18 + spacing.space14

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(outerSize),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .size(outerSize)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                },
        ) {
            drawCircle(
                color = colors.primary.copy(alpha = PulseRingColorAlpha),
                style = Stroke(
                    width = spacing.space1.toPx() / PulseRingStrokeDivider,
                    cap = StrokeCap.Round,
                ),
            )
        }
        Box(
            modifier = Modifier
                .size(innerSize)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            colors.primary,
                            colors.accentSoft,
                        ),
                    ),
                ),
        )
    }
}

@Composable
private fun ListeningStatusLabel(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "●",
            style = CafeTheme.typography.caption,
            color = CafeTheme.colors.primary,
        )
        Text(
            text = text,
            style = CafeTheme.typography.caption,
            color = CafeTheme.colors.muted,
        )
    }
}

@Composable
private fun TranscriptPanel(state: VoiceUiState) {
    DarkPanel {
        Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space2)) {
            Text(
                text = "인식된 음성",
                style = CafeTheme.typography.caption,
                color = CafeTheme.colors.muted,
            )
            Text(
                text = "“${state.transcriptDisplayText()}”",
                style = CafeTheme.typography.bodyL,
                color = if (state is VoiceUiState.Listening || state.transcript.isBlank()) {
                    CafeTheme.colors.muted
                } else {
                    CafeTheme.colors.onDark
                },
            )
        }
    }
}

@Composable
private fun PermissionRationalePanel() {
    DarkPanel {
        Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space2)) {
            Text(
                text = "마이크 권한이 필요해요",
                style = CafeTheme.typography.h3,
                color = CafeTheme.colors.onDark,
            )
            Text(
                text = "음성 주문을 위해 마이크 권한을 사용해요. 권한을 허용하지 않아도 메뉴에서 직접 주문할 수 있어요.",
                style = CafeTheme.typography.body,
                color = CafeTheme.colors.muted,
            )
        }
    }
}

@Composable
private fun AiResultPanel(state: VoiceUiState) {
    DarkPanel {
        Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space4)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "AI 인식 결과",
                    style = CafeTheme.typography.caption,
                    color = CafeTheme.colors.muted,
                )
                if (state is VoiceUiState.Parsed) {
                    ConfidenceTag(confidencePercent = state.confidencePercent)
                }
            }

            when (state) {
                is VoiceUiState.Parsed -> ParsedResultContent(state = state)
                else -> ResultStatusMessage(text = state.resultStatusText())
            }
        }
    }
}

@Composable
private fun ConfidenceTag(confidencePercent: Int) {
    Surface(
        shape = CafeTheme.shapes.radiusPill,
        color = CafeTheme.colors.accentSoft,
        contentColor = CafeTheme.colors.primary,
    ) {
        Text(
            text = "신뢰도 $confidencePercent%",
            modifier = Modifier.padding(
                horizontal = CafeTheme.spacing.space3,
                vertical = CafeTheme.spacing.space1,
            ),
            style = CafeTheme.typography.meta,
            color = CafeTheme.colors.primary,
        )
    }
}

@Composable
private fun ParsedResultContent(state: VoiceUiState.Parsed) {
    Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space3)) {
        if (state.items.isEmpty()) {
            ResultStatusMessage(text = "확인된 메뉴가 없어요. 다시 말씀해주세요.")
        } else {
            state.items.forEach { item ->
                ParsedItemRow(item = item)
            }
        }

        if (state.unmatched.isNotEmpty()) {
            Text(
                text = "확인 필요: ${state.unmatched.joinToString()}",
                style = CafeTheme.typography.caption,
                color = CafeTheme.colors.warning,
            )
        }

        DarkDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "예상 금액",
                style = CafeTheme.typography.caption,
                color = CafeTheme.colors.muted,
            )
            Text(
                text = formatWon(state.estimatedTotalAmount),
                style = CafeTheme.typography.h1,
                color = CafeTheme.colors.onDark,
            )
        }
    }
}

@Composable
private fun ParsedItemRow(item: ParsedOrderItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = item.resultLabel(),
            modifier = Modifier.weight(ContentWeight),
            style = CafeTheme.typography.body,
            color = CafeTheme.colors.onDark,
            maxLines = ParsedItemTextMaxLines,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "✕ ${item.quantity}",
            style = CafeTheme.typography.body,
            color = CafeTheme.colors.muted,
        )
    }
}

@Composable
private fun ResultStatusMessage(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        style = CafeTheme.typography.body,
        color = CafeTheme.colors.muted,
    )
}

@Composable
private fun DarkDivider() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(CafeTheme.spacing.space1 / DividerHeightDivider)
            .background(CafeTheme.colors.onDark.copy(alpha = DividerAlpha)),
    )
}

@Composable
private fun VoiceBottomActions(
    state: VoiceUiState,
    onRequestPermission: () -> Unit,
    onRetry: () -> Unit,
    onConfirm: () -> Unit,
    onNavigateToMenu: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val spacing = CafeTheme.spacing

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = spacing.space5,
                top = spacing.space3,
                end = spacing.space5,
                bottom = spacing.space4,
            ),
        verticalArrangement = Arrangement.spacedBy(spacing.space3),
    ) {
        if (state is VoiceUiState.PermissionRequired) {
            CafeButton(
                text = "마이크 권한 허용",
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.space3),
            ) {
                VoiceSecondaryButton(
                    text = "메뉴로 주문하기",
                    onClick = onNavigateToMenu,
                    modifier = Modifier.weight(ActionButtonWeight),
                )
                VoiceSecondaryButton(
                    text = "설정 열기",
                    onClick = onOpenSettings,
                    modifier = Modifier.weight(ActionButtonWeight),
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.space3),
            ) {
                VoiceSecondaryButton(
                    text = "다시 말하기",
                    onClick = onRetry,
                    modifier = Modifier.weight(ActionButtonWeight),
                )
                CafeButton(
                    text = "이대로 주문",
                    onClick = onConfirm,
                    modifier = Modifier.weight(ActionButtonWeight),
                    enabled = state.canConfirmOrder(),
                )
            }
        }
    }
}

@Composable
private fun VoiceSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val containerColor = when {
        !enabled -> colors.onDark.copy(alpha = DarkButtonDisabledAlpha)
        pressed -> colors.onDark.copy(alpha = DarkButtonPressedAlpha)
        else -> colors.onDark.copy(alpha = DarkButtonContainerAlpha)
    }
    val contentColor = if (enabled) colors.onDark else colors.muted

    Surface(
        onClick = onClick,
        modifier = modifier.semantics(mergeDescendants = true) {},
        enabled = enabled,
        shape = CafeTheme.shapes.radiusLg,
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(
            width = spacing.space1 / BorderWidthDivider,
            color = colors.onDark.copy(alpha = DarkButtonBorderAlpha),
        ),
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier
                .height(spacing.space10 + spacing.space3)
                .padding(horizontal = spacing.space5),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = CafeTheme.typography.bodyL,
                color = contentColor,
                maxLines = ButtonTextMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DarkPanel(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CafeTheme.shapes.radiusLg,
        color = CafeTheme.colors.onDark.copy(alpha = DarkPanelAlpha),
        contentColor = CafeTheme.colors.onDark,
        border = BorderStroke(
            width = CafeTheme.spacing.space1 / BorderWidthDivider,
            color = CafeTheme.colors.onDark.copy(alpha = DarkPanelBorderAlpha),
        ),
    ) {
        Box(modifier = Modifier.padding(CafeTheme.spacing.space4)) {
            content()
        }
    }
}

private fun VoiceUiState.canConfirmOrder(): Boolean =
    this is VoiceUiState.Parsed && items.isNotEmpty() && items.none { item -> item.isSoldOut }

private fun VoiceUiState.listeningStatusText(): String =
    when (this) {
        VoiceUiState.PermissionRequired -> "권한 필요"
        VoiceUiState.Idle -> "대기 중"
        is VoiceUiState.Listening -> "듣는 중"
        is VoiceUiState.Interpreting -> "이해하는 중"
        is VoiceUiState.Parsed -> "인식 완료"
        is VoiceUiState.AddedToCart -> "장바구니로 이동"
        is VoiceUiState.Error -> "다시 시도 필요"
    }

private fun VoiceUiState.transcriptDisplayText(): String =
    transcript.ifBlank {
        when (this) {
            VoiceUiState.PermissionRequired -> "마이크 권한을 허용하면 주문을 들을 수 있어요"
            VoiceUiState.Idle -> "주문을 말해 주세요"
            is VoiceUiState.Listening -> "듣고 있어요"
            is VoiceUiState.Interpreting -> "주문을 이해하고 있어요"
            is VoiceUiState.Parsed -> "확인된 발화가 없어요"
            is VoiceUiState.AddedToCart -> "장바구니로 이동합니다"
            is VoiceUiState.Error -> "다시 시도해 주세요"
        }
    }

private fun VoiceUiState.resultStatusText(): String =
    when (this) {
        VoiceUiState.PermissionRequired -> "마이크 권한을 허용하면 인식 결과가 여기에 표시돼요."
        VoiceUiState.Idle -> "메뉴를 말씀하시면 주문 항목과 예상 금액을 확인할게요."
        is VoiceUiState.Listening -> "말씀하신 메뉴를 듣고 있어요."
        is VoiceUiState.Interpreting -> "말씀하신 주문을 이해하고 있어요…"
        is VoiceUiState.AddedToCart -> "주문 항목을 장바구니에 담았어요."
        is VoiceUiState.Error -> message
        is VoiceUiState.Parsed -> ""
    }

private fun ParsedOrderItem.resultLabel(): String {
    val optionLabels = selectedOptions.map { option -> option.voiceLabel() }
    return (listOf(name) + optionLabels).joinToString(" · ")
}

private fun SelectedOption.voiceLabel(): String {
    val normalized = name.lowercase(Locale.KOREA)
    return when (normalized) {
        "ice",
        "iced",
        "아이스",
        -> "ICE"

        "hot",
        "핫",
        "따뜻",
        "따뜻한",
        -> "HOT"

        else -> name
    }
}

private fun formatWon(amount: Int): String =
    "${NumberFormat.getNumberInstance(Locale.KOREA).format(amount)}원"

private fun Context.hasRecordAudioPermission(): Boolean =
    ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED

private fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts(SettingsPackageScheme, packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}

private const val ContentWeight = 1f
private const val ActionButtonWeight = 1f
private const val PulseOuterSizeMultiplier = 2
private const val PulseDurationMillis = 1_200
private const val PulseScaleMin = 1f
private const val PulseScaleMax = 1.04f
private const val PulseAlphaMin = 0.5f
private const val PulseAlphaMax = 0.9f
private const val PulseIdleAlpha = 0.72f
private const val PulseRingColorAlpha = 0.7f
private const val PulseRingStrokeDivider = 2f
private const val DarkPanelAlpha = 0.06f
private const val DarkPanelBorderAlpha = 0.04f
private const val DarkButtonContainerAlpha = 0.05f
private const val DarkButtonPressedAlpha = 0.1f
private const val DarkButtonDisabledAlpha = 0.03f
private const val DarkButtonBorderAlpha = 0.08f
private const val DividerAlpha = 0.1f
private const val BorderWidthDivider = 4
private const val DividerHeightDivider = 4
private const val ParsedItemTextMaxLines = 2
private const val ButtonTextMaxLines = 1
private const val SettingsPackageScheme = "package"
