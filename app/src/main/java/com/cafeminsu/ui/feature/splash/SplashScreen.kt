package com.cafeminsu.ui.feature.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.cafeminsu.ui.theme.CafeTheme

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    var contentVisible by remember { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (contentVisible) VisibleAlpha else HiddenAlpha,
        label = "splashContentAlpha",
    )

    LaunchedEffect(Unit) {
        contentVisible = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.primary),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.alpha(contentAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "카페민수",
                style = CafeTheme.typography.display,
                color = colors.onPrimary,
            )
            Text(
                text = "Warm cream coffee",
                style = CafeTheme.typography.caption,
                color = colors.onPrimary.copy(alpha = TaglineAlpha),
            )
        }
    }
}

private const val VisibleAlpha = 1f
private const val HiddenAlpha = 0f
private const val TaglineAlpha = 0.72f
