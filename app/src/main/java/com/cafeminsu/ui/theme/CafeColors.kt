package com.cafeminsu.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class CafeColors(
    val canvas: Color,
    val ink: Color,
    val body: Color,
    val muted: Color,
    val mutedSoft: Color,
    val primary: Color,
    val surfaceCard: Color,
    val surfaceDark: Color,
    val hairline: Color,
    val primaryHover: Color,
    val accentSoft: Color,
    val onPrimary: Color,
    val onDark: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
)

val LocalCafeColors = staticCompositionLocalOf<CafeColors> {
    kotlin.error("CafeColors is not provided")
}

fun cafeLightColors() = CafeColors(
    canvas = canvas,
    ink = ink,
    body = body,
    muted = muted,
    mutedSoft = mutedSoft,
    primary = primary,
    surfaceCard = surfaceCard,
    surfaceDark = surfaceDark,
    hairline = hairline,
    primaryHover = primaryHover,
    accentSoft = accentSoft,
    onPrimary = onPrimary,
    onDark = onDark,
    success = success,
    warning = warning,
    error = error,
)
