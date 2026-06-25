package com.ssafy.cafeminsu.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

@Composable
fun CafeMinsuTheme(content: @Composable () -> Unit) {
    val colors = cafeLightColors()
    val typography = cafeTypography()
    val shapes = CafeShapes()
    val spacing = CafeSpacing()

    CompositionLocalProvider(
        LocalCafeColors provides colors,
        LocalCafeTypography provides typography,
        LocalCafeShapes provides shapes,
        LocalCafeSpacing provides spacing,
    ) {
        MaterialTheme(
            colorScheme = cafeColorScheme(colors),
            typography = cafeMaterialTypography(typography),
            shapes = cafeMaterialShapes(shapes),
            content = content,
        )
    }
}

object CafeMinsuTheme {
    val colors: CafeColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCafeColors.current

    val typography: CafeTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalCafeTypography.current

    val shapes: CafeShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalCafeShapes.current

    val spacing: CafeSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalCafeSpacing.current
}

internal fun cafeColorScheme(colors: CafeColors): ColorScheme = lightColorScheme(
    primary = colors.primary,
    onPrimary = colors.onPrimary,
    primaryContainer = colors.accentSoft,
    onPrimaryContainer = colors.ink,
    inversePrimary = colors.primaryHover,
    secondary = colors.surfaceDark,
    onSecondary = colors.onDark,
    secondaryContainer = colors.surfaceCard,
    onSecondaryContainer = colors.ink,
    tertiary = colors.success,
    onTertiary = colors.onPrimary,
    tertiaryContainer = colors.accentSoft,
    onTertiaryContainer = colors.ink,
    background = colors.canvas,
    onBackground = colors.ink,
    surface = colors.surfaceCard,
    onSurface = colors.ink,
    surfaceVariant = colors.canvas,
    onSurfaceVariant = colors.body,
    surfaceTint = colors.primary,
    inverseSurface = colors.surfaceDark,
    inverseOnSurface = colors.onDark,
    error = colors.error,
    onError = colors.onPrimary,
    errorContainer = colors.surfaceCard,
    onErrorContainer = colors.error,
    outline = colors.hairline,
    outlineVariant = colors.hairline,
    scrim = colors.ink,
    surfaceBright = colors.canvas,
    surfaceContainer = colors.surfaceCard,
    surfaceContainerHigh = colors.surfaceCard,
    surfaceContainerHighest = colors.surfaceCard,
    surfaceContainerLow = colors.canvas,
    surfaceContainerLowest = colors.canvas,
    surfaceDim = colors.hairline,
)
