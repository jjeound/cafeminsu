package com.cafeminsu.ui.feature.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafeminsu.R
import com.cafeminsu.domain.repository.SessionRepository
import com.cafeminsu.ui.components.CafeButton
import com.cafeminsu.ui.theme.CafeTheme

@Composable
fun SignupRoute(
    sessionRepository: SessionRepository,
    onSignupComplete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignupViewModel = viewModel(
        factory = SignupViewModelFactory(sessionRepository),
    ),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                SignupEvent.NavigateHome -> onSignupComplete()
                is SignupEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    SignupScreen(
        uiState = uiState,
        onNicknameChange = viewModel::onNicknameChange,
        onClearClick = viewModel::onClearClick,
        onSubmit = viewModel::onSubmit,
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
fun SignupScreen(
    uiState: SignupUiState,
    onNicknameChange: (String) -> Unit,
    onClearClick: () -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
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
        ) {
            Spacer(modifier = Modifier.height(spacing.space2))
            Icon(
                painter = painterResource(R.drawable.ic_chevron_left),
                contentDescription = "뒤로",
                tint = colors.ink,
                modifier = Modifier
                    .size(spacing.space6)
                    .clickable(role = Role.Button, onClick = onBack),
            )
            Spacer(modifier = Modifier.height(spacing.space6))
            Text(
                text = "닉네임을 설정해주세요",
                style = CafeTheme.typography.h1,
                color = colors.ink,
            )
            Spacer(modifier = Modifier.height(spacing.space2))
            Text(
                text = "카페민수에서 사용할 이름이에요",
                style = CafeTheme.typography.body,
                color = colors.muted,
            )
            Spacer(modifier = Modifier.height(spacing.space8))
            Text(
                text = "닉네임",
                style = CafeTheme.typography.body.copy(fontWeight = FontWeight.Medium),
                color = colors.ink,
            )
            Spacer(modifier = Modifier.height(spacing.space2))
            NicknameField(
                value = uiState.nickname,
                onValueChange = onNicknameChange,
                onClear = onClearClick,
                isError = uiState.errorMessage != null,
            )
            Spacer(modifier = Modifier.height(spacing.space2))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = uiState.errorMessage ?: "한글·영문·숫자 2~10자",
                    style = CafeTheme.typography.caption,
                    color = if (uiState.errorMessage != null) colors.error else colors.muted,
                )
                Text(
                    text = "${uiState.charCount}/${SignupUiState.MaxNicknameLength}",
                    style = CafeTheme.typography.caption,
                    color = colors.muted,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            CafeButton(
                text = "시작하기",
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.canSubmit,
            )
            Spacer(modifier = Modifier.height(spacing.space8))
        }
    }
}

@Composable
private fun NicknameField(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing
    var focused by remember { mutableStateOf(false) }
    val borderColor = when {
        isError -> colors.error
        focused -> colors.primary
        else -> Color.Transparent
    }
    val minHeight = spacing.space10 + spacing.space3

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .background(color = colors.surfaceCard, shape = CafeTheme.shapes.radiusMd)
            .border(width = FieldBorderWidth, color = borderColor, shape = CafeTheme.shapes.radiusMd)
            .onFocusChanged { focused = it.isFocused },
        singleLine = true,
        textStyle = CafeTheme.typography.bodyL.copy(color = colors.ink),
        cursorBrush = SolidColor(colors.primary),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.space4, vertical = spacing.space3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = "닉네임을 입력해주세요",
                            style = CafeTheme.typography.bodyL,
                            color = colors.muted,
                        )
                    }
                    innerTextField()
                }
                if (value.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(spacing.space2))
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = "지우기",
                        tint = colors.muted,
                        modifier = Modifier
                            .size(spacing.space5)
                            .clickable(role = Role.Button, onClick = onClear),
                    )
                }
            }
        },
    )
}

private val FieldBorderWidth = 1.5.dp

private class SignupViewModelFactory(
    private val sessionRepository: SessionRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SignupViewModel::class.java)) {
            return SignupViewModel(sessionRepository) as T
        }
        error("Unknown ViewModel class: ${modelClass.name}")
    }
}
