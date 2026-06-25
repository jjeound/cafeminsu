package com.ssafy.cafeminsu.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.cafeminsu.core.designsystem.theme.CafeMinsuTheme

@Composable
fun SignInRoute(
    onOwnerLoginClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SignInViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        val errorState = uiState as? SignInUiState.Error
        if (errorState != null) {
            snackbarHostState.showSnackbar(errorState.message)
            viewModel.dismissError()
        }
    }

    SignInScreen(
        isSigningIn = uiState is SignInUiState.SigningIn,
        onKakaoLoginClick = viewModel::signInWithKakao,
        onOwnerLoginClick = onOwnerLoginClick,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
private fun SignInScreen(
    isSigningIn: Boolean,
    onKakaoLoginClick: () -> Unit,
    onOwnerLoginClick: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val colors = CafeMinsuTheme.colors
    val spacing = CafeMinsuTheme.spacing

    Scaffold(
        modifier = modifier,
        containerColor = colors.canvas,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { snackbarData ->
                Snackbar(
                    snackbarData = snackbarData,
                    containerColor = colors.surfaceDark,
                    contentColor = colors.onDark,
                    shape = CafeMinsuTheme.shapes.radiusMd,
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
            Spacer(modifier = Modifier.weight(1.4f))

            LoginBrand()

            Spacer(modifier = Modifier.weight(1.2f))

            KakaoLoginButton(
                enabled = !isSigningIn,
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
    val colors = CafeMinsuTheme.colors
    val spacing = CafeMinsuTheme.spacing

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.space4),
    ) {
        Text(
            text = "☕",
            style = CafeMinsuTheme.typography.display,
            color = colors.primary,
        )

        Text(
            text = "카페민수",
            style = CafeMinsuTheme.typography.display,
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
    val colors = CafeMinsuTheme.colors
    val spacing = CafeMinsuTheme.spacing

    Surface(
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
        shape = CafeMinsuTheme.shapes.radiusLg,
        color = if (enabled) colors.kakaoYellow else colors.hairline,
        contentColor = colors.ink,
    ) {
        Row(
            modifier = Modifier
                .height(52.dp)
                .padding(horizontal = spacing.space5),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalContentColor provides colors.ink) {
//                Icon(
//                    painter = painterResource(id = R.drawable.ic_kakao_talk),
//                    contentDescription = null,
//                    modifier = Modifier.size(22.dp),
//                    tint = colors.ink,
//                )

                Spacer(modifier = Modifier.size(spacing.space2))

                Text(
                    text = "카카오 로그인",
                    style = CafeMinsuTheme.typography.body.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = colors.ink,
                )
            }
        }
    }
}

@Composable
private fun OwnerLoginLink(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeMinsuTheme.colors

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
            style = CafeMinsuTheme.typography.body,
            color = colors.muted,
        )

        Text(
            text = "  →",
            style = CafeMinsuTheme.typography.body,
            color = colors.primary,
        )
    }
}