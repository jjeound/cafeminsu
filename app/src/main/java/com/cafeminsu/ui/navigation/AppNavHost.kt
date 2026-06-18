package com.cafeminsu.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cafeminsu.ui.feature.cart.CartRoute
import com.cafeminsu.ui.feature.gifticon.GifticonScreen
import com.cafeminsu.ui.feature.home.HomeRoute
import com.cafeminsu.ui.feature.menu.MenuDetailRoute
import com.cafeminsu.ui.feature.menu.MenuRoute
import com.cafeminsu.ui.feature.my.MyScreen
import com.cafeminsu.ui.feature.order.OrderStatusRoute
import com.cafeminsu.ui.feature.payment.PaymentRoute
import com.cafeminsu.ui.feature.stamp.StampScreen
import com.cafeminsu.ui.feature.voice.VoiceScreen
import com.cafeminsu.ui.theme.CafeTheme

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = modifier,
        containerColor = CafeTheme.colors.canvas,
        bottomBar = {
            if (currentRoute != Routes.VOICE) {
                CafeBottomBar(
                    currentRoute = currentRoute,
                    onTabSelected = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
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
            startDestination = Routes.HOME,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            composable(Routes.HOME) {
                HomeRoute(
                    onMenuClick = { menuItemId ->
                        navController.navigate(Routes.menuDetail(menuItemId))
                    },
                    onBrowseMenuClick = { navController.navigate(Routes.MENU) },
                )
            }
            composable(Routes.MENU) {
                MenuRoute(
                    onMenuClick = { menuItemId ->
                        navController.navigate(Routes.menuDetail(menuItemId))
                    },
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
                )
            }
            composable(Routes.VOICE) { VoiceScreen() }
            composable(Routes.CART) {
                CartRoute(
                    onPaymentRequested = { orderId ->
                        navController.navigate(Routes.payment(orderId))
                    },
                    onBrowseMenuClick = { navController.navigate(Routes.MENU) },
                )
            }
            composable(
                route = Routes.PAYMENT,
                arguments = listOf(
                    navArgument(Routes.PAYMENT_ORDER_ID) {
                        type = NavType.StringType
                    },
                ),
            ) {
                PaymentRoute(
                    onPaymentApproved = { orderId ->
                        navController.navigate(Routes.orderStatus(orderId)) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = false
                            }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(
                route = Routes.ORDER_STATUS,
                arguments = listOf(
                    navArgument(Routes.ORDER_STATUS_ORDER_ID) {
                        type = NavType.StringType
                    },
                ),
            ) {
                OrderStatusRoute()
            }
            composable(Routes.STAMP) { StampScreen() }
            composable(Routes.GIFTICON) { GifticonScreen() }
            composable(Routes.MY) { MyScreen() }
        }
    }
}

private data class BottomTab(
    val route: String,
    val label: String,
)

private val bottomTabs = listOf(
    BottomTab(Routes.HOME, "홈"),
    BottomTab(Routes.MENU, "메뉴"),
    BottomTab(Routes.STAMP, "스탬프"),
    BottomTab(Routes.MY, "마이"),
)

@Composable
private fun CafeBottomBar(
    currentRoute: String?,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

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
                val selected = currentRoute == tab.route

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
