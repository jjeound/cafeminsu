package com.ssafy.cafeminsu.feature.owner.orders

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ssafy.cafeminsu.core.designsystem.component.PlaceholderFeatureScreen

@Composable
fun OwnerOrdersRoute(modifier: Modifier = Modifier) {
    PlaceholderFeatureScreen(
        title = "사장님 주문",
        subtitle = "접수 및 처리 상태를 보여줄 화면입니다.",
        modifier = modifier,
    )
}
