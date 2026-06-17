package com.cafeminsu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import com.cafeminsu.ui.theme.CafeTheme

enum class CafeButtonVariant {
    Primary,
    Secondary,
    Ghost,
    Dark,
}

@Composable
fun CafeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: CafeButtonVariant = CafeButtonVariant.Primary,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val style = cafeButtonStyle(variant)
    val containerColor = when {
        !enabled && variant == CafeButtonVariant.Ghost -> Color.Transparent
        !enabled && variant == CafeButtonVariant.Secondary -> colors.canvas
        !enabled -> colors.hairline
        pressed -> style.pressedColor
        else -> style.containerColor
    }
    val contentColor = if (enabled) style.contentColor else colors.muted
    val border = style.borderColor?.let { BorderStroke(spacing.space1 / BorderWidthDivider, it) }
    val buttonHeight = spacing.space10 + spacing.space3

    Surface(
        onClick = onClick,
        modifier = modifier.semantics(mergeDescendants = true) {},
        enabled = enabled,
        shape = CafeTheme.shapes.radiusLg,
        color = containerColor,
        contentColor = contentColor,
        border = border,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier
                .height(buttonHeight)
                .padding(horizontal = spacing.space5),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                ProvideTextStyle(CafeTheme.typography.bodyL.copy(color = contentColor)) {
                    if (icon != null) {
                        icon()
                        Spacer(modifier = Modifier.width(spacing.space2))
                    }

                    Text(
                        text = text,
                        style = CafeTheme.typography.bodyL,
                        color = contentColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun cafeButtonStyle(variant: CafeButtonVariant): CafeButtonStyle {
    val colors = CafeTheme.colors

    return when (variant) {
        CafeButtonVariant.Primary -> CafeButtonStyle(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            pressedColor = colors.primaryHover,
            borderColor = null,
        )

        CafeButtonVariant.Secondary -> CafeButtonStyle(
            containerColor = colors.canvas,
            contentColor = colors.ink,
            pressedColor = colors.surfaceCard,
            borderColor = colors.hairline,
        )

        CafeButtonVariant.Ghost -> CafeButtonStyle(
            containerColor = Color.Transparent,
            contentColor = colors.primary,
            pressedColor = colors.accentSoft,
            borderColor = null,
        )

        CafeButtonVariant.Dark -> CafeButtonStyle(
            containerColor = colors.surfaceDark,
            contentColor = colors.onDark,
            pressedColor = colors.ink,
            borderColor = null,
        )
    }
}

private data class CafeButtonStyle(
    val containerColor: Color,
    val contentColor: Color,
    val pressedColor: Color,
    val borderColor: Color?,
)

private const val BorderWidthDivider = 4
