package com.ssafy.cafeminsu.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.ssafy.cafeminsu.core.designsystem.theme.CafeMinsuTheme

@Composable
fun HomeRoute(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CafeMinsuTheme.colors.canvas)
            .padding(CafeMinsuTheme.spacing.space6),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "카페민수",
            color = CafeMinsuTheme.colors.primary,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = "홈 화면을 준비 중이에요.",
            color = CafeMinsuTheme.colors.body,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
