package com.cafeminsu.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

val radiusSm = RoundedCornerShape(8.dp)
val radiusMd = RoundedCornerShape(12.dp)
val radiusLg = RoundedCornerShape(16.dp)
val radiusXl = RoundedCornerShape(24.dp)
val radiusPill = RoundedCornerShape(999.dp)

data class CafeShapes(
    val radiusSm: RoundedCornerShape,
    val radiusMd: RoundedCornerShape,
    val radiusLg: RoundedCornerShape,
    val radiusXl: RoundedCornerShape,
    val radiusPill: RoundedCornerShape,
)

val LocalCafeShapes = staticCompositionLocalOf<CafeShapes> {
    kotlin.error("CafeShapes is not provided")
}

fun cafeShapes() = CafeShapes(
    radiusSm = radiusSm,
    radiusMd = radiusMd,
    radiusLg = radiusLg,
    radiusXl = radiusXl,
    radiusPill = radiusPill,
)

internal fun cafeMaterialShapes(shapes: CafeShapes = cafeShapes()) = Shapes(
    extraSmall = shapes.radiusSm,
    small = shapes.radiusMd,
    medium = shapes.radiusLg,
    large = shapes.radiusXl,
    extraLarge = shapes.radiusXl,
)
