package com.cafeminsu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.cafeminsu.ui.theme.CafeTheme

enum class CafeCardType {
    Default,
    Product,
    Info,
}

@Composable
fun CafeCard(
    modifier: Modifier = Modifier,
    type: CafeCardType = CafeCardType.Default,
    content: @Composable () -> Unit,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing
    val style = when (type) {
        CafeCardType.Default -> CafeCardStyle(
            containerColor = colors.surfaceCard,
            contentColor = colors.body,
            borderColor = null,
            shape = CafeTheme.shapes.radiusLg,
        )

        CafeCardType.Product -> CafeCardStyle(
            containerColor = colors.surfaceDark,
            contentColor = colors.onDark,
            borderColor = null,
            shape = CafeTheme.shapes.radiusXl,
        )

        CafeCardType.Info -> CafeCardStyle(
            containerColor = colors.canvas,
            contentColor = colors.body,
            borderColor = colors.hairline,
            shape = CafeTheme.shapes.radiusLg,
        )
    }
    val border = style.borderColor?.let { BorderStroke(spacing.space1 / BorderWidthDivider, it) }

    Surface(
        modifier = modifier,
        shape = style.shape,
        color = style.containerColor,
        contentColor = style.contentColor,
        border = border,
    ) {
        Box(modifier = Modifier.padding(spacing.space5)) {
            content()
        }
    }
}

private data class CafeCardStyle(
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color?,
    val shape: androidx.compose.foundation.shape.RoundedCornerShape,
)

private const val BorderWidthDivider = 4
