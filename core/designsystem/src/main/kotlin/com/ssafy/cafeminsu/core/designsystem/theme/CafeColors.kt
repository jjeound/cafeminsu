package com.ssafy.cafeminsu.core.designsystem.theme

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
    val kakaoYellow: Color,
)

val LocalCafeColors = staticCompositionLocalOf<CafeColors> {
    error("CafeColors is not provided")
}

fun cafeLightColors() = CafeColors(
    canvas = Canvas,
    ink = Ink,
    body = Body,
    muted = Muted,
    mutedSoft = MutedSoft,
    primary = Primary,
    surfaceCard = SurfaceCard,
    surfaceDark = SurfaceDark,
    hairline = Hairline,
    primaryHover = PrimaryHover,
    accentSoft = AccentSoft,
    onPrimary = OnPrimary,
    onDark = OnDark,
    success = Success,
    warning = Warning,
    error = Error,
    kakaoYellow = KakaoYellow,
)
