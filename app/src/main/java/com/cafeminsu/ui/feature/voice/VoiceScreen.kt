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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafeminsu.domain.voice.ParsedOrderItem
import com.cafeminsu.ui.components.CafeButton
import com.cafeminsu.ui.components.CafeButtonVariant
import com.cafeminsu.ui.components.CafeCard
import com.cafeminsu.ui.components.CafeCardType
import com.cafeminsu.ui.components.ErrorView
import com.cafeminsu.ui.theme.CafeTheme

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
    Surface(
        modifier = modifier.fillMaxSize(),
        color = CafeTheme.colors.canvas,
        contentColor = CafeTheme.colors.body,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = CafeTheme.spacing.space5,
                    top = CafeTheme.spacing.space6,
                    end = CafeTheme.spacing.space5,
                    bottom = CafeTheme.spacing.space6,
                ),
            verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space6),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            VoiceHeader()

            VoicePulse(isListening = state is VoiceUiState.Listening)

            VoiceStateBody(
                state = state,
                onRequestPermission = onRequestPermission,
                onConfirm = onConfirm,
                onRetry = onRetry,
                onNavigateToMenu = onNavigateToMenu,
                onOpenSettings = onOpenSettings,
            )

            TranscriptPanel(state = state)
        }
    }
}

@Composable
private fun VoiceHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space2),
    ) {
        Text(
            text = "음성 주문",
            style = CafeTheme.typography.h1,
            color = CafeTheme.colors.ink,
        )
        Text(
            text = "말한 메뉴를 확인하고 장바구니에 담아요",
            style = CafeTheme.typography.body,
            color = CafeTheme.colors.body,
        )
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
    val pulseSize = spacing.space18 * PulseSizeMultiplier + spacing.space8
    val micSize = spacing.space18 + spacing.space8

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(spacing.space18 * PulseHeightMultiplier + spacing.space6),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(pulseSize)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(colors.primary, colors.accentSoft),
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .size(micSize)
                .clip(CircleShape)
                .background(colors.primary),
            contentAlignment = Alignment.Center,
        ) {
            VoiceMicIcon(modifier = Modifier.size(spacing.space8))
        }
    }
}

@Composable
private fun VoiceMicIcon(modifier: Modifier = Modifier) {
    val color = CafeTheme.colors.onPrimary

    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension / IconStrokeDivider
        val centerX = size.width / CenterDivider
        val micWidth = size.width * MicWidthFraction
        val micHeight = size.height * MicHeightFraction
        val micTop = size.height * MicTopFraction
        val micLeft = centerX - micWidth / CenterDivider
        val micBottom = micTop + micHeight

        drawRoundRect(
            color = color,
            topLeft = Offset(micLeft, micTop),
            size = Size(micWidth, micHeight),
            cornerRadius = CornerRadius(micWidth / CenterDivider, micWidth / CenterDivider),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
        drawArc(
            color = color,
            startAngle = MicArcStartAngle,
            sweepAngle = MicArcSweepAngle,
            useCenter = false,
            topLeft = Offset(
                x = centerX - size.width * MicArcWidthFraction / CenterDivider,
                y = size.height * MicArcTopFraction,
            ),
            size = Size(
                width = size.width * MicArcWidthFraction,
                height = size.height * MicArcHeightFraction,
            ),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
        drawLine(
            color = color,
            start = Offset(centerX, micBottom),
            end = Offset(centerX, size.height * MicStemBottomFraction),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * MicBaseStartFraction, size.height * MicBaseYFraction),
            end = Offset(size.width * MicBaseEndFraction, size.height * MicBaseYFraction),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun VoiceStateBody(
    state: VoiceUiState,
    onRequestPermission: () -> Unit,
    onConfirm: () -> Unit,
    onRetry: () -> Unit,
    onNavigateToMenu: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    when (state) {
        VoiceUiState.PermissionRequired -> PermissionRequiredContent(
            onRequestPermission = onRequestPermission,
            onNavigateToMenu = onNavigateToMenu,
            onOpenSettings = onOpenSettings,
        )

        VoiceUiState.Idle -> IdleContent(onRetry = onRetry)
        is VoiceUiState.Listening -> ListeningContent()
        is VoiceUiState.Parsed -> ParsedContent(
            state = state,
            onConfirm = onConfirm,
            onRetry = onRetry,
        )

        is VoiceUiState.AddedToCart -> AddedToCartContent()
        is VoiceUiState.Error -> ErrorView(
            message = state.message,
            retryable = true,
            onRetry = onRetry,
        )
    }
}

@Composable
private fun PermissionRequiredContent(
    onRequestPermission: () -> Unit,
    onNavigateToMenu: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space3),
    ) {
        CafeCard(
            modifier = Modifier.fillMaxWidth(),
            type = CafeCardType.Info,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space2)) {
                Text(
                    text = "마이크 권한이 필요해요",
                    style = CafeTheme.typography.h3,
                    color = CafeTheme.colors.ink,
                )
                Text(
                    text = "권한을 허용하지 않아도 메뉴에서 직접 주문할 수 있어요",
                    style = CafeTheme.typography.body,
                    color = CafeTheme.colors.body,
                )
            }
        }
        CafeButton(
            text = "마이크 권한 허용",
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space3),
        ) {
            CafeButton(
                text = "메뉴로 주문하기",
                onClick = onNavigateToMenu,
                modifier = Modifier.weight(ActionButtonWeight),
                variant = CafeButtonVariant.Secondary,
            )
            CafeButton(
                text = "설정 열기",
                onClick = onOpenSettings,
                modifier = Modifier.weight(ActionButtonWeight),
                variant = CafeButtonVariant.Secondary,
            )
        }
    }
}

@Composable
private fun IdleContent(onRetry: () -> Unit) {
    CafeButton(
        text = "음성 주문 시작",
        onClick = onRetry,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ListeningContent() {
    CafeCard(
        modifier = Modifier.fillMaxWidth(),
        type = CafeCardType.Default,
    ) {
        Text(
            text = "듣고 있어요",
            style = CafeTheme.typography.bodyL,
            color = CafeTheme.colors.ink,
        )
    }
}

@Composable
private fun ParsedContent(
    state: VoiceUiState.Parsed,
    onConfirm: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space4),
    ) {
        if (state.items.isNotEmpty()) {
            ParsedItems(items = state.items)
            CafeButton(
                text = "장바구니에 담기",
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (state.unmatched.isNotEmpty()) {
            UnmatchedCard(unmatched = state.unmatched)
        }

        CafeButton(
            text = "다시 말하기",
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
            variant = CafeButtonVariant.Secondary,
        )
    }
}

@Composable
private fun ParsedItems(items: List<ParsedOrderItem>) {
    CafeCard(
        modifier = Modifier.fillMaxWidth(),
        type = CafeCardType.Default,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space4)) {
            Text(
                text = "확인된 주문",
                style = CafeTheme.typography.h3,
                color = CafeTheme.colors.ink,
            )
            items.forEach { item ->
                ParsedItemRow(item = item)
            }
        }
    }
}

@Composable
private fun ParsedItemRow(item: ParsedOrderItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space3),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(ParsedItemNameWeight),
            verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space1),
        ) {
            Text(
                text = item.name,
                style = CafeTheme.typography.bodyL,
                color = CafeTheme.colors.ink,
                maxLines = ParsedItemTextMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.selectedOptions.isNotEmpty()) {
                Text(
                    text = item.selectedOptions.joinToString { option -> option.name },
                    style = CafeTheme.typography.caption,
                    color = CafeTheme.colors.muted,
                    maxLines = ParsedItemTextMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = "${item.quantity}개",
            style = CafeTheme.typography.bodyL,
            color = CafeTheme.colors.primary,
        )
    }
}

@Composable
private fun UnmatchedCard(unmatched: List<String>) {
    CafeCard(
        modifier = Modifier.fillMaxWidth(),
        type = CafeCardType.Info,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space2)) {
            Text(
                text = "확인하지 못한 말",
                style = CafeTheme.typography.h3,
                color = CafeTheme.colors.ink,
            )
            Text(
                text = unmatched.joinToString(),
                style = CafeTheme.typography.body,
                color = CafeTheme.colors.body,
            )
        }
    }
}

@Composable
private fun AddedToCartContent() {
    CafeCard(
        modifier = Modifier.fillMaxWidth(),
        type = CafeCardType.Default,
    ) {
        Text(
            text = "장바구니에 담았어요",
            style = CafeTheme.typography.bodyL,
            color = CafeTheme.colors.ink,
        )
    }
}

@Composable
private fun TranscriptPanel(state: VoiceUiState) {
    CafeCard(
        modifier = Modifier.fillMaxWidth(),
        type = CafeCardType.Info,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space2)) {
            Text(
                text = "들은 내용",
                style = CafeTheme.typography.h3,
                color = CafeTheme.colors.ink,
            )
            Text(
                text = state.transcript.ifBlank { state.emptyTranscriptText() },
                style = CafeTheme.typography.body,
                color = if (state is VoiceUiState.Listening) {
                    CafeTheme.colors.muted
                } else {
                    CafeTheme.colors.ink
                },
            )
        }
    }
}

private fun VoiceUiState.emptyTranscriptText(): String =
    when (this) {
        VoiceUiState.PermissionRequired -> "마이크 권한을 허용하면 주문을 들을 수 있어요"
        VoiceUiState.Idle -> "주문을 말해 주세요"
        is VoiceUiState.Listening -> "듣고 있어요"
        is VoiceUiState.Parsed -> "확인된 발화가 없어요"
        is VoiceUiState.AddedToCart -> "장바구니로 이동합니다"
        is VoiceUiState.Error -> "다시 시도해 주세요"
    }

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

private const val PulseSizeMultiplier = 2
private const val PulseHeightMultiplier = 3
private const val PulseDurationMillis = 1_200
private const val PulseScaleMin = 1f
private const val PulseScaleMax = 1.06f
private const val PulseAlphaMin = 0.42f
private const val PulseAlphaMax = 0.68f
private const val PulseIdleAlpha = 0.5f
private const val IconStrokeDivider = 12f
private const val CenterDivider = 2f
private const val MicWidthFraction = 0.36f
private const val MicHeightFraction = 0.48f
private const val MicTopFraction = 0.1f
private const val MicArcStartAngle = 0f
private const val MicArcSweepAngle = 180f
private const val MicArcWidthFraction = 0.68f
private const val MicArcHeightFraction = 0.52f
private const val MicArcTopFraction = 0.3f
private const val MicStemBottomFraction = 0.78f
private const val MicBaseStartFraction = 0.34f
private const val MicBaseEndFraction = 0.66f
private const val MicBaseYFraction = 0.86f
private const val ActionButtonWeight = 1f
private const val ParsedItemNameWeight = 1f
private const val ParsedItemTextMaxLines = 2
private const val SettingsPackageScheme = "package"
