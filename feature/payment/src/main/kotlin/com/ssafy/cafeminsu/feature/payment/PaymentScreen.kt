package com.ssafy.cafeminsu.feature.payment

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ssafy.cafeminsu.core.designsystem.component.PlaceholderFeatureScreen

@Composable
fun PaymentRoute(modifier: Modifier = Modifier) {
    PlaceholderFeatureScreen(
        title = "결제",
        subtitle = "카카오페이 리다이렉트와 결제 상태를 연결할 자리입니다.",
        modifier = modifier,
    )
}
