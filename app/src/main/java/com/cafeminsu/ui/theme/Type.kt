package com.cafeminsu.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.Typography
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
    kotlin.error("CafeTypography is not provided")
}

private val cafeFontFamily = FontFamily.SansSerif

fun cafeTypography() = CafeTypography(
    display = TextStyle(
        fontFamily = cafeFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        color = ink,
    ),
    h1 = TextStyle(
        fontFamily = cafeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        color = ink,
    ),
    h2 = TextStyle(
        fontFamily = cafeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        color = ink,
    ),
    h3 = TextStyle(
        fontFamily = cafeFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        color = ink,
    ),
    bodyL = TextStyle(
        fontFamily = cafeFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = body,
    ),
    body = TextStyle(
        fontFamily = cafeFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = body,
    ),
    caption = TextStyle(
        fontFamily = cafeFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = muted,
    ),
    meta = TextStyle(
        fontFamily = cafeFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = mutedSoft,
    ),
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
