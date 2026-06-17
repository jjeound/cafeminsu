package com.cafeminsu.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val space1 = 4.dp
val space2 = 8.dp
val space3 = 12.dp
val space4 = 16.dp
val space5 = 20.dp
val space6 = 24.dp
val space8 = 32.dp
val space10 = 40.dp
val space14 = 56.dp
val space18 = 72.dp

data class CafeSpacing(
    val space1: Dp,
    val space2: Dp,
    val space3: Dp,
    val space4: Dp,
    val space5: Dp,
    val space6: Dp,
    val space8: Dp,
    val space10: Dp,
    val space14: Dp,
    val space18: Dp,
)

val LocalCafeSpacing = staticCompositionLocalOf<CafeSpacing> {
    kotlin.error("CafeSpacing is not provided")
}

fun cafeSpacing() = CafeSpacing(
    space1 = space1,
    space2 = space2,
    space3 = space3,
    space4 = space4,
    space5 = space5,
    space6 = space6,
    space8 = space8,
    space10 = space10,
    space14 = space14,
    space18 = space18,
)
