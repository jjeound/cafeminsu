package com.ssafy.cafeminsu.ui

import com.ssafy.cafeminsu.navigation.CafeMinsuRoute

enum class TopLevelDestination(
    val route: CafeMinsuRoute,
    val label: String,
    val title: String,
) {
    Home(CafeMinsuRoute.Home, "홈", "홈"),
    Menu(CafeMinsuRoute.Store, "주문", "주문"),
    My(CafeMinsuRoute.My, "마이", "마이"),
}
