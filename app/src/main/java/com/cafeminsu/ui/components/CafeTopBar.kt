package com.cafeminsu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.cafeminsu.ui.theme.CafeTheme

@Composable
fun CafeTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    onNavigationClick: (() -> Unit)? = null,
    actionIcon: (@Composable () -> Unit)? = null,
    onActionClick: (() -> Unit)? = null,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(spacing.space14)
            .background(colors.canvas)
            .padding(horizontal = spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (navigationIcon != null) {
            TopBarIconSlot(
                onClick = onNavigationClick,
                icon = navigationIcon,
            )
        } else {
            Spacer(modifier = Modifier.width(spacing.space5))
        }

        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = spacing.space2),
            style = CafeTheme.typography.h2,
            color = colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (actionIcon != null) {
            TopBarIconSlot(
                onClick = onActionClick,
                icon = actionIcon,
            )
        } else {
            Spacer(modifier = Modifier.width(spacing.space5))
        }
    }
}

@Composable
private fun TopBarIconSlot(
    onClick: (() -> Unit)?,
    icon: @Composable () -> Unit,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing
    val touchTargetSize = spacing.space10 + spacing.space2
    val iconSize = spacing.space6
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            role = Role.Button,
            onClick = onClick,
        )
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .size(touchTargetSize)
            .semantics(mergeDescendants = true) {}
            .then(clickModifier),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.ink) {
            ProvideTextStyle(CafeTheme.typography.bodyL.copy(color = colors.ink)) {
                Box(
                    modifier = Modifier.size(iconSize),
                    contentAlignment = Alignment.Center,
                ) {
                    icon()
                }
            }
        }
    }
}
