package com.cafeminsu.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cafeminsu.R
import com.cafeminsu.core.AppResult
import com.cafeminsu.domain.auth.OwnerAuthProvider
import com.cafeminsu.domain.model.AuthState
import com.cafeminsu.domain.model.UserRole
import com.cafeminsu.domain.repository.SessionRepository
import com.cafeminsu.ui.feature.cart.CartRoute
import com.cafeminsu.ui.feature.coupon.CouponRoute
import com.cafeminsu.ui.feature.gift.GiftRoute
import com.cafeminsu.ui.feature.gifticon.GifticonDetailRoute
import com.cafeminsu.ui.feature.history.HistoryRoute
import com.cafeminsu.ui.feature.home.HomeRoute
import com.cafeminsu.ui.feature.login.LoginRoute
import com.cafeminsu.ui.feature.menu.MenuDetailRoute
import com.cafeminsu.ui.feature.menu.MenuRoute
import com.cafeminsu.ui.feature.my.MyRoute
import com.cafeminsu.ui.feature.notification.NotiRoute
import com.cafeminsu.ui.feature.order.OrderFailureDialog
import com.cafeminsu.ui.feature.order.OrderResultRoute
import com.cafeminsu.ui.feature.owner.home.OwnerHomeRoute
import com.cafeminsu.ui.feature.owner.login.OwnerLoginRoute
import com.cafeminsu.ui.feature.owner.menu.OwnerMenuRoute
import com.cafeminsu.ui.feature.owner.orders.OwnerOrdersRoute
import com.cafeminsu.ui.feature.owner.sales.OwnerSalesRoute
import com.cafeminsu.ui.feature.payment.PaymentFailureReason
import com.cafeminsu.ui.feature.payment.PaymentRoute
import com.cafeminsu.ui.feature.payment.paymentFailureUiModel
import com.cafeminsu.ui.feature.signup.SignupRoute
import com.cafeminsu.ui.feature.splash.SplashScreen
import com.cafeminsu.ui.feature.store.StoreRoute
import com.cafeminsu.ui.feature.voice.VoiceRoute
import com.cafeminsu.ui.theme.CafeTheme
import kotlinx.coroutines.delay

@Composable
fun AppNavHost(
    sessionRepository: SessionRepository,
    ownerAuthProvider: OwnerAuthProvider,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    splashDelayMillis: Long = SplashGateDelayMillis,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = modifier,
        containerColor = CafeTheme.colors.canvas,
        bottomBar = {
            if (shouldShowOwnerBottomBar(currentRoute)) {
                OwnerBottomBar(
                    currentRoute = currentRoute,
                    onTabSelected = { route ->
                        navController.navigate(route) {
                            popUpTo(Routes.OWNER_HOME) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            } else if (shouldShowBottomBar(currentRoute)) {
                CafeBottomBar(
                    currentRoute = currentRoute,
                    onTabSelected = { route ->
                        navController.navigate(route) {
                            popUpTo(Routes.HOME) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            composable(Routes.SPLASH) {
                SplashGate(
                    sessionRepository = sessionRepository,
                    splashDelayMillis = splashDelayMillis,
                    onAuthenticated = { authState ->
                        val destination = if (authState.role == UserRole.Owner) {
                            Routes.OWNER_HOME
                        } else {
                            Routes.HOME
                        }
                        navController.navigate(destination) {
                            popUpTo(Routes.SPLASH) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                    onUnauthenticated = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.SPLASH) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.LOGIN) {
                LoginRoute(
                    sessionRepository = sessionRepository,
                    onLoginSuccess = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                    onNewUser = {
                        navController.navigate(Routes.SIGNUP) {
                            popUpTo(Routes.LOGIN) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                    onOwnerLoginClick = { navController.navigate(Routes.OWNER_LOGIN) },
                )
            }
            composable(Routes.SIGNUP) {
                SignupRoute(
                    sessionRepository = sessionRepository,
                    onSignupComplete = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.SIGNUP) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                    onBack = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.SIGNUP) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.OWNER_LOGIN) {
                OwnerLoginRoute(
                    ownerAuthProvider = ownerAuthProvider,
                    onBackClick = { navController.popBackStack() },
                    onLoginSuccess = {
                        navController.navigate(Routes.OWNER_HOME) {
                            popUpTo(Routes.OWNER_LOGIN) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.OWNER_HOME) {
                OwnerHomeRoute(
                    onViewAllOrders = { navController.navigate(Routes.OWNER_ORDERS) },
                )
            }
            composable(Routes.OWNER_ORDERS) {
                OwnerOrdersRoute()
            }
            composable(Routes.OWNER_MENU) {
                OwnerMenuRoute()
            }
            composable(Routes.OWNER_SALES) {
                OwnerSalesRoute()
            }
            composable(Routes.HOME) {
                HomeRoute(
                    onRecommendedOrderClick = { menuItemId ->
                        navController.navigate(Routes.menuDetail(menuItemId))
                    },
                    onCouponClick = { navController.navigate(Routes.COUPON) },
                    onNotificationClick = { navController.navigate(Routes.NOTI) },
                    onRecentOrdersClick = { navController.navigate(Routes.HISTORY) },
                    onReorderClick = { menuItemId ->
                        navController.navigate(Routes.menuDetail(menuItemId))
                    },
                    onBrowseMenuClick = { navController.navigate(Routes.STORE) },
                )
            }
            composable(Routes.NOTI) {
                NotiRoute(
                    onBackClick = { navController.popBackStack() },
                )
            }
            composable(Routes.STORE) {
                StoreRoute(
                    onNavigateToMenu = { navController.navigate(Routes.MENU) },
                )
            }
            composable(Routes.STORE_DETAIL) {
                PlaceholderScreen(title = "매장 상세")
            }
            composable(Routes.MENU) {
                MenuRoute(
                    onMenuClick = { menuItemId ->
                        navController.navigate(Routes.menuDetail(menuItemId))
                    },
                    onVoiceClick = { navController.navigate(Routes.VOICE) },
                )
            }
            composable(
                route = Routes.MENU_DETAIL,
                arguments = listOf(
                    navArgument(Routes.MENU_DETAIL_MENU_ID) {
                        type = NavType.StringType
                    },
                ),
            ) {
                MenuDetailRoute(
                    onAddedToCart = { navController.navigate(Routes.CART) },
                    onBackClick = { navController.popBackStack() },
                )
            }
            composable(Routes.VOICE) {
                VoiceRoute(
                    onNavigateToCart = { navController.navigate(Routes.CART) },
                    onNavigateToMenu = { navController.navigate(Routes.MENU) },
                )
            }
            composable(Routes.CART) {
                CartRoute(
                    onPaymentRequested = { orderId ->
                        navController.navigate(Routes.pay(orderId))
                    },
                    onBrowseMenuClick = { navController.navigate(Routes.STORE) },
                    onBackClick = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.PAY,
                arguments = listOf(
                    navArgument(Routes.PAY_ORDER_ID) {
                        type = NavType.StringType
                    },
                ),
            ) {
                PaymentRoute(
                    onPaymentApproved = { orderId ->
                        navController.navigate(Routes.orderOk(orderId)) {
                            launchSingleTop = true
                        }
                    },
                    onBackClick = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.ORDER_OK,
                arguments = listOf(
                    navArgument(Routes.ORDER_OK_ORDER_ID) {
                        type = NavType.StringType
                    },
                ),
            ) {
                OrderResultRoute(
                    onCloseClick = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME)
                            launchSingleTop = true
                        }
                    },
                    onStatusClick = { orderId ->
                        navController.navigate(Routes.history(orderId)) {
                            launchSingleTop = true
                        }
                    },
                    onHomeClick = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME)
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.ORDER_FAIL) {
                OrderFailureDialog(
                    failure = paymentFailureUiModel(PaymentFailureReason.LimitExceeded),
                    onCancel = { navController.popBackStack() },
                    onRetry = { navController.popBackStack() },
                )
            }
            composable(Routes.MY) {
                MyRoute(
                    onHistoryClick = { navController.navigate(Routes.HISTORY) },
                    onGiftClick = { navController.navigate(Routes.GIFT) },
                    onCouponClick = { navController.navigate(Routes.COUPON) },
                    onLoginClick = { navController.navigate(Routes.LOGIN) },
                )
            }
            composable(Routes.COUPON) {
                CouponRoute(
                    onBackClick = { navController.popBackStack() },
                    onLoginClick = { navController.navigate(Routes.LOGIN) },
                )
            }
            composable(Routes.GIFT) {
                GiftRoute(
                    onBackClick = { navController.popBackStack() },
                    onLoginClick = { navController.navigate(Routes.LOGIN) },
                )
            }
            composable(
                route = Routes.GIFTICON_DETAIL,
                arguments = listOf(
                    navArgument(Routes.GIFTICON_ID) {
                        type = NavType.StringType
                    },
                ),
            ) {
                GifticonDetailRoute(
                    onLoginClick = { navController.navigate(Routes.LOGIN) },
                )
            }
            composable(Routes.HISTORY) {
                HistoryRoute(
                    onBackClick = { navController.popBackStack() },
                    onReorderClick = { menuItemId ->
                        navController.navigate(Routes.menuDetail(menuItemId))
                    },
                )
            }
            composable(
                route = Routes.HISTORY_DETAIL,
                arguments = listOf(
                    navArgument(Routes.HISTORY_ORDER_ID) {
                        type = NavType.StringType
                    },
                ),
            ) {
                HistoryRoute(
                    onBackClick = { navController.popBackStack() },
                    onReorderClick = { menuItemId ->
                        navController.navigate(Routes.menuDetail(menuItemId))
                    },
                )
            }
        }
    }
}

@Composable
private fun SplashGate(
    sessionRepository: SessionRepository,
    splashDelayMillis: Long,
    onAuthenticated: (AuthState.Authenticated) -> Unit,
    onUnauthenticated: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val authStateFlow = remember(sessionRepository) { sessionRepository.observeAuthState() }
    val authState by authStateFlow.collectAsState(initial = AuthState.Unknown)

    LaunchedEffect(authState, sessionRepository, splashDelayMillis) {
        delay(splashDelayMillis)
        val resolvedState = if (authState == AuthState.Unknown) {
            when (val result = sessionRepository.refreshOnce()) {
                is AppResult.Success -> result.data
                is AppResult.Failure -> AuthState.Guest
            }
        } else {
            authState
        }

        when (resolvedState) {
            is AuthState.Authenticated -> onAuthenticated(resolvedState)
            AuthState.Guest,
            AuthState.Expired,
            -> onUnauthenticated()

            AuthState.Unknown -> Unit
        }
    }

    SplashScreen(modifier = modifier)
}

@Composable
private fun PlaceholderScreen(
    title: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = CafeTheme.colors.canvas,
        contentColor = CafeTheme.colors.ink,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(CafeTheme.spacing.space5),
            contentAlignment = Alignment.TopStart,
        ) {
            Text(
                text = title,
                style = CafeTheme.typography.h1,
                color = CafeTheme.colors.ink,
            )
        }
    }
}

private data class BottomTab(
    val route: String,
    val label: String,
    @DrawableRes val icon: Int? = null,
)

private val bottomTabs = listOf(
    BottomTab(Routes.HOME, "홈", R.drawable.ic_home),
    BottomTab(Routes.STORE, "주문", R.drawable.ic_receipt),
    BottomTab(Routes.MY, "MY", R.drawable.ic_person),
)

private val ownerBottomTabs = listOf(
    BottomTab(Routes.OWNER_HOME, "대시보드"),
    BottomTab(Routes.OWNER_ORDERS, "주문"),
    BottomTab(Routes.OWNER_MENU, "메뉴"),
    BottomTab(Routes.OWNER_SALES, "매출"),
)

private val orderTabRoutes = setOf(
    Routes.STORE,
    Routes.MENU,
)

private fun shouldShowBottomBar(currentRoute: String?): Boolean =
    selectedTabRoute(currentRoute) != null

private fun shouldShowOwnerBottomBar(currentRoute: String?): Boolean =
    selectedOwnerTabRoute(currentRoute) != null

private fun selectedTabRoute(currentRoute: String?): String? =
    when {
        currentRoute == Routes.HOME -> Routes.HOME
        orderTabRoutes.contains(currentRoute) -> Routes.STORE
        currentRoute == Routes.MY -> Routes.MY
        else -> null
    }

private fun selectedOwnerTabRoute(currentRoute: String?): String? =
    when (currentRoute) {
        Routes.OWNER_HOME -> Routes.OWNER_HOME
        Routes.OWNER_ORDERS -> Routes.OWNER_ORDERS
        Routes.OWNER_MENU -> Routes.OWNER_MENU
        Routes.OWNER_SALES -> Routes.OWNER_SALES
        else -> null
    }

@Composable
private fun CafeBottomBar(
    currentRoute: String?,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing
    val selectedRoute = selectedTabRoute(currentRoute)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(spacing.space18)
            .background(colors.canvas)
            .drawWithContent {
                drawContent()
                drawLine(
                    color = colors.hairline,
                    start = Offset.Zero,
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            },
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            bottomTabs.forEach { tab ->
                val selected = selectedRoute == tab.route

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            onClick = { onTabSelected(tab.route) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        tab.icon?.let { iconRes ->
                            Icon(
                                painter = painterResource(iconRes),
                                contentDescription = null,
                                tint = if (selected) colors.primary else colors.muted,
                                modifier = Modifier.size(spacing.space6),
                            )
                            Spacer(modifier = Modifier.height(spacing.space1))
                        }
                        Text(
                            text = tab.label,
                            style = CafeTheme.typography.caption,
                            color = if (selected) colors.primary else colors.muted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnerBottomBar(
    currentRoute: String?,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing
    val selectedRoute = selectedOwnerTabRoute(currentRoute)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(spacing.space18)
            .background(colors.canvas)
            .drawWithContent {
                drawContent()
                drawLine(
                    color = colors.hairline,
                    start = Offset.Zero,
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            },
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            ownerBottomTabs.forEach { tab ->
                val selected = selectedRoute == tab.route

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            onClick = { onTabSelected(tab.route) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .size(spacing.space1)
                                    .background(
                                        color = colors.primary,
                                        shape = CafeTheme.shapes.radiusPill,
                                    ),
                            )
                        } else {
                            Spacer(modifier = Modifier.size(spacing.space1))
                        }
                        Spacer(modifier = Modifier.height(spacing.space1))
                        Text(
                            text = tab.label,
                            style = CafeTheme.typography.caption,
                            color = if (selected) colors.primary else colors.muted,
                        )
                    }
                }
            }
        }
    }
}

private const val SplashGateDelayMillis = 1_200L
