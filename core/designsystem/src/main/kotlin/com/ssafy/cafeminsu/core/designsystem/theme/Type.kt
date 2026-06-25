package com.ssafy.cafeminsu.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

data class CafeTypography(
    val display: TextStyle,
    val h1: TextStyle,
    val h2: TextStyle,
    val h3: TextStyle,
    val bodyL: TextStyle,
    val body: TextStyle,
    val caption: TextStyle,
    val meta: TextStyle,
)

val LocalCafeTypography = staticCompositionLocalOf<CafeTypography> {
    error("CafeTypography is not provided")
}

private val CafeFontFamily = FontFamily.SansSerif

fun cafeTypography() = CafeTypography(
    display = TextStyle(fontFamily = CafeFontFamily, fontWeight = FontWeight.Black, fontSize = 32.sp, lineHeight = 40.sp, color = Ink),
    h1 = TextStyle(fontFamily = CafeFontFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp, color = Ink),
    h2 = TextStyle(fontFamily = CafeFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp, color = Ink),
    h3 = TextStyle(fontFamily = CafeFontFamily, fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 24.sp, color = Ink),
    bodyL = TextStyle(fontFamily = CafeFontFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, color = Body),
    body = TextStyle(fontFamily = CafeFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, color = Body),
    caption = TextStyle(fontFamily = CafeFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, color = Muted),
    meta = TextStyle(fontFamily = CafeFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, color = MutedSoft),
)

internal fun cafeMaterialTypography(typography: CafeTypography = cafeTypography()) = Typography(
    displayLarge = typography.display,
    displayMedium = typography.display,
    displaySmall = typography.h1,
    headlineLarge = typography.h1,
    headlineMedium = typography.h2,
    headlineSmall = typography.h3,
    titleLarge = typography.h2,
    titleMedium = typography.h3,
    titleSmall = typography.bodyL,
    bodyLarge = typography.bodyL,
    bodyMedium = typography.body,
    bodySmall = typography.caption,
    labelLarge = typography.bodyL,
    labelMedium = typography.caption,
    labelSmall = typography.meta,
)
