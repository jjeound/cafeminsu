package com.ssafy.cafeminsu.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface CafeMinsuRoute : NavKey {
    @Serializable
    data object SignIn : CafeMinsuRoute

    @Serializable
    data object Home : CafeMinsuRoute
}
