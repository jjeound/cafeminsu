package com.cafeminsu.ui.feature.login

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafeminsu.domain.repository.SessionRepository
import com.cafeminsu.ui.theme.CafeTheme

@Composable
fun LoginRoute(
    sessionRepository: SessionRepository,
    onLoginSuccess: () -> Unit,
    onOwnerLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel(
        factory = LoginViewModelFactory(sessionRepository),
    ),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                LoginEvent.NavigateHome -> onLoginSuccess()
                is LoginEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    LoginScreen(
        uiState = uiState,
        onKakaoLoginClick = viewModel::onKakaoLoginClick,
        onOwnerLoginClick = onOwnerLoginClick,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onKakaoLoginClick: () -> Unit,
    onOwnerLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Scaffold(
        modifier = modifier,
        containerColor = colors.canvas,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { snackbarData ->
                Snackbar(
                    snackbarData = snackbarData,
                    containerColor = colors.surfaceDark,
                    contentColor = colors.onDark,
                    shape = CafeTheme.shapes.radiusMd,
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.canvas)
                .padding(innerPadding)
                .padding(horizontal = spacing.space5),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(LogoTopWeight))
            LoginBrand()
            Spacer(modifier = Modifier.weight(LogoBottomWeight))
            KakaoLoginButton(
                enabled = !uiState.isLoading,
                onClick = onKakaoLoginClick,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(spacing.space14 + spacing.space6))
            OwnerLoginLink(onClick = onOwnerLoginClick)
            Spacer(modifier = Modifier.height(spacing.space18))
        }
    }
}

@Composable
private fun LoginBrand(
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.space4),
    ) {
        Text(
            text = "✱",
            style = CafeTheme.typography.display,
            color = colors.primary,
        )
        Text(
            text = "카페민수",
            style = CafeTheme.typography.display,
            color = colors.ink,
        )
    }
}

@Composable
private fun KakaoLoginButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Surface(
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
        shape = CafeTheme.shapes.radiusLg,
        color = if (enabled) colors.kakaoYellow else colors.hairline,
        contentColor = colors.ink,
    ) {
        Row(
            modifier = Modifier
                .height(KakaoButtonHeight)
                .padding(horizontal = spacing.space5),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalContentColor provides colors.ink) {
                KakaoTalkBubbleIcon(
                    modifier = Modifier.size(KakaoIconSize),
                    color = colors.ink,
                )
                Spacer(modifier = Modifier.size(spacing.space2))
                Text(
                    text = "카카오 로그인",
                    style = CafeTheme.typography.body.copy(fontWeight = FontWeight.Medium),
                    color = colors.ink,
                )
            }
        }
    }
}

@Composable
private fun KakaoTalkBubbleIcon(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        drawSpeechBubble(color = color)
    }
}

private fun DrawScope.drawSpeechBubble(color: Color) {
    val bubbleWidth = size.width * BubbleWidthRatio
    val bubbleHeight = size.height * BubbleHeightRatio
    val bubbleLeft = size.width * BubbleLeftRatio
    val bubbleTop = size.height * BubbleTopRatio
    val tailPath = Path().apply {
        moveTo(size.width * TailStartXRatio, size.height * TailStartYRatio)
        lineTo(size.width * TailTipXRatio, size.height * TailTipYRatio)
        lineTo(size.width * TailEndXRatio, size.height * TailEndYRatio)
        close()
    }

    drawRoundRect(
        color = color,
        topLeft = Offset(bubbleLeft, bubbleTop),
        size = Size(bubbleWidth, bubbleHeight),
        cornerRadius = CornerRadius(size.width * BubbleCornerRatio),
    )
    drawPath(path = tailPath, color = color)
}

@Composable
private fun OwnerLoginLink(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    Row(
        modifier = modifier
            .clickable(
                role = Role.Button,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "점주 로그인",
            style = CafeTheme.typography.body,
            color = colors.muted,
        )
        Text(
            text = "  →",
            style = CafeTheme.typography.body,
            color = colors.primary,
        )
    }
}

private val KakaoButtonHeight = 52.dp
private val KakaoIconSize = 22.dp
private const val LogoTopWeight = 1.4f
private const val LogoBottomWeight = 1.2f
private const val BubbleWidthRatio = 0.76f
private const val BubbleHeightRatio = 0.58f
private const val BubbleLeftRatio = 0.1f
private const val BubbleTopRatio = 0.16f
private const val BubbleCornerRatio = 0.22f
private const val TailStartXRatio = 0.4f
private const val TailStartYRatio = 0.72f
private const val TailTipXRatio = 0.28f
private const val TailTipYRatio = 0.92f
private const val TailEndXRatio = 0.55f
private const val TailEndYRatio = 0.72f

private class LoginViewModelFactory(
    private val sessionRepository: SessionRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(sessionRepository) as T
        }
        error("Unknown ViewModel class: ${modelClass.name}")
    }
}
