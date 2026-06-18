package com.cafeminsu.ui.feature.my

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafeminsu.ui.components.CafeButton
import com.cafeminsu.ui.components.CafeButtonVariant
import com.cafeminsu.ui.components.CafeCard
import com.cafeminsu.ui.components.CafeCardType
import com.cafeminsu.ui.components.EmptyView
import com.cafeminsu.ui.components.ErrorView
import com.cafeminsu.ui.components.LoadingView
import com.cafeminsu.ui.theme.CafeTheme
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MyRoute(
    onOrderClick: (String) -> Unit,
    onBrowseMenuClick: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    MyScreen(
        state = state,
        onOrderClick = onOrderClick,
        onBrowseMenuClick = onBrowseMenuClick,
        onLoginClick = onLoginClick,
        onLogoutClick = viewModel::onLogout,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
fun MyScreen(
    state: MyUiState,
    onOrderClick: (String) -> Unit,
    onBrowseMenuClick: () -> Unit,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onRetry: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space5),
        ) {
            Text(
                text = "마이페이지",
                style = CafeTheme.typography.h1,
                color = CafeTheme.colors.ink,
            )

            when (state) {
                MyUiState.Loading -> LoadingView()
                is MyUiState.Content -> MyContent(
                    profile = state.profile,
                    recentOrders = state.recentOrders,
                    settings = state.settings,
                    appMeta = state.appMeta,
                    onOrderClick = onOrderClick,
                    onLogoutClick = onLogoutClick,
                )

                is MyUiState.Empty -> MyEmpty(
                    state = state,
                    onBrowseMenuClick = onBrowseMenuClick,
                    onLogoutClick = onLogoutClick,
                )

                is MyUiState.Error -> ErrorView(
                    message = state.message,
                    retryable = state.retryable,
                    onRetry = onRetry,
                )

                is MyUiState.NeedsLogin -> EmptyView(
                    message = state.message,
                    actionLabel = state.actionLabel,
                    onAction = onLoginClick,
                )
            }
        }
    }
}

@Composable
private fun MyContent(
    profile: MyProfileUiModel,
    recentOrders: List<MyOrderSummaryUiModel>,
    settings: List<MySettingItemUiModel>,
    appMeta: String,
    onOrderClick: (String) -> Unit,
    onLogoutClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space6)) {
        ProfileHeader(profile = profile)
        OrderHistoryList(
            recentOrders = recentOrders,
            onOrderClick = onOrderClick,
        )
        SettingsSection(
            settings = settings,
            appMeta = appMeta,
            onLogoutClick = onLogoutClick,
        )
    }
}

@Composable
private fun MyEmpty(
    state: MyUiState.Empty,
    onBrowseMenuClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space6)) {
        ProfileHeader(profile = state.profile)
        Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space4)) {
            SectionTitle(text = "주문 내역")
            EmptyView(
                message = state.message,
                actionLabel = state.actionLabel,
                onAction = onBrowseMenuClick,
            )
        }
        SettingsSection(
            settings = state.settings,
            appMeta = state.appMeta,
            onLogoutClick = onLogoutClick,
        )
    }
}

@Composable
private fun ProfileHeader(profile: MyProfileUiModel) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    CafeCard(
        modifier = Modifier.fillMaxWidth(),
        type = CafeCardType.Default,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
            Text(
                text = profile.displayName,
                style = CafeTheme.typography.h2,
                color = colors.ink,
            )
            Text(
                text = maskedPhone(profile.phoneLast4),
                style = CafeTheme.typography.caption,
                color = colors.muted,
            )
        }
    }
}

@Composable
private fun OrderHistoryList(
    recentOrders: List<MyOrderSummaryUiModel>,
    onOrderClick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space4)) {
        SectionTitle(text = "주문 내역")
        recentOrders.forEach { order ->
            OrderHistoryItem(
                order = order,
                onClick = { onOrderClick(order.orderId) },
            )
        }
    }
}

@Composable
private fun OrderHistoryItem(
    order: MyOrderSummaryUiModel,
    onClick: () -> Unit,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    CafeCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        type = CafeCardType.Info,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(OrderTextWeight),
                    verticalArrangement = Arrangement.spacedBy(spacing.space1),
                ) {
                    Text(
                        text = "주문번호 ${order.orderNumber}",
                        style = CafeTheme.typography.h3,
                        color = colors.ink,
                    )
                    Text(
                        text = formatOrderDate(order.createdAtMillis),
                        style = CafeTheme.typography.caption,
                        color = colors.muted,
                    )
                }
                Text(
                    text = order.statusLabel,
                    style = CafeTheme.typography.caption,
                    color = colors.primary,
                )
            }

            Text(
                text = formatWon(order.totalAmount),
                style = CafeTheme.typography.bodyL,
                color = colors.body,
            )
        }
    }
}

@Composable
private fun SettingsSection(
    settings: List<MySettingItemUiModel>,
    appMeta: String,
    onLogoutClick: () -> Unit,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
        SectionTitle(text = "설정")
        settings.forEach { setting ->
            if (setting.id == LogoutSettingId) {
                CafeButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = setting.label,
                    onClick = onLogoutClick,
                    variant = CafeButtonVariant.Secondary,
                )
            }
        }
        Text(
            text = appMeta,
            style = CafeTheme.typography.meta,
            color = colors.mutedSoft,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = CafeTheme.typography.h2,
        color = CafeTheme.colors.ink,
    )
}

private fun maskedPhone(phoneLast4: String?): String =
    phoneLast4
        ?.takeIf { it.isNotBlank() }
        ?.let { "010-****-$it" }
        ?: "전화번호 미등록"

private fun formatOrderDate(createdAtMillis: Long): String =
    Instant.ofEpochMilli(createdAtMillis)
        .atZone(ZoneId.systemDefault())
        .format(orderDateFormatter)

private fun formatWon(amount: Int): String =
    "${NumberFormat.getNumberInstance(Locale.KOREA).format(amount)}원"

private val orderDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.KOREA)

private const val LogoutSettingId = "logout"
private const val OrderTextWeight = 1f
