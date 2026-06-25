package com.ssafy.cafeminsu.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.cafeminsu.BuildConfig
import com.ssafy.cafeminsu.MainActivityAuthState
import com.ssafy.cafeminsu.MainActivityViewModel
import com.ssafy.cafeminsu.core.designsystem.theme.CafeMinsuTheme
import com.ssafy.cafeminsu.feature.auth.SignInRoute
import com.ssafy.cafeminsu.feature.coupon.CouponRoute
import com.ssafy.cafeminsu.feature.gift.GiftRoute
import com.ssafy.cafeminsu.feature.history.HistoryRoute
import com.ssafy.cafeminsu.feature.home.HomeRoute
import com.ssafy.cafeminsu.feature.menu.MenuRoute
import com.ssafy.cafeminsu.feature.my.MyRoute
import com.ssafy.cafeminsu.feature.notification.NotificationRoute
import com.ssafy.cafeminsu.feature.owner.home.OwnerHomeRoute
import com.ssafy.cafeminsu.feature.owner.login.OwnerLoginRoute
import com.ssafy.cafeminsu.feature.owner.menu.OwnerMenuRoute
import com.ssafy.cafeminsu.feature.owner.orders.OwnerOrdersRoute
import com.ssafy.cafeminsu.feature.owner.sales.OwnerSalesRoute
import com.ssafy.cafeminsu.feature.payment.PaymentRoute
import com.ssafy.cafeminsu.feature.signup.SignupRoute
import com.ssafy.cafeminsu.feature.stamp.StampRoute
import com.ssafy.cafeminsu.feature.store.StoreRoute
import com.ssafy.cafeminsu.feature.voice.VoiceRoute
import com.ssafy.cafeminsu.navigation.CafeMinsuRoute

private val topLevelRoutes = setOf(
    CafeMinsuRoute.Home,
    CafeMinsuRoute.Store,
    CafeMinsuRoute.My,
)

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
    val currentRoute = backStack.lastOrNull() as? CafeMinsuRoute
    val showTopBar = backStack.size > 1 && currentRoute !in topLevelRoutes && currentRoute != CafeMinsuRoute.Menu
    val showBottomBar = currentRoute in topLevelRoutes || currentRoute == CafeMinsuRoute.Menu

    LaunchedEffect(destination) {
        if (backStack.lastOrNull() != destination) {
            backStack.clear()
            backStack += destination
        }
    }

    BackHandler(enabled = backStack.size > 1) {
        backStack.removeLast()
    }

    Scaffold(
        topBar = {
            if (showTopBar) {
                AppTopBar(
                    title = currentRoute.title(),
                    onBackClick = { backStack.removeLastOrNull() },
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(
                    currentRoute = currentRoute,
                    onDestinationClick = { route ->
                        backStack.clear()
                        backStack += route
                    },
                )
            }
        },
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            entryProvider = entryProvider {
                entry<CafeMinsuRoute.SignIn> {
                    SignInRoute(
                        onOwnerLoginClick = { backStack += CafeMinsuRoute.OwnerLogin },
                    )
                }
                entry<CafeMinsuRoute.Home> {
                    HomeRoute(
                        onNotificationClick = { backStack += CafeMinsuRoute.Notification },
                        onBrowseMenuClick = { backStack += CafeMinsuRoute.Store },
                        onStoreClick = { backStack += CafeMinsuRoute.Store },
                        onMenuClick = { backStack += CafeMinsuRoute.Menu },
                        onMyClick = { backStack += CafeMinsuRoute.My },
                    )
                }
                entry<CafeMinsuRoute.Store> {
                    StoreRoute(
                        onNavigateToMenu = { backStack += CafeMinsuRoute.Menu },
                        kakaoNativeAppKey = BuildConfig.KAKAO_NATIVE_APP_KEY,
                    )
                }
                entry<CafeMinsuRoute.Menu> {
                    MenuRoute(
                        onBackClick = { backStack.removeLastOrNull() },
                        onCartClick = { backStack += CafeMinsuRoute.Voice },
                    )
                }
                entry<CafeMinsuRoute.My> {
                    MyRoute(
                        onHistoryClick = { backStack += CafeMinsuRoute.History },
                        onGiftClick = { backStack += CafeMinsuRoute.Gift },
                        onCouponClick = { backStack += CafeMinsuRoute.Coupon },
                        onTermsClick = {},
                        onFaqClick = {},
                        onSupportClick = {},
                        onVersionClick = {},
                        onLogoutClick = {
                            backStack.clear()
                            backStack += CafeMinsuRoute.SignIn
                        },
                    )
                }
                entry<CafeMinsuRoute.Notification> { NotificationRoute() }
                entry<CafeMinsuRoute.Coupon> { CouponRoute() }
                entry<CafeMinsuRoute.Gift> { GiftRoute() }
                entry<CafeMinsuRoute.History> { HistoryRoute() }
                entry<CafeMinsuRoute.Payment> { PaymentRoute() }
                entry<CafeMinsuRoute.Signup> { SignupRoute() }
                entry<CafeMinsuRoute.Stamp> { StampRoute() }
                entry<CafeMinsuRoute.Voice> { VoiceRoute() }
                entry<CafeMinsuRoute.OwnerLogin> { OwnerLoginRoute(modifier = Modifier) }
                entry<CafeMinsuRoute.OwnerHome> { OwnerHomeRoute() }
                entry<CafeMinsuRoute.OwnerOrders> { OwnerOrdersRoute() }
                entry<CafeMinsuRoute.OwnerMenu> { OwnerMenuRoute() }
                entry<CafeMinsuRoute.OwnerSales> { OwnerSalesRoute() }
            },
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AppTopBar(
    title: String,
    onBackClick: () -> Unit,
) {
    val colors = CafeMinsuTheme.colors

    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colors.canvas,
            titleContentColor = colors.ink,
            navigationIconContentColor = colors.ink,
        ),
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AppBottomBar(
    currentRoute: CafeMinsuRoute?,
    onDestinationClick: (CafeMinsuRoute) -> Unit,
) {
    val colors = CafeMinsuTheme.colors

    NavigationBar(containerColor = colors.canvas) {
        TopLevelDestination.entries.forEach { destination ->
            val selected = currentRoute == destination.route
            NavigationBarItem(
                selected = selected,
                onClick = { onDestinationClick(destination.route) },
                icon = {
                    Icon(
                        imageVector = when (destination) {
                            TopLevelDestination.Home -> Icons.Filled.Home
                            TopLevelDestination.Menu -> Icons.Filled.ShoppingCart
                            TopLevelDestination.My -> Icons.Filled.Person
                        },
                        contentDescription = destination.label,
                    )
                },
                label = { Text(text = destination.label) },
            )
        }
    }
}

private fun CafeMinsuRoute?.title(): String =
    when (this) {
        CafeMinsuRoute.SignIn -> "로그인"
        CafeMinsuRoute.Store -> "매장 찾기"
        CafeMinsuRoute.Menu -> "주문"
        CafeMinsuRoute.My -> "마이"
        CafeMinsuRoute.Notification -> "알림"
        CafeMinsuRoute.Coupon -> "쿠폰"
        CafeMinsuRoute.Gift -> "선물"
        CafeMinsuRoute.History -> "주문 내역"
        CafeMinsuRoute.Payment -> "결제"
        CafeMinsuRoute.Signup -> "회원가입"
        CafeMinsuRoute.Stamp -> "스탬프"
        CafeMinsuRoute.Voice -> "음성 주문"
        CafeMinsuRoute.OwnerLogin -> "사장님 로그인"
        CafeMinsuRoute.OwnerHome -> "사장님 홈"
        CafeMinsuRoute.OwnerOrders -> "주문 관리"
        CafeMinsuRoute.OwnerMenu -> "메뉴 관리"
        CafeMinsuRoute.OwnerSales -> "매출 관리"
        null -> ""
        CafeMinsuRoute.Home -> ""
    }
