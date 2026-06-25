package com.ssafy.cafeminsu.feature.owner.sales

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ssafy.cafeminsu.core.designsystem.component.PlaceholderFeatureScreen

@Composable
fun OwnerSalesRoute(modifier: Modifier = Modifier) {
    PlaceholderFeatureScreen(
        title = "사장님 매출",
        subtitle = "매출과 정산 요약 화면입니다.",
        modifier = modifier,
    )
}
