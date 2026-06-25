package com.ssafy.cafeminsu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.ssafy.cafeminsu.MainActivityAuthState
import com.ssafy.cafeminsu.MainActivityViewModel
import com.ssafy.cafeminsu.feature.auth.SignInRoute
import com.ssafy.cafeminsu.feature.home.HomeRoute
import com.ssafy.cafeminsu.feature.menu.MenuRoute
import com.ssafy.cafeminsu.feature.my.MyRoute
import com.ssafy.cafeminsu.feature.store.StoreRoute
import com.ssafy.cafeminsu.navigation.CafeMinsuRoute

@Composable
fun CafeMinsuApp(
    viewModel: MainActivityViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val destination = when (uiState.authState) {
        MainActivityAuthState.Loading,
        MainActivityAuthState.SignedOut,
        -> CafeMinsuRoute.SignIn
        MainActivityAuthState.SignedIn -> CafeMinsuRoute.Home
    }
    val backStack = rememberNavBackStack(destination)

    LaunchedEffect(destination) {
        if (backStack.lastOrNull() != destination) {
            backStack.clear()
            backStack += destination
        }
    }

    NavDisplay(
        backStack = backStack,
        entryProvider = entryProvider {
            entry<CafeMinsuRoute.SignIn> {
                SignInRoute()
            }
            entry<CafeMinsuRoute.Home> {
                HomeRoute(
                    onStoreClick = { backStack.clear(); backStack += CafeMinsuRoute.Store },
                    onMenuClick = { backStack.clear(); backStack += CafeMinsuRoute.Menu },
                    onMyClick = { backStack.clear(); backStack += CafeMinsuRoute.My },
                )
            }
            entry<CafeMinsuRoute.Store> {
                StoreRoute()
            }
            entry<CafeMinsuRoute.Menu> {
                MenuRoute()
            }
            entry<CafeMinsuRoute.My> {
                MyRoute()
            }
        },
    )
}
