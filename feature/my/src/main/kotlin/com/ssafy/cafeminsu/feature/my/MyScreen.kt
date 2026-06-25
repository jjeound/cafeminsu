package com.ssafy.cafeminsu.feature.my

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ssafy.cafeminsu.core.designsystem.theme.CafeMinsuTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Composable
fun MyRoute(
    onHistoryClick: () -> Unit = {},
    onGiftClick: () -> Unit = {},
    onCouponClick: () -> Unit = {},
    onNotificationSettingsClick: () -> Unit = {},
    onTermsClick: () -> Unit = {},
    onFaqClick: () -> Unit = {},
    onSupportClick: () -> Unit = {},
    onVersionClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MyViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    MyScreen(
        state = uiState,
        onHistoryClick = onHistoryClick,
        onGiftClick = onGiftClick,
        onCouponClick = onCouponClick,
        onNotificationSettingsClick = onNotificationSettingsClick,
        onTermsClick = onTermsClick,
        onFaqClick = onFaqClick,
        onSupportClick = onSupportClick,
        onVersionClick = onVersionClick,
        onLogoutClick = onLogoutClick,
        modifier = modifier,
    )
}

@Composable
fun MyScreen(
    state: MyUiState,
    onHistoryClick: () -> Unit,
    onGiftClick: () -> Unit,
    onCouponClick: () -> Unit,
    onNotificationSettingsClick: () -> Unit,
    onTermsClick: () -> Unit,
    onFaqClick: () -> Unit,
    onSupportClick: () -> Unit,
    onVersionClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeMinsuTheme.colors
    val spacing = CafeMinsuTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .padding(horizontal = spacing.space5, vertical = spacing.space6)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(spacing.space5),
    ) {
        ProfileCard(state = state)
        QuickMenuSection(
            items = state.quickMenus,
            onHistoryClick = onHistoryClick,
            onGiftClick = onGiftClick,
            onCouponClick = onCouponClick,
            onNotificationSettingsClick = onNotificationSettingsClick,
        )
        SettingsSection(
            items = state.settings,
            onTermsClick = onTermsClick,
            onFaqClick = onFaqClick,
            onSupportClick = onSupportClick,
            onVersionClick = onVersionClick,
            onLogoutClick = onLogoutClick,
        )
        Spacer(modifier = Modifier.height(spacing.space8))
    }
}

@Composable
private fun ProfileCard(state: MyUiState) {
    val colors = CafeMinsuTheme.colors

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surfaceDark,
        shape = CafeMinsuTheme.shapes.radiusXl,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BoxAvatar(initial = state.displayName.firstOrNull()?.toString().orEmpty())
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = state.displayName,
                            style = CafeMinsuTheme.typography.h2,
                            color = colors.onDark,
                        )
                        TierChip(text = state.gradeLabel)
                    }
                }
            }

            StatsRow(state = state)
        }
    }
}

@Composable
private fun StatsRow(state: MyUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatCell(
            value = state.orderCount,
            label = "주문",
            modifier = Modifier.weight(1f),
        )
        StatDivider()
        StatCell(
            value = state.stampCount,
            label = "스탬프",
            modifier = Modifier.weight(1f),
        )
        StatDivider()
        StatCell(
            value = state.couponCount,
            label = "쿠폰",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCell(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = CafeMinsuTheme.colors

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = value,
            style = CafeMinsuTheme.typography.h3,
            color = colors.onDark,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = label,
            style = CafeMinsuTheme.typography.caption,
            color = colors.muted,
            maxLines = 1,
        )
    }
}

@Composable
private fun StatDivider() {
    Spacer(
        modifier = Modifier
            .width(1.dp)
            .height(32.dp)
            .background(CafeMinsuTheme.colors.ink.copy(alpha = 0.18f)),
    )
}

@Composable
private fun TierChip(text: String) {
    val colors = CafeMinsuTheme.colors

    Surface(
        shape = CafeMinsuTheme.shapes.radiusPill,
        color = colors.accentSoft,
        contentColor = colors.primary,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = CafeMinsuTheme.typography.caption,
                color = colors.primary,
            )
        }
    }
}

@Composable
private fun QuickMenuSection(
    items: List<QuickMenuUiModel>,
    onHistoryClick: () -> Unit,
    onGiftClick: () -> Unit,
    onCouponClick: () -> Unit,
    onNotificationSettingsClick: () -> Unit,
) {
    val colors = CafeMinsuTheme.colors
    val spacing = CafeMinsuTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
        Text(text = "빠른메뉴", style = CafeMinsuTheme.typography.h2, color = colors.ink)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space2),
        ) {
            items.forEach { item ->
                QuickMenuTile(
                    modifier = Modifier.weight(1f),
                    item = item,
                    onClick = when (item.id) {
                        QuickMenuId.History -> onHistoryClick
                        QuickMenuId.Gift -> onGiftClick
                        QuickMenuId.Coupon -> onCouponClick
                        else -> onNotificationSettingsClick
                    },
                )
            }
        }
    }
}

@Composable
private fun QuickMenuTile(
    modifier: Modifier = Modifier,
    item: QuickMenuUiModel,
    onClick: () -> Unit,
) {
    val colors = CafeMinsuTheme.colors

    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = colors.surfaceCard,
        shape = CafeMinsuTheme.shapes.radiusLg,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 18.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = item.iconLabel,
                    style = CafeMinsuTheme.typography.bodyL,
                    color = colors.primaryHover,
                )
            }
            Text(
                text = item.title,
                style = CafeMinsuTheme.typography.bodyL,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SettingsSection(
    items: List<SettingUiModel>,
    onTermsClick: () -> Unit,
    onFaqClick: () -> Unit,
    onSupportClick: () -> Unit,
    onVersionClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    val colors = CafeMinsuTheme.colors

    Surface(
        color = colors.surfaceCard,
        shape = CafeMinsuTheme.shapes.radiusLg,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            items.forEachIndexed { index, item ->
                val onClick = when (item.id) {
                    SettingId.Terms -> onTermsClick
                    SettingId.Faq -> onFaqClick
                    SettingId.Support -> onSupportClick
                    SettingId.Version -> onVersionClick
                    SettingId.Logout -> onLogoutClick
                    else -> onLogoutClick
                }
                SettingRow(item = item, onClick = onClick)
                if (index != items.lastIndex) {
                    androidx.compose.material3.HorizontalDivider(color = colors.hairline)
                }
            }
        }
    }
}

@Composable
private fun SettingRow(
    item: SettingUiModel,
    onClick: () -> Unit,
) {
    val colors = CafeMinsuTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = item.title,
            style = CafeMinsuTheme.typography.bodyL,
            color = if (item.isDestructive) colors.primary else colors.ink,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            item.trailingText?.let { trailingText ->
                Text(
                    text = trailingText,
                    style = CafeMinsuTheme.typography.caption,
                    color = colors.mutedSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                )
                Spacer(modifier = Modifier.size(8.dp))
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.mutedSoft,
            )
        }
    }
}

@Composable
private fun BoxAvatar(initial: String) {
    val colors = CafeMinsuTheme.colors

    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(colors.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial.ifBlank { "M" },
            style = CafeMinsuTheme.typography.h2,
            color = colors.onPrimary,
        )
    }
}

class MyViewModel : ViewModel() {
    private val mutableUiState = MutableStateFlow(
        MyUiState(
            displayName = "카페민수",
            gradeLabel = "BRONZE",
            orderCount = "12",
            stampCount = "8/10",
            couponCount = "3",
            quickMenus = listOf(
                QuickMenuUiModel(QuickMenuId.History, "주문내역", "↻"),
                QuickMenuUiModel(QuickMenuId.Gift, "선물하기", "🎁"),
                QuickMenuUiModel(QuickMenuId.Coupon, "쿠폰", "◎"),
                QuickMenuUiModel(QuickMenuId.Notification, "알림설정", "◌"),
            ),
            settings = listOf(
                SettingUiModel(SettingId.Terms, "이용약관"),
                SettingUiModel(SettingId.Faq, "자주묻는질문"),
                SettingUiModel(SettingId.Support, "고객센터"),
                SettingUiModel(SettingId.Version, "버전정보", trailingText = "1.0.0"),
                SettingUiModel(SettingId.Logout, "로그아웃", isDestructive = true),
            ),
        ),
    )

    val uiState: StateFlow<MyUiState> = mutableUiState.asStateFlow()
}

data class MyUiState(
    val displayName: String,
    val gradeLabel: String,
    val orderCount: String,
    val stampCount: String,
    val couponCount: String,
    val quickMenus: List<QuickMenuUiModel>,
    val settings: List<SettingUiModel>,
)

data class QuickMenuUiModel(
    val id: String,
    val title: String,
    val iconLabel: String,
)

data class SettingUiModel(
    val id: String,
    val title: String,
    val trailingText: String? = null,
    val isDestructive: Boolean = false,
)

private object QuickMenuId {
    const val History = "history"
    const val Gift = "gift"
    const val Coupon = "coupon"
    const val Notification = "notification"
}

private object SettingId {
    const val Terms = "terms"
    const val Faq = "faq"
    const val Support = "support"
    const val Version = "version"
    const val Logout = "logout"
}
