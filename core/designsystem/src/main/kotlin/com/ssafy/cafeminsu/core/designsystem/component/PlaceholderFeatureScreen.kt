package com.ssafy.cafeminsu.core.designsystem.component

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
import com.ssafy.cafeminsu.core.designsystem.theme.CafeMinsuTheme

@Composable
fun PlaceholderFeatureScreen(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val colors = CafeMinsuTheme.colors
    val spacing = CafeMinsuTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(color = colors.surfaceCard, shape = CafeMinsuTheme.shapes.radiusXl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.space2),
            ) {
                Text(text = title, style = CafeMinsuTheme.typography.h1, color = colors.ink)
                Text(
                    text = subtitle,
                    style = CafeMinsuTheme.typography.body,
                    color = colors.muted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
