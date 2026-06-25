package com.ssafy.cafeminsu.feature.coupon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ssafy.cafeminsu.core.designsystem.component.PlaceholderFeatureScreen

@Composable
fun CouponRoute(modifier: Modifier = Modifier) {
    PlaceholderFeatureScreen(
        title = "쿠폰",
        subtitle = "보유 쿠폰과 사용 내역을 표시할 화면입니다.",
        modifier = modifier,
    )
}
