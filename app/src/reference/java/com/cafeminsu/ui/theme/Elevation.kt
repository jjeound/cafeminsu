package com.cafeminsu.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class CafeShadow(
    val offsetY: Dp,
    val blurRadius: Dp,
    val color: Color,
)

val elevCard = CafeShadow(
    offsetY = 1.dp,
    blurRadius = 2.dp,
    color = ink.copy(alpha = 0.04f),
)

val elevOverlay = CafeShadow(
    offsetY = 8.dp,
    blurRadius = 24.dp,
    color = ink.copy(alpha = 0.08f),
)

data class CafeElevations(
    val elevCard: CafeShadow,
    val elevOverlay: CafeShadow,
)

val LocalCafeElevations = staticCompositionLocalOf<CafeElevations> {
    kotlin.error("CafeElevations is not provided")
}

fun cafeElevations() = CafeElevations(
    elevCard = elevCard,
    elevOverlay = elevOverlay,
)
