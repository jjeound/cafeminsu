package com.cafeminsu.ui.feature.gift.claim

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafeminsu.R
import com.cafeminsu.ui.components.CafeButton
import com.cafeminsu.ui.components.CafeButtonVariant
import com.cafeminsu.ui.components.CafeTextField
import com.cafeminsu.ui.components.CafeTopBar
import com.cafeminsu.ui.theme.CafeTheme

@Composable
fun GiftClaimRoute(
    onBackClick: () -> Unit,
    onClaimed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GiftClaimViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is GiftClaimEvent.Claimed -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                    onClaimed()
                }
            }
        }
    }

    GiftClaimScreen(
        state = state,
        onBackClick = onBackClick,
        onCodeChanged = viewModel::onCodeChanged,
        onClaimClick = viewModel::claim,
        modifier = modifier,
    )
}

@Composable
fun GiftClaimScreen(
    state: GiftClaimUiState,
    onBackClick: () -> Unit,
    onCodeChanged: (String) -> Unit,
    onClaimClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Surface(
        modifier = modifier.fillMaxSize(),
        color = colors.canvas,
        contentColor = colors.body,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CafeTopBar(
                title = "선물 등록",
                navigationIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_left),
                        contentDescription = null,
                        tint = colors.ink,
                    )
                },
                onNavigationClick = onBackClick,
            )

            Column(
                modifier = Modifier
                    .weight(ContentWeight)
                    .fillMaxWidth()
                    .padding(
                        start = spacing.space5,
                        top = spacing.space6,
                        end = spacing.space5,
                        bottom = spacing.space5,
                    ),
                verticalArrangement = Arrangement.spacedBy(spacing.space3),
            ) {
                Text(
                    text = "선물 코드를 등록하세요",
                    style = CafeTheme.typography.h1,
                    color = colors.ink,
                )
                Text(
                    text = "받은 선물 코드를 입력하면 내 기프티콘에 담겨요.",
                    style = CafeTheme.typography.body,
                    color = colors.muted,
                )

                Text(
                    modifier = Modifier.padding(top = spacing.space3),
                    text = "등록 코드",
                    style = CafeTheme.typography.body,
                    color = colors.ink,
                )
                CafeTextField(
                    value = state.code,
                    onValueChange = onCodeChanged,
                    placeholder = "예) GFT-XXXX-XXXX",
                )

                state.errorMessage?.let { message ->
                    Text(
                        text = message,
                        style = CafeTheme.typography.caption,
                        color = colors.error,
                    )
                }
            }

            Box(
                modifier = Modifier.padding(
                    start = spacing.space5,
                    end = spacing.space5,
                    bottom = spacing.space5,
                ),
            ) {
                CafeButton(
                    text = "등록하기",
                    onClick = onClaimClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.canSubmit,
                    variant = CafeButtonVariant.Primary,
                )
            }
        }
    }
}

private const val ContentWeight = 1f
