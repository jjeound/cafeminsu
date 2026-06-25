package com.cafeminsu.ui.feature.notification.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafeminsu.R
import com.cafeminsu.ui.components.CafeCard
import com.cafeminsu.ui.components.CafeCardType
import com.cafeminsu.ui.components.CafeTopBar
import com.cafeminsu.ui.theme.CafeTheme

@Composable
fun NotificationSettingsRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    NotificationSettingsScreen(
        state = state,
        onBackClick = onBackClick,
        onToggle = viewModel::onToggle,
        modifier = modifier,
    )
}

@Composable
fun NotificationSettingsScreen(
    state: NotificationSettingsUiState,
    onBackClick: () -> Unit,
    onToggle: (NotificationCategory, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = CafeTheme.colors.canvas,
        contentColor = CafeTheme.colors.body,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CafeTopBar(
                title = "알림 설정",
                navigationIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_left),
                        contentDescription = null,
                        tint = CafeTheme.colors.ink,
                    )
                },
                onNavigationClick = onBackClick,
            )

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
                verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space4),
            ) {
                CafeCard(
                    modifier = Modifier.fillMaxWidth(),
                    type = CafeCardType.Default,
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ToggleSpecs.forEachIndexed { index, spec ->
                            NotificationToggleRow(
                                label = spec.label,
                                description = spec.description,
                                checked = state.isEnabled(spec.category),
                                onCheckedChange = { enabled -> onToggle(spec.category, enabled) },
                            )
                            if (index != ToggleSpecs.lastIndex) {
                                HorizontalDivider(color = CafeTheme.colors.hairline)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = spacing.space14)
            .padding(vertical = spacing.space3),
        horizontalArrangement = Arrangement.spacedBy(spacing.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(LabelWeight),
            verticalArrangement = Arrangement.spacedBy(spacing.space1),
        ) {
            Text(
                text = label,
                style = CafeTheme.typography.bodyL,
                color = colors.ink,
            )
            Text(
                text = description,
                style = CafeTheme.typography.caption,
                color = colors.muted,
            )
        }
        Switch(
            modifier = Modifier.semantics { contentDescription = "$label 받기" },
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onPrimary,
                checkedTrackColor = colors.primary,
                checkedBorderColor = colors.primary,
                uncheckedThumbColor = colors.canvas,
                uncheckedTrackColor = colors.hairline,
                uncheckedBorderColor = colors.hairline,
            ),
        )
    }
}

private data class NotificationToggleSpec(
    val category: NotificationCategory,
    val label: String,
    val description: String,
)

private val ToggleSpecs = listOf(
    NotificationToggleSpec(
        category = NotificationCategory.OrderStatus,
        label = "주문 상태 알림",
        description = "주문 접수·준비·완료 소식을 알려드려요",
    ),
    NotificationToggleSpec(
        category = NotificationCategory.Promotion,
        label = "혜택·이벤트 알림",
        description = "스탬프·쿠폰·이벤트 소식을 알려드려요",
    ),
    NotificationToggleSpec(
        category = NotificationCategory.Marketing,
        label = "마케팅 정보 수신",
        description = "맞춤 혜택·광고성 정보를 받아요",
    ),
)

private const val LabelWeight = 1f
