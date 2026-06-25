package com.ssafy.cafeminsu.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

data class CafeShapes(
    val radiusSm: RoundedCornerShape = RoundedCornerShape(8.dp),
    val radiusMd: RoundedCornerShape = RoundedCornerShape(12.dp),
    val radiusLg: RoundedCornerShape = RoundedCornerShape(16.dp),
    val radiusXl: RoundedCornerShape = RoundedCornerShape(24.dp),
    val radiusPill: RoundedCornerShape = RoundedCornerShape(999.dp),
)

val LocalCafeShapes = staticCompositionLocalOf { CafeShapes() }

internal fun cafeMaterialShapes(shapes: CafeShapes = CafeShapes()) = Shapes(
    extraSmall = shapes.radiusSm,
    small = shapes.radiusMd,
    medium = shapes.radiusLg,
    large = shapes.radiusXl,
    extraLarge = shapes.radiusXl,
)
