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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ssafy.cafeminsu.core.designsystem.theme.CafeMinsuTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Composable
fun MyRoute(
    modifier: Modifier = Modifier,
    viewModel: MyViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    MyScreen(
        state = uiState,
        modifier = modifier,
    )
}

@Composable
fun MyScreen(
    state: MyUiState,
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
        StatsRow(stats = state.stats)
        QuickMenuSection(items = state.quickMenus)
        SettingsList(items = state.settings)
        Spacer(modifier = Modifier.height(spacing.space8))
    }
}

@Composable
private fun ProfileCard(state: MyUiState) {
    val colors = CafeMinsuTheme.colors
    val spacing = CafeMinsuTheme.spacing

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surfaceDark,
        shape = CafeMinsuTheme.shapes.radiusXl,
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BoxAvatar()
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = state.displayName, style = CafeMinsuTheme.typography.h2, color = colors.onDark)
                Text(text = state.email, style = CafeMinsuTheme.typography.caption, color = colors.mutedSoft)
                Text(text = state.gradeLabel, style = CafeMinsuTheme.typography.body, color = colors.onDark)
            }
        }
    }
}

@Composable
private fun StatsRow(stats: MyStatsUiModel) {
    val colors = CafeMinsuTheme.colors

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCell(label = "보유 스탬프", value = stats.stampCount.toString(), modifier = Modifier.weight(1f))
        StatCell(label = "쿠폰", value = stats.couponCount.toString(), modifier = Modifier.weight(1f))
        StatCell(label = "주문", value = stats.orderCount.toString(), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val colors = CafeMinsuTheme.colors

    Surface(
        modifier = modifier,
        color = colors.surfaceCard,
        shape = CafeMinsuTheme.shapes.radiusLg,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = value, style = CafeMinsuTheme.typography.h2, color = colors.ink)
            Text(text = label, style = CafeMinsuTheme.typography.caption, color = colors.muted)
        }
    }
}

@Composable
private fun QuickMenuSection(items: List<QuickMenuUiModel>) {
    val colors = CafeMinsuTheme.colors
    val spacing = CafeMinsuTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
        Text(text = "바로가기", style = CafeMinsuTheme.typography.h2, color = colors.ink)
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.space3), modifier = Modifier.fillMaxWidth()) {
            items.chunked(2).forEach { columnItems ->
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
                    columnItems.forEach { item ->
                        QuickMenuTile(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickMenuTile(item: QuickMenuUiModel) {
    val colors = CafeMinsuTheme.colors

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = item.onClick),
        color = colors.surfaceCard,
        shape = CafeMinsuTheme.shapes.radiusLg,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = item.iconLabel, style = CafeMinsuTheme.typography.bodyL, color = colors.primaryHover)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.title, style = CafeMinsuTheme.typography.bodyL, color = colors.ink)
                Text(text = item.subtitle, style = CafeMinsuTheme.typography.caption, color = colors.muted)
            }
        }
    }
}

@Composable
private fun SettingsList(items: List<SettingUiModel>) {
    val colors = CafeMinsuTheme.colors
    val spacing = CafeMinsuTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
        Text(text = "설정", style = CafeMinsuTheme.typography.h2, color = colors.ink)
        items.forEach { item ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = item.onClick),
                color = colors.surfaceCard,
                shape = CafeMinsuTheme.shapes.radiusLg,
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = item.title, style = CafeMinsuTheme.typography.bodyL, color = colors.ink)
                        Text(text = item.subtitle, style = CafeMinsuTheme.typography.caption, color = colors.muted)
                    }
                    Text(text = "›", style = CafeMinsuTheme.typography.h2, color = colors.mutedSoft)
                }
            }
        }
    }
}

@Composable
private fun BoxAvatar() {
    val colors = CafeMinsuTheme.colors
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(colors.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "민", style = CafeMinsuTheme.typography.h2, color = colors.onPrimary)
    }
}

class MyViewModel : ViewModel() {
    val uiState: StateFlow<MyUiState> = MutableStateFlow(
        MyUiState(
            displayName = "민수님",
            email = "minsu@cafeminsu.com",
            gradeLabel = "웰컴 등급",
            stats = MyStatsUiModel(stampCount = 8, couponCount = 3, orderCount = 12),
            quickMenus = listOf(
                QuickMenuUiModel("주문내역", "최근 주문을 확인해요", "⌁", onClick = {}),
                QuickMenuUiModel("기프트", "선물함을 확인해요", "✦", onClick = {}),
                QuickMenuUiModel("알림", "푸시 설정을 조정해요", "◌", onClick = {}),
                QuickMenuUiModel("도움말", "문의 및 약관", "?", onClick = {}),
            ),
            settings = listOf(
                SettingUiModel("계정 관리", "로그인 정보와 프로필", onClick = {}),
                SettingUiModel("알림 설정", "수신 여부를 관리", onClick = {}),
                SettingUiModel("앱 정보", "버전 및 정책", onClick = {}),
            ),
        ),
    ).asStateFlow()
}

data class MyUiState(
    val displayName: String,
    val email: String,
    val gradeLabel: String,
    val stats: MyStatsUiModel,
    val quickMenus: List<QuickMenuUiModel>,
    val settings: List<SettingUiModel>,
)

data class MyStatsUiModel(
    val stampCount: Int,
    val couponCount: Int,
    val orderCount: Int,
)

data class QuickMenuUiModel(
    val title: String,
    val subtitle: String,
    val iconLabel: String,
    val onClick: () -> Unit,
)

data class SettingUiModel(
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit,
)
