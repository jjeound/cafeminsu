package com.ssafy.cafeminsu.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface CafeMinsuRoute : NavKey {
    @Serializable
    data object SignIn : CafeMinsuRoute

    @Serializable
    data object Home : CafeMinsuRoute

    @Serializable
    data object Store : CafeMinsuRoute

    @Serializable
    data object Menu : CafeMinsuRoute

    @Serializable
    data object My : CafeMinsuRoute

    @Serializable
    data object Notification : CafeMinsuRoute

    @Serializable
    data object Coupon : CafeMinsuRoute

    @Serializable
    data object Gift : CafeMinsuRoute

    @Serializable
    data object History : CafeMinsuRoute

    @Serializable
    data object Payment : CafeMinsuRoute

    @Serializable
    data object Signup : CafeMinsuRoute

    @Serializable
    data object Stamp : CafeMinsuRoute

    @Serializable
    data object Voice : CafeMinsuRoute

    @Serializable
    data object OwnerLogin : CafeMinsuRoute

    @Serializable
    data object OwnerHome : CafeMinsuRoute

    @Serializable
    data object OwnerOrders : CafeMinsuRoute

    @Serializable
    data object OwnerMenu : CafeMinsuRoute

    @Serializable
    data object OwnerSales : CafeMinsuRoute
}
