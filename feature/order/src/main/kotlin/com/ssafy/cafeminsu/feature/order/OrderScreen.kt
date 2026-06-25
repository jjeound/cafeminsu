package com.ssafy.cafeminsu.feature.order

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ssafy.cafeminsu.core.designsystem.component.PlaceholderFeatureScreen

@Composable
fun OrderRoute(modifier: Modifier = Modifier) {
    PlaceholderFeatureScreen(
        title = "주문 결과",
        subtitle = "주문 완료, 대기, 취소 상태를 보여줄 화면입니다.",
        modifier = modifier,
    )
}
