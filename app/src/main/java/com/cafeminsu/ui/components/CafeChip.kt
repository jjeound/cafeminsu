package com.cafeminsu.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import com.cafeminsu.ui.theme.CafeTheme

@Composable
fun CafeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing
    val containerColor = when {
        !enabled -> colors.hairline
        selected -> colors.primary
        else -> colors.accentSoft
    }
    val contentColor = when {
        !enabled -> colors.muted
        selected -> colors.onPrimary
        else -> colors.primary
    }

    Surface(
        onClick = onClick,
        modifier = modifier.semantics(mergeDescendants = true) {},
        enabled = enabled,
        shape = CafeTheme.shapes.radiusPill,
        color = containerColor,
        contentColor = contentColor,
        interactionSource = remember { MutableInteractionSource() },
    ) {
        Box(
            modifier = Modifier
                .height(spacing.space8)
                .padding(horizontal = spacing.space3),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                ProvideTextStyle(CafeTheme.typography.caption.copy(color = contentColor)) {
                    Text(
                        text = text,
                        style = CafeTheme.typography.caption,
                        color = contentColor,
                    )
                }
            }
        }
    }
}
