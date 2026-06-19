package com.cafeminsu.ui.feature.owner.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cafeminsu.domain.auth.OwnerAuthProvider
import com.cafeminsu.ui.components.CafeButton
import com.cafeminsu.ui.components.CafeTextField
import com.cafeminsu.ui.components.CafeTopBar
import com.cafeminsu.ui.theme.CafeTheme

@Composable
fun OwnerLoginRoute(
    ownerAuthProvider: OwnerAuthProvider,
    onBackClick: () -> Unit,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OwnerLoginViewModel = viewModel(
        factory = OwnerLoginViewModelFactory(ownerAuthProvider),
    ),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                OwnerLoginEvent.NavigateOwnerHome -> onLoginSuccess()
                is OwnerLoginEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    OwnerLoginScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onLoginClick = viewModel::login,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
fun OwnerLoginScreen(
    uiState: OwnerLoginUiState,
    onBackClick: () -> Unit,
    onLoginClick: (loginId: String, password: String) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing
    var loginId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier,
        containerColor = colors.canvas,
        topBar = {
            CafeTopBar(
                title = "점주 로그인",
                navigationIcon = {
                    Text(
                        text = "←",
                        style = CafeTheme.typography.h2,
                        color = colors.ink,
                    )
                },
                onNavigationClick = onBackClick,
            )
        },
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
            Spacer(modifier = Modifier.height(spacing.space14))
            Text(
                text = "*",
                style = CafeTheme.typography.display,
                color = colors.primary,
            )
            Spacer(modifier = Modifier.height(spacing.space4))
            Text(
                text = "매장 관리자 로그인",
                style = CafeTheme.typography.h1,
                color = colors.ink,
            )
            Spacer(modifier = Modifier.height(spacing.space1))
            Text(
                text = "카페민수 매장 계정으로 로그인하세요.",
                style = CafeTheme.typography.body,
                color = colors.muted,
            )
            Spacer(modifier = Modifier.height(spacing.space6))
            OwnerLoginFieldGroup(
                label = "아이디",
                value = loginId,
                onValueChange = { loginId = it },
                placeholder = "아이디를 입력하세요",
                modifier = Modifier.testTag(OwnerLoginIdTag),
            )
            Spacer(modifier = Modifier.height(spacing.space4))
            OwnerLoginFieldGroup(
                label = "비밀번호",
                value = password,
                onValueChange = { password = it },
                placeholder = "비밀번호를 입력하세요",
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.testTag(OwnerLoginPasswordTag),
            )
            Spacer(modifier = Modifier.height(spacing.space8))
            CafeButton(
                text = "로그인",
                onClick = { onLoginClick(loginId, password) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
            )
        }
    }
}

@Composable
private fun OwnerLoginFieldGroup(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.space2),
    ) {
        Text(
            text = label,
            style = CafeTheme.typography.caption,
            color = colors.ink,
        )
        CafeTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            modifier = modifier,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
        )
    }
}

private const val OwnerLoginIdTag = "owner-login-id"
private const val OwnerLoginPasswordTag = "owner-login-password"

private class OwnerLoginViewModelFactory(
    private val ownerAuthProvider: OwnerAuthProvider,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OwnerLoginViewModel::class.java)) {
            return OwnerLoginViewModel(ownerAuthProvider) as T
        }
        error("Unknown ViewModel class: ${modelClass.name}")
    }
}
