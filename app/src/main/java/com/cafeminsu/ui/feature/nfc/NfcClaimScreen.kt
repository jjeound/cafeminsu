package com.cafeminsu.ui.feature.nfc

import android.content.Intent
import android.nfc.NfcAdapter
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafeminsu.R
import com.cafeminsu.ui.components.CafeButton
import com.cafeminsu.ui.components.CafeButtonVariant
import com.cafeminsu.ui.components.CafeCard
import com.cafeminsu.ui.components.CafeCardType
import com.cafeminsu.ui.components.LoadingView
import com.cafeminsu.ui.theme.CafeTheme

@Composable
fun NfcClaimRoute(
    onBackClick: () -> Unit,
    onNavigateToGifticons: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NfcClaimViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 발급 어댑터 가용성. 화면 진입 시 1회 판단하면 충분하다(설정 이동 후 복귀는 사용자 재진입).
    val adapter = remember(context) { NfcAdapter.getDefaultAdapter(context) }
    val availability = resolveNfcAvailability(
        hasAdapter = adapter != null,
        enabled = adapter?.isEnabled == true,
    )

    // 성공 결과(다이얼로그 표시용). 낙관 금지 — ViewModel 의 Claimed 이벤트 이후에만 채운다.
    var claimedResult by remember { mutableStateOf<NfcClaimResultUi?>(null) }
    val currentContext by rememberUpdatedState(context)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is NfcClaimEvent.Claimed -> {
                    Toast.makeText(currentContext, ClaimedToast, Toast.LENGTH_SHORT).show()
                    claimedResult = event.coupon
                }
            }
        }
    }

    // 정상 + 결과 다이얼로그 미표시일 때만 reader mode 활성. 화면 이탈/다이얼로그 시 해제(누수 방지).
    NfcReaderEffect(
        enabled = availability == NfcAvailability.Ready && claimedResult == null,
        onRawTagRead = viewModel::onTagRead,
    )

    NfcClaimScreen(
        state = state,
        availability = availability,
        claimedResult = claimedResult,
        onBackClick = onBackClick,
        onOpenNfcSettings = {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_NFC_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        },
        onConfirmClaimed = {
            claimedResult = null
            // 기프티콘 목록으로 이동 → observeGifticons() 재구독으로 발급 쿠폰이 보인다(= 새로고침).
            onNavigateToGifticons()
        },
        modifier = modifier,
    )
}

@Composable
fun NfcClaimScreen(
    state: NfcClaimUiState,
    availability: NfcAvailability,
    claimedResult: NfcClaimResultUi?,
    onBackClick: () -> Unit,
    onOpenNfcSettings: () -> Unit,
    onConfirmClaimed: () -> Unit,
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
            CafeTopBar(onBackClick = onBackClick)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = spacing.space5,
                        top = spacing.space6,
                        end = spacing.space5,
                        bottom = spacing.space5,
                    ),
                verticalArrangement = Arrangement.spacedBy(spacing.space4),
            ) {
                when (availability) {
                    NfcAvailability.Unsupported -> UnsupportedContent()
                    NfcAvailability.Disabled -> DisabledContent(onOpenNfcSettings = onOpenNfcSettings)
                    NfcAvailability.Ready -> ReadyContent(state = state)
                }
            }
        }
    }

    if (claimedResult != null) {
        NfcClaimResultDialog(
            result = claimedResult,
            onConfirm = onConfirmClaimed,
        )
    }
}

@Composable
private fun CafeTopBar(onBackClick: () -> Unit) {
    com.cafeminsu.ui.components.CafeTopBar(
        title = "NFC 쿠폰 받기",
        navigationIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_left),
                contentDescription = null,
                tint = CafeTheme.colors.ink,
            )
        },
        onNavigationClick = onBackClick,
    )
}

@Composable
private fun ReadyContent(state: NfcClaimUiState) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Text(
        text = "NFC 쿠폰을 받아보세요",
        style = CafeTheme.typography.h1,
        color = colors.ink,
    )
    Text(
        text = "폰을 매장 NFC 태그에 대주세요",
        style = CafeTheme.typography.body,
        color = colors.muted,
    )

    CafeCard(
        modifier = Modifier.fillMaxWidth(),
        type = CafeCardType.Info,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
            if (state.claiming) {
                Text(
                    text = "쿠폰을 발급하고 있어요",
                    style = CafeTheme.typography.bodyL,
                    color = colors.ink,
                )
                LoadingView()
            } else {
                Text(
                    text = "태그를 인식하면 쿠폰이 자동으로 발급돼요",
                    style = CafeTheme.typography.body,
                    color = colors.body,
                )
            }
        }
    }

    state.errorMessage?.let { message ->
        Text(
            text = message,
            style = CafeTheme.typography.caption,
            color = colors.error,
        )
    }
}

@Composable
private fun UnsupportedContent() {
    val colors = CafeTheme.colors

    Text(
        text = "NFC를 사용할 수 없어요",
        style = CafeTheme.typography.h1,
        color = colors.ink,
    )
    Text(
        text = "이 기기는 NFC를 지원하지 않아요",
        style = CafeTheme.typography.body,
        color = colors.muted,
    )
}

@Composable
private fun DisabledContent(onOpenNfcSettings: () -> Unit) {
    val colors = CafeTheme.colors

    Text(
        text = "NFC가 꺼져 있어요",
        style = CafeTheme.typography.h1,
        color = colors.ink,
    )
    Text(
        text = "설정에서 NFC를 켜고 다시 시도해 주세요",
        style = CafeTheme.typography.body,
        color = colors.muted,
    )
    CafeButton(
        text = "NFC 설정 열기",
        onClick = onOpenNfcSettings,
        modifier = Modifier.fillMaxWidth(),
        variant = CafeButtonVariant.Secondary,
    )
}

@Composable
private fun NfcClaimResultDialog(
    result: NfcClaimResultUi,
    onConfirm: () -> Unit,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Dialog(onDismissRequest = onConfirm) {
        Surface(
            shape = CafeTheme.shapes.radiusXl,
            color = colors.canvas,
            contentColor = colors.body,
        ) {
            Column(
                modifier = Modifier.padding(
                    start = spacing.space5,
                    top = spacing.space8,
                    end = spacing.space5,
                    bottom = spacing.space5,
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.space6),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(spacing.space2),
                ) {
                    Text(
                        text = "쿠폰이 발급됐어요",
                        style = CafeTheme.typography.h2,
                        color = colors.ink,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = result.amountLabel,
                        style = CafeTheme.typography.display,
                        color = colors.primary,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = result.expiresLabel,
                        style = CafeTheme.typography.caption,
                        color = colors.muted,
                        textAlign = TextAlign.Center,
                    )
                    result.message?.let { message ->
                        Text(
                            text = message,
                            style = CafeTheme.typography.body,
                            color = colors.body,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                CafeButton(
                    text = "확인",
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private const val ClaimedToast = "쿠폰이 발급됐어요"
