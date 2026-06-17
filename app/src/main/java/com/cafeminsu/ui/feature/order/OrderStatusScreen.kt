package com.cafeminsu.ui.feature.order

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cafeminsu.ui.theme.CafeTheme

@Composable
fun OrderStatusScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CafeTheme.colors.canvas)
            .padding(
                start = CafeTheme.spacing.space5,
                top = CafeTheme.spacing.space6,
                end = CafeTheme.spacing.space5,
                bottom = CafeTheme.spacing.space6,
            ),
    ) {
        Text(
            text = "주문 상태 (M-07)",
            style = CafeTheme.typography.h1,
            color = CafeTheme.colors.ink,
        )
    }
}
