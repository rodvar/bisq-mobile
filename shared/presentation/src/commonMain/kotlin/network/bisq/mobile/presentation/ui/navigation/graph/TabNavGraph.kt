package network.bisq.mobile.presentation.ui.navigation.graph

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavUri
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import network.bisq.mobile.presentation.ui.navigation.ExternalUriHandler
import network.bisq.mobile.presentation.ui.navigation.Graph
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.navigation.Routes.Companion.getDeeplinkUriPattern
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.uicases.DashboardScreen
import network.bisq.mobile.presentation.ui.uicases.offerbook.OfferbookMarketScreen
import network.bisq.mobile.presentation.ui.uicases.open_trades.OpenTradeListScreen
import network.bisq.mobile.presentation.ui.uicases.settings.MiscItemsScreen

@Composable
fun TabNavGraph() {

    val mainPresenter: AppPresenter = koinInject()
    val selectedTab = remember { mutableStateOf(Routes.TabHome.name) }
    val navController = mainPresenter.getRootTabNavController()
    val viewModelStoreOwner = LocalViewModelStoreOwner.current
    DisposableEffect(viewModelStoreOwner) {
        navController.setViewModelStore(viewModelStoreOwner!!.viewModelStore)
        onDispose {}
    }

    // we set this up here because in RootNavGraph tab routes are not registered yet, so we may
    // mishandle them there
    DisposableEffect(Unit) {
        ExternalUriHandler.listener = { uri ->
            val navUri = NavUri(uri)
            try {
                mainPresenter.getRootNavController().navigate(navUri)
            } catch (_: Throwable) {
                // tab routes are defined on another graph, we try them as well
                try {
                    navController.navigate(navUri)
                } catch (_: Throwable) {
                    // we failed to find the route, we just ignore this
                }
            }
        }
        // Removes the listener when the composable is no longer active
        onDispose {
            ExternalUriHandler.listener = null
        }
    }

    NavHost(
        modifier = Modifier.background(color = BisqTheme.colors.backgroundColor),
        navController = navController,
        startDestination = Graph.MAIN_SCREEN_GRAPH_KEY,
    ) {
        navigation(
            startDestination = Routes.TabHome.name,
            route = Graph.MAIN_SCREEN_GRAPH_KEY
        ) {
            composable(route = Routes.TabHome.name) {
                selectedTab.value = Routes.TabHome.name
                DashboardScreen()
            }
            composable(route = Routes.TabOfferbook.name) {
                selectedTab.value = Routes.TabOfferbook.name
                OfferbookMarketScreen()
            }
            composable(
                route = Routes.TabOpenTradeList.name,
                deepLinks = listOf(
                    // TODO: fix after refactoring nav
                    navDeepLink {
                        uriPattern = getDeeplinkUriPattern(Routes.TabOpenTradeList)
                    }
                ),
            ) {
                selectedTab.value = Routes.TabOpenTradeList.name
                OpenTradeListScreen()
            }
            composable(route = Routes.TabMiscItems.name) {
                selectedTab.value = Routes.TabMiscItems.name
                MiscItemsScreen()
            }
        }
    }

}
