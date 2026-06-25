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
import com.ssafy.cafeminsu.feature.notification.NotificationRoute
import com.ssafy.cafeminsu.feature.coupon.CouponRoute
import com.ssafy.cafeminsu.feature.gift.GiftRoute
import com.ssafy.cafeminsu.feature.history.HistoryRoute
import com.ssafy.cafeminsu.feature.payment.PaymentRoute
import com.ssafy.cafeminsu.feature.signup.SignupRoute
import com.ssafy.cafeminsu.feature.stamp.StampRoute
import com.ssafy.cafeminsu.feature.voice.VoiceRoute
import com.ssafy.cafeminsu.feature.store.StoreRoute
import com.ssafy.cafeminsu.feature.owner.home.OwnerHomeRoute
import com.ssafy.cafeminsu.feature.owner.login.OwnerLoginRoute
import com.ssafy.cafeminsu.feature.owner.menu.OwnerMenuRoute
import com.ssafy.cafeminsu.feature.owner.orders.OwnerOrdersRoute
import com.ssafy.cafeminsu.feature.owner.sales.OwnerSalesRoute
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
                SignInRoute(
                    onOwnerLoginClick = {
                        backStack.clear()
                        backStack += CafeMinsuRoute.OwnerLogin
                    },
                )
            }
            entry<CafeMinsuRoute.Home> {
                HomeRoute(
                    onNotificationClick = {
                        backStack.clear()
                        backStack += CafeMinsuRoute.Notification
                    },
                    onBrowseMenuClick = {
                        backStack.clear()
                        backStack += CafeMinsuRoute.Store
                    },
                    onStoreClick = {
                        backStack.clear()
                        backStack += CafeMinsuRoute.Store
                    },
                    onMenuClick = {
                        backStack.clear()
                        backStack += CafeMinsuRoute.Menu
                    },
                    onMyClick = {
                        backStack.clear()
                        backStack += CafeMinsuRoute.My
                    },
                )
            }
            entry<CafeMinsuRoute.Store> {
                StoreRoute(
                    onNavigateToMenu = {
                        backStack.clear()
                        backStack += CafeMinsuRoute.Menu
                    },
                )
            }
            entry<CafeMinsuRoute.Menu> {
                MenuRoute(
                    onCartClick = {
                        backStack.clear()
                        backStack += CafeMinsuRoute.Voice
                    },
                )
            }
            entry<CafeMinsuRoute.My> {
                MyRoute(
                    onHistoryClick = {
                        backStack.clear()
                        backStack += CafeMinsuRoute.History
                    },
                    onGiftClick = {
                        backStack.clear()
                        backStack += CafeMinsuRoute.Gift
                    },
                    onCouponClick = {
                        backStack.clear()
                        backStack += CafeMinsuRoute.Coupon
                    },
                    onLoginClick = {
                        backStack.clear()
                        backStack += CafeMinsuRoute.SignIn
                    },
                )
            }
            entry<CafeMinsuRoute.Notification> {
                NotificationRoute()
            }
            entry<CafeMinsuRoute.Coupon> {
                CouponRoute()
            }
            entry<CafeMinsuRoute.Gift> {
                GiftRoute()
            }
            entry<CafeMinsuRoute.History> {
                HistoryRoute()
            }
            entry<CafeMinsuRoute.Payment> {
                PaymentRoute()
            }
            entry<CafeMinsuRoute.Signup> {
                SignupRoute()
            }
            entry<CafeMinsuRoute.Stamp> {
                StampRoute()
            }
            entry<CafeMinsuRoute.Voice> {
                VoiceRoute()
            }
            entry<CafeMinsuRoute.OwnerLogin> {
                OwnerLoginRoute(
                    modifier = androidx.compose.ui.Modifier,
                )
            }
            entry<CafeMinsuRoute.OwnerHome> {
                OwnerHomeRoute()
            }
            entry<CafeMinsuRoute.OwnerOrders> {
                OwnerOrdersRoute()
            }
            entry<CafeMinsuRoute.OwnerMenu> {
                OwnerMenuRoute()
            }
            entry<CafeMinsuRoute.OwnerSales> {
                OwnerSalesRoute()
            }
        },
    )
}
