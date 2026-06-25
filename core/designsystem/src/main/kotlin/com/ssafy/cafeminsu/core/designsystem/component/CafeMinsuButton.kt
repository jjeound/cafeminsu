package com.ssafy.cafeminsu.core.designsystem.component

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
import com.ssafy.cafeminsu.core.designsystem.theme.CafeMinsuTheme

enum class CafeMinsuButtonVariant {
    Primary,
    Secondary,
    Kakao,
}

@Composable
fun CafeMinsuButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: CafeMinsuButtonVariant = CafeMinsuButtonVariant.Primary,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
) {
    val colors = CafeMinsuTheme.colors
    val spacing = CafeMinsuTheme.spacing
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val style = when (variant) {
        CafeMinsuButtonVariant.Primary -> ButtonStyle(colors.primary, colors.onPrimary, colors.primaryHover, null)
        CafeMinsuButtonVariant.Secondary -> ButtonStyle(colors.canvas, colors.ink, colors.surfaceCard, colors.hairline)
        CafeMinsuButtonVariant.Kakao -> ButtonStyle(colors.kakaoYellow, colors.ink, colors.kakaoYellow, null)
    }
    val containerColor = when {
        !enabled -> colors.hairline
        pressed -> style.pressedColor
        else -> style.containerColor
    }
    val contentColor = if (enabled) style.contentColor else colors.muted
    val border = style.borderColor?.let { BorderStroke(spacing.space1 / BorderWidthDivider, it) }

    Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = CafeMinsuTheme.shapes.radiusLg,
        color = containerColor,
        contentColor = contentColor,
        border = border,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier
                .height(spacing.space10 + spacing.space3)
                .padding(horizontal = spacing.space5),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                ProvideTextStyle(CafeMinsuTheme.typography.bodyL.copy(color = contentColor)) {
                    if (icon != null) {
                        icon()
                        Spacer(modifier = Modifier.width(spacing.space2))
                    }
                    Text(text = text, style = CafeMinsuTheme.typography.bodyL, color = contentColor)
                }
            }
        }
    }
}

private data class ButtonStyle(
    val containerColor: Color,
    val contentColor: Color,
    val pressedColor: Color,
    val borderColor: Color?,
)

private const val BorderWidthDivider = 4
